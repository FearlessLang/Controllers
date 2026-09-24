package controller;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import metaParser.Frame;
import metaParser.Message;
import metaParser.Span;
import tools.Fs;
import userMessages.Report;
import userMessages.UserError;
import utils.OneOr;
import utils.Range;

public sealed interface Info{
  Span span();
  record Str(String value, Span span) implements Info{}
  record Lst(List<Info> items, Span span) implements Info{}
  record Obj(List<Field> fields, Span span) implements Info{
    public record Field(String key, Span keySpan, Info value){}
    public Optional<Field> field(String key){ return OneOr.opt("key "+key, fields.stream().filter(f->f.key().equals(key))); }
  }
  Span noSpan= new Span(URI.create("info:synthetic"),1,1,1,1);
  static Info parse(String text, URI uri){ return new Parser(text,uri).all(); }
  static UserError err(String source, Span span, String msg){
    return Report.infoError(Message.of(_->source,List.of(new Frame("",span)),msg));
  }
  static String print(Info info){
    var sb= new StringBuilder();
    write(info,0,sb);
    return sb.append('\n').toString();
  }
  private static void write(Info info, int indent, StringBuilder sb){
    switch(info){
      case Str s -> quote(s.value(),sb);
      case Lst l -> { sb.append('['); join(l.items(),sb); sb.append(']'); }
      case Obj o -> writeObj(o,indent,sb);
    }
  }
  private static void join(List<Info> items, StringBuilder sb){
    for (int i : Range.of(items)){
      if (i > 0){ sb.append(", "); }
      write(items.get(i),0,sb);
    }
  }
  private static void writeObj(Obj o, int indent, StringBuilder sb){
    if (o.fields().isEmpty()){ sb.append("{}"); return; }
    sb.append("{\n");
    for (int i : Range.of(o.fields())){
      var f= o.fields().get(i);
      sb.append("  ".repeat(indent+1));
      quote(f.key(),sb);
      sb.append(": ");
      write(f.value(),indent+1,sb);
      sb.append(i+1 < o.fields().size() ? ",\n" : "\n");
    }
    sb.append("  ".repeat(indent)).append('}');
  }
  Pattern unsafeRun= Pattern.compile("[^"+Fs.allowed.chars().mapToObj(c->"\\x{"+Integer.toHexString(c)+"}").collect(Collectors.joining())+"]+");
  Pattern uCodeText= Pattern.compile("[0-9A-F]{1,6}(?: [0-9A-F]{1,6})*");
  private static void quote(String value, StringBuilder sb){
    var escaped= value.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n");
    sb.append('"').append(unsafeRun.matcher(escaped).replaceAll(Info::codePoints)).append('"');
  }
  private static String codePoints(MatchResult run){
    var hex= run.group().codePoints().mapToObj(c->Integer.toHexString(c).toUpperCase()).collect(Collectors.joining(" "));
    return Matcher.quoteReplacement("\\u("+hex+")");
  }
  final class Parser{
    private final String text;
    private final URI uri;
    private int i= 0;
    private int line= 1;
    private int col= 1;
    private Parser(String text, URI uri){ this.text= text; this.uri= uri; }
    private Info all(){
      var v= value();
      ws();
      if (more()){ throw err(here(),"Unexpected extra text after the end of the value: a file holds exactly one value."); }
      return v;
    }
    private Info value(){
      ws();
      if (!more()){ throw err(here(),"The text ends here, but a value (a string \"...\", a list [...] or an object {...}) was expected."); }
      return switch(peek()){
        case '"' -> str();
        case '[' -> list();
        case '{' -> obj();
        default -> throw err(here(),"Expected a string \"...\", a list [...] or an object {...} here.");
      };
    }
    private Str str(){
      var start= here();
      advance();
      var sb= new StringBuilder();
      while(true){
        if (!more()){ throw err(from(start),"This string is never closed with a matching \"."); }
        var c= peek();
        if (c == '"'){ var end= here(); advance(); return new Str(sb.toString(),between(start,end)); }
        if (c == '\n'){ throw err(here(),"A string cannot contain a raw newline; write \\n instead."); }
        if (c == '\\'){ advance(); sb.append(escape()); continue; }
        sb.append(advance());
      }
    }
    private String escape(){
      if (!more()){ throw err(here(),"The text ends right after a \\: an escape needs a character after it."); }
      var at= here();
      var c= advance();
      return switch(c){
        case '"' -> "\"";
        case '\\' -> "\\";
        case 'n' -> "\n";
        case 'u' -> codePoints(at);
        default -> throw err(from(at),"Unknown escape \\"+c+": only \\\", \\\\, \\n and \\u(...) exist.");
      };
    }
    private String codePoints(Span at){
      if (!more() || peek() != '('){ throw err(from(at),"The escape \\u needs its code points in parentheses, like \\u(E9 301)."); }
      advance();
      var body= new StringBuilder();
      while(more() && ")\"\n".indexOf(peek()) < 0){ body.append(advance()); }
      if (!more() || peek() != ')'){ throw err(from(at),"The escape \"\\u("+body+"\" is never closed with a matching )."); }
      advance();
      var escape= "\"\\u("+body+")\"";
      if (!uCodeText.matcher(body).matches()){ throw err(from(at),"The escape "+escape+" is malformed: inside \\u(...) write one or more code points, each as 1 to 6 uppercase hex digits, separated by single spaces, like \\u(E9 301)."); }
      var cps= Stream.of(body.toString().split(" ")).mapToInt(h->Integer.parseInt(h,16)).toArray();
      var bad= Arrays.stream(cps).filter(cp->cp > 0x10FFFF || (cp >= 0xD800 && cp <= 0xDFFF)).findFirst();
      if (bad.isPresent()){ throw err(from(at),"The escape "+escape+" holds "+Integer.toHexString(bad.getAsInt()).toUpperCase()+", which is not a Unicode scalar: code points from D800 to DFFF (surrogates) and above 10FFFF are not characters."); }
      return new String(cps,0,cps.length);
    }
    private Lst list(){
      var start= here();
      advance();
      var items= new ArrayList<Info>();
      while(true){
        ws();
        if (!more()){ throw err(from(start),"This list is never closed with a matching ]."); }
        if (peek() == ']' && items.isEmpty()){ break; }
        items.add(value());
        ws();
        if (!more()){ throw err(from(start),"This list is never closed with a matching ]."); }
        if (peek() == ']'){ break; }
        if (peek() != ','){ throw err(here(),"Expected ',' or ']' here, to continue or to close the list."); }
        advance();
      }
      var end= here();
      advance();
      return new Lst(items,between(start,end));
    }
    private Obj obj(){
      var start= here();
      advance();
      var fields= new ArrayList<Obj.Field>();
      var seen= new HashSet<String>();
      while(true){
        ws();
        if (!more()){ throw err(from(start),"This object is never closed with a matching }."); }
        if (peek() == '}' && fields.isEmpty()){ break; }
        if (peek() != '"'){ throw err(here(),"Expected a quoted key \"...\" here."); }
        var key= str();
        if (!seen.add(key.value())){ throw err(key.span(),"Duplicate key \""+key.value()+"\": this object already has this key."); }
        ws();
        if (!more() || peek() != ':'){ throw err(here(),"Expected ':' after the key \""+key.value()+"\"."); }
        advance();
        fields.add(new Obj.Field(key.value(),key.span(),value()));
        ws();
        if (!more()){ throw err(from(start),"This object is never closed with a matching }."); }
        if (peek() == '}'){ break; }
        if (peek() != ','){ throw err(here(),"Expected ',' or '}' here, to continue or to close the object."); }
        advance();
      }
      var end= here();
      advance();
      return new Obj(fields,between(start,end));
    }
    private void ws(){
      while(more()){
        var c= peek();
        if (c == ' ' || c == '\n'){ advance(); continue; }
        var comment= c == '/' && i+1 < text.length() && text.charAt(i+1) == '/';
        if (!comment){ return; }
        while(more() && peek() != '\n'){ advance(); }
      }
    }
    private boolean more(){ return i < text.length(); }
    private char peek(){ return text.charAt(i); }
    private char advance(){
      var c= text.charAt(i);
      if (Fs.allowed.indexOf(c) < 0){ throw err(here(),"The character "+Message.displayChar(c)+" is outside the safe character set of Fearless: letters, digits, space, newline and common punctuation."); }
      i+= 1;
      if (c == '\n'){ line+= 1; col= 1; } else { col+= 1; }
      return c;
    }
    private Span here(){ return new Span(uri,line,col,line,col); }
    private Span from(Span start){ return between(start,here()); }
    private Span between(Span start, Span end){
      return new Span(uri,start.startLine(),start.startCol(),end.startLine(),Math.max(end.startCol(),start.startCol()));
    }
    private UserError err(Span span, String msg){ return Info.err(text,span,msg); }
  }
}
package controller;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
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
      case Str s -> sb.append(expr(s.value()));
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
      assert f.key().codePoints().allMatch(c->safe(c) && c != '"' && c != '\n');
      sb.append("  ".repeat(indent+1)).append('"').append(f.key()).append("\": ");
      write(f.value(),indent+1,sb);
      sb.append(i+1 < o.fields().size() ? ",\n" : "\n");
    }
    sb.append("  ".repeat(indent)).append('}');
  }
  Pattern uCodeText= Pattern.compile("[0-9A-F]{1,6}(?: [0-9A-F]{1,6})*");
  private static boolean safe(int c){ return c < 128 && Fs.allowed.indexOf(c) >= 0; }
  static String expr(String s){ return s.codePoints().allMatch(Info::safe) ? strExpr(s) : uExpr(s.codePoints().toArray()); }
  private static String strExpr(String s){
    var parts= List.of(s.split("\n",-1));
    var res= new StringBuilder(lineExpr(parts.getFirst()));
    for (var part: parts.subList(1,parts.size())){
      if (!part.isEmpty()){ res.append(" | ").append(lineExpr(part)); continue; }
      if (res.charAt(res.length()-1) == '|'){ res.append(' '); }
      res.append('|');
    }
    return res.toString();
  }
  private static String lineExpr(String s){
    if (s.indexOf('"') < 0){ return "\""+s+"\""; }
    if (s.indexOf('`') < 0){ return "`"+s+"`"; }
    return String.join("+",delimiterRuns(s).stream().map(Info::lineExpr).toList());
  }
  private static List<String> delimiterRuns(String s){
    var res= new ArrayList<String>();
    var start= 0;
    char seen= 0;
    for (int i : Range.of(0,s.length())){
      var c= s.charAt(i);
      if (c != '"' && c != '`'){ continue; }
      if (seen == 0){ seen= c; continue; }
      if (seen == c){ continue; }
      res.add(s.substring(start,i));
      start= i;
      seen= c;
    }
    if (start < s.length()){ res.add(s.substring(start)); }
    return res;
  }
  private static String uExpr(int[] cps){
    var res= new StringBuilder();
    var i= 0;
    do{
      var j= run(cps,i,true);
      var k= run(cps,j,false);
      var hex= k == j ? "" : "\""+Arrays.stream(Arrays.copyOfRange(cps,j,k)).mapToObj("%04X"::formatted).collect(Collectors.joining(" "))+"\"";
      var term= receiver(strExpr(new String(cps,i,j-i)))+".u"+hex;
      res.append(i == 0 ? term : "+("+term+")");
      i= k;
    } while (i < cps.length);
    return res.toString();
  }
  private static int run(int[] cps, int i, boolean safe){
    while (i < cps.length && safe(cps[i]) == safe){ i++; }
    return i;
  }  private static String receiver(String e){
    var oneLiteral= e.length() >= 2 && (e.charAt(0) == '"' || e.charAt(0) == '`') && e.charAt(e.length()-1) == e.charAt(0);
    return oneLiteral ? e : "("+e+")";
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
        case '"', '`', '(' -> text();
        case '[' -> list();
        case '{' -> obj();
        default -> throw err(here(),"Expected a string \"...\", a list [...] or an object {...} here.");
      };
    }
    private Str text(){
      var start= here();
      var sb= new StringBuilder(atom());
      while(true){
        ws();
        if (!more()){ break; }
        var c= peek();
        if (c == '+'){ advance(); sb.append(atom()); continue; }
        if (c == '|' || c == '^'){ advance(); sb.append(c == '|' ? "\n" : "\""); if (atomNext()){ sb.append(atom()); } continue; }
        if (c != '.'){ break; }
        u(sb);
      }
      return new Str(sb.toString(),from(start));
    }
    private boolean atomNext(){
      ws();
      return more() && "\"`(".indexOf(peek()) >= 0;
    }
    private String atom(){
      ws();
      if (!more()){ throw err(here(),"The text ends here, but a string \"...\", `...` or (...) was expected."); }
      if (peek() != '('){ return literal().value(); }
      advance();
      var res= text().value();
      ws();
      if (!more() || peek() != ')'){ throw err(here(),"Expected ')' here, to close the parenthesis."); }
      advance();
      return res;
    }
    private Str literal(){
      var start= here();
      var close= peek();
      if (close != '"' && close != '`'){ throw err(here(),"Expected a string \"...\", `...` or (...) here."); }
      advance();
      var sb= new StringBuilder();
      while(true){
        if (!more()){ throw err(from(start),"This string is never closed with a matching "+close+"."); }
        var c= peek();
        if (c == close){ var end= here(); advance(); return new Str(sb.toString(),between(start,end)); }
        if (c == '\n'){ throw err(here(),"A string cannot hold a raw newline: \"a\" | \"b\" is \"a\", a newline, then \"b\"."); }
        sb.append(advance());
      }
    }
    private void u(StringBuilder sb){
      var at= here();
      advance();
      var named= more() && peek() == 'u' && (i+1 == text.length() || !Character.isLetterOrDigit(text.charAt(i+1)));
      if (!named){ throw err(from(at),"After a string, only .u and .u\"...\" are allowed: .u makes it a UStr, .u\"E9 301\" adds the characters with those code points."); }
      advance();
      if (!atomNext()){ return; }
      var start= here();
      var body= atom();
      sb.append(codePoints(new Str(body,from(start))));
    }    private String codePoints(Str hex){
      var body= hex.value();
      if (body.isEmpty()){ return ""; }
      if (!uCodeText.matcher(body).matches()){ throw err(hex.span(),"The code points \""+body+"\" of .u\"...\" are malformed: write one or more code points, each as 1 to 6 uppercase hex digits, separated by single spaces, like .u\"00E9 0301\"."); }
      var cps= Stream.of(body.split(" ")).mapToInt(h->Integer.parseInt(h,16)).toArray();
      var bad= Arrays.stream(cps).filter(cp->cp > 0x10FFFF || (cp >= 0xD800 && cp <= 0xDFFF)).findFirst();
      if (bad.isPresent()){ throw err(hex.span(),"The code points \""+body+"\" of .u\"...\" hold "+Integer.toHexString(bad.getAsInt()).toUpperCase()+", which is not a Unicode scalar: code points from D800 to DFFF (surrogates) and above 10FFFF are not characters."); }
      return new String(cps,0,cps.length);
    }    private Lst list(){
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
        var key= literal();
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
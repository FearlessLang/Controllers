package fearlessPluginProject;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/// Reads the Info files the manager writes (controller.Info in Controllers): objects {...} of
/// strings and objects; an object becomes a Map in field order. A key is "..."; a string is written
/// as Fearless writes one: "..." and `...` joined by +, | and ^, parentheses, .u and .u("...").
/// Anything else is an error naming the file and the offset.
final class Info{
  private static final Pattern uCodeText= Pattern.compile("[0-9A-F]{1,6}(?: [0-9A-F]{1,6})*");
  private final Path file;
  private final String text;
  private int i;
  private Info(Path file){ this.file= file; this.text= ManagerLink.read(file); }
  static Map<String,Object> parse(Path file){
    var p= new Info(file);
    try{
      var res= p.obj();
      p.ws();
      if (p.i != p.text.length()){ throw p.bad("the end of the file"); }
      return res;
    }
    catch(IndexOutOfBoundsException e){ throw p.bad("more text"); }
  }
  @SuppressWarnings("unchecked") static Map<String,Object> obj(Object value){ return (Map<String,Object>)value; }
  private Object value(){
    ws();
    return text.charAt(i) == '{' ? obj() : text();
  }
  private Map<String,Object> obj(){
    var res= new LinkedHashMap<String,Object>();
    expect('{');
    if (next('}')){ return res; }
    do{
      ws();
      var key= literal();
      expect(':');
      if (res.put(key, value()) != null){ throw bad("a key other than \""+key+"\""); }
    }
    while(next(','));
    expect('}');
    return res;
  }
  private String text(){
    var sb= new StringBuilder(atom());
    while(true){
      ws();
      if (i == text.length()){ return sb.toString(); }
      var c= text.charAt(i);
      if (c == '+'){ i+= 1; sb.append(atom()); continue; }
      if (c == '|' || c == '^'){ i+= 1; sb.append(c == '|' ? "\n" : "\""); ws(); if (i < text.length() && "\"`(".indexOf(text.charAt(i)) >= 0){ sb.append(atom()); } continue; }
      if (c != '.'){ return sb.toString(); }
      i+= 1;
      if (text.charAt(i++) != 'u' || (i < text.length() && Character.isLetterOrDigit(text.charAt(i)))){ throw bad(".u or .u(...)"); }
      if (!next('(')){ continue; }
      sb.append(codePoints(text()));
      expect(')');
    }
  }
  private String atom(){
    ws();
    if (!next('(')){ return literal(); }
    var res= text();
    expect(')');
    return res;
  }
  private String literal(){
    var close= text.charAt(i++);
    if (close != '"' && close != '`'){ throw bad("a string \"...\", `...` or (...)"); }
    var end= text.indexOf(close, i);
    if (end < 0 || text.substring(i, end).indexOf('\n') >= 0){ throw bad("a string closed on its line"); }
    var res= text.substring(i, end);
    i= end+1;
    return res;
  }
  private String codePoints(String body){
    if (body.isEmpty()){ return ""; }
    if (!uCodeText.matcher(body).matches()){ throw bad("code points in .u(...), each 1 to 6 uppercase hex digits, separated by single spaces,"); }
    var cps= Stream.of(body.split(" ")).mapToInt(h->Integer.parseInt(h,16)).toArray();
    if (Arrays.stream(cps).anyMatch(cp->cp > 0x10FFFF || (cp >= 0xD800 && cp <= 0xDFFF))){ throw bad("code points in .u(...) of Unicode scalars only (no D800 to DFFF, nothing above 10FFFF)"); }
    return new String(cps,0,cps.length);
  }  private boolean next(char c){
    ws();
    if (text.charAt(i) != c){ return false; }
    i+= 1;
    return true;
  }
  private void expect(char c){ if (!next(c)){ throw bad("'"+c+"'"); } }
  private void ws(){ while(i < text.length() && (text.charAt(i) == ' ' || text.charAt(i) == '\n')){ i+= 1; } }
  private IllegalStateException bad(String expected){ return new IllegalStateException("The file "+file+" is not what the manager writes: "+expected+" was expected at offset "+i+"."); }
}

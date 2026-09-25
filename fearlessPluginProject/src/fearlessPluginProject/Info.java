package fearlessPluginProject;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/// Reads the Info files the manager writes (controller.Info in Controllers): objects {...} of
/// strings "..." (escapes \" \\ \n \\u(...)) and objects; an object becomes a Map in field order.
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
    return text.charAt(i) == '"' ? str() : obj();
  }
  private Map<String,Object> obj(){
    var res= new LinkedHashMap<String,Object>();
    expect('{');
    if (next('}')){ return res; }
    do{
      var key= str();
      expect(':');
      if (res.put(key, value()) != null){ throw bad("a key other than \""+key+"\""); }
    }
    while(next(','));
    expect('}');
    return res;
  }
  private String str(){
    expect('"');
    var sb= new StringBuilder();
    for (var c= text.charAt(i++); c != '"'; c= text.charAt(i++)){
      sb.append(c == '\\' ? escape(text.charAt(i++)) : String.valueOf(c));
    }
    return sb.toString();
  }
  private String escape(char c){
    if (c == 'n'){ return "\n"; }
    if (c == '"' || c == '\\'){ return String.valueOf(c); }
    if (c != 'u' || text.charAt(i++) != '('){ throw bad("an escape \\n, \\\", \\\\ or \\u(...)"); }
    var end= text.indexOf(')',i);
    var body= end < 0 ? "" : text.substring(i,end);
    if (!uCodeText.matcher(body).matches()){ throw bad("a \\u(...) of code points, each 1 to 6 uppercase hex digits, separated by single spaces,"); }
    var cps= Stream.of(body.split(" ")).mapToInt(h->Integer.parseInt(h,16)).toArray();
    if (Arrays.stream(cps).anyMatch(cp->cp > 0x10FFFF || (cp >= 0xD800 && cp <= 0xDFFF))){ throw bad("a \\u(...) of Unicode scalars only (no D800 to DFFF, nothing above 10FFFF)"); }
    i= end+1;
    return new String(cps,0,cps.length);
  }
  private boolean next(char c){
    ws();
    if (text.charAt(i) != c){ return false; }
    i+= 1;
    return true;
  }
  private void expect(char c){ if (!next(c)){ throw bad("'"+c+"'"); } }
  private void ws(){ while(i < text.length() && (text.charAt(i) == ' ' || text.charAt(i) == '\n')){ i+= 1; } }
  private IllegalStateException bad(String expected){ return new IllegalStateException("The file "+file+" is not what the manager writes: "+expected+" was expected at offset "+i+"."); }
}

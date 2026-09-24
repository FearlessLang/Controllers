package fearlessPluginProject;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/// Reads the Info files the manager writes (controller.Info in Controllers): objects {...} of
/// strings "..." (escapes \" \\ \n) and objects; an object becomes a Map in field order.
/// Anything else is an error naming the file and the offset.
final class Info{
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
      if (c == '\\'){ c= escape(text.charAt(i++)); }
      sb.append(c);
    }
    return sb.toString();
  }
  private char escape(char c){
    if (c == 'n'){ return '\n'; }
    if (c == '"' || c == '\\'){ return c; }
    throw bad("an escape \\n, \\\" or \\\\");
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

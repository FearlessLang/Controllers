package fearlessPluginProject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Reads what controller.Info (in Controllers) prints: a string "..." with the escapes
/// \" \\ \n, a list [...] or an object {...}; a string becomes a String, a list a List,
/// an object a Map in field order. What the manager prints is well formed, so nothing is checked.
public final class Info{
  private final String text;
  private int i;
  private Info(String text){ this.text= text; }
  public static Object parse(String text){ return new Info(text).value(); }
  @SuppressWarnings("unchecked") public static Map<String,Object> obj(Object value){ return (Map<String,Object>)value; }
  private Object value(){
    ws();
    var c= text.charAt(i);
    if (c == '"'){ return str(); }
    if (c == '['){ return list(); }
    return obj();
  }
  private List<Object> list(){
    var res= new ArrayList<Object>();
    for (i+= 1; !closes(']'); comma()){ res.add(value()); }
    return res;
  }
  private Map<String,Object> obj(){
    var res= new LinkedHashMap<String,Object>();
    for (i+= 1; !closes('}'); comma()){
      var key= str();
      ws();
      i+= 1;
      res.put(key, value());
    }
    return res;
  }
  private boolean closes(char c){
    ws();
    if (text.charAt(i) != c){ return false; }
    i+= 1;
    return true;
  }
  private void comma(){
    ws();
    if (text.charAt(i) == ','){ i+= 1; }
  }
  private void ws(){ while(text.charAt(i) == ' ' || text.charAt(i) == '\n'){ i+= 1; } }
  private String str(){
    ws();
    var sb= new StringBuilder();
    for (i+= 1; text.charAt(i) != '"'; i+= 1){
      var c= text.charAt(i);
      if (c == '\\'){ i+= 1; c= text.charAt(i) == 'n' ? '\n' : text.charAt(i); }
      sb.append(c);
    }
    i+= 1;
    return sb.toString();
  }
}

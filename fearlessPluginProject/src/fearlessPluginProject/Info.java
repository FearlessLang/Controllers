package fearlessPluginProject;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/// Reads the Info files the manager writes (controller.Info in Controllers): objects {...} of
/// strings "..." (escapes \" \\ \n) and objects; an object becomes a Map in field order.
/// A text that may hold characters outside the Fearless character set (a path, a compile error)
/// is tagged, as controller.TaggedText writes it: "Str:" then the text, "UStr:" then the text as
/// UStr.escape prints it, or "Base16:" then the bytes naming a path on this system.
/// Anything else is an error naming the file and the offset.
final class Info{
  private static final Pattern uCodeText= Pattern.compile("[0-9A-F]{1,6}(?: [0-9A-F]{1,6})*");
  private final String file;
  private final String text;
  private int i;
  private Info(String file, String text){ this.file= file; this.text= text; }
  static Map<String,Object> parse(Path file){
    var p= new Info(file.toString(), ManagerLink.read(file));
    try{
      var res= p.obj();
      p.ws();
      if (p.i != p.text.length()){ throw p.bad("the end of the file"); }
      return res;
    }
    catch(IndexOutOfBoundsException e){ throw p.bad("more text"); }
  }
  @SuppressWarnings("unchecked") static Map<String,Object> obj(Object value){ return (Map<String,Object>)value; }
  static String tagged(Object value){
    var s= (String)value;
    if (s.startsWith("Str:")){ return s.substring("Str:".length()); }
    if (s.startsWith("UStr:")){
      var p= new Info("the text \""+s+"\"", s.substring("UStr:".length()));
      try{
        var res= p.expr();
        if (p.i != p.text.length()){ throw p.bad("the end of the text"); }
        return res;
      }
      catch(IndexOutOfBoundsException e){ throw p.bad("more text"); }
    }
    if (!s.startsWith("Base16:") || !s.substring("Base16:".length()).matches("(?:[0-9A-F]{2})+")){ throw new IllegalStateException("The manager wrote the malformed text \""+s+"\"."); }
    return path(HexFormat.of().parseHex(s.substring("Base16:".length())), s);
  }
  //As controller.TaggedText: UTF-16 little endian units on Windows, sun.jnu.encoding bytes
  //elsewhere. TO TEST on Linux and macOS.
  private static String path(byte[] bytes, String s){
    if (System.getProperty("os.name").startsWith("Windows")){
      if (bytes.length % 2 != 0){ throw new IllegalStateException("The manager wrote the malformed path \""+s+"\"."); }
      var res= new StringBuilder();
      for (int j= 0; j < bytes.length; j+= 2){ res.append((char)((bytes[j] & 0xFF) | (bytes[j+1] & 0xFF) << 8)); }
      return res.toString();
    }
    try{ return Charset.forName(System.getProperty("sun.jnu.encoding")).newDecoder().decode(ByteBuffer.wrap(bytes)).toString(); }
    catch(CharacterCodingException e){ throw new IllegalStateException("The manager wrote the path \""+s+"\", which this system can not give to Java.", e); }
  }
  private Object value(){
    ws();
    return text.charAt(i) == '"' ? str() : obj();
  }
  private Map<String,Object> obj(){
    var res= new LinkedHashMap<String,Object>();
    expect('{');
    if (next('}')){ return res; }
    do{
      ws();
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
      sb.append(c == '\\' ? escape(text.charAt(i++)) : c);
    }
    return sb.toString();
  }
  private char escape(char c){
    if (c == 'n'){ return '\n'; }
    if (c == '"' || c == '\\'){ return c; }
    throw bad("an escape \\n, \\\" or \\\\");
  }
  private String expr(){
    var sb= new StringBuilder(atom());
    while(true){
      ws();
      if (i == text.length()){ return sb.toString(); }
      var c= text.charAt(i);
      if (c == '+'){ i+= 1; sb.append(atom()); continue; }
      if (c == '|' || c == '^'){ i+= 1; sb.append(c == '|' ? "\n" : "\""); if (atomNext()){ sb.append(atom()); } continue; }
      if (!text.startsWith(".u", i) || (i+2 < text.length() && Character.isLetterOrDigit(text.charAt(i+2)))){ return sb.toString(); }
      i+= 2;
      if (atomNext()){ sb.append(codePoints(atom())); }
    }
  }
  private boolean atomNext(){
    ws();
    return i < text.length() && "\"`(".indexOf(text.charAt(i)) >= 0;
  }
  private String atom(){
    if (!atomNext()){ throw bad("a string \"...\", `...` or (...)"); }
    var open= text.charAt(i++);
    if (open == '('){
      var res= expr();
      expect(')');
      return res;
    }
    var end= text.indexOf(open, i);
    if (end < 0){ throw bad("the closing "+open); }
    var res= text.substring(i, end);
    i= end+1;
    return res;
  }
  private String codePoints(String body){
    if (body.isEmpty()){ return ""; }
    if (!uCodeText.matcher(body).matches()){ throw bad("code points in .u\"...\", each 1 to 6 uppercase hex digits, separated by single spaces,"); }
    var cps= Stream.of(body.split(" ")).mapToInt(h->Integer.parseInt(h,16)).toArray();
    if (Arrays.stream(cps).anyMatch(cp->cp > 0x10FFFF || (cp >= 0xD800 && cp <= 0xDFFF))){ throw bad("code points in .u\"...\" of Unicode scalars only (no D800 to DFFF, nothing above 10FFFF)"); }
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
  private IllegalStateException bad(String expected){ return new IllegalStateException(file+" is not what the manager writes: "+expected+" was expected at offset "+i+"."); }
}

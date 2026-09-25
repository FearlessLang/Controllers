package controller;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import tools.Fs;
import userMessages.UserError;
import utils.Bug;
import utils.Range;

/// A text that may hold characters outside the Fearless character set, as the files and messages
/// of the manager write it: "Str:" then the text, when every character is in the Fearless
/// character set; else "UStr:" then the text as UStr.escape prints it, when it is Unicode; else
/// (a path only) "Base16:" then the bytes naming it on this system, two uppercase hex digits each.
/// Any of the three reads back as the same text.
public final class TaggedText{
  private TaggedText(){}
  private static final Pattern uCodeText= Pattern.compile("[0-9A-F]{1,6}(?: [0-9A-F]{1,6})*");
  public static String of(String text){
    if (text.codePoints().allMatch(TaggedText::safe)){ return "Str:"+text; }
    if (text.codePoints().noneMatch(TaggedText::surrogate)){ return "UStr:"+uStr(text.codePoints().toArray()); }
    return "Base16:"+HexFormat.of().withUpperCase().formatHex(bytes(text));
  }
  static String read(String text, Function<String,UserError> bad){
    if (text.startsWith("Str:")){ return text.substring("Str:".length()); }
    if (text.startsWith("UStr:")){ return new Reader(text.substring("UStr:".length()),bad).all(); }
    if (!text.startsWith("Base16:")){ throw bad.apply("\""+text+"\" is malformed: it starts with \"Str:\", \"UStr:\" or \"Base16:\", then the text written that way."); }
    var hex= text.substring("Base16:".length());
    if (!hex.matches("(?:[0-9A-F]{2})+")){ throw bad.apply("\""+text+"\" is malformed: after \"Base16:\" a path is the bytes naming it, each as 2 uppercase hex digits."); }
    return path(HexFormat.of().parseHex(hex),text,bad);
  }
  private static boolean safe(int c){ return c < 128 && Fs.allowed.indexOf(c) >= 0; }
  private static boolean surrogate(int c){ return c >= 0xD800 && c <= 0xDFFF; }
  //A Windows name is UTF-16 code units, written little endian; an unpaired surrogate is a unit
  //like any other, and Java keeps it in a String. Tested on Windows only.
  //Elsewhere a name is bytes, which Java decodes with sun.jnu.encoding: a String Java got from
  //such a system has no unpaired surrogate, so "Base16:" is never written there. TO TEST on
  //Linux and macOS: reading a "Base16:" path, and a folder whose name is not valid in that
  //encoding (Java gives the manager another name, which names nothing).
  private static byte[] bytes(String path){
    if (!Fs.isWindows()){ throw Bug.unreachable(); }
    var res= new byte[path.length()*2];
    for (int i : Range.of(0,path.length())){ res[2*i]= (byte)path.charAt(i); res[2*i+1]= (byte)(path.charAt(i) >> 8); }
    return res;
  }
  private static String path(byte[] bytes, String text, Function<String,UserError> bad){
    if (Fs.isWindows()){
      if (bytes.length % 2 != 0){ throw bad.apply("\""+text+"\" is malformed: a Windows name is 16 bit units, so its bytes are an even number."); }
      var res= new StringBuilder();
      for (int i= 0; i < bytes.length; i+= 2){ res.append((char)((bytes[i] & 0xFF) | (bytes[i+1] & 0xFF) << 8)); }
      return res.toString();
    }
    try{ return Charset.forName(System.getProperty("sun.jnu.encoding")).newDecoder().decode(ByteBuffer.wrap(bytes)).toString(); }
    catch(CharacterCodingException e){ throw bad.apply("\""+text+"\" names a path this system can not give to Java: its bytes are not valid in "+System.getProperty("sun.jnu.encoding")+"."); }
  }
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
    return String.join("+",delimiterRuns(s).stream().map(TaggedText::lineExpr).toList());
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
  private static String uStr(int[] cps){
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
  }
  private static String receiver(String e){
    var oneLiteral= e.length() >= 2 && (e.charAt(0) == '"' || e.charAt(0) == '`') && e.charAt(e.length()-1) == e.charAt(0);
    return oneLiteral ? e : "("+e+")";
  }
  /// A Fearless string expression: "..." and `...` joined by +, | and ^ (between two strings, or
  /// closing one), parentheses, .u, and .u"..." adding the characters with those code points.
  private static final class Reader{
    private final String s;
    private final Function<String,UserError> bad;
    private int i;
    Reader(String s, Function<String,UserError> bad){ this.s= s; this.bad= bad; }
    String all(){
      var res= expr();
      if (i != s.length()){ throw fail("the end of the text"); }
      return res;
    }
    private UserError fail(String expected){ return bad.apply("\"UStr:"+s+"\" is malformed: "+expected+" was expected at offset "+i+" after \"UStr:\"."); }
    private String expr(){
      var sb= new StringBuilder(atom());
      while(true){
        ws();
        if (i == s.length()){ return sb.toString(); }
        var c= s.charAt(i);
        if (c == '+'){ i+= 1; sb.append(atom()); continue; }
        if (c == '|' || c == '^'){ i+= 1; sb.append(c == '|' ? "\n" : "\""); if (atomNext()){ sb.append(atom()); } continue; }
        if (!s.startsWith(".u",i) || (i+2 < s.length() && Character.isLetterOrDigit(s.charAt(i+2)))){ return sb.toString(); }
        i+= 2;
        if (atomNext()){ sb.append(codePoints(atom())); }
      }
    }
    private boolean atomNext(){
      ws();
      return i < s.length() && "\"`(".indexOf(s.charAt(i)) >= 0;
    }
    private String atom(){
      if (!atomNext()){ throw fail("a string \"...\", `...` or (...)"); }
      var open= s.charAt(i++);
      if (open == '('){
        var res= expr();
        if (i == s.length() || s.charAt(i) != ')'){ throw fail("')'"); }
        i+= 1;
        return res;
      }
      var end= s.indexOf(open,i);
      if (end < 0){ throw fail("the closing "+open); }
      var res= s.substring(i,end);
      i= end+1;
      return res;
    }
    private String codePoints(String body){
      if (body.isEmpty()){ return ""; }
      if (!uCodeText.matcher(body).matches()){ throw fail("code points, each as 1 to 6 uppercase hex digits, separated by single spaces,"); }
      var cps= Stream.of(body.split(" ")).mapToInt(h->Integer.parseInt(h,16)).toArray();
      if (Arrays.stream(cps).anyMatch(cp->cp > 0x10FFFF || surrogate(cp))){ throw fail("Unicode scalars (no D800 to DFFF, nothing above 10FFFF)"); }
      return new String(cps,0,cps.length);
    }
    private void ws(){ while(i < s.length() && s.charAt(i) == ' '){ i+= 1; } }
  }
}

package suggest;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/// The documentation of the types of one package and of their methods, from the text rendering
/// the compiler writes next to the api json (docBuilder.HtmlDocRenderer.renderText): a type's
/// header line, the type's own documentation at two spaces, then one line per method at two
/// spaces with its documentation at four or more.
public final class Docs{
  private record Entry(String header, List<String> lines){}
  private static final Pattern selector= Pattern.compile("\\._*[a-z][A-Za-z0-9_]*'*|[\\\\/#*\\-+%<>=!&^~?|]+");
  private final List<String> all;
  public Docs(String txt){ this.all= List.of(txt.split("\n")); }
  /// the type's line and its own documentation, the lines before its first method
  public Optional<String> type(String type){
    return entry(type).map(e->text(e.header, e.lines.stream().takeWhile(l->!isMethod(l)).toList()));
  }
  /// the method's line and its documentation, when the method is there
  public Optional<String> method(String type, String name, int arity){
    var e= entry(type);
    if (e.isEmpty()){ return Optional.empty(); }
    var lines= e.get().lines;
    int i= 0;
    for (; i < lines.size() && (lines.get(i).startsWith("   ") || !signature(lines.get(i)).equals(name+"/"+arity)); i+= 1){}
    if (i >= lines.size()){ return Optional.empty(); }
    int j= i+1;
    for (; j < lines.size() && lines.get(j).startsWith("   "); j+= 1){}
    return Optional.of(text(lines.get(i), lines.subList(i+1, j)));
  }
  /// the header line of the type and the lines indented under it
  private Optional<Entry> entry(String type){
    var header= Pattern.compile(Pattern.quote(type)+"(\\[.*| :.*)?");
    int i= 0;
    for (; i < all.size() && !header.matcher(all.get(i)).matches(); i+= 1){}
    if (i >= all.size()){ return Optional.empty(); }
    int j= i+1;
    for (; j < all.size() && all.get(j).startsWith("  "); j+= 1){}
    return Optional.of(new Entry(all.get(i), all.subList(i+1, j)));
  }
  private static String text(String head, List<String> rest){
    var res= new StringBuilder(head.strip());
    rest.forEach(l->res.append("\n").append(l.strip()));
    return res.toString();
  }
  /// a line at the indent of a method carrying a name only a method can have; the documentation
  /// of the type is written at that same indent and is prose
  static boolean isMethod(String line){
    if (!line.startsWith("  ") || line.startsWith("   ")){ return false; }
    var sig= signature(line);
    return selector.matcher(sig.substring(0, sig.lastIndexOf('/'))).matches();
  }
  /// name/arity of a method line, [RC] name[Bs](T1,..,Tn):T; a documentation line at the same
  /// indent yields something no method is called
  static String signature(String line){
    var s= line.strip().replaceFirst("^(readH|mutH|imm|iso|read|mut) ", "");
    int j= 0;
    for (; j < s.length() && "[(:".indexOf(s.charAt(j)) < 0; j+= 1){}
    var name= s.substring(0, j);
    if (j < s.length() && s.charAt(j) == '['){ j= s.indexOf(']', j)+1; }
    if (j >= s.length() || s.charAt(j) != '('){ return name+"/0"; }
    var arity= 1;
    for (int depth= 0, k= j+1; k < s.length() && depth >= 0; k+= 1){
      var c= s.charAt(k);
      if (c == '(' || c == '['){ depth+= 1; }
      if (c == ')' || c == ']'){ depth-= 1; }
      if (c == ',' && depth == 0){ arity+= 1; }
    }
    return name+"/"+arity;
  }
}

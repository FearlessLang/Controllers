package suggest;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/// The documentation of the methods of one type, from the text rendering the compiler writes
/// next to the api json (docBuilder.HtmlDocRenderer.renderText): the type's header line, its
/// own documentation at two spaces, then one line per method at two spaces with its
/// documentation at four or more.
public final class Docs{
  private final List<String> lines;
  public Docs(String txt, String type){
    var all= List.of(txt.split("\n"));
    var header= Pattern.compile(Pattern.quote(type)+"(\\[.*| :.*)?");
    int i= 0;
    for (; i < all.size() && !header.matcher(all.get(i)).matches(); i+= 1){}
    int j= Math.min(i+1, all.size());
    for (; j < all.size() && all.get(j).startsWith("  "); j+= 1){}
    this.lines= all.subList(Math.min(i+1, all.size()), j);
  }
  /// the method's line and its documentation, when the method is there
  public Optional<String> of(String name, int arity){
    int i= 0;
    for (; i < lines.size() && (lines.get(i).startsWith("   ") || !signature(lines.get(i)).equals(name+"/"+arity)); i+= 1){}
    if (i >= lines.size()){ return Optional.empty(); }
    var res= new StringBuilder(lines.get(i).strip());
    for (i+= 1; i < lines.size() && lines.get(i).startsWith("   "); i+= 1){ res.append("\n").append(lines.get(i).strip()); }
    return Optional.of(res.toString());
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

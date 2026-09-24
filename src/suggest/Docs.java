package suggest;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/// The documentation of the types of one package and of their methods, from the text rendering
/// the compiler writes next to the api json (docBuilder.HtmlDocRenderer.renderText): a type's
/// header line, Name or Name[X1:bounds,..,Xn:bounds], then " : " and its supertypes if any; the
/// type's own documentation under it; then one line per method, [RC] name[Bs](Ts):T, opened by
/// a tab, with its own documentation under that. A tab opens a method line and nothing else;
/// documentation lines are indented or blank. Every public type and every method of a type is
/// there: a missing one is an error. An anonymous literal, whose name is private, has no entry.
public final class Docs{
  private static final Pattern generic= Pattern.compile("[\\[,]_*[A-Z]");
  private static final Pattern methodLine= Pattern.compile("\t(?:(?:readH|mutH|imm|iso|read|mut) )?([^\\[(:]+)(?:\\[[^\\]]*\\])?(?:\\((.*)\\))?:.*");
  private final List<String> lines;
  public Docs(String txt){ this.lines= List.of(txt.split("\n")); }
  /// the type's line and its own documentation, the lines before its first method
  public Optional<String> type(String simple, int arity){ return header(simple, arity).map(this::text); }
  /// the method's line and its documentation
  public Optional<String> method(String simple, int arity, String name, int n){
    return header(simple, arity).map(i->text(IntStream.range(i+1, entryEnd(i))
      .filter(j->lines.get(j).startsWith("\t") && signature(lines.get(j)).equals(name+"/"+n)).findFirst()
      .orElseThrow(()->new IllegalArgumentException("No method "+name+"/"+n+" of "+simple+" in the documentation"))));
  }
  private Optional<Integer> header(String simple, int arity){
    var res= IntStream.range(0, lines.size()).filter(i->isHeader(lines.get(i), simple, arity)).boxed().findFirst();
    if (res.isEmpty() && !simple.startsWith("_")){ throw new IllegalArgumentException("No type "+simple+" with "+arity+" generics in the documentation"); }
    return res;
  }
  private static boolean isHeader(String line, String simple, int arity){
    var h= line.split(" : ")[0];
    if (arity == 0){ return h.equals(simple); }
    return h.startsWith(simple+"[") && generic.matcher(h.substring(simple.length())).results().count() == arity;
  }
  private static boolean opens(String line){ return !line.isBlank() && !line.startsWith(" "); }
  private int entryEnd(int i){ return IntStream.range(i+1, lines.size()).filter(j->opens(lines.get(j)) && !lines.get(j).startsWith("\t")).findFirst().orElse(lines.size()); }
  /// line i and the lines under it, up to the next line opening a type or a method
  private String text(int i){
    var res= new StringBuilder(lines.get(i).strip());
    for (int j= i+1; j < lines.size() && !opens(lines.get(j)); j+= 1){ res.append("\n").append(lines.get(j).strip()); }
    return res.toString().strip();
  }
  /// name/arity of a method line, [RC] name[Bs](T1,..,Tn):T
  static String signature(String line){
    var m= methodLine.matcher(line);
    if (!m.matches()){ throw new IllegalArgumentException("Malformed method line in the documentation: "+line.strip()); }
    var ps= m.group(2);
    for (; ps != null && ps.contains("["); ps= ps.replaceAll("\\[[^\\[\\]]*\\]", "")){}
    return m.group(1)+"/"+(ps == null ? 0 : ps.split(",").length);
  }
}

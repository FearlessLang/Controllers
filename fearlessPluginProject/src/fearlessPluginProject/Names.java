package fearlessPluginProject;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IPath;

final class Names{
  private static final Set<String> noExtension= Set.of("readme", "license", "copying", "notice", "authors", "contributors", "changelog", "changes", "news", "todo", "thanks", "security", "contributing", "code_of_conduct");
  private static final Set<String> multiDot= Set.of("tar.gz", "tar.bz2", "tar.xz", "tar.zst", "tar.lz", "tar.lz4", "d.ts", "d.ts.map", "js.map", "css.map", "min.js", "min.css");
  private static final Set<String> windows= Set.of("con", "prn", "aux", "nul", "com1", "com2", "com3", "com4", "com5", "com6", "com7", "com8", "com9", "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9");
  static Optional<String> problem(IPath container, String name, boolean file){
    var inside= container.segmentCount() > 1 && container.segment(1).equals(FearlessWatcher.srcName);
    if (!inside){ return Optional.of("\""+container+"\" is outside the folder of the project: its files and folders go inside \"src\"."); }
    return problem(container.removeFirstSegments(2).append(name).toString(), file);
  }
  static Optional<String> problem(String path, boolean file){
    var segments= List.of(path.split("/", -1));
    for (int i= 0; i < segments.size(); i += 1){
      var p= segment(segments.get(i), file && i == segments.size()-1);
      if (p.isPresent()){ return p; }
    }
    var pkgs= segments.stream().filter(s -> s.startsWith("_") && !s.contains(".")).toList();
    if (pkgs.size() > 1){ return Optional.of("\""+path+"\" is inside two package folders, \""+pkgs.get(0)+"\" and \""+pkgs.get(1)+"\": a package folder cannot hold another package folder."); }
    var outside= file && pkgs.isEmpty() && path.endsWith(".fear");
    if (outside){ return Optional.of("\""+path+"\" is outside every package folder: a .fear file goes in a package folder, a folder whose name starts with \"_\"."); }
    return pkgs.stream().flatMap(Names::pkg).findFirst();
  }
  private static Stream<String> pkg(String folder){
    var name= folder.substring(1);
    if (name.equals("base") || name.equals("rank")){ return Stream.of("\""+folder+"\" is a reserved package name: \"base\" and \"rank\" belong to the standard library."); }
    return name.matches("[a-z][a-z0-9_]*") ? Stream.of() : Stream.of("\""+folder+"\" is not a valid package folder: after \"_\" a package name starts with a lowercase letter.");
  }
  private static Optional<String> segment(String s, boolean file){
    if (s.isEmpty()){ return Optional.of("A name is empty: the path has \"/\" at its start or end, or twice in a row."); }
    if (s.startsWith(".")){ return hidden(s); }
    var dot= s.indexOf('.');
    var atom= dot < 0 ? s : s.substring(0, dot);
    var c0= atom.charAt(0);
    if (!(c0 == '_' || (c0 >= 'a' && c0 <= 'z'))){ return Optional.of("\""+s+"\" starts with \""+c0+"\": a Fearless name starts with a lowercase letter or \"_\"."); }
    var bad= atom.chars().filter(c -> !(c == '_' || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9'))).findFirst();
    if (bad.isPresent()){ return Optional.of("\""+s+"\" holds \""+(char)bad.getAsInt()+"\": a Fearless name holds only lowercase letters, digits and \"_\" before its extension."); }
    if (windows.contains(atom)){ return Optional.of("\""+s+"\" is named like the Windows device \""+atom+"\": Windows cannot hold it."); }
    if (dot < 0){
      var lone= file && !noExtension.contains(s);
      return lone ? Optional.of("\""+s+"\" has no extension: only "+String.join(", ", noExtension.stream().sorted().toList())+" can have none.") : Optional.empty();
    }
    var ext= s.substring(dot+1);
    if (ext.contains(".")){ return multiDot.contains(ext) ? Optional.empty() : Optional.of("\""+s+"\" has the extension \""+ext+"\": an extension with a dot is one of "+String.join(", ", multiDot.stream().sorted().toList())+"."); }
    return ext.matches("[a-z0-9]{1,16}") ? Optional.empty() : Optional.of("\""+s+"\" has the extension \""+ext+"\": an extension is 1 to 16 lowercase letters and digits.");
  }
  private static Optional<String> hidden(String s){
    var bad= s.endsWith(".") || s.endsWith(" ") || s.chars().anyMatch(c -> c < 32 || c == 127 || "<>:\"\\|?*".indexOf(c) >= 0);
    return bad ? Optional.of("\""+s+"\" is not a valid hidden name: it cannot end with \".\" or a space, nor hold a control character or one of <>:\"\\|?*") : Optional.empty();
  }
}

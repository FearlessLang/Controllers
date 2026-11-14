package suggest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class Shapes{
  public static final String ext= ".fearless";
  private static final boolean windows= System.getProperty("os.name").startsWith("Windows");
  private static final List<String> multiDot= List.of("css.map", "d.ts", "d.ts.map", "js.map", "min.css", "min.js", "tar.bz2", "tar.gz", "tar.lz", "tar.lz4", "tar.xz", "tar.zst");
  private static final Set<String> reserved= Set.of("con", "prn", "aux", "nul", "conin$", "conout$",
    "com1", "com2", "com3", "com4", "com5", "com6", "com7", "com8", "com9", "com\u00B9", "com\u00B2", "com\u00B3",
    "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9", "lpt\u00B9", "lpt\u00B2", "lpt\u00B3");
  public static boolean validSegment(String s){ return s.length() <= 191 && s.matches("[a-z_][a-z0-9_]*") && !reserved.contains(s); }
  public static String fixedSegment(String s){
    var res= s.replaceAll("[^A-Za-z0-9_]+", "_").toLowerCase(Locale.ROOT);
    return validSegment(res) ? res : "p"+res;
  }
  public static boolean isMarker(String fileName){ return fileName.endsWith(ext) && !fileName.startsWith("."); }
  public static boolean reachable(Path folder){ return Files.isDirectory(folder, LinkOption.NOFOLLOW_LINKS) && Files.isReadable(folder); }
  public static List<Path> entries(Path folder){
    try (var s= Files.list(folder)){ return s.sorted(Comparator.comparing(p -> p.getFileName().toString())).toList(); }
    catch (IOException e){ throw new UncheckedIOException(e); }
  }
  public static List<Path> markers(Path folder){ return entries(folder).stream().filter(p -> isMarker(name(p)) && Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS)).toList(); }
  public static String marker(Path folder){ return markers(folder).stream().findFirst().map(Shapes::stem).orElse(""); }
  public static String infer(Path folder, Set<String> taken){
    var all= entries(folder);
    var first= all.stream().filter(p -> isMarker(name(p)) && Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS)).findFirst();
    var others= all.stream().filter(p -> first.filter(p::equals).isEmpty()).map(Shapes::name).toList();
    Predicate<String> ok= n -> validSegment(n) && !taken.contains(n) && others.stream().noneMatch(o -> o.equalsIgnoreCase(n+ext));
    var m= first.map(Shapes::stem).filter(ok);
    if (m.isPresent()){ return m.get(); }
    var fixed= fixedSegment(name(folder));
    var f= fixed.substring(0, Math.min(fixed.length(), 64));
    return IntStream.iterate(1, i -> i+1).mapToObj(i -> i == 1 ? f : f+i).filter(ok).findFirst().orElseThrow();
  }
  private static String name(Path p){ return p.getFileName().toString(); }
  private static String stem(Path file){ return name(file).substring(0, name(file).length()-ext.length()); }
  public static Optional<String> problem(String path, boolean file, List<String> siblings){
    var segments= List.of(path.split("/", -1));
    var last= segments.size()-1;
    return IntStream.rangeClosed(0, last).mapToObj(i -> entry(segments.subList(0, i+1), file && i == last, i == last ? siblings : List.of())).flatMap(Optional::stream).findFirst();
  }
  private static Optional<String> entry(List<String> segments, boolean file, List<String> siblings){
    var s= segments.getLast();
    if (s.isEmpty()){ return Optional.of("A name is empty: the path has \"/\" at its start or end, or twice in a row."); }
    var path= String.join("/", segments);
    var hidden= segments.stream().anyMatch(n -> n.startsWith("."));
    var limit= hidden ? 250 : 200;
    if (len(path) > limit){ return Optional.of("\""+path+"\" is longer than "+limit+" characters: a path inside a project folder is at most 200 characters long, or 250 for a hidden file or folder."); }
    var p= hidden ? hiddenName(s) : file ? fileName(s) : segment(s, s);
    return p.or(() -> together(s, hidden, siblings));
  }
  private static int len(String s){ return windows ? s.length() : s.getBytes(StandardCharsets.UTF_8).length; }
  private static Optional<String> together(String s, boolean hidden, List<String> siblings){
    var clash= siblings.stream().filter(o -> fold(o).equals(fold(s))).findFirst();
    if (clash.isPresent()){ return Optional.of("\""+s+"\" and \""+clash.get()+"\" differ only in letter case or in Unicode normalization: a folder cannot contain both."); }
    var zip= siblings.stream().filter(o -> !hidden && (o.equals(s+".zip") || s.equals(o+".zip"))).findFirst();
    return zip.map(o -> "\""+s+"\" and \""+o+"\" are a name and that name followed by \".zip\": a folder cannot contain both.");
  }
  private static String fold(String s){
    return Normalizer.normalize(s, Normalizer.Form.NFC).codePoints().map(c -> c == 0x130 || c == 0x131 ? c : Character.toLowerCase(Character.toUpperCase(c)))
      .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
  }
  private static Optional<String> segment(String s, String atom){
    var c0= atom.charAt(0);
    if (!(c0 == '_' || (c0 >= 'a' && c0 <= 'z'))){ return Optional.of("\""+s+"\" starts with \""+c0+"\": a Fearless name starts with a lowercase letter or \"_\"."); }
    var bad= atom.chars().filter(c -> !(c == '_' || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9'))).findFirst();
    if (bad.isPresent()){ return Optional.of("\""+s+"\" contains \""+(char)bad.getAsInt()+"\": a Fearless name contains only lowercase letters, digits and \"_\"."); }
    if (atom.length() > 191){ return Optional.of("\""+s+"\" is a name longer than 191 characters: a Fearless name is at most 191 characters long."); }
    return device(s, atom);
  }
  private static Optional<String> device(String s, String atom){
    if (!reserved.contains(atom)){ return Optional.empty(); }
    return Optional.of("\""+s+"\" is named like the device \""+atom+"\" of Windows: Windows cannot hold it.");
  }
  private static Optional<String> fileName(String s){
    var dot= s.indexOf('.');
    if (dot < 0){ return segment(s, s).or(() -> Optional.of("\""+s+"\" has no extension: a file name is a name, a dot and an extension.")); }
    var res= segment(s, s.substring(0, dot));
    if (res.isPresent()){ return res; }
    var x= s.substring(dot+1);
    if (x.contains(".")){ return multiDot.contains(x) ? Optional.empty() : Optional.of("\""+s+"\" has the extension \""+x+"\": an extension with a dot is one of "+String.join(", ", multiDot)+"."); }
    return x.matches("[a-z0-9]{1,16}") ? Optional.empty() : Optional.of("\""+s+"\" has the extension \""+x+"\": an extension is 1 to 16 lowercase letters and digits.");
  }
  private static Optional<String> hiddenName(String s){
    if (s.endsWith(".") || s.endsWith(" ")){ return Optional.of("\""+s+"\" ends with \""+s.substring(s.length()-1)+"\": a hidden name does not end with a dot or a space."); }
    var control= s.chars().filter(c -> c < 0x20 || (c >= 0x7F && c <= 0x9F)).findFirst();
    if (control.isPresent()){ return Optional.of("\""+s+"\" contains the control character U+"+"%04X".formatted(control.getAsInt())+": a hidden name contains no control character."); }
    var broken= s.indexOf('\uFFFD') >= 0 || !s.equals(new String(s.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));
    if (broken){ return Optional.of("\""+s+"\" contains U+FFFD or half of a surrogate pair: a hidden name is text the file system can store."); }
    var bad= s.chars().filter(c -> "<>:\"/\\|?*".indexOf(c) >= 0).findFirst();
    if (bad.isPresent()){ return Optional.of("\""+s+"\" contains \""+(char)bad.getAsInt()+"\": a hidden name contains none of < > : \" / \\ | ? *"); }
    var dot= s.indexOf('.');
    var res= device(s, (dot < 0 ? s : s.substring(0, dot)).stripTrailing().toLowerCase(Locale.ROOT));
    if (res.isPresent()){ return res; }
    return s.getBytes(StandardCharsets.UTF_8).length <= 255 ? Optional.empty() : Optional.of("\""+s+"\" is longer than 255 bytes in UTF-8: a hidden name is at most 255 bytes long.");
  }
  public static Optional<String> fearless(String path, boolean file){
    var p= problem(path, file, List.of());
    var segments= List.of(path.split("/"));
    if (p.isPresent() || segments.stream().anyMatch(s -> s.startsWith("."))){ return p; }
    var pkgs= segments.stream().filter(s -> s.startsWith("_") && !s.contains(".")).toList();
    if (pkgs.size() > 1){ return Optional.of("\""+path+"\" is inside two package folders, \""+pkgs.get(0)+"\" and \""+pkgs.get(1)+"\": a package folder cannot contain another package folder."); }
    var outside= file && pkgs.isEmpty() && path.endsWith(".fear");
    if (outside){ return Optional.of("\""+path+"\" is outside every package folder: a .fear file goes in a package folder, a folder whose name starts with \"_\"."); }
    return pkgs.stream().flatMap(Shapes::pkg).findFirst();
  }
  private static Stream<String> pkg(String folder){
    var name= folder.substring(1);
    if (name.equals("base") || name.equals("rank")){ return Stream.of("\""+folder+"\" is a reserved package name: \"base\" and \"rank\" belong to the standard library."); }
    if (reserved.contains(name)){ return Stream.of("\""+folder+"\" is not a valid package folder: \""+name+"\" is named like a device of Windows, and Windows cannot hold it."); }
    return name.matches("[a-z][a-z0-9_]*") ? Stream.of() : Stream.of("\""+folder+"\" is not a valid package folder: after \"_\" a package name starts with a lowercase letter.");
  }
}

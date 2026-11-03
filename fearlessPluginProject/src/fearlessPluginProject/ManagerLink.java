package fearlessPluginProject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.eclipse.core.runtime.FileLocator;
import org.osgi.framework.FrameworkUtil;

import suggest.Api;

/// The manager this plugin was connected to (see controller.Eclipse in Controllers).
/// manager.info, next to the plugins folder holding this plugin, names the manager program,
/// the manager folder and the compiled standard library. The plugin reads projects.info of
/// the manager folder and the .fearless_out folder of each project, and commands the manager
/// by running the manager program with a call: FearlessCommand<Verb> and the arguments of the verb, separated by spaces.
public final class ManagerLink{
  public record Project(Path folder, String kind, List<String> mains){}
  private static final Map<String,Object> link= Info.parse(FileLocator.getBundleFileLocation(FrameworkUtil.getBundle(ManagerLink.class)).orElseThrow().toPath().getParent().resolveSibling("manager.info"));
  private static final Path launcher= path(link.get("launcher"));
  private static final Path manager= path(link.get("manager"));
  static final Path baseCache= path(link.get("baseCache"));
  static Map<String,Project> projects(){
    var res= new LinkedHashMap<String,Project>();
    Info.lst(Info.parse(manager.resolve("projects.info")).get("projects")).stream().map(p -> project(Info.obj(p))).forEach(p -> marker(p.folder()).ifPresent(name -> res.putIfAbsent(name, p)));
    return res;
  }
  private static Optional<String> marker(Path folder){
    if (!Files.isDirectory(folder, LinkOption.NOFOLLOW_LINKS) || !Files.isReadable(folder)){ return Optional.empty(); }
    try (var files= Files.list(folder)){ return files.filter(p -> p.getFileName().toString().endsWith(".fearless") && !p.getFileName().toString().startsWith(".") && Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS)).map(p -> p.getFileName().toString()).sorted().findFirst().map(n -> n.substring(0, n.length()-".fearless".length())); }
    catch (NoSuchFileException e){ return Optional.empty(); }
    catch (IOException e){ throw new UncheckedIOException(e); }
  }
  static Project project(String alias){ return Objects.requireNonNull(projects().get(alias)); }
  private static Project project(Map<String,Object> o){
    var mains= o.containsKey("mains") ? Info.lst(o.get("mains")).stream().map(m -> (String)m).toList() : List.<String>of();
    return new Project(path(o.get("path")), (String)o.get("kind"), mains);
  }
  static Path out(Path folder){ return folder.resolve(".fearless_out"); }
  static List<String> mains(Path folder){
    return jsons(out(folder)).flatMap(f -> present(f).stream()).flatMap(json -> Info.obj(Info.parse(json.getKey().toString(), Api.parse(json.getValue()).manifest()).get("mains")).keySet().stream()).toList();
  }
  static Stream<Path> jsons(Path out){
    if (!Files.isDirectory(out)){ return Stream.of(); }
    try (var files= Files.list(out)){ return files.filter(p -> p.getFileName().toString().endsWith(".json") && !p.getFileName().toString().startsWith("_")).sorted().toList().stream(); }
    catch (NoSuchFileException e){ return Stream.of(); }
    catch (IOException e){ throw new UncheckedIOException(e); }
  }
  /// the error of _error.txt, while _error.txt is not a stale error file (see ControllersDesign, Eclipse)
  static Optional<String> error(Path folder){
    var file= out(folder).resolve("_error.txt");
    var time= stamp(file);
    if (time.isEmpty() || stale(folder, time.get())){ return Optional.empty(); }
    return present(file).map(Map.Entry::getValue);
  }
  /// the file and its text; none for a file that the manager deleted meanwhile (a compilation, a DeleteCache)
  private static Optional<Map.Entry<Path,String>> present(Path file){
    try{ return Optional.of(Map.entry(file, Files.readString(file))); }
    catch (NoSuchFileException e){ return Optional.empty(); }
    catch (IOException e){ throw new UncheckedIOException(e); }
  }
  private static boolean stale(Path folder, long time){
    var now= System.currentTimeMillis();
    var config= folder.resolve(".config");
    var icon= config.resolve("icon");
    var res= new boolean[]{false};
    var visitor= new SimpleFileVisitor<Path>(){
      FileVisitResult seen(BasicFileAttributes a){
        var t= a.lastModifiedTime().toMillis();
        res[0] = t > time && t <= now;
        return res[0] ? FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
      }
      @Override public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes a){
        if (!visible(folder, dir) && !dir.equals(config) && !dir.equals(icon)){ return FileVisitResult.SKIP_SUBTREE; }
        return seen(a);
      }
      @Override public FileVisitResult visitFile(Path file, BasicFileAttributes a){
        if (!visible(folder, file) && !file.getParent().equals(icon)){ return FileVisitResult.CONTINUE; }
        return seen(a);
      }
      @Override public FileVisitResult visitFileFailed(Path file, IOException e){ return FileVisitResult.CONTINUE; }
    };
    try{ Files.walkFileTree(folder, visitor); }
    catch (IOException e){ throw new UncheckedIOException(e); }
    return res[0];
  }
  private static boolean visible(Path folder, Path p){
    return StreamSupport.stream(folder.relativize(p).spliterator(), false).noneMatch(n -> n.toString().startsWith("."));
  }
  private static Optional<Long> stamp(Path file){
    try{ return Optional.of(Files.getLastModifiedTime(file).toMillis()); }
    catch (NoSuchFileException e){ return Optional.empty(); }
    catch (IOException e){ throw new UncheckedIOException(e); }
  }
  //As controller.TaggedText: the bytes of a name on Linux and macOS are the ones its file URI escapes.
  static String pathArg(Path path){
    var text= path.toString();
    if (!Info.windows){ return "Base16:"+HexFormat.of().withUpperCase().formatHex(uriBytes(path.toAbsolutePath().toUri().getRawPath())); }
    var bytes= new byte[text.length()*2];
    for (int i= 0; i < text.length(); i += 1){ bytes[2*i] = (byte)text.charAt(i); bytes[2*i+1] = (byte)(text.charAt(i) >> 8); }
    return "Base16:"+HexFormat.of().withUpperCase().formatHex(bytes);
  }
  private static byte[] uriBytes(String raw){
    var res= new ByteArrayOutputStream();
    for (int i= 0; i < raw.length(); i += 1){
      if (raw.charAt(i) != '%'){ res.write(raw.charAt(i)); continue; }
      res.write(HexFormat.fromHexDigits(raw, i+1, i+3));
      i += 2;
    }
    var bytes= res.toByteArray();
    return bytes.length > 1 && bytes[bytes.length-1] == '/' ? Arrays.copyOf(bytes, bytes.length-1) : bytes;
  }
  private static Path path(Object text){ return Info.taggedPath(text); }
  /// Runs the manager program with the call of the verb and its arguments, and waits for the end of that run, at most 10 seconds:
  /// a run that becomes the 'single' manager does not end, and it has written its message files before taking instance.lock.
  static void send(String verb, String... args){
    var cmd= List.of(launcher.toString(), Stream.concat(Stream.of("FearlessCommand"+verb), Stream.of(args)).collect(Collectors.joining(" ")));
    try{ new ProcessBuilder(cmd).redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start().waitFor(10, TimeUnit.SECONDS); }
    catch (IOException e){ throw new UncheckedIOException(e); }
    catch (InterruptedException e){ Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
  }
  static String read(Path file){
    try{ return Files.readString(file); }
    catch (IOException e){ throw new UncheckedIOException(e); }
  }
  static String lines(Path file){
    try{
      var bytes= Files.readAllBytes(file);
      var end= bytes.length;
      while (end > 0 && bytes[end-1] != '\n'){ end -= 1; }
      return StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes, 0, end)).toString();
    }
    catch (IOException e){ throw new UncheckedIOException(e); }
  }
}

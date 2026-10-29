package fearlessPluginProject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
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
import java.util.stream.Stream;

import org.eclipse.core.runtime.FileLocator;
import org.osgi.framework.FrameworkUtil;

/// The manager this plugin was connected to (see controller.Eclipse in Controllers).
/// manager.info, next to the plugins folder holding this plugin, names the manager program,
/// the manager folder and the compiled standard library. The plugin reads projects.info of
/// the manager folder and the .fearless_out folder of each project, and commands the manager
/// by running the manager program with the arguments FearlessCommand<Verb> and the arguments of the verb.
public final class ManagerLink{
  public record Project(Path folder, String kind, List<String> mains){}
  private static final Map<String,Object> link= Info.parse(FileLocator.getBundleFileLocation(FrameworkUtil.getBundle(ManagerLink.class)).orElseThrow().toPath().getParent().resolveSibling("manager.info"));
  private static final Path launcher= path(link.get("launcher"));
  private static final Path manager= path(link.get("manager"));
  static final Path baseCache= path(link.get("baseCache"));
  static Map<String,Project> projects(){
    var res= new LinkedHashMap<String,Project>();
    Info.obj(Info.parse(manager.resolve("projects.info")).get("projects")).forEach((alias,p) -> res.put(alias, project(Info.obj(p))));
    return res;
  }
  static Project project(String alias){ return Objects.requireNonNull(projects().get(alias)); }
  private static Project project(Map<String,Object> o){
    var mains= o.containsKey("mains") ? Info.lst(o.get("mains")).stream().map(m -> (String)m).toList() : List.<String>of();
    return new Project(path(o.get("path")), (String)o.get("kind"), mains);
  }
  static Path out(Path folder){ return folder.resolve(".fearless_out"); }
  static Map<String,String> mains(Path folder){
    var res= new LinkedHashMap<String,String>();
    for (var file : manifests(folder)){ Info.obj(Info.parse(file).get("mains")).forEach((main,m) -> res.put(main, (String)Info.lst(m).getFirst())); }
    return res;
  }
  static List<Path> manifests(Path folder){
    var out= out(folder);
    if (!Files.isDirectory(out)){ return List.of(); }
    try (var s= Files.list(out)){ return s.filter(p -> p.getFileName().toString().endsWith("_manifest.info")).sorted().toList(); }
    catch (IOException e){ throw new UncheckedIOException(e); }
  }
  static Optional<String> error(Path folder, String name){
    var file= out(folder).resolve(name);
    if (!Files.exists(file)){ return Optional.empty(); }
    return changed(folder) > stamp(file) ? Optional.empty() : Optional.of(read(file));
  }
  private static long changed(Path folder){
    var res= new long[]{stamp(manager.resolve("projects.info"))};
    var visitor= new SimpleFileVisitor<Path>(){
      @Override public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes a){
        var dot= !dir.equals(folder) && dir.getFileName().toString().startsWith(".");
        if (dot){ return FileVisitResult.SKIP_SUBTREE; }
        res[0] = Math.max(res[0], a.lastModifiedTime().toMillis());
        return FileVisitResult.CONTINUE;
      }
      @Override public FileVisitResult visitFile(Path file, BasicFileAttributes a){
        res[0] = Math.max(res[0], a.lastModifiedTime().toMillis());
        return FileVisitResult.CONTINUE;
      }
      @Override public FileVisitResult visitFileFailed(Path file, IOException e) throws IOException{
        if (e instanceof NoSuchFileException){ return FileVisitResult.CONTINUE; }
        throw e;
      }
    };
    try{ Files.walkFileTree(folder, visitor); }
    catch (IOException e){ throw new UncheckedIOException(e); }
    return res[0];
  }
  private static long stamp(Path file){
    try{ return Files.getLastModifiedTime(file).toMillis(); }
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
  static void send(String verb, String... args){
    var cmd= Stream.concat(Stream.of(launcher.toString(), "FearlessCommand"+verb), Stream.of(args)).toList();
    try{ new ProcessBuilder(cmd).redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start().waitFor(); }
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

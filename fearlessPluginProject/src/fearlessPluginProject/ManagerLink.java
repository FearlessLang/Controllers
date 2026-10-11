package fearlessPluginProject;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
  private ManagerLink(){}
  private static final Map<String,Object> link= Info.parse(FileLocator.getBundleFileLocation(FrameworkUtil.getBundle(ManagerLink.class)).orElseThrow().toPath().getParent().resolveSibling("manager.info"));
  private static final Path launcher= path(link.get("launcher"));
  private static final Path manager= path(link.get("manager"));
  static final Path baseCache= path(link.get("baseCache"));
  static Map<String,Project> projects(){
    var res= new LinkedHashMap<String,Project>();
    Info.obj(Info.parse(manager.resolve("projects.info")).get("projects")).forEach((alias,p)->res.put(alias, project(Info.obj(p))));
    return res;
  }
  static Project project(String alias){ return Objects.requireNonNull(projects().get(alias)); }
  private static Project project(Map<String,Object> o){
    var mains= o.containsKey("mains") ? Info.lst(o.get("mains")).stream().map(m->(String)m).toList() : List.<String>of();
    return new Project(path(o.get("path")), (String)o.get("kind"), mains);
  }
  static Path out(Path folder){ return folder.resolve(".fearless_out"); }
  static Map<String,String> mains(Path folder){
    var file= out(folder).resolve("mains.info");
    var res= new LinkedHashMap<String,String>();
    if (Files.exists(file)){ Info.parse(file).forEach((main,m)->res.put(main, (String)Info.lst(m).getFirst())); }
    return res;
  }
  static Optional<String> compileError(Path folder){
    var file= out(folder).resolve("compile_error.txt");
    return Files.exists(file) ? Optional.of(read(file)) : Optional.empty();
  }
  static String pathArg(Path path){
    var text= path.toString();
    if (!System.getProperty("os.name").startsWith("Windows")){ return "Base16:"+HexFormat.of().withUpperCase().formatHex(text.getBytes(Charset.forName(System.getProperty("sun.jnu.encoding")))); }
    var bytes= new byte[text.length()*2];
    for (int i= 0; i < text.length(); i++){ bytes[2*i]= (byte)text.charAt(i); bytes[2*i+1]= (byte)(text.charAt(i) >> 8); }
    return "Base16:"+HexFormat.of().withUpperCase().formatHex(bytes);
  }
  private static Path path(Object text){ return Path.of(Info.tagged(text)); }
  static void send(String verb, String... args){
    var cmd= Stream.concat(Stream.of(launcher.toString(), "FearlessCommand"+verb), Stream.of(args)).toList();
    try{ new ProcessBuilder(cmd).redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start().waitFor(); }
    catch(IOException e){ throw new UncheckedIOException(e); }
    catch(InterruptedException e){ Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
  }
  static String read(Path file){
    try{ return Files.readString(file); }
    catch(IOException e){ throw new UncheckedIOException(e); }
  }
  static String lines(Path file){
    try{
      var bytes= Files.readAllBytes(file);
      var end= bytes.length;
      while(end > 0 && bytes[end-1] != '\n'){ end-= 1; }
      return StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes, 0, end)).toString();
    }
    catch(IOException e){ throw new UncheckedIOException(e); }
  }
}

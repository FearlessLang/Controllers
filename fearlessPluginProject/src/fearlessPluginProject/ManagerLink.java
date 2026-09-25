package fearlessPluginProject;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.eclipse.core.runtime.FileLocator;
import org.osgi.framework.FrameworkUtil;

/// The files of the manager this plugin was connected to (see controller.Eclipse in Controllers).
/// manager.info, next to the plugins folder holding this plugin, names the manager folder and
/// the compiled standard library. In the eclipse folder of the manager folder, state.info holds
/// every registered project by name, the folder named like a project holds its console.txt and,
/// once it ran, its report.xml, and console.txt holds the manager's own notes.
/// A message is a verb, a newline, a project name (a folder for register), a newline, then a third line some verbs
/// use; the manager applies *.msg files, so a message is written as .tmp and renamed into place.
public final class ManagerLink{
  public record Project(Path folder, String kind, String running, int runs, String lastRun, int exit, Map<String,String> mains, Map<String,String> problem){}
  private ManagerLink(){}
  private static final Map<String,Object> link= Info.parse(FileLocator.getBundleFileLocation(FrameworkUtil.getBundle(ManagerLink.class)).orElseThrow().toPath().getParent().resolveSibling("manager.info"));
  private static final Path manager= path(link.get("manager"));
  static final Path baseCache= path(link.get("baseCache"));
  static Path eclipse(){ return manager.resolve("eclipse"); }
  static Map<String,Project> projects(){
    var res= new LinkedHashMap<String,Project>();
    Info.parse(eclipse().resolve("state.info")).forEach((alias,p)->res.put(alias, project(Info.obj(p))));
    return res;
  }
  static Project project(String alias){ return Objects.requireNonNull(projects().get(alias)); }
  private static Project project(Map<String,Object> o){
    return new Project(path(o.get("folder")), (String)o.get("kind"), (String)o.get("running"), Integer.parseInt((String)o.get("runs")),
      (String)o.get("lastRun"), Integer.parseInt((String)o.get("exit")), strings(o.get("mains")), problem(strings(o.get("problem"))));
  }
  /// a path for a message: "Str:" then its text, when every character is in the Fearless
  /// character set, else "Base16:" then the bytes naming it, as Info reads them back
  static String pathText(String path){
    if (path.chars().allMatch(c->c < 128 && allowed.indexOf(c) >= 0)){ return "Str:"+path; }
    //UTF-16 little endian units on Windows, sun.jnu.encoding bytes elsewhere. TO TEST on Linux and macOS.
    if (!System.getProperty("os.name").startsWith("Windows")){ return "Base16:"+HexFormat.of().withUpperCase().formatHex(path.getBytes(Charset.forName(System.getProperty("sun.jnu.encoding")))); }
    var bytes= new byte[path.length()*2];
    for (int i= 0; i < path.length(); i++){ bytes[2*i]= (byte)path.charAt(i); bytes[2*i+1]= (byte)(path.charAt(i) >> 8); }
    return "Base16:"+HexFormat.of().withUpperCase().formatHex(bytes);
  }
  private static final String allowed= "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ+-*/=<>,.;:()[]{}`'\"!?@#$%^&_|~\\ \n";
  private static Path path(Object text){ return Path.of(Info.tagged(text)); }  private static Map<String,String> problem(Map<String,String> p){
    if (p.isEmpty()){ return p; }
    return Map.of("file", p.get("file"), "line", p.get("line"), "message", Info.tagged(p.get("message")));
  }
  private static Map<String,String> strings(Object o){
    var res= new LinkedHashMap<String,String>();
    Info.obj(o).forEach((k,v)->res.put(k, (String)v));
    return res;
  }
  static void send(String verb, String name){ send(verb, name, ""); }
  static void send(String verb, String name, String third){
    var file= "%020d-%s".formatted(System.currentTimeMillis(), UUID.randomUUID());
    var messages= manager.resolve("messages");
    try{
      Files.writeString(messages.resolve(file+".tmp"), verb+"\n"+name+"\n"+third);
      Files.move(messages.resolve(file+".tmp"), messages.resolve(file+".msg"), StandardCopyOption.ATOMIC_MOVE);
    }
    catch(IOException e){ throw new UncheckedIOException(e); }
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

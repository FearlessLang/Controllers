package fearlessPluginProject;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
  private static final Path manager= Path.of((String)link.get("manager"));
  static final Path baseCache= Path.of((String)link.get("baseCache"));
  static Path eclipse(){ return manager.resolve("eclipse"); }
  static Map<String,Project> projects(){
    var res= new LinkedHashMap<String,Project>();
    Info.parse(eclipse().resolve("state.info")).forEach((alias,p)->res.put(alias, project(Info.obj(p))));
    return res;
  }
  static Project project(String alias){ return Objects.requireNonNull(projects().get(alias)); }
  private static Project project(Map<String,Object> o){
    return new Project(Path.of((String)o.get("folder")), (String)o.get("kind"), (String)o.get("running"), Integer.parseInt((String)o.get("runs")),
      (String)o.get("lastRun"), Integer.parseInt((String)o.get("exit")), strings(o.get("mains")), strings(o.get("problem")));
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

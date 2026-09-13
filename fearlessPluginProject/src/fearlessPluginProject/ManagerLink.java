package fearlessPluginProject;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.core.runtime.Platform;

/// What the manager left at connect time (see controller.Eclipse in Controllers):
/// manager.txt names its messages folder, its eclipse folder, then the documentation of
/// the standard library. In the eclipse folder, projects.txt lists every registered
/// project as alias, space, folder, and each alias's reports (state.txt, problems.txt,
/// console.txt, report.xml) sit in a folder of that name.
/// A message is a verb, a newline, then a project folder, then a third line some verbs
/// use; the manager drains *.msg files, so a message is written as .tmp and renamed into place.
public final class ManagerLink{
  private final Path messages;
  private final Path eclipse;
  public final Path baseDocs;
  private ManagerLink(Path messages, Path eclipse, Path baseDocs){ this.messages= messages; this.eclipse= eclipse; this.baseDocs= baseDocs; }
  public static Optional<ManagerLink> find(){
    var install= new File(Platform.getInstallLocation().getURL().getPath());
    var file= new File(install,"dropins/fearless/manager.txt").toPath();
    if (!Files.exists(file)){ return Optional.empty(); }
    var lines= read(file).lines().toList();
    return Optional.of(new ManagerLink(Path.of(lines.get(0)), Path.of(lines.get(1)), Path.of(lines.get(2))));
  }
  public Map<String,Path> projects(){
    var res= new LinkedHashMap<String,Path>();
    for (var line : read(eclipse.resolve("projects.txt")).lines().toList()){
      var space= line.indexOf(' ');
      res.put(line.substring(0,space), Path.of(line.substring(space+1)));
    }
    return res;
  }
  public Path reports(String alias){ return eclipse.resolve(alias); }
  public Path console(){ return eclipse.resolve("console.txt"); }
  /// What the manager publishes about a project (see controller.Eclipse.state): its kind,
  /// the main being run if any, how many runs it started, the main of the last one and its
  /// exit code, and the known mains each with the file declaring it. Empty until published.
  public record State(String kind, String running, int runs, String lastRun, int exit, Map<String,String> mains){}
  public Optional<State> state(String alias){
    var text= read(reports(alias).resolve("state.txt"));
    if (text.isEmpty()){ return Optional.empty(); }
    var o= Info.obj(Info.parse(text));
    var mains= new LinkedHashMap<String,String>();
    Info.obj(o.get("mains")).forEach((main,file)->mains.put(main, (String)file));
    return Optional.of(new State((String)o.get("kind"), (String)o.get("running"), Integer.parseInt((String)o.get("runs")), (String)o.get("lastRun"), Integer.parseInt((String)o.get("exit")), mains));
  }
  public void send(String verb, Path folder){ send(verb, folder, ""); }
  public void send(String verb, Path folder, String third){
    var name= "%020d-%s".formatted(System.currentTimeMillis(), UUID.randomUUID());
    var tmp= messages.resolve(name+".tmp");
    try{
      Files.writeString(tmp, verb+"\n"+folder+"\n"+third);
      Files.move(tmp, messages.resolve(name+".msg"), StandardCopyOption.ATOMIC_MOVE);
    }
    catch(IOException e){ throw new UncheckedIOException(e); }
  }
  public static String read(Path file){
    try{ return Files.exists(file) ? Files.readString(file) : ""; }
    catch(IOException e){ throw new UncheckedIOException(e); }
  }
}

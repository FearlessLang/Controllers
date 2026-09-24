package controller;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import controller.Info.Obj;
import controller.Info.Obj.Field;
import controller.Info.Str;
import tools.Fs;
import tools.JavacTool;
import userMessages.Report;
import userMessages.Violation;

/// The manager's side of the Eclipse plugin (fearlessPluginProject). Eclipse writes
/// nothing into a project folder: it reads projects.txt and each alias's reports from
/// dir, and asks for work through the messages folder named in manager.txt.
public record Eclipse(Path dir){
  private static final Pattern at= Pattern.compile("(?m)^In file: fear:/(\\S+)\\n\\n(\\d+)\\| ");
  public Path reports(String alias){ return dir.resolve(alias); }
  public Path console(String alias){ return reports(alias).resolve("console.txt"); }
  public Path notes(){ return dir.resolve("console.txt"); }
  /// What state.txt says about a project, as Info: its kind, whether the compiled cache is
  /// stale, its job if any, the main being run if any, how many runs the manager started,
  /// the main of the last one and its exit code, and the known mains each with the file declaring it.
  public static String stateText(Project p){
    var mainFields= p.mains().orElse(Map.of()).entrySet().stream().map(e->field(e.getKey(),e.getValue())).toList();
    return Info.print(new Obj(List.of(
      field("kind",p.kind().text),
      field("needsCompiling",""+p.needsCompiling()),
      field("busy",p.job()),
      field("running",p.running().orElse("")),
      field("runs",""+p.runs()),
      field("lastRun",p.lastRun()),
      field("exit",""+p.exit()),
      new Field("mains",Info.noSpan,new Obj(mainFields,Info.noSpan))),Info.noSpan));
  }
  private static Field field(String key, String value){ return new Field(key,Info.noSpan,new Str(value,Info.noSpan)); }
  public static String listing(List<Project> projects){ return String.join("",projects.stream().map(p->p.alias()+" "+p.folder()+"\n").toList()); }
  public void publish(String listing){ replace(dir.resolve("projects.txt"),listing); }
  public void state(Project p){ replace(reports(p.alias()).resolve("state.txt"),stateText(p)); }
  public static void append(Path file, String text){
    Fs.ensureDir(file.getParent());
    Fs.ofV(()->Files.writeString(file,text,CREATE,APPEND));
  }
  public String connect(Path chosen, Path msgDir){
    var eclipse= chosen.getParent();
    if (!Files.isRegularFile(eclipse.resolve(".eclipseproduct"))){ throw Report.notAnEclipseInstall(eclipse); }
    var plugin= JavacTool.reqAppDir(Violation::mustUseLauncher).resolve("eclipsePlugin");
    var fearless= eclipse.resolve("dropins").resolve("fearless");
    Fs.copyFresh(plugin,fearless.resolve("plugins"));
    Fs.writeUtf8(fearless.resolve("manager.txt"),msgDir+"\n"+dir+"\n"+Deployed.stdLib("baseCache").resolve("base.html")+"\n");
    return """
Eclipse is now connected:
%s

Restart Eclipse: every project this manager knows appears in its Fearless
perspective. File > New makes a project, Project > Build compiles it, the
Run button runs it, and the Terminate button of its console stops it.
""".formatted(eclipse);
  }
  private static void replace(Path file, String text){
    var tmp= file.resolveSibling(file.getFileName()+".tmp");
    Fs.writeUtf8(tmp,text);
    Fs.ofV(()->Files.move(tmp,file,ATOMIC_MOVE));
  }
  /// One Problems view marker per project: relative file path, line number, then the message; empty when the compile succeeded.
  public static void problems(Path reports, String message){
    var m= at.matcher(message);
    Fs.writeUtf8(reports.resolve("problems.txt"),m.find() ? m.group(1)+"\n"+m.group(2)+"\n"+message : "");
  }
}

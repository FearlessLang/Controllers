package controller;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import controller.Info.Obj;
import controller.Info.Obj.Field;
import controller.Info.Str;
import controller.Registry.Entry;
import controller.Registry.Kind;
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
  public Path notes(){ return dir.resolve("console.txt"); }
  public void note(String text){ append(notes(),text); }
  /// What state.txt says about a project, as Info: its kind, whether the compiled cache is
  /// stale, the job it is busy with if any, the main being run if any, how many runs the
  /// manager started, the main of the last one and its exit code, and the known mains each
  /// with the file declaring it.
  public static String state(Kind kind, boolean needsCompiling, String busy, Optional<String> running, int runs, String lastRun, int exit, Optional<Map<String,String>> mains){
    var mainFields= mains.orElse(Map.of()).entrySet().stream().map(e->field(e.getKey(),e.getValue())).toList();
    return Info.print(new Obj(List.of(
      field("kind",kind.text),
      field("needsCompiling",""+needsCompiling),
      field("busy",busy),
      field("running",running.orElse("")),
      field("runs",""+runs),
      field("lastRun",lastRun),
      field("exit",""+exit),
      new Field("mains",Info.noSpan,new Obj(mainFields,Info.noSpan))),Info.noSpan));
  }
  private static Field field(String key, String value){ return new Field(key,Info.noSpan,new Str(value,Info.noSpan)); }
  public void state(String alias, String text){ replace(reports(alias).resolve("state.txt"),text); }
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
    Fs.writeUtf8(fearless.resolve("manager.txt"),msgDir+"\n"+dir+"\n"+Session.stdLib("baseCache").resolve("base.html")+"\n");
    return """
Eclipse is now connected:
%s

Restart Eclipse: every project this manager knows appears in its Fearless
perspective. File > New makes a project, Project > Build compiles it, the
Run button runs it, and the Terminate button of its console stops it.
""".formatted(eclipse);
  }
  public void publish(List<Entry> known){
    replace(dir.resolve("projects.txt"),String.join("",known.stream().map(e->e.alias()+" "+e.path()+"\n").toList()));
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

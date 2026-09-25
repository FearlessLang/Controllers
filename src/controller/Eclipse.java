package controller;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import controller.Info.Obj;
import controller.Info.Obj.Field;
import controller.Info.Str;
import fileSupport.JUnitReport;
import tools.Fs;
import tools.JavacTool;
import userMessages.Violation;

/// The manager's side of the Eclipse plugin (fearlessPluginProject): the files of dir that
/// the plugin reads. A file the plugin reads whole is replaced at once, never rewritten in place.
public record Eclipse(Path dir){
  private static final Pattern at= Pattern.compile("(?m)^In file: fear:/(\\S+)\\n\\n(\\d+)\\| ");
  public Path console(String alias){ return dir.resolve(alias).resolve("console.txt"); }
  public Path notes(){ return dir.resolve("console.txt"); }
  public static String state(List<Project> projects){ return Info.print(obj(projects.stream().map(p->new Field(p.alias(),Info.noSpan,project(p))).toList())); }
  private static Obj project(Project p){
    var m= at.matcher(p.failure());
    var problem= m.find() ? List.of(field("file",str(m.group(1))),field("line",str(m.group(2))),field("message",str(TaggedText.of(p.failure())))) : List.<Field>of();
    return obj(List.of(
      field("folder",str(TaggedText.of(p.folder().toString()))),
      field("kind",str(p.kind().text)),
      field("running",str(p.running().orElse(""))),
      field("runs",str(""+p.runs())),
      field("lastRun",str(p.lastRun())),
      field("exit",str(""+p.exit())),
      field("mains",obj(p.mains().orElse(Map.of()).entrySet().stream().map(e->field(e.getKey(),str(e.getValue()))).toList())),
      field("problem",obj(problem))));
  }
  private static Obj obj(List<Field> fields){ return new Obj(fields,Info.noSpan); }
  private static Field field(String key, Info value){ return new Field(key,Info.noSpan,value); }
  private static Str str(String value){ return new Str(value,Info.noSpan); }
  public void publish(String state){ replace(dir.resolve("state.info"),state); }
  public void report(String alias, Path folder, String main, Instant since){
    var next= dir.resolve(alias).resolve("next");
    JUnitReport.write(next,folder,main,since);
    if (Files.exists(JUnitReport.file(next))){ Fs.ofV(()->Files.move(JUnitReport.file(next),JUnitReport.file(dir.resolve(alias)),ATOMIC_MOVE)); }
  }
  public static void append(Path file, String text){
    Fs.ensureDir(file.getParent());
    Fs.ofV(()->Files.writeString(file,text,CREATE,APPEND));
  }
  static List<Path> installs(Path dir){
    var bases= Stream.concat(Stream.of(dir),Fs.of(()->{ try(var s= Files.list(dir)){ return s.filter(Files::isDirectory).sorted(Comparator.comparing(p->p.getFileName().toString())).toList(); } }).stream());
    return bases.flatMap(b->Stream.of(b,b.resolve("eclipse"),b.resolve("Contents").resolve("Eclipse"))).filter(d->Files.isRegularFile(d.resolve(".eclipseproduct"))).distinct().toList();
  }
  public String connect(Path chosen, Path managerDir){
    var dir= Files.isDirectory(chosen) ? chosen : chosen.getParent();
    var found= installs(dir);
    if (found.isEmpty()){ return Messages.noEclipse(dir); }
    if (found.size() > 1){ return Messages.severalEclipses(dir,found); }
    var eclipse= found.getFirst();
    var plugin= JavacTool.reqAppDir(Violation::mustUseLauncher).resolve("eclipsePlugin");
    var fearless= eclipse.resolve("dropins").resolve("fearless");
    Fs.copyFresh(plugin,fearless.resolve("plugins"));
    Fs.writeUtf8(fearless.resolve("manager.info"),Info.print(obj(List.of(field("manager",str(TaggedText.of(managerDir.toString()))),field("baseCache",str(TaggedText.of(Deployed.stdLib("baseCache").toString())))))));
    return Messages.eclipseConnected(eclipse);
  }
  private static void replace(Path file, String text){
    var tmp= file.resolveSibling(file.getFileName()+".tmp");
    Fs.writeUtf8(tmp,text);
    Fs.ofV(()->Files.move(tmp,file,ATOMIC_MOVE));
  }
}

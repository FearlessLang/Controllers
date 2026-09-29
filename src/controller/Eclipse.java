package controller;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import controller.Info.Obj;
import controller.Info.Obj.Field;
import controller.Info.Str;
import fileSupport.JUnitReport;
import fileSupport.LogFiles;
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
  public void publish(String state){ replace(dir.resolve("state.info"),state.getBytes(UTF_8)); }
  /// The JUnit report of main, when the newest unit test log of folder was written since main started.
  public static Optional<String> report(Path folder, String main, Instant since){
    return Facts.retried(()->LogFiles.list(folder).stream().filter(e->e.path().getFileName().toString().startsWith("unit_test_log")).findFirst().filter(e->e.when().isAfter(since)).map(_->JUnitReport.document(JUnitReport.suite(main,folder))));
  }
  public void report(String alias, String report){ replace(dir.resolve(alias).resolve("report.xml"),report.getBytes(UTF_8)); }
  public static void append(Path file, String text){
    Fs.ensureDir(file.getParent());
    Fs.ofV(()->Files.writeString(file,text,CREATE,APPEND));
  }
  static List<Path> installs(Path dir){
    var bases= Stream.concat(Stream.of(dir),Names.list(dir).stream().filter(Files::isDirectory));
    return bases.flatMap(b->Stream.of(Optional.of(b),Names.folder(b,"eclipse"),Names.folder(b,"Contents").flatMap(c->Names.folder(c,"Eclipse"))).flatMap(Optional::stream)).filter(d->Names.child(d,".eclipseproduct").filter(Files::isRegularFile).isPresent()).distinct().toList();
  }
  public String connect(Path chosen, Path managerDir){
    var dir= Files.isDirectory(chosen) ? chosen : chosen.getParent();
    var found= installs(dir);
    if (found.isEmpty()){ return Messages.noEclipse(dir); }
    if (found.size() > 1){ return Messages.severalEclipses(dir,found); }
    var eclipse= found.getFirst();
    var plugin= JavacTool.reqAppDir(Violation::mustUseLauncher).resolve("eclipsePlugin");
    var fearless= eclipse.resolve("dropins").resolve("fearless");
    var plugins= fearless.resolve("plugins");
    var names= Names.list(plugin).stream().map(Path::getFileName).toList();
    names.forEach(n->replace(plugins.resolve(n),Fs.of(()->Files.readAllBytes(plugin.resolve(n)))));
    Names.list(plugins).stream().filter(p->!names.contains(p.getFileName())).forEach(Fs::rmTree);
    replace(fearless.resolve("manager.info"),Info.print(obj(List.of(field("manager",str(TaggedText.of(managerDir.toString()))),field("baseCache",str(TaggedText.of(Deployed.stdLib("baseCache").toString())))))).getBytes(UTF_8));
    return Messages.eclipseConnected(eclipse);
  }
  private static void replace(Path file, byte[] content){
    var tmp= file.resolveSibling(file.getFileName()+".tmp");
    Fs.ensureDir(file.getParent());
    Fs.ofV(()->{ Files.write(tmp,content); Files.move(tmp,file,ATOMIC_MOVE); });
  }
}

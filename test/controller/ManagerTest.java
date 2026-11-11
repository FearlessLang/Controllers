package controller;

import static controller.Errs.same;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.Locale;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tempDirs.RealTempDir;

import controller.Manager.State;
import controller.Registry.Kind;
import coordinator.Coordinator;
import coordinator.MainsInfo;
import fileSupport.LogFiles;
import fileAssociations.Icon;
import mainCoordinator.MakeDemo;
import resources.LocalResources;
import tools.ChildJvm;
import tools.Fs;
import utils.Push;
import tools.PathBytes;
import tools.PathClassLoader;
import userMessages.Report;
import userMessages.UserError;
import userMessages.Violation;

/// The whole manager without a window: messages in, View calls and files out.
/// Compiling writes a fresh cache and every run and compilation is a real child JVM running Child.
final class ManagerTest{
  @RealTempDir static Path classes;
  @BeforeAll static void child(){
    var src= classes.resolve("Child.java");
    Fs.writeUtf8(src, """
public class Child{
  public static void main(String[] a) throws Exception{
    if (a[0].equals("take")){ take(a); return; }
    System.out.print(a[0]+"\\n");
    while (a[0].contains("Chatty")){ System.out.print("chat\\n"); System.out.flush(); }
    System.out.flush();
    Thread.sleep(Long.parseLong(a[2]));
    System.exit(Integer.parseInt(a[1]));
  }
  static void take(String[] a) throws Exception{
    var in= new java.io.BufferedReader(new java.io.InputStreamReader(System.in));
    var json= java.nio.file.Files.readString(tools.PathClassLoader.pathOf(a[1]));
    for (var i= 3; i < a.length; i += 1){
      System.out.print("FearlessTakeFreeExtension:"+a[i].replace(',', ' ')+"\\n");
      System.out.flush();
      var answer= in.readLine();
      if (answer.isEmpty()){ System.out.print("no free extension\\n"); System.exit(1); }
      json= json.replaceFirst("@@[a-z]*", answer);
    }
    java.nio.file.Files.writeString(tools.PathClassLoader.pathOf(a[2]), json);
    System.out.print("compiled\\n");
    System.out.flush();
    System.exit(0);
  }
}
""");
    Fs.runTool("javac", List.of("-d", classes.toString(), "-cp", PathClassLoader.jar().toString(), src.toString()));
  }
  record Fake(Function<Path,Optional<MainsInfo>> read, Map<String,String> infos, List<List<Icon>> associated, List<List<String>> windows, Map<String,List<String>> held, List<String> chosen, List<Path> connected, List<Registry.Grants> granted) implements Manager.Tools{
    /// Writes the cache of the infos of the project; each extension written @@fapp or @@ffile is the autoselected extension of the previous cache, else is asked to the manager, as Coordinator.compile does.
    @Override public ChildJvm compile(Path folder, String alias, Consumer<String> out){
      if (folder.getFileName().toString().equals("nojvm")){ throw new UncheckedIOException(new IOException("Cannot run program \"java\": error=2, No such file or directory")); }
      var mains= infos.getOrDefault(folder.getFileName().toString(), "{}");
      var outDir= folder.resolve(Facts.outDir);
      FakeCache.at(outDir.resolve("_unfinished.txt"), "", 0);
      if (mains.equals("fail")){ Coordinator.writeError(folder, "broken source\n"); return jvm(out, "broken source", "1", "0"); }
      Fs.ofV(() -> Files.deleteIfExists(outDir.resolve("_error.txt")));
      var stamp= FakeCache.after(folder);
      FakeCache.at(outDir.resolve(alias+".jar"), "", stamp);
      Pattern.compile("\\[\"([a-z][a-z0-9_]*\\.[A-Za-z0-9_]+)\", \"([^\"]*)\", \"([^\"]*)\", \"([^\"]*)\"").matcher(mains).results().map(r -> new MainsInfo.Claim(r.group(1), r.group(2), r.group(3), r.group(4), "", "")).distinct()
        .forEach(c -> image(Coordinator.iconFiles(folder, c).getFirst(), 64, 64, "png"));
      if (folder.getFileName().toString().equals("edited")){ touch(folder); }
      if (folder.getFileName().toString().equals("slowcompile")){ return jvm(out, "compiling", "1", "3000"); }
      var previous= outDir.resolve(alias+".json");
      if (Files.exists(previous)){
        for (var r : Pattern.compile("\\\\\"\\\\\", \\\\\"((fapp|ffile)[0-9]{3})\\\\\"\\]").matcher(Fs.readUtf8(previous)).results().toList()){ mains = mains.replaceFirst("@@"+r.group(2), r.group(1)); }
      }
      var template= outDir.resolve("template.txt");
      Fs.writeUtf8(template, FakeCache.json(FakeCache.manifest(mains)));
      var asks= Pattern.compile("@@(fapp|ffile)").matcher(mains).results().map(r -> IntStream.range(0, 1000).mapToObj(n -> r.group(1)+String.format(Locale.ROOT, "%03d", n)).collect(Collectors.joining(","))).toList();
      return ChildJvm.start(Stream.concat(Stream.of("-cp", classes+File.pathSeparator+PathClassLoader.jar(), "Child", "take", PathClassLoader.argOf(template), PathClassLoader.argOf(outDir.resolve(alias+".json"))), asks.stream()).toList(), out);
    }
    @Override public ChildJvm run(Path folder, String main, Optional<Path> input, Registry.Grants grants, List<String> attenuators, Consumer<String> out){
      granted.add(grants);
      return jvm(out, "ran "+main+input.map(i -> " on "+i.getFileName()).orElse("")+(attenuators.isEmpty() ? "" : " under "+String.join(" ", attenuators)), "0", main.contains("Slow") ? "60000" : "0");
    }
    @Override public Coordinator.State check(Path folder){ return FakeCache.check(folder, read); }
    @Override public Path stdLibBase(){ return LocalResources.stLibPath; }
    @Override public Path baseCache(){ return classes.resolve("baseCache"); }
    @Override public void associate(List<Icon> captured, List<Project.Claimant> preloads, Function<String,String> claimedBy){
      captured.forEach(i -> Fs.of(() -> Files.readAllBytes(i.png())));
      preloads.forEach(c -> Fs.of(() -> Files.readAllBytes(c.icon(".png"))));
      associated.add(captured);
      windows.add(preloads.stream().map(c -> c.label()+" "+c.claim().icon()).toList());
      var all= Stream.concat(Stream.of(new Icon(".fearless", null, null)), captured.stream()).toList();
      var chosenByHand= all.stream().map(Icon::extension).filter(chosen::contains).toList();
      if (!chosenByHand.isEmpty()){ throw Violation.associationChosenByHand(chosenByHand.stream().collect(Collectors.toMap(e -> e, _ -> List.of("vim"), (a, _) -> a, LinkedHashMap::new)), claimedBy); }
      var blocked= all.stream().map(Icon::extension).filter(e -> !Icon.system(e) && held.containsKey(e)).collect(Collectors.toMap(e -> e, held::get, (a,_) -> a, LinkedHashMap::new));
      if (!blocked.isEmpty()){ throw new NotOurs(blocked, claimedBy); }
    }
    @Override public void connect(Path eclipse, Path managerDir){ connected.add(eclipse); }
    private static ChildJvm jvm(Consumer<String> out, String... args){
      return ChildJvm.start(Stream.concat(Stream.of("-cp", classes.toString(), "Child"), Stream.of(args)).toList(), out);
    }
  }
  static final class View implements Manager.View{
    final List<String> refused= new ArrayList<>();
    final List<String> told= new ArrayList<>();
    final Map<Path,StringBuilder> outputs= new HashMap<>();
    final List<Path> cleared= new ArrayList<>();
    final List<List<String>> choices= new ArrayList<>();
    final List<String> asked= new ArrayList<>();
    final List<List<String>> offered= new ArrayList<>();
    final List<Path> edited= new ArrayList<>();
    int shown;
    boolean visible= true;
    @Override public void show(){ shown += 1; }
    @Override public void state(State s){}
    @Override public synchronized void output(Path folder, String text){ outputs.computeIfAbsent(folder, _ -> new StringBuilder()).append(text); }
    @Override public synchronized void clear(Path folder){ cleared.add(folder); outputs.put(folder, new StringBuilder()); }
    @Override public void refuse(String text){ shown += 1; refused.add(text); }
    @Override public void tell(String text){ shown += 1; told.add(text); }
    @Override public boolean visible(){ return visible; }
    @Override public void choose(Path file, List<Project.Claimant> cs){ choices.add(Stream.concat(Stream.of(file.getFileName().toString()), cs.stream().map(Project.Claimant::label)).toList()); }
    @Override public void allow(String question, List<String> extensions){ asked.add(question); offered.add(extensions); }
    @Override public void edited(Path file){ edited.add(file); }
    synchronized String out(Path folder){ return outputs.getOrDefault(folder, new StringBuilder()).toString(); }
  }
  private final List<Throwable> failures= Collections.synchronizedList(new ArrayList<>());
  private final List<String> exits= Collections.synchronizedList(new ArrayList<>());
  private final View view= new View();
  private final Map<String,String> infos= new HashMap<>();
  private final List<List<Icon>> associated= Collections.synchronizedList(new ArrayList<>());
  private final List<List<String>> windows= Collections.synchronizedList(new ArrayList<>());
  private final Map<String,List<String>> held= new HashMap<>();
  private final List<String> chosen= new ArrayList<>();
  private final List<Path> connected= new ArrayList<>();
  private final List<Registry.Grants> granted= Collections.synchronizedList(new ArrayList<>());
  private final List<Manager> managers= new ArrayList<>();
  @AfterEach void nothingFailed(){ managers.forEach(Manager::stop); assertEquals(List.of(), failures); Disk.checkedByHandOnWindows(); }
  private Manager manager(Path dir, String... mains){ return manager(dir, view, known(mains)); }
  private static Function<Path,Optional<MainsInfo>> known(String... mains){ return known(List.of(), mains); }
  private static Function<Path,Optional<MainsInfo>> known(List<String> attenuators, String... mains){ return known(attenuators, List.of(), List.of(), mains); }
  private static Function<Path,Optional<MainsInfo>> known(List<String> attenuators, List<String> dataRead, List<String> dataEdit, String... mains){
    var main= new MainsInfo.Main(List.of(), List.of(), List.of());
    var known= Optional.of(new MainsInfo(Stream.of(mains).collect(Collectors.toMap(m -> m, _ -> main)), attenuators, dataRead, dataEdit));
    return _ -> known;
  }
  private Manager manager(Path dir, Function<Path,Optional<MainsInfo>> read){ return manager(dir, view, read); }
  private Manager manager(Path dir, View v, Function<Path,Optional<MainsInfo>> read){
    var exit= new Manager.Exit(){
      @Override public void quit(){ exits.add("quit"); }
      @Override public void reset(){ exits.add("reset"); }
      @Override public void fail(Throwable problem){ failures.add(problem); }
    };
    Fs.ensureDir(dir.resolve("manager"));
    var res= new Manager(dir.resolve("manager"), new Fake(read, infos, associated, windows, held, chosen, connected, granted), v, exit);
    managers.add(res);
    return res;
  }
  private static Path folder(Path dir, String name){
    var res= dir.resolve(name);
    Fs.ensureDir(res);
    return res;
  }
  private static Path registered(Manager m, Path dir, String name){
    var res= folder(dir, name);
    send(m, "Register", TaggedText.of(res.toString()));
    return res;
  }
  private static Path code(Manager m, Path dir, String name){
    var res= dir.resolve(name);
    Fs.writeUtf8(res.resolve("_"+name).resolve("_rank_app.fear"), "Hello: base.Main{ sys -> sys.out.println(\"Hello World!\") }\n");
    Fs.writeUtf8(res.resolve(name+".fearless"), MakeDemo.markerContent);
    FactsTest.backdated(res);
    send(m, "Register", TaggedText.of(res.toString()));
    send(m, "Kind", name, "code");
    return res;
  }
  private static Path data(Path dir){
    var res= dir.resolve("data");
    Fs.writeUtf8(res.resolve("readme.txt"), "hi\n");
    return res;
  }
  /// The message of the lines; for Mount, Attenuate and Run each word is a line, a Mount with no data project is an Unmount,
  /// and a Run naming mains is a SelectMains of those mains, then the Run.
  private static void send(Manager m, String... lines){
    var verb= lines[0];
    if (!List.of("Mount", "Attenuate", "Run").contains(verb) || lines.length < 2){ message(m, List.of(lines)); return; }
    var words= Stream.of(lines).flatMap(l -> Stream.of(l.split(" "))).toList();
    var port= words.getLast().startsWith("Port:") ? List.of(words.getLast()) : List.<String>of();
    var args= words.subList(1, words.size()-port.size());
    if (verb.equals("Mount") && args.size() == 2){ message(m, Push.of(List.of(List.of("Unmount"), args, port))); return; }
    if (verb.equals("Run") && args.size() > 1){ message(m, Push.of("SelectMains", args)); }
    message(m, Push.of(List.of(List.of(verb), verb.equals("Run") ? args.subList(0, 1) : args, port)));
  }
  private static void message(Manager m, List<String> lines){
    m.message(String.join("\n", lines));
    m.settle();
  }
  private static void until(Manager m, Predicate<State> done){
    var end= System.nanoTime()+Duration.ofSeconds(30).toNanos();
    while (true){
      m.settle();
      if (done.test(m.state())){ return; }
      assertTrue(System.nanoTime() < end);
      try{ Thread.sleep(20); }
      catch (InterruptedException e){ throw new AssertionError(e); }
    }
  }
  private static void idle(Manager m){ until(m, s -> s.projects().stream().noneMatch(Project::busy)); }
  private static void compiled(Manager m, String... names){
    for (var n : names){ send(m, "Compile", n); }
    idle(m);
  }
  private static Project project(Manager m, Path folder){ return m.state().of(folder).orElseThrow(); }
  private static List<String> listed(Manager m){ return m.state().projects().stream().map(p -> p.alias()+" "+p.folder()).toList(); }
  private static String written(Path p){ return p.toString().replace("\\", "\\\\"); }
  private static String info(Path dir){ return Fs.readUtf8(dir.resolve("manager").resolve("projects.info")); }
  private static Path edit(Manager m, Path dir, String text){
    var file= dir.resolve("edited.info");
    Fs.writeUtf8(file, text);
    send(m, "EditMetadata", TaggedText.of(file.toString()));
    return file;
  }
  private static void allow(Manager m, Path dir, String... extensions){
    var old= Registry.parse(info(dir), dir.resolve("projects.info"));
    edit(m, dir, Registry.text(new Registry.Content(old.projects(), Stream.concat(old.extensions().stream(), Stream.of(extensions)).toList())));
  }
  @Test void showShowsTheWindow(@RealTempDir Path dir){
    var m= manager(dir);
    send(m, "Show");
    assertEquals(1, view.shown);
    assertEquals(List.of(), listed(m));
    assertEquals("{\n  \"projects\": [],\n  \"systemExtensions\": []\n}\n", info(dir));
  }
  @Test void everyRequestAfterAQuitIsRefused(@RealTempDir Path dir){
    var m= manager(dir);
    var hello= registered(m, dir, "hello");
    send(m, "Quit");
    send(m, "Forget", "hello");
    send(m, "Reset");
    assertEquals(List.of("quit"), exits);
    assertEquals(List.of("hello "+hello), listed(m));
    assertEquals(List.of("Refused request \"Forget\": the manager is quitting.", "Refused request \"Reset\": the manager is quitting."), view.refused);
  }
  @Test void everyRequestAfterAResetIsRefused(@RealTempDir Path dir){
    var m= manager(dir);
    send(m, "Reset");
    send(m, "Quit");
    assertEquals(List.of("reset"), exits);
    assertEquals(List.of("Refused request \"Quit\": the manager is quitting."), view.refused);
  }
  @Test void anInvalidCallIsShownAndAFatalErrorIsFatal(@RealTempDir Path dir){
    var m= manager(dir);
    send(m, "InvalidCall", "Fearless was started with 2 arguments,", "and more.");
    assertEquals(List.of("Fearless was started with 2 arguments,\nand more."), view.refused);
    send(m, "FatalError", "the disk is gone");
    assertEquals(1, failures.size());
    same("A Fearless process reported a failure the manager cannot recover from:\n\nthe disk is gone", failures.getFirst().getMessage());
    failures.clear();
  }
  @Test void aFolderNamedWithANewlineOrNotUnicodeIsRegisteredByItsMessageAndRemembered(@RealTempDir Path dir){
    var m= manager(dir);
    var odd= folder(dir, Fs.isWindows() ? "a\ud800b" : "a\nb");
    send(m, "Register", TaggedText.ofPath(odd));
    assertEquals(List.of("a_b "+odd), listed(m));
    same(Fs.isWindows() ? "[###]\"path\": \"Base16:[###]610000D86200\",[###]" : "[###]\"path\": \"Str:\\\"[###]/a\\\" | \\\"b\\\"\",[###]", info(dir));
    var again= manager(dir);
    again.settle();
    assertEquals(Kind.idle, project(again, odd).kind());
  }
  @Test void aFolderWhoseNameIsNotUnicodeIsRegisteredByItsMessageAndRemembered(@RealTempDir Path dir){
    Assumptions.assumeFalse(Fs.isMac());
    var m= manager(dir);
    var odd= Fs.isWindows() ? dir.resolve("caf\ud800") : dir.resolve(PathBytes.path(new byte[]{'c', 'a', 'f', (byte)0xE9}));
    Fs.ensureDir(odd);
    send(m, "Register", TaggedText.ofPath(odd));
    same("[###]\"path\": \"Base16:[###]\",[###]", info(dir));
    var again= manager(dir);
    again.settle();
    assertEquals(Kind.idle, project(again, odd).kind());
  }
  @Test void registeringASymbolicLinkIsRefused(@RealTempDir Path dir){
    var m= manager(dir);
    var link= Disk.link(dir.resolve("link"), folder(dir, "target"));
    send(m, "Register", TaggedText.of(link.toString()));
    same("Refused request \"Register\" on path\n[###]link\nThe path goes through the symbolic link/junction\n[###]link\nand Fearless does not follow symbolic links/junctions.", view.refused.getLast());
    assertEquals(List.of(), listed(m));
  }
  private static Registry.Entry idleEntry(Path folder){ return new Registry.Entry("", folder, Kind.idle, List.of(), Map.of(), Map.of()); }
  private static String unreachable(Path folder){ return "The project connects to the folder \""+folder+"\".\nThis folder is now unreachable.\nThe project has now been removed."; }
  @Test void aFolderAnEditMetadataNamesThroughALinkedFolderIsAFolderTheManagerCanNotReach(@RealTempDir Path dir){
    var m= manager(dir);
    var real= folder(folder(dir, "real"), "hello");
    var linked= Disk.link(dir.resolve("linked"), dir.resolve("real")).resolve("hello");
    edit(m, dir, Registry.text(new Registry.Content(List.of(idleEntry(linked)), List.of())));
    m.settle();
    assertEquals(List.of(unreachable(linked)), view.refused);
    assertEquals(List.of(), listed(m));
    same("{\n  \"projects\": [],[###]", info(dir));
    assertEquals(List.of(), Names.list(real));
  }
  @Test void aFolderAnEditMetadataNamesInAnotherLetterCaseIsAFolderTheManagerCanNotReach(@RealTempDir Path dir){
    var m= manager(dir);
    var hello= folder(dir, "hello");
    var upper= dir.resolve("HELLO");
    edit(m, dir, Registry.text(new Registry.Content(List.of(idleEntry(upper)), List.of())));
    m.settle();
    assertEquals(List.of(unreachable(upper)), view.refused);
    assertEquals(List.of(), listed(m));
    assertEquals(List.of(), Names.list(hello));
  }
  @Test void anEditPlacingAProjectAroundAnEarlierOneMakesTheLaterOneAFolderTheManagerCanNotReach(@RealTempDir Path dir){
    var m= manager(dir);
    var outer= registered(m, dir, "outer");
    var inner= folder(outer, "inner");
    edit(m, dir, Registry.text(new Registry.Content(List.of(idleEntry(inner), idleEntry(outer)), List.of())));
    m.settle();
    assertEquals(List.of("The project \"outer\" connects to the folder \""+outer+"\".\nThis folder is now unreachable.\nThe project \"outer\" has now been removed."), view.refused);
    assertEquals(List.of("inner "+inner), listed(m));
  }
  @Test void aProjectsInfoWithAProjectInsideAnEarlierOneIsNotFatal(@RealTempDir Path dir){
    var outer= folder(dir, "outer");
    var inner= folder(outer, "inner");
    Fs.writeUtf8(folder(dir, "manager").resolve("projects.info"), Registry.text(new Registry.Content(List.of(idleEntry(outer), idleEntry(inner)), List.of())));
    var m= manager(dir);
    m.settle();
    m.settle();
    assertEquals(List.of("outer "+outer), listed(m));
    assertEquals(List.of(unreachable(inner)), view.refused);
    assertEquals(List.of(), Names.list(inner));
  }
  @Test void anEditOfOneProjectMakesAnotherWhoseFolderBecameALinkAFolderTheManagerCanNotReach(@RealTempDir Path dir){
    var m= manager(dir);
    var a= registered(m, dir, "a");
    var b= registered(m, dir, "b");
    Fs.rmTree(b);
    Disk.link(b, folder(dir, "c"));
    send(m, "Kind", "a", "dataRead");
    m.settle();
    assertEquals(List.of("The project \"b\" connects to the folder \""+b+"\".\nThis folder is now unreachable.\nThe project \"b\" has now been removed."), view.refused);
    assertEquals(List.of("a "+a), listed(m));
  }
  @Test void theFoldersAreWalkedWhenTheListOfTheProjectFoldersChangesAndNotWhenOnlyAKindChanges(@RealTempDir Path dir){
    var m= manager(dir);
    var hello= registered(m, dir, "hello");
    var left= hello.resolve(".fearless_sentinel_left");
    Fs.writeUtf8(left, "");
    send(m, "Kind", "hello", "dataRead");
    assertTrue(Files.exists(left));
    registered(m, dir, "other");
    assertFalse(Files.exists(left));
    assertEquals(List.of(), view.refused);
  }
  @Test void anAutoCheckFindsAProjectWhoseParentFolderBecameALinkUnreachable(@RealTempDir Path dir){
    var m= manager(dir);
    var hello= registered(m, dir.resolve("a"), "hello");
    Fs.ofV(() -> Files.move(dir.resolve("a"), dir.resolve("b")));
    Disk.link(dir.resolve("a"), dir.resolve("b"));
    send(m, "Check", "hello");
    same("The project \"hello\" connects to the folder \""+hello+"\".\nThis folder is now unreachable.\nThe project \"hello\" has now been removed.\nRefused request \"Check\" on project \"hello\".\nNo project is named \"hello\".[###]", String.join("\n", view.refused));
    assertEquals(List.of(), listed(m));
  }
  @Test void aFolderWithALongNameIsNamedByTheFirst64CharactersOfItsFixedSegment(@RealTempDir Path dir){
    var m= manager(dir);
    var folder= registered(m, dir, "Ab-".repeat(30));
    assertEquals(List.of("ab_".repeat(21)+"a "+folder), listed(m));
  }
  @Test void aProjectFolderReplacedByALinkToAnotherProjectWhileTheManagerIsOffIsNotFatal(@RealTempDir Path dir){
    var m= manager(dir);
    var a= registered(m, dir, "a");
    var b= registered(m, dir, "b");
    Fs.rmTree(b);
    Disk.link(b, a);
    var again= manager(dir);
    again.settle();
    assertEquals(List.of(), failures);
  }
  @Test void registeringAFolderMakesItAnIdleProjectNamedByItsMarker(@RealTempDir Path dir){
    var m= manager(dir);
    var hello= folder(dir, "hello");
    Fs.writeUtf8(hello.resolve(Facts.outDir).resolve("hello.built"), "");
    send(m, "Register", TaggedText.of(hello.toString()));
    assertEquals(List.of("hello "+hello), listed(m));
    assertEquals(List.of("hello.fearless"), Names.list(hello).stream().map(p -> p.getFileName().toString()).toList());
    var p= project(m, hello);
    assertEquals(Kind.idle, p.kind());
    assertEquals(Project.State.idle, p.state());
    assertEquals(Optional.of(hello), m.state().selected());
    assertEquals(0, view.shown);
    assertEquals(List.of(), view.refused);
  }
  @Test void aSecondFolderNamedComIsNamedWithTheFirstValidFreeName(@RealTempDir Path dir){
    var m= manager(dir);
    var first= folder(dir.resolve("a"), "com");
    var second= folder(dir.resolve("b"), "com");
    send(m, "Register", TaggedText.of(first.toString()));
    send(m, "Register", TaggedText.of(second.toString()));
    assertEquals(List.of("com "+first, "com10 "+second), listed(m));
    assertTrue(Files.isRegularFile(second.resolve("com10.fearless")));
    assertEquals(List.of(), view.refused);
  }
  @Test void aMarkerRenamedByHandRenamesTheProject(@RealTempDir Path dir){
    var m= manager(dir);
    var hello= registered(m, dir, "hello");
    registered(m, dir, "other");
    Fs.ofV(() -> Files.move(hello.resolve("hello.fearless"), hello.resolve("hi.fearless")));
    send(m, "Show", "hi");
    assertEquals(Optional.of(hello), m.state().selected());
    send(m, "Show", "hello");
    assertEquals(List.of("""
Refused request "Show" on project "hello".
No project is named "hello".
The projects are:
  hi
  other"""), view.refused);
  }
  @Test void aRequestNamingTheOldNameOfAProjectRenamedByHandIsRefused(@RealTempDir Path dir){
    var m= manager(dir);
    var hello= registered(m, dir, "hello");
    Fs.ofV(() -> Files.move(hello.resolve("hello.fearless"), hello.resolve("hi.fearless")));
    send(m, "Kind", "hello", "dataRead");
    assertEquals(List.of("Refused request \"Kind\" on project \"hello\".\nNo project is named \"hello\".\nThe projects are:\n  hi"), view.refused);
    assertEquals(Kind.idle, project(m, hello).kind());
  }
  @Test void aMissingMarkerIsWrittenAgainAtTheNextAutoCheck(@RealTempDir Path dir){
    var m= manager(dir);
    var hello= registered(m, dir, "hello");
    send(m, "Kind", "hello", "dataRead");
    Fs.ofV(() -> Files.delete(hello.resolve("hello.fearless")));
    send(m, "Register", TaggedText.of(hello.toString()));
    assertEquals(MakeDemo.markerContent, Fs.readUtf8(hello.resolve("hello.fearless")));
    assertEquals(List.of("hello "+hello), listed(m));
    assertEquals(Project.State.dataRead, project(m, hello).state());
    assertEquals(List.of(), view.refused);
  }
  @Test void aFolderLaterInProjectsInfoWithTheMarkerNameOfAnEarlierFolderGetsItsMarkerRenamed(@RealTempDir Path dir){
    var m= manager(dir);
    var first= registered(m, dir, "first");
    var second= registered(m, dir, "second");
    Fs.ofV(() -> Files.move(first.resolve("first.fearless"), first.resolve("second.fearless")));
    send(m, "Register", TaggedText.of(first.toString()));
    assertEquals(List.of("second "+first, "second2 "+second), listed(m));
    assertTrue(Files.isRegularFile(first.resolve("second.fearless")));
    assertTrue(Files.isRegularFile(second.resolve("second2.fearless")));
    assertFalse(Files.exists(second.resolve("second.fearless")));
    assertEquals(List.of(), view.refused);
  }
  @Test void aFolderWithSeveralMarkersKeepsItsNameAndIsInvalid(@RealTempDir Path dir){
    var m= manager(dir);
    var several= folder(dir, "several");
    Fs.writeUtf8(several.resolve("beta.fearless"), "");
    Fs.writeUtf8(several.resolve("alpha.fearless"), "");
    send(m, "Register", TaggedText.of(several.toString()));
    send(m, "Kind", "alpha", "dataRead");
    send(m, "Check", "alpha");
    send(m, "Check", "alpha");
    assertEquals(List.of("alpha "+several), listed(m));
    same("More than one .fearless marker file was found in\n[###]several\nA project folder holds exactly one, and its name is the project name: keep only \"alpha.fearless\".", project(m, several).problem().orElseThrow());
    assertTrue(Files.isRegularFile(several.resolve("beta.fearless")));
    assertEquals(List.of(), view.refused);
  }
  @Test void aFileRegistersTheFolderItIsInAndRegisteringAgainSelects(@RealTempDir Path dir){
    var m= manager(dir);
    var hello= folder(dir, "hello");
    var other= folder(dir, "other");
    Fs.writeUtf8(hello.resolve("hello.fearless"), "");
    send(m, "Register", TaggedText.of(hello.resolve("hello.fearless").toString()));
    assertEquals(List.of("hello "+hello), listed(m));
    send(m, "Register", TaggedText.of(other.toString()));
    assertEquals(Optional.of(other), m.state().selected());
    send(m, "Register", TaggedText.of(hello.toString()));
    assertEquals(List.of("hello "+hello, "other "+other), listed(m));
    assertEquals(Optional.of(hello), m.state().selected());
    send(m, "Show", "other");
    assertEquals(Optional.of(other), m.state().selected());
    assertEquals(List.of(), view.refused);
  }
  @Test void aCompileThenARunOfTheOnlyMain(@RealTempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    assertEquals(Project.State.codeOutdated, project(m, hello).state());
    assertEquals(Project.Action.compile, project(m, hello).action());
    compiled(m, "hello");
    same("--- compiling hello ---\ncompiled\n--- compile done ---\n", view.out(hello));
    assertEquals(Project.State.codeCompiled, project(m, hello).state());
    assertEquals(Project.Action.run, project(m, hello).action());
    send(m, "Run", "hello", "hello.Hello");
    idle(m);
    same("""
--- running hello.Hello ---
ran hello.Hello
--- hello.Hello exited with 0 after [###]s ---
""", view.out(hello));
    assertEquals(List.of(hello, hello), view.cleared);
    assertEquals(List.of("hello.Hello"), project(m, hello).entry().mains());
    assertFalse(Files.exists(hello.resolve(Facts.outDir).resolve("_error.txt")));
  }
  @Test void aRunNeverCompiles(@RealTempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    send(m, "Run", "hello", "hello.Hello");
    assertEquals(List.of("Refused request \"Run\" on project \"hello\".\nThe state of project \"hello\" is \"code: compilation cache out of date\": only a project in state \"code: compiled\" runs a main."), view.refused);
    assertEquals("", view.out(hello));
    assertFalse(Files.exists(hello.resolve(Facts.outDir)));
  }
  @Test void aFailingCompileWritesItsErrorAndASucceedingOneDeletesIt(@RealTempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    infos.put("hello", "fail");
    compiled(m, "hello");
    same("--- compiling hello ---\nbroken source\n--- compile failed with 1 ---\n", view.out(hello));
    var error= hello.resolve(Facts.outDir).resolve("_error.txt");
    assertEquals("broken source\n", Fs.readUtf8(error));
    assertEquals(Project.State.codeOutdated, project(m, hello).state());
    assertEquals(Optional.empty(), project(m, hello).problem());
    infos.remove("hello");
    compiled(m, "hello");
    assertFalse(Files.exists(error));
  }
  @Test void compilingAnUpToDateProjectStartsNoJob(@RealTempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    compiled(m, "hello");
    compiled(m, "hello");
    assertEquals(List.of(hello, hello), view.cleared);
    assertEquals(known("hello.Hello").apply(hello), project(m, hello).mains());
  }
  @Test void aBusyProjectRefusesJobsDeleteCacheKindAndForgetThenTerminateEndsTheJob(@RealTempDir Path dir){
    var m= manager(dir, "hello.Slow");
    var hello= code(m, dir, "hello");
    compiled(m, "hello");
    send(m, "Run", "hello", "hello.Slow");
    until(m, _ -> view.out(hello).contains("ran hello.Slow"));
    assertEquals(List.of("hello - hello.Slow"), m.state().running());
    assertEquals(Project.Action.terminate, project(m, hello).action());
    send(m, "Run", "hello", "hello.Slow");
    send(m, "Compile", "hello");
    send(m, "DeleteCache", "hello");
    send(m, "Fix", "hello");
    send(m, "Kind", "hello", "idle");
    send(m, "Forget", "hello");
    send(m, "Terminate", "hello");
    idle(m);
    same("""
--- running hello.Slow ---
ran hello.Slow
--- terminating hello.Slow ---
--- hello.Slow was terminated after [###]s ---
""", view.out(hello));
    assertEquals(List.of(
      "Refused request \"Run\" on project \"hello\".\nMain \"hello.Slow\" is currently running inside project \"hello\".",
      "Refused request \"Compile\" on project \"hello\".\nMain \"hello.Slow\" is currently running inside project \"hello\".",
      "Refused request \"DeleteCache\" on project \"hello\".\nMain \"hello.Slow\" is currently running inside project \"hello\".",
      "Refused request \"Fix\" on project \"hello\".\nMain \"hello.Slow\" is currently running inside project \"hello\".",
      "Refused request to change the kind of project \"hello\" to \"idle\".\nMain \"hello.Slow\" is currently running inside project \"hello\".",
      "Refused request to remove project \"hello\".\nMain \"hello.Slow\" is currently running inside project \"hello\"."), view.refused);
    assertEquals(Kind.code, project(m, hello).kind());
  }
  @Test void aRunningCompilationRefusesTerminate(@RealTempDir Path dir){
    var m= manager(dir);
    var slow= code(m, dir, "slowcompile");
    var other= registered(m, dir, "other");
    send(m, "Compile", "slowcompile");
    until(m, _ -> view.out(slow).contains("compiling\n"));
    send(m, "Show", "other");
    send(m, "Terminate", "slowcompile");
    assertEquals(List.of("Refused request \"Terminate\" on project \"slowcompile\".\nA compilation of project \"slowcompile\" is currently running."), view.refused);
    send(m, "Fix", "slowcompile");
    assertEquals("Refused request \"Fix\" on project \"slowcompile\".\nA compilation of project \"slowcompile\" is currently running.", view.refused.getLast());
    assertEquals(Optional.of(other), m.state().selected());
    idle(m);
    same("[###]compiling\n--- compile failed with 1 ---\n", view.out(slow));
  }
  @Test void theConnectionOfACompilationEndedByAnUnreachableFolderEndsWithTheMarkOfHowTheCompilationEnded(@RealTempDir Path dir){
    var m= manager(dir);
    var slow= code(m, dir, "slowcompile");
    try (var server= port()){
      var compile= follower(server);
      send(m, "Compile", "slowcompile", "Port:"+server.getLocalPort());
      until(m, _ -> view.out(slow).contains("compiling\n"));
      Fs.rmTree(slow);
      send(m, "Check", "slowcompile");
      same("Applied\n--- compiling slowcompile ---\ncompiling\n--- compile [###] ---\n", compile.join());
    }
    catch (IOException e){ throw new UncheckedIOException(e); }
  }
  @Test void compileOfACompiledProjectSaysSo(@RealTempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    compiled(m, "hello");
    send(m, "Compile", "hello");
    assertEquals("The project \"hello\" was already up to date and compiled.\n", view.out(hello));
  }
  @Test void everyCheckOfAnInvalidProjectWritesItsErrorFile(@RealTempDir Path dir){
    var m= manager(dir);
    var hello= code(m, dir, "hello");
    Fs.writeUtf8(hello.resolve("other.fearless"), "");
    send(m, "Check", "hello");
    var error= hello.resolve(Facts.outDir).resolve("_error.txt");
    same("More than one .fearless marker file was found in[###]keep only \"hello.fearless\".[###]", Fs.readUtf8(error));
    Fs.ofV(() -> Files.delete(error));
    send(m, "Check", "hello");
    same("More than one .fearless marker file was found in[###]keep only \"hello.fearless\".[###]", Fs.readUtf8(error));
  }
  @Test void aRequestRightAfterATerminateFindsTheMainEnded(@RealTempDir Path dir){
    var m= manager(dir, "hello.Slow");
    var hello= code(m, dir, "hello");
    compiled(m, "hello");
    send(m, "Run", "hello", "hello.Slow");
    until(m, _ -> view.out(hello).contains("ran hello.Slow"));
    m.message("Terminate\nhello");
    m.message("Check\nhello");
    m.settle();
    assertEquals(List.of(), view.refused);
    assertEquals("The project \"hello\" is valid, and up to date.\n", view.out(hello));
  }
  @Test void terminateEndsTheWholeRunNotOnlyTheCurrentMain(@RealTempDir Path dir){
    var m= manager(dir, "hello.Slow", "hello.Two");
    var hello= code(m, dir, "hello");
    compiled(m, "hello");
    send(m, "Run", "hello", "hello.Slow hello.Two");
    until(m, _ -> view.out(hello).contains("ran hello.Slow"));
    send(m, "Terminate", "hello");
    idle(m);
    assertFalse(view.out(hello).contains("hello.Two"));
  }
  @Test void whatAMainPrintsIsBeforeItsEndMark(@RealTempDir Path dir){
    var m= manager(dir, "hello.SlowChatty");
    var hello= code(m, dir, "hello");
    compiled(m, "hello");
    send(m, "Run", "hello", "hello.SlowChatty");
    until(m, _ -> view.out(hello).contains("chat\nchat\n"));
    send(m, "Terminate", "hello");
    idle(m);
    var out= view.out(hello);
    same("--- hello.SlowChatty was terminated after [###]s ---\n", out.substring(out.lastIndexOf("--- hello.SlowChatty was terminated")));
  }
  @Test void theSelectedMainsRunInTheirOrder(@RealTempDir Path dir){
    var m= manager(dir, "hello.One", "hello.Two", "hello.Three");
    var hello= code(m, dir, "hello");
    compiled(m, "hello");
    assertEquals(List.of(), project(m, hello).entry().mains());
    send(m, "Run", "hello", "hello.Three hello.One");
    idle(m);
    same("""
--- running hello.Three ---
ran hello.Three
--- hello.Three exited with 0 after [###]s ---
--- running hello.One ---
ran hello.One
--- hello.One exited with 0 after [###]s ---
""", view.out(hello));
    assertEquals(List.of("hello.Three", "hello.One"), project(m, hello).entry().mains());
    same("[###]\"mains\": [\"hello.Three\", \"hello.One\"][###]", info(dir));
  }
  @Test void aRunNamingAnUnknownMainIsRefused(@RealTempDir Path dir){
    var m= manager(dir, "hello.One", "hello.Two");
    var hello= code(m, dir, "hello");
    compiled(m, "hello");
    send(m, "Run", "hello", "hello.Nope hello.One");
    assertEquals(List.of("""
Refused request "Run" on project "hello".
The selected mains of project "hello" name "hello.Nope", and "hello.Nope" is not a main of project "hello".
The mains are:
  hello.One
  hello.Two"""), view.refused);
    assertEquals(List.of(hello), view.cleared);
  }
  @Test void forgettingAProjectForgetsItAndLeavesItsFolder(@RealTempDir Path dir){
    var m= manager(dir);
    var hello= registered(m, dir, "hello");
    send(m, "Forget", "hello");
    assertEquals(List.of(), listed(m));
    assertEquals(Optional.empty(), m.state().selected());
    send(m, "Show", "hello");
    assertEquals("Refused request \"Show\" on project \"hello\".\nNo project is named \"hello\".\nNo project is registered.", view.refused.getLast());
    assertTrue(Files.isRegularFile(hello.resolve("hello.fearless")));
  }
  @Test void aRequestNamesARegisteredProject(@RealTempDir Path dir){
    var m= manager(dir);
    var hello= registered(m, dir, "hello");
    send(m, "Compile", "other");
    send(m, "Compile", hello.toString());
    assertEquals(List.of("hello "+hello), listed(m));
    assertEquals("""
Refused request "Compile" on project "other".
No project is named "other".
The projects are:
  hello""", view.refused.getFirst());
    assertTrue(view.refused.get(1).startsWith("Refused request \"Compile\" on project \""+hello+"\".\nNo project is named"));
  }
  @Test void aRequestWhoseResultIsNotAValidProjectsInfoIsRefusedAndChangesNothing(@RealTempDir Path dir){
    var m= manager(dir);
    var hello= code(m, dir, "hello");
    send(m, "Register", TaggedText.of(data(dir).toString()));
    send(m, "Kind", "data", "dataRead");
    var before= info(dir);
    send(m, "Mount", "hello", "hello.One\tTwo data");
    send(m, "Mount", "hello", "hello.Ärger data");
    assertEquals(before, info(dir));
    assertEquals(Map.of(), project(m, hello).entry().mounts());
    assertEquals(2, view.refused.size());
    same("In file: [###]projects.info\n\n[###]The character [Tab 0x09] is outside the safe character set of Fearless[###]", view.refused.get(0));
    same("[###]The character [U+00C4] is outside the safe character set of Fearless[###]", view.refused.get(1));
  }
  @Test void aMessageFileThatIsNotUtf8IsAFatalError(@RealTempDir Path dir){
    var m= manager(dir);
    var messages= folder(dir.resolve("manager"), "messages");
    Fs.ofV(() -> Files.write(messages.resolve("1.msg"), new byte[]{'R', 'u', 'n', '\n', (byte)0xFF}));
    Fs.writeUtf8(messages.resolve("2.msg"), "Show");
    m.drain();
    m.settle();
    same("A message file of the manager folder does not hold what Fearless wrote there:\nits bytes do not form valid UTF-8 text.\n\nThe message file is:\n[###]1.msg", failures.removeFirst().getMessage());
  }
  @Test void aMessageFileLeftWhileTheManagerEndsIsAppliedByTheNextManager(@RealTempDir Path dir){
    var m= manager(dir);
    managers.remove(m);
    m.stop();
    Fs.writeUtf8(folder(dir.resolve("manager"), "messages").resolve("1.msg"), "Show");
    assertThrows(RejectedExecutionException.class, m::drain);
    var next= manager(dir);
    next.drain();
    next.settle();
    assertEquals(1, view.shown);
  }
  @Test void aFolderInsideARegisteredOneIsRefused(@RealTempDir Path dir){
    var m= manager(dir);
    var hello= registered(m, dir, "hello");
    send(m, "Register", TaggedText.of(folder(hello, "inner").toString()));
    same("Refused request \"Register\" on path\n[###]One of the two folders is inside the other[###]", view.refused.getFirst());
    assertEquals(List.of("hello "+hello), listed(m));
  }
  @Test void aLinkToARegisteredFolderOrIntoOneIsRefusedAsALink(@RealTempDir Path dir){
    var m= manager(dir);
    var hello= registered(m, dir, "hello");
    var other= registered(m, dir, "other");
    var link= Disk.link(dir.resolve("link"), hello);
    var inner= Disk.link(dir.resolve("inner"), folder(hello, "inner"));
    send(m, "Register", TaggedText.of(link.toString()));
    send(m, "Register", TaggedText.of(inner.toString()));
    assertEquals(Optional.of(other), m.state().selected());
    assertEquals(List.of("hello "+hello, "other "+other), listed(m));
    same("[###]link\nThe path goes through the symbolic link/junction\n[###]link\nand Fearless does not follow symbolic links/junctions.\n[###]inner\nThe path goes through the symbolic link/junction\n[###]inner\nand Fearless does not follow symbolic links/junctions.", String.join("\n", view.refused));
  }
  @Test void kindsChangeToAnyKind(@RealTempDir Path dir){
    var m= manager(dir);
    var data= data(dir);
    send(m, "Register", TaggedText.of(data.toString()));
    assertEquals(Kind.idle, project(m, data).kind());
    send(m, "Kind", "data", "dataRead");
    send(m, "Kind", "data", "code");
    assertEquals(Kind.code, project(m, data).kind());
    send(m, "Kind", "data", "dataEdit");
    send(m, "Kind", "data", "dataEdit");
    assertEquals(Kind.dataEdit, project(m, data).kind());
    send(m, "Kind", "data", "idle");
    assertEquals(Kind.idle, project(m, data).kind());
    assertEquals(List.of(), view.refused);
    assertEquals(List.of(), view.cleared);
  }
  @Test void onlyACodeProjectCompilesAndRunsAndAnyProjectIsChecked(@RealTempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var data= data(dir);
    send(m, "Register", TaggedText.of(data.toString()));
    send(m, "Compile", "data");
    send(m, "Run", "data", "data.Main");
    assertEquals(List.of(
      "Refused request \"Compile\" on project \"data\".\nThe kind of project \"data\" is \"idle\": only a project of kind \"code\" accepts request \"Compile\".",
      "Refused request \"Run\" on project \"data\".\nThe kind of project \"data\" is \"idle\": only a project of kind \"code\" accepts request \"Run\"."), view.refused);
    assertEquals("", view.out(data));
    send(m, "Check", "data");
    assertEquals("The project \"data\" is valid.\n", view.out(data));
    Fs.writeUtf8(data.resolve("Bad Name.txt"), "");
    send(m, "Check", "data");
    assertEquals("The project \"data\" is valid.\n", view.out(data));
    var checkError= data.resolve(Facts.outDir).resolve("_error.txt");
    assertFalse(Files.exists(checkError));
    assertEquals(Project.State.idle, project(m, data).state());
    send(m, "Kind", "data", "dataRead");
    send(m, "Check", "data");
    same("[###]Bad Name.txt[###]", view.out(data));
    same("[###]Bad Name.txt[###]", Fs.readUtf8(checkError));
    assertEquals(Project.State.dataReadInvalid, project(m, data).state());
    send(m, "Kind", "data", "dataEdit");
    assertEquals(Project.State.dataEditInvalid, project(m, data).state());
    assertEquals(Project.Action.check, project(m, data).action());
    Fs.ofV(() -> Files.delete(data.resolve("Bad Name.txt")));
    send(m, "Check", "data");
    assertEquals("The project \"data\" is valid.\n", view.out(data));
    var hello= code(m, dir, "hello");
    send(m, "Check", "hello");
    assertEquals("The compilation cache of this project is out of date.\n", view.out(hello));
  }
  @Test void aMountOfAProjectThatIsNotRegisteredIsRefused(@RealTempDir Path dir){
    var m= manager(dir);
    code(m, dir, "hello");
    var before= info(dir);
    send(m, "Mount", "hello", "hello.Pub diary");
    assertEquals(List.of("Refused request \"Mount\" on project \"hello\".\nNo project is named \"diary\".\nThe projects are:\n  hello"), view.refused);
    assertEquals(before, info(dir));
  }
  @Test void mountsAreEditedByRequestAndABrokenMountOrADataReadTypeWithNoMountStopsTheRun(@RealTempDir Path dir){
    var m= manager(dir, known(List.of(), List.of("hello.Pub"), List.of("hello.Diary"), "hello.Hello"));
    var hello= code(m, dir, "hello");
    compiled(m, "hello");
    var data= data(dir);
    send(m, "Register", TaggedText.of(data.toString()));
    send(m, "Kind", "data", "dataRead");
    send(m, "Mount", "hello", "hello.Diary data");
    send(m, "Mount", "hello", "hello.Old data");
    assertEquals(Map.of("hello.Diary", "data", "hello.Old", "data"), project(m, hello).entry().mounts());
    send(m, "Mount", "hello", "Lower data");
    same("[###]\"Lower\" in \"mounts\" is not a Fearless DataRead type name[###]", view.refused.getLast());
    assertEquals(Project.State.codeCompiled, project(m, hello).state());
    send(m, "Run", "hello", "hello.Hello");
    assertEquals("""
Refused request "Run" on project "hello".
Project "hello" runs only with a data project mounted on each DataRead type of the project, and with no broken mount:
  No data project is mounted on "hello.Pub".
  The mount of "data" on "hello.Diary" is broken: the kind of project "data" is "dataRead", and "hello.Diary" is a DataEdit type: only a project of kind "dataEdit" is mounted on a DataEdit type.
  The mount of "data" on "hello.Old" is broken: "hello.Old" is not a DataRead type of project "hello".""", view.refused.getLast());
    send(m, "Kind", "data", "idle");
    send(m, "Mount", "hello", "hello.Old");
    registered(m, dir, "nope");
    send(m, "Mount", "hello", "hello.Pub nope");
    send(m, "Forget", "nope");
    send(m, "Run", "hello", "hello.Hello");
    assertEquals("""
Refused request "Run" on project "hello".
Project "hello" runs only with a data project mounted on each DataRead type of the project, and with no broken mount:
  The mount of "data" on "hello.Diary" is broken: the kind of project "data" is "idle", and only a project of kind "dataRead" or "dataEdit" is a data project.
  The mount of "nope" on "hello.Pub" is broken: no project "nope" is registered.""", view.refused.getLast());
    assertEquals(3, view.refused.size());
    send(m, "Kind", "data", "dataEdit");
    send(m, "Mount", "hello", "hello.Pub data");
    send(m, "Run", "hello", "hello.Hello");
    idle(m);
    assertEquals(3, view.refused.size());
    var folder= project(m, data).folder();
    assertEquals(List.of(new Registry.Grants(Map.of("hello.Pub", folder, "hello.Diary", folder), Map.of("hello.Pub", m.lockFile(folder), "hello.Diary", m.lockFile(folder)))), granted);
  }
  @Test void attenuatorsAreSetByRequestAndARunStartsEachMainUnderItsOwn(@RealTempDir Path dir){
    var m= manager(dir, known(List.of("hello.NoGui", "hello.NoNet"), "hello.One", "hello.Two"));
    var hello= code(m, dir, "hello");
    compiled(m, "hello");
    send(m, "Attenuate", "hello", "hello.One hello.NoNet hello.NoGui");
    send(m, "Attenuate", "hello", "hello.Two hello.NoGui");
    assertEquals(Map.of("hello.One", List.of("hello.NoNet", "hello.NoGui"), "hello.Two", List.of("hello.NoGui")), project(m, hello).entry().attenuators());
    send(m, "Attenuate", "hello", "hello.Two");
    assertEquals(Map.of("hello.One", List.of("hello.NoNet", "hello.NoGui")), project(m, hello).entry().attenuators());
    send(m, "Run", "hello", "hello.One hello.Two");
    idle(m);
    same("""
--- running hello.One ---
ran hello.One under hello.NoNet hello.NoGui
--- hello.One exited with 0 after [###]s ---
--- running hello.Two ---
ran hello.Two
--- hello.Two exited with 0 after [###]s ---
""", view.out(hello));
    send(m, "Attenuate", "hello", "hello.One hello.NoGui hello.NoGui");
    assertEquals(Map.of("hello.One", List.of("hello.NoGui", "hello.NoGui")), project(m, hello).entry().attenuators());
    send(m, "Attenuate", "hello", "hello.One hello.NoNet hello.NoGui");
    send(m, "Attenuate", "hello", "hello.One NoGui");
    same("[###]\"NoGui\" in \"attenuators\".\"hello.One\" is not a Fearless attenuator name[###]", view.refused.getLast());
    var data= data(dir);
    send(m, "Register", TaggedText.of(data.toString()));
    send(m, "Attenuate", "data", "hello.One hello.NoGui");
    assertEquals(Map.of("hello.One", List.of("hello.NoGui")), project(m, data).entry().attenuators());
    assertEquals(1, view.refused.size());
    assertEquals(Map.of("hello.One", List.of("hello.NoNet", "hello.NoGui")), project(m, hello).entry().attenuators());
  }
  @Test void aRunIsRefusedWhileAnAttenuatedMainOrAnAppliedAttenuatorIsNotOfTheProject(@RealTempDir Path dir){
    var m= manager(dir, known(List.of("hello.NoGui"), "hello.One", "hello.Two"));
    var hello= code(m, dir, "hello");
    compiled(m, "hello");
    send(m, "Attenuate", "hello", "hello.Old hello.NoGui");
    send(m, "Run", "hello", "hello.One");
    assertEquals("""
Refused request "Run" on project "hello".
"attenuators" of project "hello" names "hello.Old", and "hello.Old" is not a main of project "hello".
The mains are:
  hello.One
  hello.Two""", view.refused.getLast());
    send(m, "Attenuate", "hello", "hello.Old");
    send(m, "Attenuate", "hello", "hello.Two hello.NoGui hello.NoNet");
    send(m, "Run", "hello", "hello.One");
    assertEquals("""
Refused request "Run" on project "hello".
Main "hello.Two" is attenuated by "hello.NoNet", and "hello.NoNet" is not an attenuator of project "hello".
The attenuators are:
  hello.NoGui""", view.refused.getLast());
    assertEquals(2, view.refused.size());
    same("--- compiling hello ---\ncompiled\n--- compile done ---\n", view.out(hello));
    send(m, "Attenuate", "hello", "hello.Two hello.NoGui");
    send(m, "Run", "hello", "hello.One");
    idle(m);
    same("--- running hello.One ---\nran hello.One\n--- hello.One exited with 0 after [###]s ---\n", view.out(hello));
  }
  @Test void aMountOfADataProjectWithInvalidContentStopsTheRun(@RealTempDir Path dir){
    var m= manager(dir, known(List.of(), List.of("hello.Pub"), List.of(), "hello.Hello"));
    var hello= code(m, dir, "hello");
    compiled(m, "hello");
    var data= data(dir);
    send(m, "Register", TaggedText.of(data.toString()));
    send(m, "Kind", "data", "dataRead");
    send(m, "Mount", "hello", "hello.Pub data");
    Fs.writeUtf8(data.resolve("Bad Name.txt"), "");
    send(m, "Run", "hello", "hello.Hello");
    same("""
Refused request "Run" on project "hello".
Project "hello" runs only with a data project mounted on each DataRead type of the project, and with no broken mount:
  The mount of "data" on "hello.Pub" is broken: the content of project "data" is invalid:
[###]Bad Name.txt[###]""", view.refused.getLast());
    assertEquals(1, view.refused.size());
    Fs.ofV(() -> Files.delete(data.resolve("Bad Name.txt")));
    send(m, "Run", "hello", "hello.Hello");
    idle(m);
    assertEquals(1, view.refused.size());
    same("--- running hello.Hello ---\nran hello.Hello\n[###]", view.out(hello));
  }
  @Test void aLockFileOfNoRegisteredProjectIsDeletedWhenTheManagerStarts(@RealTempDir Path dir){
    var m= manager(dir);
    var data= data(dir);
    send(m, "Register", TaggedText.of(data.toString()));
    send(m, "Kind", "data", "dataRead");
    var kept= m.lockFile(project(m, data).folder());
    var inner= Manager.inner(kept);
    var stray= kept.resolveSibling("0".repeat(64)+".lock");
    Stream.of(inner, stray).forEach(p -> Fs.writeUtf8(p, ""));
    var again= manager(dir);
    again.settle();
    assertEquals(List.of(true, true, false), Stream.of(kept, inner, stray).map(Files::exists).toList());
  }
  @Test void eachDataProjectHasALockFileOfItsOwnAlsoWhenItsFolderNameIsNotUnicode(@RealTempDir Path dir){
    Assumptions.assumeFalse(Fs.isMac());
    var m= manager(dir);
    var folders= Stream.of(0xE9, 0xE8).map(b -> Fs.isWindows() ? dir.resolve("data"+(char)(0xD800+b)) : dir.resolve(PathBytes.path(new byte[]{'d', 'a', 't', 'a', (byte)(int)b}))).toList();
    folders.forEach(f -> Fs.writeUtf8(f.resolve("readme.txt"), "hi\n"));
    folders.forEach(f -> send(m, "Register", TaggedText.ofPath(f)));
    m.state().projects().forEach(p -> send(m, "Kind", p.alias(), "dataRead"));
    var locks= m.state().projects().stream().map(p -> m.lockFile(p.folder())).distinct().toList();
    assertEquals(2, m.state().projects().size());
    assertEquals(2, locks.size(), locks.toString());
  }
  @Test void theProblemOfADataProjectNamesTheZipWhoseEntryNameIsNotUtf8(@RealTempDir Path dir){
    var m= manager(dir);
    var data= data(dir);
    var bytes= new ByteArrayOutputStream();
    Fs.ofV(() -> { try (var z= new ZipOutputStream(bytes, StandardCharsets.ISO_8859_1)){ z.putNextEntry(new ZipEntry("café.txt")); z.write('h'); } });
    Fs.ofV(() -> Files.write(data.resolve("pics.zip"), bytes.toByteArray()));
    send(m, "Register", TaggedText.of(data.toString()));
    send(m, "Kind", "data", "dataRead");
    assertEquals(Project.State.dataReadInvalid, project(m, data).state());
    same("""
Root: [###]
Path: "pics.zip"

An entry name of this zip is not UTF-8 text.
Tools read such a name in different encodings, so the same entry may get a different name on each system.[###]""", project(m, data).problem().orElseThrow());
  }
  @Test void aDataProjectThatIsGoneEndsTheRunningMainsMountingItThenIsForgotten(@RealTempDir Path dir){
    var m= manager(dir, known(List.of(), List.of("hello.Pub"), List.of(), "hello.Slow"));
    var hello= code(m, dir, "hello");
    var data= data(dir);
    send(m, "Register", TaggedText.of(data.toString()));
    send(m, "Kind", "data", "dataRead");
    send(m, "Mount", "hello", "hello.Pub data");
    compiled(m, "hello");
    send(m, "Run", "hello", "hello.Slow");
    until(m, _ -> view.out(hello).contains("ran hello.Slow"));
    Fs.rmTree(data);
    send(m, "Check", "data");
    assertEquals(List.of("hello "+hello), listed(m));
    assertFalse(project(m, hello).busy());
    same("[###]ran hello.Slow\n--- terminating hello.Slow ---\n--- hello.Slow was terminated[###]", view.out(hello));
    assertEquals("The project \"data\" connects to the folder \""+data+"\".\nThis folder is now unreachable.\nThe project \"data\" has now been removed.", view.refused.getFirst());
  }
  @Test void anUnreachableProjectIsForgottenWhileAProjectWhoseFolderBecameALinkRunsAMain(@RealTempDir Path dir) throws IOException{
    var m= manager(dir, "hello.Slow");
    var hello= code(m, dir.resolve("a"), "hello");
    var data= data(dir);
    send(m, "Register", TaggedText.of(data.toString()));
    compiled(m, "hello");
    send(m, "Run", "hello", "hello.Slow");
    until(m, _ -> view.out(hello).contains("ran hello.Slow"));
    Files.move(dir.resolve("a"), dir.resolve("b"));
    Disk.link(dir.resolve("a"), dir.resolve("b"));
    Fs.rmTree(data);
    send(m, "Check", "data");
    same("[###]The project \"data\" connects to the folder [###]\nThis folder is now unreachable.\nThe project \"data\" has now been removed.\nRefused request \"Check\" on project \"data\".\nNo project is named \"data\".[###]", String.join("\n", view.refused));
    assertEquals(List.of("hello "+hello), listed(m));
    send(m, "Terminate", "hello");
    assertEquals(List.of(), failures);
  }
  @Test void anEditAddingAProjectWhoseCacheCanNotBeDeletedSaysSoInTheOutput(@RealTempDir Path dir){
    var m= manager(dir);
    var hello= registered(m, dir, "hello");
    var text= info(dir);
    send(m, "Forget", "hello");
    var cache= folder(hello, ".fearless_out");
    Fs.writeUtf8(cache.resolve("a.built"), "");
    Disk.noWrite(cache);
    try{
      edit(m, dir, text);
      assertEquals(List.of("hello "+hello), listed(m));
      same("--- the compilation cache is not deleted: access denied: [###]\n---\n", view.out(hello));
    }
    finally{ Disk.allow(cache); }
  }
  @Test void aFolderWhoseMarkerCanNotBeRenamedGetsNoNameAndIsForgotten(@RealTempDir Path dir){
    var m= manager(dir);
    var hello= registered(m, dir, "hello");
    Fs.ofV(() -> Files.move(hello.resolve("hello.fearless"), hello.resolve("1bad.fearless")));
    Disk.noWrite(hello);
    try{
      send(m, "Check", "hello");
      assertEquals(List.of(), listed(m));
      assertEquals(List.of(), failures);
    }
    finally{ Disk.allow(hello); }
  }
  @Test void anEditAddingTwoProjectsTheFirstWithAMarkerThatCanNotBeWrittenIsNotFatal(@RealTempDir Path dir){
    var m= manager(dir);
    var hello= registered(m, dir, "hello");
    var text= info(dir);
    var locked= folder(dir, "locked");
    var other= folder(dir, "other");
    Disk.noWrite(locked);
    try{
      edit(m, dir, text.replace("  ]", "  ,\n    {\"path\": "+quoted(locked)+", \"kind\": \"idle\"},\n    {\"path\": "+quoted(other)+", \"kind\": \"idle\"}\n  ]"));
      until(m, s -> s.projects().size() == 2);
      assertEquals(List.of("hello "+hello, "other "+other), listed(m));
      assertEquals(List.of(), failures);
    }
    finally{ Disk.allow(locked); }
  }
  private static String quoted(Path p){ return "\""+TaggedText.of(p.toString()).replace("\\", "\\\\").replace("\"", "\\\"")+"\""; }
  @Test void aKindChangeOfADataProjectMountedOnARunningMainIsRefused(@RealTempDir Path dir){
    var m= manager(dir, known(List.of(), List.of("hello.Pub"), List.of(), "hello.Slow"));
    var hello= code(m, dir, "hello");
    send(m, "Register", TaggedText.of(data(dir).toString()));
    send(m, "Kind", "data", "dataEdit");
    send(m, "Mount", "hello", "hello.Pub data");
    compiled(m, "hello");
    send(m, "Run", "hello", "hello.Slow");
    until(m, _ -> view.out(hello).contains("ran hello.Slow"));
    send(m, "Kind", "data", "dataRead");
    assertEquals(List.of("Refused request to change the data projects mounted on the DataRead types of project \"hello\".\nMain \"hello.Slow\" is currently running inside project \"hello\".\nThe data projects of a main stay the same while the main runs."), view.refused);
    send(m, "Terminate", "hello");
    idle(m);
  }
  @Test void aRunningMainKeepsTheDataProjectsOfItsMountsUntilItEnds(@RealTempDir Path dir){
    var m= manager(dir, known(List.of(), List.of("hello.Pub"), List.of("hello.Notes"), "hello.Slow"));
    var hello= code(m, dir, "hello");
    var data= data(dir);
    var other= registered(m, dir, "other");
    send(m, "Register", TaggedText.of(data.toString()));
    send(m, "Kind", "data", "dataEdit");
    send(m, "Mount", "hello", "hello.Notes data");
    send(m, "Mount", "hello", "hello.Pub data");
    compiled(m, "hello");
    send(m, "Run", "hello", "hello.Slow");
    until(m, _ -> view.out(hello).contains("ran hello.Slow"));
    var folder= project(m, data).folder();
    assertEquals(List.of(new Registry.Grants(Map.of("hello.Pub", folder, "hello.Notes", folder), Map.of("hello.Pub", m.lockFile(folder), "hello.Notes", m.lockFile(folder)))), granted);
    var before= info(dir);
    send(m, "Mount", "hello", "hello.Old other");
    send(m, "Kind", "data", "dataRead");
    send(m, "Forget", "data");
    edit(m, dir, before.replace("\"hello.Pub\"", "\"hello.Journal\""));
    assertEquals(before, info(dir));
    assertEquals(Collections.nCopies(4, "Refused request to change the data projects mounted on the DataRead types of project \"hello\".\nMain \"hello.Slow\" is currently running inside project \"hello\".\nThe data projects of a main stay the same while the main runs."), view.refused);
    send(m, "Mount", "hello", "hello.Notes data");
    send(m, "Kind", "other", "dataRead");
    send(m, "Check", "data");
    assertEquals(4, view.refused.size());
    send(m, "Terminate", "hello");
    idle(m);
    send(m, "Mount", "hello", "hello.Old other");
    assertEquals(Map.of("hello.Notes", "data", "hello.Pub", "data", "hello.Old", "other"), project(m, hello).entry().mounts());
    assertEquals(Kind.dataRead, project(m, other).kind());
  }
  @Test void anEditChangingThePathOfAProjectRemovesItAndAddsAProjectWithNoCache(@RealTempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    compiled(m, "hello");
    var other= folder(dir, "other");
    Fs.writeUtf8(other.resolve(Facts.outDir).resolve("other.built"), "");
    edit(m, dir, info(dir).replace(written(hello), written(other)));
    assertEquals(List.of("other "+other), listed(m));
    assertEquals(Kind.code, project(m, other).kind());
    assertFalse(Files.exists(other.resolve(Facts.outDir).resolve("other.built")));
    assertTrue(Files.exists(hello.resolve(Facts.outDir).resolve("hello.json")));
  }
  @Test void anEditChangingThePathOfAProjectWithARunningMainIsRefused(@RealTempDir Path dir){
    var m= manager(dir, "hello.Slow");
    var hello= code(m, dir, "hello");
    compiled(m, "hello");
    send(m, "Run", "hello", "hello.Slow");
    until(m, _ -> view.out(hello).contains("ran hello.Slow"));
    var before= info(dir);
    edit(m, dir, before.replace(written(hello), written(folder(dir, "other"))));
    assertEquals(before, info(dir));
    same("Refused request to remove project \"hello\".\nMain \"hello.Slow\" is currently running inside project \"hello\".", view.refused.getLast());
    send(m, "Terminate", "hello");
    idle(m);
  }
  @Test void anEditKeepsTheTextOfItsFile(@RealTempDir Path dir){
    var m= manager(dir);
    var text= "{ \"systemExtensions\": [ ],\n  \"projects\": [ ] }\n";
    edit(m, dir, text);
    assertEquals(List.of(), view.refused);
    assertEquals(text, info(dir));
  }
  @Test void theMetadataIsEditedWholeOrNotAtAll(@RealTempDir Path dir){
    var m= manager(dir);
    var data= data(dir);
    send(m, "Register", TaggedText.of(data.toString()));
    edit(m, dir, "{\"projects\": [{\"path\": \"Str:\\\"nowhere\\\"\"}], \"systemExtensions\": []}");
    same("In file: [###]edited.info[###]\"path\" must be an absolute path[###]", view.refused.getLast());
    assertEquals(List.of(), view.edited);
    var good= edit(m, dir, Registry.text(new Registry.Content(List.of(project(m, data).entry().withKind(Kind.dataEdit)), List.of())));
    assertEquals(List.of(good), view.edited);
    assertEquals(Kind.dataEdit, project(m, data).kind());
    edit(m, dir, "{\"projects\": [], \"systemExtensions\": []}");
    assertEquals(List.of(), listed(m));
    send(m, "EditMetadata", TaggedText.of(dir.resolve("none.info").toString()));
    same("Refused request \"EditMetadata\" on path\n[###]none.info\nNo file is at that path.", view.refused.getLast());
  }
  /// Forget, Kind, Mount and Attenuate are EditMetadata with projects.info changed in one place: the same outcome, down to the refusal.
  private record Twin(Manager m, View view, Path dir){}
  private Twin twin(Path dir, String name, String... mains){
    var v= new View();
    return new Twin(manager(dir.resolve(name), v, known(mains)), v, dir.resolve(name));
  }
  private static List<String> outcome(Twin t){
    var projects= t.m.state().projects().stream().map(p -> p.alias()+" "+p.kind()+" "+p.state()+" "+p.problem()+" "+p.job());
    var all= Stream.of(Stream.of(info(t.dir), t.m.state().selected().toString()), projects, t.view.refused.stream()).flatMap(s -> s);
    return all.map(s -> masked(s, t.dir.toString())).toList();
  }
  private static String masked(String s, String dir){ return s.replace(dir.replace("\\", "\\\\"), "DIR").replace(dir.replace("\\", "/"), "DIR").replace(dir, "DIR"); }
  private void sameAsEditMetadata(Path dir, String[] mains, BiConsumer<Manager,Path> setup, Function<Registry.Content,String> edited, String... verb){
    var a= twin(dir, "a", mains);
    var b= twin(dir, "b", mains);
    setup.accept(a.m, a.dir);
    setup.accept(b.m, b.dir);
    a.view.refused.clear();
    b.view.refused.clear();
    send(a.m, verb);
    var managerDir= b.dir.resolve("manager");
    edit(b.m, b.dir, edited.apply(Registry.inferred(Registry.parse(info(b.dir), managerDir.resolve("projects.info")), new Registry.Content(List.of(), List.of()), Set.of())));
    var outcomes= List.of(outcome(a), outcome(b));
    for (var t : List.of(a, b)){
      t.m.state().projects().stream().filter(Project::busy).forEach(p -> send(t.m, "Terminate", p.alias()));
      idle(t.m);
    }
    assertEquals(outcomes.get(0), outcomes.get(1).stream().map(ManagerTest::asProjectsInfo).toList());
  }
  private static String asProjectsInfo(String s){
    return s.replace("DIR/edited.info", "DIR/manager/projects.info").replace("DIR\\edited.info", "DIR\\manager\\projects.info").replace("DIR\\\\edited.info", "DIR\\\\manager\\\\projects.info");
  }
  private static String without(Registry.Content c, String name){ return Registry.text(new Registry.Content(c.projects().stream().filter(e -> !e.alias().equals(name)).toList(), c.extensions())); }
  private static String with(Registry.Content c, String name, UnaryOperator<Registry.Entry> op){ return Registry.text(new Registry.Content(c.projects().stream().map(e -> e.alias().equals(name) ? op.apply(e) : e).toList(), c.extensions())); }
  private static void withData(Manager m, Path dir){
    code(m, dir, "hello");
    send(m, "Register", TaggedText.of(data(dir).toString()));
    send(m, "Kind", "data", "dataEdit");
  }
  private static void running(Manager m, Path dir){
    withData(m, dir);
    compiled(m, "hello");
    send(m, "Run", "hello", "hello.Slow");
    until(m, s -> s.running().equals(List.of("hello - hello.Slow")));
  }
  private static final String[] none= {};
  private static final String[] slow= {"hello.Slow"};
  @Test void forgetIsEditMetadataWithTheProjectRemoved(@RealTempDir Path dir){
    sameAsEditMetadata(dir, none, (m, d) -> registered(m, d, "hello"), c -> without(c, "hello"), "Forget", "hello");
  }
  @Test void forgetOfABusyProjectIsEditMetadataWithTheProjectRemoved(@RealTempDir Path dir){
    sameAsEditMetadata(dir, slow, ManagerTest::running, c -> without(c, "hello"), "Forget", "hello");
  }
  @Test void selectMainsIsEditMetadataWithTheSelectedMainsReplaced(@RealTempDir Path dir){
    sameAsEditMetadata(dir, none, ManagerTest::withData, c -> with(c, "hello", e -> e.withMains(List.of("hello.Old", "hello.Hello"))), "SelectMains", "hello", "hello.Old", "hello.Hello");
  }
  @Test void selectMainsNamingNoMainIsEditMetadataWithTheSelectedMainsRemoved(@RealTempDir Path dir){
    sameAsEditMetadata(dir, none, (m, d) -> { withData(m, d); message(m, List.of("SelectMains", "hello", "hello.Hello")); }, c -> with(c, "hello", e -> e.withMains(List.of())), "SelectMains", "hello");
  }
  @Test void unmountIsEditMetadataWithTheMountRemoved(@RealTempDir Path dir){
    sameAsEditMetadata(dir, none, (m, d) -> { withData(m, d); send(m, "Mount", "hello", "hello.Pub data"); }, c -> with(c, "hello", e -> e.withMounts(Map.of())), "Unmount", "hello", "hello.Pub");
  }
  @Test void aRunOfAProjectWithNoSelectedMainIsRefused(@RealTempDir Path dir){
    var m= manager(dir, "hello.One", "hello.Two");
    code(m, dir, "hello");
    compiled(m, "hello");
    message(m, List.of("Run", "hello"));
    assertEquals(List.of("Refused request \"Run\" on project \"hello\".\nProject \"hello\" has no selected main.\nThe mains are:\n  hello.One\n  hello.Two"), view.refused);
  }
  @Test void aProjectWhoseFolderIsGoneWhileItsMainRunsEndsTheMainAndIsForgotten(@RealTempDir Path dir){
    var m= manager(dir, "hello.Slow");
    var hello= code(m, dir, "hello");
    compiled(m, "hello");
    send(m, "Run", "hello", "hello.Slow");
    until(m, _ -> view.out(hello).contains("ran hello.Slow"));
    Fs.rmTree(hello);
    send(m, "Check", "hello");
    until(m, s -> s.projects().isEmpty());
    assertEquals("The project \"hello\" connects to the folder \""+hello+"\".\nThis folder is now unreachable.\nThe project \"hello\" has now been removed.", view.refused.getFirst());
    same("[###]--- terminating hello.Slow ---\n--- hello.Slow was terminated after [###]s ---\n", view.out(hello));
  }
  @Test void theLogsOfARunningMainShowAtTheNextAutoCheck(@RealTempDir Path dir){
    var m= manager(dir, "hello.Slow");
    var hello= code(m, dir, "hello");
    compiled(m, "hello");
    send(m, "Run", "hello", "hello.Slow");
    until(m, _ -> view.out(hello).contains("ran hello.Slow"));
    Fs.writeUtf8(hello.resolve(".out").resolve("logs").resolve("_base").resolve("unit_test_log$29991231_000000_000Z.log"), "<testcase classname=\"A\" name=\"A at line: 1\"></testcase>\n");
    send(m, "Show", "hello");
    assertTrue(project(m, hello).busy());
    assertEquals(1, project(m, hello).facts().logs().size());
    same("[###]<testcase classname=\"A\" name=\"A at line: 1\"></testcase>[###]", project(m, hello).facts().tests().getLast().xml());
    assertEquals(List.of("hello.Slow"), project(m, hello).ran().stream().map(Project.Ran::main).toList());
    send(m, "Terminate", "hello");
  }
  @Test void kindFromIdleIsEditMetadataWithTheKindReplaced(@RealTempDir Path dir){
    sameAsEditMetadata(dir, none, (m, d) -> registered(m, d, "data"), c -> with(c, "data", e -> e.withKind(Kind.dataRead)), "Kind", "data", "dataRead");
  }
  @Test void kindNotFromIdleIsEditMetadataWithTheKindReplaced(@RealTempDir Path dir){
    sameAsEditMetadata(dir, none, (m, d) -> { registered(m, d, "data"); send(m, "Kind", "data", "dataRead"); }, c -> with(c, "data", e -> e.withKind(Kind.code)), "Kind", "data", "code");
  }
  @Test void kindOfABusyProjectIsEditMetadataWithTheKindReplaced(@RealTempDir Path dir){
    sameAsEditMetadata(dir, slow, ManagerTest::running, c -> with(c, "hello", e -> e.withKind(Kind.idle)), "Kind", "hello", "idle");
  }
  @Test void mountIsEditMetadataWithTheMountReplaced(@RealTempDir Path dir){
    sameAsEditMetadata(dir, none, ManagerTest::withData, c -> with(c, "hello", e -> e.withMounts(Map.of("hello.Diary", "data"))), "Mount", "hello", "hello.Diary data");
  }
  @Test void mountWithNoDataProjectIsEditMetadataWithTheMountRemoved(@RealTempDir Path dir){
    sameAsEditMetadata(dir, none, (m, d) -> { withData(m, d); send(m, "Mount", "hello", "hello.Pub data"); }, c -> with(c, "hello", e -> e.withMounts(Map.of())), "Mount", "hello", "hello.Pub");
  }
  @Test void mountWithABadTypeNameIsEditMetadataWithThatMount(@RealTempDir Path dir){
    sameAsEditMetadata(dir, none, ManagerTest::withData, c -> with(c, "hello", e -> e.withMounts(Map.of("Lower", "data"))), "Mount", "hello", "Lower data");
  }
  @Test void mountOnADataProjectIsEditMetadataWithThatMount(@RealTempDir Path dir){
    sameAsEditMetadata(dir, none, ManagerTest::withData, c -> with(c, "data", e -> e.withMounts(Map.of("hello.Hello", "hello"))), "Mount", "data", "hello.Hello hello");
  }
  @Test void attenuateOfADataProjectIsEditMetadataWithTheAttenuatorsReplaced(@RealTempDir Path dir){
    sameAsEditMetadata(dir, none, ManagerTest::withData, c -> with(c, "data", e -> e.withAttenuators(Map.of("hello.Hello", List.of("base.NoManager")))), "Attenuate", "data", "hello.Hello base.NoManager");
  }
  @Test void attenuateIsEditMetadataWithTheAttenuatorsReplaced(@RealTempDir Path dir){
    sameAsEditMetadata(dir, none, ManagerTest::withData, c -> with(c, "hello", e -> e.withAttenuators(Map.of("hello.Hello", List.of("hello.NoGui", "base.NoManager")))), "Attenuate", "hello", "hello.Hello hello.NoGui base.NoManager");
  }
  @Test void attenuateWithNoAttenuatorIsEditMetadataWithTheMainRemoved(@RealTempDir Path dir){
    sameAsEditMetadata(dir, none, (m, d) -> { withData(m, d); send(m, "Attenuate", "hello", "hello.Hello hello.NoGui"); }, c -> with(c, "hello", e -> e.withAttenuators(Map.of())), "Attenuate", "hello", "hello.Hello");
  }
  @Test void mountWhileAMainRunsIsEditMetadataWithThatMount(@RealTempDir Path dir){
    sameAsEditMetadata(dir, slow, ManagerTest::running, c -> with(c, "hello", e -> e.withMounts(Map.of("hello.Old", "data"))), "Mount", "hello", "hello.Old data");
  }
  @Test void aProjectWhoseFolderIsGoneAtStartIsForgottenAfterADialog(@RealTempDir Path dir){
    var m= manager(dir);
    var data= data(dir);
    send(m, "Register", TaggedText.of(data.toString()));
    Fs.rmTree(data);
    var again= manager(dir);
    again.settle();
    again.settle();
    assertEquals(List.of(), listed(again));
    same("The project connects to the folder [###]\nThis folder is now unreachable.\nThe project has now been removed.", view.refused.getLast());
  }
  @Test void aProjectWhoseFolderIsGoneIsForgottenAfterADialog(@RealTempDir Path dir){
    var m= manager(dir);
    var data= data(dir);
    send(m, "Register", TaggedText.of(data.toString()));
    Fs.rmTree(data);
    send(m, "Show", "data");
    assertEquals(List.of(
      "The project \"data\" connects to the folder \""+data+"\".\nThis folder is now unreachable.\nThe project \"data\" has now been removed.",
      "Refused request \"Show\" on project \"data\".\nNo project is named \"data\".\nNo project is registered."), view.refused);
    assertEquals(List.of(), listed(m));
    send(m, "Register", TaggedText.of(data.toString()));
    same("Refused request \"Register\" on path\n[###]data\nNothing exists at that path.", view.refused.getLast());
    assertEquals(List.of(), listed(m));
  }
  @Test void everyRequestNamingAProjectChecksItWhileTheWindowIsHidden(@RealTempDir Path dir){
    var m= manager(dir);
    var data= data(dir);
    send(m, "Register", TaggedText.of(data.toString()));
    send(m, "Kind", "data", "dataRead");
    view.visible = false;
    var marker= data.resolve("other.fearless");
    for (var request : List.of(List.of("Mount", "data", "other.Other other"), List.of("Show", "data"), List.of("Terminate", "data"), List.of("Check", "data"))){
      var valid= !Files.exists(marker);
      if (!valid){ Fs.ofV(() -> Files.delete(marker)); }
      else{ Fs.writeUtf8(marker, ""); }
      send(m, request.toArray(String[]::new));
      assertEquals(valid, project(m, data).problem().isPresent());
    }
  }
  @Test void foldersTheManagerCanNotReadOrChangeAreRefusedOrInvalidNeverFatal(@RealTempDir Path dir){
    var m= manager(dir);
    var closed= folder(dir, "closed");
    var readOnly= folder(dir, "readOnly");
    var cached= data(dir);
    var partly= folder(dir, "partly");
    var hidden= folder(partly, "hidden");
    var cache= folder(cached, Facts.outDir);
    Fs.writeUtf8(cache.resolve("a.built"), "");
    Disk.noRead(closed);
    Assumptions.assumeFalse(Files.isReadable(closed));
    Disk.noWrite(readOnly);
    Disk.noWrite(cache);
    Disk.noRead(hidden);
    send(m, "Register", TaggedText.of(closed.toString()));
    send(m, "Register", TaggedText.of(readOnly.toString()));
    send(m, "Register", TaggedText.of(cached.toString()));
    send(m, "Register", TaggedText.of(partly.toString()));
    send(m, "Kind", "partly", "dataRead");
    send(m, "ConnectEclipse", TaggedText.of(closed.toString()));
    same("""
Refused request "Register" on path
[###]closed
The folder cannot be read or changed: access denied: [###]closed[###]""", view.refused.get(0));
    same("[###]readOnly\nThe folder cannot be read or changed: access denied: [###]readOnly[###]", view.refused.get(1));
    same("[###]data\nThe folder cannot be read or changed: access denied: [###]_unfinished.txt[###]", view.refused.get(2));
    same("Eclipse is not connected from\n[###]closed\naccess denied: [###]closed", view.refused.get(3));
    assertEquals(List.of("partly "+partly), listed(m));
    same("""
The folder of this project cannot be read:
[###]partly
access denied: [###]hidden
Give Fearless access to it, or forget this project.""", project(m, partly).problem().orElseThrow());
    var again= manager(dir);
    again.settle();
    assertTrue(project(again, partly).problem().isPresent());
    Disk.allow(cache);
    send(again, "Register", TaggedText.of(cached.toString()));
    send(again, "Kind", "data", "code");
    Fs.writeUtf8(cache.resolve("a.built"), "");
    Disk.noWrite(cache);
    send(again, "DeleteCache", "data");
    same("--- the compilation cache is not deleted: access denied: [###]_unfinished.txt\n---\n", view.out(cached));
    List.of(closed, readOnly, cache, hidden).forEach(Disk::allow);
  }
  @Test void aManagerFolderThatCanNotBeWrittenIsFatalForEveryChangeAndARefusalWritesNothing(@RealTempDir Path dir){
    var m= manager(dir);
    var hello= code(m, dir, "hello");
    var managerDir= dir.resolve("manager");
    var before= info(dir);
    send(m, "Mount", "hello", "Lower hello");
    same("[###]\"Lower\" in \"mounts\" is not a Fearless DataRead type name[###]", view.refused.getLast());
    assertEquals(before, info(dir));
    Disk.noWrite(managerDir);
    Assumptions.assumeFalse(Files.isWritable(managerDir));
    send(m, "Mount", "hello", "hello.Data hello");
    send(m, "Kind", "hello", "idle");
    edit(m, dir, "{\"projects\": [], \"systemExtensions\": []}");
    Disk.allow(managerDir);
    assertEquals(3, failures.size());
    failures.forEach(f -> same("Fearless could not save the file \"[###]\" of its manager folder.[###]", f.getMessage()));
    failures.clear();
    assertEquals(1, view.refused.size());
    assertEquals(before, info(dir));
  }
  @Test void aMessageFileTheManagerCanNotReadIsFatal(@RealTempDir Path dir){
    var m= manager(dir);
    var msg= folder(dir.resolve("manager"), "messages").resolve("1.msg");
    Fs.writeUtf8(msg, "");
    Disk.noRead(msg);
    Assumptions.assumeFalse(Files.isReadable(msg));
    m.drain();
    m.settle();
    Disk.allow(msg);
    same("Fearless could not list its manager folder, or could not read or\nremove a message file in it.[###]1.msg[###]", failures.removeFirst().getMessage());
  }
  @Test void aFolderHoldingASymbolicLinkIsRefusedOrInvalidNeverFatal(@RealTempDir Path dir){
    var before= Fs.onSymbolicLink;
    Fs.onSymbolicLink= managerMessages.Report::symbolicLink;
    try{ symbolicLinks(dir); }
    finally{ Fs.onSymbolicLink= before; }
  }
  private void symbolicLinks(Path dir){
    var m= manager(dir);
    var linked= folder(dir, "linked");
    Disk.link(linked.resolve("current"), folder(dir, "elsewhere"));
    send(m, "Register", TaggedText.of(linked.toString()));
    send(m, "Kind", "linked", "code");
    same("This path is a symbolic link[###]", project(m, linked).problem().orElseThrow());
    send(m, "ConnectEclipse", TaggedText.of(linked.toString()));
    same("Eclipse is not connected from\n[###]linked\nThis path is a symbolic link[###]", view.refused.getLast());
    var logged= folder(dir, "logged");
    send(m, "Register", TaggedText.of(logged.toString()));
    Disk.link(folder(logged.resolve(".out"), "logs").resolve("current"), folder(dir, "elsewhere"));
    Disk.link(folder(logged, ".fearless_out").resolve("current"), folder(dir, "elsewhere"));
    send(m, "Kind", "logged", "code");
    same("This path is a symbolic link[###]", project(m, logged).problem().orElseThrow());
    send(m, "DeleteCache", "logged");
    same("[###]This path is a symbolic link[###]", view.out(logged));
    var top= folder(dir, "top");
    send(m, "Register", TaggedText.of(top.toString()));
    send(m, "Kind", "top", "code");
    Disk.link(top.resolve("pics"), folder(dir, "elsewhere"));
    send(m, "Check", "top");
    same("This path is a symbolic link[###]", project(m, top).problem().orElseThrow());
    assertEquals(List.of(), failures);
  }
  @Test void aRequestNamingNoProjectIsRefused(@RealTempDir Path dir){
    var m= manager(dir);
    send(m, "Run", "hello");
    assertEquals(List.of("Refused request \"Run\" on project \"hello\".\nNo project is named \"hello\".\nNo project is registered."), view.refused);
  }
  @Test void aProjectsInfoThatIsNotValidIsFatal(@RealTempDir Path dir){
    var m= manager(dir);
    registered(m, dir, "hello");
    var file= dir.resolve("manager").resolve("projects.info");
    Fs.writeUtf8(file, info(dir).replace("\"kind\": \"idle\"", "\"kind\": \"library\""));
    Errs.err("In file: [###]projects.info[###]\"kind\" must be one of \"idle\", \"code\", \"dataRead\" or \"dataEdit\", not \"library\".[###]", () -> manager(dir));
  }
  @Test void compilingKeepsSelectedMainsThatAreGone(@RealTempDir Path dir){
    var mains= new ArrayList<>(List.of("hello.One", "hello.Two", "hello.Old"));
    var m= manager(dir, f -> known(mains.toArray(String[]::new)).apply(f));
    var hello= code(m, dir, "hello");
    compiled(m, "hello");
    send(m, "Run", "hello", "hello.Old hello.Two");
    idle(m);
    assertEquals(List.of("hello.Old", "hello.Two"), project(m, hello).entry().mains());
    mains.remove("hello.Old");
    FakeCache.write(hello, "hello", 1000);
    compiled(m, "hello");
    assertEquals(List.of("hello.Old", "hello.Two"), project(m, hello).entry().mains());
  }
  @Test void aCompileWhoseJvmDoesNotStartIsShownOnItsProject(@RealTempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var noJvm= code(m, dir, "nojvm");
    send(m, "Compile", "nojvm");
    assertEquals("--- compiling nojvm ---\n--- compile did not start: io: Cannot run program \"java\": error=2, No such file or directory\n---\n", view.out(noJvm));
    assertEquals(Project.State.codeOutdated, project(m, noJvm).state());
  }
  @Test void aProjectChangedWhileItsMainsAreReadSuccessfullyIsCheckedAgainWhileTheWindowIsHidden(@RealTempDir Path dir){
    var reads= new ArrayList<Path>();
    var m= manager(dir, f -> {
      reads.add(f);
      var src= f.resolve("_hello").resolve("_rank_app.fear");
      Fs.writeUtf8(src, Fs.readUtf8(src)+"//saved\n");
      Fs.ofV(() -> Files.setLastModifiedTime(src, FileTime.fromMillis(FakeCache.after(f))));
      return known("hello.Hello").apply(f);
    });
    var hello= code(m, dir, "hello");
    view.visible = false;
    compiled(m, "hello");
    var p= project(m, hello);
    assertEquals(List.of(hello), reads);
    assertEquals(Project.State.codeOutdated, p.state());
    assertEquals(Optional.empty(), p.mains());
  }
  private static Optional<MainsInfo> logged(Path f, Path log, List<Path> reads, Function<Path,Optional<MainsInfo>> mains){
    reads.add(f);
    if (Files.exists(log) && reads.size() <= 5){ Fs.ofV(() -> Files.setLastModifiedTime(log, FileTime.fromMillis(Fs.lastModified(log)+1000))); }
    return mains.apply(f);
  }
  @Test void aLogWrittenWhileTheManagerReadsTheMainsDoesNotMakeTheManagerReadTheMainsAgain(@RealTempDir Path dir){
    var reads= new ArrayList<Path>();
    var log= dir.resolve("hello").resolve(LogFiles.runDir).resolve("logs").resolve("_base").resolve("log.log");
    var mains= known("hello.Hello");
    var m= manager(dir, f -> logged(f, log, reads, mains));
    code(m, dir, "hello");
    compiled(m, "hello");
    Fs.writeUtf8(log, "a line\n");
    reads.clear();
    send(m, "Check", "hello");
    assertEquals(1, reads.size());
  }
  @Test void aNewManagerRemembersTheProjects(@RealTempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    compiled(m, "hello");
    var again= manager(dir, "hello.Hello");
    again.settle();
    assertEquals(Project.State.codeCompiled, project(again, hello).state());
  }
  @Test void aFolderUnderAPathOutsideTheCharacterSetIsRegisteredRunAndRemembered(@RealTempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, folder(dir, "café 😀"), "hello");
    compiled(m, "hello");
    send(m, "Run", "hello", "hello.Hello");
    idle(m);
    assertEquals(List.of("hello "+hello), listed(m));
    same("[###]ran hello.Hello[###]", view.out(hello));
    same("[###]\"path\": \"UStr:\\\"[###]caf\\\".u\\\"00E9\\\"+(\\\" \\\".u\\\"1F600\\\")+(\\\"[###]hello\\\".u)\",[###]", info(dir));
    var again= manager(dir, "hello.Hello");
    again.settle();
    assertEquals(Project.State.codeCompiled, project(again, hello).state());
  }
  private static String listen(ServerSocket server){
    try (var s= server.accept(); var in= s.getInputStream()){ return new String(in.readAllBytes(), StandardCharsets.UTF_8); }
    catch (IOException e){ throw new UncheckedIOException(e); }
  }
  private static CompletableFuture<String> follower(ServerSocket server){ return CompletableFuture.supplyAsync(() -> listen(server)); }
  private static ServerSocket port(){ return Fs.of(() -> new ServerSocket(0, 1, InetAddress.getLoopbackAddress())); }
  @Test void aRunWithAMountedDataProjectThatIsGoneIsRefusedAsABrokenMount(@RealTempDir Path dir){
    var m= manager(dir, known(List.of(), List.of("hello.Pub"), List.of(), "hello.Hello"));
    var hello= code(m, dir, "hello");
    var data= data(dir);
    send(m, "Register", TaggedText.of(data.toString()));
    send(m, "Kind", "data", "dataRead");
    send(m, "Mount", "hello", "hello.Pub data");
    compiled(m, "hello");
    Fs.rmTree(data);
    send(m, "Run", "hello", "hello.Hello");
    same("Refused request \"Run\" on project \"hello\".\n[###]The mount of \"data\" on \"hello.Pub\" is broken: no project \"data\" is registered.", view.refused.getLast());
    assertFalse(view.out(hello).contains("running hello.Hello"));
  }
  @Test void anEditOfTheSystemExtensionsReconcilesTheMapInMemoryAndAMissingClaimIconIsFatal(@RealTempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    allow(m, dir, "foo");
    infos.put("hello", claiming("hello.Hello", "fapp001", "foo"));
    compiled(m, "hello");
    Fs.rmTree(hello.resolve(Facts.outDir));
    allow(m, dir, "htm");
    same("[###]NoSuchFileException: [###]hello/.fearless_out/icons/[###].png", failures.getFirst().toString().replace('\\', '/'));
    failures.clear();
  }
  @Test void aPortFollowsTheJobOfARunOrACompileFromItsStartToItsEndAndAnyOtherRequestUntilItIsApplied(@RealTempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    try (var server= port()){
      var show= follower(server);
      send(m, "Show", "Port:"+server.getLocalPort());
      assertEquals("Applied\n", show.join());
      var unknown= follower(server);
      send(m, "Check", "nope", "Port:"+server.getLocalPort());
      assertEquals("Refused\n"+view.refused.get(0), unknown.join());
      var compile= follower(server);
      send(m, "Compile", "hello", "Port:"+server.getLocalPort());
      same("Applied\n--- compiling hello ---\ncompiled\n--- compile done ---\n", compile.join());
      var nothing= follower(server);
      send(m, "Compile", "hello", "Port:"+server.getLocalPort());
      assertEquals("Applied\nThe project \"hello\" was already up to date and compiled.\n", nothing.join());
      var run= follower(server);
      send(m, "Run", "hello", "hello.Hello", "Port:"+server.getLocalPort());
      same("Applied\n--- running hello.Hello ---\nran hello.Hello\n--- hello.Hello exited with 0 after [###]s ---\n", run.join());
      var refused= follower(server);
      send(m, "Run", "hello", "hello.Nope", "Port:"+server.getLocalPort());
      assertEquals("Refused\n"+view.refused.get(1), refused.join());
    }
    catch (IOException e){ throw new UncheckedIOException(e); }
    assertEquals(2, view.refused.size());
    idle(m);
    assertEquals(Project.State.codeCompiled, project(m, hello).state());
  }
  @Test void aPortWhereNobodyListensIsRefused(@RealTempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    int closed;
    try (var server= port()){ closed= server.getLocalPort(); }
    catch (IOException e){ throw new UncheckedIOException(e); }
    send(m, "Compile", "hello", "Port:"+closed);
    same("Refused request \"Compile\": the manager was given the port "+closed+", but no program of this machine accepts a connection there: connect: [###]", view.refused.get(0));
    assertEquals(Project.State.codeOutdated, project(m, hello).state());
  }
  @Test void anArgumentThatIsNotACallAnswersItsPortWithTheErrorOfTheDialog(@RealTempDir Path dir){
    var m= manager(dir);
    try (var server= port()){
      server.setSoTimeout(10_000);
      var refused= follower(server);
      message(m, Main.messages("FearlessCommandForget Hello Port:"+server.getLocalPort()));
      assertEquals("Refused\n"+view.refused.getFirst(), refused.join());
    }
    catch (IOException e){ throw new UncheckedIOException(e); }
  }
  @Test void anArgumentThatIsNotACallWithAPortWhereNobodyListensShowsWhyTheArgumentIsNotACall(@RealTempDir Path dir){
    var m= manager(dir);
    int closed;
    try (var server= port()){ closed= server.getLocalPort(); }
    catch (IOException e){ throw new UncheckedIOException(e); }
    message(m, Main.messages("FearlessCommandForget Hello Port:"+closed));
    same("[###]Fearless was started with the argument\n  \"FearlessCommandForget Hello Port:"+closed+"\"\nand that argument is not a call: \"Hello\" is not a projectName[###]", String.join("\n", view.refused));
  }
  @Test void theManagerStartedWithTheRootOfADriveSaysThatTheRootOfADriveIsNotAProject(@RealTempDir Path dir){
    var m= manager(dir);
    Main.messages(dir.getRoot().toString()).forEach(m::message);
    m.settle();
    same("Fearless cannot keep track of the root of a drive or of the file system as a\nproject.\n[###]", String.join("\n", view.refused));
  }
  @Test void theConnectionOfACheckReceivesTheReportOfTheCheck(@RealTempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    try (var server= port()){
      var check= follower(server);
      send(m, "Check", "hello", "Port:"+server.getLocalPort());
      assertEquals("Applied\nThe compilation cache of this project is out of date.\n", check.join());
    }
    catch (IOException e){ throw new UncheckedIOException(e); }
    assertEquals("The compilation cache of this project is out of date.\n", view.out(hello));
  }
  @Test void aCheckWhileAMainRunsIsRefusedAndLeavesTheOutputOfTheMain(@RealTempDir Path dir){
    var m= manager(dir, "hello.Slow");
    var hello= code(m, dir, "hello");
    compiled(m, "hello");
    send(m, "Run", "hello", "hello.Slow");
    until(m, _ -> view.out(hello).contains("ran hello.Slow"));
    send(m, "Check", "hello");
    same("Refused request \"Check\" on project \"hello\".\n[###]", String.join("\n", view.refused));
    same("--- running hello.Slow ---\nran hello.Slow\n", view.out(hello));
    send(m, "Terminate", "hello");
    idle(m);
  }
  @Test void theConnectionOfAFixReceivesTheChangesOfTheFix(@RealTempDir Path dir){
    var m= manager(dir);
    var hello= registered(m, dir, "hello");
    var source= hello.resolve("_app").resolve("main.fear");
    Fs.writeUtf8(hello.resolve(".DS_Store"), "");
    Fs.writeUtf8(source, "A:{}\r\n");
    try (var server= port()){
      var fix= follower(server);
      send(m, "Fix", "hello", "Port:"+server.getLocalPort());
      assertEquals("Applied\nDeleted .DS_Store\nFixed the line endings of _app/main.fear\n", fix.join());
    }
    catch (IOException e){ throw new UncheckedIOException(e); }
    assertEquals("Deleted .DS_Store\nFixed the line endings of _app/main.fear\n", view.out(hello));
    assertEquals("A:{}\n", Fs.readUtf8(source));
    assertEquals(Optional.of(hello), m.state().selected());
    send(m, "Fix", "hello");
    assertEquals("The project \"hello\" needs no fix.\n", view.out(hello));
  }
  @Test void aRunningMainRefusesTheFixOfTheDataProjectsMountedOnTheProjectOfTheMain(@RealTempDir Path dir){
    var m= manager(dir, known(List.of(), List.of("hello.Pub"), List.of(), "hello.Slow"));
    var hello= code(m, dir, "hello");
    var data= data(dir);
    send(m, "Register", TaggedText.of(data.toString()));
    send(m, "Kind", "data", "dataRead");
    send(m, "Mount", "hello", "hello.Pub data");
    compiled(m, "hello");
    send(m, "Run", "hello", "hello.Slow");
    until(m, _ -> view.out(hello).contains("ran hello.Slow"));
    send(m, "Fix", "data");
    assertEquals(List.of("Refused request \"Fix\" on project \"data\".\nMain \"hello.Slow\" is currently running inside project \"hello\".\nProject \"hello\" has a mount of project \"data\"."), view.refused);
    send(m, "Terminate", "hello");
    idle(m);
    send(m, "Fix", "data");
    assertEquals("The project \"data\" needs no fix.\n", view.out(data));
  }
  @Test void aRunningCompilationAcceptsTheFixOfTheDataProjectsMountedOnTheProjectOfTheCompilation(@RealTempDir Path dir){
    var m= manager(dir);
    var slow= code(m, dir, "slowcompile");
    var data= data(dir);
    send(m, "Register", TaggedText.of(data.toString()));
    send(m, "Kind", "data", "dataRead");
    send(m, "Mount", "slowcompile", "slowcompile.Pub data");
    send(m, "Compile", "slowcompile");
    until(m, _ -> view.out(slow).contains("compiling\n"));
    send(m, "Fix", "data");
    assertEquals(List.of(), view.refused);
    assertEquals("The project \"data\" needs no fix.\n", view.out(data));
    idle(m);
  }
  @Test void aFixMakesAnInvalidProjectValid(@RealTempDir Path dir){
    var m= manager(dir);
    var data= data(dir);
    Fs.writeUtf8(data.resolve("My File.txt"), "");
    send(m, "Register", TaggedText.of(data.toString()));
    send(m, "Kind", "data", "dataRead");
    assertEquals(Project.State.dataReadInvalid, project(m, data).state());
    send(m, "Fix", "data");
    assertEquals("Renamed My File.txt to my_file.txt\n", view.out(data));
    assertEquals(Project.State.dataRead, project(m, data).state());
  }
  @Test void aFixEndsAtTheChangeTheFileSystemRefuses(@RealTempDir Path dir){
    var m= manager(dir);
    var hello= registered(m, dir, "hello");
    var locked= folder(hello, "locked");
    Fs.writeUtf8(hello.resolve(".DS_Store"), "");
    Fs.writeUtf8(locked.resolve("notes.txt~"), "");
    Disk.noWrite(locked);
    try (var server= port()){
      var fix= follower(server);
      send(m, "Fix", "hello", "Port:"+server.getLocalPort());
      same("Applied\nDeleted .DS_Store\naccess denied: [###]notes.txt~[###]\n", fix.join());
      same("Deleted .DS_Store\naccess denied: [###]notes.txt~[###]\n", view.out(hello));
    }
    catch (IOException e){ throw new UncheckedIOException(e); }
    finally{ Disk.allow(locked); }
  }
  @Test void aFixMovesASymbolicLinkToTheFixFolder(@RealTempDir Path dir){
    var before= Fs.onSymbolicLink;
    Fs.onSymbolicLink= managerMessages.Report::symbolicLink;
    try{ fixMovesALink(dir); }
    finally{ Fs.onSymbolicLink= before; }
  }
  private void fixMovesALink(Path dir){
    var m= manager(dir);
    var hello= registered(m, dir, "hello");
    Fs.writeUtf8(hello.resolve(".DS_Store"), "");
    Disk.link(folder(hello, "linked").resolve("link"), folder(dir, "target"));
    var moved= dir.resolve("hello_fix").resolve("linked").resolve("link");
    var lines= "Deleted .DS_Store\nMoved linked/link to "+moved+"\nDeleted the empty folder linked\n";
    try (var server= port()){
      var fix= follower(server);
      send(m, "Fix", "hello", "Port:"+server.getLocalPort());
      assertEquals("Applied\n"+lines, fix.join());
    }
    catch (IOException e){ throw new UncheckedIOException(e); }
    assertEquals(lines, view.out(hello));
    assertTrue(Fs.isLink(moved));
    assertEquals(List.of("hello.fearless"), Names.list(hello).stream().map(p -> p.getFileName().toString()).toList());
  }
  @Test void connectingEclipseNeedsAnEclipseFolder(@RealTempDir Path dir){
    var m= manager(dir);
    var eclipse= dir.resolve("unzipped").resolve("eclipse");
    Fs.writeUtf8(eclipse.resolve(".eclipseproduct"), "");
    send(m, "ConnectEclipse", TaggedText.of(eclipse.getParent().toString()));
    send(m, "ConnectEclipse", TaggedText.of(dir.toString()));
    assertEquals(List.of(eclipse), connected);
    same("Eclipse is connected:\n[###]eclipse\nRestart Eclipse if it is running: Eclipse loads the Fearless plugin when it starts.\nThen open the Fearless perspective: Window > Perspective > Open Perspective > Other... > Fearless.", view.told.getFirst());
    assertEquals(1, view.told.size());
    same("Eclipse is not connected: no Eclipse folder, a folder holding the file \".eclipseproduct\", is found from\n[###]\nSelect the Eclipse folder, a file in it, a file in a folder of it, or a folder holding only the Eclipse folder.", view.refused.getFirst());
  }
  private static String claiming(String main, String shortcut, String openWith){
    return info(main, claim("base.IconsConflict", "icons/conflict.png", "", "", shortcut), claim("hello.IconsFoo", "art/foo.png", "", "", openWith));
  }
  private List<String> wanted(Path dir){ return associated.getLast().stream().filter(i -> !i.extension().equals(".fearless")).map(i -> i.extension()+" "+shown(dir, i)).toList(); }
  private static String shown(Path dir, Icon i){
    assertEquals(i.png().toString().replaceAll("png$", Fs.isWindows() ? "ico" : "png"), i.ico().toString());
    return dir.relativize(i.png()).toString().replace('\\', '/');
  }
  private static String chosenByHand(String... lines){
    return """
Fearless cannot become the program that opens Fearless projects and the
other kinds of file it registers.

Your system remembers the choices listed below. Each one is a choice
made by hand of the program that opens a kind of file, for example with
"Open with" and "Always". Fearless does not remove these choices on
its own, and stopped before touching anything: your system is exactly
as it was.

Removing these choices clears the way. Each choice is listed under the
place holding the choice: a file, or a kind of file standing for the
registry key
HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\FileExts\\<kind of file>\\UserChoice
and the key UserChoiceLatest next to it.

The choices:
"""+String.join("\n", lines);
  }
  private static String notOurs(String... lines){
    return """
Fearless cannot become the program that opens the kinds of file listed
below.

Another program already answers for these kinds of file. Fearless
stopped before touching anything: your system is exactly as it was.

What stood in the way:
"""+String.join("\n", lines);
  }
  private static String merged(String... infos){ return Stream.of(infos).map(i -> i.strip().substring(1, i.strip().length()-1)).collect(Collectors.joining(", ", "{", "}\n")); }
  /// The preloaded icons are the icons of the claims, as the Coordinator records them.
  private static String info(String main, String shortcuts, String openWiths){
    var icons= Pattern.compile("\\[\"([^\"]+)\", \"([^\"]*)\", \"([^\"]*)\", \"([^\"]*)\", ").matcher(shortcuts+openWiths).results().map(r -> claim(r.group(1), r.group(2), r.group(3), r.group(4), "").replace("\"@@\"", "\"\"")).distinct().collect(Collectors.joining(", "));
    return "{\""+main+"\": [["+shortcuts.replace("@@\"", "@@fapp\"")+"], ["+openWiths.replace("@@\"", "@@ffile\"")+"], ["+icons+"]]}\n";
  }
  /// An empty ext is an autoselected extension: the fake compile asks the manager for it.
  private static String claim(String icon, String diskPath, String zipSteps, String zipEntry, String ext){
    return Stream.of(icon, diskPath, zipSteps, zipEntry, ext, ext.isEmpty() ? "@@" : ext).map(t -> "\""+t+"\"").collect(Collectors.joining(", ", "[", "]"));
  }
  private static Map<String,List<String>> claimed(Manager m){
    return Project.claimed(m.state().projects()).entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().stream().map(c -> c.label()+(c.shortcut() ? " shortcut " : " openWith ")+c.claim().icon()).toList()));
  }
  private static Path mainsInfo(Path project){ return project.resolve(Facts.outDir).resolve(project.getFileName()+".json"); }
  private static String compileError(Path project){ return Fs.readUtf8(project.resolve(Facts.outDir).resolve("_error.txt")); }
  private static void touch(Path project){
    var src= project.resolve("_"+project.getFileName()).resolve("_rank_app.fear");
    Fs.ofV(() -> Files.setLastModifiedTime(src, FileTime.fromMillis(FakeCache.after(project))));
  }
  @Test void aCompileClaimsTheExtensionsOfItsMainsInfoAndANewManagerClaimsThemAgain(@RealTempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    allow(m, dir, "foo");
    assertEquals(Map.of(), claimed(m));
    infos.put("hello", claiming("hello.Hello", "", "foo"));
    compiled(m, "hello");
    var foo= Map.of("foo", List.of("hello::hello.Hello openWith hello.IconsFoo"), "fapp000", List.of("hello::hello.Hello shortcut base.IconsConflict"));
    assertEquals(foo, claimed(m));
    assertEquals(new MainsInfo(Coordinator.cachedMains(hello)), project(m, hello).claims());
    var again= manager(dir, "hello.Hello");
    again.settle();
    assertEquals(foo, claimed(again));
    assertEquals(Registry.text(new Registry.Content(List.of(project(again, hello).entry()), List.of("foo"))), info(dir));
    edit(again, dir, Registry.text(new Registry.Content(List.of(project(again, hello).entry().withKind(Kind.idle)), List.of("foo"))));
    assertEquals(Map.of(), claimed(again));
    assertEquals(Project.noClaims, project(again, hello).claims());
  }
  @Test void projectsClaimingTheSameExtensionAreAllItsClaimantsUntilCleanedIdleOrForgotten(@RealTempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    code(m, dir, "other");
    allow(m, dir, "foo");
    infos.put("hello", claiming("hello.Hello", "fapp042", "foo"));
    infos.put("other", claiming("other.Other", "fapp010", "foo"));
    compiled(m, "other", "hello");
    assertEquals(Map.of(
      "fapp042", List.of("hello::hello.Hello shortcut base.IconsConflict"),
      "fapp010", List.of("other::other.Other shortcut base.IconsConflict"),
      "foo", List.of("hello::hello.Hello openWith hello.IconsFoo", "other::other.Other openWith hello.IconsFoo")), claimed(m));
    send(m, "DeleteCache", "hello");
    assertEquals(Map.of("fapp010", List.of("other::other.Other shortcut base.IconsConflict"), "foo", List.of("other::other.Other openWith hello.IconsFoo")), claimed(m));
    send(m, "Kind", "other", "idle");
    assertEquals(Map.of(), claimed(m));
    compiled(m, "hello");
    assertEquals(List.of("fapp042", "foo"), claimed(m).keySet().stream().sorted().toList());
    send(m, "Forget", "hello");
    assertEquals(Map.of(), claimed(m));
    assertTrue(Files.exists(hello.resolve("hello.fapp042")));
  }
  @Test void aProjectWhoseMainsInfoIsMissingOrUnreadableClaimsNothing(@RealTempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    var other= code(m, dir, "other");
    allow(m, dir, "foo", "bar");
    infos.put("hello", claiming("hello.Hello", "fapp001", "foo"));
    infos.put("other", claiming("other.Other", "fapp002", "bar"));
    compiled(m, "hello", "other");
    assertEquals(List.of("bar", "fapp001", "fapp002", "foo"), claimed(m).keySet().stream().sorted().toList());
    Fs.ofV(() -> Files.delete(mainsInfo(hello)));
    Fs.writeUtf8(mainsInfo(other), FakeCache.json(FakeCache.manifest("{\"other.Other\": []}\n")));
    var again= manager(dir, "hello.Hello");
    again.settle();
    assertEquals(Map.of(), claimed(again));
    assertEquals(List.of(), view.refused);
  }
  @Test void everyKindChangeDeletesTheCache(@RealTempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    allow(m, dir, "foo");
    infos.put("hello", claiming("hello.Hello", "fapp001", "foo"));
    compiled(m, "hello");
    assertEquals(List.of("fapp001", "foo"), claimed(m).keySet().stream().sorted().toList());
    send(m, "Kind", "hello", "idle");
    assertEquals(Map.of(), claimed(m));
    assertFalse(Files.exists(hello.resolve(Facts.outDir)));
    send(m, "Kind", "hello", "code");
    assertEquals(Map.of(), claimed(m));
    assertEquals(Project.State.codeOutdated, project(m, hello).state());
  }
  @Test void eachAutoselectedExtensionIsTheFirstCandidateNoClaimOfTheMapUses(@RealTempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    var other= code(m, dir, "other");
    allow(m, dir, "foo", "bar");
    infos.put("hello", claiming("hello.Hello", "", "foo"));
    compiled(m, "hello");
    assertEquals(List.of("hello::hello.Hello shortcut base.IconsConflict"), claimed(m).get("fapp000"));
    assertEquals(new MainsInfo(Coordinator.cachedMains(hello)), project(m, hello).claims());
    infos.put("other", claiming("other.Other", "", "bar"));
    compiled(m, "other");
    assertEquals(List.of("other::other.Other shortcut base.IconsConflict"), claimed(m).get("fapp001"));
    same("[###]fapp001[###]", Fs.readUtf8(mainsInfo(other)));
    assertEquals(Project.State.codeCompiled, project(m, other).state());
  }
  @Test void noFreeExtensionLeftFailsTheCompile(@RealTempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    code(m, dir, "other");
    allow(m, dir, "foo");
    var all= IntStream.range(0, 1000).mapToObj(i -> claim("other.IconsO", "art/o.png", "", "", String.format(Locale.ROOT, "fapp%03d", i))).collect(Collectors.joining(", "));
    infos.put("other", info("other.Other", all, ""));
    compiled(m, "other");
    infos.put("hello", claiming("hello.Hello", "", "foo"));
    compiled(m, "hello");
    same("--- compiling hello ---\nno free extension\n--- compile failed with 1 ---\n", view.out(hello));
    assertEquals(Project.State.codeOutdated, project(m, hello).state());
    assertEquals(1000, claimed(m).size());
  }
  private static byte[] image(Path file, int w, int h, String format){
    var out= new ByteArrayOutputStream();
    var written= Fs.of(() -> ImageIO.write(new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB), format, out));
    assert written;
    Fs.ensureDir(file.getParent());
    Fs.ofV(() -> Files.write(file, out.toByteArray()));
    return out.toByteArray();
  }
  private static byte[] zip(String entry, byte[] content){
    var out= new ByteArrayOutputStream();
    Fs.ofV(() -> { try (var z= new ZipOutputStream(out)){ z.putNextEntry(new ZipEntry(entry)); z.write(content); } });
    return out.toByteArray();
  }
  private static byte[] bytes(Path file){ return Fs.of(() -> Files.readAllBytes(file)); }
  private static Path icons(Path project){ return project.resolve(Facts.outDir).resolve("icons"); }
  private static String iconError(String claim, String problem, String from){
    return "The icon \"hello.IconsFoo\" in \""+claim+"\" of main \"hello.Hello\" "+problem+" (from \""+from+"\"):\nan icon must be a square PNG image with a side from 64 to 1024 pixels.\n";
  }
  @Test void aSourceUpdatedWhileAMainRunsTerminatesTheMain(@RealTempDir Path dir){
    var m= manager(dir, "hello.Slow");
    var hello= code(m, dir, "hello");
    compiled(m, "hello");
    send(m, "Run", "hello", "hello.Slow");
    until(m, _ -> view.out(hello).contains("ran hello.Slow"));
    touch(hello);
    var shown= view.shown;
    send(m, "Show", "hello");
    idle(m);
    same("""
--- running hello.Slow ---
ran hello.Slow
--- hello.Slow was terminated after [###]s ---
Some files inside the project "hello" were updated while main "hello.Slow" was running.
""", view.out(hello));
    assertEquals(shown+1, view.shown);
    assertTrue(Files.exists(hello.resolve(Facts.outDir).resolve("hello.json")));
    assertEquals(Project.State.codeOutdated, project(m, hello).state());
  }
  @Test void aSecondMarkerWrittenWhileAMainRunsTerminatesTheMain(@RealTempDir Path dir){
    var m= manager(dir, "hello.Slow");
    var hello= code(m, dir, "hello");
    compiled(m, "hello");
    send(m, "Run", "hello", "hello.Slow");
    until(m, _ -> view.out(hello).contains("ran hello.Slow"));
    Fs.writeUtf8(hello.resolve("other.fearless"), "");
    send(m, "Show", "hello");
    idle(m);
    same("""
--- running hello.Slow ---
ran hello.Slow
--- hello.Slow was terminated after [###]s ---
Some files inside the project "hello" were updated while main "hello.Slow" was running.
""", view.out(hello));
    assertTrue(Files.exists(hello.resolve(Facts.outDir).resolve("hello.json")));
    assertEquals(Project.State.codeInvalid, project(m, hello).state());
  }
  @Test void theDesktopOpensEachCapturedExtensionWithItsIconOrTheConflictIcon(@RealTempDir Path dir){
    var m= manager(dir, "hello.Hello");
    code(m, dir, "hello");
    code(m, dir, "other");
    assertEquals(List.of(".fearless manager/icons/base.IconsFearless.png"), associated.getLast().stream().map(i -> i.extension()+" "+shown(dir, i)).toList());
    assertArrayEquals(bytes(LocalResources.stLibPath.resolve("icons").resolve("conflict.png")), bytes(dir.resolve("manager").resolve("icons").resolve("base.IconsConflict.png")));
    assertArrayEquals(bytes(LocalResources.stLibPath.resolve("icons").resolve("fearless.png")), bytes(dir.resolve("manager").resolve("icons").resolve("base.IconsFearless.png")));
    allow(m, dir, "foo");
    assertEquals(List.of(".foo manager/icons/base.IconsConflict.png"), wanted(dir));
    infos.put("hello", claiming("hello.Hello", "fapp010", "foo"));
    infos.put("other", claiming("other.Other", "fapp004", "foo"));
    compiled(m, "hello");
    assertEquals(List.of(".fapp010 hello/.fearless_out/icons/_base/icons/conflict.png", ".foo hello/.fearless_out/icons/art/foo.png"), wanted(dir));
    compiled(m, "other");
    assertEquals(List.of(
      ".fapp004 other/.fearless_out/icons/_base/icons/conflict.png",
      ".fapp010 hello/.fearless_out/icons/_base/icons/conflict.png",
      ".foo manager/icons/base.IconsConflict.png"), wanted(dir));
    send(m, "Show", "hello");
    touch(dir.resolve("hello"));
    compiled(m, "hello");
    send(m, "DeleteCache", "hello");
    assertEquals(List.of(".fapp004 other/.fearless_out/icons/_base/icons/conflict.png", ".foo other/.fearless_out/icons/art/foo.png"), wanted(dir));
    send(m, "Forget", "other");
    assertEquals(List.of(".foo manager/icons/base.IconsConflict.png"), wanted(dir));
    assertEquals(List.of(), view.refused);
  }
  @Test void aCompileWhoseNewExtensionTheDesktopRefusesFailsAndDeletesTheCacheOfEveryProjectClaimingIt(@RealTempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    code(m, dir, "other");
    allow(m, dir, "foo", "bar");
    infos.put("other", claiming("other.Other", "fapp002", "foo"));
    infos.put("hello", claiming("hello.Hello", "fapp001", "bar"));
    compiled(m, "other", "hello");
    held.put(".fapp003", List.of("vim"));
    infos.put("hello", claiming("hello.Hello", "fapp003", "bar"));
    touch(hello);
    compiled(m, "hello");
    var error= notOurs(".fapp003 claimed by \"hello.Hello\" of project \"hello\" -> vim");
    same("[###]--- compiling hello ---\ncompiled\n"+error+"\n--- compile failed ---\n", view.out(hello));
    same(error, compileError(hello));
    assertTrue(project(m, hello).needsCompiling());
    assertFalse(Files.exists(mainsInfo(hello)));
    assertEquals(Project.noClaims, project(m, hello).claims());
    assertEquals(List.of(".bar manager/icons/base.IconsConflict.png", ".fapp002 other/.fearless_out/icons/_base/icons/conflict.png", ".foo other/.fearless_out/icons/art/foo.png"), wanted(dir));
    assertEquals(List.of(), view.refused);
  }
  @Test void aMainTheCorruptionRuleEndsGetsNoFilesUpdatedLine(@RealTempDir Path dir){
    var m= manager(dir, "hello.Slow");
    var hello= code(m, dir, "hello");
    code(m, dir, "other");
    compiled(m, "hello");
    send(m, "Run", "hello", "hello.Slow");
    until(m, _ -> view.out(hello).contains("ran hello.Slow"));
    Fs.writeUtf8(mainsInfo(hello), "{");
    send(m, "DeleteCache", "other");
    idle(m);
    same("[###]ran hello.Slow\n--- hello.Slow was terminated[###]", view.out(hello));
    assertFalse(view.out(hello).contains("were updated"));
    same("Fearless crashed: either this project's compilation cache was corrupted[###]", compileError(hello));
  }
  @Test void aCorruptedCacheThatCanNotBeDeletedMakesTheProjectInvalid(@RealTempDir Path dir){
    var broken= new java.util.concurrent.atomic.AtomicBoolean();
    var m= manager(dir, f -> {
      if (broken.get()){ throw userMessages.UserError.cacheCorruptedOrCrash(new IllegalStateException("damaged")); }
      return known("hello.Hello").apply(f);
    });
    var hello= code(m, dir, "hello");
    compiled(m, "hello");
    broken.set(true);
    var out= hello.resolve(Facts.outDir);
    Disk.noWrite(out);
    try{
      send(m, "Check", "hello");
      assertTrue(project(m, hello).problem().isPresent());
      var once= view.out(hello);
      send(m, "Show", "hello");
      assertEquals(once, view.out(hello));
      assertEquals(List.of(), failures);
    }
    finally{ Disk.allow(out); }
  }
  @Test void aStartUpRefusedACapturedExtensionIsFatal(@RealTempDir Path dir){
    var m= manager(dir, "hello.Hello");
    code(m, dir, "hello");
    code(m, dir, "other");
    allow(m, dir, "foo", "qux");
    infos.put("hello", claiming("hello.Hello", "fapp001", "foo"));
    infos.put("other", claiming("other.Other", "fapp002", "qux"));
    compiled(m, "hello", "other");
    chosen.add(".qux");
    manager(dir, "hello.Hello").settle();
    chosen.clear();
    held.put(".fapp002", List.of("vim"));
    manager(dir, "hello.Hello").settle();
    held.clear();
    held.put(".fearless", List.of("fearlessBin0_003"));
    manager(dir, "hello.Hello").settle();
    assertEquals(3, failures.size());
    assertEquals(List.of(UserError.class, UserError.class, UserError.class), failures.stream().map(Object::getClass).toList());
    same(chosenByHand(".qux claimed by \"other.Other\" of project \"other\"", "  vim"), failures.get(0).getMessage());
    same(notOurs(".fapp002 claimed by \"other.Other\" of project \"other\" -> vim"), failures.get(1).getMessage());
    same(notOurs(".fearless -> fearlessBin0_003"), failures.get(2).getMessage());
    failures.clear();
  }
  @Test void everyCompileReconcilesAlsoWhenNothingChanged(@RealTempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    allow(m, dir, "foo");
    infos.put("hello", claiming("hello.Hello", "fapp010", "foo"));
    compiled(m, "hello");
    var calls= associated.size();
    touch(hello);
    compiled(m, "hello");
    assertEquals(calls+1, associated.size());
    assertEquals(associated.get(calls-1), associated.getLast());
  }
  @Test void aShortcutFileTheManagerCanNotCreateFailsTheCompile(@RealTempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    compiled(m, "hello");
    infos.put("hello", info("hello.Hello", claim("base.IconsConflict", "icons/conflict.png", "", "", "fapp010"), ""));
    Disk.noWrite(hello);
    Assumptions.assumeFalse(Files.isWritable(hello));
    touch(hello);
    compiled(m, "hello");
    Disk.allow(hello);
    var notWritten= "--- hello.fapp010 is not written: access denied: "+hello.resolve("hello.fapp010")+"\n---\n";
    assertEquals("--- compiling hello ---\ncompiled\n"+notWritten+"--- compile failed ---\n", view.out(hello));
    assertEquals(notWritten, Fs.readUtf8(hello.resolve(Facts.outDir).resolve("_error.txt")));
    assertEquals(List.of(".fapp010 hello/.fearless_out/icons/_base/icons/conflict.png"), wanted(dir));
    assertEquals(Project.State.codeOutdated, project(m, hello).state());
  }
  private static List<String> top(Path project){ return Names.list(project).stream().map(p -> p.getFileName().toString()).toList(); }
  @Test void aCompileCreatesTheMissingShortcutFilesNextToTheMarkerHoldingItsNewlineAndNeverTouchesAnExistingOne(@RealTempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    allow(m, dir, "foo");
    var shortcuts= claim("base.IconsConflict", "icons/conflict.png", "", "", "fapp010")+", "+claim("base.IconsConflict", "icons/conflict.png", "", "", "");
    infos.put("hello", merged(info("hello.FooBar", shortcuts, ""), info("hello.Foo'", "", claim("base.IconsConflict", "icons/conflict.png", "", "", "foo"))));
    compiled(m, "hello");
    var auto= "foo_bar."+"fapp000";
    assertEquals(List.of(".fearless_out", "_hello", auto, "foo_bar.fapp010", "hello.fearless"), top(hello));
    assertEquals(MakeDemo.markerContent, Fs.readUtf8(hello.resolve("foo_bar.fapp010")));
    assertEquals(MakeDemo.markerContent, Fs.readUtf8(hello.resolve(auto)));
    assertEquals(Project.State.codeCompiled, project(m, hello).state());
    Fs.writeUtf8(hello.resolve("foo_bar.fapp010"), "kept\n");
    Fs.ofV(() -> Files.move(hello.resolve(auto), dir.resolve(auto)));
    send(m, "Show", "hello");
    assertEquals(Project.State.codeCompiled, project(m, hello).state());
    touch(hello);
    compiled(m, "hello");
    assertEquals(List.of(".fearless_out", "_hello", auto, "foo_bar.fapp010", "hello.fearless"), top(hello));
    assertEquals("kept\n", Fs.readUtf8(hello.resolve("foo_bar.fapp010")));
    assertEquals(MakeDemo.markerContent, Fs.readUtf8(hello.resolve(auto)));
    assertTrue(Files.exists(dir.resolve(auto)));
    Fs.ofV(() -> Files.delete(hello.resolve("foo_bar.fapp010")));
    touch(hello);
    compiled(m, "hello");
    assertEquals(MakeDemo.markerContent, Fs.readUtf8(hello.resolve("foo_bar.fapp010")));
    assertEquals(Project.State.codeCompiled, project(m, hello).state());
  }
  @Test void aShortcutMainWithoutAFileNameOrWithAReservedOneOrSharingItsFileFailsTheCompileAndCreatesNothing(@RealTempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    var bar= claim("base.IconsConflict", "icons/conflict.png", "", "", "fapp010");
    infos.put("hello", info("hello.Hello", bar, ""));
    compiled(m, "hello");
    var before= top(hello);
    var main= "[["+bar+"], [], ["+claim("base.IconsConflict", "icons/conflict.png", "", "", "")+"]]";
    for (var c : List.of(
      Map.entry("{\"hello.Foo'\": "+main+"}\n", "Main \"hello.Foo'\" can not have a Shortcut: its name has no matching file name. A main with a Shortcut has a name that can be mapped to a file name like \"FooBar\" can be mapped to \"foo_bar\"."),
      Map.entry("{\"hello.Foo_bar\": "+main+"}\n", "Main \"hello.Foo_bar\" can not have a Shortcut: its name has no matching file name. A main with a Shortcut has a name that can be mapped to a file name like \"FooBar\" can be mapped to \"foo_bar\"."),
      Map.entry("{\"hello.Con\": "+main+"}\n", "Main \"hello.Con\" cannot have the shortcut file \"con.fapp010\": a file name in a project is at most 200 characters long, and its part before the dot is not \"con\", \"prn\", \"aux\", \"nul\", \"com1\"..\"com9\" or \"lpt1\"..\"lpt9\", reserved on Windows. Give this main another name."),
      Map.entry("{\"b.Bar\": "+main+", \"a.Bar\": "+main+"}\n", "Mains \"a.Bar\" and \"b.Bar\" cannot both have the shortcut file \"bar.fapp010\": a shortcut file is named by the name of its main and the extension of its Shortcut. Rename one of the two mains, or give one of their Shortcuts another extension."))){
      infos.put("hello", c.getKey());
      touch(hello);
      compiled(m, "hello");
      same("--- compiling hello ---\ncompiled\n"+c.getValue()+"\n--- compile failed ---\n", view.out(hello));
      same(c.getValue(), compileError(hello));
      assertEquals(new MainsInfo(Coordinator.cachedMains(hello)), project(m, hello).claims());
      assertEquals(before, top(hello));
    }
    assertEquals(List.of(".fapp010 manager/icons/base.IconsConflict.png"), wanted(dir));
  }
  @Test void aShortcutFileBreakingTheProjectFileRulesFailsTheCompileAndCreatesNothing(@RealTempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    var calls= associated.size();
    var bar= claim("base.IconsConflict", "icons/conflict.png", "", "", "fapp010");
    var longName= "hello.L"+"o".repeat(192);
    for (var c : List.of(
      Map.entry(info(longName, bar, ""), "Main \""+longName+"\" cannot have the shortcut file \"l"+"o".repeat(192)+".fapp010\": a file name in a project is at most 200 characters long, and its part before the dot is not \"con\", \"prn\", \"aux\", \"nul\", \"com1\"..\"com9\" or \"lpt1\"..\"lpt9\", reserved on Windows. Give this main another name."))){
      infos.put("hello", c.getKey());
      touch(hello);
      compiled(m, "hello");
      same("--- compiling hello ---\ncompiled\n"+c.getValue()+"\n--- compile failed ---\n", view.out(hello));
      assertEquals(List.of(".fearless_out", "_hello", "hello.fearless"), top(hello));
    }
    assertEquals(calls+1, associated.size());
    infos.put("hello", info("hello.L"+"o".repeat(191), bar, ""));
    touch(hello);
    compiled(m, "hello");
    assertTrue(Files.exists(hello.resolve("l"+"o".repeat(191)+".fapp010")));
    infos.put("hello", info("hello.Todo", bar, ""));
    touch(hello);
    compiled(m, "hello");
    assertEquals(MakeDemo.markerContent, Fs.readUtf8(hello.resolve("todo.fapp010")));
    assertEquals(Project.State.codeCompiled, project(m, hello).state());
  }
  private Manager opening(Path dir){
    return manager(dir, f -> known(f.getFileName()+"."+(f.getFileName().toString().equals("slow") ? "Slow" : "Hello")).apply(f));
  }
  private static Path doc(Path dir, String name){
    var res= dir.resolve("desk").resolve(name);
    Fs.writeUtf8(res, "text\n");
    return res;
  }
  private static String running(String main){ return running(main, ""); }
  private static String running(String main, String on){ return "--- running "+main+" ---\nran "+main+on+"\n--- "+main+" exited with 0 after [###]s ---\n"; }
  private static void open(Manager m, Path file){ send(m, "OpenWithOrShortcut", TaggedText.of(file.toString())); }
  @Test void aFileWhoseExtensionOneMainClaimsRunsThatMainAndOneNotCompiledIsRefused(@RealTempDir Path dir){
    var m= opening(dir);
    var hello= code(m, dir, "hello");
    var other= code(m, dir, "other");
    allow(m, dir, "foo");
    infos.put("hello", claiming("hello.Hello", "fapp001", "foo"));
    compiled(m, "hello");
    send(m, "Show", "other");
    open(m, doc(dir, "a.foo"));
    idle(m);
    same(running("hello.Hello", " on a.foo"), view.out(hello));
    assertEquals(Optional.of(hello), m.state().selected());
    touch(hello);
    open(m, doc(dir, "b.fapp001"));
    assertEquals(List.of("Refused request \"Run\" on project \"hello\".\nThe state of project \"hello\" is \"code: compilation cache out of date\": only a project in state \"code: compiled\" runs a main."), view.refused);
    assertEquals(List.of(), view.choices);
    assertEquals(List.of("hello "+hello, "other "+other), listed(m));
  }
  @Test void aFileOpenedLeavesTheSelectedMainsAsTheyAre(@RealTempDir Path dir){
    var m= opening(dir);
    var hello= code(m, dir, "hello");
    allow(m, dir, "foo");
    infos.put("hello", claiming("hello.Hello", "fapp001", "foo"));
    compiled(m, "hello");
    message(m, List.of("SelectMains", "hello", "hello.Gone"));
    open(m, doc(dir, "a.foo"));
    idle(m);
    same(running("hello.Hello", " on a.foo"), view.out(hello));
    assertEquals(List.of("hello.Gone"), project(m, hello).entry().mains());
  }
  @Test void aFileWhoseExtensionSeveralMainsClaimListsThemSorted(@RealTempDir Path dir){
    var m= opening(dir);
    var other= code(m, dir, "other");
    var hello= code(m, dir, "hello");
    allow(m, dir, "foo");
    infos.put("other", claiming("other.Hello", "fapp002", "foo"));
    infos.put("hello", claiming("hello.Hello", "fapp001", "foo"));
    compiled(m, "other", "hello");
    var shown= view.shown;
    open(m, doc(dir, "a.foo"));
    assertEquals(List.of(List.of("a.foo", "hello::hello.Hello", "other::other.Hello")), view.choices);
    assertEquals(shown, view.shown);
    idle(m);
    assertEquals(List.of(other, hello), view.cleared);
    send(m, "OpenWithOrShortcut", TaggedText.of(doc(dir, "a.foo").toString()), "other", "other.Hello");
    idle(m);
    same(running("other.Hello", " on a.foo"), view.out(other));
    assertEquals(Optional.of(other), m.state().selected());
    send(m, "OpenWithOrShortcut", TaggedText.of(doc(dir, "a.foo").toString()), "hello", "hello.Other");
    same("""
Refused request to open
[###]a.foo
Main "hello.Other" of project "hello" does not open files with the extension "foo".
The mains opening them are:
  hello::hello.Hello
  other::other.Hello""", view.refused.getLast());
  }
  @Test void anExtensionMatchesInAnyCaseAsTheDesktopMatchesIt(@RealTempDir Path dir){
    var m= opening(dir);
    var hello= code(m, dir, "hello");
    allow(m, dir, "foo");
    infos.put("hello", claiming("hello.Hello", "fapp001", "foo"));
    compiled(m, "hello");
    open(m, doc(dir, "Report.Old.FOO"));
    idle(m);
    same(running("hello.Hello", " on Report.Old.FOO"), view.out(hello));
  }
  @Test void aFileOpenedWhileItsMainRunsIsRefused(@RealTempDir Path dir){
    var m= opening(dir);
    var slow= code(m, dir, "slow");
    allow(m, dir, "foo");
    infos.put("slow", claiming("slow.Slow", "fapp003", "foo"));
    compiled(m, "slow");
    open(m, doc(dir, "a.foo"));
    until(m, _ -> view.out(slow).contains("ran slow.Slow"));
    open(m, doc(dir, "b.fapp003"));
    assertEquals(List.of("Refused request \"Run\" on project \"slow\".\nMain \"slow.Slow\" is currently running inside project \"slow\"."), view.refused);
    send(m, "Terminate", "slow");
    idle(m);
  }
  @Test void aFileNobodyClaimsAMissingFileAnEmptyFileAndACapturedExtensionNoMainOpensAreRefused(@RealTempDir Path dir){
    var m= opening(dir);
    var hello= code(m, dir, "hello");
    allow(m, dir, "foo", "htm");
    infos.put("hello", claiming("hello.Hello", "fapp001", "foo"));
    compiled(m, "hello");
    var empty= dir.resolve("desk").resolve("e.foo");
    Fs.writeUtf8(empty, "");
    open(m, doc(dir, "a.txt"));
    open(m, dir.resolve("desk").resolve("gone.foo"));
    open(m, empty);
    open(m, doc(dir, "page.htm"));
    open(m, dir.resolve("desk"));
    same("""
Refused request to open
[###]a.txt
No main opens files with the extension "txt".""", view.refused.get(0));
    same("Refused request to open\n[###]gone.foo\nNo file is at that path.", view.refused.get(1));
    same("Refused request to open\n[###]e.foo\nThe file \"e.foo\" is empty. File associations do not reliably work for empty files on all operating systems.", view.refused.get(2));
    same("[###]page.htm\nNo main opens files with the extension \"htm\".", view.refused.get(3));
    same("[###]desk\nNo file is at that path.", view.refused.get(4));
    assertEquals(List.of(hello), view.cleared);
    assertEquals(List.of("hello "+hello), listed(m));
  }
  @Test void aCompileClaimingASystemExtensionNotCapturedFailsAndOffersToCaptureIt(@RealTempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    infos.put("hello", claiming("hello.Hello", "fapp001", "htm"));
    compiled(m, "hello");
    var error= "The system extension \"htm\" of \"base.OpenWith[hello.IconsFoo,\\\"htm\\\"]\" of main \"hello.Hello\" is not in \"systemExtensions\" of projects.info.";
    same("--- compiling hello ---\ncompiled\n"+error+"\n--- compile failed ---\n", view.out(hello));
    same(error, compileError(hello));
    same("""
Add these system extensions to "systemExtensions" of projects.info?
  "htm" for "base.OpenWith[hello.IconsFoo,\\"htm\\"]" of main "hello.Hello" of project "hello"

The manager becomes the program opening every file with these extensions,
in place of the program opening them now. The project then needs compiling again.""", String.join("\n", view.asked));
    assertEquals(List.of("fapp001", "htm"), claimed(m).keySet().stream().sorted().toList());
    assertEquals(List.of(".fapp001 hello/.fearless_out/icons/_base/icons/conflict.png"), wanted(dir));
    assertEquals(Project.State.codeOutdated, project(m, hello).state());
    assertEquals(List.of(List.of("htm")), view.offered);
    allow(m, dir, view.offered.getLast().toArray(String[]::new));
    assertEquals(List.of("htm"), m.state().extensions());
    compiled(m, "hello");
    assertEquals(List.of("fapp001", "htm"), claimed(m).keySet().stream().sorted().toList());
    assertEquals(1, view.asked.size());
  }
  @Test void anOfferToCaptureAnExtensionAcceptedAfterARegisterKeepsThatProject(@RealTempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    infos.put("hello", claiming("hello.Hello", "fapp001", "htm"));
    compiled(m, "hello");
    var other= registered(m, dir, "other");
    allow(m, dir, view.offered.getLast().toArray(String[]::new));
    assertEquals(List.of("htm"), m.state().extensions());
    assertEquals(List.of("hello "+hello, "other "+other), listed(m));
  }
  @Test void removingASystemExtensionClaimedByAProjectWithARunningMainIsRefused(@RealTempDir Path dir){
    var m= manager(dir, "hello.Slow");
    var hello= code(m, dir, "hello");
    allow(m, dir, "foo");
    infos.put("hello", claiming("hello.Slow", "fapp001", "foo"));
    compiled(m, "hello");
    send(m, "Run", "hello", "hello.Slow");
    until(m, _ -> view.out(hello).contains("ran hello.Slow"));
    var before= info(dir);
    edit(m, dir, before.replace("\"foo\"", ""));
    assertEquals(before, info(dir));
    assertEquals("Refused request to remove \"foo\" from \"systemExtensions\": a main of project \"hello\" claims \"foo\".\nMain \"hello.Slow\" is currently running inside project \"hello\".", view.refused.getLast());
    send(m, "Terminate", "hello");
    idle(m);
  }
  @Test void anEditWhoseReconcileRefusesAnExtensionTheEditDoesNotBringIsFatal(@RealTempDir Path dir){
    var m= manager(dir);
    held.put(".fearless", List.of("fearlessBin0_003"));
    allow(m, dir, "htm");
    same(notOurs(".fearless -> fearlessBin0_003"), failures.getFirst().getMessage());
    failures.clear();
  }
  @Test void anEditAddingAProjectWhoseFolderIsMissingCreatesNoFolder(@RealTempDir Path dir){
    var m= manager(dir);
    var gone= dir.resolve("typo").resolve("proj");
    var path= TaggedText.ofPath(gone).replace("\\", "\\\\").replace("\"", "\\\"");
    edit(m, dir, "{\"projects\": {\"proj\": {\"path\": \""+path+"\", \"kind\": \"idle\"}}, \"systemExtensions\": []}");
    m.settle();
    assertFalse(Files.exists(dir.resolve("typo")));
    assertEquals(List.of(), listed(m));
  }
  @Test void anEditChangingOnlyTheSelectedMainsDoesNotReconcile(@RealTempDir Path dir){
    var m= manager(dir, "hello.Hello");
    code(m, dir, "hello");
    compiled(m, "hello");
    var calls= associated.size();
    message(m, List.of("SelectMains", "hello", "hello.Hello"));
    assertEquals(calls, associated.size());
  }
  @Test void removingASystemExtensionDeletesTheCompiledCacheOfTheProjectsOpeningIt(@RealTempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    var other= code(m, dir, "other");
    allow(m, dir, "foo", "qux");
    infos.put("hello", claiming("hello.Hello", "fapp001", "foo"));
    infos.put("other", claiming("other.Other", "fapp002", "qux"));
    compiled(m, "hello", "other");
    edit(m, dir, info(dir).replace("\"foo\", ", ""));
    assertEquals(List.of("qux"), m.state().extensions());
    assertEquals(Project.noClaims, project(m, hello).claims());
    assertFalse(Files.exists(hello.resolve(Facts.outDir)));
    assertEquals(Project.State.codeOutdated, project(m, hello).state());
    assertEquals(Project.State.codeCompiled, project(m, other).state());
    assertEquals(List.of(".fapp002 other/.fearless_out/icons/_base/icons/conflict.png", ".qux other/.fearless_out/icons/art/foo.png"), wanted(dir));
  }
}

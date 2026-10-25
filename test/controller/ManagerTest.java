package controller;

import static controller.Errs.same;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
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
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import controller.Manager.State;
import controller.Registry.Kind;
import coordinator.MainsInfo;
import fileAssociations.Icon;
import mainCoordinator.MakeDemo;
import resources.LocalResources;
import tools.ChildJvm;
import tools.Fs;
import userMessages.Report;
import userMessages.UserError;
import userMessages.Violation;

/// The whole manager without a window: messages in, View calls and files out.
/// Compiling writes a fresh cache and every run and compilation is a real child JVM running Child.
final class ManagerTest{
  @TempDir static Path classes;
  @BeforeAll static void child(){
    var src= classes.resolve("Child.java");
    Fs.writeUtf8(src, """
      public class Child{
        public static void main(String[] a) throws Exception{
          System.out.print(a[0]+"\\n");
          System.out.flush();
          Thread.sleep(Long.parseLong(a[2]));
          System.exit(Integer.parseInt(a[1]));
        }
      }
      """);
    Fs.runTool("javac", List.of("-d", classes.toString(), src.toString()));
    FactsTest.baseCache(classes);
  }
  record Fake(Function<Path,Optional<MainsInfo>> read, Map<String,String> infos, List<List<Icon>> associated, List<List<String>> windows, Map<String,List<String>> held, List<String> locked, List<Path> connected, List<Registry.Grants> granted) implements Manager.Tools{
    @Override public ChildJvm compile(Path folder, Consumer<String> out){
      if (folder.getFileName().toString().equals("nojvm")){ throw new UncheckedIOException(new IOException("Cannot run program \"java\": error=2, No such file or directory")); }
      if (infos.getOrDefault(folder.getFileName().toString(), "").equals("fail")){ return jvm(out, "broken source", "1", "0"); }
      FactsTest.cache(folder, folder.getFileName().toString(), FactsTest.after(folder), infos.getOrDefault(folder.getFileName().toString(), "{}\n"));
      MainsInfo.read(folder).orElseThrow().mains().values().stream().flatMap(m -> Stream.of(m.shortcuts(), m.openWiths(), m.icons()).flatMap(List::stream))
        .filter(c -> !c.icon().startsWith("base.")).map(c -> folder.resolve(c.diskPath())).filter(p -> !Files.exists(p)).forEach(p -> image(p, 64, 64, "png"));
      if (folder.getFileName().toString().equals("edited")){ touch(folder); }
      return jvm(out, "compiled", "0", "0");
    }
    @Override public ChildJvm run(Path folder, String main, Optional<Path> input, Registry.Grants grants, List<String> attenuators, Consumer<String> out){
      granted.add(grants);
      return jvm(out, "ran "+main+input.map(i -> " on "+i.getFileName()).orElse("")+(attenuators.isEmpty() ? "" : " under "+String.join(" ", attenuators)), "0", main.endsWith("Slow") ? "60000" : "0");
    }
    @Override public Optional<MainsInfo> mains(Path folder){ return read.apply(folder); }
    @Override public Path stdLibBase(){ return LocalResources.stLibPath; }
    @Override public Path baseCache(){ return classes.resolve("baseCache"); }
    @Override public void associate(List<Icon> captured, List<Project.Claimant> preloads, Function<String,String> claimedBy){
      captured.forEach(i -> Fs.of(() -> Files.readAllBytes(i.png())));
      preloads.forEach(c -> Fs.of(() -> Files.readAllBytes(c.icon(".png"))));
      associated.add(captured);
      windows.add(preloads.stream().map(c -> c.label()+" "+c.claim().icon()).toList());
      var all= Stream.concat(Stream.of(new Icon(".fearless", null, null)), captured.stream()).toList();
      var userLocked= all.stream().map(Icon::extension).filter(locked::contains).toList();
      if (!userLocked.isEmpty()){ throw Violation.associationUserLocked(userLocked, claimedBy); }
      var blocked= all.stream().map(Icon::extension).filter(e -> !Icon.system(e) && held.containsKey(e)).collect(Collectors.toMap(e -> e, held::get, (a,_) -> a, LinkedHashMap::new));
      if (!blocked.isEmpty()){ throw Violation.associationNotOurs(blocked, claimedBy); }
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
  private final List<String> locked= new ArrayList<>();
  private final List<Path> connected= new ArrayList<>();
  private final List<Registry.Grants> granted= Collections.synchronizedList(new ArrayList<>());
  @AfterEach void nothingFailed(){ assertEquals(List.of(), failures); }
  private Manager manager(Path dir, String... mains){ return manager(dir, view, known(mains)); }
  private static Function<Path,Optional<MainsInfo>> known(String... mains){ return known(List.of(), mains); }
  private static Function<Path,Optional<MainsInfo>> known(List<String> attenuators, String... mains){
    var main= new MainsInfo.Main("_hello/_rank_app.fear", List.of(), List.of(), List.of());
    var known= Optional.of(new MainsInfo(Stream.of(mains).collect(Collectors.toMap(m -> m, _ -> main)), attenuators));
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
    return new Manager(dir.resolve("manager"), new Fake(read, infos, associated, windows, held, locked, connected, granted), v, exit);
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
  private static void send(Manager m, String... lines){
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
  private static String info(Path dir){ return Fs.readUtf8(dir.resolve("manager").resolve("projects.info")); }
  private static Path edit(Manager m, Path dir, String text){
    var file= dir.resolve("edited.info");
    Fs.writeUtf8(file, text);
    send(m, "EditMetadata", TaggedText.of(file.toString()));
    return file;
  }
  private static void allow(Manager m, Path dir, String... extensions){
    var old= Registry.parse(info(dir), dir.resolve("projects.info"), p -> p, dir.resolve("manager"));
    edit(m, dir, Registry.text(new Registry.Content(old.projects(), Stream.concat(old.extensions().stream(), Stream.of(extensions)).toList())));
  }
  @Test void showShowsTheWindow(@TempDir Path dir){
    var m= manager(dir);
    send(m, "Show");
    assertEquals(1, view.shown);
    assertEquals(List.of(), listed(m));
    assertEquals("{\n  \"projects\": {},\n  \"systemExtensions\": []\n}\n", info(dir));
  }
  @Test void quitAndResetAreForTheProcess(@TempDir Path dir){
    var m= manager(dir);
    send(m, "Quit");
    send(m, "Reset");
    assertEquals(List.of("quit", "reset"), exits);
  }
  @Test void anInvalidCallIsShownAndAFatalErrorIsFatal(@TempDir Path dir){
    var m= manager(dir);
    send(m, "InvalidCall", "Fearless was started with 2 arguments,", "and more.");
    assertEquals(List.of("Fearless was started with 2 arguments,\nand more."), view.refused);
    send(m, "FatalError", "the disk is gone");
    assertEquals(1, failures.size());
    same("A Fearless process reported a failure the manager can not recover from:\n\nthe disk is gone", failures.getFirst().getMessage());
    failures.clear();
  }
  @Test void aFolderNamedWithANewlineOrNotUnicodeIsRegisteredByItsMessageAndRemembered(@TempDir Path dir){
    var m= manager(dir);
    var odd= folder(dir, Fs.isWindows() ? "a\ud800b" : "a\nb");
    send(m, "Register", TaggedText.line(odd.toString()));
    assertEquals(List.of("a_b "+odd), listed(m));
    same(Fs.isWindows() ? "[###]\"path\": \"Base16:[###]610000D86200\",[###]" : "[###]\"path\": \"Str:[###]/a\\nb\",[###]", info(dir));
    var again= manager(dir);
    again.settle();
    assertEquals(Kind.idle, project(again, odd).kind());
  }
  @Test void registeringAFolderMakesItAnIdleProjectNamedByItsMarker(@TempDir Path dir){
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
    assertEquals(1, view.shown);
    assertEquals(List.of(), view.refused);
  }
  @Test void aSecondFolderNamedComIsNamedWithTheFirstValidFreeName(@TempDir Path dir){
    var m= manager(dir);
    var first= folder(dir.resolve("a"), "com");
    var second= folder(dir.resolve("b"), "com");
    send(m, "Register", TaggedText.of(first.toString()));
    send(m, "Register", TaggedText.of(second.toString()));
    assertEquals(List.of("com "+first, "com10 "+second), listed(m));
    assertTrue(Files.isRegularFile(second.resolve("com10.fearless")));
    assertEquals(List.of(), view.refused);
  }
  @Test void aFileRegistersTheFolderItIsInAndRegisteringAgainSelects(@TempDir Path dir){
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
  @Test void aCompileThenARunOfTheOnlyMain(@TempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    assertEquals(Project.State.codeNoCache, project(m, hello).state());
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
    assertEquals(List.of(), project(m, hello).entry().mains());
    assertFalse(Files.exists(hello.resolve(Facts.outDir).resolve("compile_error.txt")));
  }
  @Test void aRunNeverCompiles(@TempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    send(m, "Run", "hello", "hello.Hello");
    assertEquals(List.of("Refused request \"Run\" on project \"hello\".\nThe state of project \"hello\" is \"code: not compiled (no cache)\": only a project in state \"code: compiled\" runs a main."), view.refused);
    assertEquals("", view.out(hello));
    assertFalse(Files.exists(hello.resolve(Facts.outDir)));
  }
  @Test void aFailingCompileWritesItsErrorAndASucceedingOneDeletesIt(@TempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    infos.put("hello", "fail");
    compiled(m, "hello");
    same("--- compiling hello ---\nbroken source\n--- compile failed with 1 ---\n", view.out(hello));
    var error= hello.resolve(Facts.outDir).resolve("compile_error.txt");
    assertEquals("broken source\n", Fs.readUtf8(error));
    assertEquals(Project.State.codeNoCache, project(m, hello).state());
    assertEquals(Optional.empty(), project(m, hello).problem());
    infos.remove("hello");
    compiled(m, "hello");
    assertFalse(Files.exists(error));
  }
  @Test void compilingAnUpToDateProjectStartsNoJob(@TempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    compiled(m, "hello");
    compiled(m, "hello");
    assertEquals(List.of(hello), view.cleared);
    assertEquals(known("hello.Hello").apply(hello), project(m, hello).mains());
  }
  @Test void aBusyProjectRefusesJobsDeleteCacheKindAndForgetThenTerminateEndsTheJob(@TempDir Path dir){
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
    send(m, "Kind", "hello", "idle");
    send(m, "Forget", "hello");
    send(m, "Terminate", "hello");
    idle(m);
    same("""
      --- running hello.Slow ---
      ran hello.Slow
      --- terminating hello.Slow ---
      --- hello.Slow exited with [###] after [###]s ---
      """, view.out(hello));
    assertEquals(List.of(
      "Refused request \"Run\" on project \"hello\".\nMain \"hello.Slow\" is currently running inside project \"hello\".",
      "Refused request \"Compile\" on project \"hello\".\nMain \"hello.Slow\" is currently running inside project \"hello\".",
      "Refused request \"DeleteCache\" on project \"hello\".\nMain \"hello.Slow\" is currently running inside project \"hello\".",
      "Refused request to change the kind of project \"hello\" to \"idle\".\nMain \"hello.Slow\" is currently running inside project \"hello\".",
      "Refused request to remove project \"hello\".\nMain \"hello.Slow\" is currently running inside project \"hello\"."), view.refused);
    assertEquals(Kind.code, project(m, hello).kind());
  }
  @Test void terminateEndsTheWholeRunNotOnlyTheCurrentMain(@TempDir Path dir){
    var m= manager(dir, "hello.Slow", "hello.Two");
    var hello= code(m, dir, "hello");
    compiled(m, "hello");
    send(m, "Run", "hello", "hello.Two hello.Slow");
    until(m, _ -> view.out(hello).contains("ran hello.Slow"));
    send(m, "Terminate", "hello");
    idle(m);
    assertFalse(view.out(hello).contains("hello.Two"));
  }
  @Test void theNamedMainsRunInTheOrderOfTheMainsAndBecomeTheSelectedMains(@TempDir Path dir){
    var m= manager(dir, "hello.One", "hello.Two", "hello.Three");
    var hello= code(m, dir, "hello");
    compiled(m, "hello");
    assertEquals(List.of(), project(m, hello).selectedMains());
    send(m, "Run", "hello", "hello.Three hello.One");
    idle(m);
    same("""
      --- running hello.One ---
      ran hello.One
      --- hello.One exited with 0 after [###]s ---
      --- running hello.Three ---
      ran hello.Three
      --- hello.Three exited with 0 after [###]s ---
      """, view.out(hello));
    assertEquals(List.of("hello.One", "hello.Three"), project(m, hello).selectedMains());
    same("[###]\"mains\": [\"hello.One\", \"hello.Three\"][###]", info(dir));
  }
  @Test void aRunNamingNoMainOrAnUnknownOneIsRefused(@TempDir Path dir){
    var m= manager(dir, "hello.One", "hello.Two");
    var hello= code(m, dir, "hello");
    compiled(m, "hello");
    send(m, "Run", "hello");
    send(m, "Run", "hello", "");
    send(m, "Run", "hello", "hello.Nope hello.One");
    var none= """
      Refused request "Run" on project "hello".
      The request names no main.
      The mains are:
        hello.One
        hello.Two""";
    assertEquals(List.of(none, none, """
      Refused request "Run" on project "hello".
      The request names "hello.Nope", and "hello.Nope" is not a main of project "hello".
      The mains are:
        hello.One
        hello.Two"""), view.refused);
    assertEquals(List.of(hello), view.cleared);
  }
  @Test void forgettingAProjectForgetsItAndLeavesItsFolder(@TempDir Path dir){
    var m= manager(dir);
    var hello= registered(m, dir, "hello");
    send(m, "Forget", "hello");
    assertEquals(List.of(), listed(m));
    assertEquals(Optional.empty(), m.state().selected());
    send(m, "Show", "hello");
    assertEquals("Refused request \"Show\" on project \"hello\".\nNo project is named \"hello\".\nNo project is registered.", view.refused.getLast());
    assertTrue(Files.isRegularFile(hello.resolve("hello.fearless")));
  }
  @Test void aRequestNamesARegisteredProject(@TempDir Path dir){
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
  @Test void aRequestTheManagerDoesNotKnowOrWithTheWrongArgumentsIsRefused(@TempDir Path dir){
    var m= manager(dir);
    registered(m, dir, "hello");
    send(m, "build", "hello");
    send(m, "Kind", "hello", "library");
    send(m, "Link", "hello", "data sometimes");
    send(m, "Forget", "hello", "now");
    send(m, "Run");
    assertEquals(5, view.refused.size());
    assertEquals("Refused request \"build\".\n\"build\" is not a request of the manager: the requests are \"Show\", \"Quit\", \"Reset\", \"Register\", \"ConnectEclipse\", \"EditMetadata\", \"OpenWithOrShortcut\", \"Check\", \"Terminate\", \"DeleteCache\", \"Forget\", \"Run\", \"Compile\", \"Kind\", \"Link\", \"Attenuate\".", view.refused.get(0));
    assertEquals("Refused request \"Kind\" on project \"hello\".\n\"library\" is not a kind: the kinds are \"idle\", \"code\", \"data:readOnly\" and \"data:readWrite\".", view.refused.get(1));
    assertEquals("Refused request \"Link\" on project \"hello\".\n\"data sometimes\" is not a link: a link is the name of a data project, then \"read\" or \"write\", then the type names the code uses for it, none to remove the link.", view.refused.get(2));
    assertEquals("Refused request \"Forget\" with 2 arguments.\nRequest \"Forget\" takes 1 argument.\nThe arguments are:\n  \"hello\"\n  \"now\"", view.refused.get(3));
    assertEquals("Refused request \"Run\" with 0 arguments.\nRequest \"Run\" takes 1 or 2 arguments.\nThe arguments are:\n  <none>", view.refused.get(4));
    assertEquals(Kind.idle, m.state().projects().getFirst().kind());
  }
  @Test void aRequestWhoseResultIsNotAValidProjectsInfoIsRefusedAndChangesNothing(@TempDir Path dir){
    var m= manager(dir);
    var hello= code(m, dir, "hello");
    send(m, "Register", TaggedText.of(data(dir).toString()));
    send(m, "Kind", "data", "data:readOnly");
    var before= info(dir);
    send(m, "Link", "hello", "data read One\tTwo");
    send(m, "Link", "hello", "data read Ärger");
    assertEquals(before, info(dir));
    assertEquals(Map.of(), project(m, hello).entry().reads());
    assertEquals(2, view.refused.size());
    same("In file: [###]projects.info\n\n[###]The character [Tab 0x09] is outside the safe character set of Fearless[###]", view.refused.get(0));
    same("[###]The character [U+00C4] is outside the safe character set of Fearless[###]", view.refused.get(1));
  }
  @Test void aMessageFileThatIsNotUtf8IsRefusedAndRemovedAndTheNextStartDrainsNothing(@TempDir Path dir){
    var m= manager(dir);
    var messages= folder(dir.resolve("manager"), "messages");
    Fs.ofV(() -> Files.write(messages.resolve("1.msg"), new byte[]{'R', 'u', 'n', '\n', (byte)0xFF}));
    Fs.writeUtf8(messages.resolve("2.msg"), "Show");
    m.drain();
    m.settle();
    same("The manager refused a message, and removed its file: a message is UTF-8 text the manager can read.\n[###]1.msg[###]not form valid UTF-8 text.", view.refused.getFirst());
    assertEquals(1, view.refused.size());
    assertEquals(2, view.shown);
    assertEquals(List.of(), Names.list(messages));
    var again= manager(dir);
    again.drain();
    again.settle();
    assertEquals(1, view.refused.size());
  }
  @Test void aFolderInsideARegisteredOneIsRefused(@TempDir Path dir){
    var m= manager(dir);
    var hello= registered(m, dir, "hello");
    send(m, "Register", TaggedText.of(folder(hello, "inner").toString()));
    same("Refused request \"Register\" on path\n[###]One of the two folders is inside the other[###]", view.refused.getFirst());
    assertEquals(List.of("hello "+hello), listed(m));
  }
  @Test void aLinkToARegisteredFolderOnlySelectsItAndALinkIntoOneIsRefused(@TempDir Path dir){
    var m= manager(dir);
    var hello= registered(m, dir, "hello");
    var other= registered(m, dir, "other");
    var link= Disk.link(dir.resolve("link"), hello);
    var inner= Disk.link(dir.resolve("inner"), folder(hello, "inner"));
    send(m, "Register", TaggedText.of(link.toString()));
    assertEquals(Optional.of(hello), m.state().selected());
    send(m, "Register", TaggedText.of(inner.toString()));
    assertEquals(List.of("hello "+hello, "other "+other), listed(m));
    assertEquals(1, view.refused.size());
    same("[###]A registered project has the folder\n[###]hello\nOne of the two folders is inside the other[###]", view.refused.getFirst());
  }
  @Test void kindsChangeToAnyKind(@TempDir Path dir){
    var m= manager(dir);
    var data= data(dir);
    send(m, "Register", TaggedText.of(data.toString()));
    assertEquals(Kind.idle, project(m, data).kind());
    send(m, "Kind", "data", "data:readOnly");
    send(m, "Kind", "data", "code");
    assertEquals(Kind.code, project(m, data).kind());
    send(m, "Kind", "data", "data:readWrite");
    send(m, "Kind", "data", "data:readWrite");
    assertEquals(Kind.dataReadWrite, project(m, data).kind());
    send(m, "Kind", "data", "idle");
    assertEquals(Kind.idle, project(m, data).kind());
    assertEquals(List.of(), view.refused);
    assertEquals(List.of(), view.cleared);
  }
  @Test void onlyACodeProjectCompilesAndRunsAndAnyProjectIsChecked(@TempDir Path dir){
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
    assertEquals("The project \"data\" is valid, and up to date.\n", view.out(data));
    Fs.writeUtf8(data.resolve("Bad Name.txt"), "");
    send(m, "Check", "data");
    same("[###]Bad Name.txt[###]", view.out(data));
    var checkError= data.resolve(Facts.outDir).resolve("check_error.txt");
    same("[###]Bad Name.txt[###]", Fs.readUtf8(checkError));
    assertEquals(Project.State.dataInvalid, project(m, data).state());
    assertEquals(Project.Action.check, project(m, data).action());
    Fs.ofV(() -> Files.delete(data.resolve("Bad Name.txt")));
    send(m, "Check", "data");
    assertEquals("The project \"data\" is valid, and up to date.\n", view.out(data));
    assertFalse(Files.exists(checkError));
    var hello= code(m, dir, "hello");
    send(m, "Check", "hello");
    assertEquals("This project has no compilation cache.\n", view.out(hello));
  }
  @Test void linksAreEditedByRequestAndBrokenLinksStopTheCompile(@TempDir Path dir){
    var m= manager(dir);
    var hello= code(m, dir, "hello");
    var data= data(dir);
    send(m, "Register", TaggedText.of(data.toString()));
    send(m, "Kind", "data", "data:readWrite");
    send(m, "Link", "hello", "data write Data");
    send(m, "Link", "hello", "data read Pub");
    var entry= project(m, hello).entry();
    assertEquals(List.of(Map.of("data", List.of("Pub")), Map.of("data", List.of("Data"))), List.of(entry.reads(), entry.edits()));
    assertEquals(Optional.empty(), project(m, hello).problem());
    send(m, "Link", "hello", "data read lower");
    same("[###]\"lower\" in \"reads\".\"data\" is not a Fearless type name[###]", view.refused.getLast());
    send(m, "Link", "hello", "data read Data");
    same("[###]\"Data\" is in both \"reads\".\"data\" and \"edits\".\"data\"[###]", view.refused.getLast());
    assertEquals(entry, project(m, hello).entry());
    send(m, "Link", "data", "hello read Hello");
    assertEquals(Map.of("hello", List.of("Hello")), project(m, data).entry().reads());
    assertEquals(Project.State.dataReadWrite, project(m, data).state());
    send(m, "Kind", "data", "idle");
    assertEquals(Project.State.codeInvalid, project(m, hello).state());
    send(m, "Compile", "hello");
    assertEquals("Refused request \"Compile\" on project \"hello\".\nThe content of project \"hello\" is invalid:\n\"reads\" refers to \"data\", but the kind of \"data\" is \"idle\"; \"reads\" accepts only \"data:readOnly\" or \"data:readWrite\".", view.refused.getLast());
    send(m, "Link", "hello", "data read");
    send(m, "Link", "hello", "data write");
    assertEquals(List.of(Map.of(), Map.of()), List.of(project(m, hello).entry().reads(), project(m, hello).entry().edits()));
    assertEquals(Project.State.codeNoCache, project(m, hello).state());
  }
  @Test void attenuatorsAreSetByRequestAndARunStartsEachMainUnderItsOwn(@TempDir Path dir){
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
    send(m, "Attenuate", "hello", "");
    assertEquals("Refused request \"Attenuate\" on project \"hello\".\nThe request names no main: an attenuation is the name of a main, then the attenuators applied to it in order, none to remove them all.", view.refused.getLast());
    var data= data(dir);
    send(m, "Register", TaggedText.of(data.toString()));
    send(m, "Attenuate", "data", "hello.One hello.NoGui");
    assertEquals(Map.of("hello.One", List.of("hello.NoGui")), project(m, data).entry().attenuators());
    assertEquals(2, view.refused.size());
    assertEquals(Map.of("hello.One", List.of("hello.NoNet", "hello.NoGui")), project(m, hello).entry().attenuators());
  }
  @Test void aRunIsRefusedWhileAnAttenuatedMainOrAnAppliedAttenuatorIsNotOfTheProject(@TempDir Path dir){
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
  @Test void aRunningMainKeepsTheDataProjectsOfItsLinksUntilItEnds(@TempDir Path dir){
    var m= manager(dir, "hello.Slow");
    var hello= code(m, dir, "hello");
    var data= data(dir);
    send(m, "Register", TaggedText.of(data.toString()));
    send(m, "Kind", "data", "data:readWrite");
    send(m, "Link", "hello", "data write Notes Diary");
    send(m, "Link", "hello", "data read Pub");
    compiled(m, "hello");
    send(m, "Run", "hello", "hello.Slow");
    until(m, _ -> view.out(hello).contains("ran hello.Slow"));
    var folder= project(m, data).folder();
    assertEquals(List.of(new Registry.Grants(Map.of("Pub", folder), Map.of("Notes", folder, "Diary", folder))), granted);
    var before= info(dir);
    send(m, "Link", "hello", "data read Old");
    send(m, "Kind", "data", "idle");
    send(m, "Forget", "data");
    edit(m, dir, before.replace("\"Diary\"", "\"Journal\""));
    assertEquals(before, info(dir));
    assertEquals(Collections.nCopies(4, "Refused request to change the data projects that project \"hello\" reads or writes.\nMain \"hello.Slow\" is currently running inside project \"hello\".\nThe data projects of a main stay the same while the main runs."), view.refused);
    send(m, "Link", "hello", "data write Notes Diary");
    send(m, "Check", "data");
    assertEquals(4, view.refused.size());
    send(m, "Terminate", "hello");
    idle(m);
    send(m, "Link", "hello", "data read Old");
    assertEquals(Map.of("data", List.of("Old")), project(m, hello).entry().reads());
  }
  @Test void theMetadataIsEditedWholeOrNotAtAll(@TempDir Path dir){
    var m= manager(dir);
    var data= data(dir);
    send(m, "Register", TaggedText.of(data.toString()));
    edit(m, dir, "{\"projects\": {\"data\": {\"path\": \"Str:nowhere\"}}, \"systemExtensions\": []}");
    same("In file: [###]edited.info[###]\"path\" must be an absolute path[###]", view.refused.getLast());
    assertEquals(List.of(), view.edited);
    var good= edit(m, dir, Registry.text(new Registry.Content(List.of(project(m, data).entry().withKind(Kind.dataReadWrite)), List.of())));
    assertEquals(List.of(good), view.edited);
    assertEquals(Kind.dataReadWrite, project(m, data).kind());
    edit(m, dir, "{\"projects\": {}, \"systemExtensions\": []}");
    assertEquals(List.of(), listed(m));
    send(m, "EditMetadata", TaggedText.of(dir.resolve("none.info").toString()));
    same("Refused request \"EditMetadata\" on path\n[###]none.info\nNo file is at that path.", view.refused.getLast());
  }
  /// Forget, Kind and Link are EditMetadata with projects.info changed in one place: the same outcome, down to the refusal.
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
    edit(b.m, b.dir, edited.apply(Registry.parse(info(b.dir), managerDir.resolve("projects.info"), p -> p, managerDir)));
    var outcomes= List.of(outcome(a), outcome(b));
    for (var t : List.of(a, b)){
      t.m.state().projects().stream().filter(Project::busy).forEach(p -> send(t.m, "Terminate", p.alias()));
      idle(t.m);
    }
    assertEquals(outcomes.get(0), outcomes.get(1).stream().map(s -> s.replace("DIR/edited.info", "DIR/manager/projects.info")).toList());
  }
  private static String without(Registry.Content c, String name){ return Registry.text(new Registry.Content(c.projects().stream().filter(e -> !e.alias().equals(name)).toList(), c.extensions())); }
  private static String with(Registry.Content c, String name, UnaryOperator<Registry.Entry> op){ return Registry.text(new Registry.Content(c.projects().stream().map(e -> e.alias().equals(name) ? op.apply(e) : e).toList(), c.extensions())); }
  private static void linked(Manager m, Path dir){
    code(m, dir, "hello");
    send(m, "Register", TaggedText.of(data(dir).toString()));
    send(m, "Kind", "data", "data:readWrite");
  }
  private static void running(Manager m, Path dir){
    linked(m, dir);
    send(m, "Link", "hello", "data read Pub");
    compiled(m, "hello");
    send(m, "Run", "hello", "hello.Slow");
    until(m, s -> s.running().equals(List.of("hello - hello.Slow")));
  }
  private static final String[] none= {};
  private static final String[] slow= {"hello.Slow"};
  @Test void forgetIsEditMetadataWithTheProjectRemoved(@TempDir Path dir){
    sameAsEditMetadata(dir, none, (m, d) -> registered(m, d, "hello"), c -> without(c, "hello"), "Forget", "hello");
  }
  @Test void forgetOfABusyProjectIsEditMetadataWithTheProjectRemoved(@TempDir Path dir){
    sameAsEditMetadata(dir, slow, ManagerTest::running, c -> without(c, "hello"), "Forget", "hello");
  }
  @Test void kindFromIdleIsEditMetadataWithTheKindReplaced(@TempDir Path dir){
    sameAsEditMetadata(dir, none, (m, d) -> registered(m, d, "data"), c -> with(c, "data", e -> e.withKind(Kind.dataReadOnly)), "Kind", "data", "data:readOnly");
  }
  @Test void kindNotFromIdleIsEditMetadataWithTheKindReplaced(@TempDir Path dir){
    sameAsEditMetadata(dir, none, (m, d) -> { registered(m, d, "data"); send(m, "Kind", "data", "data:readOnly"); }, c -> with(c, "data", e -> e.withKind(Kind.code)), "Kind", "data", "code");
  }
  @Test void kindOfABusyProjectIsEditMetadataWithTheKindReplaced(@TempDir Path dir){
    sameAsEditMetadata(dir, slow, ManagerTest::running, c -> with(c, "hello", e -> e.withKind(Kind.idle)), "Kind", "hello", "idle");
  }
  @Test void linkIsEditMetadataWithTheLinkReplaced(@TempDir Path dir){
    sameAsEditMetadata(dir, none, ManagerTest::linked, c -> with(c, "hello", e -> e.withLinks(e.reads(), Map.of("data", List.of("Data")))), "Link", "hello", "data write Data");
  }
  @Test void linkWithNoTypeNamesIsEditMetadataWithTheLinkRemoved(@TempDir Path dir){
    sameAsEditMetadata(dir, none, (m, d) -> { linked(m, d); send(m, "Link", "hello", "data read Pub"); }, c -> with(c, "hello", e -> e.withLinks(Map.of(), e.edits())), "Link", "hello", "data read");
  }
  @Test void linkWithABadTypeNameIsEditMetadataWithThatLink(@TempDir Path dir){
    sameAsEditMetadata(dir, none, ManagerTest::linked, c -> with(c, "hello", e -> e.withLinks(Map.of("data", List.of("lower")), e.edits())), "Link", "hello", "data read lower");
  }
  @Test void linkOfADataProjectIsEditMetadataWithThatLink(@TempDir Path dir){
    sameAsEditMetadata(dir, none, ManagerTest::linked, c -> with(c, "data", e -> e.withLinks(Map.of("hello", List.of("Hello")), e.edits())), "Link", "data", "hello read Hello");
  }
  @Test void attenuateOfADataProjectIsEditMetadataWithTheAttenuatorsReplaced(@TempDir Path dir){
    sameAsEditMetadata(dir, none, ManagerTest::linked, c -> with(c, "data", e -> e.withAttenuators(Map.of("hello.Hello", List.of("base.NoManager")))), "Attenuate", "data", "hello.Hello base.NoManager");
  }
  @Test void attenuateIsEditMetadataWithTheAttenuatorsReplaced(@TempDir Path dir){
    sameAsEditMetadata(dir, none, ManagerTest::linked, c -> with(c, "hello", e -> e.withAttenuators(Map.of("hello.Hello", List.of("hello.NoGui", "base.NoManager")))), "Attenuate", "hello", "hello.Hello hello.NoGui base.NoManager");
  }
  @Test void attenuateWithNoAttenuatorIsEditMetadataWithTheMainRemoved(@TempDir Path dir){
    sameAsEditMetadata(dir, none, (m, d) -> { linked(m, d); send(m, "Attenuate", "hello", "hello.Hello hello.NoGui"); }, c -> with(c, "hello", e -> e.withAttenuators(Map.of())), "Attenuate", "hello", "hello.Hello");
  }
  @Test void linkWhileAMainRunsIsEditMetadataWithThatLink(@TempDir Path dir){
    sameAsEditMetadata(dir, slow, ManagerTest::running, c -> with(c, "hello", e -> e.withLinks(Map.of("data", List.of("Old")), e.edits())), "Link", "hello", "data read Old");
  }
  @Test void aProjectWhoseFolderIsGoneIsInvalidAndCanBeForgotten(@TempDir Path dir){
    var m= manager(dir);
    var data= data(dir);
    send(m, "Register", TaggedText.of(data.toString()));
    Fs.rmTree(data);
    send(m, "Show", "data");
    assertEquals(Optional.of(data), m.state().selected());
    assertEquals(List.of(), view.refused);
    send(m, "Check", "data");
    assertTrue(view.out(data).startsWith("The folder of this project does not exist:"));
    send(m, "Forget", "data");
    assertEquals(List.of(), listed(m));
    send(m, "Register", TaggedText.of(data.toString()));
    same("Refused request \"Register\" on path\n[###]data\nNothing exists at that path.", view.refused.getLast());
    assertEquals(List.of(), listed(m));
  }
  @Test void everyRequestNamingAProjectChecksItWhileTheWindowIsHidden(@TempDir Path dir){
    var m= manager(dir);
    var data= data(dir);
    send(m, "Register", TaggedText.of(data.toString()));
    view.visible = false;
    for (var request : List.of(List.of("Link", "data", "other read Other"), List.of("Show", "data"), List.of("Terminate", "data"), List.of("Kind", "data", "library"))){
      var valid= Files.exists(data);
      if (valid){ Fs.rmTree(data); }
      else{ data(dir); Fs.writeUtf8(data.resolve("data.fearless"), ""); }
      send(m, request.toArray(String[]::new));
      assertEquals(valid ? Project.State.dataInvalid : Project.State.idle, project(m, data).state());
    }
  }
  @Test void foldersTheManagerCanNotReadOrChangeAreRefusedOrInvalidNeverFatal(@TempDir Path dir){
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
    send(m, "ConnectEclipse", TaggedText.of(closed.toString()));
    same("""
      Refused request "Register" on path
      [###]closed
      The folder can not be read or changed: access denied: [###]closed[###]""", view.refused.get(0));
    same("[###]readOnly\nThe folder can not be read or changed: access denied: [###]readOnly[###]", view.refused.get(1));
    same("[###]data\nThe folder can not be read or changed: access denied: [###]a.built[###]", view.refused.get(2));
    same("Eclipse is not connected from\n[###]closed\naccess denied: [###]closed", view.refused.get(3));
    assertEquals(List.of("partly "+partly), listed(m));
    same("""
      The folder of this project can not be read:
      [###]partly
      access denied: [###]hidden
      Give Fearless access to it, or forget this project.""", project(m, partly).problem().orElseThrow());
    var again= manager(dir);
    again.settle();
    assertEquals(Project.State.dataInvalid, project(again, partly).state());
    Disk.allow(cache);
    send(again, "Register", TaggedText.of(cached.toString()));
    send(again, "Kind", "data", "code");
    Fs.writeUtf8(cache.resolve("a.built"), "");
    Disk.noWrite(cache);
    send(again, "DeleteCache", "data");
    same("--- the compilation cache is not deleted: access denied: [###]a.built ---\n", view.out(cached));
    List.of(closed, readOnly, cache, hidden).forEach(Disk::allow);
  }
  @Test void aManagerFolderThatCanNotBeWrittenIsFatalForEveryChangeAndARefusalWritesNothing(@TempDir Path dir){
    var m= manager(dir);
    var hello= code(m, dir, "hello");
    var managerDir= dir.resolve("manager");
    var before= info(dir);
    send(m, "Link", "hello", "data read lower");
    same("[###]\"lower\" in \"reads\".\"data\" is not a Fearless type name[###]", view.refused.getLast());
    assertEquals(before, info(dir));
    Disk.noWrite(managerDir);
    Assumptions.assumeFalse(Files.isWritable(managerDir));
    send(m, "Link", "hello", "data read Data");
    send(m, "Kind", "hello", "idle");
    edit(m, dir, "{\"projects\": {}, \"systemExtensions\": []}");
    Disk.allow(managerDir);
    assertEquals(3, failures.size());
    failures.forEach(f -> same("Fearless could not save the file \"projects.info\" of its manager folder.[###]", f.getMessage()));
    failures.clear();
    assertEquals(1, view.refused.size());
    assertEquals(before, info(dir));
  }
  @Test void aMessageFileTheManagerCanNotReadIsFatal(@TempDir Path dir){
    var m= manager(dir);
    var msg= folder(dir.resolve("manager"), "messages").resolve("1.msg");
    Fs.writeUtf8(msg, "");
    Disk.noRead(msg);
    Assumptions.assumeFalse(Files.isReadable(msg));
    Errs.err("Fearless could not list its manager folder, or could not read or\nremove a message file in it.[###]1.msg[###]", m::drain);
    Disk.allow(msg);
  }
  @Test void aRequestNamingNoProjectOrPathIsRefused(@TempDir Path dir){
    var m= manager(dir);
    send(m, "Run", "");
    send(m, "Register", "");
    send(m, "Register", "Str: ");
    assertEquals(List.of(
      "Refused request \"Run\" on project \"\".\nNo project is named \"\".\nNo project is registered.",
      "Refused request \"Register\".\nThe path of the request is not a tagged text: \"\" is malformed: it starts with \"Str:\", \"UStr:\" or \"Base16:\", then the text written that way.",
      "Refused request \"Register\".\nThe path of the request is empty."), view.refused);
  }
  @Test void aRequestWhosePathIsRelativeIsRefused(@TempDir Path dir){
    var m= manager(dir);
    send(m, "Register", "Str:hello");
    send(m, "OpenWithOrShortcut", "Str:notes.htm");
    assertEquals(List.of(
      "Refused request \"Register\" on path \"hello\".\nPath \"hello\" is relative: a request names a path in full, from the root of its drive or file system.",
      "Refused request \"OpenWithOrShortcut\" on path \"notes.htm\".\nPath \"notes.htm\" is relative: a request names a path in full, from the root of its drive or file system."), view.refused);
    assertEquals(List.of(), listed(m));
  }
  @Test void aRegisterWhosePathIsNotATaggedTextOrNotAPathIsRefused(@TempDir Path dir){
    var m= manager(dir);
    send(m, "Register", dir.toString());
    send(m, "Register", TaggedText.of("a\u0000b"));
    assertEquals(2, view.refused.size());
    assertEquals("Refused request \"Register\".\nThe path of the request is not a tagged text: \""+dir+"\" is malformed: it starts with \"Str:\", \"UStr:\" or \"Base16:\", then the text written that way.", view.refused.get(0));
    same("Refused request \"Register\" on path \"a\" [Null 0x00] \"b\".\n\"a\" [Null 0x00] \"b\" is not a path this system accepts: "+(Fs.isWindows() ? "[###]" : "Nul character not allowed."), view.refused.get(1));
    assertEquals(List.of(), listed(m));
  }
  @Test void aProjectsInfoThatIsNotValidIsFatal(@TempDir Path dir){
    var m= manager(dir);
    registered(m, dir, "hello");
    var file= dir.resolve("manager").resolve("projects.info");
    Fs.writeUtf8(file, info(dir).replace("\"kind\": \"idle\"", "\"kind\": \"library\""));
    Errs.err("In file: [###]projects.info[###]\"kind\" must be one of \"idle\", \"code\", \"data:readOnly\" or \"data:readWrite\", not \"library\".[###]", () -> manager(dir));
  }
  @Test void compilingForgetsSelectedMainsThatAreGone(@TempDir Path dir){
    var mains= new ArrayList<>(List.of("hello.One", "hello.Two", "hello.Old"));
    var m= manager(dir, f -> known(mains.toArray(String[]::new)).apply(f));
    var hello= code(m, dir, "hello");
    compiled(m, "hello");
    send(m, "Run", "hello", "hello.Old hello.Two");
    idle(m);
    assertEquals(List.of("hello.Old", "hello.Two"), project(m, hello).entry().mains());
    mains.remove("hello.Old");
    FactsTest.cache(hello, "hello", 1000);
    compiled(m, "hello");
    assertEquals(List.of("hello.Two"), project(m, hello).entry().mains());
  }
  @Test void mainsThatCanNotBeReadMakeTheProjectOutOfDateAndAreNotReadAgainWhileNothingChanges(@TempDir Path dir){
    var reads= new ArrayList<Path>();
    var m= manager(dir, f -> { reads.add(f); throw Report.launchPathNotFound(dir.resolve("gone")); });
    var hello= code(m, dir, "hello");
    compiled(m, "hello");
    send(m, "Show", "hello");
    send(m, "Check", "hello");
    assertEquals(List.of(hello), reads);
    var p= project(m, hello);
    var error= Report.launchPathNotFound(dir.resolve("gone")).getMessage();
    assertEquals(Project.State.codeOutdated, p.state());
    assertEquals(Project.Action.compile, p.action());
    assertEquals(Optional.empty(), p.problem());
    assertEquals(error.stripTrailing()+"\n", view.out(hello));
  }
  @Test void aCompileWhoseJvmDoesNotStartIsShownOnItsProject(@TempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var noJvm= code(m, dir, "nojvm");
    send(m, "Compile", "nojvm");
    assertEquals("--- compiling nojvm ---\n--- compile did not start: io: Cannot run program \"java\": error=2, No such file or directory ---\n", view.out(noJvm));
    assertEquals(Project.State.codeNoCache, project(m, noJvm).state());
  }
  @Test void mainsTheCompilerWouldCompileMakeTheProjectOutOfDate(@TempDir Path dir){
    var m= manager(dir, _ -> Optional.empty());
    var hello= code(m, dir, "hello");
    compiled(m, "hello");
    var p= project(m, hello);
    assertEquals(Project.State.codeOutdated, p.state());
    assertEquals(Project.Action.compile, p.action());
    send(m, "Check", "hello");
    assertEquals("The compilation cache does not contain the mains of this project.\n", view.out(hello));
  }
  @Test void aProjectChangedWhileItsMainsAreReadSuccessfullyIsCheckedAgainWhileTheWindowIsHidden(@TempDir Path dir){
    var reads= new ArrayList<Path>();
    var m= manager(dir, f -> {
      reads.add(f);
      var src= f.resolve("_hello").resolve("_rank_app.fear");
      Fs.writeUtf8(src, Fs.readUtf8(src)+"//saved\n");
      Fs.ofV(() -> Files.setLastModifiedTime(src, FileTime.fromMillis(FactsTest.after(f))));
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
  @Test void aNewManagerRemembersTheProjects(@TempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    compiled(m, "hello");
    var again= manager(dir, "hello.Hello");
    again.settle();
    assertEquals(Project.State.codeCompiled, project(again, hello).state());
  }
  @Test void aFolderUnderAPathOutsideTheCharacterSetIsRegisteredRunAndRemembered(@TempDir Path dir){
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
  @Test void aPortFollowsTheJobOfARunOrACompileFromItsStartToItsEndAndAnyOtherRequestUntilItIsApplied(@TempDir Path dir){
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
      assertEquals("Applied\n", nothing.join());
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
  @Test void aPortWhereNobodyListensOrThatIsNotAPortIsRefused(@TempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    int closed;
    try (var server= port()){ closed= server.getLocalPort(); }
    catch (IOException e){ throw new UncheckedIOException(e); }
    send(m, "Compile", "hello", "Port:"+closed);
    send(m, "Compile", "hello", "Port:0");
    send(m, "Kind", "hello", "code", "Port:65536");
    same("The manager was given the port "+closed+", but no program of this machine accepts a connection there: connect: [###]", view.refused.get(0));
    assertEquals("The manager was given the port \"Port:0\", but a port is \"Port:\" then a number from 1 to 65535.", view.refused.get(1));
    assertEquals("The manager was given the port \"Port:65536\", but a port is \"Port:\" then a number from 1 to 65535.", view.refused.get(2));
    assertEquals(Project.State.codeNoCache, project(m, hello).state());
  }
  @Test void connectingEclipseNeedsAnEclipseFolder(@TempDir Path dir){
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
  private static String info(String main, String shortcuts, String openWiths){ return "{\""+main+"\": [\"_hello/_rank_app.fear\", ["+shortcuts+"], ["+openWiths+"], []]}\n"; }
  private static String claim(String icon, String diskPath, String zipSteps, String zipEntry, String ext){
    return Stream.of(icon, diskPath, zipSteps, zipEntry, ext).map(t -> "\""+t+"\"").collect(Collectors.joining(", ", "[", "]"));
  }
  private static Map<String,List<String>> claimed(Manager m){
    return Project.claimed(m.state().projects()).entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().stream().map(c -> c.label()+(c.shortcut() ? " shortcut " : " openWith ")+c.claim().icon()).toList()));
  }
  private static String fapp(String alias, String main, String icon){ return AutoselectTest.fapp(AutoselectTest.start(alias, main, icon)); }
  private static Path mainsInfo(Path project){ return project.resolve(Facts.outDir).resolve(project.getFileName()+"_manifest.info"); }
  private static String compileError(Path project){ return Fs.readUtf8(project.resolve(Facts.outDir).resolve("compile_error.txt")); }
  private static void touch(Path project){
    var src= project.resolve("_"+project.getFileName()).resolve("_rank_app.fear");
    Fs.ofV(() -> Files.setLastModifiedTime(src, FileTime.fromMillis(FactsTest.after(project))));
  }
  @Test void aCompileClaimsTheExtensionsOfItsMainsInfoAndANewManagerClaimsThemAgain(@TempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    allow(m, dir, "foo");
    assertEquals(Map.of(), claimed(m));
    infos.put("hello", claiming("hello.Hello", "", "foo"));
    compiled(m, "hello");
    var foo= Map.of("foo", List.of("hello::hello.Hello openWith hello.IconsFoo"), fapp("hello", "hello.Hello", "base.IconsConflict"), List.of("hello::hello.Hello shortcut base.IconsConflict"));
    assertEquals(foo, claimed(m));
    assertEquals(MainsInfo.read(hello).orElseThrow(), project(m, hello).claims());
    var again= manager(dir, "hello.Hello");
    again.settle();
    assertEquals(foo, claimed(again));
    assertEquals(Registry.text(new Registry.Content(List.of(project(again, hello).entry()), List.of("foo"))), info(dir));
    edit(again, dir, Registry.text(new Registry.Content(List.of(project(again, hello).entry().withKind(Kind.idle)), List.of("foo"))));
    assertEquals(Map.of(), claimed(again));
    assertEquals(Project.noClaims, project(again, hello).claims());
  }
  @Test void projectsClaimingTheSameExtensionAreAllItsClaimantsUntilCleanedIdleOrForgotten(@TempDir Path dir){
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
  @Test void aProjectWhoseMainsInfoIsMissingOrUnreadableClaimsNothing(@TempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    var other= code(m, dir, "other");
    allow(m, dir, "foo", "bar");
    infos.put("hello", claiming("hello.Hello", "fapp001", "foo"));
    infos.put("other", claiming("other.Other", "fapp002", "bar"));
    compiled(m, "hello", "other");
    assertEquals(List.of("bar", "fapp001", "fapp002", "foo"), claimed(m).keySet().stream().sorted().toList());
    Fs.ofV(() -> Files.delete(mainsInfo(hello)));
    Fs.writeUtf8(mainsInfo(other), FactsTest.manifest("{\"other.Other\": []}\n"));
    var again= manager(dir, "hello.Hello");
    again.settle();
    assertEquals(Map.of(), claimed(again));
    assertEquals(List.of(), view.refused);
  }
  @Test void aKindChangeBackToCodeReadsMainsInfoAndDropsTheCacheOfAnExtensionNotCaptured(@TempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    allow(m, dir, "foo");
    infos.put("hello", claiming("hello.Hello", "fapp001", "foo"));
    compiled(m, "hello");
    send(m, "Kind", "hello", "idle");
    assertEquals(Map.of(), claimed(m));
    send(m, "Kind", "hello", "code");
    assertEquals(Map.of("foo", List.of("hello::hello.Hello openWith hello.IconsFoo"), "fapp001", List.of("hello::hello.Hello shortcut base.IconsConflict")), claimed(m));
    send(m, "Kind", "hello", "idle");
    Fs.writeUtf8(mainsInfo(hello), FactsTest.manifest(claiming("hello.Hello", "fapp001", "bar")));
    send(m, "Kind", "hello", "code");
    assertEquals(Map.of(), claimed(m));
    assertFalse(Files.exists(hello.resolve(Facts.outDir)));
    assertEquals(Project.State.codeNoCache, project(m, hello).state());
  }
  @Test void aCompileWritesTheAutoselectedExtensionsIntoMainsInfoAndARecompileKeepsThem(@TempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    code(m, dir, "other");
    allow(m, dir, "foo", "bar");
    infos.put("hello", claiming("hello.Hello", "", "foo"));
    compiled(m, "hello");
    var auto= fapp("hello", "hello.Hello", "base.IconsConflict");
    same("[###][\"base.IconsConflict\", \"icons/conflict.png\", \"\", \"\", \""+auto+"\"][###][\"hello.IconsFoo\", \"art/foo.png\", \"\", \"\", \"foo\"][###]", Fs.readUtf8(mainsInfo(hello)));
    assertEquals(MainsInfo.read(hello).orElseThrow(), project(m, hello).claims());
    assertEquals(List.of("hello::hello.Hello shortcut base.IconsConflict"), claimed(m).get(auto));
    infos.put("other", claiming("other.Other", auto, "bar"));
    compiled(m, "other");
    touch(hello);
    compiled(m, "hello");
    assertEquals(List.of("hello::hello.Hello shortcut base.IconsConflict", "other::other.Other shortcut base.IconsConflict"), claimed(m).get(auto));
    same("[###]\""+auto+"\"[###]", Fs.readUtf8(mainsInfo(hello)));
    assertEquals(Project.State.codeCompiled, project(m, hello).state());
  }
  @Test void aMainsInfoWithMissingExtensionsIsFilledAtStartUp(@TempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    compiled(m, "hello");
    image(hello.resolve("art").resolve("foo.png"), 64, 64, "png");
    Fs.writeUtf8(mainsInfo(hello), FactsTest.manifest(claiming("hello.Hello", "", "foo")));
    allow(m, dir, "foo");
    var again= manager(dir, "hello.Hello");
    again.settle();
    var auto= fapp("hello", "hello.Hello", "base.IconsConflict");
    assertEquals(List.of(auto, "foo"), claimed(again).keySet().stream().sorted().toList());
    assertEquals(project(again, hello).claims(), MainsInfo.read(hello).orElseThrow());
    assertEquals(Project.State.codeCompiled, project(again, hello).state());
  }
  @Test void aMissingExtensionFilledAtStartUpAvoidsTheClaimsOfTheProjectsLoadedAfter(@TempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    var other= code(m, dir, "other");
    compiled(m, "hello", "other");
    var auto= fapp("hello", "hello.Hello", "base.IconsConflict");
    image(hello.resolve("art").resolve("foo.png"), 64, 64, "png");
    image(other.resolve("art").resolve("foo.png"), 64, 64, "png");
    Fs.writeUtf8(mainsInfo(hello), FactsTest.manifest(claiming("hello.Hello", "", "foo")));
    Fs.writeUtf8(mainsInfo(other), FactsTest.manifest(claiming("other.Other", auto, "bar")));
    allow(m, dir, "foo", "bar");
    var again= manager(dir, "hello.Hello");
    again.settle();
    assertEquals(List.of("hello", "other"), again.state().projects().stream().map(Project::alias).toList());
    assertEquals(List.of("other::other.Other shortcut base.IconsConflict"), claimed(again).get(auto));
    assertEquals(project(again, hello).claims(), MainsInfo.read(hello).orElseThrow());
  }
  @Test void noFreeExtensionLeftFailsTheCompile(@TempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    code(m, dir, "other");
    allow(m, dir, "foo");
    var all= IntStream.range(0, 1000).mapToObj(i -> "[\"other.IconsO\", \"art/o.png\", \"\", \"\", \"fapp%03d\"]".formatted(i)).collect(Collectors.joining(", "));
    infos.put("other", "{\"other.Other\": [\"_other/_rank_app.fear\", ["+all+"], [], []]}\n");
    compiled(m, "other");
    infos.put("hello", claiming("hello.Hello", "", "foo"));
    compiled(m, "hello");
    var error= """
      No free extension is left for "base.Shortcut[base.IconsConflict]" of main "hello.Hello": all the 1000 extensions "fapp000" to "fapp999" are used by the projects of this manager.
      """;
    same("--- compiling hello ---\ncompiled\n"+error+"--- compile failed ---\n", view.out(hello));
    same(error, compileError(hello));
    var p= project(m, hello);
    assertEquals(Project.State.codeOutdated, p.state());
    assertFalse(Files.exists(mainsInfo(hello)));
    assertEquals(Project.noClaims, p.claims());
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
    return "The icon \"hello.IconsFoo\" in \""+claim+"\" of main \"hello.Hello\" "+problem+" (from \""+from+"\"): an icon must be a square PNG image with a side from 64 to 1024 pixels.\n";
  }
  @Test void anIconIsASquarePngWithASideFrom64To1024Pixels(@TempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    allow(m, dir, "foo");
    infos.put("hello", claiming("hello.Hello", "fapp010", "foo"));
    for (var side : List.of(64, 1024)){
      var png= image(hello.resolve("art").resolve("foo.png"), side, side, "png");
      touch(hello);
      compiled(m, "hello");
      assertEquals(Project.State.codeCompiled, project(m, hello).state());
      assertArrayEquals(png, bytes(icons(hello).resolve("hello.IconsFoo.png")));
    }
    var accepted= project(m, hello).claims();
    for (var size : List.of(List.of(63, 63), List.of(1025, 1025), List.of(300, 200))){
      image(hello.resolve("art").resolve("foo.png"), size.get(0), size.get(1), "png");
      touch(hello);
      compiled(m, "hello");
      var error= iconError("base.OpenWith[hello.IconsFoo,\\\"foo\\\"]", "is "+size.get(0)+"x"+size.get(1)+" pixels", "art/foo.png");
      same("[###]compiled\n"+error+"--- compile failed ---\n", view.out(hello));
      same(error, compileError(hello));
      assertTrue(project(m, hello).needsCompiling());
      assertEquals(accepted, project(m, hello).claims());
      assertArrayEquals(bytes(icons(hello).resolve("hello.IconsFoo.png")), image(dir.resolve("last.png"), 1024, 1024, "png"));
    }
  }
  @Test void aPngIsMeasuredBeforeItIsDecodedAndMustDecode(@TempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    var foo= hello.resolve("art").resolve("foo.png");
    var png= image(foo, 64, 64, "png");
    var huge= ByteBuffer.allocate(33).put(png, 0, 16).putInt(100000).putInt(100000).put(png, 24, 9).array();
    infos.put("hello", info("hello.Hello", claim("hello.IconsFoo", "art/foo.png", "", "", ""), ""));
    for (var c : List.of(Map.entry(huge, "is 100000x100000 pixels"), Map.entry(Arrays.copyOf(png, png.length/2), "is not a PNG image"), Map.entry(Arrays.copyOf(png, 20), "is not a PNG image"))){
      Fs.ofV(() -> Files.write(foo, c.getKey()));
      touch(hello);
      compiled(m, "hello");
      same(iconError("base.Shortcut[hello.IconsFoo]", c.getValue(), "art/foo.png"), compileError(hello));
    }
  }
  @Test void anIconThatIsNotAPngFailsTheCompile(@TempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    image(hello.resolve("art").resolve("foo.jpg"), 64, 64, "jpg");
    infos.put("hello", info("hello.Hello", claim("hello.IconsFoo", "art/foo.jpg", "", "", ""), ""));
    compiled(m, "hello");
    same(iconError("base.Shortcut[hello.IconsFoo]", "is not a PNG image", "art/foo.jpg"), compileError(hello));
    assertFalse(Files.exists(icons(hello)));
  }
  @Test void thePreloadedIconsOfAMainAreCheckedInstalledAndShownByItsWindows(@TempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    image(hello.resolve("art").resolve("foo.png"), 32, 32, "png");
    var pre= claim("hello.IconsFoo", "art/foo.png", "", "", "")+", "+claim("base.IconsFearless", "icons/fearless.png", "", "", "");
    infos.put("hello", "{\"hello.Hello\": [\"_hello/_rank_app.fear\", [], [], ["+pre+"]]}\n");
    compiled(m, "hello");
    same(iconError("base.PreLoadIcon[hello.IconsFoo]", "is 32x32 pixels", "art/foo.png"), compileError(hello));
    image(hello.resolve("art").resolve("foo.png"), 64, 64, "png");
    touch(hello);
    compiled(m, "hello");
    assertEquals(iconNames("base.IconsFearless", "hello.IconsFoo"), iconFiles(hello));
    assertEquals(List.of("hello::hello.Hello hello.IconsFoo", "hello::hello.Hello base.IconsFearless"), windows.getLast());
  }
  @Test void iconsAreCopiedOutOfZipsAndTheStdLibOnceAndRewrittenOnlyWhenChanged(@TempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    allow(m, dir, "foo");
    var png= image(dir.resolve("foo.png"), 128, 128, "png");
    Fs.ensureDir(hello.resolve("art"));
    Fs.ofV(() -> Files.write(hello.resolve("art").resolve("pics.zip"), zip("inner.zip", zip("foo.png", png))));
    var foo= claim("hello.IconsFoo", "art/pics.zip", "inner.zip", "foo.png", "");
    infos.put("hello", info("hello.Hello", claim("base.IconsConflict", "icons/conflict.png", "", "", "")+", "+foo, foo.replace("\"\"]", "\"foo\"]")));
    compiled(m, "hello");
    assertEquals(Project.State.codeCompiled, project(m, hello).state());
    assertEquals(iconNames("base.IconsConflict", "hello.IconsFoo"), iconFiles(hello));
    assertArrayEquals(png, bytes(icons(hello).resolve("hello.IconsFoo.png")));
    assertArrayEquals(bytes(LocalResources.stLibPath.resolve("icons").resolve("conflict.png")), bytes(icons(hello).resolve("base.IconsConflict.png")));
    var old= FileTime.fromMillis(1_000_000);
    Fs.ofV(() -> Files.setLastModifiedTime(icons(hello).resolve("hello.IconsFoo.png"), old));
    touch(hello);
    compiled(m, "hello");
    assertEquals(old, Fs.of(() -> Files.getLastModifiedTime(icons(hello).resolve("hello.IconsFoo.png"))));
    var other= image(dir.resolve("foo.png"), 256, 256, "png");
    Fs.ofV(() -> Files.write(hello.resolve("art").resolve("pics.zip"), zip("inner.zip", zip("foo.png", other))));
    touch(hello);
    compiled(m, "hello");
    assertArrayEquals(other, bytes(icons(hello).resolve("hello.IconsFoo.png")));
  }
  private static List<String> iconFiles(Path project){ return Names.list(icons(project)).stream().map(p -> p.getFileName().toString()).sorted().toList(); }
  private static List<String> iconNames(String... types){ return Stream.of(types).flatMap(t -> Stream.of(t+".png", t+(Fs.isWindows() ? ".ico" : ".png"))).distinct().sorted().toList(); }
  @Test void aRecompileRemovesTheIconFilesNoClaimUsesAnyMore(@TempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    allow(m, dir, "bar", "foo", "qux");
    var bar= claim("hello.IconsBar", "art/bar.png", "", "", "bar");
    var foo= claim("hello.IconsFoo", "art/foo.png", "", "", "foo");
    var qux= claim("hello.IconsFoo", "art/foo.png", "", "", "qux");
    infos.put("hello", info("hello.Hello", claim("base.IconsConflict", "icons/conflict.png", "", "", "fapp001"), bar+", "+foo+", "+qux));
    compiled(m, "hello");
    assertEquals(iconNames("base.IconsConflict", "hello.IconsBar", "hello.IconsFoo"), iconFiles(hello));
    infos.put("hello", info("hello.Hello", "", qux));
    touch(hello);
    compiled(m, "hello");
    assertEquals(Project.State.codeCompiled, project(m, hello).state());
    assertEquals(iconNames("hello.IconsFoo"), iconFiles(hello));
    assertTrue(Files.exists(mainsInfo(hello)));
  }
  @Test void anIconEntryRemovedFromItsZipBeforeStartUpIsACorruptedCache(@TempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    allow(m, dir, "foo");
    var png= image(dir.resolve("foo.png"), 128, 128, "png");
    Fs.ensureDir(hello.resolve("art"));
    Fs.ofV(() -> Files.write(hello.resolve("art").resolve("pics.zip"), zip("inner.zip", zip("foo.png", png))));
    infos.put("hello", info("hello.Hello", "", claim("hello.IconsFoo", "art/pics.zip", "inner.zip", "foo.png", "foo")));
    compiled(m, "hello");
    Fs.ofV(() -> Files.write(hello.resolve("art").resolve("pics.zip"), zip("inner.zip", zip("bar.png", png))));
    var again= manager(dir, "hello.Hello");
    again.settle();
    same(corrupted, compileError(hello));
    assertEquals(Project.noClaims, project(again, hello).claims());
    assertEquals(Project.State.codeNoCache, project(again, hello).state());
  }
  private static final String corrupted= "Error: Fearless crashed: either this project's compilation cache was corrupted, or this is a Fearless bug.[###]";
  @Test void anIconRemovedBeforeStartUpFailsThatProjectOnly(@TempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    var other= code(m, dir, "other");
    allow(m, dir, "foo", "qux");
    infos.put("hello", claiming("hello.Hello", "fapp010", "foo"));
    infos.put("other", claiming("other.Other", "fapp004", "qux"));
    compiled(m, "hello", "other");
    Fs.rmTree(hello.resolve("art"));
    var again= manager(dir, "hello.Hello");
    again.settle();
    assertEquals(MainsInfo.read(other).orElseThrow(), project(again, other).claims());
    assertEquals(List.of("other::other.Other openWith hello.IconsFoo"), claimed(again).get("qux"));
    same(corrupted, compileError(hello));
    assertEquals(Project.noClaims, project(again, hello).claims());
  }
  @Test void anIconRemovedFromAnOutOfDateProjectBeforeStartUpLeavesThatProjectOutOfDate(@TempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    allow(m, dir, "foo");
    infos.put("hello", claiming("hello.Hello", "fapp010", "foo"));
    compiled(m, "hello");
    Fs.rmTree(hello.resolve("art"));
    touch(hello);
    var again= manager(dir, "hello.Hello");
    again.settle();
    assertEquals("The compilation cache of the package \"hello\" is out of date.", compileError(hello));
    assertEquals(Project.noClaims, project(again, hello).claims());
    assertEquals(Project.State.codeOutdated, project(again, hello).state());
  }
  @Test void aSourceUpdatedWhileAMainRunsTerminatesTheMainAndDeletesTheCache(@TempDir Path dir){
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
      --- hello.Slow exited with [###] after [###]s ---
      Some files inside the project "hello" were updated while main "hello.Slow" was running.
      """, view.out(hello));
    assertEquals(shown+2, view.shown);
    assertFalse(Files.exists(hello.resolve(Facts.outDir)));
    assertEquals(Project.State.codeNoCache, project(m, hello).state());
  }
  @Test void aSourceUpdatedDuringACompileFailsTheCompileAndDeletesTheCache(@TempDir Path dir){
    var m= manager(dir, "edited.Hello");
    var edited= code(m, dir, "edited");
    compiled(m, "edited");
    var error= "Some files inside the project \"edited\" were updated while the project was compiling.";
    same("[###]"+error+"\n--- compile failed ---\n", view.out(edited));
    assertEquals(error, compileError(edited));
    assertEquals(Project.State.codeNoCache, project(m, edited).state());
  }
  private List<String> wanted(Path dir){ return associated.getLast().stream().map(i -> i.extension()+" "+shown(dir, i)).toList(); }
  private static String shown(Path dir, Icon i){
    assertEquals(i.png().toString().replaceAll("png$", Fs.isWindows() ? "ico" : "png"), i.ico().toString());
    return dir.relativize(i.png()).toString().replace('\\', '/');
  }
  private static String userLocked(String... lines){
    return """
      Fearless cannot become the program that opens the kinds of file listed
      below.

      Your system remembers a choice you made by hand for these kinds of file,
      and no program can change or remove that choice, including this one.
      Fearless stopped before touching anything: your system is exactly as it
      was.

      The only way to clear it is Settings, Apps, Default apps, Reset - which
      resets every app default on your machine, not only this one.

      What is locked:
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
  @Test void theDesktopOpensEachCapturedExtensionWithItsIconOrTheConflictIcon(@TempDir Path dir){
    var m= manager(dir, "hello.Hello");
    code(m, dir, "hello");
    code(m, dir, "other");
    assertEquals(List.of(List.of()), associated);
    assertArrayEquals(bytes(LocalResources.stLibPath.resolve("icons").resolve("conflict.png")), bytes(dir.resolve("manager").resolve("icons").resolve("base.IconsConflict.png")));
    allow(m, dir, "foo");
    assertEquals(List.of(".foo manager/icons/base.IconsConflict.png"), wanted(dir));
    infos.put("hello", claiming("hello.Hello", "fapp010", "foo"));
    infos.put("other", claiming("other.Other", "fapp004", "foo"));
    compiled(m, "hello");
    assertEquals(List.of(".fapp010 hello/.fearless_out/icons/base.IconsConflict.png", ".foo hello/.fearless_out/icons/hello.IconsFoo.png"), wanted(dir));
    compiled(m, "other");
    assertEquals(List.of(
      ".fapp004 other/.fearless_out/icons/base.IconsConflict.png",
      ".fapp010 hello/.fearless_out/icons/base.IconsConflict.png",
      ".foo manager/icons/base.IconsConflict.png"), wanted(dir));
    var calls= associated.size();
    send(m, "Show", "hello");
    touch(dir.resolve("hello"));
    compiled(m, "hello");
    assertEquals(calls, associated.size());
    send(m, "DeleteCache", "hello");
    assertEquals(List.of(".fapp004 other/.fearless_out/icons/base.IconsConflict.png", ".foo other/.fearless_out/icons/hello.IconsFoo.png"), wanted(dir));
    send(m, "Forget", "other");
    assertEquals(List.of(".foo manager/icons/base.IconsConflict.png"), wanted(dir));
    assertEquals(calls+2, associated.size());
    assertEquals(List.of(), view.refused);
  }
  @Test void aCompileWhoseExtensionsTheDesktopRefusesFailsNamingTheirMainsAndKeepsTheClaimsBefore(@TempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    code(m, dir, "other");
    allow(m, dir, "foo", "bar");
    infos.put("other", claiming("other.Other", "fapp002", "foo"));
    infos.put("hello", claiming("hello.Hello", "fapp001", "bar"));
    compiled(m, "other", "hello");
    var accepted= project(m, hello).claims();
    held.put(".fapp003", List.of("vim"));
    infos.put("hello", claiming("hello.Hello", "fapp003", "bar"));
    touch(hello);
    compiled(m, "hello");
    var error= notOurs(".fapp003 claimed by \"hello.Hello\" of project \"hello\" -> vim");
    same("[###]--- compiling hello ---\ncompiled\n"+error+"\n--- compile failed ---\n", view.out(hello));
    same(error, compileError(hello));
    assertTrue(project(m, hello).needsCompiling());
    assertFalse(Files.exists(mainsInfo(hello)));
    assertEquals(accepted, project(m, hello).claims());
    assertEquals(List.of(".bar hello/.fearless_out/icons/hello.IconsFoo.png", ".fapp002 other/.fearless_out/icons/base.IconsConflict.png", ".fapp003 hello/.fearless_out/icons/base.IconsConflict.png", ".foo other/.fearless_out/icons/hello.IconsFoo.png"), wanted(dir));
    assertEquals(List.of("hello::hello.Hello shortcut base.IconsConflict"), claimed(m).get("fapp001"));
    assertEquals(iconNames("base.IconsConflict", "hello.IconsFoo"), iconFiles(hello));
    assertEquals(List.of(), view.refused);
  }
  @Test void aStartUpRefusedACapturedExtensionIsFatal(@TempDir Path dir){
    var m= manager(dir, "hello.Hello");
    code(m, dir, "hello");
    code(m, dir, "other");
    allow(m, dir, "foo", "qux");
    infos.put("hello", claiming("hello.Hello", "fapp001", "foo"));
    infos.put("other", claiming("other.Other", "fapp002", "qux"));
    compiled(m, "hello", "other");
    locked.add(".qux");
    manager(dir, "hello.Hello").settle();
    locked.clear();
    held.put(".fapp002", List.of("vim"));
    manager(dir, "hello.Hello").settle();
    held.clear();
    held.put(".fearless", List.of("fearlessBin0_003"));
    manager(dir, "hello.Hello").settle();
    assertEquals(3, failures.size());
    same(userLocked(".qux claimed by \"other.Other\" of project \"other\""), failures.get(0).getMessage());
    same(notOurs(".fapp002 claimed by \"other.Other\" of project \"other\" -> vim"), failures.get(1).getMessage());
    same(notOurs(".fearless -> fearlessBin0_003"), failures.get(2).getMessage());
    failures.clear();
  }
  @Test void addingASystemExtensionTheDesktopRefusesIsRefusedAndChangesNothing(@TempDir Path dir){
    var m= manager(dir);
    locked.add(".htm");
    var before= info(dir);
    allow(m, dir, "htm");
    same(userLocked(".htm"), view.refused.getLast());
    assertEquals(before, info(dir));
    assertEquals(List.of(), m.state().extensions());
    assertEquals(List.of(), view.edited);
  }
  @Test void aRecompileChangingAnIconImageGivesItToTheDesktopAndOneChangingNothingDoesNot(@TempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    allow(m, dir, "foo");
    infos.put("hello", claiming("hello.Hello", "fapp010", "foo"));
    compiled(m, "hello");
    var calls= associated.size();
    touch(hello);
    compiled(m, "hello");
    assertEquals(calls, associated.size());
    image(hello.resolve("art").resolve("foo.png"), 128, 128, "png");
    touch(hello);
    compiled(m, "hello");
    assertEquals(calls+1, associated.size());
    assertEquals(associated.get(calls-1), associated.getLast());
  }
  @Test void aProjectWhoseIconFilesAreGoneClaimsNothingAndTheOthersKeepTheirClaims(@TempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    code(m, dir, "other");
    allow(m, dir, "foo", "qux");
    infos.put("hello", claiming("hello.Hello", "fapp001", "foo"));
    infos.put("other", claiming("other.Other", "fapp002", "qux"));
    compiled(m, "hello", "other");
    Fs.rmTree(hello.resolve(Facts.outDir));
    send(m, "Show", "other");
    same("[###]--- the icon file of a claim of this project is gone: "+icons(hello).resolve("base.IconsConflict.png")+"; the project claims no extension until it is compiled again ---\n", view.out(hello));
    assertEquals(Project.noClaims, project(m, hello).claims());
    assertEquals(List.of(".fapp002 other/.fearless_out/icons/base.IconsConflict.png", ".foo manager/icons/base.IconsConflict.png", ".qux other/.fearless_out/icons/hello.IconsFoo.png"), wanted(dir));
    assertEquals(List.of(), view.refused);
  }
  @Test void aShortcutFileTheManagerCanNotCreateIsToldAndTheCompileIsDone(@TempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    compiled(m, "hello");
    infos.put("hello", info("hello.Hello", claim("base.IconsConflict", "icons/conflict.png", "", "", "fapp010"), ""));
    Disk.noWrite(hello);
    Assumptions.assumeFalse(Files.isWritable(hello));
    touch(hello);
    compiled(m, "hello");
    Disk.allow(hello);
    assertEquals("--- compiling hello ---\ncompiled\n--- hello.fapp010 is not written: access denied: "+hello.resolve("hello.fapp010")+" ---\n--- compile done ---\n", view.out(hello));
    assertEquals(List.of(".fapp010 hello/.fearless_out/icons/base.IconsConflict.png"), wanted(dir));
    assertEquals(Project.State.codeCompiled, project(m, hello).state());
  }
  private static List<String> top(Path project){ return Names.list(project).stream().map(p -> p.getFileName().toString()).toList(); }
  @Test void aCompileCreatesTheMissingShortcutFilesNextToTheMarkerHoldingItsNewlineAndNeverTouchesAnExistingOne(@TempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    allow(m, dir, "foo");
    var shortcuts= claim("base.IconsConflict", "icons/conflict.png", "", "", "fapp010")+", "+claim("base.IconsConflict", "icons/conflict.png", "", "", "");
    infos.put("hello", "{\"hello.FooBar\": [\"_hello/_rank_app.fear\", ["+shortcuts+"], [], []], \"hello.Foo'\": [\"_hello/_rank_app.fear\", [], ["+claim("base.IconsConflict", "icons/conflict.png", "", "", "foo")+"], []]}\n");
    compiled(m, "hello");
    var auto= "foo_bar."+fapp("hello", "hello.FooBar", "base.IconsConflict");
    assertEquals(List.of(".fearless_out", "_hello", "foo_bar.fapp010", auto, "hello.fearless"), top(hello));
    assertEquals(MakeDemo.markerContent, Fs.readUtf8(hello.resolve("foo_bar.fapp010")));
    assertEquals(MakeDemo.markerContent, Fs.readUtf8(hello.resolve(auto)));
    assertEquals(Project.State.codeCompiled, project(m, hello).state());
    Fs.writeUtf8(hello.resolve("foo_bar.fapp010"), "kept\n");
    Fs.ofV(() -> Files.move(hello.resolve(auto), dir.resolve(auto)));
    send(m, "Show", "hello");
    assertEquals(Project.State.codeCompiled, project(m, hello).state());
    touch(hello);
    compiled(m, "hello");
    assertEquals(List.of(".fearless_out", "_hello", "foo_bar.fapp010", auto, "hello.fearless"), top(hello));
    assertEquals("kept\n", Fs.readUtf8(hello.resolve("foo_bar.fapp010")));
    assertEquals(MakeDemo.markerContent, Fs.readUtf8(hello.resolve(auto)));
    assertTrue(Files.exists(dir.resolve(auto)));
    Fs.ofV(() -> Files.delete(hello.resolve("foo_bar.fapp010")));
    touch(hello);
    compiled(m, "hello");
    assertEquals(MakeDemo.markerContent, Fs.readUtf8(hello.resolve("foo_bar.fapp010")));
    assertEquals(Project.State.codeCompiled, project(m, hello).state());
  }
  @Test void aShortcutMainWithoutAFileNameOrWithAReservedOneOrSharingItsFileFailsTheCompileAndCreatesNothing(@TempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    var bar= claim("base.IconsConflict", "icons/conflict.png", "", "", "fapp010");
    infos.put("hello", info("hello.Hello", bar, ""));
    compiled(m, "hello");
    var accepted= project(m, hello).claims();
    var before= top(hello);
    var main= "[\"_hello/_rank_app.fear\", ["+bar+"], [], []]";
    for (var c : List.of(
      Map.entry("{\"hello.Foo'\": "+main+"}\n", "Main \"hello.Foo'\" can not have a Shortcut: its name has no matching file name. A main with a Shortcut has a name that can be mapped to a file name, like \"FooBar\" can be mapped to \"foo_bar\"."),
      Map.entry("{\"hello.Foo_bar\": "+main+"}\n", "Main \"hello.Foo_bar\" can not have a Shortcut: its name has no matching file name. A main with a Shortcut has a name that can be mapped to a file name, like \"FooBar\" can be mapped to \"foo_bar\"."),
      Map.entry("{\"hello.Con\": "+main+"}\n", "Main \"hello.Con\" can not have the shortcut file \"con.fapp010\": a file name in a project is at most 200 characters long, and its part before the dot is not \"con\", \"prn\", \"aux\", \"nul\", \"com1\"..\"com9\" or \"lpt1\"..\"lpt9\", reserved on Windows. Give this main another name."),
      Map.entry("{\"b.Bar\": "+main+", \"a.Bar\": "+main+"}\n", "Mains \"a.Bar\" and \"b.Bar\" can not both have the shortcut file \"bar.fapp010\": a shortcut file is named by the name of its main and the extension of its Shortcut. Rename one of the two mains, or give one of their Shortcuts another extension."))){
      infos.put("hello", c.getKey());
      touch(hello);
      compiled(m, "hello");
      same("--- compiling hello ---\ncompiled\n"+c.getValue()+"\n--- compile failed ---\n", view.out(hello));
      same(c.getValue(), compileError(hello));
      assertEquals(accepted, project(m, hello).claims());
      assertEquals(before, top(hello));
    }
    assertEquals(List.of(".fapp010 hello/.fearless_out/icons/base.IconsConflict.png"), wanted(dir));
  }
  @Test void aShortcutFileBreakingTheProjectFileRulesFailsTheCompileAndCreatesNothing(@TempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    Fs.writeUtf8(hello.resolve("todo"), "");
    var calls= associated.size();
    var bar= claim("base.IconsConflict", "icons/conflict.png", "", "", "fapp010");
    var longName= "hello.L"+"o".repeat(192);
    for (var c : List.of(
      Map.entry(info(longName, bar, ""), "Main \""+longName+"\" can not have the shortcut file \"l"+"o".repeat(192)+".fapp010\": a file name in a project is at most 200 characters long, and its part before the dot is not \"con\", \"prn\", \"aux\", \"nul\", \"com1\"..\"com9\" or \"lpt1\"..\"lpt9\", reserved on Windows. Give this main another name."),
      Map.entry(info("hello.Todo", bar, ""), "Main \"hello.Todo\" can not have the shortcut file \"todo.fapp010\": the project has the file \"todo\", and a file without extension can not share its name with a file with an extension. Rename the main, or rename the file \"todo\"."))){
      infos.put("hello", c.getKey());
      touch(hello);
      compiled(m, "hello");
      same("--- compiling hello ---\ncompiled\n"+c.getValue()+"\n--- compile failed ---\n", view.out(hello));
      assertEquals(List.of(".fearless_out", "_hello", "hello.fearless", "todo"), top(hello));
    }
    assertEquals(calls, associated.size());
    infos.put("hello", info("hello.L"+"o".repeat(191), bar, ""));
    touch(hello);
    compiled(m, "hello");
    assertTrue(Files.exists(hello.resolve("l"+"o".repeat(191)+".fapp010")));
    Fs.ofV(() -> Files.delete(hello.resolve("todo")));
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
  @Test void aFileWhoseExtensionOneMainClaimsRunsThatMainAndOneNotCompiledIsRefused(@TempDir Path dir){
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
    assertEquals(List.of("Refused request \"Run\" on project \"hello\".\nThe state of project \"hello\" is \"code: not compiled (cache out of date)\": only a project in state \"code: compiled\" runs a main."), view.refused);
    assertEquals(List.of(), view.choices);
    assertEquals(List.of("hello "+hello, "other "+other), listed(m));
  }
  @Test void aFileWhoseExtensionSeveralMainsClaimListsThemSorted(@TempDir Path dir){
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
    assertEquals(shown+1, view.shown);
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
    send(m, "OpenWithOrShortcut", TaggedText.of(doc(dir, "a.foo").toString()), "hello");
    same("[###]\"OpenWithOrShortcut\"[###]", view.refused.getLast());
  }
  @Test void anExtensionMatchesInAnyCaseAsTheDesktopMatchesIt(@TempDir Path dir){
    var m= opening(dir);
    var hello= code(m, dir, "hello");
    allow(m, dir, "foo");
    infos.put("hello", claiming("hello.Hello", "fapp001", "foo"));
    compiled(m, "hello");
    open(m, doc(dir, "Report.Old.FOO"));
    idle(m);
    same(running("hello.Hello", " on Report.Old.FOO"), view.out(hello));
  }
  @Test void aFileOpenedWhileItsMainRunsIsRefused(@TempDir Path dir){
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
  @Test void aFileNobodyClaimsAMissingFileAnEmptyFileAndACapturedExtensionNoMainOpensAreRefused(@TempDir Path dir){
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
  @Test void aCompileClaimingASystemExtensionNotCapturedFailsAndOffersToCaptureIt(@TempDir Path dir){
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
    assertEquals(Map.of(), claimed(m));
    assertEquals(Project.State.codeOutdated, project(m, hello).state());
    assertEquals(List.of(List.of("htm")), view.offered);
    allow(m, dir, view.offered.getLast().toArray(String[]::new));
    assertEquals(List.of("htm"), m.state().extensions());
    compiled(m, "hello");
    assertEquals(List.of("fapp001", "htm"), claimed(m).keySet().stream().sorted().toList());
    assertEquals(1, view.asked.size());
  }
  @Test void anOfferToCaptureAnExtensionAcceptedAfterARegisterKeepsThatProject(@TempDir Path dir){
    var m= manager(dir, "hello.Hello");
    var hello= code(m, dir, "hello");
    infos.put("hello", claiming("hello.Hello", "fapp001", "htm"));
    compiled(m, "hello");
    var other= registered(m, dir, "other");
    allow(m, dir, view.offered.getLast().toArray(String[]::new));
    assertEquals(List.of("htm"), m.state().extensions());
    assertEquals(List.of("hello "+hello, "other "+other), listed(m));
  }
  @Test void removingASystemExtensionDeletesTheCompiledCacheOfTheProjectsOpeningIt(@TempDir Path dir){
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
    assertEquals(Project.State.codeNoCache, project(m, hello).state());
    assertEquals(Project.State.codeCompiled, project(m, other).state());
    assertEquals(List.of(".fapp002 other/.fearless_out/icons/base.IconsConflict.png", ".qux other/.fearless_out/icons/hello.IconsFoo.png"), wanted(dir));
  }
}

package controller;

import static controller.Errs.same;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import controller.Manager.State;
import controller.Registry.Kind;
import tools.ChildJvm;
import tools.Fs;
import userMessages.Report;

/// The whole manager without a window: messages in, Eclipse files and View calls out.
/// Compiling writes a fresh cache and every job is a real child JVM running Child.
final class ManagerTest{
  @TempDir static Path classes;
  @BeforeAll static void child(){
    var src= classes.resolve("Child.java");
    Fs.writeUtf8(src,"""
      public class Child{
        public static void main(String[] a) throws Exception{
          System.out.print(a[0]+"\\n");
          System.out.flush();
          Thread.sleep(Long.parseLong(a[2]));
          System.exit(Integer.parseInt(a[1]));
        }
      }
      """);
    Fs.runTool("javac",List.of("-d",classes.toString(),src.toString()));
  }
  record Fake(Function<Path,Optional<Map<String,String>>> read) implements Manager.Tools{
    @Override public ChildJvm compile(Path folder, Consumer<String> out){
      if (folder.getFileName().toString().equals("nojvm")){ throw new UncheckedIOException(new IOException("Cannot run program \"java\": error=2, No such file or directory")); }
      FactsTest.cache(folder,"hello",FactsTest.after(folder));
      return jvm(out,"compiled","0","0");
    }
    @Override public ChildJvm run(Path folder, String main, Consumer<String> out){
      var log= folder.resolve(".out").resolve("logs").resolve("unit_test_log$21000101_000000_000Z.log");
      if (main.endsWith("Slow")){ Fs.writeUtf8(log,"<testcase classname=\"a\" name=\"b\" file=\"f\" line=\"1\"></testcase>\n"); }
      if (main.endsWith("Garbled")){ Fs.ensureDir(log.getParent()); Fs.ofV(()->Files.write(log,new byte[]{(byte)0xFF,'\n'})); }
      return jvm(out,"ran "+main,"0",main.endsWith("Slow") ? "60000" : "0");
    }
    @Override public Optional<Map<String,String>> mains(Path folder){ return read.apply(folder); }
    private static ChildJvm jvm(Consumer<String> out, String... args){
      return ChildJvm.start(Stream.concat(Stream.of("-cp",classes.toString(),"Child"),Stream.of(args)).toList(),out);
    }
  }
  static final class View implements Manager.View{
    final List<String> notes= new ArrayList<>();
    int shown;
    State state;
    @Override public void show(){ shown+= 1; }
    @Override public void state(State s){ state= s; }
    @Override public void output(Path folder, String text){}
    @Override public void note(String text){ notes.add(text); }
    final List<Path> cleared= new ArrayList<>();
    @Override public void clear(Path folder){ cleared.add(folder); }
    boolean visible= true;
    @Override public boolean visible(){ return visible; }
  }
  private final List<Throwable> failures= Collections.synchronizedList(new ArrayList<>());
  private final View view= new View();
  @AfterEach void nothingFailed(){ assertEquals(List.of(),failures); }
  private Manager manager(Path dir, String... mains){
    var map= new LinkedHashMap<String,String>();
    for (var m: mains){ map.put(m,"_hello/_rank_app.fear"); }
    var known= Optional.<Map<String,String>>of(Collections.unmodifiableMap(map));
    return manager(dir,_->known);
  }
  private Manager manager(Path dir, Function<Path,Optional<Map<String,String>>> read){ return new Manager(dir.resolve("manager"),new Fake(read),view,failures::add); }
  private static Path folder(Path dir, String name){
    var res= dir.resolve(name);
    Fs.ensureDir(res);
    return res;
  }
  private static Path data(Path dir){
    var res= dir.resolve("data");
    Fs.writeUtf8(res.resolve("readme.txt"),"hi\n");
    return res;
  }
  private static void send(Manager m, String... lines){
    m.message(String.join("\n",lines));
    m.settle();
  }
  private static void commit(Manager m, String text, Runnable done){
    m.commit(Registry.text(m.state().projects().stream().map(Project::entry).toList()),text,done);
    m.settle();
  }
  private static void until(Manager m, Predicate<State> done){
    var end= System.nanoTime()+Duration.ofSeconds(30).toNanos();
    while(true){
      m.settle();
      if (done.test(m.state())){ return; }
      assertTrue(System.nanoTime() < end);
      try{ Thread.sleep(20); }
      catch(InterruptedException e){ throw new AssertionError(e); }
    }
  }
  private static void idle(Manager m){ until(m,s->s.projects().stream().noneMatch(Project::busy)); }
  private static Project project(Manager m, Path folder){ return m.state().of(folder).orElseThrow(); }
  private static String eclipse(Path dir, String... path){
    var file= dir.resolve("manager").resolve("eclipse");
    for (var p: path){ file= file.resolve(p); }
    return Fs.readUtf8(file);
  }
  private static List<String> listed(Path dir){
    var state= (Info.Obj)Info.parse(eclipse(dir,"state.info"),dir.toUri());
    return state.fields().stream().map(f->f.key()+" "+TaggedText.read(((Info.Str)((Info.Obj)f.value()).field("folder").orElseThrow().value()).value(),Messages::infoError)).toList();
  }
  @Test void aMessageWithNoFolderShowsTheWindow(@TempDir Path dir){
    var m= manager(dir);
    send(m,"");
    assertEquals(1,view.shown);
    assertEquals(List.of(),listed(dir));
  }
  @Test void aFolderNamedWithANewlineIsRegisteredByItsMessage(@TempDir Path dir){
    Assumptions.assumeFalse(Fs.isWindows());
    var m= manager(dir);
    var odd= folder(dir,"a\nb");
    send(m,Main.message(odd.toString()));
    assertEquals(List.of("a_b "+odd),listed(dir));
  }
  @Test void selectingAnEmptyFolderMakesItAHelloWorldCodeProject(@TempDir Path dir){
    var m= manager(dir);
    var hello= folder(dir,"hello");
    send(m,TaggedText.of(hello.toString()));
    assertEquals(List.of("hello "+hello),listed(dir));
    assertTrue(Files.isRegularFile(hello.resolve("hello.fearless")));
    assertTrue(Files.isRegularFile(hello.resolve("_hello").resolve("_rank_app.fear")));
    var p= project(m,hello);
    assertEquals(Kind.code,p.kind());
    assertEquals(Project.State.codeNoCache,p.state());
    assertEquals(Optional.of(hello),m.state().selected());
    assertEquals(1,view.shown);
  }
  @Test void aFolderHoldingOnlyItsCompiledCacheIsEmptiedThenMadeAHelloWorldCodeProject(@TempDir Path dir){
    var m= manager(dir);
    var hello= folder(dir,"hello");
    Fs.writeUtf8(hello.resolve(Facts.outDir).resolve("hello.built"),"");
    send(m,TaggedText.of(hello.toString()));
    assertFalse(Files.exists(hello.resolve(Facts.outDir)));
    assertTrue(Files.isRegularFile(hello.resolve("_hello").resolve("_rank_app.fear")));
    assertEquals(Kind.code,project(m,hello).kind());
  }
  @Test void aSecondFolderNamedComIsNamedWithTheFirstValidFreeName(@TempDir Path dir){
    var m= manager(dir);
    var first= folder(dir.resolve("a"),"com");
    var second= folder(dir.resolve("b"),"com");
    send(m,TaggedText.of(first.toString()));
    send(m,TaggedText.of(second.toString()));
    assertEquals(List.of("com "+first,"com10 "+second),listed(dir));
    same("[###]\nHello: Main { sys -> sys.out.println(\"Hello World!\") }\n",Fs.readUtf8(second.resolve("_com10").resolve("_rank_app.fear")));
  }
  @Test void aFileRegistersTheFolderItIsInAndRegisteringAgainSelects(@TempDir Path dir){
    var m= manager(dir);
    var hello= folder(dir,"hello");
    var other= folder(dir,"other");
    Fs.writeUtf8(hello.resolve("hello.fearless"),"");
    send(m,"register",TaggedText.of(hello.resolve("hello.fearless").toString()));
    assertEquals(List.of("hello "+hello),listed(dir));
    send(m,TaggedText.of(other.toString()));
    assertEquals(Optional.of(other),m.state().selected());
    send(m,TaggedText.of(hello.toString()));
    assertEquals(List.of("hello "+hello,"other "+other),listed(dir));
    assertEquals(Optional.of(hello),m.state().selected());
    send(m,"select","other");
    assertEquals(Optional.of(other),m.state().selected());
    assertEquals(List.of(),view.notes);
  }
  @Test void aRunMessageCompilesThenRunsTheOnlyMain(@TempDir Path dir){
    var m= manager(dir,"hello.Hello");
    var hello= folder(dir,"hello");
    send(m,TaggedText.of(hello.toString()));
    send(m,"run","hello");
    idle(m);
    same("""
      --- compiling hello ---
      compiled
      --- compile done ---
      --- running hello.Hello ---
      ran hello.Hello
      --- hello.Hello exited with 0 after [###]s ---
      """,eclipse(dir,"hello","console.txt"));
    var p= project(m,hello);
    assertEquals(Project.State.codeCompiled,p.state());
    assertEquals(List.of(1,"hello.Hello",0),List.of(p.runs(),p.lastRun(),p.exit()));
    assertTrue(p.entry().compiled() > 0 && p.entry().run() > 0);
  }
  @Test void compilingAnUpToDateProjectDoesNothing(@TempDir Path dir){
    var m= manager(dir,"hello.Hello");
    var hello= folder(dir,"hello");
    send(m,TaggedText.of(hello.toString()));
    send(m,"compile","hello");
    idle(m);
    var once= eclipse(dir,"hello","console.txt");
    send(m,"compile","hello");
    idle(m);
    assertEquals(once,eclipse(dir,"hello","console.txt"));
    assertEquals(Optional.of(Map.of("hello.Hello","_hello/_rank_app.fear")),project(m,hello).mains());
  }
  @Test void aBusyProjectRefusesJobsCleaningAndKindChangesThenTerminateEndsTheJob(@TempDir Path dir){
    var m= manager(dir,"hello.Slow");
    var hello= folder(dir,"hello");
    send(m,TaggedText.of(hello.toString()));
    send(m,"run","hello");
    until(m,_->eclipse(dir,"hello","console.txt").contains("ran hello.Slow"));
    assertEquals(Optional.of("hello.Slow"),project(m,hello).running());
    same("[###]\"running\": \"hello.Slow\"[###]",eclipse(dir,"state.info"));
    send(m,"run","hello");
    send(m,"compile","hello");
    send(m,"clean","hello");
    send(m,"kind","hello","idle");
    send(m,"terminate","hello");
    idle(m);
    same("""
      --- compiling hello ---
      compiled
      --- compile done ---
      --- running hello.Slow ---
      ran hello.Slow
      --- run refused: the project is busy with hello.Slow ---
      --- compile refused: the project is busy with hello.Slow ---
      --- clear cache refused: the project is busy with hello.Slow ---
      --- kind change refused: the project is busy with hello.Slow ---
      --- terminating hello.Slow ---
      --- hello.Slow exited with [###] after [###]s ---
      """,eclipse(dir,"hello","console.txt"));
    assertEquals(Kind.code,project(m,hello).kind());
  }
  @Test void terminateEndsTheWholeRunNotOnlyTheCurrentMain(@TempDir Path dir){
    var m= manager(dir,"hello.Slow","hello.Two");
    var hello= folder(dir,"hello");
    send(m,TaggedText.of(hello.toString()));
    send(m,"mains","hello","hello.Two hello.Slow");
    send(m,"run","hello");
    until(m,_->eclipse(dir,"hello","console.txt").contains("ran hello.Slow"));
    send(m,"terminate","hello");
    idle(m);
    assertFalse(eclipse(dir,"hello","console.txt").contains("hello.Two"));
  }
  @Test void theSelectedMainsRunInTheOrderOfTheMains(@TempDir Path dir){
    var m= manager(dir,"hello.One","hello.Two","hello.Three");
    var hello= folder(dir,"hello");
    send(m,TaggedText.of(hello.toString()));
    send(m,"run","hello");
    idle(m);
    assertTrue(eclipse(dir,"hello","console.txt").endsWith("--- nothing to run: none of [hello.One, hello.Two, hello.Three] is selected ---\n"));
    send(m,"mains","hello","hello.Three hello.One");
    send(m,"clear","hello");
    send(m,"run","hello");
    idle(m);
    same("""
      --- running hello.One ---
      ran hello.One
      --- hello.One exited with 0 after [###]s ---
      --- running hello.Three ---
      ran hello.Three
      --- hello.Three exited with 0 after [###]s ---
      """,eclipse(dir,"hello","console.txt"));
    assertEquals(List.of("hello.One","hello.Three"),project(m,hello).selectedMains());
    assertEquals(List.of(hello),view.cleared);
  }
  @Test void aNamedMainRunsAloneAndAnUnknownOneIsRefused(@TempDir Path dir){
    var m= manager(dir,"hello.One","hello.Two");
    var hello= folder(dir,"hello");
    send(m,TaggedText.of(hello.toString()));
    send(m,"compile","hello");
    idle(m);
    send(m,"clear","hello");
    send(m,"run","hello","hello.Nope");
    send(m,"run","hello","hello.Two");
    idle(m);
    same("""
      --- nothing to run: hello.Nope is not one of the mains [hello.One, hello.Two] ---
      --- running hello.Two ---
      ran hello.Two
      --- hello.Two exited with 0 after [###]s ---
      """,eclipse(dir,"hello","console.txt"));
  }
  @Test void forgettingARunningProjectEndsItsJobAndForgetsIt(@TempDir Path dir){
    var m= manager(dir,"hello.Slow");
    var hello= folder(dir,"hello");
    send(m,TaggedText.of(hello.toString()));
    send(m,"run","hello");
    until(m,_->eclipse(dir,"hello","console.txt").contains("ran hello.Slow"));
    send(m,"forget","hello");
    assertEquals(List.of(),listed(dir));
    assertEquals(Optional.empty(),m.state().selected());
    send(m,"run","hello");
    assertEquals("The manager was asked to \"run\" the project \"hello\", but no project is named \"hello\".\nNo project is registered.",view.notes.getLast());
    assertTrue(Files.isRegularFile(hello.resolve("hello.fearless")));
  }
  @Test void aRequestNamesARegisteredProject(@TempDir Path dir){
    var m= manager(dir);
    var hello= folder(dir,"hello");
    send(m,TaggedText.of(hello.toString()));
    send(m,"compile","other");
    send(m,"compile",hello.toString());
    assertEquals(List.of("hello "+hello),listed(dir));
    assertEquals("""
      The manager was asked to "compile" the project "other", but no project is named "other".
      The projects are:
        hello""",view.notes.getFirst());
    assertTrue(view.notes.get(1).startsWith("The manager was asked to \"compile\" the project \""+hello+"\", but no project is named"));
    assertEquals(view.notes.getFirst()+"\n"+view.notes.get(1)+"\n",eclipse(dir,"console.txt"));
  }
  @Test void aRequestTheManagerDoesNotKnowIsRefused(@TempDir Path dir){
    var m= manager(dir);
    var hello= folder(dir,"hello");
    send(m,TaggedText.of(hello.toString()));
    send(m,"build","hello");
    send(m,"kind","hello","library");
    send(m,"link","hello","data sometimes");
    send(m,"a","b","c","d");
    assertEquals(4,view.notes.size());
    assertTrue(view.notes.get(0).startsWith("The manager was asked to \"build\", but that is not a request it knows: the requests are \"register\", \"select\""));
    assertTrue(view.notes.get(1).endsWith("to \"library\", but the kinds are \"idle\", \"code\", \"data:readOnly\" and \"data:readWrite\"."));
    assertEquals("The manager was asked to link \"hello\" with \"data sometimes\", but a link is a project name, then \"read\" or \"write\", then the type names, none to remove the link.",view.notes.get(2));
    assertTrue(view.notes.get(3).startsWith("The manager was sent a message of 4 lines"));
  }
  @Test void aThirdLineForARequestTakingNoneIsRefusedAndChangesNothing(@TempDir Path dir){
    var m= manager(dir);
    var hello= folder(dir,"hello");
    send(m,TaggedText.of(hello.toString()));
    send(m,"forget","hello","now");
    assertEquals(List.of("hello "+hello),listed(dir));
    assertEquals(List.of("The manager was asked to \"forget\" \"hello\" with the third line \"now\", but only the requests \"run\", \"kind\", \"mains\", \"link\" take a third line."),view.notes);
  }
  @Test void aRequestWhoseResultIsNotAValidProjectsInfoIsRefusedAndChangesNothing(@TempDir Path dir){
    var m= manager(dir);
    var hello= folder(dir,"hello");
    send(m,TaggedText.of(hello.toString()));
    send(m,TaggedText.of(data(dir).toString()));
    send(m,"kind","data","data:readOnly");
    var info= dir.resolve("manager").resolve("projects.info");
    var before= Fs.readUtf8(info);
    send(m,"mains","hello","hello.One\thello.Two");
    send(m,"mains","hello","hello.One\r");
    send(m,"link","hello","data read \u00c4rger");
    assertEquals(before,Fs.readUtf8(info));
    assertEquals(List.of(),project(m,hello).entry().mains());
    assertEquals(Map.of(),project(m,hello).entry().reads());
    assertEquals(3,view.notes.size());
    same("In file: [###]projects.info\n\n005|     \"mains\": [\"hello.One    hello.Two\"]\n   |                         ^^^^\n[###]The character [Tab 0x09] is outside the safe character set of Fearless[###]",view.notes.get(0));
    same("[###]The character [###] 0x0D] is outside the safe character set of Fearless[###]",view.notes.get(1));
    same("[###]\"data\": [\"?rger\"][###]The character [U+00C4] is outside the safe character set of Fearless[###]",view.notes.get(2));
  }
  @Test void aMessageFileThatIsNotUtf8IsRefusedAndRemovedAndTheNextStartDrainsNothing(@TempDir Path dir){
    var m= manager(dir);
    var messages= folder(dir.resolve("manager"),"messages");
    Fs.ofV(()->Files.write(messages.resolve("1.msg"),new byte[]{'r','u','n','\n',(byte)0xFF}));
    Fs.writeUtf8(messages.resolve("2.msg"),"");
    m.drain();
    m.settle();
    same("The manager refused a message, and removed its file: a message is UTF-8 text the manager can read.\n[###]1.msg[###]not form valid UTF-8 text[###]",view.notes.getFirst());
    assertEquals(1,view.notes.size());
    assertEquals(1,view.shown);
    assertEquals(List.of(),Fs.of(()->{ try(var s= Files.list(messages)){ return s.toList(); } }));
    var again= manager(dir);
    again.drain();
    again.settle();
    assertEquals(1,view.notes.size());
  }
  @Test void aFolderInsideARegisteredOneIsRefused(@TempDir Path dir){
    var m= manager(dir);
    var hello= folder(dir,"hello");
    send(m,TaggedText.of(hello.toString()));
    send(m,TaggedText.of(folder(hello,"inner").toString()));
    assertEquals(1,view.notes.size());
    assertEquals(List.of("hello "+hello),listed(dir));
  }
  @Test void aLinkToARegisteredFolderOnlySelectsItAndALinkIntoOneIsRefused(@TempDir Path dir){
    Assumptions.assumeTrue(Fs.isLinux());
    var m= manager(dir);
    var hello= folder(dir,"hello");
    var other= folder(dir,"other");
    send(m,TaggedText.of(hello.toString()));
    send(m,TaggedText.of(other.toString()));
    var link= Fs.of(()->Files.createSymbolicLink(dir.resolve("link"),hello));
    var inner= Fs.of(()->Files.createSymbolicLink(dir.resolve("inner"),folder(hello,"inner")));
    send(m,TaggedText.of(link.toString()));
    assertEquals(Optional.of(hello),m.state().selected());
    send(m,TaggedText.of(inner.toString()));
    assertEquals(List.of("hello "+hello,"other "+other),listed(dir));
    assertTrue(Files.isRegularFile(hello.resolve("hello.fearless")));
    assertEquals(1,view.notes.size());
    same("[###]Fearless is already keeping track of:\n[###]hello\nOne of the two is inside the other.[###]",view.notes.getFirst());
  }
  @Test void kindsChangeFromIdleAndBackToIdleOnly(@TempDir Path dir){
    var m= manager(dir);
    var data= data(dir);
    send(m,TaggedText.of(data.toString()));
    assertEquals(Kind.idle,project(m,data).kind());
    send(m,"kind","data","data:readOnly");
    send(m,"kind","data","code");
    assertEquals(Kind.dataReadOnly,project(m,data).kind());
    assertEquals("--- kind change refused: a project of kind data:readOnly goes back to idle before becoming code ---\n",eclipse(dir,"data","console.txt"));
    send(m,"kind","data","idle");
    assertEquals(Kind.idle,project(m,data).kind());
  }
  @Test void onlyACodeProjectCompilesAndRunsAndAnyProjectIsChecked(@TempDir Path dir){
    var m= manager(dir);
    var data= data(dir);
    send(m,TaggedText.of(data.toString()));
    send(m,"compile","data");
    send(m,"run","data");
    send(m,"check","data");
    Fs.writeUtf8(data.resolve("Bad Name.txt"),"");
    send(m,"check","data");
    same("""
      --- compile refused: this project is idle, and only a code project compiles ---
      --- run refused: this project is idle, and only a code project runs ---
      --- ok: no problem found ---
      [###]Bad Name.txt[###]
      """,eclipse(dir,"data","console.txt"));
    assertEquals(Project.State.dataInvalid,project(m,data).state());
  }
  @Test void linksAreEditedByRequestAndBrokenLinksStopTheCompile(@TempDir Path dir){
    var m= manager(dir);
    var hello= folder(dir,"hello");
    var data= data(dir);
    send(m,TaggedText.of(hello.toString()));
    send(m,TaggedText.of(data.toString()));
    send(m,"kind","data","data:readWrite");
    send(m,"link","hello","data write Data");
    send(m,"link","hello","data read Pub");
    var entry= project(m,hello).entry();
    assertEquals(List.of(Map.of("data",List.of("Pub")),Map.of("data",List.of("Data"))),List.of(entry.reads(),entry.edits()));
    assertEquals(Optional.empty(),project(m,hello).problem());
    send(m,"link","hello","data read lower");
    same("[###]\"lower\" in \"reads\".\"data\" is not a Fearless type name[###]",view.notes.getLast());
    send(m,"link","hello","data read Data");
    same("[###]\"Data\" is in both \"reads\".\"data\" and \"edits\".\"data\"[###]",view.notes.getLast());
    assertEquals(entry,project(m,hello).entry());
    send(m,"link","data","hello read Hello");
    assertEquals("--- link refused: this project is data:readWrite, and only a code project links to data ---\n",eclipse(dir,"data","console.txt"));
    send(m,"kind","data","idle");
    assertEquals(Project.State.codeInvalid,project(m,hello).state());
    send(m,"compile","hello");
    assertEquals("\"reads\" refers to \"data\", but the kind of \"data\" is \"idle\"; \"reads\" accepts only \"data:readOnly\" or \"data:readWrite\".\n",eclipse(dir,"hello","console.txt"));
    send(m,"link","hello","data read");
    send(m,"link","hello","data write");
    assertEquals(List.of(Map.of(),Map.of()),List.of(project(m,hello).entry().reads(),project(m,hello).entry().edits()));
    assertEquals(Project.State.codeNoCache,project(m,hello).state());
  }
  @Test void theMetadataIsCommittedWholeOrNotAtAll(@TempDir Path dir){
    var m= manager(dir);
    var data= data(dir);
    send(m,TaggedText.of(data.toString()));
    var done= new ArrayList<String>();
    commit(m,"{\"data\": {\"path\": \"Str:nowhere\"}}",()->done.add("bad"));
    assertEquals(List.of(),done);
    same("[###]\"path\" must be an absolute path[###]",view.notes.getLast());
    commit(m,Registry.text(List.of(project(m,data).entry().withKind(Kind.dataReadWrite))),()->done.add("good"));
    assertEquals(List.of("good"),done);
    assertEquals(Kind.dataReadWrite,project(m,data).kind());
    commit(m,"{}",()->done.add("empty"));
    assertEquals(List.of(),listed(dir));
  }
  @Test void metadataThatChangedWhileEditedIsNotCommitted(@TempDir Path dir){
    var m= manager(dir);
    var data= data(dir);
    send(m,TaggedText.of(data.toString()));
    var base= Registry.text(m.state().projects().stream().map(Project::entry).toList());
    var hello= folder(dir,"hello");
    send(m,TaggedText.of(hello.toString()));
    var done= new ArrayList<String>();
    m.commit(base,base,()->done.add("stale"));
    m.settle();
    assertEquals(List.of(),done);
    assertEquals(List.of("data "+data,"hello "+hello),listed(dir));
    assertEquals(Messages.metadataChanged(),view.notes.getLast());
  }
  @Test void aProjectWhoseFolderIsGoneIsInvalidAndCanBeForgotten(@TempDir Path dir){
    var m= manager(dir);
    var data= data(dir);
    send(m,TaggedText.of(data.toString()));
    Fs.rmTree(data);
    send(m,"select","data");
    assertEquals(Optional.of(data),m.state().selected());
    assertEquals(List.of(),view.notes);
    send(m,"check","data");
    assertTrue(eclipse(dir,"data","console.txt").startsWith("The folder of this project does not exist:"));
    send(m,"forget","data");
    assertEquals(List.of(),listed(dir));
    send(m,"register",TaggedText.of(data.toString()));
    same("The manager was asked to register\n"+data+"\nbut nothing exists there: register an existing folder, or a file inside one.",view.notes.getLast());
    assertEquals(List.of(),listed(dir));
  }
  @Test void everyRequestNamingAProjectChecksItWhileTheWindowIsHidden(@TempDir Path dir){
    var m= manager(dir);
    var data= data(dir);
    send(m,TaggedText.of(data.toString()));
    view.visible= false;
    for (var request: List.of(List.of("mains","data",""),List.of("link","data","other read Other"),List.of("clear","data"),List.of("terminate","data"))){
      var valid= Files.exists(data);
      if (valid){ Fs.rmTree(data); } else { data(dir); Fs.writeUtf8(data.resolve("data.fearless"),""); }
      send(m,request.toArray(String[]::new));
      assertEquals(valid ? Project.State.dataInvalid : Project.State.idle,project(m,data).state());
    }
  }
  private static void mode(Path p, String mode){ Fs.ofV(()->Files.setPosixFilePermissions(p,PosixFilePermissions.fromString(mode))); }
  @Test void foldersTheManagerCanNotReadOrChangeAreRefusedOrInvalidNeverFatal(@TempDir Path dir){
    Assumptions.assumeTrue(Fs.isLinux());
    var m= manager(dir);
    var closed= folder(dir,"closed");
    var readOnly= folder(dir,"readOnly");
    var cached= data(dir);
    var partly= folder(dir,"partly");
    var hidden= folder(partly,"hidden");
    var cache= folder(cached,Facts.outDir);
    Fs.writeUtf8(cache.resolve("a.built"),"");
    mode(closed,"---------");
    Assumptions.assumeFalse(Files.isReadable(closed));
    mode(readOnly,"r-xr-xr-x");
    mode(cache,"r-xr-xr-x");
    mode(hidden,"---------");
    send(m,TaggedText.of(closed.toString()));
    send(m,TaggedText.of(readOnly.toString()));
    send(m,TaggedText.of(cached.toString()));
    send(m,TaggedText.of(partly.toString()));
    m.connect(closed);
    m.settle();
    same("""
      The manager was asked to register
      [###]closed
      but could not read or change it: access denied: [###]closed
      Give Fearless access to the folder, then register it again.""",view.notes.get(0));
    same("[###]readOnly\nbut could not read or change it: access denied: [###]readOnly[###]",view.notes.get(1));
    same("[###]data\nbut could not read or change it: access denied: [###]a.built[###]",view.notes.get(2));
    same("Eclipse is not connected: access denied: [###]closed",view.notes.get(3));
    assertEquals(List.of("partly "+partly),listed(dir));
    same("""
      The folder of this project can not be read:
      [###]partly
      access denied: [###]hidden
      Give Fearless access to it, or forget this project.""",project(m,partly).problem().orElseThrow());
    var again= manager(dir);
    again.settle();
    assertEquals(Project.State.dataInvalid,project(again,partly).state());
    mode(cache,"rwxr-xr-x");
    send(again,TaggedText.of(cached.toString()));
    Fs.writeUtf8(cache.resolve("a.built"),"");
    mode(cache,"r-xr-xr-x");
    send(again,"clean","data");
    same("--- clear cache failed: access denied: [###]a.built ---\n",eclipse(dir,"data","console.txt"));
    List.of(closed,readOnly,cache,hidden).forEach(p->mode(p,"rwxr-xr-x"));
  }
  @Test void aManagerFolderThatCanNotBeWrittenIsFatalForEveryChangeAndARefusalWritesNothing(@TempDir Path dir){
    Assumptions.assumeTrue(Fs.isLinux());
    var m= manager(dir);
    var hello= folder(dir,"hello");
    send(m,TaggedText.of(hello.toString()));
    var managerDir= dir.resolve("manager");
    var files= List.of(managerDir.resolve("projects.info"),managerDir.resolve("activity.txt"));
    var before= files.stream().map(Fs::readUtf8).toList();
    send(m,"link","hello","data read lower");
    same("[###]\"lower\" in \"reads\".\"data\" is not a Fearless type name[###]",view.notes.getLast());
    assertEquals(before,files.stream().map(Fs::readUtf8).toList());
    mode(managerDir,"r-xr-xr-x");
    Assumptions.assumeFalse(Files.isWritable(managerDir));
    send(m,"mains","hello","hello.One");
    send(m,"kind","hello","idle");
    commit(m,"{}",()->{});
    mode(managerDir,"rwxr-xr-x");
    assertEquals(3,failures.size());
    failures.forEach(f->same("Fearless could not save what it remembers about your project folders.[###]",f.getMessage()));
    failures.clear();
    assertEquals(1,view.notes.size());
    assertEquals(List.of("hello "+hello),listed(dir));
    assertEquals(List.of(Kind.code,List.of()),List.of(project(m,hello).kind(),project(m,hello).entry().mains()));
  }
  @Test void aMessageFileTheManagerCanNotReadIsFatal(@TempDir Path dir){
    Assumptions.assumeTrue(Fs.isLinux());
    var m= manager(dir);
    var msg= folder(dir.resolve("manager"),"messages").resolve("1.msg");
    Fs.writeUtf8(msg,"");
    mode(msg,"---------");
    Assumptions.assumeFalse(Files.isReadable(msg));
    Errs.err("Fearless could not list its manager folder, or could not read or\nremove a message file in it.[###]1.msg[###]",m::drain);
    mode(msg,"rw-r--r--");
  }
  @Test void renamingAProjectKeepsItsConsole(@TempDir Path dir){
    var m= manager(dir);
    var data= data(dir);
    send(m,TaggedText.of(data.toString()));
    send(m,"compile","data");
    commit(m,"{\"other\": {\"path\": \"Str:"+data.toString().replace('\\','/')+"\", \"kind\": \"idle\"}}",()->{});
    assertEquals("--- compile refused: this project is idle, and only a code project compiles ---\n",eclipse(dir,"other","console.txt"));
  }
  @Test void aRequestNamingNoProjectOrFolderIsRefused(@TempDir Path dir){
    var m= manager(dir);
    send(m,"run","");
    send(m,"  ");
    send(m,"register","");
    send(m,"register","Str: ");
    assertEquals(0,view.shown);
    assertEquals(List.of(
      "The manager was asked to \"run\" the project \"\", but no project is named \"\".\nNo project is registered.",
      "The manager was asked to register a folder, but the message names no folder.",
      "The manager was asked to register a folder, but the message names no folder.",
      "The manager was asked to register a folder, but the message names no folder."),view.notes);
  }
  @Test void aRegisterWhosePathIsNotATaggedTextOrNotAPathIsRefused(@TempDir Path dir){
    Assumptions.assumeTrue(Fs.isLinux());
    var m= manager(dir);
    send(m,"register",dir.toString());
    send(m,"Str:a\u0000b");
    assertEquals(List.of(
      "The manager was asked to register a folder, but the path in the message is not a tagged text: \""+dir+"\" is malformed: it starts with \"Str:\", \"UStr:\" or \"Base16:\", then the text written that way.",
      "The manager was asked to register \"a\" [Null 0x00] \"b\", but that is not a path this system accepts: Nul character not allowed."),view.notes);
    assertEquals(List.of(),listed(dir));
  }
  @Test void aMissingOrUnknownKindMakesTheProjectIdleAndClearsItsCache(@TempDir Path dir){
    var m= manager(dir,"hello.Hello");
    var hello= folder(dir,"hello");
    send(m,TaggedText.of(hello.toString()));
    send(m,"compile","hello");
    idle(m);
    var info= dir.resolve("manager").resolve("projects.info");
    Fs.writeUtf8(info,Fs.readUtf8(info).replace("\"kind\": \"code\"","\"kind\": \"library\""));
    var again= manager(dir,"hello.Hello");
    again.settle();
    assertEquals(Kind.idle,project(again,hello).kind());
    assertFalse(Files.exists(hello.resolve(Facts.outDir)));
    assertEquals("In projects.info the \"kind\" of \"hello\" was missing or not one of the kinds: \"hello\" is now idle, and its compiled cache is deleted.",view.notes.getLast());
    same("[###]\"kind\": \"idle\"[###]",Fs.readUtf8(info));
  }
  @Test void compilingForgetsSelectedMainsThatAreGoneAndRunningRefusesThem(@TempDir Path dir){
    var mains= new ArrayList<>(List.of("hello.One","hello.Two"));
    var m= manager(dir,_->Optional.of(mains.stream().collect(Collectors.toMap(k->k,_->"_hello/_rank_app.fear"))));
    var hello= folder(dir,"hello");
    send(m,TaggedText.of(hello.toString()));
    send(m,"mains","hello","hello.One hello.Old hello.Two");
    send(m,"compile","hello");
    idle(m);
    assertEquals(List.of("hello.One","hello.Two"),project(m,hello).entry().mains());
    send(m,"mains","hello","hello.Old hello.Two");
    send(m,"clear","hello");
    send(m,"run","hello");
    idle(m);
    assertEquals("--- nothing to run: the selected [hello.Old] are not mains of this project; they are removed from the selected mains ---\n",eclipse(dir,"hello","console.txt"));
    assertEquals(List.of("hello.Two"),project(m,hello).entry().mains());
  }
  @Test void mainsThatCanNotBeReadMakeTheProjectInvalidAndOutOfDateAndAreNotReadAgainWhileNothingChanges(@TempDir Path dir){
    var reads= new ArrayList<Path>();
    var m= manager(dir,f->{ reads.add(f); throw Report.launchPathNotFound(dir.resolve("gone")); });
    var hello= folder(dir,"hello");
    send(m,TaggedText.of(hello.toString()));
    send(m,"run","hello");
    idle(m);
    send(m,"select","hello");
    send(m,"check","hello");
    assertEquals(List.of(hello),reads);
    var p= project(m,hello);
    var error= Report.launchPathNotFound(dir.resolve("gone")).getMessage();
    assertEquals(Project.State.codeInvalid,p.state());
    assertTrue(p.needsCompiling());
    assertEquals(error,p.problem().orElseThrow());
    assertEquals("--- compiling hello ---\ncompiled\n--- compile done ---\n"+error.stripTrailing()+"\n"+error.stripTrailing()+"\n",eclipse(dir,"hello","console.txt"));
  }
  @Test void aCompileWhoseJvmDoesNotStartIsShownOnItsProject(@TempDir Path dir){
    var m= manager(dir,"hello.Hello");
    var noJvm= folder(dir,"nojvm");
    send(m,TaggedText.of(noJvm.toString()));
    send(m,"run","nojvm");
    assertEquals("--- compiling nojvm ---\n--- compile did not start: io: Cannot run program \"java\": error=2, No such file or directory ---\n",eclipse(dir,"nojvm","console.txt"));
    var p= project(m,noJvm);
    assertEquals(List.of(Project.State.codeNoCache,-1L),List.of(p.state(),p.entry().compiled()));
  }
  @Test void aRunReportsUnderTheNameItsProjectHasNow(@TempDir Path dir){
    var m= manager(dir,"hello.Slow");
    var hello= folder(dir,"hello");
    send(m,TaggedText.of(hello.toString()));
    send(m,"run","hello");
    until(m,_->eclipse(dir,"hello","console.txt").contains("ran hello.Slow"));
    commit(m,"{\"other\": {\"path\": \"Str:"+hello.toString().replace('\\','/')+"\", \"kind\": \"code\"}}",()->{});
    var old= dir.resolve("manager").resolve("eclipse").resolve("hello").resolve("report.xml");
    Fs.ofV(()->Files.deleteIfExists(old));
    until(m,_->Files.exists(dir.resolve("manager").resolve("eclipse").resolve("other").resolve("report.xml")));
    send(m,"terminate","other");
    idle(m);
    same("[###]<testsuite name=\"hello.Slow\" tests=\"1\" failures=\"0\" errors=\"0\">\n<testcase classname=\"a\"[###]",eclipse(dir,"other","report.xml"));
    assertFalse(Files.exists(old));
  }
  @Test void aLogTheReportCanNotReadStopsTheReportOnly(@TempDir Path dir){
    var m= manager(dir,"hello.Garbled");
    var hello= folder(dir,"hello");
    send(m,TaggedText.of(hello.toString()));
    send(m,"run","hello");
    idle(m);
    same("[###]ran hello.Garbled\n--- the report of hello.Garbled stops: malformed input: [###] ---\n--- hello.Garbled exited with 0 after [###]s ---\n",eclipse(dir,"hello","console.txt"));
  }
  @Test void mainsTheCompilerWouldCompileMakeTheProjectOutOfDate(@TempDir Path dir){
    var m= manager(dir,_->Optional.empty());
    var hello= folder(dir,"hello");
    send(m,TaggedText.of(hello.toString()));
    send(m,"compile","hello");
    idle(m);
    var p= project(m,hello);
    assertEquals(Project.State.codeOutdated,p.state());
    assertEquals("Compile",p.action().text());
  }
  @Test void aProjectChangedWhileItsMainsAreReadIsCheckedAgain(@TempDir Path dir){
    var reads= new ArrayList<Path>();
    var m= manager(dir,f->{
      reads.add(f);
      if (reads.size() > 1){ return Optional.of(Map.of("hello.Hello","_hello/_rank_app.fear")); }
      Fs.writeUtf8(f.resolve("readme.txt"),"new\n");
      throw Report.launchPathNotFound(f);
    });
    var hello= folder(dir,"hello");
    send(m,TaggedText.of(hello.toString()));
    send(m,"compile","hello");
    idle(m);
    var p= project(m,hello);
    assertEquals(2,reads.size());
    assertEquals(Project.State.codeCompiled,p.state());
    assertEquals(List.of("hello.Hello"),p.knownMains());
  }
  @Test void aNewManagerRemembersTheProjectsAndStartsWithEmptyConsoles(@TempDir Path dir){
    var m= manager(dir,"hello.Hello");
    var hello= folder(dir,"hello");
    send(m,TaggedText.of(hello.toString()));
    send(m,"run","hello");
    idle(m);
    var again= manager(dir,"hello.Hello");
    again.settle();
    assertEquals("",eclipse(dir,"hello","console.txt"));
    var p= project(again,hello);
    assertEquals(Project.State.codeCompiled,p.state());
    assertTrue(p.entry().run() > 0);
  }
  @Test void aWindowsFolderWhoseNameIsNotUnicodeIsRegisteredAndRememberedAsBase16(@TempDir Path dir){
    Assumptions.assumeTrue(Fs.isWindows());
    var m= manager(dir,"hello.Hello");
    var hello= folder(folder(dir,"a\ud800"),"hello");
    send(m,Main.message(hello.toString()));
    assertEquals(List.of("hello "+hello),listed(dir));
    same("[###]\"path\": \"Base16:[###]0000D82F00680065006C006C006F00\",[###]",Fs.readUtf8(dir.resolve("manager").resolve("projects.info")));
    var again= manager(dir,"hello.Hello");
    again.settle();
    assertEquals(Kind.code,project(again,hello).kind());
  }
  @Test void aFolderUnderAPathOutsideTheCharacterSetIsRegisteredRunAndRemembered(@TempDir Path dir){
    var m= manager(dir,"hello.Hello");
    var hello= folder(folder(dir,"caf\u00e9 \ud83d\ude00"),"hello");
    send(m,TaggedText.of(hello.toString()));
    send(m,"run","hello");
    idle(m);
    assertEquals(List.of("hello "+hello),listed(dir));
    same("[###]\"folder\": \"UStr:\\\"[###]caf\\\".u\\\"00E9\\\"+(\\\" \\\".u\\\"1F600\\\")+(\\\"[###]hello\\\".u)\",[###]",eclipse(dir,"state.info"));
    same("[###]ran hello.Hello[###]",eclipse(dir,"hello","console.txt"));
    same("[###]\"path\": \"UStr:\\\"[###]/caf\\\".u\\\"00E9\\\"+(\\\" \\\".u\\\"1F600\\\")+(\\\"/hello\\\".u)\",[###]",Fs.readUtf8(dir.resolve("manager").resolve("projects.info")));
    var again= manager(dir,"hello.Hello");
    again.settle();
    assertEquals(Project.State.codeCompiled,project(again,hello).state());
  }
}

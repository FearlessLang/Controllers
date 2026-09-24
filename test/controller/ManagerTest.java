package controller;

import static controller.Errs.same;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import controller.Manager.State;
import controller.Registry.Kind;
import tools.ChildJvm;
import tools.Fs;

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
  record Fake(Map<String,String> mains) implements Manager.Tools{
    @Override public ChildJvm compile(Path folder, Path reports, Consumer<String> out){
      FactsTest.cache(folder,"hello",FactsTest.after(folder));
      return jvm(out,"compiled","0","0");
    }
    @Override public ChildJvm run(Path folder, String main, Consumer<String> out){
      return jvm(out,"ran "+main,"0",main.endsWith("Slow") ? "60000" : "0");
    }
    @Override public Optional<Map<String,String>> mains(Path folder){ return Optional.of(mains); }
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
  }
  private final List<RuntimeException> failures= Collections.synchronizedList(new ArrayList<>());
  private final View view= new View();
  @AfterEach void nothingFailed(){ assertEquals(List.of(),failures); }
  private Manager manager(Path dir, String... mains){
    var map= new LinkedHashMap<String,String>();
    for (var m: mains){ map.put(m,"_hello/_rank_app.fear"); }
    return new Manager(dir.resolve("manager"),new Fake(Collections.unmodifiableMap(map)),view,failures::add);
  }
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
  private static void until(Manager m, Predicate<State> done){
    while(true){
      m.settle();
      if (done.test(m.state())){ return; }
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
  @Test void aMessageWithNoFolderShowsTheWindow(@TempDir Path dir){
    var m= manager(dir);
    send(m,"");
    assertEquals(1,view.shown);
    assertEquals("",eclipse(dir,"projects.txt"));
  }
  @Test void selectingAnEmptyFolderMakesItAHelloWorldCodeProject(@TempDir Path dir){
    var m= manager(dir);
    var hello= folder(dir,"hello");
    send(m,hello.toString());
    assertEquals("hello "+hello+"\n",eclipse(dir,"projects.txt"));
    assertTrue(Files.isRegularFile(hello.resolve("hello.fearless")));
    assertTrue(Files.isRegularFile(hello.resolve("_hello").resolve("_rank_app.fear")));
    var p= project(m,hello);
    assertEquals(Kind.code,p.kind());
    assertEquals(Project.State.codeNoCache,p.state());
    assertEquals(Optional.of(hello),m.state().selected());
    assertEquals(1,view.shown);
    assertTrue(eclipse(dir,"hello","state.txt").contains("\"needsCompiling\": \"true\""));
  }
  @Test void aFileSelectsTheFolderItIsIn(@TempDir Path dir){
    var m= manager(dir);
    var hello= folder(dir,"hello");
    Fs.writeUtf8(hello.resolve("hello.fearless"),"");
    send(m,"select",hello.resolve("hello.fearless").toString());
    assertEquals("hello "+hello+"\n",eclipse(dir,"projects.txt"));
  }
  @Test void aRunMessageCompilesThenRunsTheOnlyMain(@TempDir Path dir){
    var m= manager(dir,"hello.Hello");
    var hello= folder(dir,"hello");
    send(m,hello.toString());
    send(m,"run",hello.toString());
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
    send(m,hello.toString());
    send(m,"compile",hello.toString());
    idle(m);
    var once= eclipse(dir,"hello","console.txt");
    send(m,"compile",hello.toString());
    idle(m);
    assertEquals(once,eclipse(dir,"hello","console.txt"));
    assertEquals(Optional.of(Map.of("hello.Hello","_hello/_rank_app.fear")),project(m,hello).mains());
  }
  @Test void aBusyProjectRefusesJobsCleaningAndKindChangesThenTerminateEndsTheJob(@TempDir Path dir){
    var m= manager(dir,"hello.Slow");
    var hello= folder(dir,"hello");
    send(m,hello.toString());
    send(m,"run",hello.toString());
    until(m,_->eclipse(dir,"hello","console.txt").contains("ran hello.Slow"));
    assertEquals(Optional.of("hello.Slow"),project(m,hello).running());
    assertTrue(eclipse(dir,"hello","state.txt").contains("\"running\": \"hello.Slow\""));
    send(m,"run",hello.toString());
    send(m,"compile",hello.toString());
    send(m,"clean",hello.toString());
    send(m,"kind",hello.toString(),"idle");
    send(m,"terminate",hello.toString());
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
    send(m,hello.toString());
    send(m,"mains",hello.toString(),"hello.Two hello.Slow");
    send(m,"run",hello.toString());
    until(m,_->eclipse(dir,"hello","console.txt").contains("ran hello.Slow"));
    send(m,"terminate",hello.toString());
    idle(m);
    assertFalse(eclipse(dir,"hello","console.txt").contains("hello.Two"));
  }
  @Test void theSelectedMainsRunInTheOrderOfTheMains(@TempDir Path dir){
    var m= manager(dir,"hello.One","hello.Two","hello.Three");
    var hello= folder(dir,"hello");
    send(m,hello.toString());
    send(m,"run",hello.toString());
    idle(m);
    assertTrue(eclipse(dir,"hello","console.txt").endsWith("--- nothing to run: none of [hello.One, hello.Two, hello.Three] is selected ---\n"));
    send(m,"mains",hello.toString(),"hello.Three hello.One");
    send(m,"clear",hello.toString());
    send(m,"run",hello.toString());
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
    send(m,hello.toString());
    send(m,"compile",hello.toString());
    idle(m);
    send(m,"clear",hello.toString());
    send(m,"run",hello.toString(),"hello.Nope");
    send(m,"run",hello.toString(),"hello.Two");
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
    send(m,hello.toString());
    send(m,"run",hello.toString());
    until(m,_->eclipse(dir,"hello","console.txt").contains("ran hello.Slow"));
    send(m,"forget",hello.toString());
    assertEquals("",eclipse(dir,"projects.txt"));
    assertEquals(Optional.empty(),m.state().selected());
    send(m,"run",hello.toString());
    assertTrue(view.notes.getLast().startsWith("Fearless was asked to \"run\" a folder it does not keep track of:"));
    assertTrue(Files.isRegularFile(hello.resolve("hello.fearless")));
  }
  @Test void onlySelectRegistersAFolder(@TempDir Path dir){
    var m= manager(dir);
    var hello= folder(dir,"hello");
    send(m,"compile",hello.toString());
    assertEquals("",eclipse(dir,"projects.txt"));
    same("""
      Fearless was asked to "compile" a folder it does not keep track of:
      [###]hello
      Only "select" adds a folder to the projects Fearless keeps track of; every
      other request applies to a folder Fearless already keeps track of.
      """,view.notes.getFirst());
    assertEquals(view.notes.getFirst().stripTrailing()+"\n",eclipse(dir,"console.txt"));
  }
  @Test void aRequestTheManagerDoesNotKnowIsRefused(@TempDir Path dir){
    var m= manager(dir);
    var hello= folder(dir,"hello");
    send(m,hello.toString());
    send(m,"build",hello.toString());
    send(m,"kind",hello.toString(),"library");
    send(m,"link",hello.toString(),"data sometimes");
    assertEquals(3,view.notes.size());
    assertTrue(view.notes.get(0).startsWith("The manager was asked to \"build\" a project, but \"build\" is not a request it knows"));
    assertTrue(view.notes.get(1).endsWith("to \"library\", but the kinds are \"idle\", \"code\", \"data:readOnly\" and \"data:readWrite\"."));
    assertTrue(view.notes.get(2).endsWith("with \"data sometimes\", but a link is a project name, then \"none\", \"read\" or \"write\", then the type names."));
  }
  @Test void aFolderInsideARegisteredOneIsRefused(@TempDir Path dir){
    var m= manager(dir);
    var hello= folder(dir,"hello");
    send(m,hello.toString());
    send(m,folder(hello,"inner").toString());
    assertEquals(1,view.notes.size());
    assertEquals("hello "+hello+"\n",eclipse(dir,"projects.txt"));
  }
  @Test void kindsChangeFromIdleAndBackToIdleOnly(@TempDir Path dir){
    var m= manager(dir);
    var data= data(dir);
    send(m,data.toString());
    assertEquals(Kind.idle,project(m,data).kind());
    send(m,"kind",data.toString(),"data:readOnly");
    send(m,"kind",data.toString(),"code");
    assertEquals(Kind.dataReadOnly,project(m,data).kind());
    assertEquals("--- kind change refused: a project of kind data:readOnly goes back to idle before becoming code ---\n",eclipse(dir,"data","console.txt"));
    send(m,"kind",data.toString(),"idle");
    assertEquals(Kind.idle,project(m,data).kind());
  }
  @Test void compilingAProjectThatIsNotCodeChecksIt(@TempDir Path dir){
    var m= manager(dir);
    var data= data(dir);
    send(m,data.toString());
    send(m,"compile",data.toString());
    Fs.writeUtf8(data.resolve("Bad Name.txt"),"");
    send(m,"compile",data.toString());
    same("""
      --- ok: no problem found ---
      [###]Bad Name.txt[###]
      """,eclipse(dir,"data","console.txt"));
    assertEquals(Project.State.dataInvalid,project(m,data).state());
  }
  @Test void linksAreEditedByRequestAndBrokenLinksStopTheCompile(@TempDir Path dir){
    var m= manager(dir);
    var hello= folder(dir,"hello");
    var data= data(dir);
    send(m,hello.toString());
    send(m,data.toString());
    send(m,"kind",data.toString(),"data:readWrite");
    send(m,"link",hello.toString(),"data write Data Pub");
    var entry= project(m,hello).entry();
    assertEquals(List.of(Map.of("data",List.of("Data","Pub")),Map.of("data",List.of("Data","Pub"))),List.of(entry.reads(),entry.edits()));
    assertEquals(Optional.empty(),project(m,hello).problem());
    send(m,"link",hello.toString(),"data read lower");
    assertTrue(view.notes.getLast().contains("\"lower\" in \"reads\".\"data\" is not a Fearless type name"));
    assertEquals(entry,project(m,hello).entry());
    send(m,"kind",data.toString(),"idle");
    assertEquals(Project.State.codeInvalid,project(m,hello).state());
    send(m,"compile",hello.toString());
    assertEquals("\"reads\" refers to \"data\", but the kind of \"data\" is \"idle\"; \"reads\" accepts only \"data:readOnly\" or \"data:readWrite\".\n",eclipse(dir,"hello","console.txt"));
    send(m,"link",hello.toString(),"data none");
    assertEquals(Project.State.codeNoCache,project(m,hello).state());
  }
  @Test void theMetadataIsCommittedWholeOrNotAtAll(@TempDir Path dir){
    var m= manager(dir);
    var data= data(dir);
    send(m,data.toString());
    var done= new ArrayList<String>();
    m.commit("{\"data\": {\"path\": \"nowhere\"}}",()->done.add("bad"));
    m.settle();
    assertEquals(List.of(),done);
    assertTrue(view.notes.getLast().contains("\"path\" must be an absolute path"));
    m.commit(Registry.text(List.of(project(m,data).entry().withKind(Kind.dataReadWrite))),()->done.add("good"));
    m.settle();
    assertEquals(List.of("good"),done);
    assertEquals(Kind.dataReadWrite,project(m,data).kind());
    m.commit("{}",()->done.add("empty"));
    m.settle();
    assertEquals("",eclipse(dir,"projects.txt"));
  }
  @Test void aProjectWhoseFolderIsGoneIsInvalidAndCanBeForgotten(@TempDir Path dir){
    var m= manager(dir);
    var data= data(dir);
    send(m,data.toString());
    Fs.rmTree(data);
    send(m,"select",data.toString());
    assertEquals(Optional.of(data),m.state().selected());
    assertEquals(List.of(),view.notes);
    send(m,"compile",data.toString());
    assertTrue(eclipse(dir,"data","console.txt").startsWith("The folder of this project does not exist:"));
    send(m,"forget",data.toString());
    assertEquals("",eclipse(dir,"projects.txt"));
    m.ask("select",data,"");
    m.settle();
    assertTrue(view.notes.getLast().startsWith("Nothing exists at the given path."));
    assertEquals("",eclipse(dir,"projects.txt"));
  }
  @Test void renamingAProjectKeepsItsConsole(@TempDir Path dir){
    var m= manager(dir);
    var data= data(dir);
    send(m,data.toString());
    send(m,"compile",data.toString());
    m.commit("{\"other\": {\"path\": \""+data.toString().replace('\\','/')+"\"}}",()->{});
    m.settle();
    assertEquals("--- ok: no problem found ---\n",eclipse(dir,"other","console.txt"));
  }
  @Test void aNewManagerRemembersTheProjectsAndStartsWithEmptyConsoles(@TempDir Path dir){
    var m= manager(dir,"hello.Hello");
    var hello= folder(dir,"hello");
    send(m,hello.toString());
    send(m,"run",hello.toString());
    idle(m);
    var again= manager(dir,"hello.Hello");
    again.settle();
    assertEquals("",eclipse(dir,"hello","console.txt"));
    var p= project(again,hello);
    assertEquals(Project.State.codeCompiled,p.state());
    assertTrue(p.entry().run() > 0);
  }
}

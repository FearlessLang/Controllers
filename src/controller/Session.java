package controller;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

import coordinator.CapabilityEnvironment;
import coordinator.Coordinator;
import core.E.Literal;
import core.OtherPackages;
import fileSupport.JUnitReport;
import naiveBackend.BackendTools;
import tools.ChildJvm;
import tools.JavacTool;
import tools.SourceOracle;
import userMessages.UserError;
import userMessages.Violation;
import utils.Bug;

/// The work Fearless does on one project: reading its mains, compiling it in a child
/// JVM, running its mains one at a time, and killing whichever child is alive.
/// At most one job runs per project; a job asked for while another runs is refused.
public final class Session{
  private final Path folder;
  private final Path reports;
  private final Executor worker;
  private final Consumer<String> out;
  private final Runnable changed;
  private static final String compiling= "compiling";
  private ChildJvm child;
  private String current= "";
  private Instant since= Instant.now();
  private int exit= -1;
  private int runs= 0;
  private String lastRun= "";
  private Optional<Map<String,String>> mains= Optional.empty();
  public Session(Path folder, Path reports, Executor worker, Consumer<String> out, Runnable changed){
    this.folder= folder;
    this.reports= reports;
    this.worker= worker;
    this.out= out;
    this.changed= changed;
  }
  public synchronized boolean busy(){ return !current.isEmpty(); }
  public synchronized String current(){ return current; }
  public synchronized Duration elapsed(){ return Duration.between(since,Instant.now()); }
  public synchronized Optional<List<String>> mains(){ return mains.map(m->List.copyOf(m.keySet())); }
  public synchronized Optional<Map<String,String>> mainFiles(){ return mains; }
  public synchronized Optional<String> running(){ return child == null ? Optional.empty() : Optional.of(current); }
  public synchronized Optional<String> runningMain(){ return running().filter(r->!r.equals(compiling)); }
  public synchronized int exit(){ return exit; }
  public synchronized int runs(){ return runs; }
  public synchronized String lastRun(){ return lastRun; }
  public synchronized void refresh(){ if (!busy()){ submit("read the mains","reading",this::readMains); } }
  public void compile(){ submit("compile",compiling,this::doCompile); }
  public void run(Optional<String> named, List<String> selected){ submit("run","starting",()->doRun(named,selected)); }
  public void compileThenRun(Optional<String> named, List<String> selected){ submit("run",compiling,()->{ if (doCompile()){ doRun(named,selected); } }); }
  public synchronized void terminate(){
    if (child == null){ return; }
    out.accept("--- terminating "+current+" ---\n");
    child.kill();
  }
  public synchronized boolean refused(String request){
    while(current.equals("reading")){ waitOrBug(0); }
    if (!busy()){ return false; }
    out.accept("--- "+request+" refused: the project is busy with "+current+" ---\n");
    return true;
  }
  private synchronized void submit(String request, String what, Runnable job){
    if (refused(request)){ return; }
    starting(what);
    worker.execute(()->guard(job));
  }
  private void guard(Runnable job){
    try{ job.run(); }
    catch(UserError e){ out.accept(e.getMessage()); }
    catch(Throwable t){ out.accept(UserError.crash(t)); }
    finally{ done(); }
  }
  private synchronized void done(){
    current= "";
    notifyAll();
    changed.run();
  }
  private synchronized void starting(String what){
    current= what;
    since= Instant.now();
    changed.run();
  }
  private void readMains(){
    Optional<Map<String,String>> res;
    try{ var c= coordinator(); res= c.mains(folder,c.sourceOracle(stdLib("base"))); }
    catch(UserError _){ res= Optional.empty(); }
    synchronized(this){ mains= res; }
  }
  private Coordinator coordinator(){
    return new Coordinator(){
      @Override public Optional<Path> baseCachePath(){ return Optional.of(stdLib("baseCache")); }
      @Override public BackendTools backendTools(String pkgName, SourceOracle oracle, OtherPackages other, List<Literal> core, CapabilityEnvironment capabilities){
        return BackendTools.of(pkgName,oracle,other,core,folder.resolve(Coordinator.outDir),baseCachePath(),stdLib("rt"),capabilities);
      }
    };
  }
  private boolean doCompile(){
    out.accept("--- compiling "+folder.getFileName()+" ---\n");
    var ec= await(()->ChildJvm.start(compileArgs(),out),()->{});
    out.accept("--- compile "+(ec == 0 ? "done" : "failed with "+ec)+" ---\n");
    readMains();
    return ec == 0;
  }
  private void doRun(Optional<String> named, List<String> selected){
    readMains();
    var known= mains();
    if (known.isEmpty()){ out.accept("--- this project needs compiling ---\n"); return; }
    var all= known.get();
    if (all.isEmpty()){ out.accept("--- nothing to run: this project has no main ---\n"); return; }
    if (named.isPresent() && !all.contains(named.get())){ out.accept("--- nothing to run: "+named.get()+" is not one of the mains "+all+" ---\n"); return; }
    var chosen= named.map(List::of).orElseGet(()->all.size() == 1 ? all : all.stream().filter(selected::contains).toList());
    if (chosen.isEmpty()){ out.accept("--- nothing to run: none of "+all+" is selected ---\n"); return; }
    chosen.forEach(this::runOne);
  }
  private void runOne(String main){
    var started= Instant.now();
    synchronized(this){ runs+= 1; lastRun= main; }
    starting(main);
    out.accept("--- running "+main+" ---\n");
    var ec= await(()->Coordinator.startMain(folder,stdLib("base"),main,coordinator().sharedClasspath(),out),()->JUnitReport.write(reports,folder,main,started));
    synchronized(this){ exit= ec; }
    out.accept("--- "+main+" exited with "+ec+" after "+elapsed().toSeconds()+"s ---\n");
  }
  //meanwhile runs every two seconds while the child lives, and once more after it exits.
  private int await(Supplier<ChildJvm> start, Runnable meanwhile){
    ChildJvm started;
    synchronized(this){ started= start.get(); child= started; }
    var publishing= CompletableFuture.runAsync(()->publish(meanwhile),worker);
    try{ return started.await(); }
    catch(InterruptedException e){ throw Bug.of(e.toString()); }
    finally{
      synchronized(this){ child= null; notifyAll(); }
      publishing.join();
    }
  }
  private synchronized void publish(Runnable meanwhile){
    do{
      if (child != null){ waitOrBug(2000); }
      meanwhile.run();
    } while(child != null);
  }
  private void waitOrBug(long millis){
    try{ wait(millis); }
    catch(InterruptedException e){ throw Bug.of(e.toString()); }
  }
  public static Path stdLib(String name){ return JavacTool.reqAppDir(Violation::mustUseLauncher).resolve("stdLib").resolve(name); }
  private List<String> compileArgs(){
    var appDir= JavacTool.reqAppDir(Violation::mustUseLauncher);
    return List.of(
      "-Djava.awt.headless=true",
      "-D"+JavacTool.appDirKey+"="+appDir,
      "-D"+JavacTool.launcherKey+"="+JavacTool.consoleKey,
      "-D"+JavacTool.versionIdKey+"="+JavacTool.reqVersionId(Violation::mustUseLauncher),
      "--enable-native-access=Commons,Coordinator",
      "-p", appDir.resolve(JavacTool.deployedModsDirName).toString(),
      "-m", "Controller/controller.ChildMain",
      folder.toString(),
      reports.toString());
  }
}
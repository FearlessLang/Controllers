package controller;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import controller.Registry.Entry;
import controller.Registry.Kind;
import fileSupport.NativeLocaleForcer;
import mainCoordinator.MakeDemo;
import realSourceOracle.AutoloadHandler;
import fileSupport.StringFiles;
import gui.Tray;
import gui.Window;
import tools.Fs;
import tools.JavacTool;
import userMessages.Report;
import userMessages.UserError;
import userMessages.Violation;
import utils.Bug;

/// The manager process. Every launch leaves one message file (the folder it was
/// started on) in the manager folder, then tries to take the instance lock: the one
/// process holding it owns the window and drains every message, its own included.
/// A message naming no folder shows that window, so starting the manager again is what
/// brings back a window that was closed.
/// Resources that live for the whole process life (the lock, the watch service, the
/// workers, the window) are never closed by us: every path out of main leads to
/// System.exit, and the operating system reclaims them on process death.
public final class Main{
  private static FileLock lock;
  public final Path managerDir;
  public final Registry registry;
  public final Eclipse eclipse;
  public final ExecutorService worker= Executors.newVirtualThreadPerTaskExecutor();
  private final Stop stop= new Stop();
  private Window window;
  private Main(Path managerDir){
    this.managerDir= managerDir;
    registry= new Registry(managerDir);
    eclipse= new Eclipse(managerDir.resolve("eclipse"));
  }
  public Path msgDir(){ return managerDir.resolve("messages"); }
  public static void main(String[] args){
    NativeLocaleForcer.forceEnglish();
    var exitCode= 0;
    try{ run(args.length == 0 ? "" : args[0]); }
    catch(UserError e){ exitCode= 1; display(e); }
    catch(VirtualMachineError|LinkageError e){ exitCode= 2; display(Violation.vmOrLinkageFailure(e)); }
    catch(Throwable t){ exitCode= 3; display(UserError.crashed(t)); }
    System.exit(exitCode);
  }
  private static void display(UserError e){
    try{ e.display(); }
    catch(InterruptedException ie){ e.displayStderr(ie); }
  }
  private static void run(String message){
    var binDir= binDir();
    var host= Install.isInstalled(binDir) ? Install.userDataHome() : binDir.getParent();
    var main= new Main(host.resolve(JavacTool.dataDirNameFor(versionId())));
    try{ Files.createDirectories(main.msgDir()); }
    catch(IOException|UnsupportedOperationException|SecurityException e){ throw Violation.couldNotCreateManagerFolder(main.managerDir,e); }
    leave(main.msgDir(),message);
    var lockFile= main.managerDir.resolve("instance.lock");
    try{ lock= FileChannel.open(lockFile,CREATE,WRITE).tryLock(); }
    catch(OverlappingFileLockException e){ throw Bug.unreachable(); }
    catch(IOException e){ throw Violation.couldNotUseInstanceLock(lockFile,e); }
    if (lock == null){ return; }
    UserError.becameManagerOwner();
    main.own();
  }
  private static String versionId(){ return JavacTool.reqVersionId(Violation::mustUseLauncher); }
  private static Path binDir(){
    var expected= "fearlessManaged"+versionId()+(Fs.isMac() ? ".app" : "");
    var startedFrom= JavacTool.reqAppDir(Violation::mustUseLauncher).toAbsolutePath().normalize();
    for(var dir= startedFrom; dir != null && dir.getFileName() != null; dir= dir.getParent()){
      if (dir.getFileName().toString().equals(expected)){ return dir; }
    }
    throw Violation.programFolderNotFound(startedFrom,expected);
  }
  //Write to a .tmp name, then atomically rename it to .msg: the owner drains
  //only *.msg, so it can never observe a half written file.
  private static void leave(Path msgDir, String message){
    var name= "%020d-%s".formatted(System.currentTimeMillis(),UUID.randomUUID());
    var tmp= msgDir.resolve(name+".tmp");
    StringFiles.writeNew(tmp,message,UserError.onFileError());
    try{ Files.move(tmp,msgDir.resolve(name+".msg"),ATOMIC_MOVE); }
    catch(IOException e){ throw Violation.couldNotLeaveStartMessage(msgDir,e); }
  }
  private void own(){
    WatchService watcher;
    try{ watcher= FileSystems.getDefault().newWatchService(); msgDir().register(watcher,ENTRY_CREATE); }
    catch(IOException|UnsupportedOperationException|SecurityException e){ throw Violation.couldNotWatchMessageFolder(msgDir(),e); }
    Thread.setDefaultUncaughtExceptionHandler((_,t)->stop.fail(t instanceof UserError e ? e : Bug.of(t)));
    window= Window.create(this);
    UserError.owner(window.frame);
    Violation.running(window::runningPrograms);
    Tray.install(window,this);
    window.show();
    drain();
    worker.submit(stop.worker(()->watch(watcher)));
    if (!Fs.isMac()){ Association.launcher().ifPresent(l->Association.reconcile(l,Association.extensions(l))); }
    try{ stop.await(); }
    finally{ worker.shutdownNow(); }
  }
  public void quit(){ stop.quit(); }
  public void fail(UserError problem){ stop.fail(problem); }
  public void forgetAssociation(){
    if (Fs.isMac() || !window.askForget()){ return; }
    try{ Association.launcher().ifPresent(l->Association.reconcile(l,List.of())); }
    catch(UserError e){ display(e); System.exit(1); }
    System.exit(0);
  }
  private void watch(WatchService watcher){
    while(true){
      WatchKey key;
      try{ key= watcher.take(); }
      catch(InterruptedException e){ return; }
      key.pollEvents();
      drain();
      if (!key.reset()){ throw Violation.messageFolderNotWatchable(msgDir()); }
    }
  }
  //Runs once before the watcher worker starts, then only from that worker: never concurrently.
  private void drain(){
    List<String> messages;
    try{ messages= take(); }
    catch(IOException e){ throw Violation.couldNotDrainMessageFolder(msgDir(),e); }
    if (messages.isEmpty()){ return; }
    messages.forEach(this::apply);
    window.foldersChanged();
  }
  private List<String> take() throws IOException{
    var files= list("*.msg");
    files.sort(Comparator.comparing(f->f.getFileName().toString()));
    var messages= new ArrayList<String>();
    for(var file: files){
      messages.add(StringFiles.read(file,UserError.onFileError()));
      Files.deleteIfExists(file);
    }
    var old= Instant.now().minusSeconds(60);
    for(var file: list("*.tmp")){
      try{ if (Files.getLastModifiedTime(file).toInstant().isBefore(old)){ Files.deleteIfExists(file); } }
      catch(NoSuchFileException e){}
    }
    return messages;
  }
  private List<Path> list(String glob) throws IOException{
    var files= new ArrayList<Path>();
    try(var stream= Files.newDirectoryStream(msgDir(),glob)){ stream.forEach(files::add); }
    return files;
  }
  public void ask(String message){ leave(msgDir(),message); }
  //A message is the folder to select, or a verb, a newline, then the folder, then
  //for run the optional main to run, and for kind the kind text as in the metadata file.
  private void apply(String message){
    var lines= message.lines().toList();
    var verb= lines.size() > 1 ? lines.getFirst() : "select";
    var at= lines.isEmpty() ? "" : lines.get(lines.size() > 1 ? 1 : 0);
    if (at.isBlank()){ window.show(); return; }
    Path folder;
    try{ folder= verb.equals("select") ? projectFolder(at,managerDir) : Path.of(at).toAbsolutePath().normalize(); }
    catch(UserError e){ tell(e); return; }
    if (!registry.has(folder) && !verb.equals("select")){ tell(Report.notRegistered(verb,folder)); return; }
    if (!registry.has(folder) && !add(folder)){ return; }
    switch(verb){
      case "select" -> { window.show(); window.select(folder); }
      case "run" -> { window.select(folder); window.run(folder,lines.size() > 2 ? Optional.of(lines.get(2)) : Optional.empty()); }
      case "terminate" -> { window.select(folder); window.terminate(folder); }
      case "compile" -> { window.select(folder); window.compile(folder); }
      case "clean" -> { window.select(folder); window.clean(folder); }
      case "kind" -> window.kind(folder,Kind.of(lines.get(2)).orElseThrow(Bug::unreachable));
      case "forget" -> window.forget(folder);
      default -> throw Bug.unreachable();
    }
  }
  private void tell(UserError e){
    eclipse.note(e.getMessage()+"\n");
    window.explain(e);
  }
  private boolean add(Path folder){
    var nested= registry.overlapping(folder);
    if (nested.isPresent()){ tell(Report.folderNestedWithRegistered(folder,nested.get())); return false; }
    var wanted= Names.compactName(folder);
    var fresh= Fs.of(()->{ try(var s= Files.list(folder)){ return s.findAny().isEmpty(); } });
    var taken= registry.all().stream().map(Entry::alias).collect(Collectors.toSet());
    var alias= Names.makeUnique(folder,taken);
    if (!alias.equals(wanted)){ tell(Report.projectNamed(folder,wanted,alias)); }
    Fs.rmTree(folder.resolve(Facts.outDir));
    Fs.rmTree(eclipse.reports(alias));
    registry.add(alias,folder);
    if (fresh){
      registry.update(folder,e->e.withKind(Kind.code));
      MakeDemo.hello(folder,Names.pkgName(alias),AutoloadHandler.capFirst(alias));
    }
    window.foldersChanged();
    return true;
  }
  //The manager folder is not a project: a Fearless started on it registers nothing.
  static Path projectFolder(String given, Path managerDir){
    Path path;
    try{ path= Path.of(given).toAbsolutePath().normalize(); }
    catch(InvalidPathException e){ throw Violation.badLaunchArg(given,false); }
    if (!Files.exists(path)){ throw Report.launchPathNotFound(path); }
    var folder= Files.isDirectory(path) ? path : path.getParent();
    var manager= managerDir.toAbsolutePath().normalize();
    if (folder.getFileName() == null){ throw Report.projectFolderIsRoot(folder); }
    if (folder.startsWith(manager) || manager.startsWith(folder)){ throw Report.managerFolderNotAProject(path,manager); }
    var unsafe= folder.toString().codePoints().filter(c->Fs.allowed.indexOf(c) < 0).findFirst();
    if (unsafe.isPresent()){ throw Report.projectFolderUnsafePath(folder,unsafe.getAsInt()); }
    return folder;
  }
}
//Note: A worker stopped by shutdownNow records a problem that nobody ever reads; this is harmless.
class Stop{
  private final CountDownLatch latch= new CountDownLatch(1);
  private final AtomicReference<RuntimeException> failure= new AtomicReference<>();
  void quit(){ latch.countDown(); }
  Runnable worker(Runnable task){ return ()->runWorker(task); }
  private void runWorker(Runnable task){
    try{ task.run(); fail(Bug.of("A manager worker stopped, but it must run for the whole life of the manager process")); }
    catch(UserError problem){ fail(problem); }
    catch(Throwable t){ fail(Bug.of(t)); }
  }
  void fail(RuntimeException problem){ failure.compareAndSet(null,problem); latch.countDown(); }
  void await(){
    try{ latch.await(); }
    catch(InterruptedException e){ throw Bug.of(e); }
    var problem= failure.get();
    if (problem != null){ throw problem; }
  }
}
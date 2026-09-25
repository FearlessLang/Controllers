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
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import fileSupport.NativeLocaleForcer;
import fileSupport.StringFiles;
import gui.Tray;
import gui.Window;
import tools.Fs;
import tools.JavacTool;
import userMessages.UserError;
import userMessages.Violation;
import utils.Bug;

/// The manager process. Every launch leaves one message file (the folder it was
/// started on) in the manager folder, then tries to take the instance lock: the one
/// process holding it owns the window and hands every message to its Manager, its own included.
/// Resources that live for the whole process life (the lock, the watch service, the
/// threads, the window) are never closed by us: every path out of main leads to
/// System.exit, and the operating system reclaims them on process death.
public final class Main{
  private static FileLock lock;
  private final Path managerDir;
  private final CountDownLatch done= new CountDownLatch(1);
  private final AtomicReference<RuntimeException> failure= new AtomicReference<>();
  private Manager manager;
  private Main(Path managerDir){ this.managerDir= managerDir; }
  public Manager manager(){ return manager; }
  private Path msgDir(){ return managerDir.resolve("messages"); }
  public static void main(String[] args){
    NativeLocaleForcer.forceEnglish();
    var exitCode= 0;
    try{ run(message(args)); }
    catch(UserError e){ exitCode= 1; display(e); }
    catch(VirtualMachineError|LinkageError e){ exitCode= 2; display(Messages.vmOrLinkageFailure(e)); }
    catch(Throwable t){ exitCode= 3; display(UserError.crashed(t)); }
    System.exit(exitCode);
  }
  static String message(String... args){ return args.length == 0 ? "" : Manager.path(args[0]).toString(); }
  private static void display(UserError e){
    try{ e.display(); }
    catch(InterruptedException ie){ e.displayStderr(ie); }
  }
  private static void run(String message){
    var main= new Main(binDir().resolveSibling(JavacTool.dataDirNameFor(versionId())));
    try{ Files.createDirectories(main.msgDir()); }
    catch(IOException|UnsupportedOperationException|SecurityException e){ throw Messages.couldNotCreateManagerFolder(main.managerDir,e); }
    leave(main.msgDir(),message);
    var lockFile= main.managerDir.resolve("instance.lock");
    try{ lock= FileChannel.open(lockFile,CREATE,WRITE).tryLock(); }
    catch(OverlappingFileLockException e){ throw Bug.unreachable(); }
    catch(IOException e){ throw Messages.couldNotUseInstanceLock(lockFile,e); }
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
    throw Messages.programFolderNotFound(startedFrom,expected);
  }
  //Write to a .tmp name, then atomically rename it to .msg: the owner drains
  //only *.msg, so it can never observe a half written file.
  private static void leave(Path msgDir, String message){
    var name= "%020d-%s".formatted(System.currentTimeMillis(),UUID.randomUUID());
    var tmp= msgDir.resolve(name+".tmp");
    StringFiles.writeNew(tmp,message,UserError.onFileError());
    try{ Files.move(tmp,msgDir.resolve(name+".msg"),ATOMIC_MOVE); }
    catch(IOException e){ throw Messages.couldNotLeaveStartMessage(msgDir,e); }
  }
  private void own(){
    WatchService watcher;
    try{ watcher= FileSystems.getDefault().newWatchService(); msgDir().register(watcher,ENTRY_CREATE); }
    catch(IOException|UnsupportedOperationException|SecurityException e){ throw Messages.couldNotWatchMessageFolder(msgDir(),e); }
    Thread.setDefaultUncaughtExceptionHandler((_,t)->fail(t instanceof UserError e ? e : Bug.of(t)));
    var window= Window.create(this);
    manager= new Manager(managerDir,new Deployed(),window,this::fail);
    UserError.owner(window.frame);
    Violation.running(()->manager.state().running());
    Tray.install(window,this);
    window.show();
    drain();
    Thread.startVirtualThread(()->watch(watcher));
    manager.start();
    if (!Fs.isMac()){ var l= Association.launcher(); Association.reconcile(l,Association.extensions(l)); }
    try{ done.await(); }
    catch(InterruptedException e){ throw Bug.of(e); }
    var problem= failure.get();
    if (problem != null){ throw problem; }
  }
  public void quit(){ done.countDown(); }
  public void fail(RuntimeException problem){ failure.compareAndSet(null,problem); done.countDown(); }
  public void forgetAssociation(Window window){
    if (Fs.isMac() || !window.askForget()){ return; }
    try{ Association.reconcile(Association.launcher(),List.of()); }
    catch(UserError e){ display(e); System.exit(1); }
    System.exit(0);
  }
  private void watch(WatchService watcher){
    while(true){
      WatchKey key;
      try{ key= watcher.take(); }
      catch(InterruptedException e){ throw Bug.of(e); }
      key.pollEvents();
      drain();
      if (!key.reset()){ fail(Messages.messageFolderNotWatchable(msgDir())); return; }
    }
  }
  //Runs once before the watcher thread starts, then only from that thread: never concurrently.
  private void drain(){
    try{ take().forEach(manager::message); }
    catch(IOException e){ throw Messages.couldNotDrainMessageFolder(msgDir(),e); }
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
}

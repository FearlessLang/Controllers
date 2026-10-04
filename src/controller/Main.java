package controller;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.Optional;
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
/// A launch that fails once it knows its manager folder deletes that folder and every
/// file association of Fearless, then says so: the one remedy is a fresh copy of Fearless.
/// Resources that live for the whole process life (the lock, the watch service, the
/// threads, the window) are never closed by us, except the ones holding or writing the
/// manager folder right before that deletion: every path out of main leads to
/// System.exit, and the operating system reclaims them on process death.
public final class Main{
  private static FileLock lock;
  private static Optional<Main> started= Optional.empty();
  private final Path managerDir;
  private final CountDownLatch done= new CountDownLatch(1);
  private final AtomicReference<Throwable> failure= new AtomicReference<>();
  private Manager manager;
  private WatchService watcher;
  private Main(Path managerDir){ this.managerDir= managerDir; }
  public Manager manager(){ return manager; }
  private Path msgDir(){ return managerDir.resolve("messages"); }
  public static void main(String[] args){
    NativeLocaleForcer.forceEnglish();
    var exitCode= 0;
    try{ run(message(args)); }
    catch(UserError e){ exitCode= 1; display(wiped(e)); }
    catch(VirtualMachineError|LinkageError e){ exitCode= 2; display(wiped(Messages.vmOrLinkageFailure(e))); }
    catch(Throwable t){ exitCode= 3; display(wiped(UserError.crashed(t))); }
    System.exit(exitCode);
  }
  //TO TEST: on Windows the launcher hands Java a '?' in place of an unpaired surrogate of the
  //argument, so a folder whose name holds one is refused as a broken path when given as the
  //argument; the window and a register message register it.
  static String message(String... args){
    if (args.length > 1){ throw Messages.tooManyArguments(List.of(args)); }
    return args.length == 0 ? "" : TaggedText.line(path(args[0]).toString());
  }
  private static Path path(String given){
    if (given.isBlank()){ throw Violation.badLaunchArg(given,false); }
    try{ return Path.of(given).toAbsolutePath().normalize(); }
    catch(InvalidPathException e){ throw Violation.badLaunchArg(given,false); }
  }
  private static UserError wiped(UserError e){ return started.map(m->m.wipe(e)).orElse(e); }
  private UserError wipe(UserError e){
    try{
      if (manager != null){ manager.stop(); }
      if (watcher != null){ Fs.ofV(watcher::close); }
      if (lock != null){ Fs.ofV(()->lock.channel().close()); }
      Fs.rmTree(managerDir);
      if (!Fs.isMac()){ Association.eradicateAll(); }
    }
    catch(UncheckedIOException w){ return e.followedBy(Messages.notWiped(managerDir,w.getCause())); }
    catch(UserError w){ return e.followedBy(Messages.notWiped(managerDir,w)); }
    return e.followedBy(Messages.wiped(managerDir));
  }
  private static void display(UserError e){
    try{ e.display(); }
    catch(InterruptedException ie){ e.displayStderr(ie); }
  }
  private static void run(String message) throws Throwable{
    var main= new Main(binDir().resolveSibling(JavacTool.dataDirNameFor(versionId())));
    started= Optional.of(main);
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
  private void own() throws Throwable{
    try{ watcher= FileSystems.getDefault().newWatchService(); msgDir().register(watcher,ENTRY_CREATE); }
    catch(IOException|UnsupportedOperationException|SecurityException e){ throw Messages.couldNotWatchMessageFolder(msgDir(),e); }
    Thread.setDefaultUncaughtExceptionHandler((_,t)->fail(t));
    var window= Window.create(this);
    manager= new Manager(managerDir,new Deployed(),window,this::fail);
    UserError.owner(window.frame);
    Violation.running(()->manager.state().running());
    Tray.install(window,this);
    window.show();
    manager.drain();
    Thread.startVirtualThread(()->watch(watcher));
    manager.start();
    try{ done.await(); }
    catch(InterruptedException e){ throw Bug.of(e); }
    var problem= failure.get();
    if (problem != null){ throw problem; }
  }
  public void quit(){ done.countDown(); }
  public void fail(Throwable problem){ failure.compareAndSet(null,problem); done.countDown(); }
  public void forgetAssociation(Window window){
    if (!window.askForget()){ return; }
    manager.forgetAssociation(this::unassociate);
  }
  private void unassociate(){ Association.reconcile(Association.launcher(),List.of(),e->""); quit(); }
  private void watch(WatchService watcher){
    while(true){
      WatchKey key;
      try{ key= watcher.take(); }
      catch(InterruptedException e){ throw Bug.of(e); }
      key.pollEvents();
      manager.drain();
      if (!key.reset()){ fail(Messages.messageFolderNotWatchable(msgDir())); return; }
    }
  }
}

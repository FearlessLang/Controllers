package controller;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.FileSystems;
import java.nio.file.InvalidPathException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import fileSupport.NativeLocaleForcer;
import fileSupport.StringFiles;
import gui.Tray;
import gui.Window;
import managerMessages.Report;
import managerMessages.Violation;
import tools.Fs;
import tools.JavacTool;
import userMessages.UserError;
import utils.Bug;

/// The manager process. Every launch leaves one message file (its arguments, turned into a
/// message) in the manager folder, then tries to take the instance lock: the one process
/// holding it owns the window and hands every message to its Manager, its own included.
/// A fatal error is shown first; then the jobs end, the manager folder is deleted and every
/// file association of Fearless is removed: the one remedy is a fresh copy of Fearless.
/// Resources that live for the whole process life (the lock, the watch service, the
/// threads, the window) are never closed by us, except the ones holding or writing the
/// manager folder right before that deletion: every path out of main leads to
/// System.exit, and the operating system reclaims them on process death.
public final class Main implements Manager.Exit{
  private static final String prefix= "FearlessCommand";
  private static final List<String> pathVerbs= List.of("Register", "ConnectEclipse", "EditMetadata", "OpenWithOrShortcut");
  private static FileLock lock;
  private static Optional<Main> started= Optional.empty();
  private final Path managerDir;
  private final CountDownLatch done= new CountDownLatch(1);
  private final AtomicReference<Throwable> failure= new AtomicReference<>();
  private volatile boolean reset;
  private Manager manager;
  private WatchService watcher;
  private Main(Path managerDir){ this.managerDir= managerDir; }
  private Path msgDir(){ return managerDir.resolve("messages"); }
  public static void main(String[] args){
    NativeLocaleForcer.forceEnglish();
    Fs.onSymbolicLink= Report::symbolicLink;
    var exitCode= 0;
    try{ run(message(args)); }
    catch (UserError e){ exitCode = 1; fatal(e); }
    catch (VirtualMachineError|LinkageError e){ exitCode = 2; fatal(Violation.vmOrLinkageFailure(e)); }
    catch (Throwable t){ exitCode = 3; fatal(UserError.crashed(t)); }
    System.exit(exitCode);
  }
  //TO TEST: on Windows the launcher hands Java a '?' in place of an unpaired surrogate of the
  //argument, so a file whose name holds one is refused as a broken path when given as the
  //argument; a "Base16:" path of a FearlessCommand argument carries it.
  static String message(String... args){
    if (args.length == 0){ return "Show"; }
    if (!args[0].startsWith(prefix)){ return file(args); }
    var verb= args[0].substring(prefix.length());
    if (List.of("InvalidCall", "FatalError").contains(verb)){ return "InvalidCall\n"+Report.reservedVerb(args[0]); }
    if (Stream.of(args).anyMatch(a -> a.indexOf('\n') >= 0)){ return "InvalidCall\n"+Report.argumentWithNewline(List.of(args)); }
    var rest= List.of(args).subList(1, args.length);
    if (pathVerbs.contains(verb) && !rest.isEmpty()){ rest = Stream.concat(Stream.of(absolute(rest.getFirst())), rest.stream().skip(1)).toList(); }
    if (!List.of("Run", "Link").contains(verb) || rest.size() < 2){ return String.join("\n", Stream.concat(Stream.of(verb), rest.stream()).toList()); }
    var words= rest.subList(1, rest.size());
    var port= words.getLast().startsWith("Port:");
    var named= String.join(" ", port ? words.subList(0, words.size()-1) : words);
    return verb+"\n"+rest.getFirst()+"\n"+named+(port ? "\n"+words.getLast() : "");
  }
  private static String absolute(String tagged){
    try{
      var given= TaggedText.read(tagged, Refused::new);
      if (given.isBlank() || Path.of(given).isAbsolute()){ return tagged; }
      return TaggedText.line(Path.of(given).toAbsolutePath().toString());
    }
    catch (Refused|InvalidPathException e){ return tagged; }
  }
  private static String file(String[] args){
    if (args.length > 1){ return "InvalidCall\n"+Report.tooManyArguments(List.of(args)); }
    Path path;
    try{ path= Path.of(args[0]).toAbsolutePath().normalize(); }
    catch (InvalidPathException e){ return "InvalidCall\n"+Report.brokenPath(args[0]); }
    if (args[0].isBlank() || path.getFileName() == null){ return "InvalidCall\n"+Report.brokenPath(args[0]); }
    var project= Files.isDirectory(path) || path.getFileName().toString().endsWith(Names.ext);
    var verb= project ? "Register" : "OpenWithOrShortcut";
    return verb+"\n"+TaggedText.line(path.toString());
  }
  public static void command(String verb, String... args){
    var cmd= Stream.concat(Stream.of(Association.launcher().toString(), prefix+verb), Stream.of(args)).toList();
    var pb= new ProcessBuilder(cmd).redirectOutput(Redirect.DISCARD).redirectError(Redirect.DISCARD);
    //The launcher marks the process it starts with _JPACKAGE_ variables; a launcher started by
    //that process inherits them, takes itself for that process, and ends with 1.
    pb.environment().keySet().removeIf(k -> k.startsWith("_JPACKAGE_"));
    Fs.of(pb::start);
  }
  public static String path(Path p){ return TaggedText.base16(p.toString()); }
  private static void fatal(UserError e){
    if (started.isEmpty()){ display(e); return; }
    var main= started.get();
    display(e.followedBy(Violation.willWipe(main.managerDir)));
    main.wipe().ifPresent(Main::display);
  }
  private Optional<UserError> wipe(){
    try{
      if (manager != null){ manager.stop(); }
      if (watcher != null){ Fs.ofV(watcher::close); }
      if (lock != null){ Fs.ofV(() -> lock.channel().close()); }
      Fs.rmTree(managerDir);
      if (!Fs.isMac()){ Association.eradicateAll(); }
      return Optional.empty();
    }
    catch (UncheckedIOException w){ return Optional.of(Violation.notWiped(managerDir, w.getCause())); }
    catch (UserError w){ return Optional.of(Violation.notWiped(managerDir, w)); }
  }
  private static void display(UserError e){
    try{ e.display(); }
    catch (InterruptedException ie){ e.displayStderr(ie); }
  }
  private static void run(String message) throws Throwable{
    var main= new Main(programDir().resolveSibling(JavacTool.dataDirNameFor(versionId())));
    started = Optional.of(main);
    try{ Fs.createDirectories(main.msgDir()); }
    catch (IOException|UnsupportedOperationException|SecurityException e){ throw Violation.couldNotCreateManagerFolder(main.managerDir, e); }
    leave(main.msgDir(), message);
    var lockFile= main.managerDir.resolve("instance.lock");
    try{ lock = Fs.openChannel(lockFile, CREATE, WRITE).tryLock(); }
    catch (OverlappingFileLockException e){ throw Bug.unreachable(); }
    catch (IOException e){ throw Violation.couldNotUseInstanceLock(lockFile, e); }
    if (lock == null){ return; }
    UserError.becameManagerOwner();
    main.own();
  }
  private static String versionId(){ return JavacTool.reqVersionId(userMessages.Violation::mustUseLauncher); }
  static Path programDir(){
    var expected= JavacTool.managedName+versionId()+(Fs.isMac() ? ".app" : "");
    var startedFrom= JavacTool.reqAppDir(userMessages.Violation::mustUseLauncher).toAbsolutePath().normalize();
    for (var dir= startedFrom; dir != null && dir.getFileName() != null; dir = dir.getParent()){
      if (dir.getFileName().toString().equals(expected)){ return dir; }
    }
    throw Violation.programFolderNotFound(startedFrom, expected);
  }
  //Write to a .tmp name, then atomically rename it to .msg: the owner drains
  //only *.msg, so it can never observe a half written file.
  private static void leave(Path msgDir, String message){
    var name= "%020d-%s".formatted(System.currentTimeMillis(), UUID.randomUUID());
    var tmp= msgDir.resolve(name+".tmp");
    StringFiles.writeNew(tmp, message, UserError.onFileError());
    try{ Fs.move(tmp, msgDir.resolve(name+".msg"), ATOMIC_MOVE); }
    catch (IOException e){ throw Violation.couldNotLeaveStartMessage(msgDir, e); }
  }
  private void own() throws Throwable{
    try{ watcher = FileSystems.getDefault().newWatchService(); Fs.watch(msgDir(), watcher, ENTRY_CREATE); }
    catch (IOException|UnsupportedOperationException|SecurityException e){ throw Violation.couldNotWatchMessageFolder(msgDir(), e); }
    probe(watcher);
    Thread.setDefaultUncaughtExceptionHandler((_,t) -> fail(t));
    var window= Window.create(this, managerDir);
    manager = new Manager(managerDir, new Deployed(), window, this);
    UserError.owner(window.frame);
    userMessages.Violation.running(() -> manager.state().running());
    window.tray(Tray.install(window));
    window.show();
    manager.drain();
    Thread.startVirtualThread(() -> watch(watcher));
    manager.start();
    try{ done.await(); }
    catch (InterruptedException e){ throw Bug.of(e); }
    var problem= failure.get();
    if (problem != null){ throw problem; }
    if (!reset){ manager.stop(); return; }
    wipe().ifPresent(Main::display);
  }
  private void probe(WatchService watcher){
    StringFiles.writeNew(msgDir().resolve("%020d-%s.tmp".formatted(System.currentTimeMillis(), UUID.randomUUID())), "", UserError.onFileError());
    WatchKey key;
    try{ key= watcher.poll(5, TimeUnit.SECONDS); }
    catch (InterruptedException e){ throw Bug.of(e); }
    if (key == null){ throw Violation.newFilesNotReported(msgDir()); }
    key.pollEvents();
    if (!key.reset()){ throw Violation.messageFolderNotWatchable(msgDir()); }
  }
  @Override public void quit(){ done.countDown(); }
  @Override public void reset(){ reset = true; done.countDown(); }
  @Override public void fail(Throwable problem){
    if (!Files.isDirectory(managerDir)){ quit(); return; }
    failure.compareAndSet(null, problem);
    done.countDown();
  }
  private void watch(WatchService watcher){
    while (true){
      WatchKey key;
      try{ key= watcher.take(); }
      catch (InterruptedException e){ throw Bug.of(e); }
      key.pollEvents();
      manager.drain();
      if (!key.reset()){ fail(Violation.messageFolderNotWatchable(msgDir())); return; }
    }
  }
}

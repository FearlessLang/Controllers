package controller;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;

import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.FileAlreadyExistsException;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.Locale;

import fileSupport.NativeLocaleForcer;
import fileSupport.StringFiles;
import gui.Window;
import managerMessages.Report;
import managerMessages.Violation;
import tools.Fs;
import tools.PathBytes;
import tools.PathClassLoader;
import tools.JavacTool;
import userMessages.UserError;
import utils.Bug;
import utils.Join;
import utils.OneOr;
import utils.Range;

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
    try{ NativeLocaleForcer.requireUtf8FileNames(); run(startMessages(args)); }
    catch (UserError e){ exitCode = fatal(e, 1); }
    catch (VirtualMachineError|LinkageError e){ exitCode = fatal(Violation.vmOrLinkageFailure(e), 2); }
    catch (Throwable t){ exitCode = fatal(UserError.crashed(t), 3); }
    System.exit(exitCode);
  }
  private static final LinkedBlockingQueue<List<File>> opened= new LinkedBlockingQueue<>();
  private static boolean awaitsOpened;
  private static void takeOpened(){ Desktop.getDesktop().setOpenFileHandler(e -> opened.add(e.getFiles())); }
  private static List<String> startMessages(String[] args){
    var byEvent= List.of(args).equals(List.of(prefix+"Opened"));
    awaitsOpened = Fs.isMac() && !GraphicsEnvironment.isHeadless() && (args.length == 0 || byEvent);
    if (!awaitsOpened){ return messages(fileAsArg(args)); }
    takeOpened();
    return byEvent ? List.of() : messages();
  }
  private void leaveOpened(long wait){
    try{ Stream.ofNullable(opened.poll(wait, TimeUnit.MILLISECONDS)).flatMap(List::stream).flatMap(Main::messagesOf).forEach(m -> leave(msgDir(), m)); }
    catch (InterruptedException e){ throw Bug.of(e); }
  }
  private static Stream<String> messagesOf(File f){ return messages(PathClassLoader.argOf(f.toPath().toAbsolutePath())).stream(); }
  //The JVM hands main the arguments decoded as text, and bytes not valid in the file name encoding are
  //lost: a file argument is read again as the bytes the process received, and handed on as PathClassLoader.argOf.
  static String[] fileAsArg(String[] args){
    if (args.length != 1 || args[0].startsWith(prefix) || args[0].isBlank()){ return args; }
    try{ return new String[]{PathClassLoader.argOf(PathBytes.arg(args, 0).toAbsolutePath())}; }
    catch (InvalidPathException e){ return args; }
  }
  /// A folder or a .fearless file opened with the manager program is Register, then Show.
  static List<String> messages(String... args){
    if (args.length == 0){ return List.of("Show"); }
    if (args.length > 1){ return List.of("InvalidCall\n"+Report.tooManyArguments(List.of(args))); }
    if (!args[0].startsWith(prefix)){ return file(args[0]); }
    try{ return List.of(call(args[0])); }
    catch (Refused e){ return List.of("InvalidCall\n"+Report.badCall(args[0], e.getMessage())+port(args[0])); }
  }
  private static String port(String call){
    var last= call.substring(call.lastIndexOf(' ')+1);
    return Follow.isPort(last) ? "\n"+last : "";
  }
  private static final List<List<String>> commands= Stream.of(
    "Show", "Show projectName", "Quit", "Reset",
    "Register base16", "ConnectEclipse base16", "EditMetadata base16", "OpenWithOrShortcut base16", "OpenWithOrShortcut base16 projectName typeName",
    "Run projectName", "Compile projectName", "Check projectName", "Terminate projectName", "DeleteCache projectName",
    "Kind projectName kind", "Forget projectName", "Fix projectName", "Mount projectName typeName projectName", "Unmount projectName typeName",
    "Attenuate projectName typeName typeName1..typeNamen", "SelectMains projectName typeName1..typeNamen").map(c -> List.of(c.split(" "))).toList();
  private static final String typeNames= "typeName1..typeNamen";
  private static final Pattern projectName= Pattern.compile("(?!(?:con|prn|aux|nul|com[1-9]|lpt[1-9])$)[a-z_][a-z0-9_]{0,190}");
  private static final Pattern typeName= Pattern.compile("(?!(?:con|prn|aux|nul|com[1-9]|lpt[1-9])[.])[a-z][a-z0-9_]*[.]_*[A-Z][A-Za-z0-9_]*'*");
  /// The message of a call: the verb, then the arguments of the verb and the port, one per line.
  private static String call(String call){
    var words= List.of(call.split(" ", -1));
    var verb= words.getFirst().substring(prefix.length());
    var forms= commands.stream().filter(c -> c.getFirst().equals(verb)).toList();
    if (forms.isEmpty()){ throw new Refused(Report.callUnknown(words.getFirst(), commands.stream().map(c -> prefix+c.getFirst()).distinct().sorted().toList())); }
    var port= words.size() > 1 && words.getLast().startsWith("Port:");
    if (port){ check(words.getLast(), Follow.isPort(words.getLast()), Report.callPort); }
    var args= words.subList(1, words.size()-(port ? 1 : 0));
    var form= OneOr.opt("form of "+verb, forms.stream().filter(f -> f.getLast().equals(typeNames) ? args.size() >= f.size()-2 : args.size() == f.size()-1))
      .orElseThrow(() -> new Refused(Report.callForms(forms.stream().map(f -> Join.of(f, prefix, " ", "")).toList())));
    for (int i : Range.of(0, args.size())){ word(form.get(Math.min(i+1, form.size()-1)), args.get(i)); }
    var mains= verb.equals("SelectMains") ? args.subList(1, args.size()) : List.<String>of();
    mains.stream().filter(m -> mains.indexOf(m) != mains.lastIndexOf(m)).findFirst().ifPresent(m -> { throw new Refused(Report.callRepeated(m)); });
    return String.join("\n", Stream.concat(Stream.of(verb), words.stream().skip(1)).toList());
  }
  private static void word(String kind, String w){
    switch (kind){
      case "projectName" -> check(w, projectName.matcher(w).matches(), Report.callProjectName);
      case "typeName", typeNames -> check(w, typeName.matcher(w).matches(), Report.callTypeName);
      case "kind" -> check(w, Registry.Kind.of(w).isPresent(), Report.callKind);
      case "base16" -> check(w, base16(w), Report.callBase16);
      default -> throw Bug.unreachable();
    }
  }
  private static void check(String w, boolean ok, String what){ if (!ok){ throw new Refused(Report.callWord(w, what)); } }
  private static boolean base16(String w){
    if (!w.matches("Base16:(?:[0-9A-F][0-9A-F])+")){ return false; }
    try{ var p= TaggedText.readPath(w, Refused::new); return p.isAbsolute() && p.normalize().getFileName() != null; }
    catch (Refused|InvalidPathException e){ return false; }
  }
  private static List<String> file(String arg){
    Path path;
    try{ path= (arg.startsWith(Fs.isWindows() ? "Base16:" : "file:") ? PathClassLoader.pathOf(arg) : Path.of(arg)).toAbsolutePath().normalize(); }
    catch (IllegalArgumentException e){ return List.of("InvalidCall\n"+Report.brokenPath(arg)); }
    if (arg.isBlank()){ return List.of("InvalidCall\n"+Report.brokenPath(arg)); }
    if (path.getFileName() == null || isProject(path)){ return List.of("Register\n"+TaggedText.ofPath(path), "Show"); }
    return List.of("OpenWithOrShortcut\n"+TaggedText.ofPath(path));
  }
  public static boolean isProject(Path path){ return Files.isDirectory(path) || path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(Names.ext); }
  private static final ExecutorService sender= Executors.newSingleThreadExecutor(Thread.ofVirtual().factory());
  /// Runs the manager program with each call in turn, each once the process before has ended, also across the calls of
  /// several GUI operations, so the messages arrive in that order.
  public static void command(String... calls){ sender.execute(() -> Stream.of(calls).forEach(Main::send)); }
  private static void send(String call){
    var pb= Fs.processBuilder(List.of(Association.launcher().toString(), prefix+call)).redirectOutput(Redirect.DISCARD).redirectError(Redirect.DISCARD);
    try{ Fs.of(pb::start).waitFor(); }
    catch (InterruptedException e){ throw Bug.of(e); }
  }
  /// A run that is not the 'single' manager leaves the fatal error as a FatalError message, and ends with 0.
  private static int fatal(UserError e, int code){
    if (started.isEmpty()){ display(e); return code; }
    var main= started.get();
    if (lock == null && main.othersHoldLock()){
      try{ leave(main.msgDir(), "FatalError\n"+e.getMessage()); return 0; }
      catch (UserError w){ display(e); return code; }
    }
    display(e.followedBy(Violation.willWipe(main.managerDir)));
    main.wipe().ifPresent(Main::display);
    return code;
  }
  private boolean othersHoldLock(){
    try{ lock = Fs.openChannel(managerDir.resolve("instance.lock"), CREATE, WRITE).tryLock(); return lock == null; }
    catch (IOException e){ return false; }
  }
  private Optional<UserError> wipe(){
    try{
      if (manager != null){ manager.stop(); }
      if (watcher != null){ Fs.ofV(watcher::close); }
      if (lock != null){ Fs.ofV(() -> lock.channel().close()); }
      Fs.rmTree(managerDir);
      Association.eradicateAll();
      return Optional.empty();
    }
    catch (UncheckedIOException w){ return Optional.of(Violation.notWiped(managerDir, w.getCause())); }
    catch (UserError w){ return Optional.of(Violation.notWiped(managerDir, w)); }
  }
  private static void display(UserError e){
    try{ e.display(); }
    catch (InterruptedException ie){ e.displayStderr(ie); }
  }
  private static void run(List<String> messages) throws Throwable{
    var main= new Main(programDir().resolveSibling(JavacTool.dataDirNameFor(versionId())));
    started = Optional.of(main);
    try{ Fs.createDirectories(main.msgDir()); }
    catch (IOException|UnsupportedOperationException|SecurityException e){ throw Violation.couldNotCreateManagerFolder(main.managerDir, e); }
    messages.forEach(m -> leave(main.msgDir(), m));
    var lockFile= main.managerDir.resolve("instance.lock");
    try{ lock = Fs.openChannel(lockFile, CREATE, WRITE).tryLock(); }
    catch (OverlappingFileLockException e){ throw Bug.unreachable(); }
    catch (IOException e){ throw Violation.couldNotUseInstanceLock(lockFile, e); }
    if (lock == null){ if (awaitsOpened){ main.leaveOpened(2000); } return; }
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
  //A name already taken gets another id: the rename never replaces a file.
  private static void leave(Path msgDir, String message){
    var name= String.format(Locale.ROOT, "%020d-%s", System.currentTimeMillis(), UUID.randomUUID());
    var tmp= msgDir.resolve(name+".tmp");
    try{
      Fs.writeString(tmp, message, CREATE_NEW);
      try{ Fs.move(tmp, msgDir.resolve(name+".msg")); }
      catch (FileAlreadyExistsException e){ Fs.deleteIfExists(tmp); throw e; }
    }
    catch (FileAlreadyExistsException e){ leave(msgDir, message); }
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
    window.tray();
    window.hide();
    manager.drain();
    Thread.startVirtualThread(() -> watch(watcher));
    if (Fs.isMac()){ takeOpened(); Thread.startVirtualThread(() -> { while (true){ leaveOpened(Long.MAX_VALUE); } }); }
    manager.start();
    try{ done.await(); }
    catch (InterruptedException e){ throw Bug.of(e); }
    var problem= failure.get();
    if (problem != null){ throw problem; }
    if (!reset){ manager.stop(); return; }
    wipe().ifPresent(Main::display);
  }
  private void probe(WatchService watcher){
    StringFiles.writeNew(msgDir().resolve(String.format(Locale.ROOT, "%020d-%s.tmp", System.currentTimeMillis(), UUID.randomUUID())), "", UserError.onFileError());
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

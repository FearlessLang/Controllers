package controller;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;

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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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
    try{ NativeLocaleForcer.requireUtf8FileNames(); run(messages(fileAsArg(args))); }
    catch (UserError e){ exitCode = fatal(e, 1); }
    catch (VirtualMachineError|LinkageError e){ exitCode = fatal(Violation.vmOrLinkageFailure(e), 2); }
    catch (Throwable t){ exitCode = fatal(UserError.crashed(t), 3); }
    System.exit(exitCode);
  }
  //TO TEST: on Windows the launcher hands Java a '?' in place of an unpaired surrogate of the
  //argument, so a file whose name holds one is refused as a broken path when given as the
  //argument; a "Base16:" path of a FearlessCommand argument carries it.
  //The JVM hands main the arguments decoded as text, and bytes not valid in the file name encoding are
  //lost: a file argument is read again as the bytes the process received, and handed on as PathClassLoader.argOf.
  static String[] fileAsArg(String[] args){
    if (args.length != 1 || args[0].startsWith(prefix) || args[0].isBlank()){ return args; }
    try{ return new String[]{PathClassLoader.argOf(PathBytes.arg(args, 0).toAbsolutePath())}; }
    catch (InvalidPathException e){ return args; }
  }
  /// A folder or a .fearless file opened with the manager program is Register, then Show.
  static List<String> messages(String... args){
    var m= message(args);
    return args.length == 1 && !args[0].startsWith(prefix) && m.startsWith("Register\n") ? List.of(m, "Show") : List.of(m);
  }
  static String message(String... args){
    if (args.length == 0){ return "Show"; }
    if (args.length > 1){ return "InvalidCall\n"+Report.tooManyArguments(List.of(args)); }
    if (!args[0].startsWith(prefix)){ return file(args[0]); }
    try{ return call(args[0]); }
    catch (Refused e){ return "InvalidCall\n"+Report.badCall(args[0], e.getMessage()); }
  }
  /// The arguments of each command of the grammar of a call: p a projectName, t a typeName, b a base16, k a kind; a last * is typeName1..typeNamen.
  private static final Map<String,List<String>> commands= Map.ofEntries(
    Map.entry("Show", List.of("", "p")), Map.entry("Quit", List.of("")), Map.entry("Reset", List.of("")),
    Map.entry("Register", List.of("b")), Map.entry("ConnectEclipse", List.of("b")), Map.entry("EditMetadata", List.of("b")), Map.entry("OpenWithOrShortcut", List.of("b", "bpt")),
    Map.entry("Run", List.of("p")), Map.entry("Compile", List.of("p")), Map.entry("Check", List.of("p")), Map.entry("Terminate", List.of("p")), Map.entry("DeleteCache", List.of("p")),
    Map.entry("Kind", List.of("pk")), Map.entry("Forget", List.of("p")), Map.entry("Mount", List.of("ptp")), Map.entry("Unmount", List.of("pt")),
    Map.entry("Attenuate", List.of("pt*")), Map.entry("SelectMains", List.of("p*")));
  private static final Pattern projectName= Pattern.compile("(?!(?:con|prn|aux|nul|com[1-9]|lpt[1-9])$)[a-z_][a-z0-9_]{0,190}");
  private static final Pattern typeName= Pattern.compile("(?!(?:con|prn|aux|nul|com[1-9]|lpt[1-9])[.])[a-z][a-z0-9_]*[.]_*[A-Z][A-Za-z0-9_]*'*");
  /// The message of a call: the verb, then the arguments of the verb and the port, one per line.
  private static String call(String call){
    var words= List.of(call.split(" ", -1));
    var verb= words.getFirst().substring(prefix.length());
    var forms= commands.get(verb);
    if (forms == null){ throw new Refused(Report.callUnknown(words.getFirst(), commands.keySet().stream().sorted().map(v -> prefix+v).toList())); }
    var port= words.size() > 1 && words.getLast().startsWith("Port:");
    if (port && !Follow.isPort(words.getLast())){ throw new Refused(Report.callWord(words.getLast(), Report.callPort)); }
    var args= words.subList(1, words.size()-(port ? 1 : 0));
    var form= OneOr.opt("form of "+verb, forms.stream().filter(f -> f.endsWith("*") ? args.size() >= f.length()-1 : args.size() == f.length()))
      .orElseThrow(() -> new Refused(Report.callForms(forms.stream().map(f -> form(words.getFirst(), f)).toList())));
    for (int i : Range.of(0, args.size())){ word(i < form.length() && form.charAt(i) != '*' ? form.charAt(i) : 't', args.get(i)); }
    var mains= verb.equals("SelectMains") ? args.subList(1, args.size()) : List.<String>of();
    mains.stream().filter(m -> mains.indexOf(m) != mains.lastIndexOf(m)).findFirst().ifPresent(m -> { throw new Refused(Report.callRepeated(m)); });
    return String.join("\n", Stream.concat(Stream.of(verb), words.stream().skip(1)).toList());
  }
  private static String form(String command, String f){
    return f.chars().mapToObj(c -> " "+switch (c){ case 'p' -> "projectName"; case 't' -> "typeName"; case 'b' -> "base16"; case 'k' -> "kind"; default -> "typeName1..typeNamen"; }).collect(Collectors.joining("", command, ""));
  }
  private static void word(char kind, String w){
    var ok= switch (kind){
      case 'p' -> projectName.matcher(w).matches();
      case 't' -> typeName.matcher(w).matches();
      case 'k' -> Registry.Kind.of(w).isPresent();
      default -> base16(w);
    };
    if (!ok){ throw new Refused(Report.callWord(w, switch (kind){ case 'p' -> Report.callProjectName; case 't' -> Report.callTypeName; case 'k' -> Report.callKind; default -> Report.callBase16; })); }
  }
  private static boolean base16(String w){
    if (!w.matches("Base16:(?:[0-9A-F][0-9A-F])+")){ return false; }
    try{ var p= TaggedText.readPath(w, Refused::new); return p.isAbsolute() && p.getFileName() != null; }
    catch (Refused|InvalidPathException e){ return false; }
  }
  private static String file(String arg){
    Path path;
    try{ path= (arg.startsWith("file:") ? PathClassLoader.pathOf(arg) : Path.of(arg)).toAbsolutePath().normalize(); }
    catch (IllegalArgumentException e){ return "InvalidCall\n"+Report.brokenPath(arg); }
    if (arg.isBlank() || path.getFileName() == null){ return "InvalidCall\n"+Report.brokenPath(arg); }
    var project= Files.isDirectory(path) || path.getFileName().toString().endsWith(Names.ext);
    var verb= project ? "Register" : "OpenWithOrShortcut";
    return verb+"\n"+TaggedText.ofPath(path);
  }
  /// Runs the manager program with each call in turn, each once the run before has ended, so the messages arrive in that order.
  public static void command(String... calls){ Thread.startVirtualThread(() -> Stream.of(calls).forEach(Main::send)); }
  private static void send(String call){
    var pb= new ProcessBuilder(Association.launcher().toString(), prefix+call).redirectOutput(Redirect.DISCARD).redirectError(Redirect.DISCARD);
    //The launcher marks the process it starts with _JPACKAGE_ variables; a launcher started by
    //that process inherits them, takes itself for that process, and ends with 1.
    pb.environment().keySet().removeIf(k -> k.startsWith("_JPACKAGE_"));
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

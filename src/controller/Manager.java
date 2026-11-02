package controller;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import controller.Registry.Content;
import controller.Registry.Entry;
import controller.Registry.Kind;
import coordinator.Coordinator;
import coordinator.MainsInfo;
import fileAssociations.Icon;
import mainCoordinator.MakeDemo;
import managerMessages.Report;
import managerMessages.Violation;
import tools.ChildJvm;
import tools.Fs;
import userMessages.UserError;
import utils.Bug;
import utils.DistinctBy;
import utils.OneOr;

/// The manager without its window: the registered projects, their jobs, and every message
/// made of them. Everything runs on one thread, one step at a time; after each step the
/// captured extensions and the View are brought up to date with the new State.
public final class Manager{
  public interface View{
    void show();
    void state(State s);
    void output(Path folder, String text);
    void clear(Path folder);
    void refuse(String text);
    void tell(String text);
    boolean visible();
    void choose(Path file, List<Project.Claimant> choices);
    void allow(String question, List<String> extensions);
    void edited(Path file);
  }
  public interface Tools{
    ChildJvm compile(Path folder, String alias, Consumer<String> out);
    ChildJvm run(Path folder, String main, Optional<Path> input, Registry.Grants grants, List<String> attenuators, Consumer<String> out);
    Coordinator.State check(Path folder);
    Path stdLibBase();
    Path baseCache();
    void associate(List<Icon> captured, List<Project.Claimant> preloads, Function<String,String> claimedBy);
    void connect(Path eclipse, Path managerDir);
  }
  public interface Exit{
    void quit();
    void reset();
    void fail(Throwable problem);
  }
  public record State(List<Project> projects, Optional<Path> selected, List<String> extensions){
    public Optional<Project> of(Path folder){ return OneOr.opt("project "+folder, projects.stream().filter(p -> p.folder().equals(folder))); }
    public Optional<Project> shown(){ return selected.flatMap(this::of); }
    public List<String> running(){ return projects.stream().filter(Project::busy).map(p -> p.alias()+" - "+p.job()).toList(); }
  }
  private static final class Live{
    Facts scanned;
    Facts facts;
    Optional<MainsInfo> mains= Optional.empty();
    MainsInfo claims= Project.noClaims;
    String job= "";
    Instant since= Instant.EPOCH;
    StringBuilder compiled= new StringBuilder();
    ChildJvm child;
    boolean terminated;
    boolean updated;
    boolean stopped;
    Optional<String> corruption= Optional.empty();
    List<String> todo= List.of();
    Optional<Path> input= Optional.empty();
    Registry.Grants grants;
    Map<String,List<String>> attenuators= Map.of();
    Optional<Follow> follow= Optional.empty();
    Set<String> answered= new HashSet<>();
    StringBuilder line= new StringBuilder();
  }
  private static final String conflict= "base.IconsConflict";
  private static final String fearless= "base.IconsFearless";
  private final ScheduledExecutorService core= Executors.newSingleThreadScheduledExecutor();
  public final Path dir;
  private final Tools tools;
  private final View view;
  private final Exit exit;
  private final Registry registry;
  private final Map<Path,Live> live= new HashMap<>();
  private Optional<Path> selected= Optional.empty();
  private Set<String> captured= Set.of();
  private int turn;
  private volatile State state= new State(List.of(), Optional.empty(), List.of());
  public Manager(Path dir, Tools tools, View view, Exit exit){
    this.dir= dir;
    this.tools= tools;
    this.view= view;
    this.exit= exit;
    registry= new Registry(dir, this::granted);
    post(this::load);
  }
  public State state(){ return state; }
  public void start(){
    core.scheduleWithFixedDelay(() -> step(this::rotate), 3, 3, TimeUnit.SECONDS);
  }
  public void message(String text){ post(() -> apply(text)); }
  void stop(){
    try{ core.submit(() -> live.values().stream().filter(l -> l.child != null).forEach(l -> l.child.kill())).get(1, TimeUnit.MINUTES); }
    catch (InterruptedException|ExecutionException|TimeoutException e){ throw Bug.of(e); }
    core.shutdownNow();
  }
  //Runs once before the watcher thread starts, then only from that thread: never concurrently.
  public void drain(){
    var msgDir= dir.resolve("messages");
    try{ Fs.ofV(() -> take(msgDir)); }
    catch (UncheckedIOException e){ throw Violation.couldNotDrainMessageFolder(msgDir, e.getCause()); }
  }
  private void take(Path msgDir) throws IOException{
    for (var file : Names.list(msgDir).stream().filter(f -> f.toString().endsWith(".msg")).toList()){
      String text;
      try{ text= UTF_8.newDecoder().decode(ByteBuffer.wrap(Fs.readAllBytes(file))).toString(); }
      catch (CharacterCodingException e){ throw Violation.messageNotUtf8(file); }
      Fs.deleteIfExists(file);
      post(() -> apply(text));
    }
    var old= Instant.now().minusSeconds(60);
    for (var file : Names.list(msgDir).stream().filter(f -> f.toString().endsWith(".tmp")).toList()){
      try{ if (Fs.getLastModifiedTime(file).toInstant().isBefore(old)){ Fs.deleteIfExists(file); } }
      catch (NoSuchFileException e){}
    }
  }
  void settle(){
    try{ core.submit(() -> {}).get(1, TimeUnit.MINUTES); }
    catch (InterruptedException|ExecutionException|TimeoutException e){ throw Bug.of(e); }
  }
  private void post(Runnable r){ core.execute(() -> step(r)); }
  private void step(Runnable r){
    try{ r.run(); publish(); }
    catch (Throwable t){ exit.fail(t); }
  }
  private void load(){
    registry.all().forEach(e -> live.put(e.path(), new Live()));
    registry.all().stream().filter(e -> e.kind() == Kind.code).forEach(e -> uncached(e.path(), () -> Coordinator.dropUnfinished(e.path())));
    Map.of(conflict, "conflict.png", fearless, "fearless.png").forEach((icon, png) -> ClaimIcons.materialise(dir.resolve("icons"), icon, Fs.of(() -> Fs.readAllBytes(tools.stdLibBase().resolve("icons").resolve(png)))));
    rebuild(Optional.empty());
    registry.all().forEach(e -> scan(e.path()));
  }
  private void open(Entry e){
    live.put(e.path(), new Live());
    scan(e.path());
  }
  private boolean allowed(Project.Claimant c){ return !Registry.isSystem(c.claim().extension()) || registry.extensions().contains(c.claim().extension()); }
  /// Rebuilds the in memory map, then reconciles; gives the error of compiled when a new extension of compiled is refused.
  private Optional<String> rebuild(Optional<Path> compiled){
    refresh();
    return reconcile(compiled, registry.extensions());
  }
  /// the claims of the in memory map, read again from the caches
  private void refresh(){
    for (var e : registry.all()){
      var l= live.get(e.path());
      if (e.kind() != Kind.code){ l.claims = Project.noClaims; continue; }
      if (l.job.equals(Project.compiling)){ continue; }
      l.claims = cached(e.path());
    }
  }
  private MainsInfo cached(Path f){
    try{ return new MainsInfo(Coordinator.cachedMains(f)); }
    catch (UserError e){ corrupted(f, e.getMessage()); return Project.noClaims; }
  }
  /// The corruption rule: ends the job of the project, deletes its cache and writes the error.
  private void corrupted(Path f, String why){
    if (!UserError.cacheCorrupted(why)){ throw new UserError(why); }
    endJob(f);
    deleted(f);
    errorFile(f, why);
    live.get(f).corruption = Optional.of(why);
  }
  private void endJob(Path f){
    var l= live.get(f);
    if (l.child == null){ return; }
    l.terminated = true;
    l.stopped = true;
    l.child.kill();
  }
  private Optional<String> reconcile(Optional<Path> compiled, List<String> extensions){
    var claimed= Project.claimed(projects());
    var next= captured(claimed, extensions);
    try{ tools.associate(next, preloads().toList(), e -> Report.claimedBy(claimed.getOrDefault(e.substring(1), List.of()))); }
    catch (Refusal r){
      var claims= compiled.stream().flatMap(f -> Project.claimants(f, alias(f), live.get(f).claims)).map(c -> "."+c.claim().extension());
      var added= extensions.stream().filter(x -> !registry.extensions().contains(x)).map(x -> "."+x);
      var brought= Stream.concat(claims, added).filter(e -> !captured.contains(e)).collect(Collectors.toSet());
      if (r.extensions.isEmpty() || r.extensions.contains(".fearless") || !brought.containsAll(r.extensions)){ throw r.error; }
      if (compiled.isEmpty()){ throw new Refused(r.getMessage()); }
      var hit= r.extensions.stream().flatMap(e -> claimed.getOrDefault(e.substring(1), List.of()).stream()).map(Project.Claimant::folder).distinct().toList();
      for (var g : hit){
        endJob(g);
        deleted(g);
        errorFile(g, r.error.getMessage());
      }
      var again= rebuild(Optional.empty());
      assert again.isEmpty();
      return compiled.filter(hit::contains).map(_ -> r.error.getMessage());
    }
    captured = next.stream().map(Icon::extension).collect(Collectors.toSet());
    return Optional.empty();
  }
  private Stream<Project.Claimant> preloads(){ return projects().stream().filter(p -> !p.job().equals(Project.compiling)).flatMap(p -> Project.preloads(p.folder(), p.alias(), p.claims())); }
  private List<Icon> captured(Map<String,List<Project.Claimant>> claimed, List<String> extensions){
    var suffix= Fs.isWindows() ? ".ico" : ".png";
    var all= new TreeSet<>(claimed.keySet().stream().filter(e -> !Registry.isSystem(e)).toList());
    all.addAll(extensions);
    var icons= dir.resolve("icons");
    var own= new Icon(".fearless", icons.resolve(fearless+suffix), icons.resolve(fearless+".png"));
    return Stream.concat(Stream.of(own), all.stream().map(e -> new Icon("."+e, icon(claimed.getOrDefault(e, List.of()), suffix), icon(claimed.getOrDefault(e, List.of()), ".png")))).toList();
  }
  private Path icon(List<Project.Claimant> cs, String suffix){ return cs.size() == 1 ? cs.getFirst().icon(suffix) : dir.resolve("icons").resolve(conflict+suffix); }
  private void apply(String message){
    var lines= List.of(message.split("\n", -1));
    var verb= lines.getFirst();
    var args= lines.subList(1, lines.size());
    if (verb.equals("InvalidCall")){ invalidCall(args); return; }
    if (verb.equals("FatalError")){ throw Violation.fatalFromRun(String.join("\n", args)); }
    var port= !args.isEmpty() && args.getLast().startsWith("Port:");
    var follow= Optional.<Follow>empty();
    try{
      follow = port ? Optional.of(Follow.to(args.getLast())) : follow;
      request(verb, port ? args.subList(0, args.size()-1) : args, follow);
      follow.ifPresent(Follow::applied);
      if (!List.of("Run", "Compile").contains(verb)){ follow.ifPresent(Follow::end); }
    }
    catch (Refused r){ follow.ifPresent(fl -> fl.refuse(r.getMessage())); view.refuse(r.getMessage()); }
  }
  /// the details of an InvalidCall, and its port when the call ended with one: the caller waits on that port
  private void invalidCall(List<String> lines){
    var port= lines.size() > 1 && Follow.isPort(lines.getLast());
    var details= String.join("\n", port ? lines.subList(0, lines.size()-1) : lines);
    view.refuse(details);
    if (!port){ return; }
    try{ Follow.to(lines.getLast()).refuse(details); }
    catch (Refused r){ view.refuse(r.getMessage()); }
  }
  private void request(String verb, List<String> args, Optional<Follow> follow){
    if (verb.equals("Show") && args.isEmpty()){ view.show(); return; }
    switch (verb){
      case "Quit" -> exit.quit();
      case "Reset" -> exit.reset();
      case "Register" -> register(path(args.getFirst()));
      case "ConnectEclipse" -> connect(path(args.getFirst()));
      case "EditMetadata" -> editMetadata(path(args.getFirst()));
      case "OpenWithOrShortcut" -> openWith(path(args.getFirst()), args.subList(1, args.size()));
      default -> project(verb, args, follow);
    }
  }
  /// A path of a message, as the manager program wrote it.
  private static Path path(String text){ return TaggedText.readPath(text, Bug::of).normalize(); }
  private void project(String verb, List<String> args, Optional<Follow> follow){
    var name= args.getFirst();
    Supplier<Refused> unknown= () -> new Refused(Report.unknownProject(verb, name, registry.all().stream().map(Entry::alias).toList()));
    var f= registry.named(name).orElseThrow(unknown).path();
    if (!scan(f)){ unreachable(f); throw unknown.get(); }
    switch (verb){
      case "Show" -> { selected = Optional.of(f); view.show(); }
      case "Run" -> run(f, registry.of(f).orElseThrow().mains(), follow, Optional.empty());
      case "Compile" -> compile(f, follow);
      case "Check" -> check(f);
      case "Terminate" -> terminate(f);
      case "DeleteCache" -> deleteCache(f);
      case "Kind" -> kind(f, args.get(1));
      case "Forget" -> forget(f);
      case "Mount" -> mount(f, args.get(1), Optional.of(registry.named(args.get(2)).orElseThrow(() -> new Refused(Report.unknownData(name, args.get(2), registry.all().stream().map(Entry::alias).toList()))).alias()));
      case "Unmount" -> mount(f, args.get(1), Optional.empty());
      case "Attenuate" -> attenuate(f, args.get(1), args.subList(2, args.size()));
      case "SelectMains" -> edit(f, e -> e.withMains(args.subList(1, args.size())));
      default -> throw Bug.unreachable();
    }
  }
  private void idle(Path f, String verb){
    var job= live.get(f).job;
    if (!job.isEmpty()){ throw new Refused(Report.busy(verb, alias(f), job)); }
  }
  private void runnable(Path f, String verb){
    idle(f, verb);
    var p= project(f);
    if (p.kind() != Kind.code){ throw new Refused(Report.notCode(verb, p.alias(), p.kind().text)); }
    p.problem().ifPresent(why -> { throw new Refused(Report.invalidContent(verb, p.alias(), why)); });
  }
  private void run(Path f, List<String> mains, Optional<Follow> follow, Optional<Path> input){
    runnable(f, "Run");
    var p= project(f);
    if (p.needsCompiling()){ throw new Refused(Report.notCompiled(p.alias(), p.state().text)); }
    var all= p.knownMains();
    if (mains.isEmpty()){ throw new Refused(Report.noMainSelected(p.alias(), all)); }
    mains.stream().filter(m -> !all.contains(m)).findFirst().ifPresent(m -> { throw new Refused(Report.notAMain(p.alias(), m, all)); });
    var attenuated= p.entry().attenuators();
    attenuated.keySet().stream().filter(m -> !all.contains(m)).findFirst().ifPresent(m -> { throw new Refused(Report.attenuatedNotAMain(p.alias(), m, all)); });
    var attenuators= p.knownAttenuators();
    attenuated.forEach((m, as) -> as.stream().filter(a -> !attenuators.contains(a)).findFirst().ifPresent(a -> { throw new Refused(Report.notAnAttenuator(p.alias(), m, a, attenuators)); }));
    var mounted= p.entry().mounts().values().stream().flatMap(d -> registry.named(d).stream()).map(Entry::path).toList();
    mounted.stream().filter(d -> !scan(d)).toList().forEach(this::unreachable);
    var mountProblems= p.mountProblems(projects());
    if (!mountProblems.isEmpty()){ throw new Refused(Report.mountsRefused(p.alias(), mountProblems)); }
    selected = Optional.of(f);
    var l= live.get(f);
    l.terminated = false;
    l.stopped = false;
    l.todo = mains;
    l.input = input;
    l.grants = Registry.grants(registry.content(), registry.of(f).orElseThrow(), l.mains.orElseThrow().dataEdit());
    l.attenuators = attenuated;
    l.follow = follow;
    view.clear(f);
    next(f);
  }
  private void compile(Path f, Optional<Follow> follow){
    runnable(f, "Compile");
    selected = Optional.of(f);
    if (!project(f).needsCompiling()){ upToDate(f, follow); return; }
    var l= live.get(f);
    l.terminated = false;
    l.stopped = false;
    l.follow = follow;
    l.compiled.setLength(0);
    view.clear(f);
    output(f, "--- compiling "+alias(f)+" ---\n");
    start(f, Project.compiling, () -> tools.compile(f, alias(f), out(f)));
  }
  private void upToDate(Path f, Optional<Follow> follow){
    view.clear(f);
    var text= Report.alreadyCompiled(alias(f))+"\n";
    view.output(f, text);
    follow.ifPresent(fl -> { fl.send(text); fl.end(); });
  }
  private void check(Path f){
    idle(f, "Check");
    selected = Optional.of(f);
    var p= project(f);
    view.clear(f);
    output(f, p.problem().or(() -> p.facts().stale()).orElse(Report.checkFine(p.alias())).stripTrailing()+"\n");
  }
  private void next(Path f){
    var l= live.get(f);
    if (l.terminated || l.todo.isEmpty()){ ended(f); return; }
    var main= l.todo.getFirst();
    l.todo = l.todo.subList(1, l.todo.size());
    output(f, "--- running "+main+" ---\n");
    start(f, main, () -> tools.run(f, main, l.input, l.grants, l.attenuators.getOrDefault(main, List.of()), out(f)));
  }
  private void ended(Path f){
    var l= live.get(f);
    l.job = "";
    l.follow.ifPresent(Follow::end);
    l.follow = Optional.empty();
    scan(f);
  }
  private void start(Path f, String what, Supplier<ChildJvm> jvm){
    var l= live.get(f);
    try{ l.child = jvm.get(); }
    catch (UncheckedIOException e){
      output(f, "--- "+(what.equals(Project.compiling) ? "compile" : what)+" did not start: "+Report.fileFailure(e.getCause())+"\n---\n");
      ended(f);
      return;
    }
    l.job = what;
    l.since = Instant.now();
    var child= l.child;
    Thread.startVirtualThread(() -> await(f, l, child));
  }
  private void await(Path f, Live l, ChildJvm child){
    var ec= code(child);
    post(() -> exited(f, l, child, ec));
  }
  private static int code(ChildJvm child){
    try{ return child.await(); }
    catch (InterruptedException e){ throw Bug.of(e); }
  }
  private void exited(Path f, Live l, ChildJvm child, int ec){
    if (live.get(f) != l || l.child != child){ return; }
    l.child = null;
    if (l.job.equals(Project.compiling)){ compiled(f, l, ec); return; }
    output(f, "--- "+l.job+" exited with "+ec+" after "+Duration.between(l.since, Instant.now()).toSeconds()+"s ---\n");
    if (!l.stopped && (l.updated || broken(f))){ l.terminated = true; output(f, Report.filesUpdated(alias(f), l.job)+"\n"); }
    l.updated = false;
    next(f);
  }
  /// What the manager does after Coordinator.compile: step 1 only after a compilation with no error, step 2 always, steps 3 and 4 only with no error.
  private void compiled(Path f, Live l, int ec){
    flushLine(f, l);
    l.job = "";
    l.answered.clear();
    var failed= ec != 0;
    if (ec == ChildMain.cacheCorrupted){
      var why= Fs.readUtf8(f.resolve(Facts.outDir).resolve("_error.txt"));
      deleted(f);
      errorFile(f, why);
    }
    var refused= List.<Project.Claimant>of();
    if (!failed){
      try{
        var claims= new MainsInfo(Coordinator.cachedMains(f));
        Project.shortcuts(Project.claimants(f, alias(f), claims));
        refused = Project.claimants(f, alias(f), claims).filter(c -> !allowed(c)).toList();
        if (!refused.isEmpty()){ throw new UserError(Report.extensionsNotAllowed(refused)); }
      }
      catch (UserError e){ failed = true; stepFailed(f, e.getMessage()); }
    }
    l.corruption = Optional.empty();
    var refusedHere= rebuild(Optional.of(f)).or(() -> l.corruption);
    refusedHere.ifPresent(why -> output(f, why.stripTrailing()+"\n"));
    failed |= refusedHere.isPresent();
    if (!failed){ failed = !Project.shortcuts(Project.claimants(f, alias(f), l.claims)).stream().map(f::resolve).filter(p -> Fs.notExists(p)).allMatch(p -> shortcut(f, p)); }
    if (!failed){
      try{ Coordinator.finish(f); }
      catch (UncheckedIOException e){ failed = true; stepFailed(f, Report.cacheNotFinished(Report.fileFailure(e.getCause()))); }
      catch (UserError e){ failed = true; stepFailed(f, e.getMessage()); rebuild(Optional.empty()); }
    }
    output(f, "--- compile "+(!failed ? "done" : ec == 0 ? "failed" : "failed with "+ec)+" ---\n");
    scan(f);
    ended(f);
    if (!refused.isEmpty()){ allow(refused); }
  }
  /// A failing step after Coordinator.compile: the corruption rule for a corrupted cache, else the error in _error.txt and in the Output.
  private void stepFailed(Path f, String why){
    output(f, why.stripTrailing()+"\n");
    if (UserError.cacheCorrupted(why)){ corrupted(f, why); return; }
    errorFile(f, why);
  }
  private void errorFile(Path f, String why){ uncached(f, () -> Coordinator.writeError(f, why)); }
  private void uncached(Path f, Runnable r){
    try{ r.run(); }
    catch (UncheckedIOException e){ output(f, Report.cacheNotDeleted(Report.fileFailure(e.getCause()))); }
  }
  private void deleted(Path f){
    uncached(f, () -> Coordinator.deleteCache(f));
    live.get(f).claims = Project.noClaims;
  }
  private boolean broken(Path f){ return checked(f).map(x -> x.stale().isPresent() || x.problem().isPresent()).orElse(true); }
  private void allow(List<Project.Claimant> refused){
    view.allow(Report.allowExtensions(refused), refused.stream().map(c -> c.claim().extension()).distinct().toList());
  }
  private boolean shortcut(Path f, Path file){
    try{ Fs.writeString(file, MakeDemo.markerContent, StandardOpenOption.CREATE_NEW); return true; }
    catch (IOException e){ stepFailed(f, Report.notWritten(file.getFileName().toString(), e)); return false; }
  }
  private void terminate(Path f){
    var l= live.get(f);
    if (l.job.equals(Project.compiling)){ throw new Refused(Report.busy("Terminate", alias(f), l.job)); }
    selected = Optional.of(f);
    if (l.child != null){ endMain(f); }
  }
  /// Ends the running main of the project, and the run it belongs to, before the next request is applied.
  private void endMain(Path f){
    var l= live.get(f);
    l.terminated = true;
    l.stopped = true;
    output(f, "--- terminating "+l.job+" ---\n");
    var child= l.child;
    child.kill();
    exited(f, l, child, code(child));
  }
  private void deleteCache(Path f){
    idle(f, "DeleteCache");
    selected = Optional.of(f);
    deleted(f);
    rebuild(Optional.empty());
    scan(f);
  }
  private void kind(Path f, String text){
    var kind= Kind.of(text).orElseThrow();
    edit(f, e -> e.withKind(kind));
  }
  private void forget(Path f){
    var c= registry.content();
    edit(new Content(c.projects().stream().filter(e -> !e.path().equals(f)).toList(), c.extensions()));
  }
  private void mount(Path f, String type, Optional<String> data){ edit(f, o -> o.withMounts(with(o.mounts(), type, data))); }
  private void attenuate(Path f, String main, List<String> attenuators){
    edit(f, o -> o.withAttenuators(with(o.attenuators(), main, Optional.of(attenuators).filter(as -> !as.isEmpty()))));
  }
  private void edit(Path f, UnaryOperator<Entry> op){
    var c= registry.content();
    edit(new Content(c.projects().stream().map(e -> e.path().equals(f) ? op.apply(e) : e).toList(), c.extensions()));
  }
  private void edit(Content next){ edit(Registry.text(next), registry.infoFile()); }
  private static <T> Map<String,T> with(Map<String,T> map, String key, Optional<T> value){
    var out= new LinkedHashMap<>(map);
    value.ifPresentOrElse(v -> out.put(key, v), () -> out.remove(key));
    return out;
  }
  static Path projectFolder(Path path, Path managerDir){
    if (!Files.exists(path)){ throw new Refused(Report.registerNothing(path)); }
    return Registry.placed(Registry.real(Files.isDirectory(path) ? path : path.getParent()), managerDir, Refused::new);
  }
  private void register(Path path){
    var folder= projectFolder(path, dir);
    if (!live.containsKey(folder)){ add(folder); }
    selected = Optional.of(folder);
    scan(folder);
  }
  private void add(Path folder){
    registry.overlapping(folder).ifPresent(o -> { throw new Refused(Report.folderNestedWithRegistered(folder, o)); });
    String alias;
    try{
      Coordinator.deleteCache(folder);
      alias= Names.makeUnique(folder, registry.all().stream().map(Entry::alias).collect(Collectors.toSet()));
    }
    catch (UncheckedIOException e){ throw new Refused(Report.registerRefused(folder, Report.fileFailure(e.getCause()))); }
    catch (UserError e){ throw new Refused(Report.registerRefused(folder, e.getMessage())); }
    registry.add(alias, folder);
    open(registry.of(folder).orElseThrow());
  }
  private void connect(Path chosen){
    Path eclipse;
    try{
      eclipse= Eclipse.find(chosen).orElseThrow(() -> new Refused(Report.noEclipse(chosen)));
      tools.connect(eclipse, dir);
    }
    catch (UncheckedIOException e){ throw new Refused(Report.eclipseNotConnected(chosen, Report.fileFailure(e.getCause()))); }
    catch (UserError e){ throw new Refused(Report.eclipseNotConnected(chosen, e.getMessage())); }
    view.tell(Report.eclipseConnected(eclipse));
  }
  private void editMetadata(Path file){
    if (!Files.isRegularFile(file)){ throw new Refused(Report.metadataNoFile(file)); }
    try{ edit(UTF_8.newDecoder().decode(ByteBuffer.wrap(Files.readAllBytes(file))).toString(), file); }
    catch (CharacterCodingException e){ throw new Refused(Report.metadataNotUtf8(file)); }
    catch (IOException e){ throw new Refused(Report.metadataUnreadable(file, e)); }
    view.edited(file);
  }
  private void edit(String text, Path file){
    Content next;
    try{ next= Registry.parse(text, file, Registry::real, dir); }
    catch (UserError e){ throw new Refused(e.getMessage()); }
    registry.all().stream().filter(o -> !live.get(o.path()).job.isEmpty()).forEach(o -> kept(o, next));
    granted(next);
    if (!registry.extensions().equals(next.extensions())){
      refresh();
      reconcile(Optional.empty(), next.extensions());
    }
    var old= registry.content();
    registry.save(next, text);
    var removed= old.projects().stream().filter(o -> same(o, next).isEmpty()).toList();
    var added= next.projects().stream().filter(e -> same(e, old).isEmpty()).toList();
    removed.forEach(o -> drop(o.path()));
    added.forEach(e -> { uncached(e.path(), () -> Coordinator.deleteCache(e.path())); open(e); });
    var dropped= old.extensions().stream().filter(x -> !next.extensions().contains(x)).toList();
    var kinds= old.projects().stream().filter(o -> same(o, next).filter(e -> e.kind() != o.kind()).isPresent()).map(Entry::path).toList();
    var claiming= live.entrySet().stream().filter(e -> Project.claimants(e.getKey(), alias(e.getKey()), e.getValue().claims).anyMatch(c -> dropped.contains(c.claim().extension()))).map(Map.Entry::getKey).toList();
    Stream.concat(kinds.stream(), claiming.stream()).distinct().forEach(this::deleted);
    var removedCode= removed.stream().anyMatch(o -> o.kind() == Kind.code);
    if (removedCode || !added.isEmpty() || !kinds.isEmpty() || !claiming.isEmpty()){ rebuild(Optional.empty()); }
    registry.all().forEach(e -> scan(e.path()));
  }
  /// the entry of c with the name and the folder of e: a project whose name or folder changes is removed and added
  private static Optional<Entry> same(Entry e, Content c){ return OneOr.opt("project "+e.alias(), c.projects().stream().filter(n -> n.alias().equals(e.alias()) && n.path().equals(e.path()))); }
  private void kept(Entry o, Content next){
    var l= live.get(o.path());
    var e= same(o, next);
    if (e.isEmpty()){ throw new Refused(Report.removedWhileBusy(o.alias(), l.job)); }
    if (e.get().kind() != o.kind()){ throw new Refused(Report.kindWhileBusy(o.alias(), e.get().kind().text, l.job)); }
    var dropped= registry.extensions().stream().filter(x -> !next.extensions().contains(x)).toList();
    Project.claimants(o.path(), o.alias(), l.claims).map(c -> c.claim().extension()).filter(dropped::contains).findFirst()
      .ifPresent(x -> { throw new Refused(Report.extensionWhileBusy(o.alias(), x, l.job)); });
  }
  private void openWith(Path file, List<String> chosen){
    if (!Files.isRegularFile(file)){ throw new Refused(Report.openNoFile(file)); }
    if (Fs.of(() -> Files.size(file)) == 0){ throw new Refused(Report.emptyFile(file)); }
    var name= file.getFileName().toString();
    var dot= name.lastIndexOf('.');
    var ext= dot < 0 ? "" : name.substring(dot+1).toLowerCase(Locale.ROOT);
    var cs= Project.claimed(projects()).getOrDefault(ext, List.of());
    if (cs.isEmpty()){ throw new Refused(Report.noMainOpens(file, ext)); }
    var picked= chosen.isEmpty() ? cs : cs.stream().filter(c -> c.label().equals(chosen.get(0)+"::"+chosen.get(1))).toList();
    if (picked.isEmpty()){ throw new Refused(Report.notOpening(file, chosen.get(0), chosen.get(1), ext, cs.stream().map(Project.Claimant::label).toList())); }
    if (picked.size() > 1){ view.choose(file, cs); return; }
    var c= picked.getFirst();
    if (!scan(c.folder())){ unreachable(c.folder()); openWith(file, chosen); return; }
    run(c.folder(), List.of(c.main()), Optional.empty(), Optional.of(file));
  }
  private void drop(Path f){
    var l= live.remove(f);
    if (l.child != null){ l.terminated = true; l.child.kill(); }
    l.follow.ifPresent(Follow::end);
    if (selected.equals(Optional.of(f))){ selected = Optional.empty(); }
  }
  /// The 'autoCheck' of the project: false for a project folder the manager can not reach, whose Forget is then posted.
  private boolean scan(Path f){
    var l= live.get(f);
    var compiling= l.job.equals(Project.compiling);
    var found= compiling ? Optional.of(l.facts).filter(_ -> Fs.isDirectory(f)) : checked(f);
    if (found.isEmpty()){
      if (l.facts == null){ l.facts = Facts.invalid(Report.unreachable(alias(f), f)); }
      post(() -> unreachable(f));
      return false;
    }
    var fresh= found.get();
    if (compiling){ return true; }
    if (!l.job.isEmpty()){
      if (fresh.stale().isPresent() || fresh.problem().isPresent()){ l.terminated = true; l.updated = true; l.child.kill(); }
      return true;
    }
    if (fresh.problem().isPresent()){ errorFile(f, fresh.problem().get()); }
    if (fresh.equals(l.scanned)){ return true; }
    l.scanned = fresh;
    l.facts = fresh;
    l.mains = fresh.mains();
    return true;
  }
  private Optional<Facts> checked(Path f){
    try{ return facts(f); }
    catch (UserError err){ corrupted(f, err.getMessage()); rebuild(Optional.empty()); return facts(f); }
  }
  private Optional<Facts> facts(Path f){
    var e= registry.of(f).orElseThrow();
    return Facts.of(f, e.alias(), e.kind(), tools::check);
  }
  /// The 'autoCheck' of a project folder the manager can not reach: the job of the project ends, and so does every running main
  /// of a project with a mount of that project; a dialog says so, then the project is forgotten.
  private void unreachable(Path f){
    var l= live.get(f);
    if (l == null){ return; }
    if (l.child != null){ l.terminated = true; l.child.kill(); }
    l.job = "";
    var alias= alias(f);
    registry.all().stream().filter(e -> e.mounts().containsValue(alias) && live.get(e.path()).child != null && !live.get(e.path()).job.equals(Project.compiling)).map(Entry::path).toList().forEach(this::endMain);
    view.refuse(Report.unreachable(alias(f), f));
    forget(f);
  }
  private void rotate(){
    if (!view.visible()){ return; }
    selected.ifPresent(this::scan);
    var others= registry.all().stream().map(Entry::path).filter(f -> !selected.equals(Optional.of(f))).toList();
    if (others.isEmpty()){ return; }
    turn += 1;
    scan(others.get(turn % others.size()));
  }
  /// Refuses a change of the mounts of a project with a running main, and of the kind or the folder of a data project mounted there.
  private void granted(Content next){
    for (var e : live.entrySet()){
      var job= e.getValue().job;
      if (job.isEmpty() || job.equals(Project.compiling)){ continue; }
      var now= registry.of(e.getKey()).orElseThrow();
      var changed= OneOr.opt("project "+e.getKey(), next.projects().stream().filter(p -> p.path().equals(e.getKey())))
        .filter(p -> !p.mounts().equals(now.mounts()) || now.mounts().values().stream().anyMatch(d -> !data(registry.content(), d).equals(data(next, d))));
      changed.ifPresent(p -> { throw new Refused(Report.grantsWhileRunning(p.alias(), job)); });
    }
  }
  private static Optional<List<Object>> data(Content c, String name){ return OneOr.opt("project "+name, c.projects().stream().filter(p -> p.alias().equals(name))).map(p -> List.of(p.kind(), p.path())); }
  private Project project(Entry e){
    var l= live.get(e.path());
    return new Project(e, l.facts, l.mains, l.claims, l.job);
  }
  private Project project(Path f){ return project(registry.of(f).orElseThrow()); }
  private List<Project> projects(){ return registry.all().stream().map(this::project).toList(); }
  private String alias(Path f){ return registry.of(f).orElseThrow().alias(); }
  private void publish(){
    var next= new State(projects(), selected, registry.extensions());
    if (next.equals(state)){ return; }
    state = next;
    view.state(next);
  }
  private void output(Path f, String text){
    view.output(f, text);
    live.get(f).follow.ifPresent(fl -> fl.send(text));
  }
  private Consumer<String> out(Path f){
    var l= live.get(f);
    return s -> post(() -> late(f, l, s));
  }
  private void late(Path f, Live l, String text){
    if (live.get(f) != l){ return; }
    if (!l.job.equals(Project.compiling)){ output(f, text); return; }
    l.line.append(text);
    for (var nl= l.line.indexOf("\n"); nl >= 0; nl = l.line.indexOf("\n")){
      var line= l.line.substring(0, nl+1);
      l.line.delete(0, nl+1);
      if (line.startsWith(ChildMain.takeFreeExtension)){ answer(l, List.of(line.substring(ChildMain.takeFreeExtension.length()).strip().split(" "))); continue; }
      l.compiled.append(line);
      output(f, line);
    }
  }
  private void flushLine(Path f, Live l){
    if (l.line.isEmpty()){ return; }
    l.compiled.append(l.line);
    output(f, l.line.toString());
    l.line.setLength(0);
  }
  /// takeFreeExtension: the first candidate no claim of the map uses and no call answered for a compilation that has not ended yet.
  private void answer(Live l, List<String> candidates){
    var used= live.values().stream().flatMap(o -> Stream.concat(o.answered.stream(), o.claims.mains().values().stream().flatMap(m -> Stream.concat(m.shortcuts().stream(), m.openWiths().stream())).map(MainsInfo.Claim::extension))).collect(Collectors.toSet());
    var free= candidates.stream().filter(c -> !used.contains(c)).findFirst();
    free.ifPresent(l.answered::add);
    if (l.child == null){ return; }
    //A compile JVM that died meanwhile does not read the answer: its end is reported when its exit is processed.
    try{ l.child.send(free.orElse("")); }
    catch (UncheckedIOException e){}
  }
}

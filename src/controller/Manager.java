package controller;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;
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

import controller.Project.Job;
import controller.Registry.Content;
import controller.Registry.Entry;
import controller.Registry.Kind;
import coordinator.Coordinator;
import coordinator.MainsInfo;
import fileAssociations.Icon;
import mainCoordinator.MakeDemo;
import managerMessages.Report;
import managerMessages.Violation;
import realSourceOracle.Fix;
import tools.ChildJvm;
import tools.Fs;
import userMessages.UserError;
import utils.Push;
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
    public Optional<Project> of(Path folder){ return OneOr.opt("project "+folder, projects.stream().filter(p -> Registry.same(p.folder(), folder))); }
    public Optional<Project> shown(){ return selected.flatMap(this::of); }
    public List<String> running(){ return projects.stream().filter(Project::busy).map(p -> p.alias()+" - "+(p.job() instanceof Job.Main(var main) ? main : "compiling")).toList(); }
  }
  enum Verb{
    Show("", "projectName"), Quit(""), Reset(""),
    Register("base16"), ConnectEclipse("base16"), EditMetadata("base16"), OpenWithOrShortcut("base16", "base16 projectName typeName"),
    Run("projectName"), Compile("projectName"), Check("projectName"), Terminate("projectName"), DeleteCache("projectName"),
    Kind("projectName kind"), Forget("projectName"), Fix("projectName"), Mount("projectName typeName projectName"), Unmount("projectName typeName"),
    Attenuate("projectName typeName typeName1..typeNamen"), SelectMains("projectName typeName1..typeNamen");
    final List<List<String>> forms;
    Verb(String... forms){ this.forms= Stream.of(forms).map(f -> List.of((name()+" "+f).split(" "))).toList(); }
  }
  private enum Cut{ none, filesUpdated, stopped }
  private static final class Live{
    Facts facts;
    Optional<MainsInfo> mains= Optional.empty();
    MainsInfo claims= Project.noClaims;
    Job job= new Job.Idle();
    ChildJvm child;
    Cut cut= Cut.none;
    Optional<String> corruption= Optional.empty();
    List<String> todo= List.of();
    Optional<Path> input= Optional.empty();
    Registry.Grants grants;
    Map<String,List<String>> attenuators= Map.of();
    Optional<Follow> follow= Optional.empty();
    Set<String> answered= new HashSet<>();
    StringBuilder line= new StringBuilder();
    List<Project.Ran> ran= List.of();
    ConcurrentLinkedQueue<String> printed= new ConcurrentLinkedQueue<>();
  }
  private static final String conflict= "base.IconsConflict";
  private static final String fearless= "base.IconsFearless";
  private final ScheduledExecutorService core= Executors.newSingleThreadScheduledExecutor();
  public final Path dir;
  private final Tools tools;
  private final View view;
  private final Exit exit;
  private final Registry registry;
  private final Map<Path,Live> live= new TreeMap<>(Registry.exact);
  private Optional<Path> selected= Optional.empty();
  private Set<String> captured= Set.of();
  private int turn;
  private volatile State state= new State(List.of(), Optional.empty(), List.of());
  private boolean ending;
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
    try{
      core.submit(this::last).get(1, TimeUnit.MINUTES);
      if (!core.awaitTermination(1, TimeUnit.MINUTES)){ throw new TimeoutException(); }
    }
    catch (InterruptedException|ExecutionException|TimeoutException e){ throw Bug.of(e); }
  }
  private void last(){
    end();
    core.shutdownNow();
  }
  //Runs once before the watcher thread starts, then only from that thread: never concurrently.
  public void drain(){ post(() -> taken().forEach(this::apply)); }
  private List<String> taken(){
    var msgDir= dir.resolve("messages");
    try{ return Fs.of(() -> take(msgDir)); }
    catch (UncheckedIOException e){ throw Violation.couldNotDrainMessageFolder(msgDir, e.getCause()); }
  }
  private List<String> take(Path msgDir) throws IOException{
    var res= new ArrayList<String>();
    for (var file : Names.list(msgDir).stream().filter(f -> f.toString().endsWith(".msg")).toList()){
      String text;
      try{ text= UTF_8.newDecoder().decode(ByteBuffer.wrap(Fs.readAllBytes(file))).toString(); }
      catch (CharacterCodingException e){ throw Violation.messageNotUtf8(file); }
      Fs.deleteIfExists(file);
      res.add(text);
    }
    var old= Instant.now().minusSeconds(60);
    for (var file : Names.list(msgDir).stream().filter(f -> f.toString().endsWith(".tmp")).toList()){
      try{ if (Fs.getLastModifiedTime(file).toInstant().isBefore(old)){ Fs.deleteIfExists(file); } }
      catch (NoSuchFileException e){}
    }
    return res;
  }
  void settle(){
    try{ core.submit(() -> {}).get(1, TimeUnit.MINUTES); }
    catch (InterruptedException|ExecutionException|TimeoutException e){ throw Bug.of(e); }
  }
  private void post(Runnable r){ core.execute(() -> step(r)); }
  private void step(Runnable r){
    try{ r.run(); publish(); }
    catch (NotOurs n){ exit.fail(n.error); }
    catch (Throwable t){ exit.fail(t); }
  }
  private void load(){
    registry.all().forEach(e -> live.put(e.path(), new Live()));
    registry.all().stream().filter(e -> e.kind() == Kind.code && registry.reaches(e.path())).forEach(e -> uncached(e.path(), () -> Coordinator.dropUnfinished(e.path())));
    Map.of(conflict, "conflict.png", fearless, "fearless.png").forEach((icon, png) -> ClaimIcons.materialise(dir.resolve("icons"), icon, Fs.of(() -> Fs.readAllBytes(tools.stdLibBase().resolve("icons").resolve(png)))));
    rebuild();
    dropLocks();
    registry.all().forEach(e -> scan(e.path()));
  }
  private void dropLocks(){
    Fs.ensureDir(dir.resolve("locks"));
    var keep= registry.all().stream().map(e -> lockFile(e.path())).flatMap(l -> Stream.of(l, inner(l))).collect(Collectors.toSet());
    var stray= Fs.of(() -> { try (var s= Files.list(dir.resolve("locks"))){ return s.filter(p -> !keep.contains(p)).toList(); } });
    stray.stream().filter(Manager::free).forEach(p -> Fs.ofV(() -> Files.delete(p)));
  }
  static Path inner(Path lock){ return lock.resolveSibling(lock.getFileName().toString().replace(".lock", "_inner.lock")); }
  private static boolean free(Path lock){
    try (var ch= FileChannel.open(lock, StandardOpenOption.WRITE); var held= ch.tryLock()){ return held != null; }
    catch (IOException e){ throw new UncheckedIOException(e); }
  }
  private void open(Entry e){
    live.put(e.path(), new Live());
    scan(e.path());
  }
  private boolean allowed(Project.Claimant c){ return !Registry.isSystem(c.claim().extension()) || registry.extensions().contains(c.claim().extension()); }
  private void rebuild(){ refresh(); reconcile(registry.extensions()); }
  /// Rebuilds the in memory map, then reconciles; gives the error of compiled when a new extension of compiled is refused.
  private Optional<String> rebuild(Path compiled){
    try{ rebuild(); return Optional.empty(); }
    catch (NotOurs n){
      var brought= Project.claimants(compiled, alias(compiled), live.get(compiled).claims).map(c -> "."+c.claim().extension()).filter(e -> !captured.contains(e)).collect(Collectors.toSet());
      if (n.extensions.contains(".fearless") || !brought.containsAll(n.extensions)){ throw n.error; }
      var claimed= Project.claimed(projects());
      var hit= n.extensions.stream().flatMap(e -> claimed.getOrDefault(e.substring(1), List.of()).stream()).map(Project.Claimant::folder).distinct().toList();
      assert hit.contains(compiled);
      for (var g : hit){
        endJob(g);
        deleted(g);
        errorFile(g, n.error.getMessage());
      }
      rebuild();
      return Optional.of(n.error.getMessage());
    }
  }
  /// the claims of the in memory map, read again from the caches
  private void refresh(){
    for (var e : registry.all()){
      var l= live.get(e.path());
      if (e.kind() != Kind.code){ l.claims = Project.noClaims; continue; }
      if (l.job instanceof Job.Compile){ continue; }
      l.claims = cached(e.path());
    }
  }
  private MainsInfo cached(Path f){
    try{ return new MainsInfo(Coordinator.cachedMains(f)); }
    catch (UserError e){
      if (UserError.cacheCorrupted(e.getMessage()) && live.get(f).corruption.isEmpty()){ corrupted(f, e.getMessage()); }
      return Project.noClaims;
    }
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
    l.cut = Cut.stopped;
    l.child.kill();
  }
  private void end(){
    ending = true;
    live.keySet().forEach(this::endJob);
  }
  private void reconcile(List<String> extensions){
    var claimed= Project.claimed(projects());
    var next= captured(claimed, extensions);
    tools.associate(next, preloads().toList(), e -> Report.claimedBy(claimed.getOrDefault(e.substring(1), List.of())));
    captured = next.stream().map(Icon::extension).collect(Collectors.toSet());
  }
  private Stream<Project.Claimant> preloads(){ return projects().stream().filter(p -> !(p.job() instanceof Job.Compile)).flatMap(p -> Project.preloads(p.folder(), p.alias(), p.claims())); }
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
    if (verb.equals("FatalError")){ throw Violation.fatalFromRun(String.join("\n", args)); }
    var port= !args.isEmpty() && args.getLast().startsWith("Port:");
    var given= port ? args.subList(0, args.size()-1) : args;
    var follow= Optional.<Follow>empty();
    try{
      follow = port ? Optional.of(Follow.to(verb, args.getLast())) : follow;
      if (verb.equals("InvalidCall")){ throw new Refused(String.join("\n", given)); }
      var v= Verb.valueOf(verb);
      request(v, given, follow);
      follow.ifPresent(Follow::applied);
      if (v != Verb.Run && v != Verb.Compile){ follow.ifPresent(Follow::end); }
    }
    catch (Refused r){
      var why= verb.equals("InvalidCall") ? String.join("\n", given) : r.getMessage();
      follow.ifPresent(fl -> fl.refuse(why));
      view.refuse(why);
    }
  }
  private void request(Verb verb, List<String> args, Optional<Follow> follow){
    if (ending){ throw new Refused(Report.quitting(verb.name())); }
    switch (verb){
      case Show -> { if (!args.isEmpty()){ selected = Optional.of(registered(verb, args.getFirst())); } view.show(); }
      case Quit -> { end(); exit.quit(); }
      case Reset -> { end(); exit.reset(); }
      case Register -> register(path(args.getFirst()));
      case ConnectEclipse -> connect(path(args.getFirst()));
      case EditMetadata -> editMetadata(path(args.getFirst()));
      case OpenWithOrShortcut -> openWith(path(args.getFirst()), args.subList(1, args.size()));
      case Run -> { var f= registered(verb, args.getFirst()); run(f, registry.of(f).orElseThrow().mains(), follow, Optional.empty()); }
      case Compile -> compile(registered(verb, args.getFirst()), follow);
      case Check -> check(registered(verb, args.getFirst()), follow);
      case Terminate -> terminate(registered(verb, args.getFirst()));
      case DeleteCache -> deleteCache(registered(verb, args.getFirst()));
      case Kind -> kind(registered(verb, args.getFirst()), args.get(1));
      case Forget -> forget(registered(verb, args.getFirst()));
      case Fix -> fix(registered(verb, args.getFirst()), follow);
      case Mount -> mount(registered(verb, args.getFirst()), args.get(1), Optional.of(lookedUp(args.get(2)).orElseThrow(() -> new Refused(Report.unknownData(args.getFirst(), args.get(2), registry.all().stream().map(Entry::alias).toList()))).alias()));
      case Unmount -> mount(registered(verb, args.getFirst()), args.get(1), Optional.empty());
      case Attenuate -> attenuate(registered(verb, args.getFirst()), args.get(1), args.subList(2, args.size()));
      case SelectMains -> edit(registered(verb, args.getFirst()), e -> e.withMains(args.subList(1, args.size())));
      default -> throw Bug.unreachable();
    }
  }
  /// A path of a message, as the manager program wrote it.
  private static Path path(String text){ return TaggedText.readPath(text, Bug::of).normalize(); }
  private Path registered(Verb verb, String name){
    Supplier<Refused> unknown= () -> new Refused(Report.unknownProject(verb.name(), name, registry.all().stream().map(Entry::alias).toList()));
    var f= lookedUp(name).orElseThrow(unknown).path();
    if (!scan(f)){ unreachable(f); throw unknown.get(); }
    return f;
  }
  private Optional<Entry> lookedUp(String name){
    var res= renaming(() -> registry.lookedUp(name));
    registry.all().stream().filter(e -> e.alias().isEmpty()).map(Entry::path).toList().forEach(this::unreachable);
    return res;
  }
  /// rebuilds the in memory map when the names of the projects change
  private <T> T renaming(Supplier<T> look){
    var before= registry.all().stream().map(Entry::alias).toList();
    var res= look.get();
    if (!before.equals(registry.all().stream().map(Entry::alias).toList())){ rebuild(); }
    return res;
  }
  private void idle(Path f, Verb verb){
    var job= live.get(f).job;
    if (!(job instanceof Job.Idle)){ throw new Refused(Report.busy(verb.name(), alias(f), job)); }
  }
  private void runnable(Path f, Verb verb){
    idle(f, verb);
    var p= project(f);
    if (p.kind() != Kind.code){ throw new Refused(Report.notCode(verb.name(), p.alias(), p.kind().text)); }
    p.problem().ifPresent(why -> { throw new Refused(Report.invalidContent(verb.name(), p.alias(), why)); });
  }
  private void run(Path f, List<String> mains, Optional<Follow> follow, Optional<Path> input){
    runnable(f, Verb.Run);
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
    l.cut = Cut.none;
    l.todo = mains;
    l.ran = List.of();
    l.input = input;
    l.grants = Registry.grants(registry.content(), registry.of(f).orElseThrow(), l.mains.orElseThrow().dataEdit(), this::lockFile);
    l.attenuators = attenuated;
    l.follow = follow;
    view.clear(f);
    next(f);
  }
  private void compile(Path f, Optional<Follow> follow){
    runnable(f, Verb.Compile);
    selected = Optional.of(f);
    if (!project(f).needsCompiling()){ upToDate(f, follow); return; }
    live.get(f).follow = follow;
    view.clear(f);
    output(f, "--- compiling "+alias(f)+" ---\n");
    start(f, new Job.Compile(), () -> tools.compile(f, alias(f), out(f)));
  }
  private void upToDate(Path f, Optional<Follow> follow){
    view.clear(f);
    var text= Report.alreadyCompiled(alias(f))+"\n";
    view.output(f, text);
    follow.ifPresent(fl -> { fl.send(text); fl.end(); });
  }
  private void check(Path f, Optional<Follow> follow){
    idle(f, Verb.Check);
    selected = Optional.of(f);
    var p= project(f);
    view.clear(f);
    var text= p.problem().or(() -> p.facts().stale()).orElse(Report.checkFine(p.alias(), p.kind() == Kind.code)).stripTrailing()+"\n";
    view.output(f, text);
    follow.ifPresent(fl -> fl.send(text));
  }
  private void fix(Path f, Optional<Follow> follow){
    project(f).usedBy(projects()).ifPresent(u -> { throw new Refused(Report.inUse(Verb.Fix.name(), alias(f), u.alias(), u.job())); });
    selected = Optional.of(f);
    view.clear(f);
    var said= new StringBuilder();
    try{ Fix.of(f, s -> said.append(s).append("\n")); }
    catch (UncheckedIOException e){ said.append(Report.fileFailure(e.getCause())).append("\n"); }
    catch (UserError e){ said.append(e.getMessage().stripTrailing()).append("\n"); }
    var text= said.isEmpty() ? Report.needsNoFix(alias(f))+"\n" : said.toString();
    view.output(f, text);
    follow.ifPresent(fl -> fl.send(text));
    scan(f);
  }
  private void next(Path f){
    var l= live.get(f);
    if (l.cut != Cut.none || l.todo.isEmpty()){ ended(f); return; }
    var main= l.todo.getFirst();
    l.todo = l.todo.subList(1, l.todo.size());
    output(f, "--- running "+main+" ---\n");
    l.ran = Push.of(l.ran, new Project.Ran(main, Instant.now()));
    start(f, new Job.Main(main), () -> tools.run(f, main, l.input, l.grants, l.attenuators.getOrDefault(main, List.of()), out(f)));
  }
  private void ended(Path f){
    var l= live.get(f);
    l.job = new Job.Idle();
    l.follow.ifPresent(Follow::end);
    l.follow = Optional.empty();
    scan(f);
  }
  private void start(Path f, Job job, Supplier<ChildJvm> jvm){
    var l= live.get(f);
    try{ l.child = jvm.get(); }
    catch (UncheckedIOException e){
      output(f, "--- "+(job instanceof Job.Main(var main) ? main : "compile")+" did not start: "+Report.fileFailure(e.getCause())+"\n---\n");
      ended(f);
      return;
    }
    l.job = job;
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
    switch (l.job){
      case Job.Compile _ -> compiled(f, l, ec);
      case Job.Main(var main) -> mainExited(f, l, main, ec);
      case Job.Idle _ -> throw Bug.unreachable();
    }
  }
  private void mainExited(Path f, Live l, String main, int ec){
    output(f, "--- "+main+(l.cut != Cut.none ? " was terminated" : " exited with "+ec)+" after "+Duration.between(l.ran.getLast().since(), Instant.now()).toSeconds()+"s ---\n");
    if (l.cut == Cut.filesUpdated || l.cut == Cut.none && broken(f)){ l.cut = Cut.filesUpdated; output(f, Report.filesUpdated(alias(f), main)+"\n"); }
    next(f);
  }
  /// What the manager does after Coordinator.compile: step 1 only after a compilation with no error, step 2 always, steps 3 and 4 only with no error.
  private void compiled(Path f, Live l, int ec){
    flushLine(f, l);
    l.job = new Job.Idle();
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
    var refusedHere= rebuild(f).or(() -> l.corruption);
    refusedHere.ifPresent(why -> output(f, why.stripTrailing()+"\n"));
    failed |= refusedHere.isPresent();
    if (!failed){ failed = !Project.shortcuts(Project.claimants(f, alias(f), l.claims)).stream().map(f::resolve).filter(p -> Fs.notExists(p)).allMatch(p -> shortcut(f, p)); }
    if (!failed){
      try{ Coordinator.finish(f); }
      catch (UncheckedIOException e){ failed = true; stepFailed(f, Report.cacheNotFinished(Report.fileFailure(e.getCause()))); }
      catch (UserError e){ failed = true; stepFailed(f, e.getMessage()); rebuild(); }
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
  /// _error.txt passes the error on to Eclipse: a folder where it can not be written gets none
  private void errorFile(Path f, String why){
    try{ Coordinator.writeError(f, why); }
    catch (UncheckedIOException | UserError _){}
  }
  private void uncached(Path f, Runnable r){
    try{ r.run(); }
    catch (UncheckedIOException e){ output(f, Report.cacheNotDeleted(Report.fileFailure(e.getCause()))); }
    catch (UserError e){ output(f, e.getMessage().stripTrailing()+"\n"); }
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
    if (l.job instanceof Job.Compile){ throw new Refused(Report.busy(Verb.Terminate.name(), alias(f), l.job)); }
    selected = Optional.of(f);
    if (l.child != null){ endMain(f); }
  }
  /// Ends the running main of the project, and the run it belongs to, before the next request is applied.
  private void endMain(Path f){
    var l= live.get(f);
    l.cut = Cut.stopped;
    output(f, "--- terminating "+((Job.Main)l.job).main()+" ---\n");
    var child= l.child;
    child.kill();
    var ec= code(child);
    drain(f, l);
    exited(f, l, child, ec);
  }
  private void deleteCache(Path f){
    idle(f, Verb.DeleteCache);
    selected = Optional.of(f);
    deleted(f);
    rebuild();
    scan(f);
  }
  private void kind(Path f, String text){
    var kind= Kind.of(text).orElseThrow();
    edit(f, e -> e.withKind(kind));
  }
  private void forget(Path f){
    var c= registry.content();
    edit(new Content(c.projects().stream().filter(e -> !Registry.same(e.path(), f)).toList(), c.extensions()));
  }
  private void mount(Path f, String type, Optional<String> data){ edit(f, o -> o.withMounts(with(o.mounts(), type, data))); }
  private void attenuate(Path f, String main, List<String> attenuators){
    edit(f, o -> o.withAttenuators(with(o.attenuators(), main, Optional.of(attenuators).filter(as -> !as.isEmpty()))));
  }
  private void edit(Path f, UnaryOperator<Entry> op){
    var c= registry.content();
    edit(new Content(c.projects().stream().map(e -> Registry.same(e.path(), f) ? op.apply(e) : e).toList(), c.extensions()));
  }
  private void edit(Content next){ edit(Registry.text(next), registry.infoFile()); }
  private static <T> Map<String,T> with(Map<String,T> map, String key, Optional<T> value){
    var out= new LinkedHashMap<>(map);
    value.ifPresentOrElse(v -> out.put(key, v), () -> out.remove(key));
    return out;
  }
  static Path projectFolder(Path path){
    if (!Files.exists(path)){ throw new Refused(Report.registerNothing(path)); }
    return Files.isDirectory(path) ? path : path.getParent();
  }
  static Map<Path,Registry.Away> away(Path folder, List<Path> registered, Path managerDir){
    return Registry.away(Push.of(registered.stream().filter(r -> !Registry.same(r, folder)).toList(), folder), managerDir);
  }
  static Path chosen(Path folder, Map<Path,Registry.Away> away){
    return switch (away.get(folder)){
      case null -> folder;
      case Registry.Same(var same) -> same;
      case Registry.Why(var why) -> throw new Refused(why);
    };
  }
  private void register(Path path){
    var folder= projectFolder(path);
    var away= away(folder, Registry.folders(registry.content()), dir);
    var chosen= chosen(folder, away);
    if (!live.containsKey(chosen)){ add(chosen, away.keySet()); }
    selected = Optional.of(chosen);
    scan(chosen);
  }
  private void add(Path folder, Set<Path> away){
    String alias;
    try{
      Coordinator.deleteCache(folder);
      alias= Names.infer(folder, registry.all().stream().map(Entry::alias).collect(Collectors.toSet()));
      Names.nameAs(folder, alias);
    }
    catch (UncheckedIOException e){ throw new Refused(Report.registerRefused(folder, Report.fileFailure(e.getCause()))); }
    catch (UserError e){ throw new Refused(Report.registerRefused(folder, e.getMessage())); }
    registry.add(alias, folder, away);
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
    Content parsed;
    try{ parsed= Registry.parse(text, file); }
    catch (UserError e){ throw new Refused(e.getMessage()); }
    var away= registry.conditions(Registry.folders(parsed));
    var next= Registry.inferred(parsed, registry.content(), away);
    registry.all().stream().filter(o -> !(live.get(o.path()).job instanceof Job.Idle)).forEach(o -> kept(o, next));
    granted(next);
    if (!registry.extensions().equals(next.extensions())){ reconcile(next.extensions()); }
    var old= registry.content();
    registry.save(next, text, away);
    var removed= old.projects().stream().filter(o -> same(o, next).isEmpty()).toList();
    var added= next.projects().stream().filter(e -> same(e, old).isEmpty()).toList();
    removed.forEach(o -> drop(o.path()));
    added.forEach(e -> live.put(e.path(), new Live()));
    added.stream().filter(e -> registry.reaches(e.path())).forEach(e -> uncached(e.path(), () -> Coordinator.deleteCache(e.path())));
    added.forEach(e -> scan(e.path()));
    var dropped= old.extensions().stream().filter(x -> !next.extensions().contains(x)).toList();
    var kinds= old.projects().stream().filter(o -> same(o, next).filter(e -> e.kind() != o.kind()).isPresent()).map(Entry::path).toList();
    var claiming= live.entrySet().stream().filter(e -> Project.claimants(e.getKey(), alias(e.getKey()), e.getValue().claims).anyMatch(c -> dropped.contains(c.claim().extension()))).map(Map.Entry::getKey).toList();
    Stream.concat(kinds.stream(), claiming.stream()).distinct().forEach(this::deleted);
    var removedCode= removed.stream().anyMatch(o -> o.kind() == Kind.code);
    var renamed= registry.all().stream().anyMatch(e -> same(e, old).filter(o -> !o.alias().equals(e.alias())).isPresent());
    if (removedCode || renamed || !added.isEmpty() || !kinds.isEmpty() || !claiming.isEmpty()){ rebuild(); }
    registry.all().forEach(e -> scan(e.path()));
  }
  /// the entry of c with the folder of e: a project whose folder changes is removed and added
  private static Optional<Entry> same(Entry e, Content c){ return OneOr.opt("project "+e.path(), c.projects().stream().filter(n -> Registry.same(n.path(), e.path()))); }
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
    if (!chosen.isEmpty()){ lookedUp(chosen.getFirst()); }
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
    l.follow.ifPresent(Follow::end);
    if (selected.filter(s -> Registry.same(s, f)).isPresent()){ selected = Optional.empty(); }
  }
  /// The 'autoCheck' of the project: false for a project folder the manager can not reach, whose Forget is then posted.
  private boolean scan(Path f){
    renaming(() -> registry.nameCheck(f));
    var l= live.get(f);
    var compiling= l.job instanceof Job.Compile;
    var named= !alias(f).isEmpty();
    var found= !named || !registry.reaches(f) ? Optional.<Facts>empty() : compiling ? Optional.of(l.facts) : checked(f);
    if (found.isEmpty()){
      if (l.facts == null){ l.facts = Facts.invalid(Report.unreachable(alias(f), f)); }
      post(() -> unreachable(f));
      return false;
    }
    var fresh= found.get();
    if (compiling){ return true; }
    if (l.job instanceof Job.Main){
      if (fresh.stale().isPresent() || fresh.problem().isPresent()){
        if (l.cut == Cut.none){ l.cut = Cut.filesUpdated; }
        l.child.kill();
        return true;
      }
      l.facts = fresh;
      return true;
    }
    if (fresh.problem().isPresent()){ errorFile(f, fresh.problem().get()); }
    if (fresh.equals(l.facts)){ return true; }
    l.facts = fresh;
    l.mains = fresh.mains();
    return true;
  }
  /// a cache the corruption rule could not delete is shown as 'invalid' until the next compilation, not corrected again
  private Optional<Facts> checked(Path f){
    try{ return facts(f); }
    catch (UserError err){
      if (live.get(f).corruption.isPresent()){ return Optional.of(Facts.invalid(err.getMessage())); }
      corrupted(f, err.getMessage());
      rebuild();
    }
    try{ return facts(f); }
    catch (UserError err){ return Optional.of(Facts.invalid(err.getMessage())); }
  }
  private Optional<Facts> facts(Path f){
    var e= registry.of(f).orElseThrow();
    Supplier<Optional<Facts>> read= () -> Facts.of(f, e.alias(), e.kind(), tools::check, live.get(f).ran.stream().findFirst().map(Project.Ran::since).orElse(Instant.MAX));
    if (!e.kind().isData()){ return read.get(); }
    var known= live.get(f).facts;
    try (var ch= FileChannel.open(lockFile(f), StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE)){
      var held= known == null ? ch.lock(0, Long.MAX_VALUE, true) : ch.tryLock(0, Long.MAX_VALUE, true);
      if (held == null){ return Optional.of(known); }
      return read.get();
    }
    catch (IOException ex){ throw new UncheckedIOException(ex); }
  }
  Path lockFile(Path folder){
    var locks= dir.resolve("locks");
    Fs.ensureDir(locks);
    try{ return locks.resolve(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(TaggedText.base16(folder.toAbsolutePath()).getBytes(UTF_8)))+".lock"); }
    catch (NoSuchAlgorithmException ex){ throw Bug.of(ex); }
  }
  /// The 'autoCheck' of a project folder the manager can not reach: the job of the project ends, and so does every running main
  /// of a project with a mount of that project; a dialog says so, then the project is forgotten.
  private void unreachable(Path f){
    var l= live.get(f);
    if (l == null){ return; }
    if (l.child != null && l.job instanceof Job.Main){ endMain(f); }
    registry.all().stream().map(Entry::path).filter(g -> mounts(g, f)).toList().forEach(this::endMain);
    if (l.child != null){ endCompile(f, l); }
    l.job = new Job.Idle();
    view.refuse(Report.unreachable(alias(f), f));
    try{ forget(f); }
    catch (Refused r){ view.refuse(r.getMessage()); }
  }
  private void endCompile(Path f, Live l){
    var child= l.child;
    l.child = null;
    child.kill();
    code(child);
    drain(f, l);
    flushLine(f, l);
    output(f, "--- compile was terminated ---\n");
  }
  private boolean mounts(Path g, Path f){
    var l= live.get(g);
    return l.child != null && l.job instanceof Job.Main && l.grants.mounts().containsValue(f);
  }
  private void rotate(){
    if (!view.visible()){ return; }
    selected.ifPresent(this::scan);
    var others= registry.all().stream().map(Entry::path).filter(f -> selected.filter(s -> Registry.same(s, f)).isEmpty()).toList();
    if (others.isEmpty()){ return; }
    turn += 1;
    scan(others.get(turn % others.size()));
  }
  /// Refuses a change of the mounts of a project with a running main, and of the kind or the folder of a data project mounted there.
  private void granted(Content next){
    for (var e : live.entrySet()){
      var job= e.getValue().job;
      if (!(job instanceof Job.Main)){ continue; }
      var now= registry.of(e.getKey()).orElseThrow();
      var changed= OneOr.opt("project "+e.getKey(), next.projects().stream().filter(p -> Registry.same(p.path(), e.getKey())))
        .filter(p -> !p.mounts().equals(now.mounts()) || now.mounts().values().stream().anyMatch(d -> !data(registry.content(), d).equals(data(next, d))));
      changed.ifPresent(p -> { throw new Refused(Report.grantsWhileRunning(p.alias(), job)); });
    }
  }
  private static Optional<List<Object>> data(Content c, String name){ return OneOr.opt("project "+name, c.projects().stream().filter(p -> p.alias().equals(name))).map(p -> List.of(p.kind(), p.path())); }
  private Project project(Entry e){
    var l= live.get(e.path());
    return new Project(e, l.facts, l.mains, l.claims, l.job, l.ran);
  }
  private Project project(Path f){ return project(registry.of(f).orElseThrow()); }
  private List<Project> projects(){ return registry.all().stream().map(this::project).toList(); }
  private String alias(Path f){ return registry.of(f).orElseThrow().alias(); }
  private void publish(){
    var next= new State(projects().stream().filter(p -> !p.alias().isEmpty()).toList(), selected, registry.extensions());
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
    return s -> { l.printed.add(s); post(() -> drain(f, l)); };
  }
  private void drain(Path f, Live l){
    for (var s= l.printed.poll(); s != null; s = l.printed.poll()){ late(f, l, s); }
  }
  private void late(Path f, Live l, String text){
    if (live.get(f) != l){ return; }
    if (!(l.job instanceof Job.Compile)){ output(f, text); return; }
    l.line.append(text);
    for (var nl= l.line.indexOf("\n"); nl >= 0; nl = l.line.indexOf("\n")){
      var line= l.line.substring(0, nl+1);
      l.line.delete(0, nl+1);
      if (line.startsWith(ChildMain.takeFreeExtension)){ answer(l, List.of(line.substring(ChildMain.takeFreeExtension.length()).strip().split(" "))); continue; }
      output(f, line);
    }
  }
  private void flushLine(Path f, Live l){
    if (l.line.isEmpty()){ return; }
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

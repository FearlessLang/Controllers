package controller;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.file.InvalidPathException;
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
    List<String> todo= List.of();
    Optional<Path> input= Optional.empty();
    Registry.Grants grants;
    Map<String,List<String>> attenuators= Map.of();
    Optional<Follow> follow= Optional.empty();
    Set<String> answered= new HashSet<>();
    StringBuilder line= new StringBuilder();
  }
  private static final String conflict= "base.IconsConflict";
  static final List<String> pathVerbs= List.of("Register", "ConnectEclipse", "EditMetadata", "OpenWithOrShortcut");
  private static final Map<String,List<Integer>> verbs= verbs();
  private static Map<String,List<Integer>> verbs(){
    var res= new LinkedHashMap<String,List<Integer>>();
    res.put("Show", List.of(0, 1));
    Stream.of("Quit", "Reset").forEach(v -> res.put(v, List.of(0)));
    Stream.of("Register", "ConnectEclipse", "EditMetadata", "OpenWithOrShortcut", "Check", "Terminate", "DeleteCache", "Forget").forEach(v -> res.put(v, List.of(1)));
    res.put("OpenWithOrShortcut", List.of(1, 3));
    res.put("Run", List.of(2));
    res.put("Compile", List.of(1));
    res.put("Kind", List.of(2));
    res.put("Mount", List.of(2));
    res.put("Attenuate", List.of(2));
    return res;
  }
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
      Runnable r;
      try{ var text= UTF_8.newDecoder().decode(ByteBuffer.wrap(Fs.readAllBytes(file))).toString(); r= () -> apply(text); }
      catch (CharacterCodingException e){ r = () -> view.refuse(Report.unreadableMessage(file)); }
      Fs.deleteIfExists(file);
      post(r);
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
    ClaimIcons.materialise(dir.resolve("icons"), conflict, Fs.of(() -> Fs.readAllBytes(tools.stdLibBase().resolve("icons").resolve("conflict.png"))));
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
    for (var e : registry.all()){
      var l= live.get(e.path());
      if (e.kind() != Kind.code){ l.claims = Project.noClaims; continue; }
      if (l.job.equals(Project.compiling)){ continue; }
      l.claims = cached(e.path());
    }
    return reconcile(compiled, registry.extensions());
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
    output(f, why.stripTrailing()+"\n");
  }
  private void endJob(Path f){
    var l= live.get(f);
    if (l.child == null){ return; }
    l.terminated = true;
    l.child.kill();
  }
  private Optional<String> reconcile(Optional<Path> compiled, List<String> extensions){
    var claimed= Project.claimed(projects());
    var next= captured(claimed, extensions);
    try{ tools.associate(next, preloads().toList(), e -> Report.claimedBy(claimed.getOrDefault(e.substring(1), List.of()))); }
    catch (Refusal r){
      var brought= compiled.stream().flatMap(f -> Project.claimants(f, alias(f), live.get(f).claims)).map(c -> "."+c.claim().extension()).filter(e -> !captured.contains(e)).collect(Collectors.toSet());
      if (r.extensions.isEmpty() || r.extensions.contains(".fearless") || !brought.containsAll(r.extensions)){ throw r.error; }
      var hit= r.extensions.stream().flatMap(e -> claimed.getOrDefault(e.substring(1), List.of()).stream()).map(Project.Claimant::folder).distinct().toList();
      for (var g : hit){
        endJob(g);
        deleted(g);
        errorFile(g, r.error.getMessage());
        output(g, r.error.getMessage().stripTrailing()+"\n");
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
    return all.stream().map(e -> new Icon("."+e, icon(claimed.getOrDefault(e, List.of()), suffix), icon(claimed.getOrDefault(e, List.of()), ".png"))).toList();
  }
  private Path icon(List<Project.Claimant> cs, String suffix){ return cs.size() == 1 ? cs.getFirst().icon(suffix) : dir.resolve("icons").resolve(conflict+suffix); }
  private void apply(String message){
    var lines= List.of(message.split("\n", -1));
    var verb= lines.getFirst();
    var args= lines.subList(1, lines.size());
    if (verb.equals("InvalidCall")){ view.refuse(String.join("\n", args)); return; }
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
  static void wellFormed(String message){
    var lines= List.of(message.split("\n", -1));
    var verb= lines.getFirst();
    var port= lines.size() > 1 && lines.getLast().startsWith("Port:");
    if (port && !Follow.isPort(lines.getLast())){ throw new Refused(Report.badPort(lines.getLast())); }
    var args= lines.subList(1, lines.size()-(port ? 1 : 0));
    var takes= verbs.get(verb);
    if (takes == null){ throw new Refused(Report.unknownVerb(verb, List.copyOf(verbs.keySet()))); }
    if (!takes.contains(args.size())){ throw new Refused(Report.wrongLines(verb, args, takes)); }
    var root= pathVerbs.contains(verb) && path(verb, args.getFirst()).getFileName() == null;
    if (root && verb.equals("Register")){ throw new Refused(Report.projectFolderIsRoot(path(verb, args.getFirst()))); }
    var second= args.size() > 1 ? words(args.get(1)) : List.<String>of();
    if (verb.equals("Kind") && Kind.of(args.get(1)).isEmpty()){ throw new Refused(Report.unknownKind(args.getFirst(), args.get(1))); }
    if (verb.equals("Mount") && (second.isEmpty() || second.size() > 2)){ throw new Refused(Report.malformedMount(args.getFirst(), args.get(1))); }
    if (verb.equals("Attenuate") && second.isEmpty()){ throw new Refused(Report.noMainAttenuated(args.getFirst())); }
    if (verb.equals("Run") && second.isEmpty()){ throw new Refused(Report.noMainNamed(args.getFirst())); }
  }
  private void request(String verb, List<String> args, Optional<Follow> follow){
    if (verb.equals("Show") && args.isEmpty()){ view.show(); return; }
    switch (verb){
      case "Quit" -> exit.quit();
      case "Reset" -> exit.reset();
      case "Register" -> register(path(verb, args.getFirst()));
      case "ConnectEclipse" -> connect(path(verb, args.getFirst()));
      case "EditMetadata" -> editMetadata(path(verb, args.getFirst()));
      case "OpenWithOrShortcut" -> openWith(path(verb, args.getFirst()), args.subList(1, args.size()));
      default -> project(verb, args, follow);
    }
  }
  private static Path path(String verb, String text){
    Path res;
    try{ res= TaggedText.readPath(text, m -> new Refused(Report.notTagged(verb, m))); }
    catch (InvalidPathException e){ throw new Refused(e.getInput().isBlank() ? Report.blankPath(verb) : Report.notAPath(verb, e.getInput(), e.getReason())); }
    if (res.toString().isBlank()){ throw new Refused(Report.blankPath(verb)); }
    return res.normalize();
  }
  private void project(String verb, List<String> args, Optional<Follow> follow){
    var name= args.getFirst();
    var f= registry.named(name).orElseThrow(() -> new Refused(Report.unknownProject(verb, name, registry.all().stream().map(Entry::alias).toList()))).path();
    scan(f);
    switch (verb){
      case "Show" -> { selected = Optional.of(f); view.show(); }
      case "Run" -> run(f, args.get(1), follow, Optional.empty());
      case "Compile" -> compile(f, follow);
      case "Check" -> check(f);
      case "Terminate" -> terminate(f);
      case "DeleteCache" -> deleteCache(f);
      case "Kind" -> kind(f, args.get(1));
      case "Forget" -> forget(f);
      case "Mount" -> mount(f, args.get(1));
      case "Attenuate" -> attenuate(f, args.get(1));
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
  private void run(Path f, String named, Optional<Follow> follow, Optional<Path> input){
    runnable(f, "Run");
    var p= project(f);
    if (p.needsCompiling()){ throw new Refused(Report.notCompiled(p.alias(), p.state().text)); }
    var all= p.knownMains();
    var words= words(named);
    words.stream().filter(w -> !all.contains(w)).findFirst().ifPresent(w -> { throw new Refused(Report.notAMain(p.alias(), w, all)); });
    var mains= all.stream().filter(words::contains).toList();
    var attenuated= p.entry().attenuators();
    attenuated.keySet().stream().filter(m -> !all.contains(m)).findFirst().ifPresent(m -> { throw new Refused(Report.attenuatedNotAMain(p.alias(), m, all)); });
    var attenuators= p.knownAttenuators();
    attenuated.forEach((m, as) -> as.stream().filter(a -> !attenuators.contains(a)).findFirst().ifPresent(a -> { throw new Refused(Report.notAnAttenuator(p.alias(), m, a, attenuators)); }));
    p.entry().mounts().values().stream().flatMap(d -> registry.named(d).stream()).forEach(e -> scan(e.path()));
    var mountProblems= p.mountProblems(projects());
    if (!mountProblems.isEmpty()){ throw new Refused(Report.mountsRefused(p.alias(), mountProblems)); }
    selected = Optional.of(f);
    if (all.size() > 1){ registry.update(f, e -> e.withMains(mains)); }
    var l= live.get(f);
    l.terminated = false;
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
    l.follow = follow;
    l.compiled.setLength(0);
    view.clear(f);
    output(f, "--- compiling "+alias(f)+" ---\n");
    start(f, Project.compiling, () -> tools.compile(f, alias(f), out(f)));
  }
  private void upToDate(Path f, Optional<Follow> follow){
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
    int ec;
    try{ ec= child.await(); }
    catch (InterruptedException e){ throw Bug.of(e); }
    post(() -> exited(f, l, child, ec));
  }
  private void exited(Path f, Live l, ChildJvm child, int ec){
    if (live.get(f) != l || l.child != child){ return; }
    l.child = null;
    if (l.job.equals(Project.compiling)){ compiled(f, l, ec); return; }
    output(f, "--- "+l.job+" exited with "+ec+" after "+Duration.between(l.since, Instant.now()).toSeconds()+"s ---\n");
    if (broken(f)){ l.terminated = true; output(f, Report.filesUpdated(alias(f), l.job)+"\n"); }
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
    var refusedHere= rebuild(Optional.of(f));
    failed |= refusedHere.isPresent();
    if (!failed){ failed = !Project.shortcuts(Project.claimants(f, alias(f), l.claims)).stream().map(f::resolve).filter(p -> Fs.notExists(p)).allMatch(p -> shortcut(f, p)); }
    if (!failed){
      try{ Coordinator.finish(f); }
      catch (UncheckedIOException e){ failed = true; stepFailed(f, Report.cacheNotFinished(Report.fileFailure(e.getCause()))); }
    }
    output(f, "--- compile "+(!failed ? "done" : ec == 0 ? "failed" : "failed with "+ec)+" ---\n");
    scan(f);
    if (l.mains.isPresent()){ forgetStale(f); }
    ended(f);
    if (!refused.isEmpty()){ allow(refused); }
  }
  /// A failing step after Coordinator.compile: the corruption rule for a corrupted cache, else the error in _error.txt and in the Output.
  private void stepFailed(Path f, String why){
    if (UserError.cacheCorrupted(why)){ corrupted(f, why); return; }
    errorFile(f, why);
    output(f, why.stripTrailing()+"\n");
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
  private boolean broken(Path f){
    var facts= facts(f);
    return facts.stale().isPresent() || facts.problem().isPresent();
  }
  private void allow(List<Project.Claimant> refused){
    view.allow(Report.allowExtensions(refused), refused.stream().map(c -> c.claim().extension()).distinct().toList());
  }
  private boolean shortcut(Path f, Path file){
    try{ Fs.writeString(file, MakeDemo.markerContent, StandardOpenOption.CREATE_NEW); return true; }
    catch (IOException e){ stepFailed(f, Report.notWritten(file.getFileName().toString(), e)); return false; }
  }
  private void terminate(Path f){
    selected = Optional.of(f);
    var l= live.get(f);
    if (l.job.equals(Project.compiling)){ throw new Refused(Report.busy("Terminate", alias(f), l.job)); }
    if (l.child == null){ return; }
    l.terminated = true;
    output(f, "--- terminating "+l.job+" ---\n");
    l.child.kill();
  }
  private void forgetStale(Path f){
    var known= project(f).knownMains();
    registry.update(f, e -> e.withMains(e.mains().stream().filter(known::contains).toList()));
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
  private void mount(Path f, String text){
    var words= words(text);
    edit(f, o -> o.withMounts(with(o.mounts(), words.getFirst(), words.stream().skip(1).findFirst())));
  }
  private void attenuate(Path f, String text){
    var words= words(text);
    edit(f, o -> o.withAttenuators(with(o.attenuators(), words.getFirst(), Optional.of(words.subList(1, words.size())).filter(as -> !as.isEmpty()))));
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
  private static List<String> words(String text){ return Stream.of(text.split(" +")).filter(w -> !w.isEmpty()).toList(); }
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
    catch (UncheckedIOException e){ throw new Refused(Report.registerRefused(folder, e.getCause())); }
    registry.add(alias, folder);
    open(registry.of(folder).orElseThrow());
  }
  private void connect(Path chosen){
    Path eclipse;
    try{
      eclipse= Eclipse.find(chosen).orElseThrow(() -> new Refused(Report.noEclipse(chosen)));
      tools.connect(eclipse, dir);
    }
    catch (UncheckedIOException e){ throw new Refused(Report.eclipseNotConnected(chosen, e.getCause())); }
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
      try{ reconcile(Optional.empty(), next.extensions()); }
      catch (Refusal|UserError e){ throw new Refused(e.getMessage()); }
    }
    var old= registry.content();
    registry.save(next);
    live.keySet().stream().filter(f -> registry.of(f).isEmpty()).toList().forEach(this::drop);
    var added= registry.all().stream().filter(e -> !live.containsKey(e.path())).toList();
    added.forEach(e -> { uncached(e.path(), () -> Coordinator.deleteCache(e.path())); open(e); });
    var dropped= old.extensions().stream().filter(x -> !next.extensions().contains(x)).toList();
    old.projects().stream().filter(o -> registry.of(o.path()).filter(e -> e.kind() != o.kind()).isPresent()).forEach(o -> deleted(o.path()));
    live.entrySet().stream().filter(e -> Project.claimants(e.getKey(), alias(e.getKey()), e.getValue().claims).anyMatch(c -> dropped.contains(c.claim().extension()))).map(Map.Entry::getKey).toList().forEach(this::deleted);
    rebuild(Optional.empty());
    registry.all().forEach(e -> scan(e.path()));
  }
  private void kept(Entry o, Content next){
    var job= live.get(o.path()).job;
    var e= OneOr.opt("project "+o.alias(), next.projects().stream().filter(n -> n.path().equals(o.path())));
    if (e.isEmpty()){ throw new Refused(Report.removedWhileBusy(o.alias(), job)); }
    if (e.get().kind() != o.kind()){ throw new Refused(Report.kindWhileBusy(o.alias(), e.get().kind().text, job)); }
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
    if (picked.size() > 1){ view.show(); view.choose(file, cs); return; }
    var c= picked.getFirst();
    scan(c.folder());
    run(c.folder(), c.main(), Optional.empty(), Optional.of(file));
  }
  private void drop(Path f){
    var l= live.remove(f);
    if (l.child != null){ l.terminated = true; l.child.kill(); }
    l.follow.ifPresent(Follow::end);
    if (selected.equals(Optional.of(f))){ selected = Optional.empty(); }
  }
  private void scan(Path f){
    var l= live.get(f);
    if (!l.job.isEmpty()){ inUse(f); return; }
    Facts fresh;
    try{ fresh= facts(f); }
    catch (UserError err){ corrupted(f, err.getMessage()); rebuild(Optional.empty()); fresh= facts(f); }
    if (fresh.problem().isPresent() && Fs.isDirectory(f)){ errorFile(f, fresh.problem().get()); }
    if (fresh.equals(l.scanned)){ return; }
    l.scanned = fresh;
    l.facts = fresh;
    l.mains = fresh.mains();
  }
  private Facts facts(Path f){
    var e= registry.of(f).orElseThrow();
    return Facts.of(f, e.alias(), e.kind(), tools::check);
  }
  private void inUse(Path f){
    var l= live.get(f);
    if (l.job.equals(Project.compiling) || !broken(f)){ return; }
    l.terminated = true;
    l.child.kill();
  }
  private void rotate(){
    if (!view.visible()){ return; }
    selected.ifPresent(this::scan);
    var all= registry.all();
    if (all.isEmpty()){ return; }
    turn += 1;
    scan(all.get(turn % all.size()).path());
  }
  private void granted(Content next){
    for (var e : live.entrySet()){
      var job= e.getValue().job;
      if (job.isEmpty() || job.equals(Project.compiling)){ continue; }
      var mounts= registry.of(e.getKey()).orElseThrow().mounts();
      var changed= OneOr.opt("project "+e.getKey(), next.projects().stream().filter(p -> p.path().equals(e.getKey()))).filter(p -> !p.mounts().equals(mounts) || !Registry.grants(next, p, e.getValue().mains.orElseThrow().dataEdit()).equals(e.getValue().grants));
      changed.ifPresent(p -> { throw new Refused(Report.grantsWhileRunning(p.alias(), job)); });
    }
  }
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
    if (l.child != null){ l.child.send(free.orElse("")); }
  }
}

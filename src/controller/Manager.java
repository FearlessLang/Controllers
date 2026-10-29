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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
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
    ChildJvm compile(Path folder, Consumer<String> out);
    ChildJvm run(Path folder, String main, Optional<Path> input, Registry.Grants grants, List<String> attenuators, Consumer<String> out);
    Optional<MainsInfo> mains(Path folder);
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
  }
  private static final String conflict= "base.IconsConflict";
  private static final Map<String,List<Integer>> verbs= verbs();
  private static Map<String,List<Integer>> verbs(){
    var res= new LinkedHashMap<String,List<Integer>>();
    res.put("Show", List.of(0, 1));
    Stream.of("Quit", "Reset").forEach(v -> res.put(v, List.of(0)));
    Stream.of("Register", "ConnectEclipse", "EditMetadata", "OpenWithOrShortcut", "Check", "Terminate", "DeleteCache", "Forget").forEach(v -> res.put(v, List.of(1)));
    res.put("OpenWithOrShortcut", List.of(1, 3));
    res.put("Run", List.of(1, 2));
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
  private List<?> wanted= List.of();
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
    try{ r.run(); associate(registry.extensions()); publish(); }
    catch (Throwable t){ exit.fail(t); }
  }
  private void load(){
    registry.all().forEach(e -> live.put(e.path(), new Live()));
    registry.all().forEach(e -> live.get(e.path()).claims = read(e.path()));
    registry.all().forEach(e -> { claim(e.path()); scan(e.path()); });
    ClaimIcons.materialise(dir.resolve("icons"), conflict, Fs.of(() -> Fs.readAllBytes(tools.stdLibBase().resolve("icons").resolve("conflict.png"))));
  }
  private void open(Entry e){
    live.put(e.path(), new Live());
    claim(e.path());
    scan(e.path());
  }
  private void claim(Path f){
    var l= live.get(f);
    try{
      var claims= accepted(f);
      l.claims = Project.noClaims;
      if (Project.claimants(f, alias(f), claims).allMatch(this::allowed)){ l.claims = claims; return; }
      uncache(f).ifPresent(w -> output(f, Report.cacheNotDeleted(w)));
    }
    catch (Refused e){ l.claims = Project.noClaims; failed(f, 0, e.getMessage()); }
  }
  private MainsInfo read(Path f){
    if (registry.of(f).orElseThrow().kind() != Kind.code){ return Project.noClaims; }
    try{ return MainsInfo.read(f).orElse(Project.noClaims); }
    catch (UserError|UncheckedIOException e){ return Project.noClaims; }
  }
  private boolean allowed(Project.Claimant c){ return !Registry.isSystem(c.claim().extension()) || registry.extensions().contains(c.claim().extension()); }
  private void associate(List<String> extensions){
    Stream.concat(Project.claimed(projects()).values().stream().flatMap(List::stream), preloads()).filter(c -> !Fs.isRegularFile(c.icon(".png")))
      .gather(DistinctBy.of(Project.Claimant::folder)).toList().forEach(this::iconGone);
    var claimed= Project.claimed(projects());
    var next= captured(claimed, extensions);
    var preloads= preloads().toList();
    List<?> known= List.of(next, preloads, Stream.concat(next.stream().map(Icon::png), preloads.stream().map(c -> c.icon(".png"))).map(Fs::lastModified).toList());
    if (wanted.equals(known)){ return; }
    tools.associate(next, preloads, e -> Report.claimedBy(claimed.getOrDefault(e.substring(1), List.of())));
    wanted = known;
  }
  private Stream<Project.Claimant> preloads(){ return projects().stream().flatMap(p -> Project.preloads(p.folder(), p.alias(), p.claims())); }
  private void iconGone(Project.Claimant c){
    live.get(c.folder()).claims = Project.noClaims;
    output(c.folder(), Report.iconGone(c.icon(".png")));
  }
  private List<Icon> captured(Map<String,List<Project.Claimant>> claimed, List<String> extensions){
    var suffix= Fs.isWindows() ? ".ico" : ".png";
    var all= new TreeSet<>(claimed.keySet());
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
  private void request(String verb, List<String> args, Optional<Follow> follow){
    var takes= verbs.get(verb);
    if (takes == null){ throw new Refused(Report.unknownVerb(verb, List.copyOf(verbs.keySet()))); }
    if (!takes.contains(args.size())){ throw new Refused(Report.wrongLines(verb, args, takes)); }
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
    if (!res.isAbsolute()){ throw new Refused(Report.relativePath(verb, res.toString())); }
    return res.normalize();
  }
  private void project(String verb, List<String> args, Optional<Follow> follow){
    var name= args.getFirst();
    var f= registry.named(name).orElseThrow(() -> new Refused(Report.unknownProject(verb, name, registry.all().stream().map(Entry::alias).toList()))).path();
    scan(f);
    switch (verb){
      case "Show" -> { selected = Optional.of(f); view.show(); }
      case "Run" -> run(f, args.size() > 1 ? args.get(1) : "", follow, Optional.empty());
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
    if (words.isEmpty()){ throw new Refused(Report.noMainNamed(p.alias(), all)); }
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
    if (!project(f).needsCompiling()){ follow.ifPresent(Follow::end); return; }
    var l= live.get(f);
    l.terminated = false;
    l.follow = follow;
    l.compiled.setLength(0);
    view.clear(f);
    output(f, "--- compiling "+alias(f)+" ---\n");
    start(f, Project.compiling, () -> tools.compile(f, out(f)));
  }
  private void check(Path f){
    selected = Optional.of(f);
    var p= project(f);
    view.clear(f);
    output(f, p.problem().or(() -> p.facts().stale()).orElse(Report.checkFine(p.alias())).stripTrailing()+"\n");
    if (Fs.isDirectory(f)){ errorFile(f, "check_error.txt", p.problem()); }
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
      output(f, "--- "+(what.equals(Project.compiling) ? "compile" : what)+" did not start: "+Report.fileFailure(e.getCause())+" ---\n");
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
    if (broken(f)){ l.terminated = true; output(f, uncached(f, Report.filesUpdated(alias(f), l.job))+"\n"); }
    next(f);
  }
  private void compiled(Path f, Live l, int ec){
    var previous= l.claims;
    var refused= List.<Project.Claimant>of();
    Optional<String> failure;
    try{
      if (ec != 0){ throw new Refused(l.compiled.toString()); }
      var claims= accepted(f);
      if (stale(f).isPresent()){ throw new Refused(uncached(f, Report.filesUpdated(alias(f), l.job))); }
      refused = Project.claimants(f, alias(f), claims).filter(c -> !allowed(c)).toList();
      if (!refused.isEmpty()){ throw new Refused(Report.extensionsNotAllowed(refused)); }
      l.claims = claims;
      associate(registry.extensions());
      failure= Optional.empty();
    }
    catch (Refused e){ failure = Optional.of(e.getMessage()); }
    catch (UserError e){ l.claims = previous; failure = Optional.of(e.getMessage()); }
    failure.ifPresentOrElse(why -> failed(f, ec, why), () -> succeeded(f, l));
    output(f, "--- compile "+(failure.isEmpty() ? "done" : ec == 0 ? "failed" : "failed with "+ec)+" ---\n");
    l.job = "";
    scan(f);
    if (l.mains.isPresent()){ forgetStale(f); }
    ended(f);
    if (!refused.isEmpty()){ allow(refused); }
  }
  private MainsInfo accepted(Path f){
    var read= read(f);
    try{
      var icons= ClaimIcons.read(tools.stdLibBase(), Stream.concat(Project.claimants(f, alias(f), read), Project.preloads(f, alias(f), read)), e -> unreadable(f, e));
      var others= live.entrySet().stream().filter(e -> !e.getKey().equals(f)).map(e -> e.getValue().claims);
      var filled= Project.filled(read, live.get(f).claims, alias(f), others);
      Project.shortcuts(Project.claimants(f, alias(f), filled));
      icons.forEach((icon,bytes) -> ClaimIcons.materialise(f.resolve(Facts.outDir).resolve("icons"), icon, bytes));
      if (!filled.equals(read)){ MainsInfo.write(f, filled); }
      return filled;
    }
    catch (UserError e){ throw new Refused(e.getMessage()); }
    catch (UncheckedIOException e){ throw new Refused(Report.claimsNotSaved(e.getCause())); }
  }
  private Refused unreadable(Path f, RuntimeException e){
    var stale= stale(f);
    var job= live.get(f).job;
    if (stale.isPresent() && job.isEmpty()){ return new Refused(stale.get()); }
    return new Refused(uncached(f, stale.isPresent() ? Report.filesUpdated(alias(f), job) : UserError.cacheCorruptedOrCrash(e).getMessage()));
  }
  private String uncached(Path f, String why){
    uncache(f).ifPresent(w -> output(f, Report.cacheNotDeleted(w)));
    live.get(f).claims = Project.noClaims;
    view.show();
    return why;
  }
  private Optional<String> stale(Path f){ return Facts.of(f, alias(f), Kind.code, tools.baseCache()).stale(); }
  private boolean broken(Path f){
    var facts= Facts.of(f, alias(f), Kind.code, tools.baseCache());
    return facts.stale().isPresent() || facts.problem().isPresent();
  }
  private void succeeded(Path f, Live l){
    Project.shortcuts(Project.claimants(f, alias(f), l.claims)).stream().map(f::resolve).filter(p -> Fs.notExists(p)).forEach(p -> shortcut(f, p));
    unclaimedIcons(f, l.claims);
    errorFile(f, "compile_error.txt", Optional.empty());
  }
  private void failed(Path f, int ec, String why){
    if (ec == 0){ output(f, why.stripTrailing()+"\n"); }
    for (var p : MainsInfo.files(f)){
      try{ Fs.deleteIfExists(p); }
      catch (IOException e){ output(f, Report.notWritten(p.getFileName().toString(), e)); }
    }
    errorFile(f, "compile_error.txt", Optional.of(why));
  }
  private void errorFile(Path f, String name, Optional<String> why){
    var file= f.resolve(Facts.outDir).resolve(name);
    try{
      if (why.isEmpty()){ Fs.deleteIfExists(file); return; }
      Fs.createDirectories(file.getParent());
      Fs.writeString(file, why.get());
    }
    catch (IOException e){ output(f, Report.notWritten(name, e)); }
  }
  private void allow(List<Project.Claimant> refused){
    view.allow(Report.allowExtensions(refused), refused.stream().map(c -> c.claim().extension()).distinct().toList());
  }
  private void shortcut(Path f, Path file){
    try{ Fs.writeString(file, MakeDemo.markerContent, StandardOpenOption.CREATE_NEW); }
    catch (IOException e){ output(f, Report.notWritten(file.getFileName().toString(), e)); }
  }
  private void unclaimedIcons(Path f, MainsInfo claims){
    var kept= Stream.concat(Project.claimants(f, alias(f), claims), Project.preloads(f, alias(f), claims)).flatMap(c -> Stream.of(c.icon(".png"), c.icon(Fs.isWindows() ? ".ico" : ".png"))).toList();
    try (var s= Fs.list(f.resolve(Facts.outDir).resolve("icons"))){ for (var p : s.filter(p -> !kept.contains(p)).toList()){ Fs.delete(p); } }
    catch (NoSuchFileException e){}
    catch (IOException e){ output(f, Report.notWritten("icons", e)); }
  }
  private void terminate(Path f){
    selected = Optional.of(f);
    var l= live.get(f);
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
    uncache(f).ifPresent(w -> output(f, Report.cacheNotDeleted(w)));
    live.get(f).claims = Project.noClaims;
    scan(f);
  }
  private static Optional<String> uncache(Path f){
    try{ Fs.rmTree(f.resolve(Facts.outDir)); return Optional.empty(); }
    catch (UncheckedIOException e){ return Optional.of(Report.fileFailure(e.getCause())); }
  }
  private void kind(Path f, String text){
    var kind= Kind.of(text).orElseThrow(() -> new Refused(Report.unknownKind(alias(f), text)));
    edit(f, e -> e.withKind(kind));
  }
  private void forget(Path f){
    var c= registry.content();
    edit(new Content(c.projects().stream().filter(e -> !e.path().equals(f)).toList(), c.extensions()));
  }
  private void mount(Path f, String text){
    var words= words(text);
    if (words.isEmpty() || words.size() > 2){ throw new Refused(Report.malformedMount(alias(f), text)); }
    edit(f, o -> o.withMounts(with(o.mounts(), words.getFirst(), words.stream().skip(1).findFirst())));
  }
  private void attenuate(Path f, String text){
    var words= words(text);
    if (words.isEmpty()){ throw new Refused(Report.noMainAttenuated(alias(f))); }
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
    view.show();
  }
  private void add(Path folder){
    registry.overlapping(folder).ifPresent(o -> { throw new Refused(Report.folderNestedWithRegistered(folder, o)); });
    String alias;
    try{
      Fs.rmTree(folder.resolve(Facts.outDir));
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
    if (!registry.extensions().containsAll(next.extensions())){
      try{ associate(next.extensions()); }
      catch (UserError e){ throw new Refused(e.getMessage()); }
    }
    var old= registry.content();
    registry.replace(next);
    live.keySet().stream().filter(f -> registry.of(f).isEmpty()).toList().forEach(this::drop);
    registry.all().stream().filter(e -> !live.containsKey(e.path())).forEach(this::open);
    var dropped= old.extensions().stream().filter(x -> !next.extensions().contains(x)).toList();
    old.projects().stream().filter(o -> registry.of(o.path()).filter(e -> e.kind() != o.kind()).isPresent()).forEach(o -> claim(o.path()));
    live.entrySet().stream().filter(e -> Project.claimants(e.getKey(), alias(e.getKey()), e.getValue().claims).anyMatch(c -> dropped.contains(c.claim().extension()))).map(Map.Entry::getKey).toList().forEach(this::claim);
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
    var e= registry.of(f).orElseThrow();
    var fresh= Facts.of(f, e.alias(), e.kind(), tools.baseCache());
    if (fresh.equals(l.scanned)){ return; }
    l.scanned = fresh;
    l.facts = fresh;
    l.mains = Optional.empty();
    if (e.kind() != Kind.code || fresh.stale().isPresent() || fresh.problem().isPresent()){ return; }
    Optional<String> error= Optional.empty();
    try{ l.mains = tools.mains(f); }
    catch (UserError err){ error = Optional.of(err.getMessage()); }
    catch (UncheckedIOException err){ error = Optional.of(Report.fileFailure(err.getCause())); }
    if (l.mains.isEmpty()){ l.facts = fresh.outOfDate(error.orElse(Report.packagesToCompile())); }
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
    if (l.job.equals(Project.compiling)){ l.compiled.append(text); }
    output(f, text);
  }
}

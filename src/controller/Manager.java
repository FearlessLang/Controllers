package controller;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
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
    void choose(List<Project.Claimant> choices);
    void allow(String question, String content);
    void edited(Path file);
    void taken(List<String> programs);
  }
  public interface Tools{
    ChildJvm compile(Path folder, Consumer<String> out);
    ChildJvm run(Path folder, String main, Consumer<String> out);
    Optional<Map<String,String>> mains(Path folder);
    Path stdLibBase();
    void associate(List<Icon> captured, Function<String,String> claimedBy);
    List<String> others();
    void connect(Path eclipse, Path managerDir);
  }
  public interface Exit{
    void quit();
    void reset();
    void fail(Throwable problem);
  }
  public record State(List<Project> projects, Optional<Path> selected, List<String> extensions){
    public Optional<Project> of(Path folder){ return OneOr.opt("project "+folder,projects.stream().filter(p->p.folder().equals(folder))); }
    public Optional<Project> shown(){ return selected.flatMap(this::of); }
    public List<String> running(){ return projects.stream().filter(Project::busy).map(p->p.alias()+" - "+p.job()).toList(); }
  }
  private static final class Live{
    Facts scanned;
    Facts facts;
    Optional<Map<String,String>> mains= Optional.empty();
    MainsInfo claims= Project.noClaims;
    String job= "";
    Instant since= Instant.EPOCH;
    StringBuilder compiled= new StringBuilder();
    ChildJvm child;
    boolean terminated;
    List<String> todo= List.of();
    Optional<Follow> follow= Optional.empty();
  }
  private static final String conflict= "base.IconsConflict";
  private static final Map<String,List<Integer>> verbs= verbs();
  private static Map<String,List<Integer>> verbs(){
    var res= new LinkedHashMap<String,List<Integer>>();
    res.put("Show",List.of(0,1));
    Stream.of("Quit","Reset").forEach(v->res.put(v,List.of(0)));
    Stream.of("Register","ConnectEclipse","EditMetadata","OpenWithOrShortcut","Check","Terminate","DeleteCache","Forget").forEach(v->res.put(v,List.of(1)));
    res.put("Run",List.of(1,2,3));
    res.put("Compile",List.of(1,2));
    res.put("Kind",List.of(2));
    res.put("Link",List.of(2));
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
  private boolean taken;
  private int turn;
  private volatile State state= new State(List.of(),Optional.empty(),List.of());
  public Manager(Path dir, Tools tools, View view, Exit exit){
    this.dir= dir;
    this.tools= tools;
    this.view= view;
    this.exit= exit;
    registry= new Registry(dir);
    post(this::load);
  }
  public State state(){ return state; }
  public void start(){
    core.scheduleWithFixedDelay(()->step(this::rotate),3,3,TimeUnit.SECONDS);
    core.scheduleWithFixedDelay(()->step(this::watchAssociations),10,10,TimeUnit.SECONDS);
  }
  public void message(String text){ post(()->apply(text)); }
  void stop(){
    try{ core.submit(()->live.values().stream().filter(l->l.child != null).forEach(l->l.child.kill())).get(1,TimeUnit.MINUTES); }
    catch(InterruptedException|ExecutionException|TimeoutException e){ throw Bug.of(e); }
    core.shutdownNow();
  }
  //Runs once before the watcher thread starts, then only from that thread: never concurrently.
  public void drain(){
    var msgDir= dir.resolve("messages");
    try{ Fs.ofV(()->take(msgDir)); }
    catch(UncheckedIOException e){ throw Violation.couldNotDrainMessageFolder(msgDir,e.getCause()); }
  }
  private void take(Path msgDir) throws IOException{
    for(var file: Names.list(msgDir).stream().filter(f->f.toString().endsWith(".msg")).toList()){
      Runnable r;
      try{ var text= UTF_8.newDecoder().decode(ByteBuffer.wrap(Files.readAllBytes(file))).toString(); r= ()->apply(text); }
      catch(CharacterCodingException e){ r= ()->view.refuse(Report.unreadableMessage(file)); }
      Files.deleteIfExists(file);
      post(r);
    }
    var old= Instant.now().minusSeconds(60);
    for(var file: Names.list(msgDir).stream().filter(f->f.toString().endsWith(".tmp")).toList()){
      try{ if (Files.getLastModifiedTime(file).toInstant().isBefore(old)){ Files.deleteIfExists(file); } }
      catch(NoSuchFileException e){}
    }
  }
  void settle(){
    try{ core.submit(()->{}).get(1,TimeUnit.MINUTES); }
    catch(InterruptedException|ExecutionException|TimeoutException e){ throw Bug.of(e); }
  }
  private void post(Runnable r){ core.execute(()->step(r)); }
  private void step(Runnable r){
    try{ r.run(); associate(registry.extensions()); publish(); }
    catch(Throwable t){ exit.fail(t); }
  }
  private void load(){
    registry.all().forEach(e->live.put(e.path(),new Live()));
    registry.all().forEach(e->live.get(e.path()).claims= read(e.path()));
    registry.all().forEach(e->{ claim(e.path()); scan(e.path()); });
    ClaimIcons.materialise(dir.resolve("icons"),conflict,Fs.of(()->Files.readAllBytes(tools.stdLibBase().resolve("icons").resolve("conflict.png"))));
  }
  private void open(Entry e){
    live.put(e.path(),new Live());
    claim(e.path());
    scan(e.path());
  }
  private void claim(Path f){
    var l= live.get(f);
    try{
      var claims= accepted(f);
      l.claims= Project.noClaims;
      if (Project.claimants(f,alias(f),claims).allMatch(this::allowed)){ l.claims= claims; return; }
      uncache(f).ifPresent(w->output(f,Report.cacheNotDeleted(w)));
    }
    catch(Refused e){ l.claims= Project.noClaims; failed(f,0,e.getMessage()); }
  }
  private MainsInfo read(Path f){
    if (registry.of(f).orElseThrow().kind() != Kind.code){ return Project.noClaims; }
    try{ return MainsInfo.read(f).orElse(Project.noClaims); }
    catch(UserError|UncheckedIOException e){ return Project.noClaims; }
  }
  private boolean allowed(Project.Claimant c){ return !Registry.isSystem(c.claim().extension()) || registry.extensions().contains(c.claim().extension()); }
  private void associate(List<String> extensions){
    if (taken){ return; }
    Project.claimed(projects()).values().stream().flatMap(List::stream).filter(c->!Files.isRegularFile(c.icon(".png")))
      .gather(DistinctBy.of(Project.Claimant::folder)).toList().forEach(this::iconGone);
    var claimed= Project.claimed(projects());
    var next= captured(claimed,extensions);
    List<?> known= List.of(next,next.stream().map(i->Fs.lastModified(i.png())).toList());
    if (wanted.equals(known)){ return; }
    tools.associate(next,e->Report.claimedBy(claimed.getOrDefault(e.substring(1),List.of())));
    wanted= known;
  }
  private void iconGone(Project.Claimant c){
    live.get(c.folder()).claims= Project.noClaims;
    output(c.folder(),Report.iconGone(c.icon(".png")));
  }
  private List<Icon> captured(Map<String,List<Project.Claimant>> claimed, List<String> extensions){
    var suffix= Fs.isWindows() ? ".ico" : ".png";
    var all= new TreeSet<>(claimed.keySet());
    all.addAll(extensions);
    return all.stream().map(e->new Icon("."+e,icon(claimed.getOrDefault(e,List.of()),suffix),icon(claimed.getOrDefault(e,List.of()),".png"))).toList();
  }
  private Path icon(List<Project.Claimant> cs, String suffix){ return cs.size() == 1 ? cs.getFirst().icon(suffix) : dir.resolve("icons").resolve(conflict+suffix); }
  private void watchAssociations(){
    if (taken){ return; }
    var others= tools.others();
    if (!others.isEmpty()){ taken= true; view.taken(others); return; }
    wanted= List.of();
    associate(registry.extensions());
  }
  private void apply(String message){
    var lines= List.of(message.split("\n",-1));
    var verb= lines.getFirst();
    var args= lines.subList(1,lines.size());
    if (verb.equals("InvalidCall")){ view.refuse(String.join("\n",args)); return; }
    if (verb.equals("FatalError")){ throw Violation.fatalFromRun(String.join("\n",args)); }
    try{ request(verb,args); }
    catch(Refused r){ view.refuse(r.getMessage()); }
  }
  private void request(String verb, List<String> args){
    var takes= verbs.get(verb);
    if (takes == null){ throw new Refused(Report.unknownVerb(verb,List.copyOf(verbs.keySet()))); }
    if (!takes.contains(args.size())){ throw new Refused(Report.wrongLines(verb,args,takes)); }
    if (verb.equals("Show") && args.isEmpty()){ view.show(); return; }
    switch(verb){
      case "Quit" -> exit.quit();
      case "Reset" -> exit.reset();
      case "Register" -> register(path(verb,args.getFirst()));
      case "ConnectEclipse" -> connect(path(verb,args.getFirst()));
      case "EditMetadata" -> editMetadata(path(verb,args.getFirst()));
      case "OpenWithOrShortcut" -> openWith(path(verb,args.getFirst()));
      default -> project(verb,args);
    }
  }
  private static Path path(String verb, String text){
    var given= TaggedText.read(text,m->new Refused(Report.notTagged(verb,m)));
    if (given.isBlank()){ throw new Refused(Report.blankPath(verb)); }
    try{ return Path.of(given).toAbsolutePath().normalize(); }
    catch(InvalidPathException e){ throw new Refused(Report.notAPath(verb,given,e.getReason())); }
  }
  private void project(String verb, List<String> args){
    var name= args.getFirst();
    var f= registry.named(name).orElseThrow(()->new Refused(Report.unknownProject(verb,name,registry.all().stream().map(Entry::alias).toList()))).path();
    scan(f);
    switch(verb){
      case "Show" -> { selected= Optional.of(f); view.show(); }
      case "Run" -> followed(args.size() > 2 ? args.get(2) : "",fl->run(f,args.size() > 1 ? args.get(1) : "",fl));
      case "Compile" -> followed(args.size() > 1 ? args.get(1) : "",fl->compile(f,fl));
      case "Check" -> check(f);
      case "Terminate" -> terminate(f);
      case "DeleteCache" -> deleteCache(f);
      case "Kind" -> kind(f,args.get(1));
      case "Forget" -> forget(f);
      case "Link" -> link(f,args.get(1));
      default -> throw Bug.unreachable();
    }
  }
  private static void followed(String port, Consumer<Optional<Follow>> job){
    var follow= port.isEmpty() ? Optional.<Follow>empty() : Optional.of(Follow.to(port));
    try{ job.accept(follow); }
    catch(Refused r){ follow.ifPresent(Follow::end); throw r; }
  }
  private void idle(Path f, String verb){
    var job= live.get(f).job;
    if (!job.isEmpty()){ throw new Refused(Report.busy(verb,alias(f),job)); }
  }
  private void runnable(Path f, String verb){
    idle(f,verb);
    var p= project(f);
    if (p.kind() != Kind.code){ throw new Refused(Report.notCode(verb,p.alias(),p.kind().text)); }
    p.problem().ifPresent(why->{ throw new Refused(Report.invalidContent(verb,p.alias(),why)); });
  }
  private void run(Path f, String named, Optional<Follow> follow){
    runnable(f,"Run");
    var p= project(f);
    if (p.needsCompiling()){ throw new Refused(Report.notCompiled(p.alias(),p.state().text)); }
    var all= p.knownMains();
    var words= words(named);
    if (words.isEmpty()){ throw new Refused(Report.noMainNamed(p.alias(),all)); }
    words.stream().filter(w->!all.contains(w)).findFirst().ifPresent(w->{ throw new Refused(Report.notAMain(p.alias(),w,all)); });
    var mains= all.stream().filter(words::contains).toList();
    selected= Optional.of(f);
    if (all.size() > 1){ registry.update(f,e->e.withMains(mains)); }
    var l= live.get(f);
    l.terminated= false;
    l.todo= mains;
    l.follow= follow;
    view.clear(f);
    next(f);
  }
  private void compile(Path f, Optional<Follow> follow){
    runnable(f,"Compile");
    selected= Optional.of(f);
    if (!project(f).needsCompiling()){ follow.ifPresent(Follow::end); return; }
    var l= live.get(f);
    l.terminated= false;
    l.follow= follow;
    l.compiled.setLength(0);
    view.clear(f);
    output(f,"--- compiling "+alias(f)+" ---\n");
    start(f,Project.compiling,()->tools.compile(f,out(f)));
  }
  private void check(Path f){
    selected= Optional.of(f);
    var p= project(f);
    view.clear(f);
    output(f,p.problem().or(()->p.facts().stale()).orElse(Report.checkFine(p.alias())).stripTrailing()+"\n");
    if (Files.isDirectory(f)){ errorFile(f,"check_error.txt",p.problem()); }
  }
  private void next(Path f){
    var l= live.get(f);
    if (l.terminated || l.todo.isEmpty()){ ended(f); return; }
    var main= l.todo.getFirst();
    l.todo= l.todo.subList(1,l.todo.size());
    output(f,"--- running "+main+" ---\n");
    start(f,main,()->tools.run(f,main,out(f)));
  }
  private void ended(Path f){
    var l= live.get(f);
    l.job= "";
    l.follow.ifPresent(Follow::end);
    l.follow= Optional.empty();
    scan(f);
  }
  private void start(Path f, String what, Supplier<ChildJvm> jvm){
    var l= live.get(f);
    try{ l.child= jvm.get(); }
    catch(UncheckedIOException e){
      output(f,"--- "+(what.equals(Project.compiling) ? "compile" : what)+" did not start: "+Report.fileFailure(e.getCause())+" ---\n");
      ended(f);
      return;
    }
    l.job= what;
    l.since= Instant.now();
    var child= l.child;
    Thread.startVirtualThread(()->await(f,l,child));
  }
  private void await(Path f, Live l, ChildJvm child){
    int ec;
    try{ ec= child.await(); }
    catch(InterruptedException e){ throw Bug.of(e); }
    post(()->exited(f,l,child,ec));
  }
  private void exited(Path f, Live l, ChildJvm child, int ec){
    if (live.get(f) != l || l.child != child){ return; }
    l.child= null;
    if (l.job.equals(Project.compiling)){ compiled(f,l,ec); return; }
    output(f,"--- "+l.job+" exited with "+ec+" after "+Duration.between(l.since,Instant.now()).toSeconds()+"s ---\n");
    next(f);
  }
  private void compiled(Path f, Live l, int ec){
    var previous= l.claims;
    var refused= List.<Project.Claimant>of();
    Optional<String> failure;
    try{
      if (ec != 0){ throw new Refused(l.compiled.toString()); }
      var claims= accepted(f);
      refused= Project.claimants(f,alias(f),claims).filter(c->!allowed(c)).toList();
      if (!refused.isEmpty()){ throw new Refused(Report.extensionsNotAllowed(refused)); }
      l.claims= claims;
      associate(registry.extensions());
      failure= Optional.empty();
    }
    catch(Refused e){ failure= Optional.of(e.getMessage()); }
    catch(UserError e){ l.claims= previous; failure= Optional.of(e.getMessage()); }
    failure.ifPresentOrElse(why->failed(f,ec,why),()->succeeded(f,l));
    output(f,"--- compile "+(failure.isEmpty() ? "done" : ec == 0 ? "failed" : "failed with "+ec)+" ---\n");
    l.job= "";
    scan(f);
    if (l.mains.isPresent()){ forgetStale(f); }
    ended(f);
    if (!refused.isEmpty()){ allow(refused); }
  }
  private MainsInfo accepted(Path f){
    var read= read(f);
    try{
      var icons= ClaimIcons.read(tools.stdLibBase(),Project.claimants(f,alias(f),read));
      var others= live.entrySet().stream().filter(e->!e.getKey().equals(f)).map(e->e.getValue().claims);
      var filled= Project.filled(read,live.get(f).claims,alias(f),others);
      Project.shortcuts(Project.claimants(f,alias(f),filled));
      icons.forEach((icon,bytes)->ClaimIcons.materialise(f.resolve(Facts.outDir).resolve("icons"),icon,bytes));
      if (!filled.equals(read)){ Fs.writeUtf8(f.resolve(Facts.outDir).resolve("mains.info"),filled.print()); }
      return filled;
    }
    catch(UserError e){ throw new Refused(e.getMessage()); }
    catch(UncheckedIOException e){ throw new Refused(Report.claimsNotSaved(e.getCause())); }
  }
  private void succeeded(Path f, Live l){
    Project.shortcuts(Project.claimants(f,alias(f),l.claims)).stream().map(f::resolve).filter(p->Files.notExists(p,LinkOption.NOFOLLOW_LINKS)).forEach(p->shortcut(f,p));
    unclaimedIcons(f,l.claims);
    errorFile(f,"compile_error.txt",Optional.empty());
  }
  private void failed(Path f, int ec, String why){
    if (ec == 0){ output(f,why.stripTrailing()+"\n"); }
    try{ Files.deleteIfExists(f.resolve(Facts.outDir).resolve("mains.info")); }
    catch(IOException e){ output(f,Report.notWritten("mains.info",e)); }
    errorFile(f,"compile_error.txt",Optional.of(why));
  }
  private void errorFile(Path f, String name, Optional<String> why){
    var file= f.resolve(Facts.outDir).resolve(name);
    try{
      if (why.isEmpty()){ Files.deleteIfExists(file); return; }
      Files.createDirectories(file.getParent());
      Files.writeString(file,why.get());
    }
    catch(IOException e){ output(f,Report.notWritten(name,e)); }
  }
  private void allow(List<Project.Claimant> refused){
    var extensions= new TreeSet<>(registry.extensions());
    refused.forEach(c->extensions.add(c.claim().extension()));
    view.allow(Report.allowExtensions(refused),Registry.text(new Content(registry.all(),List.copyOf(extensions))));
  }
  private void shortcut(Path f, Path file){
    try{ Files.writeString(file,MakeDemo.markerContent,StandardOpenOption.CREATE_NEW); }
    catch(IOException e){ output(f,Report.notWritten(file.getFileName().toString(),e)); }
  }
  private void unclaimedIcons(Path f, MainsInfo claims){
    var kept= Project.claimants(f,alias(f),claims).flatMap(c->Stream.of(c.icon(".png"),c.icon(Fs.isWindows() ? ".ico" : ".png"))).toList();
    try(var s= Files.list(f.resolve(Facts.outDir).resolve("icons"))){ for (var p: s.filter(p->!kept.contains(p)).toList()){ Files.delete(p); } }
    catch(NoSuchFileException e){}
    catch(IOException e){ output(f,Report.notWritten("icons",e)); }
  }
  private void terminate(Path f){
    selected= Optional.of(f);
    var l= live.get(f);
    if (l.child == null){ return; }
    l.terminated= true;
    output(f,"--- terminating "+l.job+" ---\n");
    l.child.kill();
  }
  private void forgetStale(Path f){
    var known= project(f).knownMains();
    registry.update(f,e->e.withMains(e.mains().stream().filter(known::contains).toList()));
  }
  private void deleteCache(Path f){
    idle(f,"DeleteCache");
    selected= Optional.of(f);
    uncache(f).ifPresent(w->output(f,Report.cacheNotDeleted(w)));
    live.get(f).claims= Project.noClaims;
    scan(f);
  }
  private static Optional<String> uncache(Path f){
    try{ Fs.rmTree(f.resolve(Facts.outDir)); return Optional.empty(); }
    catch(UncheckedIOException e){ return Optional.of(Report.fileFailure(e.getCause())); }
  }
  private void kind(Path f, String text){
    var kind= Kind.of(text).orElseThrow(()->new Refused(Report.unknownKind(alias(f),text)));
    idle(f,"Kind");
    var from= registry.of(f).orElseThrow().kind();
    if (from != Kind.idle && kind != Kind.idle){ throw new Refused(Report.kindChange(alias(f),from.text,kind.text)); }
    registry.update(f,e->e.withKind(kind));
    claim(f);
    scan(f);
  }
  private void forget(Path f){
    idle(f,"Forget");
    drop(f);
    registry.remove(f);
  }
  private void link(Path f, String text){
    var e= registry.of(f).orElseThrow();
    var words= words(text);
    var wellFormed= words.size() >= 2 && List.of("read","write").contains(words.get(1));
    if (!wellFormed){ throw new Refused(Report.malformedLink(e.alias(),text)); }
    if (e.kind() != Kind.code){ throw new Refused(Report.notCode("Link",e.alias(),e.kind().text)); }
    var data= words.get(0);
    var names= words.subList(2,words.size());
    registry.update(f,o->words.get(1).equals("write") ? o.withLinks(o.reads(),with(o.edits(),data,names)) : o.withLinks(with(o.reads(),data,names),o.edits()));
  }
  private static Map<String,List<String>> with(Map<String,List<String>> map, String key, List<String> names){
    var out= new LinkedHashMap<>(map);
    if (names.isEmpty()){ out.remove(key); } else { out.put(key,names); }
    return out;
  }
  private static List<String> words(String text){ return Stream.of(text.split(" +")).filter(w->!w.isEmpty()).toList(); }
  static Path projectFolder(Path path, Path managerDir){
    if (!Files.exists(path)){ throw new Refused(Report.registerNothing(path)); }
    return Registry.placed(Registry.real(Files.isDirectory(path) ? path : path.getParent()),managerDir,Refused::new);
  }
  private void register(Path path){
    var folder= projectFolder(path,dir);
    if (!live.containsKey(folder)){ add(folder); }
    selected= Optional.of(folder);
    scan(folder);
    view.show();
  }
  private void add(Path folder){
    registry.overlapping(folder).ifPresent(o->{ throw new Refused(Report.folderNestedWithRegistered(folder,o)); });
    String alias;
    try{
      Fs.rmTree(folder.resolve(Facts.outDir));
      alias= Names.makeUnique(folder,registry.all().stream().map(Entry::alias).collect(Collectors.toSet()));
    }
    catch(UncheckedIOException e){ throw new Refused(Report.registerRefused(folder,e.getCause())); }
    registry.add(alias,folder);
    open(registry.of(folder).orElseThrow());
  }
  private void connect(Path chosen){
    Path eclipse;
    try{
      eclipse= Eclipse.find(chosen).orElseThrow(()->new Refused(Report.noEclipse(chosen)));
      tools.connect(eclipse,dir);
    }
    catch(UncheckedIOException e){ throw new Refused(Report.eclipseNotConnected(chosen,e.getCause())); }
    view.tell(Report.eclipseConnected(eclipse));
  }
  private void editMetadata(Path file){
    if (!Files.isRegularFile(file)){ throw new Refused(Report.metadataNoFile(file)); }
    Content next;
    try{
      var text= UTF_8.newDecoder().decode(ByteBuffer.wrap(Files.readAllBytes(file))).toString();
      next= Registry.parse(text,file,Registry::real,dir);
    }
    catch(CharacterCodingException e){ throw new Refused(Report.metadataNotUtf8(file)); }
    catch(IOException e){ throw new Refused(Report.metadataUnreadable(file,e)); }
    catch(UserError e){ throw new Refused(e.getMessage()); }
    if (!registry.extensions().containsAll(next.extensions())){
      try{ associate(next.extensions()); }
      catch(UserError e){ throw new Refused(e.getMessage()); }
    }
    var old= registry.content();
    live.keySet().stream().filter(f->next.projects().stream().noneMatch(e->e.path().equals(f))).toList().forEach(this::drop);
    registry.replace(next);
    registry.all().stream().filter(e->!live.containsKey(e.path())).forEach(this::open);
    var dropped= old.extensions().stream().filter(x->!next.extensions().contains(x)).toList();
    old.projects().stream().filter(o->registry.of(o.path()).filter(e->e.kind() != o.kind()).isPresent()).forEach(o->claim(o.path()));
    live.entrySet().stream().filter(e->Project.claimants(e.getKey(),alias(e.getKey()),e.getValue().claims).anyMatch(c->dropped.contains(c.claim().extension()))).map(Map.Entry::getKey).toList().forEach(this::claim);
    registry.all().forEach(e->scan(e.path()));
    view.edited(file);
  }
  private void openWith(Path file){
    if (!Files.isRegularFile(file)){ throw new Refused(Report.openNoFile(file)); }
    if (Fs.of(()->Files.size(file)) == 0){ throw new Refused(Report.emptyFile(file)); }
    var name= file.getFileName().toString();
    var dot= name.lastIndexOf('.');
    var ext= dot < 0 ? "" : name.substring(dot+1).toLowerCase(Locale.ROOT);
    var cs= Project.claimed(projects()).getOrDefault(ext,List.of());
    if (cs.isEmpty()){ throw new Refused(Report.noMainOpens(file,ext)); }
    if (cs.size() == 1){ project("Run",List.of(cs.getFirst().alias(),cs.getFirst().main())); return; }
    view.show();
    view.choose(cs);
  }
  private void drop(Path f){
    var l= live.remove(f);
    if (l.child != null){ l.terminated= true; l.child.kill(); }
    l.follow.ifPresent(Follow::end);
    if (selected.equals(Optional.of(f))){ selected= Optional.empty(); }
  }
  private void scan(Path f){
    var l= live.get(f);
    if (!l.job.isEmpty()){ return; }
    var e= registry.of(f).orElseThrow();
    var fresh= Facts.of(f,e.alias(),e.kind());
    if (fresh.equals(l.scanned)){ return; }
    l.scanned= fresh;
    l.facts= fresh;
    l.mains= Optional.empty();
    if (e.kind() != Kind.code || fresh.stale().isPresent()){ return; }
    Optional<String> error= Optional.empty();
    try{ l.mains= tools.mains(f); }
    catch(UserError err){ error= Optional.of(err.getMessage()); }
    catch(UncheckedIOException err){ error= Optional.of(Report.fileFailure(err.getCause())); }
    if (!Facts.of(f,e.alias(),e.kind()).equals(fresh)){ scan(f); return; }
    if (l.mains.isEmpty()){ l.facts= fresh.outOfDate(error.orElse(Report.packagesToCompile())); }
  }
  private void rotate(){
    if (!view.visible()){ return; }
    selected.ifPresent(this::scan);
    var all= registry.all();
    if (all.isEmpty()){ return; }
    turn+= 1;
    scan(all.get(turn % all.size()).path());
  }
  private Project project(Entry e){
    var l= live.get(e.path());
    return new Project(e,l.facts,l.mains,l.claims,registry.linkProblem(e,f->live.get(f).facts.problem()),l.job);
  }
  private Project project(Path f){ return project(registry.of(f).orElseThrow()); }
  private List<Project> projects(){ return registry.all().stream().map(this::project).toList(); }
  private String alias(Path f){ return registry.of(f).orElseThrow().alias(); }
  private void publish(){
    var next= new State(projects(),selected,registry.extensions());
    if (next.equals(state)){ return; }
    state= next;
    view.state(next);
  }
  private void output(Path f, String text){
    view.output(f,text);
    live.get(f).follow.ifPresent(fl->fl.send(text));
  }
  private Consumer<String> out(Path f){
    var l= live.get(f);
    return s->post(()->late(f,l,s));
  }
  private void late(Path f, Live l, String text){
    if (live.get(f) != l){ return; }
    if (l.job.equals(Project.compiling)){ l.compiled.append(text); }
    output(f,text);
  }
}

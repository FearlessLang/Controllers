package controller;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import controller.Registry.Entry;
import controller.Registry.Kind;
import coordinator.MainsInfo;
import mainCoordinator.MakeDemo;
import tools.ChildJvm;
import tools.Fs;
import userMessages.UserError;
import utils.Bug;
import utils.OneOr;

/// The manager without its window: the registered projects, their jobs, and every request
/// made of them, by a message or by the window. Everything runs on one thread, one step at a
/// time; after each step the Eclipse files and the View are brought up to date with the new State.
public final class Manager{
  public interface View{
    void show();
    void state(State s);
    void output(Path folder, String text);
    void note(String text);
    void clear(Path folder);
    boolean visible();
  }
  public interface Tools{
    ChildJvm compile(Path folder, Consumer<String> out);
    ChildJvm run(Path folder, String main, Consumer<String> out);
    Optional<Map<String,String>> mains(Path folder);
  }
  public record State(List<Project> projects, Optional<Path> selected){
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
    int runs;
    String lastRun= "";
    int exit= -1;
    StringBuilder compiled= new StringBuilder();
    String failure= "";
    ChildJvm child;
    boolean terminated;
    Supplier<List<String>> then;
    List<String> todo= List.of();
    ScheduledFuture<?> reporting;
  }
  private final ScheduledExecutorService core= Executors.newSingleThreadScheduledExecutor();
  public final Path dir;
  public final Eclipse eclipse;
  private final Tools tools;
  private final View view;
  private final Consumer<Throwable> fail;
  private final Registry registry;
  private final Map<Path,Live> live= new HashMap<>();
  private Optional<Path> selected= Optional.empty();
  private int turn;
  private volatile State state= new State(List.of(),Optional.empty());
  public Manager(Path dir, Tools tools, View view, Consumer<Throwable> fail){
    this.dir= dir;
    this.tools= tools;
    this.view= view;
    this.fail= fail;
    eclipse= new Eclipse(dir.resolve("eclipse"));
    registry= new Registry(dir);
    post(this::load);
  }
  public State state(){ return state; }
  public void start(){ core.scheduleWithFixedDelay(()->step(this::rotate),3,3,TimeUnit.SECONDS); }
  public void message(String text){ post(()->apply(text)); }
  void stop(){
    core.shutdownNow();
    try{ var done= core.awaitTermination(1,TimeUnit.MINUTES); assert done; }
    catch(InterruptedException e){ throw Bug.of(e); }
  }
  //Runs once before the watcher thread starts, then only from that thread: never concurrently.
  public void drain(){
    var msgDir= dir.resolve("messages");
    try{ Fs.ofV(()->take(msgDir)); }
    catch(UncheckedIOException e){ throw Messages.couldNotDrainMessageFolder(msgDir,e.getCause()); }
  }
  private void take(Path msgDir) throws IOException{
    for(var file: Names.list(msgDir).stream().filter(f->f.toString().endsWith(".msg")).toList()){
      Runnable r;
      try{ var text= UTF_8.newDecoder().decode(ByteBuffer.wrap(Files.readAllBytes(file))).toString(); r= ()->apply(text); }
      catch(CharacterCodingException e){ r= ()->tell(Messages.unreadableMessage(file)); }
      Files.deleteIfExists(file);
      post(r);
    }
    var old= Instant.now().minusSeconds(60);
    for(var file: Names.list(msgDir).stream().filter(f->f.toString().endsWith(".tmp")).toList()){
      try{ if (Files.getLastModifiedTime(file).toInstant().isBefore(old)){ Files.deleteIfExists(file); } }
      catch(NoSuchFileException e){}
    }
  }
  public void ask(String verb, String name, String arg){ post(()->request(verb,name,arg)); }
  public void commit(String base, String text, Runnable done){ post(()->commitNow(base,text,done)); }
  public void connect(Path chosen){ post(()->connectNow(chosen)); }
  public void refuse(String text){ post(()->tell(text)); }
  void settle(){
    try{ core.submit(()->{}).get(1,TimeUnit.MINUTES); }
    catch(InterruptedException|ExecutionException|TimeoutException e){ throw Bug.of(e); }
  }
  private void post(Runnable r){ core.execute(()->step(r)); }
  private void step(Runnable r){
    try{ r.run(); publish(); }
    catch(Throwable t){ fail.accept(t); }
  }
  private void load(){
    Fs.writeUtf8(eclipse.notes(),"");
    registry.all().forEach(e->live.put(e.path(),new Live()));
    live.forEach((f,l)->l.claims= read(f));
    registry.all().forEach(this::open);
    registry.reset.forEach(e->{
      var kept= uncache(e.path());
      scan(e.path());
      tell(Messages.kindReset(e.alias(),kept));
    });
    eclipse.publish(Eclipse.state(registry.all().stream().map(this::project).toList()));
  }
  private void open(Entry e){
    live.put(e.path(),new Live());
    Fs.writeUtf8(eclipse.console(e.alias()),"");
    accept(e.path());
    scan(e.path());
  }
  private MainsInfo read(Path f){
    if (registry.of(f).orElseThrow().kind() != Kind.code){ return Project.noClaims; }
    try{ return MainsInfo.read(f).orElse(Project.noClaims); }
    catch(UserError|UncheckedIOException e){ return Project.noClaims; }
  }
  private boolean accept(Path f){
    var l= live.get(f);
    var previous= l.claims;
    var read= read(f);
    var file= f.resolve(Facts.outDir).resolve("mains.info");
    try{ l.claims= Project.filled(read,previous,alias(f),live.entrySet().stream().filter(e->!e.getKey().equals(f)).map(e->e.getValue().claims)); }
    catch(UserError e){
      l.claims= previous;
      l.failure= e.getMessage();
      output(f,l.failure.stripTrailing()+"\n");
      Fs.ofV(()->Files.deleteIfExists(file));
      return false;
    }
    if (!l.claims.equals(read)){ Fs.writeUtf8(file,l.claims.print()); }
    return true;
  }
  private static final List<String> verbs= List.of("register","select","run","compile","check","terminate","clean","kind","forget","mains","link","clear");
  private static final List<String> thirdLine= List.of("run","kind","mains","link");
  private void apply(String message){
    if (message.isEmpty()){ view.show(); return; }
    var lines= List.of(message.split("\n",-1));
    if (lines.size() == 1){ register(lines.getFirst()); return; }
    if (lines.size() > 3){ tell(Messages.tooManyLines(message)); return; }
    request(lines.get(0),lines.get(1),lines.size() > 2 ? lines.get(2) : "");
  }
  private void request(String verb, String name, String arg){
    if (!verbs.contains(verb)){ tell(Messages.unknownVerb(verb,verbs)); return; }
    if (!arg.isEmpty() && !thirdLine.contains(verb)){ tell(Messages.thirdLineRefused(verb,name,arg,thirdLine)); return; }
    if (verb.equals("register")){ register(name); return; }
    var e= registry.named(name);
    if (e.isEmpty()){ tell(Messages.unknownProject(verb,name,registry.all().stream().map(Entry::alias).toList())); return; }
    var folder= e.get().path();
    scan(folder);
    if (List.of("select","run","compile","check","terminate","clean").contains(verb)){ selected= Optional.of(folder); }
    switch(verb){
      case "select" -> view.show();
      case "run" -> job(folder,true,arg.isEmpty() ? Optional.empty() : Optional.of(arg));
      case "compile" -> job(folder,false,Optional.empty());
      case "check" -> output(folder,project(folder).problem().map(s->s.stripTrailing()+"\n").orElse("--- ok: no problem found ---\n"));
      case "terminate" -> terminate(folder);
      case "clean" -> clean(folder);
      case "kind" -> kind(folder,arg);
      case "forget" -> { drop(folder); registry.remove(folder); }
      case "mains" -> edit(folder,o->o.withMains(words(arg)));
      case "link" -> link(folder,words(arg));
      case "clear" -> { Fs.writeUtf8(console(folder),""); view.clear(folder); }
      default -> throw Bug.unreachable();
    }
  }
  private void register(String given){
    if (given.isBlank()){ tell(Messages.registerNoFolder()); return; }
    Path folder;
    try{ folder= projectFolder(TaggedText.read(given,m->new Registry.Refused(Messages.registerNotTagged(m))),dir); }
    catch(Registry.Refused e){ tell(e.getMessage()); return; }
    if (!live.containsKey(folder) && !add(folder)){ return; }
    selected= Optional.of(folder);
    scan(folder);
    view.show();
  }
  private boolean add(Path folder){
    var nested= registry.overlapping(folder);
    if (nested.isPresent()){ tell(Messages.folderNestedWithRegistered(folder,nested.get())); return false; }
    String wanted;
    boolean fresh;
    String alias;
    try{
      wanted= Names.compactName(folder);
      Fs.rmTree(folder.resolve(Facts.outDir));
      fresh= Fs.of(()->{ try(var s= Files.list(folder)){ return s.findAny().isEmpty(); } });
      alias= Names.makeUnique(folder,registry.all().stream().map(Entry::alias).collect(Collectors.toSet()));
      if (fresh){ MakeDemo.hello(folder,Names.pkgName(alias),"Hello"); }
    }
    catch(UncheckedIOException e){ tell(Messages.registerRefused(folder,e.getCause())); return false; }
    if (!alias.equals(wanted)){ tell(Messages.projectNamed(folder,wanted,alias)); }
    registry.add(alias,folder,fresh ? Kind.code : Kind.idle);
    open(registry.of(folder).orElseThrow());
    return true;
  }
  private void job(Path f, boolean run, Optional<String> named){
    var request= run ? "run" : "compile";
    if (refused(f,request)){ return; }
    var p= project(f);
    if (p.kind() != Kind.code){ output(f,"--- "+request+" refused: this project is "+p.kind().text+", and only a code project "+(run ? "runs" : "compiles")+" ---\n"); return; }
    if (p.linkProblem().isPresent()){ output(f,p.linkProblem().get().stripTrailing()+"\n"); return; }
    var l= live.get(f);
    l.terminated= false;
    l.then= run ? ()->chosen(f,named) : List::of;
    if (!p.needsCompiling()){ l.todo= l.then.get(); next(f); return; }
    l.compiled.setLength(0);
    output(f,"--- compiling "+f.getFileName()+" ---\n");
    if (start(f,Project.compiling,()->tools.compile(f,out(f)))){ registry.update(f,e->e.withTimes(System.currentTimeMillis(),e.run())); }
  }
  private List<String> chosen(Path f, Optional<String> named){
    var p= project(f);
    var all= p.knownMains();
    if (p.mains().isEmpty() && p.problem().isPresent()){ output(f,p.problem().get().stripTrailing()+"\n"); return List.of(); }
    if (p.mains().isEmpty()){ return nothing(f,"this project needs compiling"); }
    if (all.isEmpty()){ return nothing(f,"this project has no main"); }
    if (named.isPresent() && !all.contains(named.get())){ return nothing(f,UserError.disp(named.get())+" is not one of the mains "+Messages.quoted(all)); }
    var stale= all.size() == 1 ? List.<String>of() : p.entry().mains().stream().filter(m->!all.contains(m)).toList();
    if (named.isEmpty() && !stale.isEmpty()){
      forgetStale(f);
      return nothing(f,"the selected "+Messages.quoted(stale)+" are not mains of this project; they are removed from the selected mains");
    }
    var chosen= named.map(List::of).orElseGet(p::selectedMains);
    if (chosen.isEmpty()){ return nothing(f,"none of "+Messages.quoted(all)+" is selected"); }
    return chosen;
  }
  private List<String> nothing(Path f, String why){ output(f,"--- nothing to run: "+why+" ---\n"); return List.of(); }
  private void next(Path f){
    var l= live.get(f);
    if (l.terminated || l.todo.isEmpty()){ scan(f); return; }
    var main= l.todo.getFirst();
    l.todo= l.todo.subList(1,l.todo.size());
    output(f,"--- running "+main+" ---\n");
    if (!start(f,main,()->tools.run(f,main,out(f)))){ return; }
    l.runs+= 1;
    l.lastRun= main;
    l.exit= -1;
    registry.update(f,e->e.withTimes(e.compiled(),System.currentTimeMillis()));
    l.reporting= core.scheduleAtFixedRate(()->step(()->report(f,l)),2,2,TimeUnit.SECONDS);
  }
  private void report(Path f, Live l){
    Optional<String> report;
    try{ report= Eclipse.report(f,l.job,l.since); }
    catch(UncheckedIOException e){
      l.reporting.cancel(false);
      output(f,"--- the report of "+l.job+" stops: "+Messages.fileFailure(e.getCause())+" ---\n");
      return;
    }
    report.ifPresent(r->eclipse.report(alias(f),r));
  }
  private boolean start(Path f, String what, Supplier<ChildJvm> jvm){
    var l= live.get(f);
    ChildJvm child;
    try{ child= jvm.get(); }
    catch(UncheckedIOException e){
      output(f,"--- "+(what.equals(Project.compiling) ? "compile" : what)+" did not start: "+Messages.fileFailure(e.getCause())+" ---\n");
      l.todo= List.of();
      scan(f);
      return false;
    }
    l.job= what;
    l.since= Instant.now();
    l.child= child;
    Thread.startVirtualThread(()->await(f,l,child));
    return true;
  }
  private void await(Path f, Live l, ChildJvm child){
    int ec;
    try{ ec= child.await(); }
    catch(InterruptedException e){ throw Bug.of(e); }
    post(()->exited(f,l,child,ec));
  }
  private void exited(Path f, Live l, ChildJvm child, int ec){
    if (live.get(f) != l || l.child != child){ return; }
    var what= l.job;
    if (!what.equals(Project.compiling) && !l.reporting.isCancelled()){ l.reporting.cancel(false); report(f,l); }
    l.child= null;
    l.job= "";
    if (what.equals(Project.compiling)){
      l.failure= ec == 0 ? "" : l.compiled.toString();
      var done= ec == 0 && accept(f);
      output(f,"--- compile "+(done ? "done" : ec == 0 ? "failed" : "failed with "+ec)+" ---\n");
      scan(f);
      if (l.mains.isPresent()){ forgetStale(f); }
      l.todo= done && !l.terminated ? l.then.get() : List.of();
      next(f);
      return;
    }
    l.exit= ec;
    output(f,"--- "+what+" exited with "+ec+" after "+Duration.between(l.since,Instant.now()).toSeconds()+"s ---\n");
    next(f);
  }
  private void terminate(Path f){
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
  private void clean(Path f){
    if (refused(f,"clear cache")){ return; }
    uncache(f).ifPresent(w->output(f,"--- clear cache failed: "+w+" ---\n"));
    accept(f);
    scan(f);
  }
  private static Optional<String> uncache(Path f){
    try{ Fs.rmTree(f.resolve(Facts.outDir)); return Optional.empty(); }
    catch(UncheckedIOException e){ return Optional.of(Messages.fileFailure(e.getCause())); }
  }
  private void kind(Path f, String text){
    var kind= Kind.of(text);
    if (kind.isEmpty()){ tell(Messages.unknownKind(f,text)); return; }
    if (refused(f,"kind change")){ return; }
    var from= project(f).kind();
    if (from != Kind.idle && kind.get() != Kind.idle && from != kind.get()){ output(f,"--- kind change refused: a project of kind "+from.text+" goes back to idle before becoming "+kind.get().text+" ---\n"); return; }
    registry.update(f,e->e.withKind(kind.get()));
    if (from != kind.get()){ accept(f); }
    scan(f);
  }
  private void link(Path f, List<String> words){
    var e= registry.of(f).orElseThrow();
    if (words.size() < 2 || !List.of("read","write").contains(words.get(1))){ tell(Messages.malformedLink(e.alias(),String.join(" ",words))); return; }
    if (e.kind() != Kind.code){ output(f,"--- link refused: this project is "+e.kind().text+", and only a code project links to data ---\n"); return; }
    var alias= words.get(0);
    var names= words.subList(2,words.size());
    edit(f,o->words.get(1).equals("write") ? o.withLinks(o.reads(),with(o.edits(),alias,names)) : o.withLinks(with(o.reads(),alias,names),o.edits()));
  }
  private static Map<String,List<String>> with(Map<String,List<String>> map, String key, List<String> names){
    var out= new LinkedHashMap<>(map);
    if (names.isEmpty()){ out.remove(key); } else { out.put(key,names); }
    return out;
  }
  private static List<String> words(String text){ return Stream.of(text.split(" +")).filter(w->!w.isEmpty()).toList(); }
  private void edit(Path f, UnaryOperator<Entry> op){
    try{ registry.update(f,op); }
    catch(Registry.Refused e){ tell(e.getMessage()); }
  }
  private void commitNow(String base, String text, Runnable done){
    var old= registry.all();
    try{ registry.commit(base,text); }
    catch(Registry.Refused e){ tell(e.getMessage()); return; }
    var renamed= old.stream().filter(o->registry.of(o.path()).filter(e->!e.alias().equals(o.alias())).isPresent()).collect(Collectors.toMap(Entry::path,o->Fs.readUtf8(eclipse.console(o.alias()))));
    renamed.forEach((f,shown)->Fs.writeUtf8(console(f),shown));
    live.keySet().stream().filter(f->registry.of(f).isEmpty()).toList().forEach(this::drop);
    registry.all().stream().filter(e->!live.containsKey(e.path())).forEach(this::open);
    old.stream().filter(o->registry.of(o.path()).filter(e->e.kind() != o.kind()).isPresent()).forEach(o->accept(o.path()));
    registry.all().forEach(e->scan(e.path()));
    done.run();
  }
  private void connectNow(Path chosen){
    String said;
    try{ said= eclipse.connect(chosen,dir); }
    catch(UncheckedIOException e){ said= Messages.eclipseNotConnected(e.getCause()); }
    tell(said);
  }
  private void drop(Path f){
    var l= live.remove(f);
    if (l.child != null){ l.terminated= true; l.child.kill(); }
    if (l.reporting != null){ l.reporting.cancel(false); }
    if (selected.equals(Optional.of(f))){ selected= Optional.empty(); }
  }
  private boolean refused(Path f, String request){
    var job= live.get(f).job;
    if (job.isEmpty()){ return false; }
    output(f,"--- "+request+" refused: the project is busy with "+job+" ---\n");
    return true;
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
    if (e.kind() != Kind.code || !fresh.upToDate()){ return; }
    Optional<String> error= Optional.empty();
    try{ l.mains= tools.mains(f); }
    catch(UserError err){ error= Optional.of(err.getMessage()); }
    catch(UncheckedIOException err){ error= Optional.of(Messages.fileFailure(err.getCause())); }
    if (!Facts.of(f,e.alias(),e.kind()).equals(fresh)){ scan(f); return; }
    if (l.mains.isEmpty()){ l.facts= fresh.outOfDate(error); }
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
    return new Project(e,l.facts,l.mains,l.claims,registry.linkProblem(e,f->live.get(f).facts.problem()),l.job,l.since,l.runs,l.lastRun,l.exit,l.failure);
  }
  private Project project(Path f){ return project(registry.of(f).orElseThrow()); }
  private String alias(Path f){ return registry.of(f).orElseThrow().alias(); }
  private Path console(Path f){ return eclipse.console(alias(f)); }
  private void publish(){
    var next= new State(registry.all().stream().map(this::project).toList(),selected);
    if (next.equals(state)){ return; }
    var old= state;
    var text= Eclipse.state(next.projects());
    if (!text.equals(Eclipse.state(old.projects()))){ eclipse.publish(text); }
    state= next;
    view.state(next);
  }
  private void tell(String text){
    Eclipse.append(eclipse.notes(),text.stripTrailing()+"\n");
    view.note(text);
  }
  private void output(Path f, String text){
    Eclipse.append(console(f),text);
    view.output(f,text);
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
  static Path projectFolder(String given, Path managerDir){
    if (given.isBlank()){ throw new Registry.Refused(Messages.registerNoFolder()); }
    Path path;
    try{ path= Path.of(given).toAbsolutePath().normalize(); }
    catch(InvalidPathException e){ throw new Registry.Refused(Messages.registerNotAPath(given,e.getReason())); }
    if (!Files.exists(path)){ throw new Registry.Refused(Messages.registerNothing(path)); }
    return Registry.placed(Registry.real(Files.isDirectory(path) ? path : path.getParent()),managerDir,Registry.Refused::new);
  }
}

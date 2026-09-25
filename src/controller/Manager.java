package controller;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
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
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import controller.Registry.Entry;
import controller.Registry.Kind;
import mainCoordinator.MakeDemo;
import realSourceOracle.AutoloadHandler;
import tools.ChildJvm;
import tools.Fs;
import userMessages.Report;
import userMessages.UserError;
import userMessages.Violation;
import utils.Bug;
import utils.Join;
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
    Facts facts;
    Optional<Map<String,String>> mains= Optional.empty();
    String job= "";
    Instant since= Instant.EPOCH;
    int runs;
    String lastRun= "";
    int exit= -1;
    StringBuilder compiled= new StringBuilder();
    String failure= "";
    ChildJvm child;
    boolean terminated;
    boolean thenRun;
    Optional<String> named= Optional.empty();
    List<String> todo= List.of();
    ScheduledFuture<?> reporting;
  }
  private final ScheduledExecutorService core= Executors.newSingleThreadScheduledExecutor();
  public final Path dir;
  public final Eclipse eclipse;
  private final Tools tools;
  private final View view;
  private final Consumer<RuntimeException> fail;
  private final Registry registry;
  private final Map<Path,Live> live= new HashMap<>();
  private Optional<Path> selected= Optional.empty();
  private int turn;
  private volatile State state= new State(List.of(),Optional.empty());
  public Manager(Path dir, Tools tools, View view, Consumer<RuntimeException> fail){
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
  public void ask(String verb, String name, String arg){ post(()->request(verb,name,arg)); }
  public void commit(String text, Runnable done){ post(()->commitNow(text,done)); }
  public void connect(Path chosen){ post(()->connectNow(chosen)); }
  void settle(){
    try{ core.submit(()->{}).get(); }
    catch(InterruptedException|ExecutionException e){ throw Bug.of(e); }
  }
  private void post(Runnable r){ core.execute(()->step(r)); }
  private void step(Runnable r){
    try{ r.run(); publish(); }
    catch(UserError e){ fail.accept(e); }
    catch(Throwable t){ fail.accept(Bug.of(t)); }
  }
  private void load(){
    Fs.writeUtf8(eclipse.notes(),"");
    registry.all().forEach(this::open);
    registry.reset.forEach(e->{
      Fs.rmTree(e.path().resolve(Facts.outDir));
      scan(e.path());
      tell("In projects.info the \"kind\" of \""+e.alias()+"\" was missing or not one of the kinds: \""+e.alias()+"\" is now idle, and its compiled cache is deleted.");
    });
    eclipse.publish(Eclipse.state(registry.all().stream().map(this::project).toList()));
  }
  private void open(Entry e){
    live.put(e.path(),new Live());
    Fs.writeUtf8(eclipse.console(e.alias()),"");
    scan(e.path());
  }
  private static final List<String> verbs= List.of("register","select","run","compile","check","terminate","clean","kind","forget","mains","link","clear");
  private void apply(String message){
    if (message.isEmpty()){ view.show(); return; }
    var lines= List.of(message.split("\n",-1));
    if (lines.size() == 1){ register(lines.getFirst()); return; }
    if (lines.size() > 3){ tell("The manager was sent a message of "+lines.size()+" lines, but a message is empty, to show the window, or a path, or a request of two or three lines: a verb, then a project name (a path for \"register\"), then for some verbs a third line:\n"+message); return; }
    request(lines.get(0),lines.get(1),lines.size() > 2 ? lines.get(2) : "");
  }
  private void request(String verb, String name, String arg){
    if (!verbs.contains(verb)){ tell("The manager was asked to \""+verb+"\", but that is not a request it knows: the requests are "+Join.of(verbs.stream().map(v->"\""+v+"\""),"",", ","")+"."); return; }
    if (verb.equals("register")){ register(name); return; }
    var e= registry.named(name);
    if (e.isEmpty()){ tell("The manager was asked to \""+verb+"\" the project \""+name+"\", but no project is named \""+name+"\"."+Join.of(registry.all().stream().map(o->"\n  "+o.alias()),"\nThe projects are:","","","\nNo project is registered.")); return; }
    var folder= e.get().path();
    if (List.of("select","run","compile","check","terminate","clean").contains(verb)){ selected= Optional.of(folder); }
    switch(verb){
      case "select" -> { scan(folder); view.show(); }
      case "run" -> job(folder,true,arg.isEmpty() ? Optional.empty() : Optional.of(arg));
      case "compile" -> job(folder,false,Optional.empty());
      case "check" -> check(folder);
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
    if (given.isBlank()){ tell("The manager was asked to register a folder, but the message names no folder."); return; }
    Path folder;
    try{ folder= projectFolder(given,dir); }
    catch(UserError e){ tell(e.getMessage()); return; }
    if (!live.containsKey(folder) && !add(folder)){ return; }
    selected= Optional.of(folder);
    scan(folder);
    view.show();
  }
  private boolean add(Path folder){
    var nested= registry.overlapping(folder);
    if (nested.isPresent()){ tell(Report.folderNestedWithRegistered(folder,nested.get()).getMessage()); return false; }
    var wanted= Names.compactName(folder);
    var fresh= Fs.of(()->{ try(var s= Files.list(folder)){ return s.findAny().isEmpty(); } });
    var alias= Names.makeUnique(folder,registry.all().stream().map(Entry::alias).collect(Collectors.toSet()));
    if (!alias.equals(wanted)){ tell(Report.projectNamed(folder,wanted,alias).getMessage()); }
    Fs.rmTree(folder.resolve(Facts.outDir));
    registry.add(alias,folder);
    if (fresh){
      registry.update(folder,e->e.withKind(Kind.code));
      MakeDemo.hello(folder,Names.pkgName(alias),AutoloadHandler.capFirst(alias));
    }
    open(registry.of(folder).orElseThrow());
    return true;
  }
  private void job(Path f, boolean run, Optional<String> named){
    var request= run ? "run" : "compile";
    if (refused(f,request)){ return; }
    scan(f);
    var p= project(f);
    if (p.kind() != Kind.code){ output(f,"--- "+request+" refused: this project is "+p.kind().text+", and only a code project "+(run ? "runs" : "compiles")+" ---\n"); return; }
    if (p.linkProblem().isPresent()){ output(f,p.linkProblem().get().stripTrailing()+"\n"); return; }
    var l= live.get(f);
    l.terminated= false;
    if (!p.needsCompiling()){ l.todo= run ? chosen(f,named) : List.of(); next(f); return; }
    l.thenRun= run;
    l.named= named;
    registry.update(f,e->e.withTimes(System.currentTimeMillis(),e.run()));
    output(f,"--- compiling "+f.getFileName()+" ---\n");
    l.compiled.setLength(0);
    start(f,Project.compiling,tools.compile(f,out(f)));
  }
  private List<String> chosen(Path f, Optional<String> named){
    var p= project(f);
    var all= p.knownMains();
    if (p.mains().isEmpty()){ return nothing(f,"this project needs compiling"); }
    if (all.isEmpty()){ return nothing(f,"this project has no main"); }
    if (named.isPresent() && !all.contains(named.get())){ return nothing(f,named.get()+" is not one of the mains "+all); }
    var stale= all.size() == 1 ? List.<String>of() : p.entry().mains().stream().filter(m->!all.contains(m)).toList();
    if (named.isEmpty() && !stale.isEmpty()){
      forgetStale(f);
      return nothing(f,"the selected "+stale+" are not mains of this project; they are removed from the selected mains");
    }
    var chosen= named.map(List::of).orElseGet(p::selectedMains);
    if (chosen.isEmpty()){ return nothing(f,"none of "+all+" is selected"); }
    return chosen;
  }
  private List<String> nothing(Path f, String why){ output(f,"--- nothing to run: "+why+" ---\n"); return List.of(); }
  private void next(Path f){
    var l= live.get(f);
    if (l.terminated || l.todo.isEmpty()){ scan(f); return; }
    var main= l.todo.getFirst();
    l.todo= l.todo.subList(1,l.todo.size());
    l.runs+= 1;
    l.lastRun= main;
    registry.update(f,e->e.withTimes(e.compiled(),System.currentTimeMillis()));
    output(f,"--- running "+main+" ---\n");
    start(f,main,tools.run(f,main,out(f)));
    var alias= alias(f);
    var since= l.since;
    l.reporting= core.scheduleAtFixedRate(()->step(()->eclipse.report(alias,f,main,since)),2,2,TimeUnit.SECONDS);
  }
  private void start(Path f, String what, ChildJvm child){
    var l= live.get(f);
    l.job= what;
    l.since= Instant.now();
    l.child= child;
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
    var what= l.job;
    l.child= null;
    l.job= "";
    if (what.equals(Project.compiling)){
      l.failure= ec == 0 ? "" : l.compiled.toString();
      output(f,"--- compile "+(ec == 0 ? "done" : "failed with "+ec)+" ---\n");
      scan(f);
      if (l.mains.isPresent()){ forgetStale(f); }
      l.todo= ec == 0 && l.thenRun && !l.terminated ? chosen(f,l.named) : List.of();
      next(f);
      return;
    }
    l.reporting.cancel(false);
    eclipse.report(alias(f),f,what,l.since);
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
  private void check(Path f){
    scan(f);
    output(f,project(f).problem().map(s->s.stripTrailing()+"\n").orElse("--- ok: no problem found ---\n"));
  }
  private void clean(Path f){
    if (refused(f,"clear cache")){ return; }
    Fs.rmTree(f.resolve(Facts.outDir));
    scan(f);
  }
  private void kind(Path f, String text){
    var kind= Kind.of(text);
    if (kind.isEmpty()){ tell("The manager was asked to change the kind of\n"+f+"\nto \""+text+"\", but the kinds are \"idle\", \"code\", \"data:readOnly\" and \"data:readWrite\"."); return; }
    if (refused(f,"kind change")){ return; }
    var from= project(f).kind();
    if (from != Kind.idle && kind.get() != Kind.idle && from != kind.get()){ output(f,"--- kind change refused: a project of kind "+from.text+" goes back to idle before becoming "+kind.get().text+" ---\n"); return; }
    registry.update(f,e->e.withKind(kind.get()));
    scan(f);
  }
  private void link(Path f, List<String> words){
    var e= registry.of(f).orElseThrow();
    if (words.size() < 2 || !List.of("read","write").contains(words.get(1))){ tell("The manager was asked to link \""+e.alias()+"\" with \""+String.join(" ",words)+"\", but a link is a project name, then \"read\" or \"write\", then the type names, none to remove the link."); return; }
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
  private static List<String> words(String text){ return text.isBlank() ? List.of() : List.of(text.strip().split(" +")); }
  private void edit(Path f, UnaryOperator<Entry> op){
    try{ registry.update(f,op); }
    catch(UserError e){ tell(e.getMessage()); }
  }
  private void commitNow(String text, Runnable done){
    var old= registry.all();
    try{ registry.commit(text); }
    catch(UserError e){ tell(e.getMessage()); return; }
    var renamed= old.stream().filter(o->registry.of(o.path()).filter(e->!e.alias().equals(o.alias())).isPresent()).collect(Collectors.toMap(Entry::path,o->Fs.readUtf8(eclipse.console(o.alias()))));
    renamed.forEach((f,shown)->Fs.writeUtf8(console(f),shown));
    live.keySet().stream().filter(f->registry.of(f).isEmpty()).toList().forEach(this::drop);
    registry.all().stream().filter(e->!live.containsKey(e.path())).forEach(this::open);
    registry.all().forEach(e->scan(e.path()));
    done.run();
  }
  private void connectNow(Path chosen){ tell(eclipse.connect(chosen,dir)); }
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
    if (fresh.equals(l.facts)){ return; }
    l.facts= fresh;
    l.mains= Optional.empty();
    if (e.kind() != Kind.code || !fresh.upToDate()){ return; }
    Optional<String> error= Optional.empty();
    try{ l.mains= tools.mains(f); }
    catch(UserError err){ error= Optional.of(err.getMessage()); }
    if (l.mains.isPresent()){ return; }
    l.facts= null;
    if (!Facts.of(f,e.alias(),e.kind()).equals(fresh)){ scan(f); return; }
    l.facts= fresh.outOfDate(error);
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
    return new Project(e,l.facts,l.mains,registry.linkProblem(e,f->live.get(f).facts.problem()),l.job,l.since,l.runs,l.lastRun,l.exit,l.failure);
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
    var path= path(given);
    if (!Files.exists(path)){ throw Report.launchPathNotFound(path); }
    var folder= Files.isDirectory(path) ? path : path.getParent();
    var manager= managerDir.toAbsolutePath().normalize();
    if (folder.getFileName() == null){ throw Report.projectFolderIsRoot(folder); }
    if (folder.startsWith(manager) || manager.startsWith(folder)){ throw Report.managerFolderNotAProject(path,manager); }
    return folder;
  }
  static Path path(String given){
    if (given.isBlank()){ throw Violation.badLaunchArg(given,false); }
    try{ return Path.of(given).toAbsolutePath().normalize(); }
    catch(InvalidPathException e){ throw Violation.badLaunchArg(given,false); }
  }
}

package controller;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import controller.Registry.Entry;
import controller.Registry.Kind;
import coordinator.MainsInfo;
import realSourceOracle.AutoloadHandler;
import realSourceOracle.BuildWithZip;
import userMessages.Report;
import utils.Join;

/// One registered project as the manager knows it at one moment: its metadata, what its
/// folder holds, its mains once known, why its links are broken, its job, if any, and the output of its last compile when that failed.
public record Project(Entry entry, Facts facts, Optional<Map<String,String>> mains, MainsInfo claims, Optional<String> linkProblem, String job, Instant since, int runs, String lastRun, int exit, String failure){
  public static final String compiling= "compiling";
  public static final MainsInfo noClaims= new MainsInfo(Map.of());
  public record Claimant(Path folder, String alias, String main, boolean shortcut, MainsInfo.Claim claim){
    public String label(){ return alias+"::"+main; }
  }
  public enum State{
    codeInvalid("code: invalid content"), dataInvalid("data: invalid content"), idle("idle"), dataReadOnly("data: read only"), dataReadWrite("data: read write"),
    codeNoCache("code: not compiled (no cache)"), codeOutdated("code: not compiled (cache out of date)"), codeCompiled("code: compiled"), busy("code: busy");
    public final String text;
    State(String text){ this.text= text; }
  }
  private static final DateTimeFormatter when= DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
  public Path folder(){ return entry.path(); }
  public String alias(){ return entry.alias(); }
  public Kind kind(){ return entry.kind(); }
  public boolean busy(){ return !job.isEmpty(); }
  public Optional<String> running(){ return busy() && !job.equals(compiling) ? Optional.of(job) : Optional.empty(); }
  public boolean needsCompiling(){ return kind() == Kind.code && !facts.upToDate(); }
  public Optional<String> problem(){ return facts.problem().or(()->linkProblem).or(()->Optional.of(failure).filter(f->!f.isEmpty())); }
  public List<String> knownMains(){ return mains.map(m->List.copyOf(m.keySet())).orElse(List.of()); }
  public List<String> selectedMains(){
    var known= knownMains();
    return known.size() == 1 ? known : known.stream().filter(entry.mains()::contains).toList();
  }
  private Stream<Claimant> claimants(){
    return claims.mains().entrySet().stream()
      .flatMap(e->Stream.concat(claimants(e.getKey(),e.getValue().shortcuts(),true),claimants(e.getKey(),e.getValue().openWiths(),false)));
  }
  private Stream<Claimant> claimants(String main, List<MainsInfo.Claim> cs, boolean shortcut){
    return cs.stream().filter(c->!c.extension().isEmpty()).map(c->new Claimant(folder(),alias(),main,shortcut,c));
  }
  public static Map<String,List<Claimant>> claimed(List<Project> projects){
    return projects.stream().flatMap(Project::claimants)
      .sorted(Comparator.comparing(Claimant::label))
      .collect(Collectors.groupingBy(c->c.claim().extension(),TreeMap::new,Collectors.toUnmodifiableList()));
  }
  static MainsInfo filled(MainsInfo info, MainsInfo previous, String alias, Stream<MainsInfo> all){
    var fill= new Fill(previous,alias,extensions(Stream.concat(Stream.of(info),all)),extensions(Stream.of(info)));
    return new MainsInfo(info.mains().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,e->fill.main(e.getKey(),e.getValue()))));
  }
  private static HashSet<String> extensions(Stream<MainsInfo> infos){
    return infos.flatMap(i->i.mains().values().stream())
      .flatMap(m->Stream.concat(m.shortcuts().stream(),m.openWiths().stream()))
      .map(MainsInfo.Claim::extension).collect(Collectors.toCollection(HashSet::new));
  }
  private record Fill(MainsInfo previous, String alias, HashSet<String> used, Set<String> explicit){
    MainsInfo.Main main(String main, MainsInfo.Main m){ return new MainsInfo.Main(m.file(),claims(main,m.shortcuts(),true),claims(main,m.openWiths(),false)); }
    List<MainsInfo.Claim> claims(String main, List<MainsInfo.Claim> cs, boolean shortcut){ return cs.stream().map(c->claim(main,c,shortcut)).toList(); }
    MainsInfo.Claim claim(String main, MainsInfo.Claim c, boolean shortcut){
      if (!c.extension().isEmpty()){ return c; }
      var prefix= shortcut ? "fapp" : "ffile";
      var start= Math.floorMod((alias+"::"+main+"::"+c.icon()).hashCode(),1000);
      var ext= Stream.ofNullable(previous.mains().get(main))
        .flatMap(m->(shortcut ? m.shortcuts() : m.openWiths()).stream())
        .filter(p->p.icon().equals(c.icon())).map(MainsInfo.Claim::extension)
        .filter(e->e.matches(prefix+"[0-9]{3}") && !explicit.contains(e)).findFirst()
        .or(()->IntStream.range(0,1000).mapToObj(i->prefix+"%03d".formatted((start+i)%1000)).filter(e->!used.contains(e)).findFirst())
        .orElseThrow(()->Messages.noFreeExtension(main,shortcut,c.icon()));
      used.add(ext);
      return new MainsInfo.Claim(c.icon(),c.diskPath(),c.zipSteps(),c.zipEntry(),ext);
    }
  }
  static List<String> shortcuts(Path project, MainsInfo info){
    var files= new LinkedHashMap<String,String>();
    for (var e: info.mains().entrySet()){
      for (var c: e.getValue().shortcuts()){
        var file= shortcutFile(project,e.getKey(),c.extension());
        var other= files.putIfAbsent(file,e.getKey());
        if (other != null){ throw Messages.shortcutsCollide(other,e.getKey(),file); }
      }
    }
    return List.copyOf(files.keySet());
  }
  private static String shortcutFile(Path project, String main, String ext){
    var name= AutoloadHandler.fileName(main.substring(main.lastIndexOf('.')+1)).orElseThrow(()->Messages.shortcutNoFileName(main));
    if (BuildWithZip.winReserved.contains(name)){ throw Messages.shortcutReservedName(main,name); }
    var file= name+"."+ext;
    if (ext.equals("zip") || ext.equals("fear")){ throw Messages.shortcutExtRefused(main,file); }
    if (file.length() > BuildWithZip.maxPath){ throw Messages.shortcutTooLong(main,file); }
    var masks= Report.allowedNoExtFiles.contains(name) && Files.isRegularFile(project.resolve(name),LinkOption.NOFOLLOW_LINKS);
    if (masks){ throw Messages.shortcutMasksFile(main,file,name); }
    return file;
  }
  public State state(){
    if (busy()){ return State.busy; }
    if (problem().isPresent()){ return kind() == Kind.code ? State.codeInvalid : State.dataInvalid; }
    return switch(kind()){
      case idle -> State.idle;
      case dataReadOnly -> State.dataReadOnly;
      case dataReadWrite -> State.dataReadWrite;
      case code -> !facts.hasCache() ? State.codeNoCache : !facts.upToDate() ? State.codeOutdated : State.codeCompiled;
    };
  }
  public record Action(String text, String verb, boolean enabled){}
  public Action action(){
    if (busy()){ return new Action("Terminate","terminate",true); }
    if (kind() != Kind.code){ return new Action("Check","check",true); }
    if (needsCompiling()){ return new Action("Compile","compile",true); }
    return new Action(knownMains().size() > 1 ? "Run selected" : "Run","run",!selectedMains().isEmpty());
  }
  public String information(){
    var rows= new ArrayList<>(List.of(
      row("Folder",folder().toString()),
      row("Name",alias()),
      row("Kind",kind().text),
      row("Files",facts.files()+""),
      row("Total size",bytes(facts.bytes())),
      row("Last modified",stamp(facts.modified()))));
    if (kind() == Kind.code){
      rows.add(row("Compiled cache",facts.upToDate() ? "up to date" : "needs compiling"));
      rows.add(row("Last compile",stamp(entry.compiled())));
      rows.add(row("Last run",stamp(entry.run())));
      rows.add(row("Packages",Join.of(facts.pkgs(),""," ","","<none>")));
      rows.add(row("Mains selected",Join.of(entry.mains(),""," ","","<none>")));
      rows.add(row("Reads",Join.of(entry.reads().keySet().stream(),""," ","","<none>")));
      rows.add(row("Edits",Join.of(entry.edits().keySet().stream(),""," ","","<none>")));
    }
    rows.add(row("Job",busy() ? job : "none"));
    rows.add(row("Problems",problem().isEmpty() ? "none" : "see Error report"));
    return String.join("\n",rows);
  }
  private static String row(String name, String value){ return "%-16s%s".formatted(name,value); }
  private static String stamp(long millis){ return millis < 0 ? "never" : when.format(Instant.ofEpochMilli(millis)); }
  private static String bytes(long size){
    if (size < 1024){ return size+" bytes"; }
    if (size < 1024*1024){ return "%.1f kB".formatted(size/1024.0); }
    return "%.1f MB".formatted(size/(1024.0*1024));
  }
}

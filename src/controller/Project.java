package controller;

import java.nio.file.Path;
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
import realSourceOracle.BuildWithZip;
import realSourceOracle.PathEntry;
import managerMessages.Report;
import userMessages.UserError;
import tools.Fs;

/// One registered project as the manager knows it at one moment: its metadata, what its
/// folder holds, its mains once known, why its links are broken, and its job, if any.
public record Project(Entry entry, Facts facts, Optional<Map<String,String>> mains, MainsInfo claims, Optional<String> linkProblem, String job){
  public static final String compiling= "compiling";
  public static final MainsInfo noClaims= new MainsInfo(Map.of());
  public record Claimant(Path folder, String alias, String main, String kind, MainsInfo.Claim claim){
    public boolean shortcut(){ return kind.equals("Shortcut"); }
    public String label(){ return alias+"::"+main; }
    public Path icon(String suffix){ return folder.resolve(Facts.outDir).resolve("icons").resolve(claim.icon()+suffix); }
    public Path shortcutFile(){ return folder.resolve(Project.shortcutFile(folder, main, claim.extension())); }
  }
  public enum State{
    codeInvalid("code: invalid content"), dataInvalid("data: invalid content"), idle("idle"), dataReadOnly("data: read only"), dataReadWrite("data: read write"),
    codeNoCache("code: not compiled (no cache)"), codeOutdated("code: not compiled (cache out of date)"), codeCompiled("code: compiled"), busy("code: busy");
    public final String text;
    State(String text){ this.text= text; }
  }
  public Path folder(){ return entry.path(); }
  public String alias(){ return entry.alias(); }
  public Kind kind(){ return entry.kind(); }
  public boolean busy(){ return !job.isEmpty(); }
  public boolean needsCompiling(){ return kind() == Kind.code && facts.stale().isPresent(); }
  public Optional<String> problem(){ return facts.problem().or(() -> linkProblem); }
  public List<String> knownMains(){ return mains.map(m -> List.copyOf(m.keySet())).orElse(List.of()); }
  public List<String> selectedMains(){
    var known= knownMains();
    return known.size() == 1 ? known : known.stream().filter(entry.mains()::contains).toList();
  }
  static Stream<Claimant> claimants(Path folder, String alias, MainsInfo info){
    return info.mains().entrySet().stream()
      .flatMap(e -> Stream.concat(claimants(folder, alias, e.getKey(), e.getValue().shortcuts(), "Shortcut"), claimants(folder, alias, e.getKey(), e.getValue().openWiths(), "OpenWith")));
  }
  static Stream<Claimant> preloads(Path folder, String alias, MainsInfo info){
    return info.mains().entrySet().stream().flatMap(e -> claimants(folder, alias, e.getKey(), e.getValue().icons(), "PreLoadIcon"));
  }
  private static Stream<Claimant> claimants(Path folder, String alias, String main, List<MainsInfo.Claim> cs, String kind){ return cs.stream().map(c -> new Claimant(folder, alias, main, kind, c)); }
  public static Map<String,List<Claimant>> claimed(List<Project> projects){
    return projects.stream().flatMap(p -> claimants(p.folder(), p.alias(), p.claims))
      .sorted(Comparator.comparing(Claimant::label))
      .collect(Collectors.groupingBy(c -> c.claim().extension(), TreeMap::new, Collectors.toUnmodifiableList()));
  }
  static MainsInfo filled(MainsInfo info, MainsInfo previous, String alias, Stream<MainsInfo> all){
    var fill= new Fill(previous, alias, extensions(Stream.concat(Stream.of(info), all)), extensions(Stream.of(info)));
    return new MainsInfo(info.mains().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> fill.main(e.getKey(), e.getValue()))));
  }
  private static HashSet<String> extensions(Stream<MainsInfo> infos){
    return infos.flatMap(i -> i.mains().values().stream())
      .flatMap(m -> Stream.concat(m.shortcuts().stream(), m.openWiths().stream()))
      .map(MainsInfo.Claim::extension).collect(Collectors.toCollection(HashSet::new));
  }
  private record Fill(MainsInfo previous, String alias, HashSet<String> used, Set<String> explicit){
    MainsInfo.Main main(String main, MainsInfo.Main m){ return new MainsInfo.Main(m.file(), claims(main, m.shortcuts(), true), claims(main, m.openWiths(), false), m.icons()); }
    List<MainsInfo.Claim> claims(String main, List<MainsInfo.Claim> cs, boolean shortcut){ return cs.stream().map(c -> claim(main, c, shortcut)).toList(); }
    MainsInfo.Claim claim(String main, MainsInfo.Claim c, boolean shortcut){
      if (!c.extension().isEmpty()){ return c; }
      var prefix= shortcut ? "fapp" : "ffile";
      var start= Math.floorMod((alias+"::"+main+"::"+c.icon()).hashCode(), 1000);
      var ext= Stream.ofNullable(previous.mains().get(main))
        .flatMap(m -> (shortcut ? m.shortcuts() : m.openWiths()).stream())
        .filter(p -> p.icon().equals(c.icon())).map(MainsInfo.Claim::extension)
        .filter(e -> e.matches(prefix+"[0-9]{3}") && !explicit.contains(e)).findFirst()
        .or(() -> IntStream.range(0, 1000).mapToObj(i -> prefix+"%03d".formatted((start+i)%1000)).filter(e -> !used.contains(e)).findFirst())
        .orElseThrow(() -> Report.noFreeExtension(main, shortcut, c.icon()));
      used.add(ext);
      return new MainsInfo.Claim(c.icon(), c.diskPath(), c.zipSteps(), c.zipEntry(), ext);
    }
  }
  static List<String> shortcuts(Stream<Claimant> cs){
    var files= new LinkedHashMap<String,String>();
    for (var c : cs.filter(Claimant::shortcut).toList()){
      var file= shortcutFile(c.folder(), c.main(), c.claim().extension());
      var other= files.putIfAbsent(file, c.main());
      if (other != null){ throw Report.shortcutsCollide(other, c.main(), file); }
    }
    return List.copyOf(files.keySet());
  }
  private static String shortcutFile(Path project, String main, String ext){
    var name= Fs.fileNameOfType(main.substring(main.lastIndexOf('.')+1)).orElseThrow(() -> Report.shortcutNoFileName(main));
    var file= name+"."+ext;
    try{ BuildWithZip.checkIndividualVisibleSegment(new PathEntry(project, Path.of(file))); }
    catch (UserError e){ throw Report.shortcutBadFileName(main, file); }
    var masks= userMessages.Report.allowedNoExtFiles.contains(name) && Fs.isRegularFile(project.resolve(name));
    if (masks){ throw Report.shortcutMasksFile(main, file, name); }
    return file;
  }
  public State state(){
    if (busy()){ return State.busy; }
    if (problem().isPresent()){ return kind() == Kind.code ? State.codeInvalid : State.dataInvalid; }
    return switch (kind()){
      case idle -> State.idle;
      case dataReadOnly -> State.dataReadOnly;
      case dataReadWrite -> State.dataReadWrite;
      case code -> !facts.hasCache() ? State.codeNoCache : needsCompiling() ? State.codeOutdated : State.codeCompiled;
    };
  }
  public enum Action{ check, compile, run, terminate }
  public Action action(){
    if (busy()){ return Action.terminate; }
    if (kind() != Kind.code || problem().isPresent()){ return Action.check; }
    if (needsCompiling()){ return Action.compile; }
    return Action.run;
  }
}

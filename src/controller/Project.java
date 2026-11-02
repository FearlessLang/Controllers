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
import java.util.Locale;

import controller.Registry.Entry;
import controller.Registry.Kind;
import coordinator.MainsInfo;
import realSourceOracle.BuildWithZip;
import realSourceOracle.PathEntry;
import managerMessages.Report;
import userMessages.UserError;
import utils.OneOr;
import tools.Fs;

/// One registered project as the manager knows it at one moment: its metadata, what its
/// folder holds, its mains once known, and its job, if any.
public record Project(Entry entry, Facts facts, Optional<MainsInfo> mains, MainsInfo claims, String job){
  public static final String compiling= "compiling";
  public static final MainsInfo noClaims= new MainsInfo(Map.of());
  public record Claimant(Path folder, String alias, String main, String kind, MainsInfo.Claim claim){
    public boolean shortcut(){ return kind.equals("Shortcut"); }
    public String label(){ return alias+"::"+main; }
    public Path icon(String suffix){ return folder.resolve(Facts.outDir).resolve("icons").resolve(claim.icon()+suffix); }
    public Path shortcutFile(){ return folder.resolve(Project.shortcutFile(folder, main, claim.extension())); }
  }
  public enum State{
    codeInvalid("code: invalid content"), dataReadInvalid("dataRead: invalid content"), dataEditInvalid("dataEdit: invalid content"), idle("idle"), dataRead("dataRead"), dataEdit("dataEdit"),
    codeOutdated("code: compilation cache out of date"), codeCompiled("code: compiled"), busy("code: busy");
    public final String text;
    State(String text){ this.text= text; }
  }
  public Path folder(){ return entry.path(); }
  public String alias(){ return entry.alias(); }
  public Kind kind(){ return entry.kind(); }
  public boolean busy(){ return !job.isEmpty(); }
  public boolean needsCompiling(){ return kind() == Kind.code && facts.stale().isPresent(); }
  public Optional<String> problem(){ return facts.problem(); }
  public List<String> knownMains(){ return mains.map(m -> List.copyOf(m.mains().keySet())).orElse(List.of()); }
  public List<String> knownAttenuators(){ return mains.map(MainsInfo::attenuators).orElse(List.of()); }
  public List<String> knownDataRead(){ return mains.map(m -> Stream.concat(m.dataRead().stream(), m.dataEdit().stream()).sorted().toList()).orElse(List.of()); }
  public Optional<String> brokenMount(String type, List<Project> all){
    var data= entry.mounts().get(type);
    if (!knownDataRead().contains(type)){ return Optional.of(Report.mountNotDataRead(type, alias())); }
    var target= OneOr.opt("project "+data, all.stream().filter(p -> p.alias().equals(data)));
    if (target.isEmpty()){ return Optional.of(Report.mountNotRegistered(data)); }
    var kind= target.get().kind();
    if (!kind.isData()){ return Optional.of(Report.mountNotData(data, kind.text)); }
    if (kind == Kind.dataRead && mains.orElseThrow().dataEdit().contains(type)){ return Optional.of(Report.mountNotEditable(data, type)); }
    return target.get().problem().map(why -> Report.mountInvalid(data, why));
  }
  public List<String> mountProblems(List<Project> all){
    var unmounted= knownDataRead().stream().filter(t -> !entry.mounts().containsKey(t)).map(Report::notMounted);
    var broken= entry.mounts().keySet().stream().flatMap(t -> brokenMount(t, all).map(why -> Report.brokenMount(t, entry.mounts().get(t), why)).stream());
    return Stream.concat(unmounted, broken).toList();
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
    if (problem().isPresent() && kind() != Kind.idle){ return kind() == Kind.code ? State.codeInvalid : kind() == Kind.dataRead ? State.dataReadInvalid : State.dataEditInvalid; }
    return switch (kind()){
      case idle -> State.idle;
      case dataRead -> State.dataRead;
      case dataEdit -> State.dataEdit;
      case code -> !needsCompiling() ? State.codeCompiled : State.codeOutdated;
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

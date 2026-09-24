package controller;

import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import controller.Registry.Entry;
import controller.Registry.Kind;
import utils.Join;

/// One registered project as the manager knows it at one moment: its metadata, what its
/// folder holds, its mains once known, why its links are broken, and its job, if any.
public record Project(Entry entry, Facts facts, Optional<Map<String,String>> mains, Optional<String> linkProblem, String job, Instant since, int runs, String lastRun, int exit){
  public static final String compiling= "compiling";
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
  public Optional<String> problem(){ return facts.problem().or(()->linkProblem); }
  public List<String> knownMains(){ return mains.map(m->List.copyOf(m.keySet())).orElse(List.of()); }
  public List<String> selectedMains(){
    var known= knownMains();
    return known.size() == 1 ? known : known.stream().filter(entry.mains()::contains).toList();
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
    if (kind() != Kind.code){ return new Action("Check","compile",true); }
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

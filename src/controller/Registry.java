package controller;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardOpenOption.CREATE_NEW;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import core.TName;
import fileAssociations.Icon;
import fileSupport.Info;
import fileSupport.Info.Obj;
import fileSupport.Info.Obj.Field;
import fileSupport.StringFiles;
import managerMessages.Report;
import managerMessages.Violation;
import metaParser.Span;
import realSourceOracle.Fix;
import tools.Fs;
import userMessages.UserError;
import utils.Join;
import utils.OneOr;
import utils.Push;

/// The registered projects and the system extensions of `projects.info`: read once, then
/// kept in memory and written back whole on every change.
public final class Registry{
  public enum Kind{
    idle("idle"), code("code"), dataRead("dataRead"), dataEdit("dataEdit");
    public final String text;
    Kind(String text){ this.text= text; }
    public boolean isData(){ return this == dataRead || this == dataEdit; }
    public static Optional<Kind> of(String text){ return OneOr.opt("kind "+text, Stream.of(values()).filter(k -> k.text.equals(text))); }
  }
  public record Entry(String alias, Path path, Kind kind, List<String> mains, Map<String,String> mounts, Map<String,List<String>> attenuators){
    public Entry{ mains = List.copyOf(mains); mounts = Collections.unmodifiableMap(new LinkedHashMap<>(mounts)); attenuators = Collections.unmodifiableMap(new LinkedHashMap<>(attenuators)); }
    public Entry withKind(Kind k){ return new Entry(alias, path, k, mains, mounts, attenuators); }
    public Entry withMains(List<String> m){ return new Entry(alias, path, kind, m, mounts, attenuators); }
    public Entry withMounts(Map<String,String> m){ return new Entry(alias, path, kind, mains, m, attenuators); }
    public Entry withAttenuators(Map<String,List<String>> a){ return new Entry(alias, path, kind, mains, mounts, a); }
  }
  public record Content(List<Entry> projects, List<String> extensions){
    public Content{ projects = List.copyOf(projects); extensions = List.copyOf(extensions); }
  }
  public record Grants(Map<String,Path> mounts, Map<String,Path> locks){}
  private static final List<String> keys= List.of("path", "kind", "mains", "mounts", "attenuators");
  private static final String kinds= "\"idle\", \"code\", \"dataRead\" or \"dataEdit\"";
  private static final String mainShape= "a Fearless main name: a package name, a dot, then a type name, like \"hello.Hello1\"";
  private static final String dataReadShape= "a Fearless DataRead type name: a package name, a dot, then a type name, like \"hello.Diary\"";
  private static final String attenuatorShape= "a Fearless attenuator name: a package name, a dot, then a type name, like \"hello.NoGui\"";
  public static final String systemShape= "a system extension: 1 to "+Fs.maxExtSeg+" lowercase letters or digits, other than \"fearless\", \"fapp000\" to \"fapp999\" and \"ffile000\" to \"ffile999\"";
  public sealed interface Away{}
  public record Why(String text) implements Away{}
  public record Same(Path folder) implements Away{}
  private record Seen(Path walked, Path found, boolean top){}
  public static final Comparator<Path> exact= Comparator.comparing(Path::toString).thenComparing(Comparator.<Path>naturalOrder());
  private static final Comparator<Seen> seenOrder= Comparator.comparing(Seen::walked, exact).thenComparing(Seen::found, exact).thenComparing(Seen::top);
  private static final String sentinel= ".fearless_sentinel_";
  private final Path dir;
  private final Consumer<Content> check;
  private Content content= new Content(List.of(), List.of());
  private Set<Path> away= Set.of();
  public Registry(Path dir, Consumer<Content> check){
    this.dir= dir;
    this.check= check;
    if (!Fs.exists(infoFile())){ save(content); return; }
    var text= StringFiles.read(infoFile(), UserError.onFileError());
    content = parse(text, infoFile());
    away = away(folders(content), dir).keySet();
    lookup();
  }
  public static List<Path> folders(Content c){ return c.projects().stream().map(Entry::path).toList(); }
  public static boolean same(Path a, Path b){ return exact.compare(a, b) == 0; }
  public Set<Path> conditions(List<Path> folders){
    var now= folders(content);
    var unchanged= now.size() == folders.size() && IntStream.range(0, now.size()).allMatch(i -> same(now.get(i), folders.get(i)));
    return unchanged ? away : away(folders, dir).keySet();
  }
  public static Map<Path,Away> away(List<Path> folders, Path managerDir){
    var res= new TreeMap<Path,Away>(exact);
    folders.forEach(f -> alone(f).ifPresent(why -> res.put(f, new Why(why))));
    var marked= folders.stream().filter(f -> !res.containsKey(f)).toList();
    var marks= new LinkedHashMap<String,Path>();
    var hits= new ArrayList<Map.Entry<Seen,Path>>();
    try{
      var own= fresh(managerDir, marks);
      clean(managerDir).ifPresent(e -> { throw Violation.couldNotCleanManagerFolder(managerDir, e.getKey(), e.getValue()); });
      marked.forEach(f -> clean(f).ifPresent(e -> res.put(f, refused(f, e.getValue()))));
      mark(managerDir, own, marks).ifPresent(e -> { throw Violation.couldNotSaveManagerFile(managerDir.resolve(own), e); });
      marked.stream().filter(f -> !res.containsKey(f)).forEach(f -> mark(f, fresh(f, marks), marks).ifPresent(e -> res.put(f, refused(f, e))));
      marks.values().stream().collect(Collectors.toCollection(() -> new TreeSet<>(exact))).forEach(t -> walk(t, t, marks, hits));
    }
    finally{ unmark(marks); }
    var seen= hits.stream().filter(h -> Files.notExists(h.getValue(), LinkOption.NOFOLLOW_LINKS)).map(Map.Entry::getKey).collect(Collectors.toCollection(() -> new TreeSet<>(seenOrder)));
    var reached= new ArrayList<Path>();
    for (var f : folders){
      if (res.containsKey(f)){ continue; }
      met(f, managerDir, reached, seen).ifPresentOrElse(a -> res.put(f, a), () -> reached.add(f));
    }
    return res;
  }
  private static Away refused(Path f, IOException e){ return new Why(Report.registerRefused(f, Report.fileFailure(e))); }
  private static Optional<String> alone(Path f){
    Optional<Path> at;
    try{ at= stray(f); }
    catch (UncheckedIOException e){ return Optional.of(Report.registerRefused(f, Report.fileFailure(e.getCause()))); }
    if (at.isPresent()){ return Optional.of(at.get().getFileName() == null ? Report.registerDriveLetter(f) : Fs.isLink(at.get()) ? Report.registerLink(f, at.get()) : Report.registerNotListed(f, at.get())); }
    return Optional.of(Report.projectFolderIsRoot(f)).filter(_ -> f.getFileName() == null);
  }
  private static boolean corresponds(Path folder){
    try{ return stray(folder).isEmpty(); }
    catch (UncheckedIOException _){ return false; }
  }
  private static Optional<Path> stray(Path folder){
    var at= folder.getRoot();
    if (at.toString().matches("[a-z]:.*")){ return Optional.of(at); }
    for (var name : folder){
      if (!listed(at, name.toString())){ return Optional.of(at.resolve(name)); }
      at = at.resolve(name);
      if (!Fix.realFolder(at)){ return Optional.of(at); }
    }
    return Optional.empty();
  }
  private static boolean listed(Path folder, String name){ return Fs.of(() -> { try (var s= Files.list(folder)){ return s.anyMatch(k -> k.getFileName().toString().equals(name)); } }); }
  private static String fresh(Path folder, Map<String,Path> marks){
    var name= sentinel+UUID.randomUUID();
    return marks.containsKey(name) || Files.exists(folder.resolve(name), LinkOption.NOFOLLOW_LINKS) ? fresh(folder, marks) : name;
  }
  private static Optional<Map.Entry<Path,IOException>> clean(Path folder){
    List<Path> old;
    try (var s= Files.list(folder)){ old= s.filter(k -> k.getFileName().toString().startsWith(sentinel)).toList(); }
    catch (IOException e){ return Optional.of(Map.entry(folder, e)); }
    return old.stream().flatMap(k -> delete(k).map(e -> Map.entry(k, e)).stream()).findFirst();
  }
  private static Optional<IOException> mark(Path folder, String name, Map<String,Path> marks){
    try{ Files.createFile(folder.resolve(name)); marks.put(name, folder); return Optional.empty(); }
    catch (IOException e){ return Optional.of(e); }
  }
  private static void unmark(Map<String,Path> marks){
    var failed= marks.entrySet().stream().flatMap(m -> delete(m.getValue().resolve(m.getKey())).stream()).toList();
    failed.stream().findFirst().ifPresent(e -> { throw new UncheckedIOException(e); });
  }
  private static Optional<IOException> delete(Path file){
    try{ Files.delete(file); return Optional.empty(); }
    catch (IOException e){ return Optional.of(e); }
  }
  private static void walk(Path top, Path folder, Map<String,Path> marks, List<Map.Entry<Seen,Path>> hits){
    List<Path> kids;
    try (var s= Files.list(folder)){ kids= s.toList(); }
    catch (IOException | UncheckedIOException _){ return; }
    for (var k : kids){
      Optional.ofNullable(marks.get(k.getFileName().toString())).ifPresent(x -> hits.add(Map.entry(new Seen(top, x, same(folder, top)), k)));
      if (Fix.realFolder(k)){ walk(top, k, marks, hits); }
    }
  }
  private static Optional<Away> met(Path f, Path m, List<Path> reached, Set<Seen> seen){
    if (meets(seen, m, f)){ return Optional.of(new Why(Report.managerFolderNotAProject(f, m))); }
    return reached.stream().filter(p -> meets(seen, p, f)).findFirst().map(p -> seen.contains(new Seen(p, f, true)) ? new Same(p) : new Why(Report.folderNestedWithRegistered(f, p)));
  }
  private static boolean meets(Set<Seen> seen, Path a, Path b){ return Stream.of(new Seen(a, b, true), new Seen(a, b, false), new Seen(b, a, false)).anyMatch(seen::contains); }
  public Path infoFile(){ return dir.resolve("projects.info"); }
  public List<Entry> all(){ return content.projects(); }
  public List<String> extensions(){ return content.extensions(); }
  public Content content(){ return content; }
  public static boolean isSystem(String ext){ return Fs.isExtSeg(ext) && Icon.system("."+ext); }
  public Optional<Entry> of(Path folder){ return OneOr.opt("registered "+folder, all().stream().filter(e -> same(e.path(), folder))); }
  public Optional<Entry> named(String alias){ return OneOr.opt("registered "+alias, all().stream().filter(e -> e.alias().equals(alias))); }
  public Optional<Entry> lookedUp(String alias){
    named(alias).ifPresentOrElse(e -> nameCheck(e.path()), this::lookup);
    return named(alias);
  }
  /// true when the check did a lookup
  public boolean nameCheck(Path folder){
    var differs= reaches(folder) && !Names.marker(folder).equals(of(folder).orElseThrow().alias());
    if (differs){ lookup(); }
    return differs;
  }
  public boolean reaches(Path folder){ return !away.contains(folder) && corresponds(folder) && Names.reachable(folder); }
  /// a folder whose marker can not be written gets no name
  public void lookup(){ content = inferred(content, content, away, this::marked); }
  private Entry marked(Entry e){
    if (e.alias().isEmpty() || !reaches(e.path()) || Names.marker(e.path()).equals(e.alias())){ return e; }
    try{ Names.nameAs(e.path(), e.alias()); return e; }
    catch (UncheckedIOException _){ return new Entry("", e.path(), e.kind(), e.mains(), e.mounts(), e.attenuators()); }
  }
  public static Content inferred(Content c, Content known, Set<Path> away){ return inferred(c, known, away, e -> e); }
  private static Content inferred(Content c, Content known, Set<Path> away, UnaryOperator<Entry> mark){
    var taken= new HashSet<String>();
    var res= new ArrayList<Entry>();
    for (var e : c.projects()){
      var name= !away.contains(e.path()) && Names.reachable(e.path()) ? Names.infer(e.path(), taken) : known.projects().stream().filter(k -> same(k.path(), e.path())).map(Entry::alias).filter(n -> !taken.contains(n)).findFirst().orElse("");
      var named= mark.apply(new Entry(name, e.path(), e.kind(), e.mains(), e.mounts(), e.attenuators()));
      taken.add(named.alias());
      res.add(named);
    }
    return new Content(res, c.extensions());
  }
  public void add(String alias, Path folder, Set<Path> away){
    assert folder.isAbsolute();
    assert all().stream().noneMatch(e -> same(e.path(), folder) || e.alias().equals(alias));
    var c= new Content(Push.of(all(), new Entry(alias, folder, Kind.idle, List.of(), Map.of(), Map.of())), extensions());
    save(c, text(c), away);
  }
  public void update(Path folder, UnaryOperator<Entry> op){
    assert of(folder).isPresent();
    save(new Content(all().stream().map(e -> same(e.path(), folder) ? op.apply(e) : e).toList(), extensions()));
  }
  public static Grants grants(Content c, Entry e, List<String> dataEdit, UnaryOperator<Path> lockFile){
    var mounts= new LinkedHashMap<String,Path>();
    e.mounts().forEach((type, data) -> c.projects().stream().filter(t -> t.alias().equals(data) && granted(t.kind(), dataEdit.contains(type))).forEach(t -> mounts.put(type, t.path())));
    var locks= new LinkedHashMap<String,Path>();
    mounts.forEach((t, p) -> locks.put(t, lockFile.apply(p)));
    return new Grants(Collections.unmodifiableMap(mounts), Collections.unmodifiableMap(locks));
  }
  private static boolean granted(Kind k, boolean editType){ return editType ? k == Kind.dataEdit : k.isData(); }
  public static String text(Content c){
    var projects= Join.of(c.projects().stream().map(e -> "    "+Info.print(entryToInfo(e)).strip().replace("\n", "\n    ")), "[\n", ",\n", "\n  ]", "[]");
    return "{\n  \"projects\": "+projects+",\n  \"systemExtensions\": "+Info.print(strList(c.extensions())).strip()+"\n}\n";
  }
  public String text(){ return text(content); }
  public void save(Content c){
    var text= text(c);
    assert text(parse(text, infoFile())).equals(text);
    save(c, text, conditions(folders(c)));
  }
  /// writes text, the text of an EditMetadata, which holds c
  public void save(Content c, String text, Set<Path> away){
    check.accept(c);
    var tmp= dir.resolve(UUID.randomUUID()+".tmp");
    try{ Fs.writeString(tmp, text, CREATE_NEW); Fs.move(tmp, infoFile(), ATOMIC_MOVE); }
    catch (IOException e){ throw Violation.couldNotSaveManagerFile(infoFile(), e); }
    content = c;
    this.away = away;
  }
  public static Content parse(String source, Path file){
    var root= Info.parse(source, file.toUri());
    if (!(root instanceof Obj top)){ throw Info.err(source, root.span(), Report.infoNotObject()); }
    for (var f : top.fields()){
      if (!List.of("projects", "systemExtensions").contains(f.key())){ throw Info.err(source, f.keySpan(), Report.infoUnknownKey(f.key())); }
    }
    var projects= top.field("projects").orElseThrow(() -> Info.err(source, top.span(), Report.infoMissing("projects")));
    var extensions= top.field("systemExtensions").orElseThrow(() -> Info.err(source, top.span(), Report.infoMissing("systemExtensions")));
    return new Content(entries(source, projects.value()), names(source, extensions.value(), "\"systemExtensions\"", Registry::isSystem, systemShape));
  }
  private static List<Entry> entries(String source, Info root){
    if (!(root instanceof Info.Lst top)){ throw Info.err(source, root.span(), Report.infoProjectsNotList()); }
    var entries= new ArrayList<Entry>();
    for (var item : top.items()){
      var e= entryOf(source, item, entries.size()+1);
      var twin= IntStream.range(0, entries.size()).filter(i -> same(entries.get(i).path(), e.path())).findFirst();
      if (twin.isPresent()){ throw Info.err(source, pathSpan(item), Report.infoSamePath(entries.size()+1, twin.getAsInt()+1)); }
      entries.add(e);
    }
    return entries;
  }
  private static Span pathSpan(Info project){ return ((Obj)project).field("path").orElseThrow().value().span(); }
  private static Entry entryOf(String source, Info item, int n){
    if (!(item instanceof Obj obj)){ throw Info.err(source, item.span(), Report.infoProjectNotObject()); }
    for (var f : obj.fields()){
      if (!keys.contains(f.key())){ throw Info.err(source, f.keySpan(), Report.infoUnknownAttribute(f.key(), keys)); }
    }
    var mains= obj.field("mains").map(f -> names(source, f.value(), "\"mains\"", Registry::isMainName, mainShape)).orElse(List.of());
    return new Entry("", pathOf(source, n, obj), kindOf(source, n, obj), mains, mounts(source, obj), attenuators(source, obj));
  }
  private static Path pathOf(String source, int n, Obj obj){
    var field= obj.field("path").orElseThrow(() -> Info.err(source, obj.span(), Report.infoMissingPath(n)));
    Path path;
    try{ path= TaggedText.readPath(str(source, field.value(), "\"path\""), m -> Info.err(source, field.value().span(), m)); }
    catch (InvalidPathException e){ throw Info.err(source, field.value().span(), Report.infoNotAPath(e.getMessage())); }
    if (path.toString().isEmpty()){ throw Info.err(source, field.value().span(), Report.infoEmptyPath()); }
    if (!path.isAbsolute()){ throw Info.err(source, field.value().span(), Report.infoNotAbsolute(path.toString())); }
    return path.normalize();
  }
  private static Kind kindOf(String source, int n, Obj obj){
    var field= obj.field("kind").orElseThrow(() -> Info.err(source, obj.span(), Report.infoMissingKind(n, kinds)));
    var s= str(source, field.value(), "\"kind\"");
    return Kind.of(s).orElseThrow(() -> Info.err(source, field.value().span(), Report.infoBadKind(s, kinds)));
  }
  private static String str(String source, Info value, String label){
    if (!(value instanceof Info.Str s)){ throw Info.err(source, value.span(), Report.infoNotString(label)); }
    return s.value();
  }
  private static List<String> names(String source, Info value, String label, Predicate<String> ok, String shape){
    if (!(value instanceof Info.Lst l)){ throw Info.err(source, value.span(), Report.infoNotList(label)); }
    var seen= new LinkedHashSet<String>();
    for (var item : l.items()){
      var s= str(source, item, "Every entry in "+label);
      if (!ok.test(s)){ throw Info.err(source, item.span(), Report.infoNotShape(s, label, shape)); }
      if (!seen.add(s)){ throw Info.err(source, item.span(), Report.infoRepeated(s, label)); }
    }
    return List.copyOf(seen);
  }
  private static Map<String,String> mounts(String source, Obj obj){
    var field= obj.field("mounts");
    if (field.isEmpty()){ return Map.of(); }
    if (!(field.get().value() instanceof Obj o)){ throw Info.err(source, field.get().value().span(), Report.infoMountsNotObject()); }
    var out= new LinkedHashMap<String,String>();
    for (var f : o.fields()){
      if (!isMainName(f.key())){ throw Info.err(source, f.keySpan(), Report.infoNotShape(f.key(), "\"mounts\"", dataReadShape)); }
      var label= "\"mounts\".\""+f.key()+"\"";
      var data= str(source, f.value(), label);
      if (!Names.isName(data)){ throw Info.err(source, f.value().span(), Report.infoMountBadName(data, label)); }
      out.put(f.key(), data);
    }
    return out;
  }
  private static Map<String,List<String>> attenuators(String source, Obj obj){
    var field= obj.field("attenuators");
    if (field.isEmpty()){ return Map.of(); }
    if (!(field.get().value() instanceof Obj o)){ throw Info.err(source, field.get().value().span(), Report.infoAttenuatorsNotObject()); }
    var out= new LinkedHashMap<String,List<String>>();
    for (var f : o.fields()){
      if (!isMainName(f.key())){ throw Info.err(source, f.keySpan(), Report.infoNotShape(f.key(), "\"attenuators\"", mainShape)); }
      var label= "\"attenuators\".\""+f.key()+"\"";
      if (!(f.value() instanceof Info.Lst l)){ throw Info.err(source, f.value().span(), Report.infoNotList(label)); }
      for (var item : l.items()){
        var a= str(source, item, "Every entry in "+label);
        if (!isMainName(a)){ throw Info.err(source, item.span(), Report.infoNotShape(a, label, attenuatorShape)); }
      }
      if (l.items().isEmpty()){ throw Info.err(source, f.value().span(), Report.infoAttenuatorsEmpty(label)); }
      out.put(f.key(), l.items().stream().map(i -> ((Info.Str)i).value()).toList());
    }
    return out;
  }
  private static boolean isMainName(String s){
    var dot= s.indexOf('.');
    return dot > 0 && TName.isPkgName(s.substring(0, dot)) && TName.isTypeName(s.substring(dot+1));
  }
  private static Info entryToInfo(Entry e){
    var fields= new ArrayList<Field>();
    fields.add(new Field("path", Info.noSpan, new Info.Str(TaggedText.ofPath(e.path()), Info.noSpan)));
    fields.add(new Field("kind", Info.noSpan, new Info.Str(e.kind().text, Info.noSpan)));
    if (!e.mains().isEmpty()){ fields.add(new Field("mains", Info.noSpan, strList(e.mains()))); }
    if (!e.mounts().isEmpty()){ fields.add(new Field("mounts", Info.noSpan, new Obj(e.mounts().entrySet().stream().map(m -> new Field(m.getKey(), Info.noSpan, new Info.Str(m.getValue(), Info.noSpan))).toList(), Info.noSpan))); }
    if (!e.attenuators().isEmpty()){ fields.add(new Field("attenuators", Info.noSpan, aliasMap(e.attenuators()))); }
    return new Obj(fields, Info.noSpan);
  }
  private static Info strList(List<String> xs){ return new Info.Lst(xs.stream().<Info>map(x -> new Info.Str(x, Info.noSpan)).toList(), Info.noSpan); }
  private static Info aliasMap(Map<String,List<String>> m){
    return new Obj(m.entrySet().stream().map(e -> new Field(e.getKey(), Info.noSpan, strList(e.getValue()))).toList(), Info.noSpan);
  }
}

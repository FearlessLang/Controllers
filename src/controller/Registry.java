package controller;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardOpenOption.CREATE_NEW;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
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
  private final Path dir;
  private final Consumer<Content> check;
  private Content content= new Content(List.of(), List.of());
  public Registry(Path dir, Consumer<Content> check){
    this.dir= dir;
    this.check= check;
    if (!Fs.exists(infoFile())){ save(content); return; }
    var text= StringFiles.read(infoFile(), UserError.onFileError());
    content = parse(text, infoFile(), p -> p, dir);
    lookup();
  }
  public static Path real(Path folder){
    try{ return folder.toRealPath(); }
    catch (IOException e){ return folder; }
  }
  static Path placed(Path folder, Path managerDir, Function<String,RuntimeException> err){
    var manager= real(managerDir);
    if (driveTop(folder)){ throw err.apply(Report.projectFolderIsRoot(folder)); }
    if (overlap(folder, manager)){ throw err.apply(Report.managerFolderNotAProject(folder, manager)); }
    return folder;
  }
  private static boolean driveTop(Path folder){
    if (folder.getFileName() == null){ return true; }
    try{ return !Files.getFileStore(folder).equals(Files.getFileStore(folder.getParent())); }
    catch (IOException e){ return false; }
  }
  private static boolean overlap(Path a, Path b){ return a.startsWith(b) || b.startsWith(a); }
  public Path infoFile(){ return dir.resolve("projects.info"); }
  public List<Entry> all(){ return content.projects(); }
  public List<String> extensions(){ return content.extensions(); }
  public Content content(){ return content; }
  public static boolean isSystem(String ext){ return Fs.isExtSeg(ext) && Icon.system("."+ext); }
  public Optional<Entry> of(Path folder){ return OneOr.opt("registered "+folder, all().stream().filter(e -> e.path().equals(folder))); }
  public Optional<Entry> named(String alias){ return OneOr.opt("registered "+alias, all().stream().filter(e -> e.alias().equals(alias))); }
  public Optional<Entry> lookedUp(String alias){
    named(alias).ifPresentOrElse(e -> nameCheck(e.path()), this::lookup);
    return named(alias);
  }
  /// true when the check did a lookup
  public boolean nameCheck(Path folder){
    var differs= Names.reachable(folder) && !Names.marker(folder).equals(of(folder).orElseThrow().alias());
    if (differs){ lookup(); }
    return differs;
  }
  /// a folder whose marker can not be written gets no name
  public void lookup(){ content = inferred(content, content, Registry::marked); }
  private static Entry marked(Entry e){
    if (e.alias().isEmpty() || !Names.reachable(e.path()) || Names.marker(e.path()).equals(e.alias())){ return e; }
    try{ Names.nameAs(e.path(), e.alias()); return e; }
    catch (UncheckedIOException _){ return new Entry("", e.path(), e.kind(), e.mains(), e.mounts(), e.attenuators()); }
  }
  public static Content inferred(Content c, Content known){ return inferred(c, known, e -> e); }
  private static Content inferred(Content c, Content known, UnaryOperator<Entry> mark){
    var taken= new HashSet<String>();
    var res= new ArrayList<Entry>();
    for (var e : c.projects()){
      var name= Names.reachable(e.path()) ? Names.infer(e.path(), taken) : known.projects().stream().filter(k -> k.path().equals(e.path())).map(Entry::alias).filter(n -> !taken.contains(n)).findFirst().orElse("");
      var named= mark.apply(new Entry(name, e.path(), e.kind(), e.mains(), e.mounts(), e.attenuators()));
      taken.add(named.alias());
      res.add(named);
    }
    return new Content(res, c.extensions());
  }
  public Optional<Path> overlapping(Path folder){
    return all().stream().map(Entry::path).filter(o -> !o.equals(folder) && overlap(folder, o)).findFirst();
  }
  public void add(String alias, Path folder){
    assert folder.isAbsolute() && folder.equals(real(folder));
    assert all().stream().noneMatch(e -> e.path().equals(folder) || e.alias().equals(alias));
    assert overlapping(folder).isEmpty();
    save(new Content(Push.of(all(), new Entry(alias, folder, Kind.idle, List.of(), Map.of(), Map.of())), extensions()));
  }
  public void update(Path folder, UnaryOperator<Entry> op){
    assert of(folder).isPresent();
    save(new Content(all().stream().map(e -> e.path().equals(folder) ? op.apply(e) : e).toList(), extensions()));
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
    assert text(parse(text, infoFile(), p -> p, dir)).equals(text);
    save(c, text);
  }
  /// writes text, the text of an EditMetadata, which holds c
  public void save(Content c, String text){
    check.accept(c);
    var tmp= dir.resolve(UUID.randomUUID()+".tmp");
    try{ Fs.writeString(tmp, text, CREATE_NEW); Fs.move(tmp, infoFile(), ATOMIC_MOVE); }
    catch (IOException e){ throw Violation.couldNotSaveManagerFile(infoFile(), e); }
    content = c;
  }
  public static Content parse(String source, Path file, UnaryOperator<Path> identity, Path managerDir){
    var root= Info.parse(source, file.toUri());
    if (!(root instanceof Obj top)){ throw Info.err(source, root.span(), Report.infoNotObject()); }
    for (var f : top.fields()){
      if (!List.of("projects", "systemExtensions").contains(f.key())){ throw Info.err(source, f.keySpan(), Report.infoUnknownKey(f.key())); }
    }
    var projects= top.field("projects").orElseThrow(() -> Info.err(source, top.span(), Report.infoMissing("projects")));
    var extensions= top.field("systemExtensions").orElseThrow(() -> Info.err(source, top.span(), Report.infoMissing("systemExtensions")));
    return new Content(entries(source, projects.value(), identity, managerDir), names(source, extensions.value(), "\"systemExtensions\"", Registry::isSystem, systemShape));
  }
  private static List<Entry> entries(String source, Info root, UnaryOperator<Path> identity, Path managerDir){
    if (!(root instanceof Info.Lst top)){ throw Info.err(source, root.span(), Report.infoProjectsNotList()); }
    var entries= new ArrayList<Entry>();
    for (var item : top.items()){
      entries.add(entryOf(source, item, entries.size()+1, identity));
      placed(entries.getLast().path(), managerDir, m -> Info.err(source, pathSpan(item), m));
    }
    for (var a= 0; a < entries.size(); a += 1){
      for (var b= a+1; b < entries.size(); b += 1){
        if (overlap(entries.get(a).path(), entries.get(b).path())){ throw Info.err(source, pathSpan(top.items().get(b)), Report.infoOverlap(b+1, a+1)); }
      }
    }
    return entries;
  }
  private static Span pathSpan(Info project){ return ((Obj)project).field("path").orElseThrow().value().span(); }
  private static Entry entryOf(String source, Info item, int n, UnaryOperator<Path> identity){
    if (!(item instanceof Obj obj)){ throw Info.err(source, item.span(), Report.infoProjectNotObject()); }
    for (var f : obj.fields()){
      if (!keys.contains(f.key())){ throw Info.err(source, f.keySpan(), Report.infoUnknownAttribute(f.key(), keys)); }
    }
    var mains= obj.field("mains").map(f -> names(source, f.value(), "\"mains\"", Registry::isMainName, mainShape)).orElse(List.of());
    return new Entry("", identity.apply(pathOf(source, n, obj)), kindOf(source, n, obj), mains, mounts(source, obj), attenuators(source, obj));
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

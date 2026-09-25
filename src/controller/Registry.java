package controller;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import controller.Info.Obj;
import controller.Info.Obj.Field;
import core.TName;
import fileSupport.StringFiles;
import userMessages.UserError;
import userMessages.Violation;
import utils.Join;
import utils.OneOr;
import utils.Push;

/// The registered projects: read once from `projects.info` and `activity.txt`, then kept
/// in memory and written back whole on every change.
public final class Registry{
  public enum Kind{
    idle("idle"), code("code"), dataReadOnly("data:readOnly"), dataReadWrite("data:readWrite");
    public final String text;
    Kind(String text){ this.text= text; }
    public boolean isData(){ return this == dataReadOnly || this == dataReadWrite; }
    public static Optional<Kind> of(String text){ return OneOr.opt("kind "+text, Stream.of(values()).filter(k->k.text.equals(text))); }
  }
  public record Entry(String alias, Path path, Kind kind, List<String> mains,
      Map<String,List<String>> reads, Map<String,List<String>> edits, long compiled, long run){
    public Entry withKind(Kind k){ return new Entry(alias,path,k,mains,reads,edits,compiled,run); }
    public Entry withMains(List<String> m){ return new Entry(alias,path,kind,List.copyOf(m),reads,edits,compiled,run); }
    public Entry withLinks(Map<String,List<String>> r, Map<String,List<String>> e){ return new Entry(alias,path,kind,mains,Map.copyOf(r),Map.copyOf(e),compiled,run); }
    public Entry withTimes(long c, long r){ return new Entry(alias,path,kind,mains,reads,edits,c,r); }
  }
  private static final List<String> keys= List.of("path","kind","mains","reads","edits");
  private static final String kinds= "\"idle\", \"code\", \"data:readOnly\" or \"data:readWrite\"";
  private static final String mainShape= "a Fearless main name: a package name, a dot, then a type name, like \"hello.Hello1\"";
  private static final String typeShape= "a Fearless type name: after any leading underscores, it starts with an uppercase letter";
  private final Path dir;
  private List<Entry> all= List.of();
  public final List<Entry> reset;
  public Registry(Path dir){
    this.dir= dir;
    if (!Files.exists(infoFile())){ reset= List.of(); return; }
    var text= read(infoFile());
    var root= Info.parse(text,infoFile().toUri());
    var bad= badKinds(root);
    all= withTimes(fromInfo(text,bad.isEmpty() ? root : idle((Obj)root,bad)));
    reset= all.stream().filter(e->bad.contains(e.alias())).toList();
    if (!reset.isEmpty()){ save(all); }
  }
  private static List<String> badKinds(Info root){
    if (!(root instanceof Obj top)){ return List.of(); }
    return top.fields().stream().filter(f->f.value() instanceof Obj o && o.field("kind").map(Field::value).filter(v->v instanceof Info.Str s && Kind.of(s.value()).isPresent()).isEmpty()).map(Field::key).toList();
  }
  private static Obj idle(Obj top, List<String> bad){
    return new Obj(top.fields().stream().map(f->bad.contains(f.key()) ? new Field(f.key(),f.keySpan(),idle((Obj)f.value())) : f).toList(),top.span());
  }
  private static Obj idle(Obj project){
    var kind= new Field("kind",Info.noSpan,new Info.Str(Kind.idle.text,Info.noSpan));
    return new Obj(Push.of(project.fields().stream().filter(f->!f.key().equals("kind")).toList(),kind),project.span());
  }
  private Path infoFile(){ return dir.resolve("projects.info"); }
  private Path activityFile(){ return dir.resolve("activity.txt"); }
  public List<Entry> all(){ return all; }
  public Optional<Entry> of(Path folder){ return OneOr.opt("registered "+folder, all.stream().filter(e->e.path().equals(folder))); }
  public Optional<Entry> named(String alias){ return OneOr.opt("registered "+alias, all.stream().filter(e->e.alias().equals(alias))); }
  public Optional<Path> overlapping(Path folder){
    return all.stream().map(Entry::path).filter(o->!o.equals(folder) && (folder.startsWith(o) || o.startsWith(folder))).findFirst();
  }
  public void add(String alias, Path folder){
    assert folder.equals(folder.toAbsolutePath().normalize());
    assert all.stream().noneMatch(e->e.path().equals(folder) || e.alias().equals(alias));
    assert overlapping(folder).isEmpty();
    save(Push.of(all,new Entry(alias,folder,Kind.idle,List.of(),Map.of(),Map.of(),-1,-1)));
  }
  public void remove(Path folder){ save(all.stream().filter(e->!e.path().equals(folder)).toList()); }
  public void update(Path folder, UnaryOperator<Entry> op){
    assert of(folder).isPresent();
    save(all.stream().map(e->e.path().equals(folder) ? op.apply(e) : e).toList());
  }
  public void commit(String text){ save(entries(text).stream().map(e->of(e.path()).map(o->e.withTimes(o.compiled(),o.run())).orElse(e)).toList()); }
  public static String text(List<Entry> entries){ return Info.print(toInfo(entries)); }
  /// Why the links of a code project are broken, if they are; invalid says why a project is invalid.
  public Optional<String> linkProblem(Entry e, Function<Path,Optional<String>> invalid){
    if (e.kind() != Kind.code){ return Optional.empty(); }
    return problemIn(e.reads(),"reads",false,invalid).or(()->problemIn(e.edits(),"edits",true,invalid));
  }
  private Optional<String> problemIn(Map<String,List<String>> links, String field, boolean needsWrite, Function<Path,Optional<String>> invalid){
    for (var alias: links.keySet()){
      var target= OneOr.opt("registered "+alias,all.stream().filter(o->o.alias().equals(alias)));
      if (target.isEmpty()){
        return Optional.of("\""+field+"\" refers to \""+alias+"\", but no project called \""+alias+"\" is registered.");
      }
      var kind= target.get().kind();
      var wrongKind= needsWrite ? kind != Kind.dataReadWrite : !kind.isData();
      if (wrongKind){
        var needed= needsWrite ? "\"data:readWrite\"" : "\"data:readOnly\" or \"data:readWrite\"";
        return Optional.of("\""+field+"\" refers to \""+alias+"\", but the kind of \""+alias+"\" is \""+kind.text+"\"; \""+field+"\" accepts only "+needed+".");
      }
      var problem= invalid.apply(target.get().path());
      if (problem.isPresent()){ return Optional.of("\""+field+"\" refers to \""+alias+"\", which is itself invalid:\n"+problem.get()); }
    }
    return Optional.empty();
  }
  private List<Entry> entries(String text){ return fromInfo(text,Info.parse(text,infoFile().toUri())); }
  private void save(List<Entry> entries){
    var text= text(entries);
    this.entries(text);
    writeText(infoFile(),text);
    writeText(activityFile(),Join.of(entries.stream().map(e->e.compiled()+" "+e.run()+" "+e.path().toUri()),"","\n","\n",""));
    all= entries;
  }
  private static String read(Path file){ return StringFiles.read(file,UserError.onFileError()); }
  private List<Entry> withTimes(List<Entry> entries){
    if (!Files.exists(activityFile())){ return entries; }
    var times= read(activityFile()).lines().map(l->l.split(" ",3)).collect(Collectors.toMap(p->Path.of(URI.create(p[2])),p->p));
    return entries.stream().map(e->Optional.ofNullable(times.get(e.path())).map(t->e.withTimes(Long.parseLong(t[0]),Long.parseLong(t[1]))).orElse(e)).toList();
  }
  private void writeText(Path file, String text){
    var tmp= dir.resolve(UUID.randomUUID()+".tmp");
    StringFiles.writeNew(tmp,text,UserError.onFileError());
    try{ Files.move(tmp,file,ATOMIC_MOVE); }
    catch(IOException e){ throw Violation.couldNotSaveRegisteredFolders(dir,e); }
  }
  public static List<Entry> fromInfo(String source, Info root){
    if (!(root instanceof Obj top)){
      throw Info.err(source,root.span(),"The whole file must be an object {...} mapping each project name to the metadata of that project.");
    }
    var entries= new ArrayList<Entry>();
    for (var field: top.fields()){
      if (!Names.isName(field.key())){
        throw Info.err(source,field.keySpan(),"\""+field.key()+"\" is not a valid project name: a project name uses only lowercase letters, digits and underscores, and starts with a letter or an underscore.");
      }
      entries.add(entryOf(source,field));
    }
    for (var a: entries){
      for (var b: entries.subList(entries.indexOf(a)+1,entries.size())){
        if (!a.path().equals(b.path()) && !a.path().startsWith(b.path()) && !b.path().startsWith(a.path())){ continue; }
        var span= ((Obj)top.field(b.alias()).orElseThrow().value()).field("path").orElseThrow().value().span();
        throw Info.err(source,span,"\""+b.alias()+"\" has the same path as \""+a.alias()+"\", or one is inside the other; every file belongs to exactly one project.");
      }
    }
    return entries;
  }
  private static Entry entryOf(String source, Field field){
    if (!(field.value() instanceof Obj obj)){
      throw Info.err(source,field.value().span(),"The metadata of \""+field.key()+"\" must be an object {...}.");
    }
    for (var f: obj.fields()){
      if (!keys.contains(f.key())){ throw Info.err(source,f.keySpan(),"Unknown project attribute \""+f.key()+"\": the attributes of a project are "+Join.of(keys.stream().map(k->"\""+k+"\""),"",", ","")+"."); }
    }
    var mains= names(source,obj,"mains","\"mains\"",Registry::isMainName,mainShape);
    var reads= aliasMap(source,obj,"reads");
    var edits= aliasMap(source,obj,"edits");
    for (var link: edits.entrySet()){
      var both= link.getValue().stream().filter(reads.getOrDefault(link.getKey(),List.of())::contains).findFirst();
      if (both.isEmpty()){ continue; }
      var names= (Info.Lst)((Obj)obj.field("edits").orElseThrow().value()).field(link.getKey()).orElseThrow().value();
      var span= OneOr.of("type name "+both.get(),names.items().stream().filter(i->((Info.Str)i).value().equals(both.get()))).span();
      throw Info.err(source,span,"\""+both.get()+"\" is in both \"reads\".\""+link.getKey()+"\" and \"edits\".\""+link.getKey()+"\": a type name in \"edits\" also reads, so it is not repeated in \"reads\"; a type name in \"reads\" only reads.");
    }
    return new Entry(field.key(),pathOf(source,field.key(),obj),kindOf(source,field.key(),obj),mains,reads,edits,-1,-1);
  }
  private static Path pathOf(String source, String alias, Obj obj){
    var field= obj.field("path").orElseThrow(()->Info.err(source,obj.span(),"Project \""+alias+"\" is missing its \"path\": the absolute path of the project folder."));
    var s= str(source,field.value(),"\"path\"");
    if (s.isEmpty()){ throw Info.err(source,field.value().span(),"\"path\" cannot be empty: it is the absolute path of the project folder."); }
    Path path;
    try{ path= Path.of(s); }
    catch(InvalidPathException e){ throw Info.err(source,field.value().span(),"\"path\" is not a path this system accepts: "+e.getMessage()); }
    if (!path.isAbsolute()){ throw Info.err(source,field.value().span(),"\"path\" must be an absolute path, not \""+s+"\"."); }
    return path.normalize();
  }
  private static Kind kindOf(String source, String alias, Obj obj){
    var field= obj.field("kind").orElseThrow(()->Info.err(source,obj.span(),"Project \""+alias+"\" is missing its \"kind\": one of "+kinds+"."));
    var s= str(source,field.value(),"\"kind\"");
    return Kind.of(s).orElseThrow(()->Info.err(source,field.value().span(),"\"kind\" must be one of "+kinds+", not \""+s+"\"."));
  }
  private static String str(String source, Info value, String label){
    if (!(value instanceof Info.Str s)){ throw Info.err(source,value.span(),label+" must be a string \"...\"."); }
    return s.value();
  }
  private static List<String> names(String source, Obj obj, String key, String label, Predicate<String> ok, String shape){
    var field= obj.field(key);
    if (field.isEmpty()){ return List.of(); }
    if (!(field.get().value() instanceof Info.Lst l)){ throw Info.err(source,field.get().value().span(),label+" must be a list [...] of strings."); }
    var seen= new LinkedHashSet<String>();
    for (var item: l.items()){
      var s= str(source,item,"Every entry in "+label);
      if (!ok.test(s)){ throw Info.err(source,item.span(),"\""+s+"\" in "+label+" is not "+shape+"."); }
      if (!seen.add(s)){ throw Info.err(source,item.span(),"\""+s+"\" is repeated in "+label+"."); }
    }
    return List.copyOf(seen);
  }
  private static Map<String,List<String>> aliasMap(String source, Obj obj, String key){
    var field= obj.field(key);
    if (field.isEmpty()){ return Map.of(); }
    if (!(field.get().value() instanceof Obj o)){
      throw Info.err(source,field.get().value().span(),"\""+key+"\" must be an object {...} mapping a project name to a list of type names.");
    }
    var out= new LinkedHashMap<String,List<String>>();
    for (var f: o.fields()){
      if (!Names.isName(f.key())){ throw Info.err(source,f.keySpan(),"\""+f.key()+"\" in \""+key+"\" is not a valid project name."); }
      var label= "\""+key+"\".\""+f.key()+"\"";
      var names= names(source,o,f.key(),label,TName::isTypeName,typeShape);
      if (names.isEmpty()){ throw Info.err(source,f.value().span(),label+" names no type: a link names the one or more type names the code uses for \""+f.key()+"\"."); }
      out.put(f.key(),names);
    }
    return Collections.unmodifiableMap(out);
  }
  private static boolean isMainName(String s){
    var dot= s.indexOf('.');
    return dot > 0 && TName.isPkgName(s.substring(0,dot)) && TName.isTypeName(s.substring(dot+1));
  }
  public static Info toInfo(List<Entry> entries){
    return new Obj(entries.stream().map(e->new Field(e.alias(),Info.noSpan,entryToInfo(e))).toList(),Info.noSpan);
  }
  private static Info entryToInfo(Entry e){
    var fields= new ArrayList<Field>();
    fields.add(new Field("path",Info.noSpan,new Info.Str(e.path().toString().replace('\\','/'),Info.noSpan)));
    fields.add(new Field("kind",Info.noSpan,new Info.Str(e.kind().text,Info.noSpan)));
    if (!e.mains().isEmpty()){ fields.add(new Field("mains",Info.noSpan,strList(e.mains()))); }
    if (!e.reads().isEmpty()){ fields.add(new Field("reads",Info.noSpan,aliasMap(e.reads()))); }
    if (!e.edits().isEmpty()){ fields.add(new Field("edits",Info.noSpan,aliasMap(e.edits()))); }
    return new Obj(fields,Info.noSpan);
  }
  private static Info strList(List<String> xs){ return new Info.Lst(xs.stream().<Info>map(x->new Info.Str(x,Info.noSpan)).toList(),Info.noSpan); }
  private static Info aliasMap(Map<String,List<String>> m){
    return new Obj(m.entrySet().stream().map(e->new Field(e.getKey(),Info.noSpan,strList(e.getValue()))).toList(),Info.noSpan);
  }
}

package suggest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipInputStream;

import suggest.Tokens.Item;
import suggest.Tokens.Kind;

/// The compilation cache of a project as Coordinator.check finds it, from the files of the project folder,
/// of its .fearless_out folder and of the base cache: the rules of coordinator.Cache.check, with the causes of
/// an invalid project this class reads; a project invalid for another cause is found out of date.
/// The map directives of the project are read from the rank files with the tokens of the compiler.
public final class Cached{
  public enum State{ invalid, outOfDate, compiled }
  private static final class Invalid extends RuntimeException{ private static final long serialVersionUID= 1L; }
  private record Ref(List<String> segments, long stamp, String text){
    String fearPath(){ return "fear:/"+String.join("/", segments); }
    String name(){ return segments.getLast(); }
  }
  private record Pkg(String name, int rank, List<Ref> files){}
  private static final List<String> ranks= List.of(
    "_rank_base", "_rank_core", "_rank_driver", "_rank_worker", "_rank_framework", "_rank_accumulator", "_rank_tool", "_rank_app");
  public static State state(Path folder, Path baseCache){
    List<Pkg> pkgs;
    try{ pkgs= packages(folder); }
    catch (Invalid e){ return State.invalid; }
    var out= folder.resolve(".fearless_out");
    if (Files.exists(out.resolve("_unfinished.txt")) || others(out, pkgs)){ return State.outOfDate; }
    var maps= maps(pkgs);
    return pkgs.stream().allMatch(p -> upToDate(p, pkgs, maps, out, baseCache)) ? State.compiled : State.outOfDate;
  }
  /// the packages the map directives of the project give the package names written in pkg; none for a project this class finds invalid
  public static Map<String,String> packages(Path folder, String pkg){
    try{ return maps(packages(folder)).getOrDefault(pkg, Map.of()); }
    catch (Invalid e){ return Map.of(); }
  }
  private static boolean others(Path out, List<Pkg> pkgs){
    if (!Files.isDirectory(out)){ return false; }
    var kept= Stream.concat(Stream.of("_error.txt", "_unfinished.txt", "icons"), pkgs.stream().flatMap(p -> Stream.of(".json", ".jar", ".html", ".txt").map(x -> p.name()+x))).toList();
    try (var s= Files.list(out)){ return s.anyMatch(f -> !kept.contains(f.getFileName().toString())); }
    catch (IOException e){ throw new UncheckedIOException(e); }
  }
  private static boolean upToDate(Pkg p, List<Pkg> pkgs, Map<String,Map<String,String>> maps, Path out, Path baseCache){
    var json= out.resolve(p.name()+".json");
    var jar= stamp(out.resolve(p.name()+".jar"));
    if (!Files.exists(json) || jar < 0){ return false; }
    try{ return Api.parse(Files.readString(json)).inputsSnapshot().equals(snapshot(p, pkgs, maps, out, baseCache)) && jar >= p.files().stream().mapToLong(Ref::stamp).max().getAsLong(); }
    catch (IllegalArgumentException e){ return false; }
    catch (IOException e){ throw new UncheckedIOException(e); }
  }
  private static String snapshot(Pkg p, List<Pkg> pkgs, Map<String,Map<String,String>> maps, Path out, Path baseCache){
    var paths= p.files().stream().map(Ref::fearPath).sorted().toList();
    var lower= Stream.concat(Stream.of("base "+stamp(baseCache.resolve("base.jar"))), pkgs.stream().filter(q -> q.rank() < p.rank()).map(q -> q.name()+" "+stamp(out.resolve(q.name()+".jar")))).sorted().toList();
    var directives= maps.getOrDefault(p.name(), Map.of()).entrySet().stream().map(e -> e.getKey()+" "+e.getValue()).sorted().toList();
    return String.join("\n", paths)+"\n---\n"+String.join("\n", lower)+"\n---\n"+String.join("\n", directives);
  }
  /// target package to written name to package: of each map directive, the one of the rank file of highest rank
  private static Map<String,Map<String,String>> maps(List<Pkg> pkgs){
    var res= new HashMap<String,Map<String,String>>();
    pkgs.stream().sorted(Comparator.comparingInt(Pkg::rank)).flatMap(Cached::directives).forEach(d -> res.computeIfAbsent(d.get(2), k -> new HashMap<>()).put(d.get(0), d.get(1)));
    return res;
  }
  /// [in, out, target] of each `map in as _out in _target` of the rank file of the package
  private static Stream<List<String>> directives(Pkg p){
    var text= p.files().stream().filter(r -> r.name().startsWith("_rank_")).findFirst().orElseThrow().text();
    var items= Tokens.group(Tokens.tokens(text)).items;
    return IntStream.range(0, items.size()-5).filter(j -> map(items, j))
      .mapToObj(j -> List.of(Tokens.text(items.get(j+1)), Tokens.text(items.get(j+3)).substring(1), Tokens.text(items.get(j+5)).substring(1)));
  }
  private static boolean map(List<Item> items, int j){
    return word(items.get(j), "map") && Tokens.is(items.get(j+1), Kind.LowercaseId) && word(items.get(j+2), "as") && folder(items.get(j+3)) && word(items.get(j+4), "in") && folder(items.get(j+5));
  }
  private static boolean word(Item it, String w){ return Tokens.is(it, Kind.LowercaseId) && Tokens.text(it).equals(w); }
  private static boolean folder(Item it){ return Tokens.is(it, Kind.LowercaseId) && Tokens.text(it).startsWith("_"); }
  private static List<Pkg> packages(Path folder){
    var byPkg= new LinkedHashMap<String,List<Ref>>();
    for (var r : refs(folder)){
      var pkg= pkg(r);
      if (pkg.isEmpty() && r.name().endsWith(".fear")){ throw new Invalid(); }
      pkg.ifPresent(n -> byPkg.computeIfAbsent(n, k -> new ArrayList<>()).add(r));
    }
    return byPkg.entrySet().stream().map(e -> new Pkg(e.getKey(), rank(e.getValue()), e.getValue())).toList();
  }
  private static Optional<String> pkg(Ref r){
    var candidates= r.segments().stream().filter(s -> s.startsWith("_") && !s.contains(".")).toList();
    if (candidates.size() > 1){ throw new Invalid(); }
    return candidates.stream().findAny().map(s -> s.substring(1));
  }
  private static int rank(List<Ref> files){
    var reserved= files.stream().filter(r -> r.name().startsWith("_rank_")).toList();
    if (reserved.size() != 1){ throw new Invalid(); }
    var name= reserved.getFirst().name();
    if (!name.endsWith(".fear")){ throw new Invalid(); }
    var stem= name.substring(0, name.indexOf('.'));
    for (int i= 0; i < ranks.size(); i += 1){
      var pref= ranks.get(i);
      if (stem.equals(pref)){ return (i+1)*1000+999; }
      if (!stem.startsWith(pref)){ continue; }
      var digits= stem.substring(pref.length());
      if (!digits.matches("[0-9]{3}")){ throw new Invalid(); }
      return (i+1)*1000+Integer.parseInt(digits);
    }
    throw new Invalid();
  }
  private static List<Ref> refs(Path folder){
    var res= new ArrayList<Ref>();
    try (var all= Files.walk(folder)){
      for (var p : all.filter(p -> !Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS)).toList()){
        var segments= StreamSupport.stream(folder.relativize(p).spliterator(), false).map(Path::toString).toList();
        if (invisible(segments)){ continue; }
        if (!Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS)){ throw new Invalid(); }
        dated(stamp(p));
        if (!segments.getLast().endsWith(".zip")){ res.add(new Ref(segments, stamp(p), segments.getLast().startsWith("_rank_") ? Files.readString(p) : "")); continue; }
        try (var in= Files.newInputStream(p)){ zip(unzipped(segments), in, stamp(p), res); }
      }
    }
    catch (IOException|UncheckedIOException e){ throw new Invalid(); }
    res.removeIf(r -> invisible(r.segments()));
    return res;
  }
  private static void zip(List<String> at, InputStream in, long stamp, ArrayList<Ref> res) throws IOException{
    var zip= new ZipInputStream(in, StandardCharsets.UTF_8);
    for (var e= zip.getNextEntry(); e != null; e = zip.getNextEntry()){
      if (e.isDirectory()){ continue; }
      var segments= Stream.concat(at.stream(), Stream.of(e.getName().split("/"))).toList();
      if (!invisible(segments)){ dated(e.getTime()); }
      if (e.getName().endsWith(".zip")){ zip(unzipped(segments), new ByteArrayInputStream(zip.readAllBytes()), stamp, res); continue; }
      res.add(new Ref(segments, stamp, segments.getLast().startsWith("_rank_") ? new String(zip.readAllBytes(), StandardCharsets.UTF_8) : ""));
    }
  }
  private static void dated(long t){ if (t <= 0 || t > System.currentTimeMillis()+5_000){ throw new Invalid(); } }
  private static List<String> unzipped(List<String> segments){
    var last= segments.getLast();
    return Stream.concat(segments.stream().limit(segments.size()-1), Stream.of(last.substring(0, last.length()-4))).toList();
  }
  private static boolean invisible(List<String> segments){ return segments.stream().anyMatch(s -> s.startsWith(".")); }
  private static long stamp(Path file){
    try{ return Files.getLastModifiedTime(file, LinkOption.NOFOLLOW_LINKS).toMillis(); }
    catch (NoSuchFileException e){ return -1; }
    catch (IOException e){ throw new UncheckedIOException(e); }
  }
}

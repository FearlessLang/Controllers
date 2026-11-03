package controller;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import core.TName;
import mainCoordinator.MakeDemo;
import realSourceOracle.BuildWithZip;
import realSourceOracle.PathEntry;
import tools.Fs;
import managerMessages.Report;
import userMessages.UserError;
import utils.OneOr;

/// A project is named by the stem of the first `.fearless` marker file in its folder.
public final class Names{
  public static final String ext= ".fearless";
  private static final Path anyRoot= Path.of("").toAbsolutePath();
  public static boolean isName(String name){
    PathEntry kid;
    try{ kid= new PathEntry(anyRoot, Path.of(name+ext)); }
    catch (InvalidPathException e){ return false; }
    if (kid.local().getNameCount() != 1 || kid.local().isAbsolute() || BuildWithZip.isInvisible(kid)){ return false; }
    try{ BuildWithZip.checkIndividualVisibleSegment(kid); return true; }
    catch (UserError e){ return false; }
  }
  public static Optional<String> markerProblem(Path folder, String alias){
    if (markers(folder).size() < 2){ return Optional.empty(); }
    return Optional.of(Report.markersMany(folder, alias+ext));
  }
  static boolean reachable(Path folder){ return Files.isDirectory(folder, LinkOption.NOFOLLOW_LINKS) && Files.isReadable(folder); }
  static String marker(Path folder){ return markers(folder).stream().findFirst().map(Names::stem).orElse(""); }
  static String infer(Path folder, Set<String> taken){
    var marked= marker(folder);
    return isName(marked) && !taken.contains(marked) ? marked : free(folder, folder.getFileName().toString(), taken);
  }
  public static String free(Path folder, String wanted, Set<String> taken){
    var lower= wanted.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]+", "_");
    var cut= lower.substring(0, Math.min(lower.length(), 64));
    var name= isName(cut) ? cut : ("p"+cut).substring(0, Math.min(cut.length()+1, 64));
    return IntStream.iterate(1, i -> i+1).mapToObj(i -> i == 1 ? name : name+i).filter(n -> isName(n) && isFree(folder, n, taken)).findFirst().orElseThrow();
  }
  private static boolean isFree(Path folder, String name, Set<String> taken){
    var moved= markers(folder).stream().limit(1).toList();
    return !taken.contains(name) && entries(folder).stream().filter(p -> !moved.contains(p)).noneMatch(p -> clashes(p, name));
  }
  private static boolean clashes(Path p, String name){
    var f= p.getFileName().toString();
    return f.equalsIgnoreCase(name+ext) || f.equalsIgnoreCase(name) && !Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS);
  }
  static void nameAs(Path folder, String name){
    assert isName(name);
    var target= folder.resolve(name+ext);
    var all= markers(folder);
    if (!all.isEmpty() && !all.getFirst().getFileName().toString().equals(name+ext)){
      var tmp= folder.resolve(UUID.randomUUID()+".tmp");
      Fs.ofV(() -> { Fs.move(all.getFirst(), tmp); Fs.move(tmp, target); });
    }
    if (all.isEmpty() || Fs.of(() -> Fs.size(target)) == 0){ Fs.writeUtf8(target, MakeDemo.markerContent); }
  }
  static List<Path> list(Path dir){ return Fs.of(() -> { try (var s= Fs.list(dir)){ return s.sorted(Comparator.comparing(p -> p.getFileName().toString())).toList(); } }); }
  static Optional<Path> child(Path dir, String name){ return OneOr.opt(name, list(dir).stream().filter(p -> p.getFileName().toString().equals(name))); }
  static Optional<Path> folder(Path dir, String name){ return child(dir, name).filter(Fs::isDirectory); }
  /// the top level of a project folder, symbolic links included: a link makes the project 'invalid', not unnamed
  private static List<Path> entries(Path dir){ return Fs.of(() -> { try (var s= Files.list(dir)){ return s.sorted(Comparator.comparing(p -> p.getFileName().toString())).toList(); } }); }
  private static List<Path> markers(Path folder){
    return entries(folder).stream().filter(p -> p.getFileName().toString().endsWith(ext) && Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS)).toList();
  }
  private static String stem(Path file){
    var name= file.getFileName().toString();
    return name.substring(0, name.length()-ext.length());
  }
}
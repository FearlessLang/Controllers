package controller;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import core.TName;
import mainCoordinator.MakeDemo;
import realSourceOracle.BuildWithZip;
import realSourceOracle.PathEntry;
import tools.Fs;
import managerMessages.Report;
import suggest.Shapes;
import userMessages.UserError;
import utils.OneOr;

/// A project is named by the stem of the first `.fearless` marker file in its folder.
public final class Names{
  private static final Path anyRoot= Path.of("").toAbsolutePath();
  public static boolean isName(String name){
    PathEntry kid;
    try{ kid= new PathEntry(anyRoot, Path.of(name+Shapes.ext)); }
    catch (InvalidPathException e){ return false; }
    if (kid.local().getNameCount() != 1 || kid.local().isAbsolute() || BuildWithZip.isInvisible(kid)){ return false; }
    try{ BuildWithZip.checkIndividualVisibleSegment(kid); return true; }
    catch (UserError e){ return false; }
  }
  public static Optional<String> markerProblem(Path folder, String alias){
    if (Shapes.markers(folder).size() < 2){ return Optional.empty(); }
    return Optional.of(Report.markersMany(folder, alias+Shapes.ext));
  }
  static void nameAs(Path folder, String name){
    assert isName(name);
    var target= folder.resolve(name+Shapes.ext);
    var all= Shapes.markers(folder);
    if (!all.isEmpty() && !all.getFirst().getFileName().toString().equals(name+Shapes.ext)){
      var tmp= folder.resolve(UUID.randomUUID()+".tmp");
      Fs.ofV(() -> { Fs.move(all.getFirst(), tmp); Fs.move(tmp, target); });
    }
    if (all.isEmpty() || Fs.of(() -> Fs.size(target)) == 0){ Fs.writeUtf8(target, MakeDemo.markerContent); }
  }
  static List<Path> list(Path dir){ return Fs.of(() -> { try (var s= Fs.list(dir)){ return s.sorted(Comparator.comparing(p -> p.getFileName().toString())).toList(); } }); }
  static Optional<Path> child(Path dir, String name){ return OneOr.opt(name, list(dir).stream().filter(p -> p.getFileName().toString().equals(name))); }
  static Optional<Path> folder(Path dir, String name){ return child(dir, name).filter(Fs::isDirectory); }
}
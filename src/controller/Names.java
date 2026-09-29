package controller;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import core.TName;
import mainCoordinator.MakeDemo;
import realSourceOracle.BuildWithZip;
import realSourceOracle.PathEntry;
import tools.Fs;
import userMessages.UserError;

/// A project is named by the stem of the one `.fearless` marker file in its folder.
public final class Names{
  private Names(){}
  public static final String ext= ".fearless";
  private static final Path anyRoot= Path.of("").toAbsolutePath();
  public static String compactName(Path folder){
    var all= markers(folder);
    return all.size() == 1 ? stem(all.getFirst()) : folder.getFileName().toString();
  }
  public static String pkgName(String alias){
    var s= alias.replaceFirst("^_+","");
    return TName.isPkgName(s) && !s.equals("base") && !s.equals("rank") ? s : "app_"+s;
  }
  public static boolean isName(String name){
    PathEntry kid;
    try{ kid= new PathEntry(anyRoot,Path.of(name+ext)); }
    catch(InvalidPathException e){ return false; }
    if (kid.local().getNameCount() != 1 || kid.local().isAbsolute() || BuildWithZip.isInvisible(kid)){ return false; }
    try{ BuildWithZip.checkIndividualVisibleSegment(kid); return true; }
    catch(UserError e){ return false; }
  }
  public static Optional<String> markerProblem(Path folder, String alias){
    var all= markers(folder);
    if (all.size() > 1){
      return Optional.of("More than one .fearless marker file was found in\n"+folder+"\nA project folder holds exactly one, and its name is the project name: keep only \""+alias+ext+"\".");
    }
    if (hasMarker(folder,alias)){ return Optional.empty(); }
    return Optional.of("The marker file \""+alias+ext+"\" is missing from\n"+folder+"\nRestore it, or forget and re-add this project folder.");
  }
  public static String makeUnique(Path folder, Set<String> taken){
    var all= markers(folder);
    var marked= all.size() == 1 ? stem(all.getFirst()) : "";
    var chosen= isName(marked) && !taken.contains(marked) ? marked : free(folder,folder.getFileName().toString(),taken);
    nameAs(folder,chosen);
    return chosen;
  }
  public static String free(Path folder, String wanted, Set<String> taken){
    var lower= wanted.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]+","_");
    var base= lower.substring(0,Math.min(lower.length(),64));
    var name= isName(base) ? base : "p"+base;
    return IntStream.iterate(1,i->i+1).mapToObj(i->i == 1 ? name : name+i).filter(n->isName(n) && isFree(folder,n,taken)).findFirst().orElseThrow();
  }
  private static boolean isFree(Path folder, String name, Set<String> taken){
    return !taken.contains(name) && Fs.of(()->{ try(var s= Files.list(folder)){ return s.noneMatch(p->p.getFileName().toString().equalsIgnoreCase(name+ext)); } });
  }
  private static boolean hasMarker(Path folder, String name){ return Files.exists(folder.resolve(name+ext)); }
  private static void nameAs(Path folder, String name){
    assert isName(name);
    var target= folder.resolve(name+ext);
    var all= markers(folder);
    if (!Files.exists(target) && !all.isEmpty()){ Fs.ofV(()->Files.move(all.getFirst(),target)); }
    if (!Files.exists(target) || Fs.of(()->Files.size(target)) == 0){ Fs.writeUtf8(target,MakeDemo.markerContent); }
  }
  private static List<Path> markers(Path folder){
    if (!Files.isDirectory(folder)){ return List.of(); }
    return Fs.of(()->{ try(var s= Files.list(folder)){ return s
      .filter(Files::isRegularFile)
      .filter(p->p.getFileName().toString().endsWith(ext))
      .sorted()
      .toList();
    }});
  }
  private static String stem(Path file){
    var name= file.getFileName().toString();
    return name.substring(0,name.length()-ext.length());
  }
}
package controller;

import java.io.ByteArrayInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import javax.imageio.ImageIO;

import controller.Registry.Kind;
import coordinator.Coordinator;
import fileSupport.LogFiles;
import realSourceOracle.RealSourceOracleWithZip;
import tools.Fs;
import managerMessages.Report;
import userMessages.UserError;

/// What the file system says about a project folder: its compiled cache under
/// `.fearless_out` and why it is out of date, its icon, its logs, and why it is invalid if it is.
public record Facts(boolean hasCache, Optional<String> stale, Optional<Icon> icon, List<LogFiles.Entry> logs, Optional<String> problem){
  public static final String outDir= Coordinator.outDir;
  /// An icon is the same while its file is: the image is read from it once.
  public record Icon(Path file, long stamp, BufferedImage image){
    @Override public boolean equals(Object o){ return o instanceof Icon i && file.equals(i.file) && stamp == i.stamp; }
    @Override public int hashCode(){ return file.hashCode(); }
  }
  private static final Map<Path,Icon> decoded= new ConcurrentHashMap<>();
  public static Facts of(Path folder, String alias, Kind kind, Path baseCache){
    try{ return retried(() -> read(folder, alias, kind, baseCache)); }
    catch (UncheckedIOException e){ return invalid(Report.folderUnreadable(folder, e.getCause())); }
  }
  private static Facts invalid(String problem){ return new Facts(false, Optional.empty(), Optional.empty(), List.of(), Optional.of(problem)); }
  static <T> T retried(Supplier<T> read){
    for (var left= 10;; left -= 1){
      try{ return read.get(); }
      catch (UncheckedIOException e){ if (left == 0 || !(e.getCause() instanceof NoSuchFileException)){ throw e; } }
    }
  }
  private static Facts read(Path folder, String alias, Kind kind, Path baseCache){
    if (!Fs.isDirectory(folder)){ return invalid(Report.folderMissing(folder)); }
    UserError.root = folder;
    Map<String,Boolean> built= Map.of();
    Optional<String> problem;
    try{
      if (kind == Kind.code){ built = Coordinator.pkgsBuilt(folder, baseCache); }
      else{ new RealSourceOracleWithZip(folder); }
      problem= Names.markerProblem(folder, alias);
    }
    catch (UserError e){ problem = Optional.of(e.getMessage()); }
    Optional<Icon> icon= Optional.empty();
    try{ icon = icon(folder); }
    catch (UserError e){ if (problem.isEmpty()){ problem = Optional.of(e.getMessage()); } }
    var out= folder.resolve(outDir);
    var hasCache= Fs.isDirectory(out) && Names.list(out).stream().anyMatch(p -> p.getFileName().toString().endsWith(".built"));
    var stalePkgs= built.entrySet().stream().filter(e -> !e.getValue()).map(Map.Entry::getKey).toList();
    var stale= kind != Kind.code ? Optional.<String>empty()
      : !hasCache || built.isEmpty() ? Optional.of(Report.noCache())
      : stalePkgs.isEmpty() ? Optional.<String>empty() : Optional.of(Report.stalePackages(stalePkgs));
    return new Facts(hasCache, stale, icon, LogFiles.list(folder), problem);
  }
  Facts outOfDate(String why){ return new Facts(hasCache, Optional.of(why), icon, logs, problem); }
  static Optional<Icon> icon(Path folder){
    var dir= Names.folder(folder, ".config").flatMap(c -> Names.folder(c, "icon"));
    if (dir.isEmpty()){ return Optional.empty(); }
    var pngs= Names.list(dir.get()).stream().filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png") && Fs.isRegularFile(p)).toList();
    if (pngs.size() > 1){ throw Report.projectIconsMany(dir.get(), pngs); }
    if (pngs.isEmpty()){ return Optional.empty(); }
    var png= pngs.getFirst();
    var stamp= Fs.lastModified(png);
    var known= decoded.get(png);
    if (known != null && known.stamp() == stamp){ return Optional.of(known); }
    BufferedImage image;
    try{ image= ImageIO.read(new ByteArrayInputStream(Fs.readAllBytes(png))); }
    catch (IOException e){ throw Report.projectIconUnreadable(png); }
    if (image == null){ throw Report.projectIconUnreadable(png); }
    var res= new Icon(png, stamp, image);
    decoded.put(png, res);
    return Optional.of(res);
  }
}

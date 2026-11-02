package controller;

import java.io.ByteArrayInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.imageio.ImageIO;

import controller.Registry.Kind;
import coordinator.Coordinator;
import coordinator.MainsInfo;
import fileSupport.LogFiles;
import realSourceOracle.RealSourceOracleWithZip;
import tools.Fs;
import managerMessages.Report;
import userMessages.UserError;

/// What the file system says about a project folder: its compilation cache under
/// `.fearless_out` and why it is out of date, its icon, its logs, and why it is invalid if it is.
public record Facts(Optional<String> stale, Optional<MainsInfo> mains, List<String> packages, Optional<Icon> icon, List<LogFiles.Entry> logs, Optional<String> problem){
  public static final String outDir= Coordinator.outDir;
  /// An icon is the same while its file is: the image is read from it once.
  public record Icon(Path file, long stamp, BufferedImage image){
    @Override public boolean equals(Object o){ return o instanceof Icon i && file.equals(i.file) && stamp == i.stamp; }
    @Override public int hashCode(){ return file.hashCode(); }
  }
  private static final Map<Path,Icon> decoded= new ConcurrentHashMap<>();
  /// A Coordinator.State that is Error.cacheCorrupted is raised: the manager applies the corruption rule.
  /// None for a folder the manager can not reach.
  public static Optional<Facts> of(Path folder, String alias, Kind kind, Function<Path,Coordinator.State> check){
    if (!Fs.isDirectory(folder) || !Files.isReadable(folder)){ return Optional.empty(); }
    try{ return Optional.of(retried(() -> read(folder, alias, kind, check))); }
    catch (UncheckedIOException e){ return Optional.of(new Facts(Optional.empty(), Optional.empty(), List.of(), Optional.empty(), List.of(), Optional.of(Report.folderUnreadable(folder, e.getCause())))); }
  }
  static <T> T retried(Supplier<T> read){
    for (var left= 10;; left -= 1){
      try{ return read.get(); }
      catch (UncheckedIOException e){ if (left == 0 || !(e.getCause() instanceof NoSuchFileException)){ throw e; } }
    }
  }
  private static Facts read(Path folder, String alias, Kind kind, Function<Path,Coordinator.State> check){
    UserError.root = folder;
    Optional<String> problem;
    Coordinator.State state= new Coordinator.State.OutOfDate();
    try{
      problem= Names.markerProblem(folder, alias);
      if (kind != Kind.code){ new RealSourceOracleWithZip(folder); }
    }
    catch (UserError e){ problem = Optional.of(e.getMessage()); }
    Optional<Icon> icon= Optional.empty();
    try{ icon = icon(folder); }
    catch (UserError e){ if (problem.isEmpty()){ problem = Optional.of(e.getMessage()); } }
    if (kind == Kind.code && problem.isEmpty()){ state = check.apply(folder); }
    if (state instanceof Coordinator.State.Invalid(var why)){ problem = Optional.of(why); }
    var compiled= state instanceof Coordinator.State.Compiled c ? Optional.of(c) : Optional.<Coordinator.State.Compiled>empty();
    var stale= kind == Kind.code && problem.isEmpty() && compiled.isEmpty() ? Optional.of(Report.outOfDate()) : Optional.<String>empty();
    return new Facts(stale, compiled.map(Coordinator.State.Compiled::manifest), compiled.map(Coordinator.State.Compiled::packages).orElse(List.of()), icon, LogFiles.list(folder), problem);
  }
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

package controller;

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

import javax.imageio.ImageIO;

import controller.Registry.Kind;
import coordinator.Coordinator;
import fileSupport.LogFiles;
import realSourceOracle.RealSourceOracleWithZip;
import tools.Fs;
import userMessages.UserError;

/// What the file system says about a project folder: its authored files, its compiled
/// cache under `.fearless_out`, its icon, its logs, and why it is invalid if it is.
public record Facts(int files, long bytes, long modified, List<String> pkgs, boolean hasCache, boolean upToDate, Optional<Icon> icon, List<LogFiles.Entry> logs, Optional<String> problem){
  public static final String outDir= Coordinator.outDir;
  /// An icon is the same while its file is: the image is read from it once.
  public record Icon(Path file, long stamp, BufferedImage image){
    @Override public boolean equals(Object o){ return o instanceof Icon i && file.equals(i.file) && stamp == i.stamp; }
    @Override public int hashCode(){ return file.hashCode(); }
  }
  public static Facts of(Path folder, String alias, Kind kind){
    if (!Files.isDirectory(folder)){
      return new Facts(0,0,-1,List.of(),false,false,Optional.empty(),List.of(),Optional.of("The folder of this project does not exist:\n"+folder+"\nRestore it, or forget this project."));
    }
    UserError.root= folder;
    while(true){
      try{ return read(folder,alias,kind); }
      catch(UncheckedIOException e){ if (!(e.getCause() instanceof NoSuchFileException)){ throw e; } }
    }
  }
  private static Facts read(Path folder, String alias, Kind kind){
    var src= sources(folder);
    Map<String,Boolean> built= Map.of();
    Optional<Icon> icon= Optional.empty();
    Optional<String> problem;
    try{
      if (kind == Kind.code){ built= Coordinator.pkgsBuilt(folder); } else { new RealSourceOracleWithZip(folder); }
      icon= icon(folder);
      problem= Names.markerProblem(folder,alias);
    }
    catch(UserError e){ problem= Optional.of(e.getMessage()); }
    var upToDate= !built.isEmpty() && !built.containsValue(false);
    var modified= src.stream().mapToLong(Fs::lastModified).max().orElse(-1);
    var bytes= src.stream().mapToLong(p->Fs.of(()->Files.size(p))).sum();
    return new Facts(src.size(),bytes,modified,List.copyOf(built.keySet()),Files.isDirectory(folder.resolve(outDir)),upToDate,icon,LogFiles.list(folder),problem);
  }
  Facts outOfDate(Optional<String> error){ return new Facts(files,bytes,modified,pkgs,hasCache,false,icon,logs,problem.or(()->error)); }
  static Optional<Icon> icon(Path folder){
    var dir= folder.resolve(".config").resolve("icon");
    if (!Files.isDirectory(dir)){ return Optional.empty(); }
    var pngs= Fs.of(()->{ try(var s= Files.list(dir)){ return s
      .filter(Files::isRegularFile)
      .filter(p->p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
      .sorted()
      .toList();
    }});
    if (pngs.size() > 1){ throw Messages.projectIconsMany(dir,pngs); }
    if (pngs.isEmpty()){ return Optional.empty(); }
    var png= pngs.getFirst();
    BufferedImage image;
    try{ image= ImageIO.read(png.toFile()); }
    catch(IOException e){ throw Messages.projectIconUnreadable(png); }
    if (image == null){ throw Messages.projectIconUnreadable(png); }
    return Optional.of(new Icon(png,Fs.lastModified(png),image));
  }
  private static List<Path> sources(Path folder){
    var cache= folder.resolve(outDir);
    var written= folder.resolve(LogFiles.runDir);
    return Fs.walk(folder,s->s.filter(p->!p.startsWith(cache) && !p.startsWith(written)).filter(Files::isRegularFile).toList());
  }
}

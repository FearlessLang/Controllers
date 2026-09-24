package controller;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
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
import userMessages.Report;
import userMessages.UserError;

/// What the file system says about a project folder: its authored files, and the
/// compiled cache Fearless keeps for it under `.fearless_out`.
public record Facts(Path folder, int files, long bytes, long modified, List<String> pkgs, boolean cacheUpToDate, Optional<BufferedImage> icon, Optional<String> problem){
  public static final String outDir= Coordinator.outDir;
  public boolean valid(){ return problem.isEmpty(); }
  public boolean hasCache(){ return Files.isDirectory(folder.resolve(outDir)); }
  public static Facts of(Path folder, Kind kind){
    var f= folder.toAbsolutePath().normalize();
    var src= sources(f);
    Map<String,Boolean> built= Map.of();
    Optional<BufferedImage> icon= Optional.empty();
    Optional<String> problem= Optional.empty();
    UserError.root= f;
    try{
      icon= icon(f);
      if (kind == Kind.code){ built= Coordinator.pkgsBuilt(f); } else { new RealSourceOracleWithZip(f); }
    }
    catch(UserError e){ problem= Optional.of(e.getMessage()); }
    var upToDate= !built.isEmpty() && !built.containsValue(false);
    var modified= src.stream().mapToLong(Fs::lastModified).max().orElse(-1);
    return new Facts(f,src.size(),src.stream().mapToLong(p->Fs.of(()->Files.size(p))).sum(),modified,List.copyOf(built.keySet()),upToDate,icon,problem);
  }
  static Optional<BufferedImage> icon(Path folder){
    var dir= folder.resolve(".config").resolve("icon");
    if (!Files.isDirectory(dir)){ return Optional.empty(); }
    var pngs= Fs.of(()->{ try(var s= Files.list(dir)){ return s
      .filter(Files::isRegularFile)
      .filter(p->p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
      .sorted()
      .toList();
    }});
    if (pngs.size() > 1){ throw Report.projectIconsMany(dir,pngs); }
    if (pngs.isEmpty()){ return Optional.empty(); }
    var png= pngs.getFirst();
    BufferedImage res;
    try{ res= ImageIO.read(png.toFile()); }
    catch(IOException e){ throw Report.projectIconUnreadable(png); }
    if (res == null){ throw Report.projectIconUnreadable(png); }
    return Optional.of(res);
  }
  private static List<Path> sources(Path folder){
    var cache= folder.resolve(outDir);
    var written= folder.resolve(LogFiles.runDir);
    return Fs.walk(folder,s->s.filter(p->!p.startsWith(cache) && !p.startsWith(written)).filter(Files::isRegularFile).toList());
  }
}
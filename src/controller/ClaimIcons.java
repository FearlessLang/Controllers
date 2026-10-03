package controller;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import coordinator.MainsInfo;
import fileAssociations.Ico;
import realSourceOracle.ZipLocator;
import tools.Fs;

final class ClaimIcons{
  private static final byte[] pngHead= {(byte)0x89,'P','N','G','\r','\n',0x1A,'\n',0,0,0,13,'I','H','D','R'};
  static Map<String,byte[]> read(Path project, Path base, MainsInfo info){
    var res= new LinkedHashMap<String,byte[]>();
    for (var e: info.mains().entrySet()){
      for (var c: e.getValue().shortcuts()){ res.computeIfAbsent(c.icon(),_->icon(project,base,e.getKey(),true,c)); }
      for (var c: e.getValue().openWiths()){ res.computeIfAbsent(c.icon(),_->icon(project,base,e.getKey(),false,c)); }
    }
    return res;
  }
  private static byte[] icon(Path project, Path base, String main, boolean shortcut, MainsInfo.Claim c){
    var file= (c.icon().startsWith("base.") ? base : project).resolve(c.diskPath());
    var steps= c.zipSteps().isEmpty() ? List.<String>of() : List.of(c.zipSteps().split(";"));
    byte[] bytes;
    try{ bytes= c.zipEntry().isEmpty() ? Fs.of(()->Files.readAllBytes(file)) : ZipLocator.entryBytes(file,steps,c.zipEntry()); }
    catch(UncheckedIOException e){ throw Messages.iconUnreadable(main,shortcut,c,e.getCause()); }
    if (bytes.length < 24 || !Arrays.equals(bytes,0,16,pngHead,0,16)){ throw Messages.iconRefused(main,shortcut,c,"is not a PNG image"); }
    var w= ByteBuffer.wrap(bytes).getInt(16);
    var h= ByteBuffer.wrap(bytes).getInt(20);
    if (w != h || w < 64 || w > 1024){ throw Messages.iconRefused(main,shortcut,c,"is "+w+"x"+h+" pixels"); }
    if (image(bytes) == null){ throw Messages.iconRefused(main,shortcut,c,"is not a PNG image"); }
    return bytes;
  }
  private static BufferedImage image(byte[] bytes){
    try{ return ImageIO.read(new ByteArrayInputStream(bytes)); }
    catch(IOException e){ return null; }
  }
  static void materialise(Path dir, Map<String,byte[]> icons){ icons.forEach((icon,bytes)->write(dir,icon,bytes)); }
  private static void write(Path dir, String icon, byte[] bytes){
    var png= dir.resolve(icon+".png");
    if (Files.exists(png) && Arrays.equals(Fs.of(()->Files.readAllBytes(png)),bytes)){ return; }
    Fs.ensureDir(dir);
    Fs.ofV(()->Files.write(png,bytes));
    if (Fs.isWindows()){ Ico.fromPng(png,dir.resolve(icon+".ico")); }
  }
}

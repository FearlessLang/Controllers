package controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import fileAssociations.Ico;
import realSourceOracle.ZipLocator;
import tools.Fs;
import managerMessages.Report;

final class ClaimIcons{
  private static final byte[] pngHead= {(byte)0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 13, 'I', 'H', 'D', 'R'};
  static Map<String,byte[]> read(Path base, Stream<Project.Claimant> cs, Function<RuntimeException,RuntimeException> unreadable){
    var res= new LinkedHashMap<String,byte[]>();
    cs.forEach(c -> res.computeIfAbsent(c.claim().icon(), _ -> icon(base, c, unreadable)));
    return res;
  }
  private static byte[] icon(Path base, Project.Claimant claimant, Function<RuntimeException,RuntimeException> unreadable){
    var c= claimant.claim();
    var file= (c.icon().startsWith("base.") ? base : claimant.folder()).resolve(c.diskPath());
    var steps= c.zipSteps().isEmpty() ? List.<String>of() : List.of(c.zipSteps().split(";"));
    byte[] bytes;
    try{ bytes= c.zipEntry().isEmpty() ? Fs.of(() -> Fs.readAllBytes(file)) : ZipLocator.entryBytes(file, steps, c.zipEntry()); }
    catch (RuntimeException e){ throw unreadable.apply(e); }
    if (bytes.length < 24 || !Arrays.equals(bytes, 0, 16, pngHead, 0, 16)){ throw Report.iconRefused(claimant, "is not a PNG image"); }
    var w= ByteBuffer.wrap(bytes).getInt(16);
    var h= ByteBuffer.wrap(bytes).getInt(20);
    if (w != h || w < 64 || w > 1024){ throw Report.iconRefused(claimant, "is "+w+"x"+h+" pixels"); }
    if (!decodes(bytes)){ throw Report.iconRefused(claimant, "is not a PNG image"); }
    return bytes;
  }
  private static boolean decodes(byte[] bytes){
    try{ return ImageIO.read(new ByteArrayInputStream(bytes)) != null; }
    catch (IOException e){ return false; }
  }
  static void materialise(Path dir, String icon, byte[] bytes){
    var png= dir.resolve(icon+".png");
    var ico= dir.resolve(icon+".ico");
    var same= Fs.exists(png) && (!Fs.isWindows() || Fs.exists(ico)) && Arrays.equals(Fs.of(() -> Fs.readAllBytes(png)), bytes);
    if (same){ return; }
    Fs.ensureDir(dir);
    Fs.ofV(() -> Fs.write(png, bytes));
    if (Fs.isWindows()){ Ico.fromPng(png, ico); }
  }
}

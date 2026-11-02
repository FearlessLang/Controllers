package controller;

import java.nio.file.Path;
import java.util.Arrays;

import fileAssociations.Ico;
import tools.Fs;

final class ClaimIcons{
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

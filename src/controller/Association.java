package controller;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import fileAssociations.LinuxAssociations;
import tools.JavacTool;
import tools.PathClassLoader;
import userMessages.Violation;

/// Makes this Fearless the program the desktop opens `.fearless` files with.
public final class Association{
  static Path launcher(){
    var app= System.getProperty("jpackage.app-path");
    if (app == null){ throw Violation.mustUseLauncher(); }
    return Path.of(app);
  }
  public static Path iconFile(){ return JavacTool.reqAppDir(Violation::mustUseLauncher).resolve("icon.png"); }
  static String deskId(Path folder, String main){ return mainCoordinator.Main.identity(launcher())+"-"+LinuxAssociations.hash((folder+"\n"+main).getBytes(StandardCharsets.UTF_8)); }
  static List<String> desk(Path folder, String main){
    return List.of("-DfearlessDesk.id="+deskId(folder, main), "-DfearlessDesk.icons="+PathClassLoader.argOf(folder.resolve(Facts.outDir).resolve("icons")), "-DfearlessDesk.relaunch=\""+launcher()+"\" \""+folder+"\"", "-DfearlessManager.launcher="+launcher());
  }
}
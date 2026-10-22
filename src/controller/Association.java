package controller;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import fileAssociations.FileAssociations;
import fileAssociations.Icon;
import fileAssociations.LinuxAssociations;
import fileAssociations.WindowIcon;
import tools.Fs;
import tools.JavacTool;
import userMessages.Violation;

/// Makes this Fearless the program the desktop opens `.fearless` files with.
public final class Association{
  static final Predicate<String> belongsToFamily= s -> s.contains("earless");
  static Path launcher(){
    var app= System.getProperty("jpackage.app-path");
    if (app == null){ throw Violation.mustUseLauncher(); }
    return Path.of(app);
  }
  ///The launcher names us: one identity per Fearless, and the manager's own is not the portable one.
  static String identity(Path launcher){
    var file= launcher.getFileName().toString();
    var dot= file.lastIndexOf('.');
    return dot < 0 ? file : file.substring(0, dot);
  }
  public static Path iconFile(){ return JavacTool.reqAppDir(Violation::mustUseLauncher).resolve("icon.png"); }
  static String deskId(Path folder, String main){ return identity(launcher())+"-"+LinuxAssociations.hash((folder+"\n"+main).getBytes(StandardCharsets.UTF_8)); }
  static List<String> desk(Path folder, String main){
    return List.of("-DfearlessDesk.id="+deskId(folder, main), "-DfearlessDesk.icons="+folder.resolve(Facts.outDir).resolve("icons"), "-DfearlessDesk.relaunch=\""+launcher()+"\" \""+folder+"\"", "-DfearlessManager.launcher="+launcher());
  }
  static void reconcile(Path launcher, List<Icon> extensions, List<WindowIcon> windows, Function<String,String> claimedBy){
    FileAssociations.reconcile(identity(launcher), belongsToFamily, launcher, extensions, windows, launcher, iconFile(),
      reported -> Violation.associationsAmbiguous(reported).withRecovery("Remove all Fearless registrations", Association::eradicateAll),
      locked -> Fs.isWindows() ? Violation.associationUserLocked(locked, claimedBy) : Violation.associationChosenDefault(locked, claimedBy),
      held -> Violation.associationNotOurs(held, claimedBy),
      Violation::associationNotWritable,
      Violation::associationLeftHalfDone);
  }
  static List<String> others(){
    var mine= identity(launcher());
    return FileAssociations.identities(belongsToFamily).stream().filter(i -> !i.equals(mine)).toList();
  }
  static void eradicateAll(){ FileAssociations.eradicateAll(belongsToFamily, Violation::associationLeftHalfDone); }
}
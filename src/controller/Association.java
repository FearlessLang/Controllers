package controller;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

import fileAssociations.FileAssociations;
import fileAssociations.Icon;
import fileAssociations.LinuxAssociations;
import fileAssociations.WindowIcon;
import tools.Fs;
import tools.JavacTool;
import tools.PathClassLoader;
import userMessages.Violation;

/// Makes this Fearless the program the desktop opens `.fearless` files with.
public final class Association{
  static final Predicate<String> belongsToFamily= s -> s.contains("earless");
  static Path launcher(){
    var app= System.getProperty("jpackage.app-path");
    if (app == null){ throw Violation.mustUseLauncher(); }
    return Path.of(app);
  }
  ///The launcher names us: one identity per launcher, and the manager's own is not the portable one.
  static String identity(Path launcher){
    var file= launcher.getFileName().toString();
    var dot= file.lastIndexOf('.');
    return dot < 0 ? file : file.substring(0, dot);
  }
  public static Path iconFile(){ return JavacTool.reqAppDir(Violation::mustUseLauncher).resolve("icon.png"); }
  static String deskId(Path folder, String main){ return identity(launcher())+"-"+LinuxAssociations.hash((folder+"\n"+main).getBytes(StandardCharsets.UTF_8)); }
  static List<String> desk(Path folder, String main){
    return List.of("-DfearlessDesk.id="+deskId(folder, main), "-DfearlessDesk.icons="+PathClassLoader.argOf(folder.resolve(Facts.outDir).resolve("icons")), "-DfearlessDesk.relaunch=\""+launcher()+"\" \""+folder+"\"", "-DfearlessManager.launcher="+launcher());
  }
  static void reconcile(Path launcher, List<Icon> extensions, List<WindowIcon> windows, Function<String,String> claimedBy){
    FileAssociations.reconcile(identity(launcher), belongsToFamily, launcher, extensions, windows, iconFile(),
      c -> JavacTool.managerFolderOf(c).filter(d -> !JavacTool.managerFolderOf(launcher).equals(Optional.of(d))).ifPresent(Association::removeAtOnce),
      Violation::associationsAmbiguous,
      choices -> Violation.associationChosenByHand(choices, claimedBy).withRecovery("Remove these choices", () -> FileAssociations.forgetChoices(choices, Violation::associationNotWritable)),
      held -> new NotOurs(held, claimedBy),
      Violation::associationNotWritable,
      Violation::associationLeftHalfDone);
  }
  /// The manager of that folder never sees the folder deleted in part: the folder is renamed in its folder, then deleted.
  private static void removeAtOnce(Path dir){
    if (!Fs.exists(dir)){ return; }
    var gone= dir.resolveSibling(dir.getFileName()+"_"+UUID.randomUUID().toString().replace("-", ""));
    Fs.ofV(() -> Fs.move(dir, gone, StandardCopyOption.ATOMIC_MOVE));
    Fs.rmTree(gone);
  }
  static void eradicateAll(){ FileAssociations.eradicateAll(belongsToFamily, Violation::associationLeftHalfDone); }
}
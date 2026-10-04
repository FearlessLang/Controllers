package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;

import tools.Fs;

/// The folder of a shown project going away makes the project invalid: the manager notices it by itself, keeps the project registered, and its Error report says the folder does not exist; once the folder is back the tile is back by itself exactly as before.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, nothing is registered for .fearless, the folder gone beside the manager holds only its marker and one source file, and nothing is at gone2 beside it.
/// Action 1: run the launcher on gone: the manager window opens showing gone, an idle project.
/// Action 2: rename the folder gone to gone2: the tile of gone changes by itself.
/// Action 3: choose Error report in its Project menu: a dialog opens.
/// Action 4: click in its text, select all of it and copy it: the text copied says the folder of the project does not exist, names it, and says to restore it or forget the project.
/// Action 5: press OK: the dialog goes away; projects.info is as it was after action 1 and nothing was made at gone.
/// Action 6: rename gone2 back to gone: the tile is back by itself exactly as it was after action 1.
/// Action 7: end the manager.
final class FolderGoneTest extends ManagerTest{
  static final Path gone= data.resolveSibling("gone");
  static final Path moved= data.resolveSibling("gone2");
  final At managerShown= new At("managerShown",on("ubuntu-gnome",3000));
  final Area tile= new Area("tile",on("ubuntu-gnome",74,149,128,88));
  final Area dialog= new Area("dialog",on("ubuntu-gnome",1600,840,700,30));
  final Click projectMenu= new Click("projectMenu",on("ubuntu-gnome",158,79));
  final Click errorReport= new Click("errorReport",on("ubuntu-gnome",180,212));
  final Click focusReport= new Click("focusReport",on("ubuntu-gnome",1970,1100));
  final Click ok= new Click("ok",on("ubuntu-gnome",1953,1323));
  @Override protected void walk() throws Exception{
    clean();
    Fs.writeUtf8(gone.resolve("gone.fearless"),"\n");
    Fs.writeUtf8(gone.resolve("_gone").resolve("_rank_app.fear"),"use base.Main as Main;\n\nHello:Main{s->base.Debug#(\"gone\")}\n");
    launch(gone.toString());
    managerShown.go();
    look();
    var cell= tile.aim();
    var first= pixels(cell);
    var registered= Fs.readUtf8(info);
    Files.move(gone,moved);
    until(()->!Arrays.equals(first,pixels(cell)));
    var where= dialog.aim();
    var behind= pixels(where);
    projectMenu.go();
    errorReport.go();
    until(()->!Arrays.equals(behind,pixels(where)));
    focusReport.go();
    pilot.chord(KeyEvent.VK_CONTROL,KeyEvent.VK_A);
    pilot.chord(KeyEvent.VK_CONTROL,KeyEvent.VK_C);
    assertEquals("""
      The folder of this project does not exist:
      %s
      Restore it, or forget this project.""".formatted(gone),Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor));
    ok.go();
    until(()->Arrays.equals(behind,pixels(where)));
    assertEquals(registered,Fs.readUtf8(info));
    assertFalse(Files.exists(gone));
    Files.move(moved,gone);
    until(()->Arrays.equals(first,pixels(cell)));
    stopManagers();
  }
  @Override @AfterEach void clean() throws Exception{
    super.clean();
    Fs.rmTree(gone);
    Fs.rmTree(moved);
  }
}

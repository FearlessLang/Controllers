package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;

import tools.Fs;

/// Renaming a project in the metadata editor keeps its Output under the new name, and makes the project invalid until the marker file in its folder carries the new name too: its Error report says which marker is missing, and once the marker is renamed the project is valid again by itself.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, nothing is registered for .fearless, and the folder old beside the manager holds only its marker old.fearless and one source file.
/// Action 1: run the launcher on old: the manager window opens showing old, an idle project.
/// Action 2: click the empty space below the tiles, and move the divider between tiles and panel as far left as it goes with the keyboard.
/// Action 3: press Check: the Output says no problem was found.
/// Action 4: choose Edit project metadata... in its Manager menu: the metadata editor opens.
/// Action 5: double click the name old in the text, type fresh over it and press Commit: the manager remembers the folder as fresh, the console file of fresh holds the Output, the top of the panel changes, and the Output is shown exactly as before.
/// Action 6: choose Error report in its Project menu: a dialog opens.
/// Action 7: click in its text, select all of it and copy it: the text copied says the marker file fresh.fearless is missing from the folder, names it, and says to restore it or forget and re-add the folder.
/// Action 8: press OK: the dialog goes away.
/// Action 9: open the Information section.
/// Action 10: rename the marker old.fearless to fresh.fearless: the Information section changes by itself.
/// Action 11: press Check: the Output says no problem was found a second time, and so does the console file of fresh.
/// Action 12: end the manager.
final class EditMetadataRenameTest extends ManagerTest{
  static final Path old= data.resolveSibling("old");
  static final Path console= data.resolve("eclipse").resolve("fresh").resolve("console.txt");
  static final String fine= "--- ok: no problem found ---\n";
  final At managerShown= new At("managerShown",linux(3000),windows(3000));
  final Click focusTiles= new Click("focusTiles",linux(200,1500),windows(200,400));
  final Area head= new Area("head",linux(80,94,1320,78),windows(22,50,600,60));
  final Area output= new Area("output",linux(91,264,3734,1861),windows(26,214,1238,422));
  final Area dialog= new Area("dialog",linux(1600,840,700,30),windows(600,76,300,20));
  final Click check= new Click("check",linux(164,111),windows(102,67));
  final Click managerMenu= new Click("managerMenu",linux(98,79),windows(31,33));
  final Click editMetadata= new Click("editMetadata",linux(128,103),windows(61,58));
  final At editorShown= new At("editorShown",linux(2000),windows(2000));
  final DoubleClick nameOld= new DoubleClick("nameOld",linux(1589,864),windows(274,87));
  final Click commit= new Click("commit",linux(2238,1370),windows(924,619));
  final Click projectMenu= new Click("projectMenu",linux(158,79),windows(89,33));
  final Click errorReport= new Click("errorReport",linux(180,212),windows(111,166));
  final Click focusReport= new Click("focusReport",linux(1970,1100),windows(660,300));
  final Click ok= new Click("ok",linux(1953,1323),windows(639,568));
  final Click openInformation= new Click("openInformation",linux(150,167),windows(79,120));
  final Area information= new Area("information",linux(80,160,1320,180),windows(22,134,600,180));
  final Click checkAgain= new Click("checkAgain",linux(164,111),windows(102,67));
  @Override protected void walk() throws Exception{
    clean();
    Fs.writeUtf8(old.resolve("old.fearless"),"\n");
    Fs.writeUtf8(old.resolve("_old").resolve("_rank_app.fear"),"use base.Main as Main;\n\nHello:Main{s->base.Debug#(\"old\")}\n");
    launch(old.toString());
    managerShown.go();
    focusTiles.go();
    pilot.chord(KeyEvent.VK_F8);
    pilot.chord(KeyEvent.VK_HOME);
    check.go();
    until(()->Fs.readUtf8(data.resolve("eclipse").resolve("old").resolve("console.txt")).equals(fine));
    look();
    var top= head.aim();
    var named= pixels(top);
    var at= output.aim();
    var once= pixels(at);
    managerMenu.go();
    editMetadata.go();
    editorShown.go();
    nameOld.go();
    IntStream.of(KeyEvent.VK_F,KeyEvent.VK_R,KeyEvent.VK_E,KeyEvent.VK_S,KeyEvent.VK_H).forEach(pilot::chord);
    commit.go();
    until(()->Fs.readUtf8(info).contains("\"fresh\""));
    assertEquals("""
      {
        "fresh": {
          "path": "Str:%s",
          "kind": "idle"
        }
      }
      """.formatted(slashed(old)),Fs.readUtf8(info));
    until(()->Files.exists(console) && Fs.readUtf8(console).equals(fine));
    look();
    until(()->!Arrays.equals(named,pixels(top)));
    until(()->Arrays.equals(once,pixels(at)));
    var where= dialog.aim();
    var behind= pixels(where);
    projectMenu.go();
    errorReport.go();
    until(()->!Arrays.equals(behind,pixels(where)));
    focusReport.go();
    pilot.chord(KeyEvent.VK_CONTROL,KeyEvent.VK_A);
    pilot.chord(KeyEvent.VK_CONTROL,KeyEvent.VK_C);
    assertEquals("""
      The marker file "fresh.fearless" is missing from
      %s
      Restore it, or forget and re-add this project folder.""".formatted(old),Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor));
    ok.go();
    until(()->Arrays.equals(behind,pixels(where)));
    var rows= information.aim();
    var closed= pixels(rows);
    openInformation.go();
    look();
    until(()->!Arrays.equals(closed,pixels(rows)));
    var invalid= pixels(rows);
    Files.move(old.resolve("old.fearless"),old.resolve("fresh.fearless"));
    until(()->!Arrays.equals(invalid,pixels(rows)));
    checkAgain.go();
    until(()->Fs.readUtf8(console).equals(fine+fine));
    look();
    until(()->!Arrays.equals(once,pixels(at)));
    stopManagers();
  }
  @Override @AfterEach void clean() throws Exception{
    super.clean();
    Fs.rmTree(old);
  }
}

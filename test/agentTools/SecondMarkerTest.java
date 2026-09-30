package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;

import tools.Fs;

/// A second marker file put into a registered folder makes the project invalid: the manager notices it by itself, and its Error report and Check say why; once that file is gone the manager notices it by itself again, shows the project exactly as before, and Check finds no problem.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, nothing is registered for .fearless, and the folder twice beside the manager holds only its marker and one source file.
/// Action 1: run the launcher on twice: the manager window opens showing twice, an idle project.
/// Action 2: click the empty space below the tiles, and move the divider between tiles and panel as far left as it goes with the keyboard.
/// Action 3: put a second marker file into twice: the Information section of the panel changes by itself.
/// Action 4: choose Error report in its Project menu: a dialog opens.
/// Action 5: click in its text, select all of it and copy it: the text copied says the folder holds more than one marker and names the one to keep.
/// Action 6: press OK: the dialog goes away.
/// Action 7: press Check: the Output says exactly what the Error report says.
/// Action 8: delete the second marker file, and open the Information section again: the kind buttons and the Information section are back by themselves exactly as they were before the second marker.
/// Action 9: press Check: the Output says no problem was found.
/// Action 10: end the manager.
final class SecondMarkerTest extends ManagerTest{
  static final Path twice= data.resolveSibling("twice");
  static final Path second= twice.resolve("other.fearless");
  static final Path console= data.resolve("eclipse").resolve("twice").resolve("console.txt");
  final At managerShown= new At("managerShown",linux(3000));
  final Click focusTiles= new Click("focusTiles",linux(200,1500));
  final Area rows= new Area("rows",linux(80,128,1320,212));
  final Area dialog= new Area("dialog",linux(1600,840,700,30));
  final Click projectMenu= new Click("projectMenu",linux(158,79));
  final Click errorReport= new Click("errorReport",linux(180,212));
  final Click focusReport= new Click("focusReport",linux(1970,1100));
  final Click ok= new Click("ok",linux(1953,1330));
  final Click check= new Click("check",linux(164,111));
  final Click openInformation= new Click("openInformation",linux(150,167));
  final Click checkAgain= new Click("checkAgain",linux(164,111));
  @Override protected void walk() throws Exception{
    clean();
    Fs.writeUtf8(twice.resolve("twice.fearless"),"\n");
    Fs.writeUtf8(twice.resolve("_twice").resolve("_rank_app.fear"),"use base.Main as Main;\n\nHello:Main{s->base.Debug#(\"twice\")}\n");
    launch(twice.toString());
    managerShown.go();
    focusTiles.go();
    pilot.chord(KeyEvent.VK_F8);
    pilot.chord(KeyEvent.VK_HOME);
    look();
    var at= rows.aim();
    var valid= pixels(at);
    Fs.writeUtf8(second,"\n");
    until(()->!Arrays.equals(valid,pixels(at)));
    var where= dialog.aim();
    var behind= pixels(where);
    projectMenu.go();
    errorReport.go();
    until(()->!Arrays.equals(behind,pixels(where)));
    focusReport.go();
    pilot.chord(KeyEvent.VK_CONTROL,KeyEvent.VK_A);
    pilot.chord(KeyEvent.VK_CONTROL,KeyEvent.VK_C);
    var problem= """
      More than one .fearless marker file was found in
      %s
      A project folder holds exactly one, and its name is the project name: keep only "twice.fearless".""".formatted(twice);
    assertEquals(problem,Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor));
    ok.go();
    until(()->Arrays.equals(behind,pixels(where)));
    check.go();
    until(()->!Fs.readUtf8(console).isEmpty());
    assertEquals(problem+"\n",Fs.readUtf8(console));
    Files.delete(second);
    openInformation.go();
    look();
    until(()->Arrays.equals(valid,pixels(at)));
    checkAgain.go();
    until(()->Fs.readUtf8(console).length()>problem.length()+1);
    assertEquals(problem+"\n--- ok: no problem found ---\n",Fs.readUtf8(console));
    stopManagers();
  }
  @Override @AfterEach void clean() throws Exception{
    super.clean();
    Fs.rmTree(twice);
  }
}

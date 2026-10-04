package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.awt.Desktop;
import java.nio.file.Files;
import java.util.Arrays;

import tools.Fs;

/// Once the manager has made the desk open .fearless files with it, opening a Fearless file from the file manager starts the manager on the project folder holding that file, even with no manager running.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, helloWorld was never compiled, and nothing is registered for .fearless.
/// Action 1: run the launcher: the manager window opens with no tile, and the manager remembers no project.
/// Action 2: choose Quit manager in its Manager menu: the manager ends.
/// Action 3: open the file manager on helloWorld.
/// Action 4: double click hello_world.fearless: the manager window opens with one tile, and the manager remembers helloWorld as an idle project.
/// Action 5: end the manager and close the file manager window.
final class OpenFearlessFileTest extends ManagerTest{
  final At managerShown= new At("managerShown",on("ubuntu-gnome",3000));
  final Area menuBar= new Area("menuBar",on("ubuntu-gnome",68,69,190,20));
  final Area firstTile= new Area("firstTile",on("ubuntu-gnome",74,149,128,88));
  final Click managerMenu= new Click("managerMenu",on("ubuntu-gnome",98,79));
  final Click quitManager= new Click("quitManager",on("ubuntu-gnome",128,212));
  final At filesShown= new At("filesShown",on("ubuntu-gnome",3000));
  final DoubleClick openFile= new DoubleClick("openFile",on("ubuntu-gnome",1872,915));
  final Click closeFiles= new Click("closeFiles",on("ubuntu-gnome",2374,844));
  @Override protected void walk() throws Exception{
    clean();
    var run= launch();
    managerShown.go();
    look();
    var menu= menuBar.aim();
    var tile= firstTile.aim();
    var bar= pixels(menu);
    var empty= pixels(tile);
    assertFalse(Files.exists(info));
    managerMenu.go();
    quitManager.go();
    until(()->!run.isAlive());
    assertEquals(0,run.exitValue());
    Desktop.getDesktop().open(project.toFile());
    filesShown.go();
    openFile.go();
    until(()->Arrays.equals(bar,pixels(menu)) && !Arrays.equals(empty,pixels(tile)));
    assertEquals("""
      {
        "hello_world": {
          "path": "Str:%s",
          "kind": "idle"
        }
      }
      """.formatted(project),Fs.readUtf8(info));
    stopManagers();
    closeFiles.go();
  }
}

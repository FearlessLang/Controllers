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
/// Setup: no manager runs, the manager has no data folder, helloWorld was never compiled, nothing is registered for .fearless, and the file manager remembers no place for its windows.
/// Action 1: run the launcher: the manager window opens with no tile, and the manager remembers no project.
/// Action 2: choose Quit manager in its Manager menu: the manager ends.
/// Action 3: open the file manager on helloWorld.
/// Action 4: double click hello_world.fearless: the manager window opens with one tile, and the manager remembers helloWorld as an idle project.
/// Action 5: end the manager and close the file manager window.
final class OpenFearlessFileTest extends ManagerTest{
  final At managerShown= new At("managerShown",linux(3000),windows(3000));
  final Area menuBar= new Area("menuBar",linux(68,69,190,20),windows(0,23,190,22));
  final Area firstTile= new Area("firstTile",linux(74,149,128,88),windows(7,101,127,88));
  final Click managerMenu= new Click("managerMenu",linux(98,79),windows(31,33));
  final Click quitManager= new Click("quitManager",linux(128,191),windows(61,167));
  final At filesShown= new At("filesShown",linux(3000),windows(4000));
  final DoubleClick openFile= new DoubleClick("openFile",linux(1872,915),windows(503,254));
  final Click closeFiles= new Click("closeFiles",linux(2374,844),windows(1016,58));
  @Override protected void walk() throws Exception{
    clean();
    forgetWindowPlaces();
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
      """.formatted(slashed(project)),Fs.readUtf8(info));
    stopManagers();
    closeFiles.go();
  }
}

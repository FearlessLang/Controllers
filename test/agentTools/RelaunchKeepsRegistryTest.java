package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.awt.event.KeyEvent;
import java.util.Arrays;

import tools.Fs;

/// The manager remembers its projects across a restart: launched again with no argument after Quit manager, it shows every project it remembers with the kind it had, none of them selected, and choosing one shows its panel exactly as before the restart.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, helloWorld was never compiled, and nothing is registered for .fearless.
/// Action 1: run the launcher on helloWorld: the manager window opens showing helloWorld selected, an idle project.
/// Action 2: click the empty space below the tiles, and move the divider between tiles and panel as far left as it goes with the keyboard.
/// Action 3: press Become code: the manager remembers helloWorld as a code project, and the panel shows it as one.
/// Action 4: choose Quit manager in its Manager menu: the manager ends.
/// Action 5: run the launcher: the manager window opens with the helloWorld tile not selected, and the manager remembers exactly what it remembered before.
/// Action 6: click the helloWorld tile: it is selected.
/// Action 7: click the empty space below the tiles, and move the divider as far left as it goes with the keyboard: the panel shows helloWorld exactly as it did before the manager ended.
/// Action 8: end the manager.
final class RelaunchKeepsRegistryTest extends ManagerTest{
  final At managerShown= new At("managerShown",linux(3000),windows(3000));
  final Area firstTile= new Area("firstTile",linux(74,149,128,88),windows(7,101,127,88));
  final Click focusTiles= new Click("focusTiles",linux(200,1500),windows(200,400));
  final Click becomeCode= new Click("becomeCode",linux(400,139),windows(332,94));
  final At codeShown= new At("codeShown",linux(1000),windows(1000));
  final Area panel= new Area("panel",linux(80,94,1320,340),windows(22,50,600,340));
  final Click managerMenu= new Click("managerMenu",linux(98,79),windows(31,33));
  final Click quitManager= new Click("quitManager",linux(128,191),windows(61,167));
  final At managerShownAgain= new At("managerShownAgain",linux(3000),windows(3000));
  final Click selectTile= new Click("selectTile",linux(138,190),windows(71,145));
  final Click focusTilesAgain= new Click("focusTilesAgain",linux(200,1500),windows(200,400));
  @Override protected void walk() throws Exception{
    clean();
    var run= launch(project.toString());
    managerShown.go();
    look();
    var tile= firstTile.aim();
    var selected= pixels(tile)[0];
    focusTiles.go();
    pilot.chord(KeyEvent.VK_F8);
    pilot.chord(KeyEvent.VK_HOME);
    becomeCode.go();
    until(()->Fs.readUtf8(info).contains("\"code\""));
    var remembered= Fs.readUtf8(info);
    assertEquals("""
      {
        "hello_world": {
          "path": "Str:%s",
          "kind": "code"
        }
      }
      """.formatted(slashed(project)),remembered);
    codeShown.go();
    look();
    var at= panel.aim();
    var code= pixels(at);
    managerMenu.go();
    quitManager.go();
    until(()->!run.isAlive());
    assertEquals(0,run.exitValue());
    launch();
    managerShownAgain.go();
    look();
    assertNotEquals(selected,pixels(tile)[0]);
    assertEquals(remembered,Fs.readUtf8(info));
    selectTile.go();
    until(()->pixels(tile)[0]==selected);
    focusTilesAgain.go();
    pilot.chord(KeyEvent.VK_F8);
    pilot.chord(KeyEvent.VK_HOME);
    look();
    until(()->Arrays.equals(code,pixels(at)));
    stopManagers();
  }
}

package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.Arrays;

import tools.Fs;

/// A folder added as a new entry to the text of the metadata editor is registered when the text is committed, exactly as running the launcher on it registers it: a second tile appears, the manager remembers both folders, and the new project answers in an Output of its own.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, helloWorld was never compiled, nothing is registered for .fearless, and the clipboard holds the metadata naming helloWorld as hello_world and then helloStackTraces as start, both idle.
/// Action 1: run the launcher on helloWorld: the manager window opens showing helloWorld, an idle project.
/// Action 2: choose Edit project metadata... in its Manager menu: the metadata editor opens.
/// Action 3: click in its text, select all of it and paste.
/// Action 4: press Commit: the editor goes away, the manager remembers exactly what the clipboard holds, and a second tile appears beside helloWorld, which keeps the selection.
/// Action 5: click the second tile: it takes the selection.
/// Action 6: click the empty space below the tiles, move the divider between tiles and panel as far left as it goes with the keyboard, and press Check: the Output of start says no problem was found, and the Output of helloWorld stays empty.
/// Action 7: bring the desk back to the Setup state and run the launcher on helloWorld: the manager window opens showing helloWorld.
/// Action 8: run the launcher on helloStackTraces: it ends at once, the manager remembers exactly what it remembered after action 4, and the tiles look exactly as after action 5.
/// Action 9: end the manager.
final class EditMetadataAddProjectTest extends ManagerTest{
  static final String both= """
    {
      "hello_world": {
        "path": "Str:%s",
        "kind": "idle"
      },
      "start": {
        "path": "Str:%s",
        "kind": "idle"
      }
    }
    """.formatted(slashed(project),slashed(other));
  final At managerShown= new At("managerShown",linux(3000),windows(3000));
  final Area buttons= new Area("buttons",linux(2190,1350,170,40),windows(884,606,152,26));
  final Click managerMenu= new Click("managerMenu",linux(98,79),windows(31,33));
  final Click editMetadata= new Click("editMetadata",linux(128,103),windows(61,58));
  final Click focusText= new Click("focusText",linux(1900,1200),windows(700,400));
  final Area firstTile= new Area("firstTile",linux(74,149,128,88),windows(7,101,127,88));
  final Area secondTile= new Area("secondTile",linux(202,149,128,88),windows(135,101,127,88));
  final Click commit= new Click("commit",linux(2238,1370),windows(924,619));
  final Click selectSecond= new Click("selectSecond",linux(265,190),windows(199,145));
  final Area tiles= new Area("tiles",linux(68,68,310,180),windows(0,23,310,180));
  final Click focusTiles= new Click("focusTiles",linux(200,1500),windows(200,400));
  final Click check= new Click("check",linux(164,111),windows(102,67));
  final At managerShownAgain= new At("managerShownAgain",linux(3000),windows(3000));
  @Override protected void walk() throws Exception{
    clean();
    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(both),null);
    launch(project.toString());
    managerShown.go();
    look();
    var at= buttons.aim();
    var behind= pixels(at);
    managerMenu.go();
    editMetadata.go();
    until(()->!Arrays.equals(behind,pixels(at)));
    focusText.go();
    pilot.chord(KeyEvent.VK_CONTROL,KeyEvent.VK_A);
    pilot.chord(KeyEvent.VK_CONTROL,KeyEvent.VK_V);
    look();
    var first= firstTile.aim();
    var second= secondTile.aim();
    var selected= pixels(first);
    var empty= pixels(second);
    commit.go();
    until(()->Fs.readUtf8(info).equals(both));
    look();
    until(()->Arrays.equals(behind,pixels(at)));
    until(()->!Arrays.equals(empty,pixels(second)));
    assertTrue(Arrays.equals(selected,pixels(first)));
    selectSecond.go();
    look();
    until(()->!Arrays.equals(selected,pixels(first)));
    var place= tiles.aim();
    var shown= pixels(place);
    focusTiles.go();
    pilot.chord(KeyEvent.VK_F8);
    pilot.chord(KeyEvent.VK_HOME);
    check.go();
    until(()->Fs.readUtf8(console("start")).equals("--- ok: no problem found ---\n"));
    assertEquals("",Fs.readUtf8(console("hello_world")));
    clean();
    launch(project.toString());
    managerShownAgain.go();
    var run= new ProcessBuilder(launcher.toString(),other.toString()).start();
    until(()->!run.isAlive());
    assertEquals(0,run.exitValue());
    until(()->Fs.readUtf8(info).equals(both));
    look();
    until(()->Arrays.equals(shown,pixels(place)));
    stopManagers();
  }
  private static Path console(String alias){ return data.resolve("eclipse").resolve(alias).resolve("console.txt"); }
}

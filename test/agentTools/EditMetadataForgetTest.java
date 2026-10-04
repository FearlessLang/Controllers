package agentTools;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.stream.IntStream;

import tools.Fs;

/// Deleting the entry of the shown project from the text of the metadata editor and committing it forgets that project exactly as Forget project does: its tile goes away, the other tile stays as it was, not selected, the right side of the window goes empty, the Project menu offers nothing about a project, and the manager keeps running and remembers only the other folder.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, helloWorld was never compiled, nothing is registered for .fearless, and the clipboard holds the metadata naming only helloWorld, as hello_world, idle.
/// Action 1: run the launcher on helloWorld: the manager window opens showing helloWorld.
/// Action 2: run the launcher on helloStackTraces: a second tile appears beside helloWorld and takes the selection from it.
/// Action 3: choose Edit project metadata... in its Manager menu: the metadata editor opens.
/// Action 4: click in its text, select all of it and paste.
/// Action 5: press Commit: the editor goes away, the manager remembers exactly what the clipboard holds and keeps running, the second tile goes away, the helloWorld tile stays as it was, not selected, and the right side of the window is one plain colour.
/// Action 6: open the Project menu.
/// Action 7: bring the desk back to the Setup state, run the launcher on helloWorld and then on helloStackTraces: the second tile appears and takes the selection.
/// Action 8: choose Forget project in its Project menu: the manager remembers exactly what it remembered after action 5, and the tiles and the right side of the window look exactly as after action 5.
/// Action 9: open the Project menu: it looks exactly as in action 6.
/// Action 10: end the manager.
final class EditMetadataForgetTest extends ManagerTest{
  static final String only= """
    {
      "hello_world": {
        "path": "Str:%s",
        "kind": "idle"
      }
    }
    """.formatted(project);
  final At managerShown= new At("managerShown",on("ubuntu-gnome",3000));
  final Area firstTile= new Area("firstTile",on("ubuntu-gnome",74,149,128,88));
  final Area secondTile= new Area("secondTile",on("ubuntu-gnome",202,149,128,88));
  final Area buttons= new Area("buttons",on("ubuntu-gnome",2190,1350,170,40));
  final Click managerMenu= new Click("managerMenu",on("ubuntu-gnome",98,79));
  final Click editMetadata= new Click("editMetadata",on("ubuntu-gnome",128,103));
  final Click focusText= new Click("focusText",on("ubuntu-gnome",1900,1200));
  final Click commit= new Click("commit",on("ubuntu-gnome",2238,1370));
  final Area side= new Area("side",on("ubuntu-gnome",1400,94,2430,2030));
  final Area tiles= new Area("tiles",on("ubuntu-gnome",68,68,310,180));
  final Area menu= new Area("menu",on("ubuntu-gnome",132,90,177,160));
  final Click projectMenu= new Click("projectMenu",on("ubuntu-gnome",158,79));
  final At menuShown= new At("menuShown",on("ubuntu-gnome",300));
  final At managerShownAgain= new At("managerShownAgain",on("ubuntu-gnome",3000));
  final Click projectMenuAgain= new Click("projectMenuAgain",on("ubuntu-gnome",158,79));
  final Click forgetProject= new Click("forgetProject",on("ubuntu-gnome",180,237));
  final Click projectMenuOnceMore= new Click("projectMenuOnceMore",on("ubuntu-gnome",158,79));
  @Override protected void walk() throws Exception{
    clean();
    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(only),null);
    var run= launch(project.toString());
    managerShown.go();
    look();
    var first= firstTile.aim();
    var second= secondTile.aim();
    var empty= pixels(second);
    new ProcessBuilder(launcher.toString(),other.toString()).start();
    until(()->!Arrays.equals(empty,pixels(second)));
    look();
    var unselected= pixels(first);
    var at= buttons.aim();
    var behind= pixels(at);
    managerMenu.go();
    editMetadata.go();
    until(()->!Arrays.equals(behind,pixels(at)));
    focusText.go();
    pilot.chord(KeyEvent.VK_CONTROL,KeyEvent.VK_A);
    pilot.chord(KeyEvent.VK_CONTROL,KeyEvent.VK_V);
    commit.go();
    until(()->Fs.readUtf8(info).equals(only));
    look();
    var right= side.aim();
    until(()->IntStream.of(pixels(right)).distinct().count()==1);
    assertArrayEquals(empty,pixels(second));
    assertArrayEquals(unselected,pixels(first));
    assertTrue(run.isAlive());
    var blank= pixels(right);
    var place= tiles.aim();
    var shown= pixels(place);
    var popup= menu.aim();
    var closed= pixels(popup);
    projectMenu.go();
    look();
    until(()->!Arrays.equals(closed,pixels(popup)));
    menuShown.go();
    var disabled= pixels(popup);
    clean();
    launch(project.toString());
    managerShownAgain.go();
    new ProcessBuilder(launcher.toString(),other.toString()).start();
    until(()->!Arrays.equals(empty,pixels(second)));
    projectMenuAgain.go();
    forgetProject.go();
    until(()->Fs.readUtf8(info).equals(only));
    look();
    until(()->Arrays.equals(shown,pixels(place)));
    assertArrayEquals(blank,pixels(right));
    projectMenuOnceMore.go();
    look();
    until(()->Arrays.equals(disabled,pixels(popup)));
    stopManagers();
  }
}

package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.util.Arrays;

import tools.Fs;

/// Project > Add folder... registers the folder chosen in its chooser exactly as running the launcher on that folder does, and a cancelled chooser changes nothing.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, helloWorld was never compiled, nothing is registered for .fearless, and the clipboard holds the path of helloWorld.
/// Action 1: run the launcher: the manager window opens with no tile.
/// Action 2: choose Add folder... in its Project menu: a chooser opens.
/// Action 3: press Cancel: the chooser goes away, the window is exactly as before, and the manager remembers no project.
/// Action 4: choose Add folder... in the Project menu again: the chooser opens again.
/// Action 5: click its file name field, paste and press Enter: the chooser goes away, and the manager remembers helloWorld as an idle project.
/// Action 6: bring the desk back to the Setup state and run the launcher on helloWorld: the manager window opens with exactly the tiles it had before, and the manager remembers exactly what it remembered before.
/// Action 7: end the manager.
final class AddFolderTest extends ManagerTest{
  final At managerShown= new At("managerShown",linux(3000),windows(3000));
  final Area window= new Area("window",linux(68,32,3772,2098),windows(0,24,1280,624));
  final Click projectMenu= new Click("projectMenu",linux(158,79),windows(89,33));
  final Click addFolder= new Click("addFolder",linux(180,103),windows(111,57));
  final At chooserShown= new At("chooserShown",linux(1500),windows(4000));
  final Click cancel= new Click("cancel",linux(2155,1252),windows(841,487));
  final Click projectMenuAgain= new Click("projectMenuAgain",linux(158,79),windows(89,33));
  final Click addFolderAgain= new Click("addFolderAgain",linux(180,103),windows(111,57));
  final At chooserShownAgain= new At("chooserShownAgain",linux(1500),windows(4000));
  final Click fileName= new Click("fileName",linux(2000,1180),windows(685,416));
  final At tileShown= new At("tileShown",linux(1000),windows(1000));
  final Area tiles= new Area("tiles",linux(68,68,310,180),windows(0,23,310,180));
  @Override protected void walk() throws Exception{
    clean();
    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(project.toString()),null);
    launch();
    managerShown.go();
    look();
    var at= window.aim();
    var before= pixels(at);
    projectMenu.go();
    addFolder.go();
    chooserShown.go();
    cancel.go();
    look();
    until(()->Arrays.equals(before,pixels(at)));
    assertFalse(Files.exists(info));
    projectMenuAgain.go();
    addFolderAgain.go();
    chooserShownAgain.go();
    fileName.go();
    pilot.chord(KeyEvent.VK_CONTROL,KeyEvent.VK_V);
    pilot.chord(KeyEvent.VK_ENTER);
    var remembered= """
      {
        "hello_world": {
          "path": "Str:%s",
          "kind": "idle"
        }
      }
      """.formatted(slashed(project));
    until(()->Files.exists(info) && Fs.readUtf8(info).equals(remembered));
    tileShown.go();
    look();
    var place= tiles.aim();
    var shown= pixels(place);
    clean();
    launch(project.toString());
    look();
    until(()->Arrays.equals(shown,pixels(place)));
    assertEquals(remembered,Fs.readUtf8(info));
    stopManagers();
  }
}

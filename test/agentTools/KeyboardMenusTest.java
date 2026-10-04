package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.util.Arrays;

import tools.Fs;

/// The menus of the manager work from the keyboard alone: a mnemonic opens its menu with the first item highlighted, exactly as a click and Down do, the arrow keys wrap round the enabled items and Enter chooses one, and a dialog opened so is read and closed with keys.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, helloWorld was never compiled, nothing is registered for .fearless, and the clipboard holds an empty text.
/// Action 1: run the launcher: the manager window opens with no tile.
/// Action 2: run the launcher on helloWorld: it ends at once, the manager remembers helloWorld and shows its tile.
/// Action 3: click the empty space below the tiles.
/// Action 4: click Project in the menu bar, and press Down: its menu opens, then its first item, Add folder..., is highlighted.
/// Action 5: press Escape: the menu closes and the window is exactly as before action 4.
/// Action 6: press alt+P: the Project menu opens with Add folder... highlighted, exactly as after action 4.
/// Action 7: press Up and Enter: Forget project is chosen: the manager remembers no project and the tiles are exactly as after action 1.
/// Action 8: press alt+M, Down and Enter: the raw project state dialog opens.
/// Action 9: press shift+Tab, then select all of its text and copy it: the text copied is <nothing registered>.
/// Action 10: press Escape: the dialog goes away.
/// Action 11: press alt+M, Up and Enter: Quit manager is chosen and the manager ends with exit 0.
final class KeyboardMenusTest extends ManagerTest{
  final At managerShown= new At("managerShown",linux(3000),windows(3000));
  final Area tiles= new Area("tiles",linux(68,68,310,180),windows(0,23,310,180));
  final Click focusTiles= new Click("focusTiles",linux(200,1500),windows(200,400));
  final Area window= new Area("window",linux(68,32,3772,2098),windows(0,24,1280,624));
  final Click projectMenu= new Click("projectMenu",linux(158,79),windows(89,33));
  final Area dialog= new Area("dialog",linux(1600,850,700,25),windows(600,96,300,10));
  @Override protected void walk() throws Exception{
    clean();
    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(""),null);
    var run= launch();
    managerShown.go();
    look();
    var cell= tiles.aim();
    var empty= pixels(cell);
    var handed= launch(project.toString());
    until(()->!handed.isAlive());
    assertEquals(0,handed.exitValue());
    assertEquals("""
      {
        "hello_world": {
          "path": "Str:%s",
          "kind": "idle"
        }
      }
      """.formatted(slashed(project)),Fs.readUtf8(info));
    focusTiles.go();
    look();
    var at= window.aim();
    var before= pixels(at);
    projectMenu.go();
    look();
    until(()->!Arrays.equals(before,pixels(at)));
    var opened= pixels(at);
    pilot.chord(KeyEvent.VK_DOWN);
    until(()->!Arrays.equals(opened,pixels(at)));
    var clicked= pixels(at);
    pilot.chord(KeyEvent.VK_ESCAPE);
    until(()->Arrays.equals(before,pixels(at)));
    pilot.chord(KeyEvent.VK_ALT,KeyEvent.VK_P);
    until(()->Arrays.equals(clicked,pixels(at)));
    pilot.chord(KeyEvent.VK_UP);
    pilot.chord(KeyEvent.VK_ENTER);
    until(()->Fs.readUtf8(info).equals("{}\n"));
    until(()->Arrays.equals(empty,pixels(cell)));
    var where= dialog.aim();
    var behind= pixels(where);
    pilot.chord(KeyEvent.VK_ALT,KeyEvent.VK_M);
    pilot.chord(KeyEvent.VK_DOWN);
    pilot.chord(KeyEvent.VK_ENTER);
    until(()->!Arrays.equals(behind,pixels(where)));
    pilot.chord(KeyEvent.VK_SHIFT,KeyEvent.VK_TAB);
    pilot.chord(KeyEvent.VK_CONTROL,KeyEvent.VK_A);
    pilot.chord(KeyEvent.VK_CONTROL,KeyEvent.VK_C);
    assertEquals("<nothing registered>",Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor));
    pilot.chord(KeyEvent.VK_ESCAPE);
    until(()->Arrays.equals(behind,pixels(where)));
    pilot.chord(KeyEvent.VK_ALT,KeyEvent.VK_M);
    pilot.chord(KeyEvent.VK_UP);
    pilot.chord(KeyEvent.VK_ENTER);
    until(()->!run.isAlive());
    assertEquals(0,run.exitValue());
  }
}

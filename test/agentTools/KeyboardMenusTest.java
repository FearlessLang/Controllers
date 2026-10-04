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
  final At managerShown= new At("managerShown",on("ubuntu-gnome",3000),on("debian-mate",2500),on("void-i3",3000),on("omarchy-hyprland",2000),on("debian-gnome-x11",2500),on("xubuntu-xfce",2500),on("debian-cinnamon",2500));
  final Area tiles= new Area("tiles",on("ubuntu-gnome",68,68,310,180),on("debian-mate",2,59,310,180),on("void-i3",2,18,310,180),on("omarchy-hyprland",13,91,300,180),on("debian-gnome-x11",2,69,310,180),on("xubuntu-xfce",2,120,310,180),on("debian-cinnamon",2,37,310,180));
  final Click focusTiles= new Click("focusTiles",on("ubuntu-gnome",200,1500),on("debian-mate",200,800),on("void-i3",200,759),on("omarchy-hyprland",160,450),on("debian-gnome-x11",200,810),on("xubuntu-xfce",200,791),on("debian-cinnamon",200,778));
  final Area window= new Area("window",on("ubuntu-gnome",68,32,3772,2098),on("debian-mate",0,28,1920,990),on("void-i3",0,0,1920,1030),on("omarchy-hyprland",6,19,948,485),on("debian-gnome-x11",0,32,1920,1022),on("xubuntu-xfce",0,27,1920,1022),on("debian-cinnamon",0,0,1920,1012));
  final Click projectMenu= new Click("projectMenu",on("ubuntu-gnome",158,79),on("debian-mate",92,70),on("void-i3",92,29),on("omarchy-hyprland",95,28),on("debian-gnome-x11",92,80),on("xubuntu-xfce",105,60),on("debian-cinnamon",92,47));
  final Area dialog= new Area("dialog",on("ubuntu-gnome",1600,850,700,25),on("debian-mate",860,530,200,25),on("void-i3",860,520,200,25),on("omarchy-hyprland",380,260,200,25),on("debian-gnome-x11",860,545,200,25),on("xubuntu-xfce",860,545,200,25),on("debian-cinnamon",860,509,200,25));
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
      """.formatted(project),Fs.readUtf8(info));
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

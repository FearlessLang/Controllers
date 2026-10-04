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
  final At managerShown= new At("managerShown",on("ubuntu-gnome",3000),on("debian-mate",2500),on("void-i3",3000),on("omarchy-hyprland",2000),on("debian-gnome-x11",2500),on("kubuntu-plasma",2500),on("xubuntu-xfce",2500),on("debian-cinnamon",2500));
  final Area window= new Area("window",on("ubuntu-gnome",68,32,3772,2098),on("debian-mate",0,28,1920,990),on("void-i3",0,0,1920,1030),on("omarchy-hyprland",6,19,948,485),on("debian-gnome-x11",0,32,1920,1022),on("kubuntu-plasma",0,0,1920,1000),on("xubuntu-xfce",0,27,1920,1022),on("debian-cinnamon",0,0,1920,1012));
  final Click projectMenu= new Click("projectMenu",on("ubuntu-gnome",158,79),on("debian-mate",92,70),on("void-i3",92,29),on("omarchy-hyprland",96,30),on("debian-gnome-x11",92,80),on("kubuntu-plasma",92,39),on("xubuntu-xfce",105,60),on("debian-cinnamon",92,47));
  final Click addFolder= new Click("addFolder",on("ubuntu-gnome",180,103),on("debian-mate",114,93),on("void-i3",114,52),on("omarchy-hyprland",118,53),on("debian-gnome-x11",114,103),on("kubuntu-plasma",114,62),on("xubuntu-xfce",120,82),on("debian-cinnamon",114,71));
  final At chooserShown= new At("chooserShown",on("ubuntu-gnome",1500),on("debian-mate",1500),on("void-i3",2000),on("omarchy-hyprland",2000),on("debian-gnome-x11",1500),on("kubuntu-plasma",1500),on("xubuntu-xfce",1500),on("debian-cinnamon",1500));
  final Click cancel= new Click("cancel",on("ubuntu-gnome",2155,1252),on("debian-mate",1157,696),on("void-i3",1160,654),on("omarchy-hyprland",680,408),on("debian-gnome-x11",1161,712),on("kubuntu-plasma",1156,664),on("xubuntu-xfce",1165,705),on("debian-cinnamon",1160,676));
  final Click projectMenuAgain= new Click("projectMenuAgain",on("ubuntu-gnome",158,79),on("debian-mate",92,70),on("void-i3",92,29),on("omarchy-hyprland",96,30),on("debian-gnome-x11",92,80),on("kubuntu-plasma",92,39),on("xubuntu-xfce",105,60),on("debian-cinnamon",92,47));
  final Click addFolderAgain= new Click("addFolderAgain",on("ubuntu-gnome",180,103),on("debian-mate",114,93),on("void-i3",114,52),on("omarchy-hyprland",118,53),on("debian-gnome-x11",114,103),on("kubuntu-plasma",114,62),on("xubuntu-xfce",120,82),on("debian-cinnamon",114,71));
  final At chooserShownAgain= new At("chooserShownAgain",on("ubuntu-gnome",1500),on("debian-mate",1500),on("void-i3",2000),on("omarchy-hyprland",2000),on("debian-gnome-x11",1500),on("kubuntu-plasma",1500),on("xubuntu-xfce",1500),on("debian-cinnamon",1500));
  final Click fileName= new Click("fileName",on("ubuntu-gnome",2000,1180),on("debian-mate",999,623),on("void-i3",1006,586),on("omarchy-hyprland",520,342),on("debian-gnome-x11",1000,640),on("kubuntu-plasma",1000,592),on("xubuntu-xfce",1010,637),on("debian-cinnamon",1000,604));
  final At tileShown= new At("tileShown",on("ubuntu-gnome",1000),on("debian-mate",1000),on("void-i3",1500),on("omarchy-hyprland",1500),on("debian-gnome-x11",1000),on("kubuntu-plasma",1000),on("xubuntu-xfce",1000),on("debian-cinnamon",1000));
  final Area tiles= new Area("tiles",on("ubuntu-gnome",68,68,310,180),on("debian-mate",2,59,310,180),on("void-i3",2,18,310,180),on("omarchy-hyprland",6,19,310,180),on("debian-gnome-x11",2,69,310,180),on("kubuntu-plasma",2,28,310,180),on("xubuntu-xfce",2,120,310,180),on("debian-cinnamon",2,37,310,180));
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
      """.formatted(project);
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

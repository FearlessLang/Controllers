package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import tools.Fs;

/// Running the launcher on a second project folder while the manager runs hands that folder to the running manager and ends at once: the folder joins the same window as a second tile, which takes the selection, and the manager remembers both folders.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, helloWorld was never compiled, and nothing is registered for .fearless.
/// Action 1: run the launcher on helloWorld: the manager window opens with one tile, helloWorld selected.
/// Action 2: run the launcher on helloStackTraces: it ends at once, a second tile appears beside helloWorld and takes the selection from it, the manager keeps running, and it remembers helloWorld and helloStackTraces as idle projects.
/// Action 3: end the manager.
final class SecondLaunchOnFolderTest extends ManagerTest{
  final At managerShown= new At("managerShown",on("ubuntu-gnome",3000),on("debian-gnome-x11",2500),on("xubuntu-xfce",2500),on("debian-cinnamon",2500));
  final Area firstTile= new Area("firstTile",on("ubuntu-gnome",74,149,128,88),on("debian-gnome-x11",8,150,128,88),on("xubuntu-xfce",8,125,128,88),on("debian-cinnamon",8,118,128,88));
  final Area secondTile= new Area("secondTile",on("ubuntu-gnome",202,149,128,88),on("debian-gnome-x11",136,150,128,88),on("xubuntu-xfce",136,125,128,88),on("debian-cinnamon",136,118,128,88));
  @Override protected void walk() throws Exception{
    clean();
    var run= launch(project.toString());
    managerShown.go();
    look();
    var first= firstTile.aim();
    var second= secondTile.aim();
    var selected= pixels(first);
    var empty= pixels(second);
    var again= new ProcessBuilder(launcher.toString(),other.toString()).start();
    until(()->!again.isAlive());
    assertEquals(0,again.exitValue());
    until(()->!Arrays.equals(empty,pixels(second)));
    assertFalse(Arrays.equals(selected,pixels(first)));
    assertEquals(selected[0],pixels(second)[0]);
    assertTrue(run.isAlive());
    assertEquals("""
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
      """.formatted(project,other),Fs.readUtf8(info));
    stopManagers();
  }
}

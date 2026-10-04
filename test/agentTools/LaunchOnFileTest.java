package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import tools.Fs;

/// Opening a Fearless file with the manager registers the project folder holding it, exactly as running the launcher on that folder does.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, helloWorld was never compiled, and nothing is registered for .fearless.
/// Action 1: run the launcher on helloWorld: the manager window opens with one tile, helloWorld selected, and the manager remembers helloWorld as an idle project.
/// Action 2: bring the desk back to the Setup state.
/// Action 3: run the launcher on hello_world.fearless in helloWorld: the manager window opens with exactly the tiles it had before, and the manager remembers exactly what it remembered before.
/// Action 4: end the manager.
final class LaunchOnFileTest extends ManagerTest{
  final At managerShown= new At("managerShown",on("ubuntu-gnome",3000),on("debian-gnome-x11",2500),on("xubuntu-xfce",2500),on("debian-cinnamon",2500));
  final Area tiles= new Area("tiles",on("ubuntu-gnome",68,68,310,180),on("debian-gnome-x11",2,69,310,180),on("xubuntu-xfce",2,120,310,180),on("debian-cinnamon",2,37,310,180));
  @Override protected void walk() throws Exception{
    clean();
    launch(project.toString());
    managerShown.go();
    look();
    var at= tiles.aim();
    var shown= pixels(at);
    var remembered= Fs.readUtf8(info);
    assertEquals("""
      {
        "hello_world": {
          "path": "Str:%s",
          "kind": "idle"
        }
      }
      """.formatted(project),remembered);
    clean();
    launch(project.resolve("hello_world.fearless").toString());
    until(()->Arrays.equals(shown,pixels(at)));
    assertEquals(remembered,Fs.readUtf8(info));
    stopManagers();
  }
}

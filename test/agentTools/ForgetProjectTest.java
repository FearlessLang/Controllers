package agentTools;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import tools.Fs;

/// Forgetting a project takes its tile out of the window and the manager stops remembering it, while the other projects stay as they were, the manager keeps running, and the folder of the forgotten project is left exactly as it was.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, helloWorld was never compiled, and nothing is registered for .fearless.
/// Action 1: run the launcher on helloWorld: the manager window opens with one tile, helloWorld selected.
/// Action 2: run the launcher on helloStackTraces: a second tile appears beside helloWorld and takes the selection from it.
/// Action 3: choose Forget project in its Project menu: the second tile goes away, the helloWorld tile stays as it was, not selected, the manager keeps running and remembers only helloWorld, and every file and folder of helloStackTraces is still there, unchanged.
/// Action 4: end the manager.
final class ForgetProjectTest extends ManagerTest{
  final At managerShown= new At("managerShown",on("ubuntu-gnome",3000));
  final Area firstTile= new Area("firstTile",on("ubuntu-gnome",74,149,128,88));
  final Area secondTile= new Area("secondTile",on("ubuntu-gnome",202,149,128,88));
  final Click projectMenu= new Click("projectMenu",on("ubuntu-gnome",158,79));
  final Click forgetProject= new Click("forgetProject",on("ubuntu-gnome",180,237));
  @Override protected void walk() throws Exception{
    clean();
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
    var files= files();
    projectMenu.go();
    forgetProject.go();
    look();
    until(()->Arrays.equals(empty,pixels(second)));
    assertArrayEquals(unselected,pixels(first));
    assertTrue(run.isAlive());
    assertEquals("""
      {
        "hello_world": {
          "path": "Str:%s",
          "kind": "idle"
        }
      }
      """.formatted(project),Fs.readUtf8(info));
    assertEquals(files,files());
    stopManagers();
  }
  private static List<String> files(){ return Fs.walk(other,s->s.map(p->p+" "+Fs.lastModified(p)).toList()); }
}

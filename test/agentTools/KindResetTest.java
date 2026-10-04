package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import tools.Fs;

/// A project remembered with a kind that is not one of the kinds is loaded as an idle project: the manager says so in a note when it starts, deletes the compiled cache of the project, and remembers the project as idle, so the next start shows it the same way with no note.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the data folder of the manager holds only the file of the projects it remembers, naming helloWorld as hello_world with the kind "cooked", helloWorld holds a compiled cache, and nothing is registered for .fearless.
/// Action 1: run the launcher: the manager window opens with a note saying hello_world is now idle and its compiled cache is deleted; the manager remembers helloWorld as idle and the cache is gone.
/// Action 2: press OK: the note goes away and the window shows the tile of hello_world, not selected.
/// Action 3: choose Quit manager in its Manager menu: the manager ends.
/// Action 4: run the launcher: the manager window opens with no note, showing the tile of hello_world exactly as before, and the manager remembers exactly what it remembered before.
/// Action 5: end the manager.
final class KindResetTest extends ManagerTest{
  static final Path cache= project.resolve(".fearless_out");
  static final String remembered= """
    {
      "hello_world": {
        "path": "Str:%s",
        "kind": "%s"
      }
    }
    """;
  final At noteShown= new At("noteShown",linux(1000),windows(1000));
  final Click ok= new Click("ok",linux(1952,1132),windows(639,367));
  final Area tiles= new Area("tiles",linux(68,68,310,180),windows(0,23,310,180));
  final Click managerMenu= new Click("managerMenu",linux(98,79),windows(31,33));
  final Click quitManager= new Click("quitManager",linux(128,191),windows(61,167));
  final At managerShown= new At("managerShown",linux(3000),windows(3000));
  @Override protected void walk() throws Exception{
    clean();
    Fs.writeUtf8(info,remembered.formatted(slashed(project),"cooked"));
    Fs.writeUtf8(cache.resolve("_map.json"),"{}\n");
    var run= launch();
    until(()->Files.exists(notes) && !Fs.readUtf8(notes).isEmpty());
    noteShown.go();
    assertEquals("In projects.info the \"kind\" of \"hello_world\" was missing or not one of the kinds: \"hello_world\" is now idle, and its compiled cache is deleted.\n",Fs.readUtf8(notes));
    var idle= remembered.formatted(slashed(project),"idle");
    assertEquals(idle,Fs.readUtf8(info));
    assertFalse(Files.exists(cache));
    ok.go();
    look();
    var at= tiles.aim();
    var shown= pixels(at);
    managerMenu.go();
    quitManager.go();
    until(()->!run.isAlive());
    assertEquals(0,run.exitValue());
    launch();
    managerShown.go();
    assertEquals("",Fs.readUtf8(notes));
    assertEquals(idle,Fs.readUtf8(info));
    look();
    until(()->Arrays.equals(shown,pixels(at)));
    stopManagers();
  }
}

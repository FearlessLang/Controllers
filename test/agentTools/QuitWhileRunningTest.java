package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.Arrays;

import tools.Fs;
import utils.OneOr;

/// Quitting the manager while one of its programs runs ends that program too: the window of the program goes away with the window of the manager, and nothing is left running.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, helloWorld and testGui1 were never compiled, and nothing is registered for .fearless.
/// Action 1: run the launcher on testGui1: the manager window opens showing testGui1, an idle project.
/// Action 2: click the empty space below the tiles, and move the divider between tiles and panel as far left as it goes with the keyboard.
/// Action 3: press Become code: the manager remembers testGui1 as a code project.
/// Action 4: press Compile: the Output says the compile is done.
/// Action 5: press Run: the Output says the program runs, and the window of the program opens.
/// Action 6: choose Quit manager in its Manager menu: the manager ends, the program ends, and the desk shows its background again.
final class QuitWhileRunningTest extends ManagerTest{
  static final Path console= data.resolve("eclipse").resolve("start").resolve("console.txt");
  final At managerShown= new At("managerShown",linux(3000));
  final Click focusTiles= new Click("focusTiles",linux(200,1500));
  final Click becomeCode= new Click("becomeCode",linux(400,139));
  final Click compile= new Click("compile",linux(164,111));
  final Click run= new Click("run",linux(164,111));
  final Area programWindow= new Area("programWindow",linux(1915,1075,10,10));
  final Click managerMenu= new Click("managerMenu",linux(98,79));
  final Click quitManager= new Click("quitManager",linux(128,191));
  @Override protected void walk() throws Exception{
    clean();
    var desk= look();
    var manager= launch(gui.toString());
    managerShown.go();
    focusTiles.go();
    pilot.chord(KeyEvent.VK_F8);
    pilot.chord(KeyEvent.VK_HOME);
    becomeCode.go();
    until(()->Fs.readUtf8(info).contains("\"code\""));
    assertEquals("""
      {
        "start": {
          "path": "Str:%s",
          "kind": "code"
        }
      }
      """.formatted(gui),Fs.readUtf8(info));
    compile.go();
    until(()->Fs.readUtf8(console).contains("--- compile "));
    assertEquals("--- compiling testGui1 ---\n--- compile done ---\n",Fs.readUtf8(console));
    look();
    var at= programWindow.aim();
    var before= pixels(at);
    run.go();
    until(()->!Arrays.equals(before,pixels(at)));
    assertEquals("--- compiling testGui1 ---\n--- compile done ---\n--- running gui_example.Foo ---\n",Fs.readUtf8(console));
    var program= OneOr.of("program",manager.descendants());
    managerMenu.go();
    quitManager.go();
    until(()->!manager.isAlive());
    assertEquals(0,manager.exitValue());
    until(()->!program.isAlive());
    until(()->same(desk,look()));
  }
}

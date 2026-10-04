package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.opentest4j.AssertionFailedError;

import tools.Fs;
import utils.Err;
import utils.OneOr;

/// Terminating a program the manager runs ends that program alone: its window goes away, the Output says it was terminated and how it exited, the manager keeps running, and the top of the panel offers Run again exactly as before the run.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, helloWorld and testGui1 were never compiled, and nothing is registered for .fearless.
/// Action 1: run the launcher on testGui1: the manager window opens showing testGui1, an idle project.
/// Action 2: click the empty space below the tiles, and move the divider between tiles and panel as far left as it goes with the keyboard.
/// Action 3: press Become code: the manager remembers testGui1 as a code project.
/// Action 4: press Compile: the Output says the compile is done, and the manager knows the main of testGui1.
/// Action 5: press Run: the Output says the program runs, the window of the program opens, and the top of the panel changes.
/// Action 6: press Terminate: the program ends, the manager knows its run ended and how it exited, the Output says it was terminated and how it exited, the manager still runs, the window of the program goes away, and the top of the panel is back exactly as before the run.
/// Action 7: end the manager.
final class TerminateTest extends ManagerTest{
  static{ Err.setUp(AssertionFailedError.class,Assertions::assertEquals,Assertions::assertTrue); }
  static final Path console= data.resolve("eclipse").resolve("start").resolve("console.txt");
  final At managerShown= new At("managerShown",on("ubuntu-gnome",3000));
  final Click focusTiles= new Click("focusTiles",on("ubuntu-gnome",200,1500));
  final Click becomeCode= new Click("becomeCode",on("ubuntu-gnome",400,139));
  final Click compile= new Click("compile",on("ubuntu-gnome",164,111));
  final At mainShown= new At("mainShown",on("ubuntu-gnome",1000));
  final Area head= new Area("head",on("ubuntu-gnome",80,94,1320,78));
  final Area programWindow= new Area("programWindow",on("ubuntu-gnome",1915,1075,10,10));
  final Click run= new Click("run",on("ubuntu-gnome",164,111));
  final Click terminate= new Click("terminate",on("ubuntu-gnome",164,111));
  @Override protected void walk() throws Exception{
    clean();
    var manager= launch(gui.toString());
    managerShown.go();
    focusTiles.go();
    pilot.chord(KeyEvent.VK_F8);
    pilot.chord(KeyEvent.VK_HOME);
    becomeCode.go();
    until(()->Fs.readUtf8(info).contains("\"code\""));
    compile.go();
    until(()->Fs.readUtf8(state).contains("gui_example.Foo"));
    assertEquals("--- compiling testGui1 ---\n--- compile done ---\n",Fs.readUtf8(console));
    mainShown.go();
    look();
    var at= head.aim();
    var ready= pixels(at);
    var window= programWindow.aim();
    var desk= pixels(window);
    run.go();
    until(()->!Arrays.equals(desk,pixels(window)));
    assertEquals("--- compiling testGui1 ---\n--- compile done ---\n--- running gui_example.Foo ---\n",Fs.readUtf8(console));
    var program= OneOr.of("program",manager.descendants());
    look();
    assertFalse(Arrays.equals(ready,pixels(at)));
    terminate.go();
    until(()->!program.isAlive());
    until(()->Fs.readUtf8(state).contains("\"running\": \"\""));
    assertEquals("""
      {
        "start": {
          "folder": "Str:%s",
          "kind": "code",
          "running": "",
          "runs": "1",
          "lastRun": "gui_example.Foo",
          "exit": "143",
          "mains": {
            "gui_example.Foo": "_gui_example/_rank_app.fear"
          },
          "problem": {}
        }
      }
      """.formatted(gui),Fs.readUtf8(state));
    Err.strCmp("""
      --- compiling testGui1 ---
      --- compile done ---
      --- running gui_example.Foo ---
      --- terminating gui_example.Foo ---
      --- gui_example.Foo exited with 143 after [###]s ---
      """,Fs.readUtf8(console));
    assertTrue(manager.isAlive());
    until(()->Arrays.equals(desk,pixels(window)));
    look();
    until(()->Arrays.equals(ready,pixels(at)));
    stopManagers();
  }
}

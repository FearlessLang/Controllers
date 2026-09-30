package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.opentest4j.AssertionFailedError;

import tools.Fs;
import utils.Err;
import utils.OneOr;

/// Closing the only window of a program the manager runs ends the program by itself: the manager counts the run as ended with exit 0, the Output says so with no word of terminating, and the top of the panel offers Run again exactly as before the run.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, nothing is registered for .fearless, and the folder shut beside the manager holds only its marker and a program that opens one window and waits until it is closed.
/// Action 1: run the launcher on shut: the manager window opens showing shut, an idle project.
/// Action 2: click the empty space below the tiles, and move the divider between tiles and panel as far left as it goes with the keyboard.
/// Action 3: press Become code: the manager remembers shut as a code project.
/// Action 4: press Compile: the Output says the compile is done, and the manager knows shut.Show as the one main of shut.
/// Action 5: press Run: the Output says the program runs, the window of the program opens, and the top of the panel changes.
/// Action 6: close the window of the program with the close button of its title bar: the program ends by itself, the manager counts one run of shut.Show exited with 0, the Output says it exited with 0 and nothing about terminating, the manager still runs, the window of the program is gone, and the top of the panel is back exactly as before the run.
/// Action 7: end the manager.
final class CloseProgramWindowTest extends ManagerTest{
  static{ Err.setUp(AssertionFailedError.class,Assertions::assertEquals,Assertions::assertTrue); }
  static final Path shut= data.resolveSibling("shut");
  static final Path console= data.resolve("eclipse").resolve("shut").resolve("console.txt");
  final At managerShown= new At("managerShown",linux(3000));
  final Click focusTiles= new Click("focusTiles",linux(200,1500));
  final Click becomeCode= new Click("becomeCode",linux(400,139));
  final Click compile= new Click("compile",linux(164,111));
  final At mainShown= new At("mainShown",linux(1000));
  final Area head= new Area("head",linux(80,94,1320,78));
  final Area programWindow= new Area("programWindow",linux(1915,1075,10,10));
  final Click run= new Click("run",linux(164,111));
  final At programShown= new At("programShown",linux(1000));
  final Click closeProgram= new Click("closeProgram",linux(1996,1075));
  @Override protected void walk() throws Exception{
    clean();
    Fs.writeUtf8(shut.resolve("shut.fearless"),"\n");
    Fs.writeUtf8(shut.resolve("_shut").resolve("_rank_app.fear"),"""
      use base.Main as Main;
      use base.Consumer as Consumer;
      use base.Frame as Frame;

      Show: Main{sys -> sys.gui.run Note}
      Note: Consumer[mut Frame]{:: .title "shut" .content{:: .label{:: .text "close this window" } } }
      """);
    var manager= launch(shut.toString());
    managerShown.go();
    focusTiles.go();
    pilot.chord(KeyEvent.VK_F8);
    pilot.chord(KeyEvent.VK_HOME);
    becomeCode.go();
    until(()->Fs.readUtf8(info).contains("\"code\""));
    compile.go();
    until(()->Fs.readUtf8(state).contains("shut.Show"));
    assertEquals("--- compiling shut ---\n--- compile done ---\n",Fs.readUtf8(console));
    mainShown.go();
    look();
    var at= head.aim();
    var ready= pixels(at);
    var window= programWindow.aim();
    var desk= pixels(window);
    run.go();
    until(()->!Arrays.equals(desk,pixels(window)));
    assertEquals("--- compiling shut ---\n--- compile done ---\n--- running shut.Show ---\n",Fs.readUtf8(console));
    var program= OneOr.of("program",manager.descendants());
    look();
    assertFalse(Arrays.equals(ready,pixels(at)));
    programShown.go();
    closeProgram.go();
    until(()->!program.isAlive());
    until(()->Fs.readUtf8(state).contains("\"running\": \"\""));
    assertEquals("""
      {
        "shut": {
          "folder": "Str:%s",
          "kind": "code",
          "running": "",
          "runs": "1",
          "lastRun": "shut.Show",
          "exit": "0",
          "mains": {
            "shut.Show": "_shut/_rank_app.fear"
          },
          "problem": {}
        }
      }
      """.formatted(shut),Fs.readUtf8(state));
    Err.strCmp("""
      --- compiling shut ---
      --- compile done ---
      --- running shut.Show ---
      --- shut.Show exited with 0 after [###]s ---
      """,Fs.readUtf8(console));
    assertTrue(manager.isAlive());
    until(()->Arrays.equals(desk,pixels(window)));
    look();
    until(()->Arrays.equals(ready,pixels(at)));
    stopManagers();
  }
  @Override @AfterEach void clean() throws Exception{
    super.clean();
    Fs.rmTree(shut);
  }
}

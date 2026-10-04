package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.opentest4j.AssertionFailedError;

import tools.Fs;
import utils.Err;

/// Terminating the first of the ticked mains that Run selected runs one after the other ends the whole run: the Output says that main was terminated and how it exited, no other ticked main runs, and the manager counts that one run alone.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, nothing is registered for .fearless, and the folder chain beside the manager holds only its marker and a program with two mains: the first listed opens one window and waits until it is closed, the second prints a line.
/// Action 1: run the launcher on chain: the manager window opens showing chain, an idle project.
/// Action 2: click the empty space below the tiles, and move the divider between tiles and panel as far left as it goes with the keyboard.
/// Action 3: press Become code: the manager remembers chain as a code project.
/// Action 4: press Compile: the manager knows the two mains of chain.
/// Action 5: press All: the manager remembers both mains as selected, and the panel shows them ticked.
/// Action 6: press Run selected: the Output says the first main runs, the window of the program opens, and the kind button changes.
/// Action 7: press Terminate: the program ends, the manager counts one run, of the first main, and knows how it exited, the Output says it was terminated and how it exited and nothing about the second main, the manager still runs and still remembers both mains as selected, the window of the program goes away, and the kind button and the ticked mains look exactly as before action 6.
/// Action 8: end the manager.
final class TerminateDuringRunSelectedTest extends ManagerTest{
  static{ Err.setUp(AssertionFailedError.class,Assertions::assertEquals,Assertions::assertTrue); }
  static final Path chain= data.resolveSibling("chain");
  static final Path console= data.resolve("eclipse").resolve("chain").resolve("console.txt");
  final At managerShown= new At("managerShown",linux(3000),windows(3000));
  final Click focusTiles= new Click("focusTiles",linux(200,1500),windows(200,400));
  final Click becomeCode= new Click("becomeCode",linux(400,139),windows(332,94));
  final Click compile= new Click("compile",linux(164,111),windows(102,67));
  final At mainsShown= new At("mainsShown",linux(1000),windows(1000));
  final Area ticks= new Area("ticks",linux(90,180,210,50),windows(22,132,210,48));
  final Click all= new Click("all",linux(112,165),windows(45,118));
  final Area kindButton= new Area("kindButton",linux(80,128,1320,25),windows(22,82,600,24));
  final Area programWindow= new Area("programWindow",linux(1915,1075,10,10),windows(635,355,10,10));
  final Click runSelected= new Click("runSelected",linux(164,111),windows(102,67));
  final Click terminate= new Click("terminate",linux(164,111),windows(102,67));
  @Override protected void walk() throws Exception{
    clean();
    Fs.writeUtf8(chain.resolve("chain.fearless"),"\n");
    Fs.writeUtf8(chain.resolve("_chain").resolve("_rank_app.fear"),"""
      use base.Main as Main;
      use base.Consumer as Consumer;
      use base.Frame as Frame;

      Show: Main{sys -> sys.gui.run Note}
      Tell: Main{sys -> sys.out.println("told")}
      Note: Consumer[mut Frame]{:: .title "chain" .content{:: .label{:: .text "terminate this program" } } }
      """);
    var manager= launch(chain.toString());
    managerShown.go();
    focusTiles.go();
    pilot.chord(KeyEvent.VK_F8);
    pilot.chord(KeyEvent.VK_HOME);
    becomeCode.go();
    until(()->Fs.readUtf8(info).contains("\"code\""));
    compile.go();
    until(()->Fs.readUtf8(state).contains("chain.Tell"));
    mainsShown.go();
    look();
    var boxes= ticks.aim();
    var unticked= pixels(boxes);
    all.go();
    until(()->Fs.readUtf8(info).contains("chain.Tell"));
    var both= """
      {
        "chain": {
          "path": "Str:%s",
          "kind": "code",
          "mains": ["chain.Show", "chain.Tell"]
        }
      }
      """.formatted(slashed(chain));
    assertEquals(both,Fs.readUtf8(info));
    look();
    until(()->!Arrays.equals(unticked,pixels(boxes)));
    var ticked= pixels(boxes);
    var kind= kindButton.aim();
    var idle= pixels(kind);
    var window= programWindow.aim();
    var desk= pixels(window);
    runSelected.go();
    until(()->!Arrays.equals(desk,pixels(window)));
    assertEquals("--- compiling chain ---\n--- compile done ---\n--- running chain.Show ---\n",Fs.readUtf8(console));
    var program= program(manager);
    look();
    until(()->!Arrays.equals(idle,pixels(kind)));
    terminate.go();
    until(()->!program.isAlive());
    until(()->Fs.readUtf8(state).contains("\"running\": \"\""));
    assertEquals("""
      {
        "chain": {
          "folder": "Str:%s",
          "kind": "code",
          "running": "",
          "runs": "1",
          "lastRun": "chain.Show",
          "exit": "%s",
          "mains": {
            "chain.Show": "_chain/_rank_app.fear",
            "chain.Tell": "_chain/_rank_app.fear"
          },
          "problem": {}
        }
      }
      """.formatted(escaped(chain),killed),Fs.readUtf8(state));
    Err.strCmp("""
      --- compiling chain ---
      --- compile done ---
      --- running chain.Show ---
      --- terminating chain.Show ---
      --- chain.Show exited with %s after [###]s ---
      """.formatted(killed),Fs.readUtf8(console));
    assertTrue(manager.isAlive());
    assertEquals(both,Fs.readUtf8(info));
    until(()->Arrays.equals(desk,pixels(window)));
    look();
    until(()->Arrays.equals(idle,pixels(kind)) && Arrays.equals(ticked,pixels(boxes)));
    stopManagers();
  }
  @Override @AfterEach void clean() throws Exception{
    super.clean();
    Fs.rmTree(chain);
  }
}

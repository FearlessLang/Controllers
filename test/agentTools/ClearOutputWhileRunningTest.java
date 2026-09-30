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
import utils.OneOr;

/// Clear output while a program runs empties the Output and the console file of its project at once, and what the program and the manager print afterwards lands in the emptied Output and console file, from the top.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, nothing is registered for .fearless, and the folder talk beside the manager holds only its marker and a program that prints before, opens one window, and prints after once that window is closed.
/// Action 1: run the launcher on talk: the manager window opens showing talk, an idle project.
/// Action 2: click the empty space below the tiles, and move the divider between tiles and panel as far left as it goes with the keyboard.
/// Action 3: press Become code: the manager remembers talk as a code project.
/// Action 4: press Compile: the Output says the compile is done, and the manager knows talk.Show as the one main of talk.
/// Action 5: press Run: the window of the program opens, and the Output says the program runs and what it printed.
/// Action 6: press Clear output: the Output is empty, and so is its console file, while the program still runs.
/// Action 7: bring the window of the program back from the bar of open windows.
/// Action 8: close the window of the program with the close button of its title bar: the program ends by itself, and the Output and its console file hold only what the program printed after the clear and the line saying it exited with 0.
/// Action 9: end the manager.
final class ClearOutputWhileRunningTest extends ManagerTest{
  static{ Err.setUp(AssertionFailedError.class,Assertions::assertEquals,Assertions::assertTrue); }
  static final Path talk= data.resolveSibling("talk");
  static final Path console= data.resolve("eclipse").resolve("talk").resolve("console.txt");
  final At managerShown= new At("managerShown",linux(3000));
  final Click focusTiles= new Click("focusTiles",linux(200,1500));
  final Click becomeCode= new Click("becomeCode",linux(400,139));
  final Click compile= new Click("compile",linux(164,111));
  final At mainShown= new At("mainShown",linux(1000));
  final Area programWindow= new Area("programWindow",linux(1915,1075,10,10));
  final Click run= new Click("run",linux(164,111));
  final At programShown= new At("programShown",linux(1000));
  final Click clearOutput= new Click("clearOutput",linux(3784,289));
  final Area output= new Area("output",linux(91,319,3734,1806));
  final Area rest= new Area("rest",linux(91,360,3734,1765));
  final Click bringProgramBack= new Click("bringProgramBack",linux(32,450));
  final At programBack= new At("programBack",linux(1000));
  final Click closeProgram= new Click("closeProgram",linux(1997,1074));
  @Override protected void walk() throws Exception{
    clean();
    Fs.writeUtf8(talk.resolve("talk.fearless"),"\n");
    Fs.writeUtf8(talk.resolve("_talk").resolve("_rank_app.fear"),"""
      use base.Main as Main;
      use base.Block as Block;
      use base.Consumer as Consumer;
      use base.Frame as Frame;

      Show: Main{sys -> Block#(sys.out.println "before", sys.gui.run Note, sys.out.println "after")}
      Note: Consumer[mut Frame]{:: .title "talk" .content{:: .label{:: .text "close this window" } } }
      """);
    var manager= launch(talk.toString());
    managerShown.go();
    focusTiles.go();
    pilot.chord(KeyEvent.VK_F8);
    pilot.chord(KeyEvent.VK_HOME);
    becomeCode.go();
    until(()->Fs.readUtf8(info).contains("\"code\""));
    compile.go();
    until(()->Fs.readUtf8(state).contains("talk.Show"));
    mainShown.go();
    var window= programWindow.aim();
    var desk= pixels(window);
    run.go();
    until(()->!Arrays.equals(desk,pixels(window)));
    until(()->Fs.readUtf8(console).endsWith("before\n"));
    assertEquals("--- compiling talk ---\n--- compile done ---\n--- running talk.Show ---\nbefore\n",Fs.readUtf8(console));
    var program= OneOr.of("program",manager.descendants());
    programShown.go();
    clearOutput.go();
    until(()->Fs.readUtf8(console).isEmpty());
    look();
    var at= output.aim();
    until(()->Arrays.stream(pixels(at)).distinct().count()==1);
    var below= rest.aim();
    var blank= pixels(below);
    assertTrue(program.isAlive());
    bringProgramBack.go();
    until(()->!Arrays.equals(desk,pixels(window)));
    programBack.go();
    closeProgram.go();
    until(()->!program.isAlive());
    until(()->Fs.readUtf8(state).contains("\"exit\": \"0\""));
    Err.strCmp("""
      after
      --- talk.Show exited with 0 after [###]s ---
      """,Fs.readUtf8(console));
    until(()->Arrays.equals(desk,pixels(window)));
    look();
    until(()->Arrays.stream(pixels(at)).distinct().count()>1);
    assertTrue(Arrays.equals(blank,pixels(below)));
    stopManagers();
  }
  @Override @AfterEach void clean() throws Exception{
    super.clean();
    Fs.rmTree(talk);
  }
}

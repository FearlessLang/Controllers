package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.Arrays;

import tools.Fs;

/// Each registered project keeps an Output of its own: choosing another tile shows the Output of that project exactly as it was left, and Clear output empties only the Output of the project shown.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, helloWorld was never compiled, and nothing is registered for .fearless.
/// Action 1: run the launcher on helloWorld: the manager window opens showing helloWorld, an idle project.
/// Action 2: click the empty space below the tiles, and move the divider between tiles and panel as far left as it goes with the keyboard.
/// Action 3: close the Information section: the Output is empty.
/// Action 4: press Check: the Output says no problem was found.
/// Action 5: press Check again: the Output says it twice.
/// Action 6: run the launcher on helloStackTraces: the panel of start is shown instead, with an empty Output.
/// Action 7: press Check: the Output of start says no problem was found once, exactly as the Output of helloWorld did after the first Check, and the console file of helloWorld is unchanged.
/// Action 8: move the divider as far right as it goes with the keyboard, click the helloWorld tile and then the empty space below the tiles, and move the divider back as far left as it goes: the Output shows the two lines of helloWorld exactly as before.
/// Action 9: press Clear output: the Output of helloWorld is empty, exactly as before the first Check, and so is its console file, while the console file of start is unchanged.
/// Action 10: move the divider as far right as it goes, click the start tile and then the empty space below the tiles, and move the divider back as far left as it goes: the Output of start still says no problem was found once, exactly as before.
/// Action 11: end the manager.
final class TileSwitchTest extends ManagerTest{
  static final Path helloConsole= data.resolve("eclipse").resolve("hello_world").resolve("console.txt");
  static final Path startConsole= data.resolve("eclipse").resolve("start").resolve("console.txt");
  static final String ok= "--- ok: no problem found ---\n";
  final At managerShown= new At("managerShown",linux(3000));
  final Click focusTiles= new Click("focusTiles",linux(200,1500));
  final Click closeInformation= new Click("closeInformation",linux(150,167));
  final Area output= new Area("output",linux(91,264,3734,1861));
  final Click check= new Click("check",linux(164,111));
  final Click checkAgain= new Click("checkAgain",linux(164,111));
  final Click checkStart= new Click("checkStart",linux(164,111));
  final Click helloTile= new Click("helloTile",linux(138,192));
  final Click focusHello= new Click("focusHello",linux(200,1500));
  final Click clearOutput= new Click("clearOutput",linux(3785,233));
  final Click startTile= new Click("startTile",linux(265,192));
  final Click focusStart= new Click("focusStart",linux(200,1500));
  @Override protected void walk() throws Exception{
    clean();
    launch(project.toString());
    managerShown.go();
    focusTiles.go();
    left();
    closeInformation.go();
    look();
    var at= output.aim();
    var empty= pixels(at);
    check.go();
    until(()->Fs.readUtf8(helloConsole).equals(ok));
    look();
    until(()->!Arrays.equals(empty,pixels(at)));
    var once= pixels(at);
    checkAgain.go();
    until(()->Fs.readUtf8(helloConsole).equals(ok+ok));
    look();
    until(()->!Arrays.equals(once,pixels(at)));
    var twice= pixels(at);
    var run= new ProcessBuilder(launcher.toString(),other.toString()).start();
    until(()->!run.isAlive());
    assertEquals(0,run.exitValue());
    look();
    until(()->!Arrays.equals(twice,pixels(at)));
    assertEquals("",Fs.readUtf8(startConsole));
    checkStart.go();
    until(()->Fs.readUtf8(startConsole).equals(ok));
    look();
    until(()->Arrays.equals(once,pixels(at)));
    assertEquals(ok+ok,Fs.readUtf8(helloConsole));
    pilot.chord(KeyEvent.VK_F8);
    pilot.chord(KeyEvent.VK_END);
    look();
    until(()->!Arrays.equals(once,pixels(at)));
    var tiles= pixels(at);
    choose(helloTile,focusHello,at,tiles);
    look();
    until(()->Arrays.equals(twice,pixels(at)));
    clearOutput.go();
    until(()->Fs.readUtf8(helloConsole).isEmpty());
    look();
    until(()->Arrays.equals(empty,pixels(at)));
    assertEquals(ok,Fs.readUtf8(startConsole));
    pilot.chord(KeyEvent.VK_F8);
    pilot.chord(KeyEvent.VK_END);
    look();
    until(()->Arrays.equals(tiles,pixels(at)));
    choose(startTile,focusStart,at,tiles);
    look();
    until(()->Arrays.equals(once,pixels(at)));
    stopManagers();
  }
  private void choose(Click tile, Click focus, int[] at, int[] tiles){
    tile.go();
    look();
    until(()->!Arrays.equals(tiles,pixels(at)));
    focus.go();
    left();
  }
  private void left(){
    pilot.chord(KeyEvent.VK_F8);
    pilot.chord(KeyEvent.VK_HOME);
  }
}

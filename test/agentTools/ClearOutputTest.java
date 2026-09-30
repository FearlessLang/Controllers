package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.Arrays;

import tools.Fs;

/// Clear output empties the Output of a project and the console file the manager keeps for it, and what is printed afterwards starts again from the top of the empty Output.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, helloWorld was never compiled, and nothing is registered for .fearless.
/// Action 1: run the launcher on helloWorld: the manager window opens showing helloWorld, an idle project.
/// Action 2: click the empty space below the tiles, and move the divider between tiles and panel as far left as it goes with the keyboard.
/// Action 3: close the Information section: the Output is empty.
/// Action 4: press Check: the Output says no problem was found.
/// Action 5: press Check again: the Output says it twice.
/// Action 6: press Clear output: the Output is empty again, exactly as before the first Check, and so is its console file.
/// Action 7: press Check: the Output says no problem was found once, exactly as after the first Check.
/// Action 8: end the manager.
final class ClearOutputTest extends ManagerTest{
  static final Path console= data.resolve("eclipse").resolve("hello_world").resolve("console.txt");
  static final String ok= "--- ok: no problem found ---\n";
  final At managerShown= new At("managerShown",linux(3000));
  final Click focusTiles= new Click("focusTiles",linux(200,1500));
  final Click closeInformation= new Click("closeInformation",linux(150,167));
  final Area output= new Area("output",linux(91,264,3734,1861));
  final Click check= new Click("check",linux(164,111));
  final Click checkAgain= new Click("checkAgain",linux(164,111));
  final Click clearOutput= new Click("clearOutput",linux(3785,233));
  final Click checkAfterClear= new Click("checkAfterClear",linux(164,111));
  @Override protected void walk() throws Exception{
    clean();
    launch(project.toString());
    managerShown.go();
    focusTiles.go();
    pilot.chord(KeyEvent.VK_F8);
    pilot.chord(KeyEvent.VK_HOME);
    closeInformation.go();
    look();
    var at= output.aim();
    var empty= pixels(at);
    assertEquals("",Fs.readUtf8(console));
    check.go();
    until(()->Fs.readUtf8(console).equals(ok));
    look();
    until(()->!Arrays.equals(empty,pixels(at)));
    var once= pixels(at);
    checkAgain.go();
    until(()->Fs.readUtf8(console).equals(ok+ok));
    look();
    until(()->!Arrays.equals(once,pixels(at)));
    clearOutput.go();
    until(()->Fs.readUtf8(console).isEmpty());
    look();
    until(()->Arrays.equals(empty,pixels(at)));
    checkAfterClear.go();
    until(()->Fs.readUtf8(console).equals(ok));
    look();
    until(()->Arrays.equals(once,pixels(at)));
    stopManagers();
  }
}

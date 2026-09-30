package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.opentest4j.AssertionFailedError;

import tools.Fs;
import utils.Err;

/// A selected main that the project does not declare, while its mains are known, stops Run selected before anything runs: the Output says which selected main is not a main of the project and that it is removed from the selected mains, and the manager forgets it; the main still selected runs at the next Run selected.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, helloWorld was never compiled, nothing is registered for .fearless, and the clipboard holds the metadata naming helloWorld as hello_world, code, with hello.Hello1 and hello.Nope as the selected mains.
/// Action 1: run the launcher on helloWorld: the manager window opens showing helloWorld selected, an idle project.
/// Action 2: click the empty space below the tiles, and move the divider between tiles and panel as far left as it goes with the keyboard.
/// Action 3: press Become code: the manager remembers helloWorld as a code project.
/// Action 4: press Compile: the manager knows the mains of helloWorld.
/// Action 5: choose Edit project metadata... in its Manager menu: the metadata editor opens.
/// Action 6: click in its text, select all of it and paste.
/// Action 7: press Commit: the editor goes away and the manager remembers exactly what the clipboard holds.
/// Action 8: press Run selected: nothing runs; the Output says the selected hello.Nope is not a main of this project and is removed from the selected mains, and the manager remembers hello.Hello1 as the one selected main.
/// Action 9: press Run selected: the Output shows hello.Hello1 running, printing its line and exiting with 0, and the manager counts one run, of hello.Hello1, exited with 0.
/// Action 10: end the manager.
final class StaleMainNoteTest extends ManagerTest{
  static{ Err.setUp(AssertionFailedError.class,Assertions::assertEquals,Assertions::assertTrue); }
  static final String registry= """
    {
      "hello_world": {
        "path": "Str:%s",
        "kind": "code",
        "mains": [%s]
      }
    }
    """;
  final At managerShown= new At("managerShown",linux(3000));
  final Click focusTiles= new Click("focusTiles",linux(200,1500));
  final Click becomeCode= new Click("becomeCode",linux(400,139));
  final At codeShown= new At("codeShown",linux(1000));
  final Click compile= new Click("compile",linux(164,111));
  final At mainsShown= new At("mainsShown",linux(1000));
  final Area buttons= new Area("buttons",linux(2190,1350,170,40));
  final Click managerMenu= new Click("managerMenu",linux(98,79));
  final Click editMetadata= new Click("editMetadata",linux(128,103));
  final Click focusText= new Click("focusText",linux(1900,1200));
  final Click commit= new Click("commit",linux(2238,1370));
  final Click runSelected= new Click("runSelected",linux(164,111));
  final Click runSelectedAgain= new Click("runSelectedAgain",linux(164,111));
  @Override protected void walk() throws Exception{
    clean();
    var both= registry.formatted(project,"\"hello.Hello1\", \"hello.Nope\"");
    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(both),null);
    launch(project.toString());
    managerShown.go();
    focusTiles.go();
    pilot.chord(KeyEvent.VK_F8);
    pilot.chord(KeyEvent.VK_HOME);
    becomeCode.go();
    until(()->Fs.readUtf8(info).contains("\"code\""));
    codeShown.go();
    var noMains= Fs.readUtf8(state);
    compile.go();
    until(()->!noMains.equals(Fs.readUtf8(state)));
    mainsShown.go();
    look();
    var at= buttons.aim();
    var behind= pixels(at);
    managerMenu.go();
    editMetadata.go();
    until(()->!Arrays.equals(behind,pixels(at)));
    focusText.go();
    pilot.chord(KeyEvent.VK_CONTROL,KeyEvent.VK_A);
    pilot.chord(KeyEvent.VK_CONTROL,KeyEvent.VK_V);
    commit.go();
    until(()->Fs.readUtf8(info).equals(both));
    look();
    until(()->Arrays.equals(behind,pixels(at)));
    var compiled= """
      --- compiling helloWorld ---
      --- compile done ---
      """;
    assertEquals(compiled,Fs.readUtf8(console()));
    runSelected.go();
    var refused= compiled+"--- nothing to run: the selected \"hello.Nope\" are not mains of this project; they are removed from the selected mains ---\n";
    until(()->Fs.readUtf8(console()).equals(refused));
    assertEquals(registry.formatted(project,"\"hello.Hello1\""),Fs.readUtf8(info));
    runSelectedAgain.go();
    until(()->Fs.readUtf8(state).contains("\"runs\": \"1\"") && Fs.readUtf8(state).contains("\"exit\": \"0\""));
    Err.strCmp(refused+"""
      --- running hello.Hello1 ---
      hello world 3
      --- hello.Hello1 exited with 0 after [###]s ---
      """,Fs.readUtf8(console()));
    assertEquals("""
      {
        "hello_world": {
          "folder": "Str:%s",
          "kind": "code",
          "running": "",
          "runs": "1",
          "lastRun": "hello.Hello1",
          "exit": "0",
          "mains": {
            "hello.Hello1": "_hello/_rank_app.fear",
            "hello.Hello3": "_hello/_rank_app.fear",
            "hello.Hello4": "_hello/_rank_app.fear",
            "hello.Hello5": "_hello/_rank_app.fear",
            "hello.Hello6": "_hello/_rank_app.fear"
          },
          "problem": {}
        }
      }
      """.formatted(project),Fs.readUtf8(state));
    stopManagers();
  }
  private static Path console(){ return data.resolve("eclipse").resolve("hello_world").resolve("console.txt"); }
}

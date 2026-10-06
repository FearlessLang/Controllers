package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.event.KeyEvent;
import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.opentest4j.AssertionFailedError;

import tools.Fs;
import utils.Err;

/// All ticks every main of a compiled project, and Run selected runs them all, one after the other in the order they are listed, a main that fails showing its error and its exit code in the Output like any other; None then unticks them all and leaves nothing to run.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, helloWorld was never compiled, and nothing is registered for .fearless.
/// Action 1: run the launcher on helloWorld: the manager window opens showing helloWorld selected, an idle project.
/// Action 2: click the empty space below the tiles, and move the divider between tiles and panel as far left as it goes with the keyboard.
/// Action 3: press Become code: the manager remembers helloWorld as a code project.
/// Action 4: press Compile: the manager knows the five mains of helloWorld, and the panel shows them, none ticked, with Run selected disabled.
/// Action 5: press All: the manager remembers all five mains as selected, in the order they are listed, and the panel shows them ticked.
/// Action 6: press Run selected: the Output shows the five mains running in that order, each printing its line and exiting with 0, except hello.Hello6, which prints its error and the lines where it was raised, then its exit code; the manager counts five runs, the last of hello.Hello6.
/// Action 7: press None: the manager remembers helloWorld exactly as before All, with no selected main, and the mains and Run selected look exactly as right after the compile.
/// Action 8: end the manager.
final class AllMainsRunTest extends ManagerTest{
  static{ Err.setUp(AssertionFailedError.class,Assertions::assertEquals,Assertions::assertTrue); }
  final At managerShown= new At("managerShown",on("ubuntu-gnome",3000));
  final Click focusTiles= new Click("focusTiles",on("ubuntu-gnome",200,1500));
  final Click becomeCode= new Click("becomeCode",on("ubuntu-gnome",400,139));
  final At codeShown= new At("codeShown",on("ubuntu-gnome",1000));
  final Click compile= new Click("compile",on("ubuntu-gnome",164,111));
  final At mainsShown= new At("mainsShown",on("ubuntu-gnome",1000));
  final Area ticks= new Area("ticks",on("ubuntu-gnome",90,180,210,70));
  final Area runButton= new Area("runButton",on("ubuntu-gnome",134,98,114,30));
  final Click all= new Click("all",on("ubuntu-gnome",112,165));
  final Click runSelected= new Click("runSelected",on("ubuntu-gnome",164,111));
  final Click none= new Click("none",on("ubuntu-gnome",162,165));
  @Override protected void walk() throws Exception{
    clean();
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
    var boxes= ticks.aim();
    var button= runButton.aim();
    var unticked= pixels(boxes);
    var disabled= pixels(button);
    var code= Fs.readUtf8(info);
    all.go();
    until(()->Fs.readUtf8(info).contains("hello.Hello6"));
    assertEquals("""
      {
        "hello_world": {
          "path": "Str:%s",
          "kind": "code",
          "mains": ["hello.Hello1", "hello.Hello3", "hello.Hello4", "hello.Hello5", "hello.Hello6"]
        }
      }
      """.formatted(project),Fs.readUtf8(info));
    look();
    until(()->!Arrays.equals(unticked,pixels(boxes)));
    runSelected.go();
    until(()->Fs.readUtf8(state).contains("\"runs\": \"5\"") && !Fs.readUtf8(state).contains("\"exit\": \"-1\""));
    Err.strCmp("""
      {
        "hello_world": {
          "folder": "Str:%s",
          "kind": "code",
          "running": "",
          "runs": "5",
          "lastRun": "hello.Hello6",
          "exit": "[###]",
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
    Err.strCmp("""
      --- compiling helloWorld ---
      --- compile done ---
      --- running hello.Hello1 ---
      hello world 3
      --- hello.Hello1 exited with 0 after [###]s ---
      --- running hello.Hello3 ---
      [Hi]
      --- hello.Hello3 exited with 0 after [###]s ---
      --- running hello.Hello4 ---
      [1, 2, 3, 4]
      --- hello.Hello4 exited with 0 after [###]s ---
      --- running hello.Hello5 ---
      [11, 12, 13, 14]
      --- hello.Hello5 exited with 0 after [###]s ---
      --- running hello.Hello6 ---
      AAAAh
      imm Bar.bar error line: 16 in file _hello/_rank_app.fear
      imm Foo.foo error line: 15 in file _hello/_rank_app.fear
      imm Hello6.main(_) error line: 14 in file _hello/_rank_app.fear
      --- hello.Hello6 exited with [###] after [###]s ---
      """,Fs.readUtf8(data.resolve("eclipse").resolve("hello_world").resolve("console.txt")));
    none.go();
    until(()->code.equals(Fs.readUtf8(info)));
    look();
    until(()->Arrays.equals(unticked,pixels(boxes)) && Arrays.equals(disabled,pixels(button)));
    stopManagers();
  }
}

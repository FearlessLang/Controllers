package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.event.KeyEvent;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.opentest4j.AssertionFailedError;

import tools.Fs;
import utils.Err;

/// Registering an empty folder makes it a code project holding a marker named after the folder and a hello program, which compiles to one main that Run runs with no choice to make.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, nothing is registered for .fearless, and the folder empty beside the manager exists and holds nothing.
/// Action 1: run the launcher on empty: the manager window opens showing empty selected, empty now holds the marker empty.fearless and a hello program, and the manager remembers empty as a code project.
/// Action 2: click the empty space below the tiles, and move the divider between tiles and panel as far left as it goes with the keyboard.
/// Action 3: press Compile: the Output says the compile is done, and the manager knows empty.Hello as the one main of empty.
/// Action 4: press Run: the Output shows empty.Hello running, printing Hello World! and exiting with 0, and the manager counts one run of empty.Hello, exited with 0.
/// Action 5: end the manager.
final class EmptyFolderTest extends ManagerTest{
  static{ Err.setUp(AssertionFailedError.class,Assertions::assertEquals,Assertions::assertTrue); }
  static final Path empty= data.resolveSibling("empty");
  final At managerShown= new At("managerShown",on("ubuntu-gnome",3000));
  final Click focusTiles= new Click("focusTiles",on("ubuntu-gnome",200,1500));
  final Click compile= new Click("compile",on("ubuntu-gnome",164,111));
  final At runShown= new At("runShown",on("ubuntu-gnome",1000));
  final Click run= new Click("run",on("ubuntu-gnome",164,111));
  @Override protected void walk() throws Exception{
    clean();
    Fs.ensureDir(empty);
    launch(empty.toString());
    managerShown.go();
    assertEquals("\n",Fs.readUtf8(empty.resolve("empty.fearless")));
    assertEquals("""
      use base.Main as Main;
      use base.Lists as List;
      use base.Num as Num;
      use base.Void as Void;
      use base.Str as Str;

      Hello: Main { sys -> sys.out.println("Hello World!") }
      """,Fs.readUtf8(empty.resolve("_empty").resolve("_rank_app.fear")));
    assertEquals("""
      {
        "empty": {
          "path": "Str:%s",
          "kind": "code"
        }
      }
      """.formatted(empty),Fs.readUtf8(info));
    focusTiles.go();
    pilot.chord(KeyEvent.VK_F8);
    pilot.chord(KeyEvent.VK_HOME);
    var noMains= Fs.readUtf8(state);
    compile.go();
    until(()->!noMains.equals(Fs.readUtf8(state)));
    var compiled= """
      {
        "empty": {
          "folder": "Str:%s",
          "kind": "code",
          "running": "",
          "runs": "%s",
          "lastRun": "%s",
          "exit": "%s",
          "mains": {
            "empty.Hello": "_empty/_rank_app.fear"
          },
          "problem": {}
        }
      }
      """;
    assertEquals(compiled.formatted(empty,"0","","-1"),Fs.readUtf8(state));
    runShown.go();
    run.go();
    until(()->Fs.readUtf8(state).contains("\"exit\": \"0\""));
    assertEquals(compiled.formatted(empty,"1","empty.Hello","0"),Fs.readUtf8(state));
    Err.strCmp("""
      --- compiling empty ---
      --- compile done ---
      --- running empty.Hello ---
      Hello World!
      --- empty.Hello exited with 0 after [###]s ---
      """,Fs.readUtf8(data.resolve("eclipse").resolve("empty").resolve("console.txt")));
    stopManagers();
  }
  @Override @AfterEach void clean() throws Exception{
    super.clean();
    Fs.rmTree(empty);
  }
}

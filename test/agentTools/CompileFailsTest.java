package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;

import tools.Fs;

/// A compile that fails says why in the Output, tells the manager the file and line of the problem, and leaves the panel saying the project is invalid; once the source is fixed, Compile succeeds and the problem is gone.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, nothing is registered for .fearless, and the folder broken beside the manager holds only its marker and one source file whose main uses a type that does not exist.
/// Action 1: run the launcher on broken: the manager window opens showing broken, an idle project.
/// Action 2: click the empty space below the tiles, and move the divider between tiles and panel as far left as it goes with the keyboard.
/// Action 3: press Become code: the manager remembers broken as a code project.
/// Action 4: press Compile: the Output says the compile failed and why, the manager knows the file and line of the problem and no main, and the mains row of the panel changes.
/// Action 5: replace the source file with one whose main prints a text.
/// Action 6: press Compile: the Output says the compile is done, the manager knows the main of broken and no problem, and the mains row of the panel changes again.
/// Action 7: end the manager.
final class CompileFailsTest extends ManagerTest{
  static final Path broken= data.resolveSibling("broken");
  static final Path source= broken.resolve("_broken").resolve("_rank_app.fear");
  static final Path console= data.resolve("eclipse").resolve("broken").resolve("console.txt");
  final At managerShown= new At("managerShown",linux(3000),windows(3000));
  final Click focusTiles= new Click("focusTiles",linux(200,1500),windows(200,400));
  final Click becomeCode= new Click("becomeCode",linux(400,139),windows(332,94));
  final At codeShown= new At("codeShown",linux(1000),windows(1000));
  final Area rows= new Area("rows",linux(80,128,1320,45),windows(22,82,600,48));
  final Click compile= new Click("compile",linux(164,111),windows(102,67));
  final Click compileFixed= new Click("compileFixed",linux(164,111),windows(102,67));
  final At mainShown= new At("mainShown",linux(1000),windows(1000));
  @Override protected void walk() throws Exception{
    clean();
    Fs.writeUtf8(broken.resolve("broken.fearless"),"\n");
    Fs.writeUtf8(source,"use base.Main as Main;\n\nHello:Main{s->Nope.nope}\n");
    launch(broken.toString());
    managerShown.go();
    focusTiles.go();
    pilot.chord(KeyEvent.VK_F8);
    pilot.chord(KeyEvent.VK_HOME);
    becomeCode.go();
    until(()->Fs.readUtf8(info).contains("\"code\""));
    codeShown.go();
    look();
    var at= rows.aim();
    var code= pixels(at);
    compile.go();
    until(()->Fs.readUtf8(state).contains("\"line\""));
    var failed= """
      --- compiling broken ---
      In file: fear:/_broken/_rank_app.fear

      003| Hello:Main{s->Nope.nope}
         |               ^^^^^

      While inspecting a type name
      Type "Nope" is not declared in package "broken" and is not made visible via "use".
      In scope: "Hello", "Main".
      Error 7 WellFormedness
      --- compile failed with 1 ---
      """;
    assertEquals(failed,Fs.readUtf8(console));
    assertEquals("""
      {
        "broken": {
          "folder": "Str:%s",
          "kind": "code",
          "running": "",
          "runs": "0",
          "lastRun": "",
          "exit": "-1",
          "mains": {},
          "problem": {
            "file": "_broken/_rank_app.fear",
            "line": "003",
            "message": "Str:In file: fear:/_broken/_rank_app.fear\\n\\n003| Hello:Main{s->Nope.nope}\\n   |               ^^^^^\\n\\nWhile inspecting a type name\\nType \\"Nope\\" is not declared in package \\"broken\\" and is not made visible via \\"use\\".\\nIn scope: \\"Hello\\", \\"Main\\".\\nError 7 WellFormedness\\n"
          }
        }
      }
      """.formatted(escaped(broken)),Fs.readUtf8(state));
    look();
    until(()->!Arrays.equals(code,pixels(at)));
    var invalid= pixels(at);
    Fs.writeUtf8(source,"use base.Main as Main;\n\nHello:Main{s->base.Debug#(\"fixed\")}\n");
    compileFixed.go();
    until(()->Fs.readUtf8(state).contains("broken.Hello"));
    assertEquals(failed+"--- compiling broken ---\n--- compile done ---\n",Fs.readUtf8(console));
    assertEquals("""
      {
        "broken": {
          "folder": "Str:%s",
          "kind": "code",
          "running": "",
          "runs": "0",
          "lastRun": "",
          "exit": "-1",
          "mains": {
            "broken.Hello": "_broken/_rank_app.fear"
          },
          "problem": {}
        }
      }
      """.formatted(escaped(broken)),Fs.readUtf8(state));
    mainShown.go();
    look();
    assertFalse(Arrays.equals(code,pixels(at)));
    assertFalse(Arrays.equals(invalid,pixels(at)));
    stopManagers();
  }
  @Override @AfterEach void clean() throws Exception{
    super.clean();
    Fs.rmTree(broken);
  }
}

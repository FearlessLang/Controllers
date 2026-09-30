package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;

import tools.Fs;

/// Saving an edited source file of a compiled project is noticed by the manager by itself: it forgets the mains it knew and the panel looks exactly as before the first compile, so Compile is offered again and finds the mains of the edited source.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, nothing is registered for .fearless, and the folder draft beside the manager holds only its marker and one source file with the main First.
/// Action 1: run the launcher on draft: the manager window opens showing draft, an idle project.
/// Action 2: click the empty space below the tiles, and move the divider between tiles and panel as far left as it goes with the keyboard.
/// Action 3: press Become code: the manager remembers draft as a code project.
/// Action 4: press Compile: the Output says the compile is done, the manager knows draft.First as the one main of draft, and the mains row of the panel changes.
/// Action 5: save the source file with a second main Second added: the manager by itself knows no main of draft any more, the Output says nothing new, and the kind button and the mains row look exactly as before the compile.
/// Action 6: press the same button again: the Output says a second compile is done, the manager knows draft.First and draft.Second, and the mains row looks different from both its earlier looks.
/// Action 7: end the manager.
final class RecompileAfterEditTest extends ManagerTest{
  static final Path draft= data.resolveSibling("draft");
  static final Path source= draft.resolve("_draft").resolve("_rank_app.fear");
  static final Path console= data.resolve("eclipse").resolve("draft").resolve("console.txt");
  static final String first= "use base.Main as Main;\n\nFirst:Main{s->base.Debug#(\"first\")}\n";
  static final String compiled= "--- compiling draft ---\n--- compile done ---\n";
  static final String states= """
    {
      "draft": {
        "folder": "Str:%s",
        "kind": "code",
        "running": "",
        "runs": "0",
        "lastRun": "",
        "exit": "-1",
        "mains": %s,
        "problem": {}
      }
    }
    """;
  final At managerShown= new At("managerShown",linux(3000));
  final Click focusTiles= new Click("focusTiles",linux(200,1500));
  final Click becomeCode= new Click("becomeCode",linux(400,139));
  final At codeShown= new At("codeShown",linux(1000));
  final Area rows= new Area("rows",linux(80,128,1320,45));
  final Click compile= new Click("compile",linux(164,111));
  final Click compileEdited= new Click("compileEdited",linux(164,111));
  @Override protected void walk() throws Exception{
    clean();
    Fs.writeUtf8(draft.resolve("draft.fearless"),"\n");
    Fs.writeUtf8(source,first);
    launch(draft.toString());
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
    until(()->Fs.readUtf8(state).contains("draft.First"));
    assertEquals(compiled,Fs.readUtf8(console));
    assertEquals(states.formatted(draft,"""
      {
            "draft.First": "_draft/_rank_app.fear"
          }"""),Fs.readUtf8(state));
    look();
    until(()->!Arrays.equals(code,pixels(at)));
    var one= pixels(at);
    Fs.writeUtf8(source,first+"Second:Main{s->base.Debug#(\"second\")}\n");
    until(()->!Fs.readUtf8(state).contains("draft.First"));
    assertEquals(states.formatted(draft,"{}"),Fs.readUtf8(state));
    assertEquals(compiled,Fs.readUtf8(console));
    look();
    until(()->Arrays.equals(code,pixels(at)));
    compileEdited.go();
    until(()->Fs.readUtf8(state).contains("draft.Second"));
    assertEquals(compiled+compiled,Fs.readUtf8(console));
    assertEquals(states.formatted(draft,"""
      {
            "draft.First": "_draft/_rank_app.fear",
            "draft.Second": "_draft/_rank_app.fear"
          }"""),Fs.readUtf8(state));
    look();
    until(()->!Arrays.equals(code,pixels(at)));
    assertFalse(Arrays.equals(one,pixels(at)));
    stopManagers();
  }
  @Override @AfterEach void clean() throws Exception{
    super.clean();
    Fs.rmTree(draft);
  }
}

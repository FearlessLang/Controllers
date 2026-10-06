package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.stream.IntStream;

import tools.Fs;

/// Committing metadata that breaks the rules is refused: a note says exactly where and why, the manager keeps remembering what it did, and the metadata editor stays open with the text as it was typed; closing the editor then leaves the window exactly as before it opened.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, helloWorld was never compiled, and nothing is registered for .fearless.
/// Action 1: run the launcher on helloWorld: the manager window opens showing helloWorld, an idle project.
/// Action 2: click the empty space below the tiles.
/// Action 3: choose Edit project metadata... in its Manager menu: the metadata editor opens.
/// Action 4: double click the kind idle in the text, type lazy over it and press Commit: a note shows the line of the kind and says the kind must be one of the four kinds, not lazy, and the manager still remembers helloWorld as idle.
/// Action 5: press OK: the note goes away.
/// Action 6: click in the text of the editor, select all of it and copy it: the text copied is what the manager remembers with lazy in place of idle.
/// Action 7: press Close: the editor goes away, the window is exactly as before it opened, and the manager still remembers helloWorld as idle.
/// Action 8: end the manager.
final class EditMetadataInvalidTest extends ManagerTest{
  static final String registry= """
    {
      "hello_world": {
        "path": "Str:%s",
        "kind": "%s"
      }
    }
    """;
  final At managerShown= new At("managerShown",linux(3000),windows(3000));
  final Click focusTiles= new Click("focusTiles",linux(200,1500),windows(200,400));
  final Area window= new Area("window",linux(68,32,3772,2098),windows(0,24,1280,624));
  final Click managerMenu= new Click("managerMenu",linux(98,79),windows(31,33));
  final Click editMetadata= new Click("editMetadata",linux(128,103),windows(61,58));
  final At editorShown= new At("editorShown",linux(2000),windows(2000));
  final DoubleClick kindIdle= new DoubleClick("kindIdle",linux(1672,898),windows(360,123));
  final Click commit= new Click("commit",linux(2238,1370),windows(924,619));
  final At noteShown= new At("noteShown",linux(1000),windows(1000));
  final Click ok= new Click("ok",linux(1952,1183),windows(639,414));
  final Click focusText= new Click("focusText",linux(1900,1200),windows(700,400));
  final Click close= new Click("close",linux(2317,1370),windows(1003,619));
  @Override protected void walk() throws Exception{
    clean();
    launch(project.toString());
    managerShown.go();
    var idle= registry.formatted(slashed(project),"idle");
    assertEquals(idle,Fs.readUtf8(info));
    focusTiles.go();
    look();
    var at= window.aim();
    var before= pixels(at);
    managerMenu.go();
    editMetadata.go();
    editorShown.go();
    kindIdle.go();
    IntStream.of(KeyEvent.VK_L,KeyEvent.VK_A,KeyEvent.VK_Z,KeyEvent.VK_Y).forEach(pilot::chord);
    commit.go();
    until(()->Files.exists(notes) && !Fs.readUtf8(notes).isEmpty());
    noteShown.go();
    var note= """
      In file: %s

      004|     "kind": "lazy"
         |             ^^^^^^

      While inspecting the file
      "kind" must be one of "idle", "code", "data:readOnly" or "data:readWrite", not "lazy".
      """.formatted(info);
    assertEquals(note,Fs.readUtf8(notes));
    assertEquals(idle,Fs.readUtf8(info));
    ok.go();
    focusText.go();
    pilot.chord(KeyEvent.VK_CONTROL,KeyEvent.VK_A);
    pilot.chord(KeyEvent.VK_CONTROL,KeyEvent.VK_C);
    assertEquals(registry.formatted(slashed(project),"lazy"),Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor));
    close.go();
    look();
    until(()->Arrays.equals(before,pixels(at)));
    assertEquals(idle,Fs.readUtf8(info));
    assertEquals(note,Fs.readUtf8(notes));
    stopManagers();
  }
}

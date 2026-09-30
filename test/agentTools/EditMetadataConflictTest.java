package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.stream.IntStream;

import tools.Fs;

/// A commit in the metadata editor is refused when what the manager remembers changed while the editor was open: a note says so and to open the editor again, nothing is saved, and the editor keeps the typed text; opened again, the editor shows what the manager remembers now, and the same edit is committed.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, helloWorld was never compiled, and nothing is registered for .fearless.
/// Action 1: run the launcher on helloWorld: the manager window opens showing helloWorld, an idle project.
/// Action 2: choose Edit project metadata... in its Manager menu: the metadata editor opens.
/// Action 3: double click the kind idle in the text and type code over it.
/// Action 4: run the launcher on helloStackTraces: it ends at once, and the manager remembers helloWorld and helloStackTraces, both idle.
/// Action 5: press Commit: a note shows saying the metadata is not committed since the projects the manager remembers changed while it was edited, the manager notes file holds exactly that text, and the manager still remembers both as idle.
/// Action 6: press OK: the note goes away and the editor is exactly as before action 5.
/// Action 7: click in the text of the editor, select all of it and copy it: the text copied is what the manager remembered before action 4 with code in place of idle.
/// Action 8: press Close: the editor goes away.
/// Action 9: choose Edit project metadata... in the Manager menu again: the metadata editor opens.
/// Action 10: click in its text, select all of it and copy it: the text copied is exactly what the manager remembers.
/// Action 11: double click the kind idle of helloWorld in the text, type code over it and press Commit: the editor goes away, and the manager remembers helloWorld as code and helloStackTraces as idle.
/// Action 12: end the manager.
final class EditMetadataConflictTest extends ManagerTest{
  static final String one= """
    {
      "hello_world": {
        "path": "Str:%s",
        "kind": "%s"
      }
    }
    """;
  static final String two= """
    {
      "hello_world": {
        "path": "Str:%s",
        "kind": "%s"
      },
      "start": {
        "path": "Str:%s",
        "kind": "idle"
      }
    }
    """;
  final At managerShown= new At("managerShown",linux(3000));
  final Area buttons= new Area("buttons",linux(2190,1350,170,40));
  final Click managerMenu= new Click("managerMenu",linux(98,79));
  final Click editMetadata= new Click("editMetadata",linux(128,103));
  final DoubleClick kindIdle= new DoubleClick("kindIdle",linux(1672,898));
  final Area note= new Area("note",linux(1600,1060,700,40));
  final Click commit= new Click("commit",linux(2238,1370));
  final Click ok= new Click("ok",linux(1953,1136));
  final Click focusText= new Click("focusText",linux(1900,1200));
  final Click close= new Click("close",linux(2317,1370));
  final Click managerMenuAgain= new Click("managerMenuAgain",linux(98,79));
  final Click editMetadataAgain= new Click("editMetadataAgain",linux(128,103));
  final Click focusTextAgain= new Click("focusTextAgain",linux(1900,1200));
  final DoubleClick kindIdleAgain= new DoubleClick("kindIdleAgain",linux(1672,898));
  final Click commitAgain= new Click("commitAgain",linux(2238,1370));
  @Override protected void walk() throws Exception{
    clean();
    launch(project.toString());
    managerShown.go();
    assertEquals(one.formatted(project,"idle"),Fs.readUtf8(info));
    look();
    var at= buttons.aim();
    var behind= pixels(at);
    managerMenu.go();
    editMetadata.go();
    until(()->!Arrays.equals(behind,pixels(at)));
    kindIdle.go();
    typeCode();
    var handed= new ProcessBuilder(launcher.toString(),other.toString()).start();
    until(()->!handed.isAlive());
    assertEquals(0,handed.exitValue());
    var both= two.formatted(project,"idle",other);
    until(()->Fs.readUtf8(info).equals(both));
    var where= note.aim();
    var edited= pixels(where);
    commit.go();
    until(()->Files.exists(notes) && Fs.readUtf8(notes).equals("""
      The project metadata is not committed: projects.info changed while it was edited.
      Close the editor, and choose Edit project metadata again to edit what projects.info holds now.
      """));
    until(()->!Arrays.equals(edited,pixels(where)));
    assertEquals(both,Fs.readUtf8(info));
    ok.go();
    until(()->Arrays.equals(edited,pixels(where)));
    focusText.go();
    assertEquals(one.formatted(project,"code"),copy());
    close.go();
    until(()->Arrays.equals(behind,pixels(at)));
    managerMenuAgain.go();
    editMetadataAgain.go();
    until(()->!Arrays.equals(behind,pixels(at)));
    focusTextAgain.go();
    assertEquals(both,copy());
    kindIdleAgain.go();
    typeCode();
    commitAgain.go();
    until(()->Fs.readUtf8(info).equals(two.formatted(project,"code",other)));
    until(()->Arrays.equals(behind,pixels(at)));
    stopManagers();
  }
  private void typeCode(){ IntStream.of(KeyEvent.VK_C,KeyEvent.VK_O,KeyEvent.VK_D,KeyEvent.VK_E).forEach(pilot::chord); }
  private Object copy() throws Exception{
    pilot.chord(KeyEvent.VK_CONTROL,KeyEvent.VK_A);
    pilot.chord(KeyEvent.VK_CONTROL,KeyEvent.VK_C);
    return Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
  }
}

package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.stream.IntStream;

import tools.Fs;

/// Editing the kind of a project in the metadata editor and committing it changes the project as its kind button would: the manager remembers the new kind and the panel shows the project as that kind, and the kind button it then offers brings the project back exactly as it was.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, helloWorld was never compiled, and nothing is registered for .fearless.
/// Action 1: run the launcher on helloWorld: the manager window opens showing helloWorld selected, an idle project.
/// Action 2: click the empty space below the tiles, and move the divider between tiles and panel as far left as it goes with the keyboard.
/// Action 3: choose Edit project metadata... in its Manager menu: the metadata editor opens.
/// Action 4: click in its text, select all of it and copy it: the text copied is exactly what the manager remembers.
/// Action 5: double click the kind idle in the text, type code over it and press Commit: the manager remembers helloWorld as a code project, and the top of the panel changes.
/// Action 6: press Back to idle: the manager remembers helloWorld as an idle project exactly as before the commit, and the top of the panel is back exactly as it was then.
/// Action 7: end the manager.
final class EditMetadataCommitTest extends ManagerTest{
  final At managerShown= new At("managerShown",linux(3000),windows(3000));
  final Click focusTiles= new Click("focusTiles",linux(200,1500),windows(200,400));
  final Area head= new Area("head",linux(80,94,1320,78),windows(22,50,600,60));
  final Click managerMenu= new Click("managerMenu",linux(98,79),windows(31,33));
  final Click editMetadata= new Click("editMetadata",linux(128,103),windows(61,58));
  final At editorShown= new At("editorShown",linux(2000),windows(2000));
  final Click focusText= new Click("focusText",linux(1900,1200),windows(700,400));
  final DoubleClick kindIdle= new DoubleClick("kindIdle",linux(1672,898),windows(360,123));
  final Click commit= new Click("commit",linux(2238,1370),windows(924,619));
  final Click backToIdle= new Click("backToIdle",linux(138,140),windows(70,94));
  @Override protected void walk() throws Exception{
    clean();
    launch(project.toString());
    managerShown.go();
    focusTiles.go();
    pilot.chord(KeyEvent.VK_F8);
    pilot.chord(KeyEvent.VK_HOME);
    look();
    var at= head.aim();
    var idle= pixels(at);
    var registry= """
      {
        "hello_world": {
          "path": "Str:%s",
          "kind": "%s"
        }
      }
      """;
    assertEquals(registry.formatted(slashed(project),"idle"),Fs.readUtf8(info));
    managerMenu.go();
    editMetadata.go();
    editorShown.go();
    focusText.go();
    pilot.chord(KeyEvent.VK_CONTROL,KeyEvent.VK_A);
    pilot.chord(KeyEvent.VK_CONTROL,KeyEvent.VK_C);
    assertEquals(Fs.readUtf8(info),Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor));
    kindIdle.go();
    IntStream.of(KeyEvent.VK_C,KeyEvent.VK_O,KeyEvent.VK_D,KeyEvent.VK_E).forEach(pilot::chord);
    commit.go();
    until(()->Fs.readUtf8(info).contains("\"code\""));
    assertEquals(registry.formatted(slashed(project),"code"),Fs.readUtf8(info));
    look();
    until(()->!Arrays.equals(idle,pixels(at)));
    backToIdle.go();
    until(()->Fs.readUtf8(info).contains("\"idle\""));
    assertEquals(registry.formatted(slashed(project),"idle"),Fs.readUtf8(info));
    look();
    until(()->Arrays.equals(idle,pixels(at)));
    stopManagers();
  }
}

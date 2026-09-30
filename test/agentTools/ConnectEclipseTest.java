package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;

import tools.Fs;
import utils.OneOr;

/// Manager > Connect Eclipse... refuses a folder holding no Eclipse installation with a note saying where it looked, and connects the installation in the folder chosen next: the plugin shipped with the manager replaces the older one in its dropins folder, beside a file naming the manager folder and the compiled base library.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, nothing is registered for .fearless, the folder ide beside the manager holds an Eclipse installation (its .eclipseproduct file) with an older plugin in dropins/fearless/plugins, and the clipboard holds the path of its dropins folder.
/// Action 1: run the launcher: the manager window opens with no tile.
/// Action 2: choose Connect Eclipse... in its Manager menu: a chooser opens.
/// Action 3: click its file name field, paste and press Enter: the chooser goes away and a note says no Eclipse installation is in the dropins folder, and ide is unchanged.
/// Action 4: press OK: the note goes away and the window is exactly as before.
/// Action 5: choose Connect Eclipse... in the Manager menu again: the chooser opens again.
/// Action 6: click its file name field, paste, erase the last folder name and press Enter: the chooser goes away and a note says the installation in ide is now connected; its dropins folder holds exactly the plugin shipped with the manager, in place of the older one, and the file naming the manager folder and the compiled base library.
/// Action 7: press OK: the note goes away and the window is exactly as before.
/// Action 8: end the manager.
final class ConnectEclipseTest extends ManagerTest{
  static final Path ide= data.resolveSibling("ide");
  static final Path dropins= ide.resolve("dropins");
  static final Path fearless= dropins.resolve("fearless");
  static final Path lib= app.resolve("lib").resolve("app");
  static final Path shipped= lib.resolve("eclipsePlugin");
  final At managerShown= new At("managerShown",linux(3000));
  final Area window= new Area("window",linux(68,32,3772,2098));
  final Click managerMenu= new Click("managerMenu",linux(98,79));
  final Click connectEclipse= new Click("connectEclipse",linux(128,145));
  final At chooserShown= new At("chooserShown",linux(1500));
  final Click fileName= new Click("fileName",linux(2000,1180));
  final At noteShown= new At("noteShown",linux(1000));
  final Click ok= new Click("ok",linux(1952,1164));
  final Click managerMenuAgain= new Click("managerMenuAgain",linux(98,79));
  final Click connectEclipseAgain= new Click("connectEclipseAgain",linux(128,145));
  final At chooserShownAgain= new At("chooserShownAgain",linux(1500));
  final Click fileNameAgain= new Click("fileNameAgain",linux(2000,1180));
  final At noteShownAgain= new At("noteShownAgain",linux(1000));
  final Click okAgain= new Click("okAgain",linux(1952,1174));
  @Override protected void walk() throws Exception{
    clean();
    Fs.writeUtf8(ide.resolve(".eclipseproduct"),"name=Eclipse Platform\n");
    Fs.writeUtf8(fearless.resolve("plugins").resolve("fearlessPluginProject_3.2.0.jar"),"older\n");
    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(dropins.toString()),null);
    var jar= Fs.walk(shipped,s->OneOr.of("one plugin",s.filter(Files::isRegularFile))).getFileName().toString();
    launch();
    managerShown.go();
    look();
    var at= window.aim();
    var before= pixels(at);
    managerMenu.go();
    connectEclipse.go();
    chooserShown.go();
    fileName.go();
    pilot.chord(KeyEvent.VK_CONTROL,KeyEvent.VK_V);
    pilot.chord(KeyEvent.VK_ENTER);
    var none= """
      Eclipse is not connected: no Eclipse installation, a folder holding the file ".eclipseproduct", is in
        %s
      or in its folders "eclipse" or "Contents/Eclipse", or in those of a folder of it.

      Select the Eclipse program, the folder holding it, or the folder Eclipse was unzipped into.
      """.formatted(dropins);
    until(()->Files.exists(notes) && Fs.readUtf8(notes).equals(none));
    noteShown.go();
    assertEquals(List.of(".eclipseproduct","dropins/fearless/plugins/fearlessPluginProject_3.2.0.jar"),files());
    ok.go();
    look();
    until(()->Arrays.equals(before,pixels(at)));
    managerMenuAgain.go();
    connectEclipseAgain.go();
    chooserShownAgain.go();
    fileNameAgain.go();
    pilot.chord(KeyEvent.VK_CONTROL,KeyEvent.VK_V);
    IntStream.range(0,"/dropins".length()).forEach(_->pilot.chord(KeyEvent.VK_BACK_SPACE));
    pilot.chord(KeyEvent.VK_ENTER);
    var connected= none+"""
      Eclipse is now connected:
      %s

      Restart Eclipse: every project this manager knows appears in its Fearless
      perspective. File > New makes a project, Project > Build compiles it, the
      Run button runs it, and the Terminate button of its console stops it.
      """.formatted(ide);
    until(()->Fs.readUtf8(notes).equals(connected));
    noteShownAgain.go();
    assertEquals(List.of(".eclipseproduct","dropins/fearless/manager.info","dropins/fearless/plugins/"+jar),files());
    assertEquals(-1L,Files.mismatch(shipped.resolve(jar),fearless.resolve("plugins").resolve(jar)));
    assertEquals("""
      {
        "manager": "Str:%s",
        "baseCache": "Str:%s"
      }
      """.formatted(data,lib.resolve("stdLib").resolve("baseCache")),Fs.readUtf8(fearless.resolve("manager.info")));
    okAgain.go();
    look();
    until(()->Arrays.equals(before,pixels(at)));
    stopManagers();
  }
  private static List<String> files(){ return Fs.walk(ide,s->s.filter(Files::isRegularFile).map(p->ide.relativize(p).toString()).sorted().toList()); }
  @Override @AfterEach void clean() throws Exception{
    super.clean();
    Fs.rmTree(ide);
  }
}

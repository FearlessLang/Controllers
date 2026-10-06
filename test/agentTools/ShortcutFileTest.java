package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.awt.Desktop;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterEach;

import tools.Fs;

/// Compiling a project whose main claims a Shortcut makes the manager create the shortcut file in the project, and opening that file from the file manager runs the main in the manager that is already running.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, nothing is registered for .fearless, the file manager remembers no place for its windows, and the folder shortcuts beside the manager holds its marker, one package with a main claiming a Shortcut and a square icon.
/// Action 1: run the launcher on shortcuts: the manager window opens showing shortcuts, an idle project.
/// Action 2: click the empty space below the tiles, and move the divider between tiles and panel as far left as it goes with the keyboard.
/// Action 3: press Become code: the manager remembers shortcuts as a code project.
/// Action 4: press Compile: the Output says the compile is done, and the folder holds one shortcut file named after the main.
/// Action 5: open the file manager on shortcuts.
/// Action 6: double click the shortcut file: the Output says the main runs and ends with exit code 0, and the manager that was already running is the only one.
/// Action 7: end the manager and close the file manager window.
final class ShortcutFileTest extends ManagerTest{
  static final Path shortcuts= data.resolveSibling("shortcuts");
  static final Path console= data.resolve("eclipse").resolve("shortcuts").resolve("console.txt");
  final At managerShown= new At("managerShown",windows(3000));
  final Click focusTiles= new Click("focusTiles",windows(200,400));
  final Click becomeCode= new Click("becomeCode",windows(332,94));
  final Click compile= new Click("compile",windows(102,67));
  final At filesShown= new At("filesShown",windows(4000));
  final DoubleClick openShortcut= new DoubleClick("openShortcut",windows(500,283));
  final Click closeFiles= new Click("closeFiles",windows(1016,58));
  static long managers(){ return ProcessHandle.allProcesses().filter(p->p.info().command().filter(launcher.toString()::equals).isPresent()).count(); }
  @Override protected void walk() throws Exception{
    clean();
    Fs.writeUtf8(shortcuts.resolve("shortcuts.fearless"),"\n");
    Fs.writeUtf8(shortcuts.resolve("_hello").resolve("_rank_app.fear"),"use base.Main as Main;\nuse base.Shortcut as Shortcut;\n\nHello:Main, Shortcut[IconsHello]{s->base.Debug#(`hello`)}\n");
    Fs.ensureDir(shortcuts.resolve("_hello").resolve("icons"));
    Fs.ofV(()->ImageIO.write(new BufferedImage(64,64,BufferedImage.TYPE_INT_RGB),"png",shortcuts.resolve("_hello").resolve("icons").resolve("hello.png").toFile()));
    launch(shortcuts.toString());
    managerShown.go();
    focusTiles.go();
    pilot.chord(KeyEvent.VK_F8);
    pilot.chord(KeyEvent.VK_HOME);
    becomeCode.go();
    until(()->Fs.readUtf8(info).contains("\"code\""));
    compile.go();
    until(()->Fs.readUtf8(console).contains("--- compile done ---"));
    assertEquals(List.of("hello.fapp"),Fs.walk(shortcuts,s->s.filter(p->p.getParent().equals(shortcuts)).map(p->p.getFileName().toString().replaceAll("[0-9]{3}$","")).filter(n->n.startsWith("hello.")).toList()));
    assertFalse(Fs.readUtf8(console).contains("--- running "));
    var owners= managers();
    forgetWindowPlaces();
    Desktop.getDesktop().open(shortcuts.toFile());
    filesShown.go();
    openShortcut.go();
    until(()->Fs.readUtf8(console).contains(" exited with 0 after "));
    assertEquals(owners,managers());
    stopManagers();
    closeFiles.go();
  }
  @Override @AfterEach void clean() throws Exception{
    super.clean();
    Fs.rmTree(shortcuts);
  }
}

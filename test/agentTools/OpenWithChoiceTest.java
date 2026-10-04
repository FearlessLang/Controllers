package agentTools;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.awt.Desktop;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.opentest4j.AssertionFailedError;

import tools.Fs;
import utils.Err;

/// Opening a file whose extension is claimed by two mains asks which one to run, and runs only the one picked.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, nothing is registered for .fearless, the file manager remembers no place for its windows, and the folder choice beside the manager holds its marker, one package with two mains both claiming OpenWith for ffile123, a square icon, and a file note.ffile123.
/// Action 1: run the launcher on choice: the manager window opens showing choice, an idle project.
/// Action 2: click the empty space below the tiles, and move the divider between tiles and panel as far left as it goes with the keyboard.
/// Action 3: press Become code: the manager remembers choice as a code project.
/// Action 4: press Compile: the Output says the compile is done.
/// Action 5: open the file manager on choice.
/// Action 6: double click note.ffile123: a window lists the two mains.
/// Action 7: select the second main and press Run: the Output says the second main runs and ends with exit code 0, and the first main did not run.
/// Action 8: end the manager and close the file manager window.
final class OpenWithChoiceTest extends ManagerTest{
  static{ Err.setUp(AssertionFailedError.class,Assertions::assertEquals,Assertions::assertTrue); }
  static final Path choice= data.resolveSibling("choice");
  static final Path console= data.resolve("eclipse").resolve("choice").resolve("console.txt");
  final At managerShown= new At("managerShown",windows(3000));
  final Click focusTiles= new Click("focusTiles",windows(200,400));
  final Click becomeCode= new Click("becomeCode",windows(332,94));
  final Click compile= new Click("compile",windows(102,67));
  final At filesShown= new At("filesShown",windows(4000));
  final DoubleClick openNote= new DoubleClick("openNote",windows(480,312));
  final At chooserShown= new At("chooserShown",windows(2000));
  final Click pickSecond= new Click("pickSecond",windows(650,247));
  final Click runPicked= new Click("runPicked",windows(698,485));
  final Click closeFiles= new Click("closeFiles",windows(1016,58));
  @Override protected void walk() throws Exception{
    clean();
    Fs.writeUtf8(choice.resolve("choice.fearless"),"\n");
    Fs.writeUtf8(choice.resolve("_hello").resolve("_rank_app.fear"),"use base.Main as Main;\nuse base.OpenWith as OpenWith;\n\nFirst:Main, OpenWith[IconsHello,`ffile123`]{s->base.Debug#(`first`)}\nSecond:Main, OpenWith[IconsHello,`ffile123`]{s->base.Debug#(`second`)}\n");
    Fs.ensureDir(choice.resolve("_hello").resolve("icons"));
    Fs.ofV(()->ImageIO.write(new BufferedImage(64,64,BufferedImage.TYPE_INT_RGB),"png",choice.resolve("_hello").resolve("icons").resolve("hello.png").toFile()));
    Fs.writeUtf8(choice.resolve("note.ffile123"),"dear reader\n");
    launch(choice.toString());
    managerShown.go();
    focusTiles.go();
    pilot.chord(KeyEvent.VK_F8);
    pilot.chord(KeyEvent.VK_HOME);
    becomeCode.go();
    until(()->Fs.readUtf8(info).contains("\"code\""));
    compile.go();
    until(()->Fs.readUtf8(console).contains("--- compile done ---"));
    assertFalse(Fs.readUtf8(console).contains("--- running "));
    forgetWindowPlaces();
    Desktop.getDesktop().open(choice.toFile());
    filesShown.go();
    openNote.go();
    chooserShown.go();
    pickSecond.go();
    runPicked.go();
    until(()->Fs.readUtf8(console).contains(" exited with 0 after "));
    Err.strCmp("""
      --- compiling choice ---
      --- compile done ---
      --- running hello.Second ---
      second
      --- hello.Second exited with 0 after [###]s ---
      """,Fs.readUtf8(console));
    stopManagers();
    closeFiles.go();
  }
  @Override @AfterEach void clean() throws Exception{
    super.clean();
    Fs.rmTree(choice);
  }
}

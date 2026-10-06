package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.awt.Desktop;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.opentest4j.AssertionFailedError;

import tools.Fs;
import utils.Err;

/// A main claiming OpenWith for a system extension is compiled only once the user allows the extension; from then on opening a file with that extension in the file manager runs the main, and removing the extension in the System extensions view makes the project stop claiming it.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, nothing is registered for .fearless, the file manager remembers no place for its windows, and the folder opens beside the manager holds its marker, one package with a main claiming OpenWith for fzz and a square icon, and a file letter.fzz.
/// Action 1: run the launcher on opens: the manager window opens showing opens, an idle project.
/// Action 2: click the empty space below the tiles, and move the divider between tiles and panel as far left as it goes with the keyboard.
/// Action 3: press Become code: the manager remembers opens as a code project.
/// Action 4: press Compile: a question asks whether to allow the system extension fzz.
/// Action 5: press Allow: the Output says the compile is done, and the manager remembers fzz as an allowed extension.
/// Action 6: open the file manager on opens.
/// Action 7: double click letter.fzz: the Output says the main runs and ends with exit code 0.
/// Action 8: close the file manager window.
/// Action 9: choose System extensions in the Manager menu: a window lists fzz.
/// Action 10: select fzz and press Remove: the manager remembers no allowed extension.
/// Action 11: close the System extensions window and end the manager.
final class SystemExtensionTest extends ManagerTest{
  static{ Err.setUp(AssertionFailedError.class,Assertions::assertEquals,Assertions::assertTrue); }
  static final Path opens= data.resolveSibling("opens");
  static final Path console= data.resolve("eclipse").resolve("opens").resolve("console.txt");
  static final Path extensions= data.resolve("extensions.info");
  final At managerShown= new At("managerShown",windows(3000));
  final Click focusTiles= new Click("focusTiles",windows(200,400));
  final Click becomeCode= new Click("becomeCode",windows(332,94));
  final Click compile= new Click("compile",windows(102,67));
  final At questionShown= new At("questionShown",windows(3000));
  final Click allow= new Click("allow",windows(579,397));
  final At filesShown= new At("filesShown",windows(4000));
  final DoubleClick openLetter= new DoubleClick("openLetter",windows(480,283));
  final Click closeFiles= new Click("closeFiles",windows(1016,58));
  final Click managerMenu= new Click("managerMenu",windows(31,33));
  final Click systemExtensions= new Click("systemExtensions",windows(61,100));
  final Click pickExtension= new Click("pickExtension",windows(450,223));
  final Click removeExtension= new Click("removeExtension",windows(857,469));
  final Click closeExtensions= new Click("closeExtensions",windows(883,197));
  final Click managerMenuAgain= new Click("managerMenuAgain",windows(31,33));
  final Click quitManager= new Click("quitManager",windows(61,167));
  @Override protected void walk() throws Exception{
    clean();
    Fs.writeUtf8(opens.resolve("opens.fearless"),"\n");
    Fs.writeUtf8(opens.resolve("_hello").resolve("_rank_app.fear"),"use base.Main as Main;\nuse base.OpenWith as OpenWith;\n\nHello:Main, OpenWith[IconsHello,\"fzz\"]{s->base.Debug#(`hello`)}\n");
    Fs.ensureDir(opens.resolve("_hello").resolve("icons"));
    Fs.ofV(()->ImageIO.write(new BufferedImage(64,64,BufferedImage.TYPE_INT_RGB),"png",opens.resolve("_hello").resolve("icons").resolve("hello.png").toFile()));
    Fs.writeUtf8(opens.resolve("letter.fzz"),"dear reader\n");
    var manager= launch(opens.toString());
    managerShown.go();
    focusTiles.go();
    pilot.chord(KeyEvent.VK_F8);
    pilot.chord(KeyEvent.VK_HOME);
    becomeCode.go();
    until(()->Fs.readUtf8(info).contains("\"code\""));
    assertFalse(Files.exists(extensions));
    compile.go();
    until(()->Files.exists(opens.resolve(".fearless_out").resolve("mains.info")));
    questionShown.go();
    allow.go();
    until(()->Fs.readUtf8(console).contains("--- compile done ---"));
    assertEquals("[\"fzz\"]\n",Fs.readUtf8(extensions));
    assertFalse(Fs.readUtf8(console).contains("--- running "));
    forgetWindowPlaces();
    Desktop.getDesktop().open(opens.toFile());
    filesShown.go();
    openLetter.go();
    until(()->Fs.readUtf8(console).contains(" exited with 0 after "));
    closeFiles.go();
    managerMenu.go();
    systemExtensions.go();
    pickExtension.go();
    removeExtension.go();
    until(()->Fs.readUtf8(extensions).equals("[]\n"));
    Err.strCmp("""
      --- compiling opens ---
      --- compile done ---
      --- running hello.Hello ---
      hello
      --- hello.Hello exited with 0 after [###]s ---
      The system extension \"fzz\" of [###] of main \"hello.Hello\" is not allowed in this manager.
      The project claims no extension until it is compiled again.
      """,Fs.readUtf8(console));
    closeExtensions.go();
    managerMenuAgain.go();
    quitManager.go();
    until(()->!manager.isAlive());
  }
  @Override @AfterEach void clean() throws Exception{
    super.clean();
    Fs.rmTree(opens);
  }
}

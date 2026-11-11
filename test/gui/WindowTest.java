package gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tempDirs.RealTempDir;
import org.opentest4j.AssertionFailedError;

import controller.Facts;
import managerMessages.Report;
import managerMessages.Violation;
import tools.Fs;
import tools.JavacTool;
import userMessages.UserError;
import utils.Err;

/// What goes wrong while the window is made, and the messages the window raises by itself.
final class WindowTest{
  static{ Err.setUp(AssertionFailedError.class, Assertions::assertEquals, Assertions::assertTrue); }
  @Test void anOpenerEndingWithAnErrorSaysThatNoProgramOpensThePath(@RealTempDir Path dir){
    if (Fs.isWindows()){ fail("""
      Agent on Windows: explorer.exe ends with the same exit code whether or not a program opens the path, so the manager never says that no program of the system opens it.
      Investigate how to tell that no program opens a path (ShellExecuteEx giving SE_ERR_NOASSOC, the Open with dialog),
      then make Window.open say Report.noProgramOpens there, and make this test check it."""); }
    var path= dir.resolve("a.html");
    var said= new CompletableFuture<String>();
    Window.said(said::complete, path, Fs.of(new ProcessBuilder("sh", "-c", "exit 4")::start));
    assertEquals(Report.noProgramOpens(path), said.completeOnTimeout("nothing said", 10, TimeUnit.SECONDS).join());
  }
  @Test void aUserErrorWhileMakingTheWindowComesThroughUnchanged(){
    var icon= Violation.couldNotDecodeIcon(Path.of("app", "icon.png"));
    assertSame(icon, assertThrows(UserError.class, () -> Window.onEdt(() -> { throw icon; })));
  }
  @Test void anyOtherFailureWhileMakingTheWindowReportsItsOwnReason(){
    Err.strCmp("""
Fearless could not open its window.

Fearless needs to show a window, but opening the window failed.

Reported reason:
no screen""", assertThrows(UserError.class, () -> Window.onEdt(() -> { throw new IllegalStateException("no screen"); })).getMessage());
  }
  @Test void documentationThatIsNotThereIsSaidToTheManager(@RealTempDir Path dir){
    var said= new ArrayList<String>();
    var gen= dir.resolve(Facts.outDir);
    Window.open(said::add, gen.resolve("hello.html"));
    assertEquals(List.of("Nothing is opened: nothing exists at\n  "+gen.resolve("hello.html")), said);
  }
  private static Window window(Path dir){
    var app= dir.resolve("app");
    Fs.ensureDir(app);
    Fs.ofV(() -> ImageIO.write(new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB), "png", app.resolve("icon.png").toFile()));
    System.setProperty(JavacTool.appDirKey, app.toString());
    System.setProperty(JavacTool.launcherKey, JavacTool.consoleKey);
    try{ return Window.create(null, dir.resolve("manager")); }
    finally{ System.clearProperty(JavacTool.appDirKey); System.clearProperty(JavacTool.launcherKey); }
  }
  @Test void anErrorAboutTheFileOfTheEditorNamesTheTextOfTheEditor(@RealTempDir Path dir){
    var file= dir.resolve("projects1.info");
    var error= "In file: "+metaParser.PrettyFileName.displayFileName(file.toUri())+"\n\n001| {\n   | ^";
    assertEquals("In the text of the editor:\n\n001| {\n   | ^", Window.editorText(java.util.Set.of(file), error));
    assertEquals(error, Window.editorText(java.util.Set.of(dir.resolve("other.info")), error));
  }
  @Test void anEditMetadataOfAFileTheWindowDidNotWriteKeepsTheFile(@RealTempDir Path dir){
    var saved= dir.resolve("saved.info");
    Fs.writeUtf8(saved, "{\n  \"projects\": {},\n  \"systemExtensions\": []\n}\n");
    var window= window(dir);
    try{ window.edited(saved); }
    finally{ window.frame.dispose(); }
    assertTrue(Files.isRegularFile(saved));
  }
}

package gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opentest4j.AssertionFailedError;

import controller.Facts;
import managerMessages.Violation;
import tools.Fs;
import tools.JavacTool;
import userMessages.UserError;
import utils.Err;

/// What goes wrong while the window is made, and the messages the window raises by itself.
final class WindowTest{
  static{ Err.setUp(AssertionFailedError.class, Assertions::assertEquals, Assertions::assertTrue); }
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
  @Test void documentationThatIsNotThereIsSaidToTheManager(@TempDir Path dir){
    var said= new ArrayList<String>();
    var gen= dir.resolve(Facts.outDir).resolve("gen_java");
    Panel.openDocs(said::add, dir);
    Fs.ensureDir(gen);
    Panel.openDocs(said::add, dir);
    Window.open(said::add, gen.resolve("index.html"));
    assertEquals(List.of(
      "The documentation is not opened: no .html file is in\n  "+gen,
      "The documentation is not opened: no .html file is in\n  "+gen,
      "Nothing is opened: nothing exists at\n  "+gen.resolve("index.html")), said);
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
  @Test void anEditMetadataOfAFileTheWindowDidNotWriteKeepsTheFile(@TempDir Path dir){
    var saved= dir.resolve("saved.info");
    Fs.writeUtf8(saved, "{\n  \"projects\": {},\n  \"systemExtensions\": []\n}\n");
    var window= window(dir);
    try{ window.edited(saved); }
    finally{ window.frame.dispose(); }
    assertTrue(Files.isRegularFile(saved));
  }
}

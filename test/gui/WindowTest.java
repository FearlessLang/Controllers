package gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opentest4j.AssertionFailedError;

import controller.Facts;
import controller.Messages;
import controller.Project;
import coordinator.MainsInfo;
import tools.Fs;
import userMessages.UserError;
import utils.Err;

/// What goes wrong while the window is made, and the messages the window raises by itself, which go to the manager.
final class WindowTest{
  static{ Err.setUp(AssertionFailedError.class,Assertions::assertEquals,Assertions::assertTrue); }
  @Test void aUserErrorWhileMakingTheWindowComesThroughUnchanged(){
    var icon= Messages.couldNotDecodeIcon(Path.of("app","icon.png"));
    assertSame(icon,assertThrows(UserError.class,()->Window.onEdt(()->{ throw icon; })));
  }
  @Test void anyOtherFailureWhileMakingTheWindowReportsItsOwnReason(){
    Err.strCmp("""
      Fearless could not open its window.

      Fearless needs to show a window, but opening the window failed.

      Reported reason:
      no screen""",assertThrows(UserError.class,()->Window.onEdt(()->{ throw new IllegalStateException("no screen"); })).getMessage());
  }
  @Test void documentationThatIsNotThereIsSaidToTheManager(@TempDir Path dir){
    var said= new ArrayList<String>();
    var gen= dir.resolve(Facts.outDir).resolve("gen_java");
    Panel.openDocs(said::add,dir);
    Fs.ensureDir(gen);
    Panel.openDocs(said::add,dir);
    Window.open(said::add,gen.resolve("index.html"));
    assertEquals(List.of(
      "The documentation is not opened: no such file: "+gen,
      "The documentation is not opened: no .html file is in\n"+gen,
      "Nothing is opened: nothing exists at\n"+gen.resolve("index.html")),said);
  }
  @Test void anAllowedExtensionIsShownWithTheMainsClaimingIt(@TempDir Path dir){
    var claim= new MainsInfo.Claim("a.IconsFoo","icons/foo.png","","","htm");
    var claimed= Map.of("htm",List.of(new Project.Claimant(dir.resolve("a"),"a","a.Main",false,claim),new Project.Claimant(dir.resolve("b"),"b","b.Main",false,claim)));
    assertEquals(".htm   a::a.Main, b::b.Main",Window.extensionRow("htm",claimed));
    assertEquals(".pdf   <claimed by no main>",Window.extensionRow("pdf",claimed));
  }
}

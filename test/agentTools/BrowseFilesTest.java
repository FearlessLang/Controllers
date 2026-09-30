package agentTools;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.awt.Desktop;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;

import tools.Fs;

/// Browse files in the Project menu opens the file manager on the folder of the shown project, looking exactly as the file manager opened on that folder from the desk; with the folder gone it opens nothing and a note says nothing exists there.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, nothing is registered for .fearless, the folder browsed beside the manager holds only its marker and one source file, and nothing is at browsed2 beside it.
/// Action 1: run the launcher on browsed: the manager window opens showing browsed.
/// Action 2: choose Browse files in its Project menu: the file manager window opens over the manager window.
/// Action 3: close the file manager window: the manager window is exactly as before action 2.
/// Action 4: open the file manager on browsed: its window shows exactly as after action 2.
/// Action 5: close the file manager window: the manager window is exactly as before action 2 again.
/// Action 6: rename the folder browsed to browsed2: the tile of browsed changes by itself.
/// Action 7: choose Browse files in its Project menu again: a note shows saying nothing exists at browsed, and the manager notes file holds exactly that text.
/// Action 8: press OK: the window is exactly as after action 6, with no file manager window, and nothing was made at browsed.
/// Action 9: end the manager.
final class BrowseFilesTest extends ManagerTest{
  static final Path browsed= data.resolveSibling("browsed");
  static final Path moved= data.resolveSibling("browsed2");
  final At managerShown= new At("managerShown",linux(3000));
  final Area window= new Area("window",linux(68,32,3772,2098));
  final Click projectMenu= new Click("projectMenu",linux(158,79));
  final Click browseFiles= new Click("browseFiles",linux(180,149));
  final At filesShown= new At("filesShown",linux(1000));
  final Click closeFiles= new Click("closeFiles",linux(2374,843));
  final Click closeFilesAgain= new Click("closeFilesAgain",linux(2374,843));
  final Area tile= new Area("tile",linux(74,149,128,88));
  final Click projectMenuAgain= new Click("projectMenuAgain",linux(158,79));
  final Click browseFilesAgain= new Click("browseFilesAgain",linux(180,149));
  final Click ok= new Click("ok",linux(1952,1136));
  @Override protected void walk() throws Exception{
    clean();
    Fs.writeUtf8(browsed.resolve("browsed.fearless"),"\n");
    Fs.writeUtf8(browsed.resolve("_browsed").resolve("_rank_app.fear"),"use base.Main as Main;\n\nHello:Main{s->base.Debug#(\"browsed\")}\n");
    launch(browsed.toString());
    managerShown.go();
    look();
    var at= window.aim();
    var before= pixels(at);
    projectMenu.go();
    browseFiles.go();
    look();
    until(()->!Arrays.equals(before,pixels(at)));
    filesShown.go();
    var files= pixels(at);
    closeFiles.go();
    look();
    until(()->Arrays.equals(before,pixels(at)));
    Desktop.getDesktop().open(browsed.toFile());
    until(()->Arrays.equals(files,pixels(at)));
    closeFilesAgain.go();
    look();
    until(()->Arrays.equals(before,pixels(at)));
    var cell= tile.aim();
    var first= pixels(cell);
    Files.move(browsed,moved);
    until(()->!Arrays.equals(first,pixels(cell)));
    var gone= pixels(at);
    projectMenuAgain.go();
    browseFilesAgain.go();
    until(()->Files.exists(notes) && Fs.readUtf8(notes).equals("Nothing is opened: nothing exists at\n"+browsed+"\n"));
    look();
    until(()->!Arrays.equals(gone,pixels(at)));
    ok.go();
    look();
    until(()->Arrays.equals(gone,pixels(at)));
    assertFalse(Files.exists(browsed));
    stopManagers();
  }
  @Override @AfterEach void clean() throws Exception{
    super.clean();
    Fs.rmTree(browsed);
    Fs.rmTree(moved);
  }
}

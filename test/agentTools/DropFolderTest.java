package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Desktop;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;

import tools.Fs;

/// A folder carried out of the file manager and dropped onto the manager window is registered exactly as running the launcher on that folder does, and it stays where it was.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, nothing is registered for .fearless, the file manager remembers no place for its windows, and the folder drops beside the manager holds only the folder dropped, which holds its marker and one source file.
/// Action 1: run the launcher: the manager window opens with no tile.
/// Action 2: open the file manager on drops: its window shows dropped, over the manager window.
/// Action 3: carry dropped out of the file manager window and let it go over the tiles of the manager window: the manager window comes in front showing dropped, the manager remembers dropped as an idle project, and drops still holds dropped with exactly its files.
/// Action 4: bring the desk back to the Setup state: the manager ends and the file manager window shows again.
/// Action 5: close the file manager window: the desk shows exactly what it showed before action 1.
/// Action 6: run the launcher on dropped: the manager window opens with exactly the tiles it had after action 3, and the manager remembers exactly what it remembered then.
/// Action 7: end the manager.
final class DropFolderTest extends ManagerTest{
  static final Path drops= data.resolveSibling("drops");
  static final Path dropped= drops.resolve("dropped");
  final At managerShown= new At("managerShown",linux(3000),windows(3000));
  final Area window= new Area("window",linux(68,32,3772,2098),windows(0,24,1280,624));
  final At filesShown= new At("filesShown",linux(2000),windows(4000));
  final Drag carryDropped= new Drag("carryDropped",linux(1759,915,200,1500),windows(475,226,130,300));
  final At tileShown= new At("tileShown",linux(1000),windows(1000));
  final Area tiles= new Area("tiles",linux(68,68,310,180),windows(0,23,310,180));
  final Click closeFiles= new Click("closeFiles",linux(2374,843),windows(1016,58));
  @Override protected void walk() throws Exception{
    Assumptions.assumeFalse(elevated(),"A file manager of a lower integrity level can not drop onto an elevated window, and the manager this test starts is as elevated as the test.");
    clean();
    forgetWindowPlaces();
    Fs.writeUtf8(dropped.resolve("dropped.fearless"),"\n");
    Fs.writeUtf8(dropped.resolve("_dropped").resolve("_rank_app.fear"),"use base.Main as Main;\n\nHello:Main{s->base.Debug#(\"dropped\")}\n");
    look();
    var at= window.aim();
    var desk= pixels(at);
    launch();
    managerShown.go();
    look();
    var before= pixels(at);
    Desktop.getDesktop().open(drops.toFile());
    until(()->!Arrays.equals(before,pixels(at)));
    filesShown.go();
    carryDropped.go();
    var remembered= """
      {
        "dropped": {
          "path": "Str:%s",
          "kind": "idle"
        }
      }
      """.formatted(slashed(dropped));
    until(()->Files.exists(info) && Fs.readUtf8(info).equals(remembered));
    assertEquals(List.of("dropped/_dropped/_rank_app.fear","dropped/dropped.fearless"),Fs.walk(drops,s->s.filter(Files::isRegularFile).map(p->slashed(drops.relativize(p))).sorted().toList()));
    tileShown.go();
    look();
    var place= tiles.aim();
    var shown= pixels(place);
    super.clean();
    closeFiles.go();
    look();
    until(()->Arrays.equals(desk,pixels(at)));
    launch(dropped.toString());
    look();
    until(()->Arrays.equals(shown,pixels(place)));
    assertEquals(remembered,Fs.readUtf8(info));
    stopManagers();
  }
  @Override @AfterEach void clean() throws Exception{
    super.clean();
    Fs.rmTree(drops);
  }
}

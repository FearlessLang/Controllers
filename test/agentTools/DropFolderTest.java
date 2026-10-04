package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Desktop;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;

import tools.Fs;

/// A folder carried out of the file manager and dropped onto the manager window is registered exactly as running the launcher on that folder does, and it stays where it was.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, nothing is registered for .fearless, and the folder drops beside the manager holds only the folder dropped, which holds its marker and one source file.
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
  final At managerShown= new At("managerShown",on("ubuntu-gnome",3000),on("debian-gnome-x11",2500),on("xubuntu-xfce",2500),on("debian-cinnamon",2500),on("lubuntu-lxqt",2500));
  final Area window= new Area("window",on("ubuntu-gnome",68,32,3772,2098),on("debian-gnome-x11",0,32,1920,1022),on("xubuntu-xfce",0,27,1920,1022),on("debian-cinnamon",0,0,1920,1012),on("lubuntu-lxqt",0,0,1920,1018));
  final At filesShown= new At("filesShown",on("ubuntu-gnome",2000),on("debian-gnome-x11",2000),on("xubuntu-xfce",2000),on("debian-cinnamon",2000),on("lubuntu-lxqt",2000));
  final Drag carryDropped= new Drag("carryDropped",on("ubuntu-gnome",1759,915,200,1500),on("debian-gnome-x11",794,410,200,800),on("xubuntu-xfce",878,428,200,800),on("debian-cinnamon",274,190,200,768),on("lubuntu-lxqt",271,130,200,761));
  final At tileShown= new At("tileShown",on("ubuntu-gnome",1000),on("debian-gnome-x11",1500),on("xubuntu-xfce",1500),on("debian-cinnamon",1500),on("lubuntu-lxqt",1500));
  final Area tiles= new Area("tiles",on("ubuntu-gnome",68,68,310,180),on("debian-gnome-x11",2,69,310,180),on("xubuntu-xfce",2,120,310,180),on("debian-cinnamon",2,37,310,180),on("lubuntu-lxqt",2,30,310,180));
  final Click closeFiles= new Click("closeFiles",on("ubuntu-gnome",2374,843),on("debian-gnome-x11",1380,304),on("xubuntu-xfce",1269,311),on("debian-cinnamon",818,54),on("lubuntu-lxqt",628,16));
  @Override protected void walk() throws Exception{
    clean();
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
      """.formatted(dropped);
    until(()->Files.exists(info) && Fs.readUtf8(info).equals(remembered));
    assertEquals(List.of("dropped/_dropped/_rank_app.fear","dropped/dropped.fearless"),Fs.walk(drops,s->s.filter(Files::isRegularFile).map(p->drops.relativize(p).toString()).sorted().toList()));
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

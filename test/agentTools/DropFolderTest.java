package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.util.List;

import tools.Fs;

final class DropFolderTest extends ManagerTest{
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("xubuntu_xfce", () -> waitAfter(val(21500))));
  final Action openDrops= action("openDrops",
    on("ubuntu_gnome", () -> waitDoubleClick(val(100), val(798), val(380))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action filesShown= action("filesShown",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("xubuntu_xfce", () -> waitAfter(val(19500))));
  final Action carryDropped= action("carryDropped",
    on("ubuntu_gnome", () -> waitDrag(val(0), val(798), val(380), val(300), val(600))),
    on("xubuntu_xfce", () -> waitDrag(val(0), val(879), val(425), val(200), val(700))));
  final Action droppedSaved= action("droppedSaved",
    on("ubuntu_gnome", () -> waitAfter(val(300))),
    on("xubuntu_xfce", () -> waitAfter(val(12500))));
  final Action filesClosed= action("filesClosed",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("xubuntu_xfce", () -> waitAfter(val(5500))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("xubuntu_xfce", () -> waitAfter(val(9000))));
  @Override void walk() throws Throwable{
    var drops= files.resolve("drops");
    var dropped= drops.resolve("dropped");
    Fs.writeUtf8(dropped.resolve("dropped.fearless"), "\n");
    Fs.writeUtf8(dropped.resolve("_dropped").resolve("_rank_app.fear"), "use base.Main as Main;\n\nHello:Main{s->base.Debug#(\"dropped\")}\n");
    var bare= look();
    start();
    managerShown.go();
    toFiles();
    openDrops.go();
    filesShown.go();
    carryDropped.go();
    droppedSaved.go();
    stabilize();
    checkContent(info, """
      {
        "projects": {
          "dropped": {
            "path": "Str:%s",
            "kind": "idle"
          }
        },
        "systemExtensions": []
      }
      """.formatted(slashed(dropped)));
    assertEquals(List.of("dropped/_dropped/_rank_app.fear", "dropped/dropped.fearless"), Fs.walk(drops, s -> s.filter(Files::isRegularFile).map(p -> slashed(drops.relativize(p))).sorted().toList()));
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    closeFiles();
    filesClosed.go();
    same(bare, inWindow(100, 200, 200, 400));
  }
}

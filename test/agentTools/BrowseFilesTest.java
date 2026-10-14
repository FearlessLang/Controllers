package agentTools;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.awt.Rectangle;
import java.nio.file.Files;

import tools.Fs;

final class BrowseFilesTest extends ManagerTest{
  final Action openBrowsed= action("openBrowsed",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action openBrowsedMarker= action("openBrowsedMarker",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action projectShown= action("projectShown",
    on("ubuntu_gnome", () -> waitAfter(val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(null))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome", () -> waitAfter(val(17500))),
    on("xubuntu_xfce", () -> waitAfter(val(21500))));
  final Action browse= action("browse",
    on("ubuntu_gnome", () -> waitClick(val(0), val(1097), val(113))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(1118), val(93))));
  final Action filesShown= action("filesShown",
    on("ubuntu_gnome", () -> waitAfter(val(6500))),
    on("xubuntu_xfce", () -> waitAfter(val(8500))));
  final Action closeFiles= action("closeFiles",
    on("ubuntu_gnome", () -> waitClick(val(0), val(1414), val(303))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(1275), val(309))));
  final Action filesClosed= action("filesClosed",
    on("ubuntu_gnome", () -> waitAfter(val(3500))),
    on("xubuntu_xfce", () -> waitAfter(val(4000))));
  final Action tileChanged= action("tileChanged",
    on("ubuntu_gnome", () -> waitAfter(val(6500))),
    on("xubuntu_xfce", () -> waitAfter(val(8500))));
  final Action browseAgain= action("browseAgain",
    on("ubuntu_gnome", () -> waitClick(val(0), val(1097), val(113))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(1118), val(93))));
  final Action noteShown= action("noteShown",
    on("ubuntu_gnome", () -> waitAfter(val(3500))),
    on("xubuntu_xfce", () -> waitAfter(val(4000))));
  final Action okButton= action("okButton",
    on("ubuntu_gnome", () -> waitClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitClick(val(null), val(null), val(null))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome", () -> waitAfter(val(7000))),
    on("xubuntu_xfce", () -> waitAfter(val(9000))));
  @Override void walk() throws Throwable{
    var browsed= files.resolve("browsed");
    var moved= files.resolve("browsed2");
    Fs.writeUtf8(browsed.resolve("browsed.fearless"), "\n");
    Fs.writeUtf8(browsed.resolve("_browsed").resolve("_rank_app.fear"), "use base.Main as Main;\n\nHello:Main{s->base.Debug#(\"browsed\")}\n");
    var bare= look();
    start();
    managerShown.go();
    toFiles();
    openBrowsed.go();
    var folder= look();
    openBrowsedMarker.go();
    closeFiles();
    projectShown.go();
    var center= new Rectangle(760, 460, 400, 160);
    var before= look();
    browse.go();
    filesShown.go();
    same(folder, center);
    closeFiles.go();
    filesClosed.go();
    same(before, center);
    Files.move(browsed, moved);
    tileChanged.go();
    browseAgain.go();
    noteShown.go();
    changed(before, center);
    stabilize();
    assertFalse(Files.exists(browsed));
    okButton.go();
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    same(bare, center);
  }
}

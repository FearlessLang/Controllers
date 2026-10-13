package agentTools;

import java.nio.file.Files;

import tools.Fs;

final class EditMetadataRenameTest extends ManagerTest{
  final Action openOld= action("openOld",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action openOldMarker= action("openOldMarker",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action projectShown= action("projectShown",
    on("ubuntu_gnome", () -> waitAfter(val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(null))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome", () -> waitAfter(val(17500))),
    on("xubuntu_xfce", () -> waitAfter(val(21500))));
  final Action editorShown= action("editorShown",
    on("ubuntu_gnome", () -> waitAfter(val(5500))),
    on("xubuntu_xfce", () -> waitAfter(val(7000))));
  final Action nameOld= action("nameOld",
    on("ubuntu_gnome", () -> doubleClick(val(646), val(342))),
    on("xubuntu_xfce", () -> doubleClick(val(613), val(339))));
  final Action commit= action("commit",
    on("ubuntu_gnome", () -> click(val(1278), val(830))),
    on("xubuntu_xfce", () -> click(val(1245), val(827))));
  final Action renamed= action("renamed",
    on("ubuntu_gnome", () -> waitAfter(val(10000))),
    on("xubuntu_xfce", () -> waitAfter(val(12500))));
  final Action markerSeen= action("markerSeen",
    on("ubuntu_gnome", () -> waitAfter(val(10000))),
    on("xubuntu_xfce", () -> waitAfter(val(12500))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome", () -> waitAfter(val(7000))),
    on("xubuntu_xfce", () -> waitAfter(val(9000))));
  @Override void walk() throws Throwable{
    var old= files.resolve("old");
    Fs.writeUtf8(old.resolve("old.fearless"), "\n");
    Fs.writeUtf8(old.resolve("_old").resolve("_rank_app.fear"), "use base.Main as Main;\n\nHello:Main{s->base.Debug#(\"old\")}\n");
    start();
    managerShown.go();
    toFiles();
    openOld.go();
    openOldMarker.go();
    closeFiles();
    projectShown.go();
    var tile= inWindow(69, 95, 128, 85);
    var valid= look();
    managerMenu.go();
    editMetadata.go();
    editorShown.go();
    nameOld.go();
    type("fresh");
    commit.go();
    renamed.go();
    changed(valid, tile);
    stabilize();
    checkContent(info, """
      {
        "projects": {
          "fresh": {
            "path": "Str:%s",
            "kind": "idle"
          }
        },
        "systemExtensions": []
      }
      """.formatted(slashed(old)));
    var invalid= look();
    Files.move(old.resolve("old.fearless"), old.resolve("fresh.fearless"));
    stabilize();
    markerSeen.go();
    changed(invalid, tile);
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
  }
}

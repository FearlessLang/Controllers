package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.util.List;

import tools.Fs;

final class ChooserNavigationTest extends ManagerTest{
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome", () -> waitAfter(val(17500))),
    on("xubuntu_xfce", () -> waitAfter(val(21500))));
  final Action chooserShown= action("chooserShown",
    on("ubuntu_gnome", () -> waitAfter(val(5000))),
    on("xubuntu_xfce", () -> waitAfter(val(6500))));
  final Action upLevel= action("upLevel",
    on("ubuntu_gnome", () -> waitClick(val(0), val(1109), val(436))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(1085), val(427))));
  final Action homeLeft= action("homeLeft",
    on("ubuntu_gnome", () -> waitAfter(val(3000))),
    on("xubuntu_xfce", () -> waitAfter(val(3500))));
  final Action rootShown= action("rootShown",
    on("ubuntu_gnome", () -> waitAfter(val(3000))),
    on("xubuntu_xfce", () -> waitAfter(val(4000))));
  final Action openDataInChooser= action("openDataInChooser",
    on("ubuntu_gnome", () -> waitDoubleClick(val(0), val(790), val(539))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(0), val(745), val(505))));
  final Action dataShown= action("dataShown",
    on("ubuntu_gnome", () -> waitAfter(val(3000))),
    on("xubuntu_xfce", () -> waitAfter(val(3500))));
  final Action openFilesIO= action("openFilesIO",
    on("ubuntu_gnome", () -> waitDoubleClick(val(0), val(940), val(473))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(0), val(745), val(485))));
  final Action filesIOShown= action("filesIOShown",
    on("ubuntu_gnome", () -> waitAfter(val(3000))),
    on("xubuntu_xfce", () -> waitAfter(val(4000))));
  final Action openFilesInChooser= action("openFilesInChooser",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action openAttic= action("openAttic",
    on("ubuntu_gnome", () -> waitDoubleClick(val(0), val(790), val(473))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(0), val(745), val(465))));
  final Action atticShown= action("atticShown",
    on("ubuntu_gnome", () -> waitAfter(val(3000))),
    on("xubuntu_xfce", () -> waitAfter(val(3500))));
  final Action pickInner= action("pickInner",
    on("ubuntu_gnome", () -> waitClick(val(0), val(790), val(473))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(745), val(465))));
  final Action open= action("open",
    on("ubuntu_gnome", () -> waitClick(val(0), val(1116), val(712))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(1080), val(705))));
  final Action tileShown= action("tileShown",
    on("ubuntu_gnome", () -> waitAfter(val(7000))),
    on("xubuntu_xfce", () -> waitAfter(val(9000))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome", () -> waitAfter(val(7000))),
    on("xubuntu_xfce", () -> waitAfter(val(8500))));
  @Override void walk() throws Throwable{
    var attic= files.resolve("attic");
    var inner= attic.resolve("inner");
    Fs.writeUtf8(inner.resolve("inner.fearless"), "\n");
    Fs.writeUtf8(inner.resolve("_inner").resolve("_rank_app.fear"), "use base.Main as Main;\n\nHello:Main{s->base.Debug#(\"inner\")}\n");
    var atticFiles= Fs.walk(attic, s -> s.sorted().toList());
    var bare= look();
    start();
    managerShown.go();
    managerMenu.go();
    addFolder.go();
    chooserShown.go();
    upLevel.go();
    homeLeft.go();
    upLevel.go();
    rootShown.go();
    openDataInChooser.go();
    dataShown.go();
    openFilesIO.go();
    filesIOShown.go();
    openFilesInChooser.go();
    openAttic.go();
    atticShown.go();
    stabilize();
    checkContent(info, "{\n  \"projects\": {},\n  \"systemExtensions\": []\n}\n");
    assertEquals(atticFiles, Fs.walk(attic, s -> s.sorted().toList()));
    pickInner.go();
    open.go();
    tileShown.go();
    stabilize();
    checkContent(info, """
      {
        "projects": {
          "inner": {
            "path": "Str:%s",
            "kind": "idle"
          }
        },
        "systemExtensions": []
      }
      """.formatted(slashed(inner)));
    assertEquals(List.of(), Fs.walk(attic, s -> s.filter(p -> p.getParent().equals(attic) && Files.isRegularFile(p)).toList()));
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    closeFiles();
    same(bare, inWindow(100, 200, 200, 400));
  }
}

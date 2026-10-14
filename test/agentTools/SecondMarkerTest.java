package agentTools;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;

import tools.Fs;

final class SecondMarkerTest extends ManagerTest{
  final Action openTwice= action("openTwice",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action openTwiceMarker= action("openTwiceMarker",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action projectShown= action("projectShown",
    on("ubuntu_gnome", () -> waitAfter(val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(null))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome", () -> waitAfter(val(17500))),
    on("xubuntu_xfce", () -> waitAfter(val(21500))));
  final Action secondNoticed= action("secondNoticed",
    on("ubuntu_gnome", () -> waitAfter(val(10000))),
    on("xubuntu_xfce", () -> waitAfter(val(12500))));
  final Action check= action("check",
    on("ubuntu_gnome", () -> waitClick(val(0), val(527), val(113))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(478), val(93))));
  final Action checked= action("checked",
    on("ubuntu_gnome", () -> waitAfter(val(4500))),
    on("xubuntu_xfce", () -> waitAfter(val(6000))));
  final Action secondGone= action("secondGone",
    on("ubuntu_gnome", () -> waitAfter(val(10000))),
    on("xubuntu_xfce", () -> waitAfter(val(12500))));
  final Action checkAgain= action("checkAgain",
    on("ubuntu_gnome", () -> waitClick(val(0), val(527), val(113))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(478), val(93))));
  final Action checkedAgain= action("checkedAgain",
    on("ubuntu_gnome", () -> waitAfter(val(3500))),
    on("xubuntu_xfce", () -> waitAfter(val(4000))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome", () -> waitAfter(val(7000))),
    on("xubuntu_xfce", () -> waitAfter(val(9000))));
  @Override void walk() throws Throwable{
    var twice= files.resolve("twice");
    var second= twice.resolve("other.fearless");
    Fs.writeUtf8(twice.resolve("twice.fearless"), "\n");
    Fs.writeUtf8(twice.resolve("_twice").resolve("_rank_app.fear"), "use base.Main as Main;\n\nHello:Main{s->base.Debug#(\"twice\")}\n");
    var bare= look();
    start();
    managerShown.go();
    toFiles();
    openTwice.go();
    openTwiceMarker.go();
    closeFiles();
    projectShown.go();
    var tile= inWindow(69, 95, 128, 85);
    var output= inWindow(410, 168, 1000, 80);
    var valid= look();
    Fs.writeUtf8(second, "\n");
    stabilize();
    secondNoticed.go();
    changed(valid, tile);
    check.go();
    checked.go();
    changed(valid, output);
    stabilize();
    var error= files.resolve("twice/.fearless_out/check_error.txt");
    checkContent(error, "More than one .fearless marker file was found in[###]keep only \"twice.fearless\".");
    var invalid= look();
    Files.delete(second);
    stabilize();
    secondGone.go();
    same(valid, tile);
    checkAgain.go();
    checkedAgain.go();
    changed(invalid, output);
    stabilize();
    assertFalse(Files.exists(error));
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    same(bare, tile);
  }
}

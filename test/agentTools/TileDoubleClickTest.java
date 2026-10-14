package agentTools;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;

import tools.Fs;

final class TileDoubleClickTest extends ManagerTest{
  final Action becomeCode= action("becomeCode",
    on("ubuntu_gnome", () -> waitClick(val(100), val(989), val(112))),
    on("xubuntu_xfce", () -> waitClick(val(null), val(998), val(93))));
  final Action openTapped= action("openTapped",
    on("ubuntu_gnome", () -> waitDoubleClick(val(100), val(798), val(380))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  //double clicks the marker, the file ending with .fearless; not the folder starting with _
  final Action openTappedMarker= action("openTappedMarker",
    on("ubuntu_gnome", () -> waitDoubleClick(val(100), val(911), val(380))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action projectShown= action("projectShown",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("xubuntu_xfce", () -> waitAfter(val(null))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("xubuntu_xfce", () -> waitAfter(val(21500))));
  final Action codeShown= action("codeShown",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("xubuntu_xfce", () -> waitAfter(val(6000))));
  final Action tileTwice= action("tileTwice",
    on("ubuntu_gnome", () -> waitDoubleClick(val(0), val(132), val(128))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(0), val(66), val(110))));
  final Action notRun= action("notRun",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("xubuntu_xfce", () -> waitAfter(val(6500))));
  final Action compile= action("compile",
    on("ubuntu_gnome", () -> waitClick(val(100), val(557), val(112))),
    on("xubuntu_xfce", () -> waitClick(val(null), val(503), val(93))));
  final Action compiled= action("compiled",
    on("ubuntu_gnome", () -> waitAfter(val(2000))),//the compile of tapped runs in its own JVM
    on("xubuntu_xfce", () -> waitAfter(val(26500))));
  final Action tileTwiceCompiled= action("tileTwiceCompiled",
    on("ubuntu_gnome", () -> waitDoubleClick(val(0), val(132), val(128))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(0), val(66), val(110))));
  final Action ran= action("ran",
    on("ubuntu_gnome", () -> waitAfter(val(1500))),//the main of tapped runs in its own JVM
    on("xubuntu_xfce", () -> waitAfter(val(10500))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("xubuntu_xfce", () -> waitAfter(val(9000))));
  @Override void walk() throws Throwable{
    var tapped= files.resolve("tapped");
    Fs.writeUtf8(tapped.resolve("tapped.fearless"), "\n");
    Fs.writeUtf8(tapped.resolve("_tapped").resolve("_rank_app.fear"), "use base.Main as Main;\n\nGo: Main{sys -> sys.out.println \"went\"}\n");
    var bare= look();
    start();
    managerShown.go();
    toFiles();
    openTapped.go();
    openTappedMarker.go();
    projectShown.go();
    becomeCode.go();
    codeShown.go();
    var output= inWindow(410, 168, 1000, 80);
    var button= inWindow(527, 102, 30, 20);
    var before= look();
    tileTwice.go();
    notRun.go();
    same(before, output);
    same(before, button);
    stabilize();
    assertFalse(Files.exists(tapped.resolve(".fearless_out").resolve("mains.info")));
    compile.go();
    compiled.go();
    var compiledLook= look();
    tileTwiceCompiled.go();
    ran.go();
    changed(compiledLook, output);
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    closeFiles();
    same(bare, output);
  }
}

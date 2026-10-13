package agentTools;

import tools.Fs;

final class OutputClearTest extends ManagerTest{
  final Action becomeCode= action("becomeCode",
    on("ubuntu_gnome", () -> waitClick(val(100), val(962), val(112))),
    on("xubuntu_xfce", () -> waitClick(val(null), val(968), val(93))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("xubuntu_xfce", () -> waitAfter(val(21500))));
  final Action codeShown= action("codeShown",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("xubuntu_xfce", () -> waitAfter(val(6000))));
  final Action compile= action("compile",
    on("ubuntu_gnome", () -> waitClick(val(100), val(530), val(112))),
    on("xubuntu_xfce", () -> waitClick(val(null), val(473), val(93))));
  final Action compiled= action("compiled",
    on("ubuntu_gnome", () -> waitAfter(val(2000))),//the compile of said runs in its own JVM
    on("xubuntu_xfce", () -> waitAfter(val(26000))));
  final Action clear= action("clear",
    on("ubuntu_gnome", () -> waitClick(val(100), val(1879), val(150))),
    on("xubuntu_xfce", () -> waitClick(val(null), val(1877), val(130))));
  final Action cleared= action("cleared",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("xubuntu_xfce", () -> waitAfter(val(4000))));
  final Action run= action("run",
    on("ubuntu_gnome", () -> waitClick(val(100), val(517), val(112))),
    on("xubuntu_xfce", () -> waitClick(val(null), val(458), val(93))));
  final Action ran= action("ran",
    on("ubuntu_gnome", () -> waitAfter(val(1500))),//the main of said runs in its own JVM
    on("xubuntu_xfce", () -> waitAfter(val(9000))));
  final Action clearAgain= action("clearAgain",
    on("ubuntu_gnome", () -> waitClick(val(100), val(1879), val(150))),
    on("xubuntu_xfce", () -> waitClick(val(null), val(1877), val(130))));
  final Action clearedAgain= action("clearedAgain",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("xubuntu_xfce", () -> waitAfter(val(4000))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("xubuntu_xfce", () -> waitAfter(val(8500))));
  final Action openSaid= action("openSaid",
    on("ubuntu_gnome", () -> waitDoubleClick(val(100), val(798), val(377))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action openSaidMarker= action("openSaidMarker",
    on("ubuntu_gnome", () -> waitDoubleClick(val(100), val(911), val(377))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action projectShown= action("projectShown",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("xubuntu_xfce", () -> waitAfter(val(null))));
  @Override void walk() throws Throwable{
    var said= files.resolve("said");
    Fs.writeUtf8(said.resolve("said.fearless"), "\n");
    Fs.writeUtf8(said.resolve("_said").resolve("_rank_app.fear"), "use base.Main as Main;\n\nSay: Main{sys -> sys.out.println \"said once\"}\n");
    start();
    managerShown.go();
    toFiles();
    openSaid.go();
    openSaidMarker.go();
    projectShown.go();
    becomeCode.go();
    codeShown.go();
    var output= inWindow(410, 170, 1000, 90);
    var empty= look();
    compile.go();
    compiled.go();
    changed(empty, output);
    clear.go();
    cleared.go();
    same(empty, output);
    run.go();
    ran.go();
    changed(empty, output);
    clearAgain.go();
    clearedAgain.go();
    same(empty, output);
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    closeFiles();
  }
}

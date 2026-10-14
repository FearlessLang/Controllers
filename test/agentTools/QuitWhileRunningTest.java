package agentTools;


final class QuitWhileRunningTest extends ManagerTest{
  final Action programArea= action("programArea",
    on("ubuntu_gnome", () -> region(val(930), val(500), val(120), val(120))),
    on("xubuntu_xfce", () -> region(val(930), val(500), val(120), val(120))));
  final Action becomeCode= action("becomeCode",
    on("ubuntu_gnome", () -> waitClick(val(0), val(970), val(112))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(976), val(93))));
  final Action openTestGui1= action("openTestGui1",
    on("ubuntu_gnome", () -> waitDoubleClick(val(100), val(798), val(380))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action openStartMarker= action("openStartMarker",
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
  final Action compile= action("compile",
    on("ubuntu_gnome", () -> waitClick(val(0), val(538), val(112))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(481), val(93))));
  final Action compiled= action("compiled",
    on("ubuntu_gnome", () -> waitAfter(val(2000))),//the compile of testGui1 runs in its own JVM
    on("xubuntu_xfce", () -> waitAfter(val(26000))));
  final Action run= action("run",
    on("ubuntu_gnome", () -> waitClick(val(0), val(525), val(112))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(466), val(93))));
  final Action programShown= action("programShown",
    on("ubuntu_gnome", () -> waitAfter(val(1500))),//the main of testGui1 starts in its own JVM and opens its window
    on("xubuntu_xfce", () -> waitAfter(val(10500))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("xubuntu_xfce", () -> waitAfter(val(9000))));
  @Override void walk() throws Throwable{
    project("testGui1");
    var bare= look();
    start();
    managerShown.go();
    toFiles();
    openTestGui1.go();
    openStartMarker.go();
    projectShown.go();
    becomeCode.go();
    codeShown.go();
    compile.go();
    compiled.go();
    programArea.go();
    var program= region;
    var before= look();
    run.go();
    programShown.go();
    changed(before, program);
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    closeFiles();
    changed(before, inWindow(100, 200, 200, 400));
    same(bare, program);
  }
}

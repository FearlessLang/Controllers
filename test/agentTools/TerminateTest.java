package agentTools;

import java.awt.Rectangle;

final class TerminateTest extends ManagerTest{
  final Action becomeCode= action("becomeCode",
    on("ubuntu_gnome", () -> waitClick(val(100), val(970), val(112))),
    on("xubuntu_xfce", () -> waitClick(val(null), val(976), val(93))));
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
    on("ubuntu_gnome", () -> waitClick(val(100), val(538), val(112))),
    on("xubuntu_xfce", () -> waitClick(val(null), val(481), val(93))));
  final Action compiled= action("compiled",
    on("ubuntu_gnome", () -> waitAfter(val(2000))),
    on("xubuntu_xfce", () -> waitAfter(val(26000))));
  final Action run= action("run",
    on("ubuntu_gnome", () -> waitClick(val(100), val(525), val(112))),
    on("xubuntu_xfce", () -> waitClick(val(null), val(466), val(93))));
  final Action programShown= action("programShown",
    on("ubuntu_gnome", () -> waitAfter(val(1500))),
    on("xubuntu_xfce", () -> waitAfter(val(10500))));
  final Action terminate= action("terminate",
    on("ubuntu_gnome", () -> waitClick(val(100), val(545), val(112))),
    on("xubuntu_xfce", () -> waitClick(val(null), val(489), val(93))));
  final Action terminated= action("terminated",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("xubuntu_xfce", () -> waitAfter(val(6000))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("xubuntu_xfce", () -> waitAfter(val(8500))));
  @Override void walk() throws Throwable{
    var gui= project("testGui1");
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
    var program= new Rectangle(930, 500, 120, 120);
    var before= look();
    run.go();
    programShown.go();
    changed(before, program);
    terminate.go();
    terminated.go();
    same(before, program);
    stabilize();
    checkContent(info, """
      {
        "projects": {
          "start": {
            "path": "Str:%s",
            "kind": "code"
          }
        },
        "systemExtensions": []
      }
      """.formatted(slashed(gui)));
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    closeFiles();
    same(bare, program);
  }
}

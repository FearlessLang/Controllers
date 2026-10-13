package agentTools;

import java.awt.Rectangle;

final class TerminateTest extends ManagerTest{
  final Action becomeCode= action("becomeCode",
    on("ubuntu_gnome", () -> click(val(970), val(113))),
    on("xubuntu_xfce", () -> click(val(976), val(93))));
  final Action openTestGui1= action("openTestGui1",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action openStartMarker= action("openStartMarker",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action projectShown= action("projectShown",
    on("ubuntu_gnome", () -> waitAfter(val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(null))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome", () -> waitAfter(val(17500))),
    on("xubuntu_xfce", () -> waitAfter(val(21500))));
  final Action codeShown= action("codeShown",
    on("ubuntu_gnome", () -> waitAfter(val(4500))),
    on("xubuntu_xfce", () -> waitAfter(val(6000))));
  final Action compile= action("compile",
    on("ubuntu_gnome", () -> click(val(523), val(113))),
    on("xubuntu_xfce", () -> click(val(481), val(93))));
  final Action compiled= action("compiled",
    on("ubuntu_gnome", () -> waitAfter(val(21000))),
    on("xubuntu_xfce", () -> waitAfter(val(26000))));
  final Action run= action("run",
    on("ubuntu_gnome", () -> click(val(523), val(113))),
    on("xubuntu_xfce", () -> click(val(466), val(93))));
  final Action programShown= action("programShown",
    on("ubuntu_gnome", () -> waitAfter(val(8500))),
    on("xubuntu_xfce", () -> waitAfter(val(10500))));
  final Action terminate= action("terminate",
    on("ubuntu_gnome", () -> click(val(523), val(113))),
    on("xubuntu_xfce", () -> click(val(489), val(93))));
  final Action terminated= action("terminated",
    on("ubuntu_gnome", () -> waitAfter(val(4500))),
    on("xubuntu_xfce", () -> waitAfter(val(6000))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome", () -> waitAfter(val(7000))),
    on("xubuntu_xfce", () -> waitAfter(val(8500))));
  @Override void walk() throws Throwable{
    var gui= project("testGui1");
    var bare= look();
    start();
    managerShown.go();
    toFiles();
    openTestGui1.go();
    openStartMarker.go();
    closeFiles();
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
    same(bare, program);
  }
}

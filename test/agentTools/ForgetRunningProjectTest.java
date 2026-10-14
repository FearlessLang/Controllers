package agentTools;


final class ForgetRunningProjectTest extends ManagerTest{
  final Action centerArea= action("centerArea",
    on("ubuntu_gnome", () -> region(val(760), val(460), val(400), val(160))),
    on("xubuntu_xfce", () -> region(val(760), val(460), val(400), val(160))));
  final Action becomeCode= action("becomeCode",
    on("ubuntu_gnome", () -> waitClick(val(0), val(970), val(113))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(976), val(93))));
  final Action forget= action("forget",
    on("ubuntu_gnome", () -> waitClick(val(0), val(873), val(113))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(862), val(93))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome", () -> waitAfter(val(17500))),
    on("xubuntu_xfce", () -> waitAfter(val(21500))));
  final Action codeShown= action("codeShown",
    on("ubuntu_gnome", () -> waitAfter(val(4500))),
    on("xubuntu_xfce", () -> waitAfter(val(6000))));
  final Action compile= action("compile",
    on("ubuntu_gnome", () -> waitClick(val(0), val(523), val(113))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(481), val(93))));
  final Action compiled= action("compiled",
    on("ubuntu_gnome", () -> waitAfter(val(21000))),
    on("xubuntu_xfce", () -> waitAfter(val(26000))));
  final Action run= action("run",
    on("ubuntu_gnome", () -> waitClick(val(0), val(523), val(113))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(466), val(93))));
  final Action programShown= action("programShown",
    on("ubuntu_gnome", () -> waitAfter(val(8500))),
    on("xubuntu_xfce", () -> waitAfter(val(10500))));
  final Action ignored= action("ignored",
    on("ubuntu_gnome", () -> waitAfter(val(4500))),
    on("xubuntu_xfce", () -> waitAfter(val(6000))));
  final Action terminate= action("terminate",
    on("ubuntu_gnome", () -> waitClick(val(0), val(523), val(113))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(489), val(93))));
  final Action terminated= action("terminated",
    on("ubuntu_gnome", () -> waitAfter(val(5500))),
    on("xubuntu_xfce", () -> waitAfter(val(6500))));
  final Action forgotten= action("forgotten",
    on("ubuntu_gnome", () -> waitAfter(val(4500))),
    on("xubuntu_xfce", () -> waitAfter(val(6000))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome", () -> waitAfter(val(7000))),
    on("xubuntu_xfce", () -> waitAfter(val(8500))));
  final Action openTestGui1= action("openTestGui1",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action openStartMarker= action("openStartMarker",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action projectShown= action("projectShown",
    on("ubuntu_gnome", () -> waitAfter(val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(null))));
  @Override void walk() throws Throwable{
    var gui= project("testGui1");
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
    var code= """
      {
        "projects": {
          "start": {
            "path": "Str:%s",
            "kind": "code"
          }
        },
        "systemExtensions": []
      }
      """.formatted(slashed(gui));
    centerArea.go();
    var center= region;
    var greyed= inWindow(720, 102, 197, 22);
    var compiledLook= look();
    run.go();
    programShown.go();
    var running= look();
    forget.go();
    ignored.go();
    same(running, greyed);
    same(compiledLook, center);
    stabilize();
    checkContent(info, code);
    terminate.go();
    terminated.go();
    forget.go();
    forgotten.go();
    stabilize();
    checkContent(info, "{\n  \"projects\": {},\n  \"systemExtensions\": []\n}\n");
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    closeFiles();
  }
}

package agentTools;

final class RelaunchKeepsRegistryTest extends ManagerTest{
  final Action becomeCode= action("becomeCode",
    on("ubuntu_gnome", () -> click(val(1027), val(113))),
    on("xubuntu_xfce", () -> click(val(1040), val(93))));
  final Action openHelloWorld= action("openHelloWorld",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action openHelloWorldMarker= action("openHelloWorldMarker",
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
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome", () -> waitAfter(val(5500))),
    on("xubuntu_xfce", () -> waitAfter(val(6500))));
  final Action managerShownAgain= action("managerShownAgain",
    on("ubuntu_gnome", () -> waitAfter(val(17500))),
    on("xubuntu_xfce", () -> waitAfter(val(21500))));
  final Action tileSelected= action("tileSelected",
    on("ubuntu_gnome", () -> waitAfter(val(4000))),
    on("xubuntu_xfce", () -> waitAfter(val(5000))));
  final Action managerQuitAgain= action("managerQuitAgain",
    on("ubuntu_gnome", () -> waitAfter(val(7000))),
    on("xubuntu_xfce", () -> waitAfter(val(9000))));
  @Override void walk() throws Throwable{
    var project= project("helloWorld");
    var bare= look();
    start();
    managerShown.go();
    toFiles();
    openHelloWorld.go();
    openHelloWorldMarker.go();
    closeFiles();
    projectShown.go();
    becomeCode.go();
    codeShown.go();
    var panel= inWindow(410, 95, 420, 60);
    var selected= look();
    var remembered= """
      {
        "projects": {
          "hello_world": {
            "path": "Str:%s",
            "kind": "code"
          }
        },
        "systemExtensions": []
      }
      """.formatted(slashed(project));
    stabilize();
    checkContent(info, remembered);
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    same(bare, panel);
    start();
    managerShownAgain.go();
    changed(selected, panel);
    stabilize();
    checkContent(info, remembered);
    firstTile.go();
    tileSelected.go();
    same(selected, panel);
    managerMenu.go();
    quitManager.go();
    managerQuitAgain.go();
    same(bare, panel);
  }
}

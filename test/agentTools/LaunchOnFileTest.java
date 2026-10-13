package agentTools;

import java.awt.Rectangle;

final class LaunchOnFileTest extends ManagerTest{
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome", () -> waitAfter(val(17500))),
    on("xubuntu_xfce", () -> waitAfter(val(21500))));
  final Action selected= action("selected",
    on("ubuntu_gnome", () -> waitAfter(val(10000))),
    on("xubuntu_xfce", () -> waitAfter(val(12500))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome", () -> waitAfter(val(8500))),
    on("xubuntu_xfce", () -> waitAfter(val(11000))));
  final Action openHelloWorld= action("openHelloWorld",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action openHelloWorldMarker= action("openHelloWorldMarker",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action projectShown= action("projectShown",
    on("ubuntu_gnome", () -> waitAfter(val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(null))));
  final Action openHelloWorldAgain= action("openHelloWorldAgain",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action openHelloWorldMarkerAgain= action("openHelloWorldMarkerAgain",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  @Override void walk() throws Throwable{
    var project= project("helloWorld");
    var remembered= """
      {
        "projects": {
          "hello_world": {
            "path": "Str:%s",
            "kind": "idle"
          }
        },
        "systemExtensions": []
      }
      """.formatted(slashed(project));
    start();
    managerShown.go();
    toFiles();
    openHelloWorld.go();
    openHelloWorldMarker.go();
    closeFiles();
    projectShown.go();
    stabilize();
    checkContent(info, remembered);
    var before= look();
    toFiles();
    openHelloWorldAgain.go();
    openHelloWorldMarkerAgain.go();
    closeFiles();
    selected.go();
    same(before, new Rectangle(760, 460, 400, 160));
    same(before, inWindow(69, 95, 1100, 85));
    stabilize();
    checkContent(info, remembered);
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(info, remembered);
  }
}

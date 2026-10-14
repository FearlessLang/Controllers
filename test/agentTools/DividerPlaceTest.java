package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DividerPlaceTest extends ManagerTest{
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
  final Action tilesEnd= action("tilesEnd",
    on("ubuntu_gnome", () -> listEnd(val(200), val(700), val(385))),
    on("xubuntu_xfce", () -> listEnd(val(133), val(680), val(318))));
  final Action managerHidden= action("managerHidden",
    on("ubuntu_gnome", () -> waitAfter(val(3500))),
    on("xubuntu_xfce", () -> waitAfter(val(4500))));
  final Action tilesWhole= action("tilesWhole",
    on("ubuntu_gnome", () -> listEnd(val(200), val(700), val(1918))),
    on("xubuntu_xfce", () -> listEnd(val(133), val(680), val(1918))));
  final Action tilesGone= action("tilesGone",
    on("ubuntu_gnome", () -> listEnd(val(200), val(700), val(200))),
    on("xubuntu_xfce", () -> listEnd(val(133), val(680), val(133))));
  final Action secondEnded= action("secondEnded",
    on("ubuntu_gnome", () -> waitAfter(val(15500))),
    on("xubuntu_xfce", () -> waitAfter(val(19500))));
  final Action managerBack= action("managerBack",
    on("ubuntu_gnome", () -> waitAfter(val(2500))),
    on("xubuntu_xfce", () -> waitAfter(val(3000))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome", () -> waitAfter(val(6500))),
    on("xubuntu_xfce", () -> waitAfter(val(8000))));
  final Action thirdShown= action("thirdShown",
    on("ubuntu_gnome", () -> waitAfter(val(17500))),
    on("xubuntu_xfce", () -> waitAfter(val(22000))));
  final Action thirdQuit= action("thirdQuit",
    on("ubuntu_gnome", () -> waitAfter(val(6500))),
    on("xubuntu_xfce", () -> waitAfter(val(8000))));
  final Action fourthShown= action("fourthShown",
    on("ubuntu_gnome", () -> waitAfter(val(17500))),
    on("xubuntu_xfce", () -> waitAfter(val(22000))));
  final Action projectShownFourth= action("projectShownFourth",
    on("ubuntu_gnome", () -> waitAfter(val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(null))));
  final Action fourthQuit= action("fourthQuit",
    on("ubuntu_gnome", () -> waitAfter(val(6500))),
    on("xubuntu_xfce", () -> waitAfter(val(8000))));
  @Override void walk() throws Throwable{
    project("helloWorld");
    start();
    managerShown.go();
    toFiles();
    openHelloWorld.go();
    openHelloWorldMarker.go();
    closeFiles();
    projectShown.go();
    tilesEnd.go();
    closeWindow.go();
    managerHidden.go();
    tilesGone.go();
    start();
    secondEnded.go();
    managerBack.go();
    tilesEnd.go();
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    start();
    thirdShown.go();
    tilesWhole.go();
    managerMenu.go();
    quitManager.go();
    thirdQuit.go();
    start();
    fourthShown.go();
    toFiles();
    openHelloWorld.go();
    openHelloWorldMarker.go();
    closeFiles();
    projectShownFourth.go();
    tilesEnd.go();
    managerMenu.go();
    quitManager.go();
    fourthQuit.go();
  }
  void listEnd(int x, int y, int end){
    var shot= pilot.shot();
    var at= x;
    for (; at<shot.getWidth() && (shot.getRGB(at, y)&0xffffff)==0xffffff; at += 1){}
    assertEquals(end, at, "The tile list, white along the row y="+y+" from x="+x+", ends at x="+at);
  }
}

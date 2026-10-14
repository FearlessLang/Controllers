package agentTools;

final class TileSwitchTest extends ManagerTest{
  final Action openHelloWorld= action("openHelloWorld",
    on("ubuntu_gnome", () -> waitDoubleClick(val(100), val(911), val(380))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action openHelloWorldMarker= action("openHelloWorldMarker",
    on("ubuntu_gnome", () -> waitDoubleClick(val(100), val(911), val(380))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action projectShown= action("projectShown",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("xubuntu_xfce", () -> waitAfter(val(null))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("xubuntu_xfce", () -> waitAfter(val(21500))));
  final Action checkHello= action("checkHello",
    on("ubuntu_gnome", () -> waitClick(val(0), val(580), val(113))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(538), val(93))));
  final Action helloChecked= action("helloChecked",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("xubuntu_xfce", () -> waitAfter(val(6000))));
  final Action openHelloStackTraces= action("openHelloStackTraces",
    on("ubuntu_gnome", () -> waitDoubleClick(val(100), val(798), val(380))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action openStartMarker= action("openStartMarker",
    on("ubuntu_gnome", () -> waitDoubleClick(val(100), val(911), val(380))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action secondShown= action("secondShown",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("xubuntu_xfce", () -> waitAfter(val(13500))));
  final Action helloTile= action("helloTile",
    on("ubuntu_gnome", () -> waitClick(val(0), val(133), val(130))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(66), val(110))));
  final Action helloShown= action("helloShown",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("xubuntu_xfce", () -> waitAfter(val(5000))));
  final Action startTile= action("startTile",
    on("ubuntu_gnome", () -> waitClick(val(0), val(261), val(130))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(194), val(110))));
  final Action startShown= action("startShown",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("xubuntu_xfce", () -> waitAfter(val(5000))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("xubuntu_xfce", () -> waitAfter(val(9000))));
  @Override void walk() throws Throwable{
    project("helloWorld");
    project("helloStackTraces");
    var bare= look();
    start();
    managerShown.go();
    toFiles();
    openHelloWorld.go();
    openHelloWorldMarker.go();
    projectShown.go();
    var panel= inWindow(410, 95, 1000, 155);
    var name= inWindow(450, 95, 50, 32);
    var output= inWindow(410, 168, 1000, 80);
    var helloEmpty= look();
    checkHello.go();
    helloChecked.go();
    changed(helloEmpty, panel);
    var hello= look();
    toFiles();
    openHelloStackTraces.go();
    openStartMarker.go();
    secondShown.go();
    changed(hello, panel);
    var start= look();
    helloTile.go();
    helloShown.go();
    same(hello, name);
    same(hello, output);
    startTile.go();
    startShown.go();
    same(start, name);
    same(start, output);
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    closeFiles();
    same(bare, panel);
  }
}

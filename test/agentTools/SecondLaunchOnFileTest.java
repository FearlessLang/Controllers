package agentTools;

final class SecondLaunchOnFileTest extends ManagerTest{
  final Action openHelloWorld= action("openHelloWorld",
    on("ubuntu_gnome",() -> waitDoubleClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",() -> waitDoubleClick(val(null),val(null),val(null))));
  final Action openHelloWorldMarker= action("openHelloWorldMarker",
    on("ubuntu_gnome",() -> waitDoubleClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",() -> waitDoubleClick(val(null),val(null),val(null))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",() -> waitAfter(val(17500))),
    on("xubuntu_xfce",() -> waitAfter(val(21500))));
  final Action openHelloStackTraces= action("openHelloStackTraces",
    on("ubuntu_gnome",() -> waitDoubleClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",() -> waitDoubleClick(val(null),val(null),val(null))));
  final Action openStartMarker= action("openStartMarker",
    on("ubuntu_gnome",() -> waitDoubleClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",() -> waitDoubleClick(val(null),val(null),val(null))));
  final Action secondShown= action("secondShown",
    on("ubuntu_gnome",() -> waitAfter(val(10000))),
    on("xubuntu_xfce",() -> waitAfter(val(12500))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",() -> waitAfter(val(7000))),
    on("xubuntu_xfce",() -> waitAfter(val(9000))));
  @Override void walk() throws Throwable{
    var project= project("helloWorld");
    var other= project("helloStackTraces");
    var bare= look();
    start();
    managerShown.go();
    toFiles();
    openHelloWorld.go();
    openHelloWorldMarker.go();
    closeFiles();
    toFiles();
    openHelloStackTraces.go();
    openStartMarker.go();
    closeFiles();
    secondShown.go();
    stabilize();
    checkContent(info,"""
      {
        "projects": {
          "hello_world": {
            "path": "Str:%s",
            "kind": "idle"
          },
          "start": {
            "path": "Str:%s",
            "kind": "idle"
          }
        },
        "systemExtensions": []
      }
      """.formatted(slashed(project),slashed(other)));
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    same(bare,inWindow(100,200,200,400));
  }
}

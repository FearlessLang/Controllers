package agentTools;

final class OpenFearlessFileTest extends ManagerTest{
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("xubuntu_xfce", () -> waitAfter(val(21500))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("xubuntu_xfce", () -> waitAfter(val(9000))));
  final Action filesShown= action("filesShown",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("xubuntu_xfce", () -> waitAfter(val(21000))));
  final Action managerOpened= action("managerOpened",
    on("ubuntu_gnome", () -> waitAfter(val(2000))),//the double click starts a manager that is not running: its JVM starts and applies the Register
    on("xubuntu_xfce", () -> waitAfter(val(26500))));
  final Action managerQuitAgain= action("managerQuitAgain",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("xubuntu_xfce", () -> waitAfter(val(8500))));
  final Action openHelloWorld= action("openHelloWorld",
    on("ubuntu_gnome", () -> waitDoubleClick(val(100), val(798), val(380))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action openHelloWorldMarker= action("openHelloWorldMarker",
    on("ubuntu_gnome", () -> waitDoubleClick(val(100), val(911), val(380))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  @Override void walk() throws Throwable{
    var project= project("helloWorld");
    start();
    managerShown.go();
    stabilize();
    checkContent(info, "{\n  \"projects\": {},\n  \"systemExtensions\": []\n}\n");
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    closeFiles();
    var bare= look();
    toFiles();
    openHelloWorld.go();
    filesShown.go();
    var fileManager= Pilot.changed(bare, pilot.shot(), 6);
    openHelloWorldMarker.go();
    managerOpened.go();
    stabilize();
    checkContent(info, """
      {
        "projects": {
          "hello_world": {
            "path": "Str:%s",
            "kind": "idle"
          }
        },
        "systemExtensions": []
      }
      """.formatted(slashed(project)));
    managerMenu.go();
    quitManager.go();
    managerQuitAgain.go();
    closeFiles();
    same(bare, fileManager);
  }
}

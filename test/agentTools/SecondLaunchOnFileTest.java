package agentTools;

import java.util.List;

final class SecondLaunchOnFileTest extends ManagerTest{
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(3500))),
    on("xubuntu_xfce",()->waitUntilTime(val(4500))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(8500))),
    on("xubuntu_xfce",()->waitUntilTime(val(11000))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(26000))),
    on("xubuntu_xfce",()->waitUntilTime(val(32500))));
  final Action appsShownSecond= action("appsShownSecond",
    on("ubuntu_gnome",()->waitUntilTime(val(29000))),
    on("xubuntu_xfce",()->waitUntilTime(val(36500))));
  final Action terminalShownSecond= action("terminalShownSecond",
    on("ubuntu_gnome",()->waitUntilTime(val(34000))),
    on("xubuntu_xfce",()->waitUntilTime(val(42500))));
  final Action secondShown= action("secondShown",
    on("ubuntu_gnome",()->waitUntilTime(val(44000))),
    on("xubuntu_xfce",()->waitUntilTime(val(55000))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",()->waitUntilTime(val(51000))),
    on("xubuntu_xfce",()->waitUntilTime(val(64000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var project= project("helloWorld");
    var other= project("helloStackTraces");
    launchScript("first",project.resolve("hello_world.fearless"));
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    launchScript("second",other.resolve("start.fearless"));
    runInTerminal(appsShownSecond,terminalShownSecond);
    secondShown.go();
    stabilize();
    checkContent(List.of("second.exit"),"0\n");
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
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
  }
}

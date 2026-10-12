package agentTools;

import java.awt.Rectangle;
import java.util.List;

final class LaunchOnFileTest extends ManagerTest{
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(3500))),
    on("xubuntu_xfce",()->waitUntilTime(val(4500))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(8500))),
    on("xubuntu_xfce",()->waitUntilTime(val(11000))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(26000))),
    on("xubuntu_xfce",()->waitUntilTime(val(32500))));
  final Action appsShownOnFolder= action("appsShownOnFolder",
    on("ubuntu_gnome",()->waitUntilTime(val(29000))),
    on("xubuntu_xfce",()->waitUntilTime(val(36500))));
  final Action terminalShownOnFolder= action("terminalShownOnFolder",
    on("ubuntu_gnome",()->waitUntilTime(val(34000))),
    on("xubuntu_xfce",()->waitUntilTime(val(42500))));
  final Action selected= action("selected",
    on("ubuntu_gnome",()->waitUntilTime(val(44000))),
    on("xubuntu_xfce",()->waitUntilTime(val(55000))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",()->waitUntilTime(val(52500))),
    on("xubuntu_xfce",()->waitUntilTime(val(66000))));
  @Override void walk() throws Throwable{
    noManagerData();
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
    launchScript("first",project.resolve("hello_world.fearless"));
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    stabilize();
    checkContent(info,remembered);
    var before= look();
    launchScript("second",project);
    runInTerminal(appsShownOnFolder,terminalShownOnFolder);
    selected.go();
    same(before,new Rectangle(760,460,400,160));
    same(before,inWindow(69,95,1100,85));
    stabilize();
    checkContent(List.of("second.exit"),"0\n");
    checkContent(info,remembered);
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
    checkContent(info,remembered);
  }
}

package agentTools;

import java.util.List;

final class LaunchOnFileTest extends ManagerTest{
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(2000))),
    on("debian_gnome_x11",()->waitUntilTime(val(2000))),
    on("xubuntu_xfce",()->waitUntilTime(val(2000))),
    on("debian_cinnamon",()->waitUntilTime(val(2000))),
    on("debian_mate",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(5500))),
    on("debian_gnome_x11",()->waitUntilTime(val(6500))),
    on("xubuntu_xfce",()->waitUntilTime(val(6500))),
    on("debian_cinnamon",()->waitUntilTime(val(6500))),
    on("debian_mate",()->waitUntilTime(val(6500))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(18000))),
    on("debian_gnome_x11",()->waitUntilTime(val(20000))),
    on("xubuntu_xfce",()->waitUntilTime(val(20000))),
    on("debian_cinnamon",()->waitUntilTime(val(20000))),
    on("debian_mate",()->waitUntilTime(val(20000))));
  final Action appsShownToEnd= action("appsShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(19500))),
    on("debian_gnome_x11",()->waitUntilTime(val(22000))),
    on("xubuntu_xfce",()->waitUntilTime(val(22000))),
    on("debian_cinnamon",()->waitUntilTime(val(22000))),
    on("debian_mate",()->waitUntilTime(val(22000))));
  final Action terminalShownToEnd= action("terminalShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(23000))),
    on("debian_gnome_x11",()->waitUntilTime(val(26500))),
    on("xubuntu_xfce",()->waitUntilTime(val(26500))),
    on("debian_cinnamon",()->waitUntilTime(val(26500))),
    on("debian_mate",()->waitUntilTime(val(26500))));
  final Action managerEnded= action("managerEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(33000))),
    on("debian_gnome_x11",()->waitUntilTime(val(38000))),
    on("xubuntu_xfce",()->waitUntilTime(val(38000))),
    on("debian_cinnamon",()->waitUntilTime(val(38000))),
    on("debian_mate",()->waitUntilTime(val(38000))));
  final Action appsShownOnFile= action("appsShownOnFile",
    on("ubuntu_gnome",()->waitUntilTime(val(34500))),
    on("debian_gnome_x11",()->waitUntilTime(val(40000))),
    on("xubuntu_xfce",()->waitUntilTime(val(40000))),
    on("debian_cinnamon",()->waitUntilTime(val(40000))),
    on("debian_mate",()->waitUntilTime(val(40000))));
  final Action terminalShownOnFile= action("terminalShownOnFile",
    on("ubuntu_gnome",()->waitUntilTime(val(38000))),
    on("debian_gnome_x11",()->waitUntilTime(val(44500))),
    on("xubuntu_xfce",()->waitUntilTime(val(44500))),
    on("debian_cinnamon",()->waitUntilTime(val(44500))),
    on("debian_mate",()->waitUntilTime(val(44500))));
  final Action managerShownOnFile= action("managerShownOnFile",
    on("ubuntu_gnome",()->waitUntilTime(val(51000))),
    on("debian_gnome_x11",()->waitUntilTime(val(58000))),
    on("xubuntu_xfce",()->waitUntilTime(val(58000))),
    on("debian_cinnamon",()->waitUntilTime(val(58000))),
    on("debian_mate",()->waitUntilTime(val(58000))));
  final Action appsShownToEndOnFile= action("appsShownToEndOnFile",
    on("ubuntu_gnome",()->waitUntilTime(val(52500))),
    on("debian_gnome_x11",()->waitUntilTime(val(60000))),
    on("xubuntu_xfce",()->waitUntilTime(val(60000))),
    on("debian_cinnamon",()->waitUntilTime(val(60000))),
    on("debian_mate",()->waitUntilTime(val(60000))));
  final Action terminalShownToEndOnFile= action("terminalShownToEndOnFile",
    on("ubuntu_gnome",()->waitUntilTime(val(56000))),
    on("debian_gnome_x11",()->waitUntilTime(val(64500))),
    on("xubuntu_xfce",()->waitUntilTime(val(64500))),
    on("debian_cinnamon",()->waitUntilTime(val(64500))),
    on("debian_mate",()->waitUntilTime(val(64500))));
  final Action managerEndedOnFile= action("managerEndedOnFile",
    on("ubuntu_gnome",()->waitUntilTime(val(66000))),
    on("debian_gnome_x11",()->waitUntilTime(val(76000))),
    on("xubuntu_xfce",()->waitUntilTime(val(76000))),
    on("debian_cinnamon",()->waitUntilTime(val(76000))),
    on("debian_mate",()->waitUntilTime(val(76000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var project= project("helloWorld");
    var remembered= """
      {
        "hello_world": {
          "path": "Str:%s",
          "kind": "idle"
        }
      }
      """.formatted(slashed(project));
    launchScript("first",project);
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    stabilize();
    checkContent(info,remembered);
    endScript();
    runInTerminal(appsShownToEnd,terminalShownToEnd);
    managerEnded.go();
    stabilize();
    checkContent(List.of("first.exit"),"137\n");
    noManagerData();
    launchScript("second",project.resolve("hello_world.fearless"));
    runInTerminal(appsShownOnFile,terminalShownOnFile);
    managerShownOnFile.go();
    stabilize();
    checkContent(info,remembered);
    endScript();
    runInTerminal(appsShownToEndOnFile,terminalShownToEndOnFile);
    managerEndedOnFile.go();
    stabilize();
    checkContent(List.of("second.exit"),"137\n");
  }
}

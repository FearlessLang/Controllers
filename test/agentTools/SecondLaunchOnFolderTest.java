package agentTools;

import java.util.List;

final class SecondLaunchOnFolderTest extends ManagerTest{
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(2000))),
    on("debian_gnome_x11",()->waitUntilTime(val(2000))),
    on("xubuntu_xfce",()->waitUntilTime(val(2000))),
    on("debian_cinnamon",()->waitUntilTime(val(2000))),
    on("debian_mate",()->waitUntilTime(val(2000))),
    on("lubuntu_lxqt",()->waitUntilTime(val(2000))),
    on("kubuntu_plasma",()->waitUntilTime(val(2000))),
    on("void_i3",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(5500))),
    on("debian_gnome_x11",()->waitUntilTime(val(6500))),
    on("xubuntu_xfce",()->waitUntilTime(val(6500))),
    on("debian_cinnamon",()->waitUntilTime(val(6500))),
    on("debian_mate",()->waitUntilTime(val(6500))),
    on("lubuntu_lxqt",()->waitUntilTime(val(7000))),
    on("kubuntu_plasma",()->waitUntilTime(val(7000))),
    on("void_i3",()->waitUntilTime(val(6500))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(18000))),
    on("debian_gnome_x11",()->waitUntilTime(val(20000))),
    on("xubuntu_xfce",()->waitUntilTime(val(20000))),
    on("debian_cinnamon",()->waitUntilTime(val(20000))),
    on("debian_mate",()->waitUntilTime(val(20000))),
    on("lubuntu_lxqt",()->waitUntilTime(val(21000))),
    on("kubuntu_plasma",()->waitUntilTime(val(21000))),
    on("void_i3",()->waitUntilTime(val(30000))));
  final Action appsShownSecond= action("appsShownSecond",
    on("ubuntu_gnome",()->waitUntilTime(val(19500))),
    on("debian_gnome_x11",()->waitUntilTime(val(22000))),
    on("xubuntu_xfce",()->waitUntilTime(val(22000))),
    on("debian_cinnamon",()->waitUntilTime(val(22000))),
    on("debian_mate",()->waitUntilTime(val(22000))),
    on("lubuntu_lxqt",()->waitUntilTime(val(23000))),
    on("kubuntu_plasma",()->waitUntilTime(val(23000))),
    on("void_i3",()->waitUntilTime(val(32000))));
  final Action terminalShownSecond= action("terminalShownSecond",
    on("ubuntu_gnome",()->waitUntilTime(val(23000))),
    on("debian_gnome_x11",()->waitUntilTime(val(26500))),
    on("xubuntu_xfce",()->waitUntilTime(val(26500))),
    on("debian_cinnamon",()->waitUntilTime(val(26500))),
    on("debian_mate",()->waitUntilTime(val(26500))),
    on("lubuntu_lxqt",()->waitUntilTime(val(28000))),
    on("kubuntu_plasma",()->waitUntilTime(val(28000))),
    on("void_i3",()->waitUntilTime(val(36500))));
  final Action secondShown= action("secondShown",
    on("ubuntu_gnome",()->waitUntilTime(val(34000))),
    on("debian_gnome_x11",()->waitUntilTime(val(40000))),
    on("xubuntu_xfce",()->waitUntilTime(val(40000))),
    on("debian_cinnamon",()->waitUntilTime(val(40000))),
    on("debian_mate",()->waitUntilTime(val(40000))),
    on("lubuntu_lxqt",()->waitUntilTime(val(42000))),
    on("kubuntu_plasma",()->waitUntilTime(val(42000))),
    on("void_i3",()->waitUntilTime(val(60000))));
  final Action appsShownToEnd= action("appsShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(35500))),
    on("debian_gnome_x11",()->waitUntilTime(val(42000))),
    on("xubuntu_xfce",()->waitUntilTime(val(42000))),
    on("debian_cinnamon",()->waitUntilTime(val(42000))),
    on("debian_mate",()->waitUntilTime(val(42000))),
    on("lubuntu_lxqt",()->waitUntilTime(val(44000))),
    on("kubuntu_plasma",()->waitUntilTime(val(44000))),
    on("void_i3",()->waitUntilTime(val(62000))));
  final Action terminalShownToEnd= action("terminalShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(39000))),
    on("debian_gnome_x11",()->waitUntilTime(val(46500))),
    on("xubuntu_xfce",()->waitUntilTime(val(46500))),
    on("debian_cinnamon",()->waitUntilTime(val(46500))),
    on("debian_mate",()->waitUntilTime(val(46500))),
    on("lubuntu_lxqt",()->waitUntilTime(val(49000))),
    on("kubuntu_plasma",()->waitUntilTime(val(49000))),
    on("void_i3",()->waitUntilTime(val(66500))));
  final Action managerEnded= action("managerEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(49000))),
    on("debian_gnome_x11",()->waitUntilTime(val(57000))),
    on("xubuntu_xfce",()->waitUntilTime(val(57000))),
    on("debian_cinnamon",()->waitUntilTime(val(57000))),
    on("debian_mate",()->waitUntilTime(val(57000))),
    on("lubuntu_lxqt",()->waitUntilTime(val(60000))),
    on("kubuntu_plasma",()->waitUntilTime(val(60000))),
    on("void_i3",()->waitUntilTime(val(80000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var project= project("helloWorld");
    var other= project("helloStackTraces");
    launchScript("first",project);
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    launchScript("second",other);
    runInTerminal(appsShownSecond,terminalShownSecond);
    secondShown.go();
    stabilize();
    checkContent(List.of("second.exit"),"0\n");
    checkContent(info,"""
      {
        "hello_world": {
          "path": "Str:%s",
          "kind": "idle"
        },
        "start": {
          "path": "Str:%s",
          "kind": "idle"
        }
      }
      """.formatted(slashed(project),slashed(other)));
    endScript();
    runInTerminal(appsShownToEnd,terminalShownToEnd);
    managerEnded.go();
    stabilize();
    checkContent(List.of("first.exit"),"137\n");
  }
}

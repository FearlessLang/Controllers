package agentTools;

import java.awt.event.KeyEvent;
import java.util.List;

final class QuitWhileRunningTest extends ManagerTest{
  final List<String> console= List.of(data,"eclipse","start","console.txt");
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(2000))),
    on("debian_gnome_x11",()->waitUntilTime(val(2000))),
    on("xubuntu_xfce",()->waitUntilTime(val(2000))),
    on("debian_cinnamon",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(5500))),
    on("debian_gnome_x11",()->waitUntilTime(val(6500))),
    on("xubuntu_xfce",()->waitUntilTime(val(6500))),
    on("debian_cinnamon",()->waitUntilTime(val(6500))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(18500))),
    on("debian_gnome_x11",()->waitUntilTime(val(20000))),
    on("xubuntu_xfce",()->waitUntilTime(val(20000))),
    on("debian_cinnamon",()->waitUntilTime(val(20000))));
  final Action focusTiles= action("focusTiles",
    on("ubuntu_gnome",()->click(val(200),val(1500))),
    on("debian_gnome_x11",()->click(val(134),val(1500))),
    on("xubuntu_xfce",()->click(val(268),val(1500))),
    on("debian_cinnamon",()->click(val(134),val(1500))),
    on("windows",()->click(val(200),val(400))));
  final Action becomeCode= action("becomeCode",
    on("ubuntu_gnome",()->click(val(400),val(139))),
    on("debian_gnome_x11",()->click(val(334),val(140))),
    on("xubuntu_xfce",()->click(val(762),val(239))),
    on("debian_cinnamon",()->click(val(334),val(108))),
    on("windows",()->click(val(332),val(94))));
  final Action codeShown= action("codeShown",
    on("ubuntu_gnome",()->waitUntilTime(val(21600))),
    on("debian_gnome_x11",()->waitUntilTime(val(24000))),
    on("xubuntu_xfce",()->waitUntilTime(val(24000))),
    on("debian_cinnamon",()->waitUntilTime(val(24000))));
  final Action compile= action("compile",
    on("ubuntu_gnome",()->click(val(164),val(111))),
    on("debian_gnome_x11",()->click(val(108),val(112))),
    on("xubuntu_xfce",()->click(val(224),val(185))),
    on("debian_cinnamon",()->click(val(109),val(80))),
    on("windows",()->click(val(102),val(67))));
  final Action compiled= action("compiled",
    on("ubuntu_gnome",()->waitUntilTime(val(35200))),
    on("debian_gnome_x11",()->waitUntilTime(val(36000))),
    on("xubuntu_xfce",()->waitUntilTime(val(36000))),
    on("debian_cinnamon",()->waitUntilTime(val(36000))));
  final Action run= action("run",
    on("ubuntu_gnome",()->click(val(164),val(111))),
    on("debian_gnome_x11",()->click(val(96),val(112))),
    on("xubuntu_xfce",()->click(val(194),val(185))),
    on("debian_cinnamon",()->click(val(96),val(80))),
    on("windows",()->click(val(102),val(67))));
  final Action programShown= action("programShown",
    on("ubuntu_gnome",()->waitUntilTime(val(39800))),
    on("debian_gnome_x11",()->waitUntilTime(val(44000))),
    on("xubuntu_xfce",()->waitUntilTime(val(44000))),
    on("debian_cinnamon",()->waitUntilTime(val(44000))));
  final Action managerMenu= action("managerMenu",
    on("ubuntu_gnome",()->click(val(98),val(79))),
    on("debian_gnome_x11",()->click(val(32),val(80))),
    on("xubuntu_xfce",()->click(val(74),val(120))),
    on("debian_cinnamon",()->click(val(32),val(47))),
    on("windows",()->click(val(31),val(33))));
  final Action quitManager= action("quitManager",
    on("ubuntu_gnome",()->click(val(116),val(212))),
    on("debian_gnome_x11",()->click(val(60),val(212))),
    on("xubuntu_xfce",()->click(val(110),val(362))),
    on("debian_cinnamon",()->click(val(60),val(180))),
    on("windows",()->click(val(61),val(167))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",()->waitUntilTime(val(43000))),
    on("debian_gnome_x11",()->waitUntilTime(val(49000))),
    on("xubuntu_xfce",()->waitUntilTime(val(49000))),
    on("debian_cinnamon",()->waitUntilTime(val(49000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var gui= project("testGui1");
    launchScript("first",gui);
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    focusTiles.go();
    keys(KeyEvent.VK_F8);
    keys(KeyEvent.VK_HOME);
    becomeCode.go();
    codeShown.go();
    stabilize();
    checkContent(info,"""
      {
        "start": {
          "path": "Str:%s",
          "kind": "code"
        }
      }
      """.formatted(slashed(gui)));
    compile.go();
    compiled.go();
    stabilize();
    checkContent(console,"--- compiling testGui1 ---\n--- compile done ---\n");
    run.go();
    programShown.go();
    stabilize();
    checkContent(console,"--- compiling testGui1 ---\n--- compile done ---\n--- running gui_example.Foo ---\n");
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
  }
}

package agentTools;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.util.List;

final class WindowCloseTest extends ManagerTest{
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
    on("ubuntu_gnome",()->waitUntilTime(val(18000))),
    on("debian_gnome_x11",()->waitUntilTime(val(20000))),
    on("xubuntu_xfce",()->waitUntilTime(val(20000))),
    on("debian_cinnamon",()->waitUntilTime(val(20000))));
  final Action closeManager= action("closeManager",
    on("ubuntu_gnome",()->click(val(3822),val(50))),
    on("debian_gnome_x11",()->click(val(3822),val(50))),
    on("xubuntu_xfce",()->click(val(3818),val(74))),
    on("debian_cinnamon",()->click(val(3821),val(18))),
    on("windows",()->click(val(1255),val(11))));
  final Action managerHidden= action("managerHidden",
    on("ubuntu_gnome",()->waitUntilTime(val(20000))),
    on("debian_gnome_x11",()->waitUntilTime(val(22000))),
    on("xubuntu_xfce",()->waitUntilTime(val(22000))),
    on("debian_cinnamon",()->waitUntilTime(val(22000))));
  final Action appsShownSecond= action("appsShownSecond",
    on("ubuntu_gnome",()->waitUntilTime(val(21500))),
    on("debian_gnome_x11",()->waitUntilTime(val(24000))),
    on("xubuntu_xfce",()->waitUntilTime(val(24000))),
    on("debian_cinnamon",()->waitUntilTime(val(24000))));
  final Action terminalShownSecond= action("terminalShownSecond",
    on("ubuntu_gnome",()->waitUntilTime(val(25000))),
    on("debian_gnome_x11",()->waitUntilTime(val(28500))),
    on("xubuntu_xfce",()->waitUntilTime(val(28500))),
    on("debian_cinnamon",()->waitUntilTime(val(28500))));
  final Action secondEnded= action("secondEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(36000))),
    on("debian_gnome_x11",()->waitUntilTime(val(42000))),
    on("xubuntu_xfce",()->waitUntilTime(val(42000))),
    on("debian_cinnamon",()->waitUntilTime(val(42000))));
  final Action managerBack= action("managerBack",
    on("ubuntu_gnome",()->waitUntilTime(val(37000))),
    on("debian_gnome_x11",()->waitUntilTime(val(43000))),
    on("xubuntu_xfce",()->waitUntilTime(val(43000))),
    on("debian_cinnamon",()->waitUntilTime(val(43000))));
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
    on("ubuntu_gnome",()->waitUntilTime(val(41500))),
    on("debian_gnome_x11",()->waitUntilTime(val(48000))),
    on("xubuntu_xfce",()->waitUntilTime(val(48000))),
    on("debian_cinnamon",()->waitUntilTime(val(48000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var project= project("helloWorld");
    launchScript("first",project);
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    closeManager.go();
    managerHidden.go();
    stabilize();
    assertFalse(Files.exists(filesIOFolder.resolve("first.exit")));
    launchScript("second");
    runInTerminal(appsShownSecond,terminalShownSecond);
    secondEnded.go();
    stabilize();
    checkContent(List.of("second.exit"),"0\n");
    managerBack.go();
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
  }
}

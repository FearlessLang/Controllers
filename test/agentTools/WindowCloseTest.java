package agentTools;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.util.List;

final class WindowCloseTest extends ManagerTest{
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
  final Action closeManager= action("closeManager",
    on("ubuntu_gnome",()->click(val(3822),val(50))),
    on("debian_gnome_x11",()->click(val(3822),val(50))),
    on("xubuntu_xfce",()->click(val(3818),val(74))),
    on("debian_cinnamon",()->click(val(3821),val(18))),
    on("debian_mate",()->click(val(3800),val(82))),
    on("lubuntu_lxqt",()->click(val(3829),val(282))),
    on("kubuntu_plasma",()->click(val(3825),val(14))),
    on("void_i3",()->keys(KeyEvent.VK_ALT,KeyEvent.VK_SHIFT,KeyEvent.VK_Q)),
    on("windows",()->click(val(1255),val(11))));
  final Action managerHidden= action("managerHidden",
    on("ubuntu_gnome",()->waitUntilTime(val(20000))),
    on("debian_gnome_x11",()->waitUntilTime(val(22000))),
    on("xubuntu_xfce",()->waitUntilTime(val(22000))),
    on("debian_cinnamon",()->waitUntilTime(val(22000))),
    on("debian_mate",()->waitUntilTime(val(22000))),
    on("lubuntu_lxqt",()->waitUntilTime(val(23000))),
    on("kubuntu_plasma",()->waitUntilTime(val(23000))),
    on("void_i3",()->waitUntilTime(val(32000))));
  final Action appsShownSecond= action("appsShownSecond",
    on("ubuntu_gnome",()->waitUntilTime(val(21500))),
    on("debian_gnome_x11",()->waitUntilTime(val(24000))),
    on("xubuntu_xfce",()->waitUntilTime(val(24000))),
    on("debian_cinnamon",()->waitUntilTime(val(24000))),
    on("debian_mate",()->waitUntilTime(val(24000))),
    on("lubuntu_lxqt",()->waitUntilTime(val(25000))),
    on("kubuntu_plasma",()->waitUntilTime(val(25000))),
    on("void_i3",()->waitUntilTime(val(34000))));
  final Action terminalShownSecond= action("terminalShownSecond",
    on("ubuntu_gnome",()->waitUntilTime(val(25000))),
    on("debian_gnome_x11",()->waitUntilTime(val(28500))),
    on("xubuntu_xfce",()->waitUntilTime(val(28500))),
    on("debian_cinnamon",()->waitUntilTime(val(28500))),
    on("debian_mate",()->waitUntilTime(val(28500))),
    on("lubuntu_lxqt",()->waitUntilTime(val(30000))),
    on("kubuntu_plasma",()->waitUntilTime(val(30000))),
    on("void_i3",()->waitUntilTime(val(38500))));
  final Action secondEnded= action("secondEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(36000))),
    on("debian_gnome_x11",()->waitUntilTime(val(42000))),
    on("xubuntu_xfce",()->waitUntilTime(val(42000))),
    on("debian_cinnamon",()->waitUntilTime(val(42000))),
    on("debian_mate",()->waitUntilTime(val(42000))),
    on("lubuntu_lxqt",()->waitUntilTime(val(44000))),
    on("kubuntu_plasma",()->waitUntilTime(val(44000))),
    on("void_i3",()->waitUntilTime(val(62000))));
  final Action managerBack= action("managerBack",
    on("ubuntu_gnome",()->waitUntilTime(val(37000))),
    on("debian_gnome_x11",()->waitUntilTime(val(43000))),
    on("xubuntu_xfce",()->waitUntilTime(val(43000))),
    on("debian_cinnamon",()->waitUntilTime(val(43000))),
    on("debian_mate",()->waitUntilTime(val(43000))),
    on("lubuntu_lxqt",()->waitUntilTime(val(45000))),
    on("kubuntu_plasma",()->waitUntilTime(val(45000))),
    on("void_i3",()->waitUntilTime(val(63000))));
  final Action managerMenu= action("managerMenu",
    on("ubuntu_gnome",()->click(val(98),val(79))),
    on("debian_gnome_x11",()->click(val(32),val(80))),
    on("xubuntu_xfce",()->click(val(74),val(120))),
    on("debian_cinnamon",()->click(val(32),val(47))),
    on("debian_mate",()->click(val(64),val(138))),
    on("lubuntu_lxqt",()->click(val(24),val(300))),
    on("kubuntu_plasma",()->click(val(30),val(39))),
    on("void_i3",()->click(val(78),val(54))),
    on("windows",()->click(val(31),val(33))));
  final Action quitManager= action("quitManager",
    on("ubuntu_gnome",()->click(val(116),val(212))),
    on("debian_gnome_x11",()->click(val(60),val(212))),
    on("xubuntu_xfce",()->click(val(110),val(362))),
    on("debian_cinnamon",()->click(val(60),val(180))),
    on("debian_mate",()->click(val(110),val(405))),
    on("lubuntu_lxqt",()->click(val(40),val(400))),
    on("kubuntu_plasma",()->click(val(60),val(171))),
    on("void_i3",()->click(val(115),val(297))),
    on("windows",()->click(val(61),val(167))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",()->waitUntilTime(val(41500))),
    on("debian_gnome_x11",()->waitUntilTime(val(48000))),
    on("xubuntu_xfce",()->waitUntilTime(val(48000))),
    on("debian_cinnamon",()->waitUntilTime(val(48000))),
    on("debian_mate",()->waitUntilTime(val(48000))),
    on("lubuntu_lxqt",()->waitUntilTime(val(50000))),
    on("kubuntu_plasma",()->waitUntilTime(val(50000))),
    on("void_i3",()->waitUntilTime(val(70000))));
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

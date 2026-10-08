package agentTools;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.util.List;

final class WindowCloseTest extends ManagerTest{
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(18000))));
  final Action closeManager= action("closeManager",
    on("ubuntu_gnome",()->click(val(3822),val(50))),
    on("windows",()->click(val(1255),val(11))));
  final Action managerHidden= action("managerHidden",
    on("ubuntu_gnome",()->waitUntilTime(val(20000))));
  final Action appsShownSecond= action("appsShownSecond",
    on("ubuntu_gnome",()->waitUntilTime(val(21500))));
  final Action terminalShownSecond= action("terminalShownSecond",
    on("ubuntu_gnome",()->waitUntilTime(val(25000))));
  final Action secondEnded= action("secondEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(36000))));
  final Action managerBack= action("managerBack",
    on("ubuntu_gnome",()->waitUntilTime(val(37000))));
  final Action managerMenu= action("managerMenu",
    on("ubuntu_gnome",()->click(val(98),val(79))),
    on("windows",()->click(val(31),val(33))));
  final Action quitManager= action("quitManager",
    on("ubuntu_gnome",()->click(val(116),val(212))),
    on("windows",()->click(val(61),val(167))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",()->waitUntilTime(val(41500))));
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

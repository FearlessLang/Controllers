package agentTools;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.awt.Rectangle;
import java.nio.file.Files;
import java.util.List;

final class WindowCloseTest extends ManagerTest{
  final Action appsShown= action("appsShown",on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",on("ubuntu_gnome",()->waitUntilTime(val(18000))));
  final Action managerHidden= action("managerHidden",on("ubuntu_gnome",()->waitUntilTime(val(20000))));
  final Action appsShownSecond= action("appsShownSecond",on("ubuntu_gnome",()->waitUntilTime(val(21500))));
  final Action terminalShownSecond= action("terminalShownSecond",on("ubuntu_gnome",()->waitUntilTime(val(25000))));
  final Action managerBack= action("managerBack",on("ubuntu_gnome",()->waitUntilTime(val(33000))));
  final Action hiddenAgain= action("hiddenAgain",on("ubuntu_gnome",()->waitUntilTime(val(35000))));
  final Action trayMenuShown= action("trayMenuShown",on("ubuntu_gnome",()->waitUntilTime(val(36500))));
  final Action backFromTray= action("backFromTray",on("ubuntu_gnome",()->waitUntilTime(val(40000))));
  final Action managerQuit= action("managerQuit",on("ubuntu_gnome",()->waitUntilTime(val(45000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var project= project("helloWorld");
    var tiles= new Rectangle(100,200,200,400);
    launchScript("first",project.resolve("hello_world.fearless"));
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    var shown= look();
    closeWindow.go();
    managerHidden.go();
    changed(shown,tiles);
    stabilize();
    assertFalse(Files.exists(filesIOFolder.resolve("first.exit")));
    launchScript("second");
    runInTerminal(appsShownSecond,terminalShownSecond);
    managerBack.go();
    same(shown,tiles);
    stabilize();
    checkContent(List.of("second.exit"),"0\n");
    closeWindow.go();
    hiddenAgain.go();
    changed(shown,tiles);
    trayIcon.go();
    trayMenuShown.go();
    trayShow.go();
    backFromTray.go();
    same(shown,tiles);
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
  }
}

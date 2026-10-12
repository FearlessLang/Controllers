package agentTools;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.awt.Rectangle;
import java.nio.file.Files;
import java.util.List;

final class WindowCloseTest extends ManagerTest{
  final Action appsShown= action("appsShown",on("ubuntu_gnome",()->waitUntilTime(val(3500))));
  final Action terminalShown= action("terminalShown",on("ubuntu_gnome",()->waitUntilTime(val(8500))));
  final Action managerShown= action("managerShown",on("ubuntu_gnome",()->waitUntilTime(val(26000))));
  final Action managerHidden= action("managerHidden",on("ubuntu_gnome",()->waitUntilTime(val(29500))));
  final Action appsShownSecond= action("appsShownSecond",on("ubuntu_gnome",()->waitUntilTime(val(32500))));
  final Action terminalShownSecond= action("terminalShownSecond",on("ubuntu_gnome",()->waitUntilTime(val(37500))));
  final Action managerBack= action("managerBack",on("ubuntu_gnome",()->waitUntilTime(val(48500))));
  final Action hiddenAgain= action("hiddenAgain",on("ubuntu_gnome",()->waitUntilTime(val(52000))));
  final Action trayMenuShown= action("trayMenuShown",on("ubuntu_gnome",()->waitUntilTime(val(55000))));
  final Action backFromTray= action("backFromTray",on("ubuntu_gnome",()->waitUntilTime(val(60000))));
  final Action hiddenThird= action("hiddenThird",on("ubuntu_gnome",()->waitUntilTime(val(63500))));
  final Action trayIconRight= action("trayIconRight",on("ubuntu_gnome",()->rightClick(val(1736),val(15))));
  final Action trayMenuShownAgain= action("trayMenuShownAgain",on("ubuntu_gnome",()->waitUntilTime(val(66500))));
  final Action trayQuit= action("trayQuit",on("ubuntu_gnome",()->click(val(1706),val(117))));
  final Action managerQuit= action("managerQuit",on("ubuntu_gnome",()->waitUntilTime(val(73000))));
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
    closeWindow.go();
    hiddenThird.go();
    changed(shown,tiles);
    trayIconRight.go();
    trayMenuShownAgain.go();
    trayQuit.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
  }
}

package agentTools;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.util.List;

final class WindowCloseTest extends ManagerTest{
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitAfter(val(3500))),
    on("xubuntu_xfce",()->waitAfter(val(4500))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitAfter(val(5000))),
    on("xubuntu_xfce",()->waitAfter(val(6500))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitAfter(val(17500))),
    on("xubuntu_xfce",()->waitAfter(val(21500))));
  final Action managerHidden= action("managerHidden",
    on("ubuntu_gnome",()->waitAfter(val(3500))),
    on("xubuntu_xfce",()->waitAfter(val(4500))));
  final Action appsShownSecond= action("appsShownSecond",
    on("ubuntu_gnome",()->waitAfter(val(3000))),
    on("xubuntu_xfce",()->waitAfter(val(4000))));
  final Action terminalShownSecond= action("terminalShownSecond",
    on("ubuntu_gnome",()->waitAfter(val(5000))),
    on("xubuntu_xfce",()->waitAfter(val(6000))));
  final Action managerBack= action("managerBack",
    on("ubuntu_gnome",()->waitAfter(val(11000))),
    on("xubuntu_xfce",()->waitAfter(val(14000))));
  final Action hiddenAgain= action("hiddenAgain",
    on("ubuntu_gnome",()->waitAfter(val(3500))),
    on("xubuntu_xfce",()->waitAfter(val(4000))));
  final Action trayMenuShown= action("trayMenuShown",
    on("ubuntu_gnome",()->waitAfter(val(3000))),
    on("xubuntu_xfce",()->waitAfter(val(4000))));
  final Action backFromTray= action("backFromTray",
    on("ubuntu_gnome",()->waitAfter(val(5000))),
    on("xubuntu_xfce",()->waitAfter(val(6000))));
  final Action hiddenThird= action("hiddenThird",
    on("ubuntu_gnome",()->waitAfter(val(3500))),
    on("xubuntu_xfce",()->waitAfter(val(4500))));
  final Action trayIconRight= action("trayIconRight",
    on("ubuntu_gnome",()->rightClick(val(1736),val(15))),
    on("xubuntu_xfce",()->rightClick(val(1783),val(12))));
  final Action trayMenuShownAgain= action("trayMenuShownAgain",
    on("ubuntu_gnome",()->waitAfter(val(3000))),
    on("xubuntu_xfce",()->waitAfter(val(4000))));
  final Action trayQuit= action("trayQuit",
    on("ubuntu_gnome",()->click(val(1706),val(117))),
    on("xubuntu_xfce",()->click(val(1715),val(66))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",()->waitAfter(val(6500))),
    on("xubuntu_xfce",()->waitAfter(val(8000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var project= project("helloWorld");
    var tiles= inWindow(100,200,200,400);
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

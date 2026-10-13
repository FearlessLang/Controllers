package agentTools;

final class WindowCloseTest extends ManagerTest{
  final Action openHelloWorld= action("openHelloWorld",
    on("ubuntu_gnome",()->waitDoubleClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",()->waitDoubleClick(val(null),val(null),val(null))));
  final Action openHelloWorldMarker= action("openHelloWorldMarker",
    on("ubuntu_gnome",()->waitDoubleClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",()->waitDoubleClick(val(null),val(null),val(null))));
  final Action projectShown= action("projectShown",
    on("ubuntu_gnome",()->waitAfter(val(null))),
    on("xubuntu_xfce",()->waitAfter(val(null))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitAfter(val(17500))),
    on("xubuntu_xfce",()->waitAfter(val(21500))));
  final Action managerHidden= action("managerHidden",
    on("ubuntu_gnome",()->waitAfter(val(3500))),
    on("xubuntu_xfce",()->waitAfter(val(4500))));
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
    project("helloWorld");
    var tiles= inWindow(100,200,200,400);
    var bare= look();
    start();//filesIcon, pilotioBookmark, openApp or openAppNextToManagerFolder, openBin, openLauncher (double click fearlessManaged0_001w): see ManagerTest.start()
    managerShown.go();
    toFiles();
    openHelloWorld.go();
    openHelloWorldMarker.go();
    closeFileManager.go();
    projectShown.go();
    var shown= look();
    closeWindow.go();
    managerHidden.go();
    changed(shown,tiles);
    start();//filesIcon, pilotioBookmark, openApp or openAppNextToManagerFolder, openBin, openLauncher (double click fearlessManaged0_001w): see ManagerTest.start()
    managerBack.go();
    same(shown,tiles);
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
    closeFileManager.go();
    same(bare,tiles);
  }
}

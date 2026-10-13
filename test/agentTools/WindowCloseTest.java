package agentTools;

final class WindowCloseTest extends ManagerTest{
  final Action openHelloWorld= action("openHelloWorld",
    on("ubuntu_gnome", () -> waitDoubleClick(val(100), val(798), val(380))),
    on("lubuntu_lxqt", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitDoubleClick(val(100), val(843), val(294))),
    on("debian_mate", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(100), val(878), val(430))),
    on("arch_sway", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("void_i3", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("omarchy_hyprland", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action openHelloWorldMarker= action("openHelloWorldMarker",
    on("ubuntu_gnome", () -> waitDoubleClick(val(100), val(911), val(380))),
    on("lubuntu_lxqt", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitDoubleClick(val(100), val(872), val(343))),
    on("debian_mate", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(100), val(1015), val(430))),
    on("arch_sway", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("void_i3", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("omarchy_hyprland", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action projectShown= action("projectShown",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("lubuntu_lxqt", () -> waitAfter(val(null))),
    on("fedora_gnome", () -> waitAfter(val(null))),
    on("fedora_cosmic", () -> waitAfter(val(100))),
    on("debian_mate", () -> waitAfter(val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(100))),
    on("arch_sway", () -> waitAfter(val(null))),
    on("void_i3", () -> waitAfter(val(null))),
    on("omarchy_hyprland", () -> waitAfter(val(null))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("lubuntu_lxqt", () -> waitAfter(val(null))),
    on("fedora_gnome", () -> waitAfter(val(null))),
    on("fedora_cosmic", () -> waitAfter(val(200))),
    on("debian_mate", () -> waitAfter(val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(100))),
    on("arch_sway", () -> waitAfter(val(null))),
    on("void_i3", () -> waitAfter(val(null))),
    on("omarchy_hyprland", () -> waitAfter(val(null))));
  final Action managerHidden= action("managerHidden",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("lubuntu_lxqt", () -> waitAfter(val(null))),
    on("fedora_gnome", () -> waitAfter(val(null))),
    on("fedora_cosmic", () -> waitAfter(val(100))),
    on("debian_mate", () -> waitAfter(val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(100))),
    on("arch_sway", () -> waitAfter(val(null))),
    on("void_i3", () -> waitAfter(val(null))),
    on("omarchy_hyprland", () -> waitAfter(val(null))));
  final Action managerBack= action("managerBack",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("lubuntu_lxqt", () -> waitAfter(val(null))),
    on("fedora_gnome", () -> waitAfter(val(null))),
    on("fedora_cosmic", () -> waitAfter(val(100))),
    on("debian_mate", () -> waitAfter(val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(100))),
    on("arch_sway", () -> waitAfter(val(null))),
    on("void_i3", () -> waitAfter(val(null))),
    on("omarchy_hyprland", () -> waitAfter(val(null))));
  final Action hiddenAgain= action("hiddenAgain",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("lubuntu_lxqt", () -> waitAfter(val(null))),
    on("fedora_gnome", () -> waitAfter(val(null))),
    on("fedora_cosmic", () -> waitAfter(val(100))),
    on("debian_mate", () -> waitAfter(val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(100))),
    on("arch_sway", () -> waitAfter(val(null))),
    on("void_i3", () -> waitAfter(val(null))),
    on("omarchy_hyprland", () -> waitAfter(val(null))));
  final Action trayMenuShown= action("trayMenuShown",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("lubuntu_lxqt", () -> waitAfter(val(null))),
    on("fedora_gnome", () -> waitAfter(val(null))),
    on("fedora_cosmic", () -> waitAfter(val(100))),
    on("debian_mate", () -> waitAfter(val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(100))),
    on("arch_sway", () -> waitAfter(val(null))),
    on("void_i3", () -> waitAfter(val(null))),
    on("omarchy_hyprland", () -> waitAfter(val(null))));
  final Action backFromTray= action("backFromTray",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("lubuntu_lxqt", () -> waitAfter(val(null))),
    on("fedora_gnome", () -> waitAfter(val(null))),
    on("fedora_cosmic", () -> waitAfter(val(100))),
    on("debian_mate", () -> waitAfter(val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(100))),
    on("arch_sway", () -> waitAfter(val(null))),
    on("void_i3", () -> waitAfter(val(null))),
    on("omarchy_hyprland", () -> waitAfter(val(null))));
  final Action hiddenThird= action("hiddenThird",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("lubuntu_lxqt", () -> waitAfter(val(null))),
    on("fedora_gnome", () -> waitAfter(val(null))),
    on("fedora_cosmic", () -> waitAfter(val(100))),
    on("debian_mate", () -> waitAfter(val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(100))),
    on("arch_sway", () -> waitAfter(val(null))),
    on("void_i3", () -> waitAfter(val(null))),
    on("omarchy_hyprland", () -> waitAfter(val(null))));
  final Action trayIconRight= action("trayIconRight",
    on("ubuntu_gnome", () -> waitRightClick(val(100), val(1736), val(15))),
    on("lubuntu_lxqt", () -> waitRightClick(val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitRightClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitRightClick(val(100), val(1660), val(15))),
    on("debian_mate", () -> waitRightClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitRightClick(val(100), val(1783), val(12))),
    on("arch_sway", () -> waitRightClick(val(null), val(null), val(null))),
    on("void_i3", () -> waitRightClick(val(null), val(null), val(null))),
    on("omarchy_hyprland", () -> waitRightClick(val(null), val(null), val(null))));
  final Action trayMenuShownAgain= action("trayMenuShownAgain",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("lubuntu_lxqt", () -> waitAfter(val(null))),
    on("fedora_gnome", () -> waitAfter(val(null))),
    on("fedora_cosmic", () -> waitAfter(val(100))),
    on("debian_mate", () -> waitAfter(val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(100))),
    on("arch_sway", () -> waitAfter(val(null))),
    on("void_i3", () -> waitAfter(val(null))),
    on("omarchy_hyprland", () -> waitAfter(val(null))));
  final Action trayQuit= action("trayQuit",
    on("ubuntu_gnome", () -> waitClick(val(100), val(1706), val(117))),
    on("lubuntu_lxqt", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitClick(val(100), val(1552), val(106))),
    on("debian_mate", () -> waitClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitClick(val(100), val(1714), val(67))),
    on("arch_sway", () -> waitClick(val(null), val(null), val(null))),
    on("void_i3", () -> waitClick(val(null), val(null), val(null))),
    on("omarchy_hyprland", () -> waitClick(val(null), val(null), val(null))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("lubuntu_lxqt", () -> waitAfter(val(null))),
    on("fedora_gnome", () -> waitAfter(val(null))),
    on("fedora_cosmic", () -> waitAfter(val(100))),
    on("debian_mate", () -> waitAfter(val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(100))),
    on("arch_sway", () -> waitAfter(val(null))),
    on("void_i3", () -> waitAfter(val(null))),
    on("omarchy_hyprland", () -> waitAfter(val(null))));
  @Override void walk() throws Throwable{
    project("helloWorld");
    var tile= inWindow(69, 95, 128, 85);
    start();//showFilesIcon and filesIcon, or showFileManager and fileManagerToFront, pilotioBookmark, openApp or openAppNextToManagerFolder, openBin, openLauncher (double click fearlessManaged0_001w), confirmLauncher: see ManagerTest.start()
    managerShown.go();
    var empty= look();
    toFiles();//showFileManager, fileManagerToFront (the file manager window of start() is open behind the manager), pilotioBookmark, openFiles: see ManagerTest.toFiles()
    openHelloWorld.go();
    openHelloWorldMarker.go();
    projectShown.go();
    var shown= look();
    changed(empty, tile);
    closeWindow.go();
    managerHidden.go();
    changed(shown, tile);
    closeFiles();//the manager window is hidden, so the file manager is visible on every desk
    start();//showFilesIcon and filesIcon, or showFileManager and fileManagerToFront, pilotioBookmark, openApp or openAppNextToManagerFolder, openBin, openLauncher (double click fearlessManaged0_001w), confirmLauncher: see ManagerTest.start()
    managerBack.go();
    same(shown, tile);
    closeWindow.go();
    hiddenAgain.go();
    changed(shown, tile);
    trayIcon.go();
    trayMenuShown.go();
    trayShow.go();
    backFromTray.go();
    same(shown, tile);
    closeWindow.go();
    hiddenThird.go();
    changed(shown, tile);
    trayIconRight.go();
    trayMenuShownAgain.go();
    trayQuit.go();
    managerQuit.go();
    closeFiles();
    changed(shown, tile);
  }
}

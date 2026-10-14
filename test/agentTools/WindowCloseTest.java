package agentTools;

final class WindowCloseTest extends ManagerTest{
  final Action openHelloWorld= action("openHelloWorld",
    on("ubuntu_gnome", () -> waitDoubleClick(val(100), val(798), val(380))),
    on("kubuntu_plasma", () -> waitDoubleClick(val(100), val(788), val(305))),
    on("opensuse_plasma", () -> waitDoubleClick(val(100), val(797), val(310))),
    on("debian_cinnamon", () -> waitDoubleClick(val(100), val(396), val(305))),
    on("lubuntu_lxqt", () -> waitDoubleClick(val(100), val(910), val(395))),
    on("fedora_gnome", () -> waitDoubleClick(val(100), val(794), val(400))),
    on("debian_gnome_x11", () -> waitDoubleClick(val(100), val(794), val(400))),
    on("fedora_cosmic", () -> waitDoubleClick(val(100), val(843), val(294))),
    on("debian_mate", () -> waitDoubleClick(val(100), val(394), val(353))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(100), val(878), val(430))),
    on("arch_sway", () -> waitDoubleClick(val(100), val(240), val(165))),
    on("void_i3", () -> waitDoubleClick(val(100), val(240), val(122))),
    on("omarchy_hyprland", () -> waitDoubleClick(val(300), val(304), val(155))));
  final Action openHelloWorldMarker= action("openHelloWorldMarker",
    on("ubuntu_gnome", () -> waitDoubleClick(val(100), val(911), val(380))),
    on("kubuntu_plasma", () -> waitDoubleClick(val(100), val(925), val(305))),
    on("opensuse_plasma", () -> waitDoubleClick(val(100), val(934), val(310))),
    on("debian_cinnamon", () -> waitDoubleClick(val(100), val(520), val(305))),
    on("lubuntu_lxqt", () -> waitDoubleClick(val(100), val(1036), val(395))),
    on("fedora_gnome", () -> waitDoubleClick(val(100), val(964), val(400))),
    on("debian_gnome_x11", () -> waitDoubleClick(val(100), val(964), val(400))),
    on("fedora_cosmic", () -> waitDoubleClick(val(100), val(872), val(343))),
    on("debian_mate", () -> waitDoubleClick(val(100), val(553), val(353))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(100), val(1015), val(430))),
    on("arch_sway", () -> waitDoubleClick(val(100), val(378), val(165))),
    on("void_i3", () -> waitDoubleClick(val(100), val(376), val(122))),
    on("omarchy_hyprland", () -> waitDoubleClick(val(300), val(484), val(155))));
  final Action projectShown= action("projectShown",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("kubuntu_plasma", () -> waitAfter(val(100))),
    on("opensuse_plasma", () -> waitAfter(val(100))),
    on("debian_cinnamon", () -> waitAfter(val(100))),
    on("lubuntu_lxqt", () -> waitAfter(val(100))),
    on("fedora_gnome", () -> waitAfter(val(100))),
    on("debian_gnome_x11", () -> waitAfter(val(100))),
    on("fedora_cosmic", () -> waitAfter(val(100))),
    on("debian_mate", () -> waitAfter(val(100))),
    on("xubuntu_xfce", () -> waitAfter(val(100))),
    on("arch_sway", () -> waitAfter(val(100))),
    on("void_i3", () -> waitAfter(val(100))),
    on("omarchy_hyprland", () -> waitAfter(val(100))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("kubuntu_plasma", () -> waitAfter(val(100))),
    on("opensuse_plasma", () -> waitAfter(val(100))),
    on("debian_cinnamon", () -> waitAfter(val(100))),
    on("lubuntu_lxqt", () -> waitAfter(val(100))),
    on("fedora_gnome", () -> waitAfter(val(100))),
    on("debian_gnome_x11", () -> waitAfter(val(100))),
    on("fedora_cosmic", () -> waitAfter(val(200))),
    on("debian_mate", () -> waitAfter(val(100))),
    on("xubuntu_xfce", () -> waitAfter(val(100))),
    on("arch_sway", () -> waitAfter(val(100))),
    on("void_i3", () -> waitAfter(val(100))),
    on("omarchy_hyprland", () -> waitAfter(val(100))));
  final Action managerHidden= action("managerHidden",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("kubuntu_plasma", () -> waitAfter(val(100))),
    on("opensuse_plasma", () -> waitAfter(val(100))),
    on("debian_cinnamon", () -> waitAfter(val(100))),
    on("lubuntu_lxqt", () -> waitAfter(val(100))),
    on("fedora_gnome", () -> waitAfter(val(100))),
    on("debian_gnome_x11", () -> waitAfter(val(100))),
    on("fedora_cosmic", () -> waitAfter(val(100))),
    on("debian_mate", () -> waitAfter(val(100))),
    on("xubuntu_xfce", () -> waitAfter(val(100))),
    on("arch_sway", () -> waitAfter(val(100))),
    on("void_i3", () -> waitAfter(val(100))),
    on("omarchy_hyprland", () -> waitAfter(val(100))));
  final Action managerBack= action("managerBack",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("kubuntu_plasma", () -> waitAfter(val(100))),
    on("opensuse_plasma", () -> waitAfter(val(100))),
    on("debian_cinnamon", () -> waitAfter(val(100))),
    on("lubuntu_lxqt", () -> waitAfter(val(100))),
    on("fedora_gnome", () -> waitAfter(val(100))),
    on("debian_gnome_x11", () -> waitAfter(val(100))),
    on("fedora_cosmic", () -> waitAfter(val(100))),
    on("debian_mate", () -> waitAfter(val(100))),
    on("xubuntu_xfce", () -> waitAfter(val(100))),
    on("arch_sway", () -> waitAfter(val(100))),
    on("void_i3", () -> waitAfter(val(100))),
    on("omarchy_hyprland", () -> waitAfter(val(100))));
  final Action hiddenAgain= action("hiddenAgain",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("kubuntu_plasma", () -> waitAfter(val(100))),
    on("opensuse_plasma", () -> waitAfter(val(100))),
    on("debian_cinnamon", () -> waitAfter(val(100))),
    on("lubuntu_lxqt", () -> waitAfter(val(100))),
    on("fedora_gnome", () -> waitAfter(val(100))),
    on("debian_gnome_x11", () -> waitAfter(val(100))),
    on("fedora_cosmic", () -> waitAfter(val(100))),
    on("debian_mate", () -> waitAfter(val(100))),
    on("xubuntu_xfce", () -> waitAfter(val(100))),
    on("arch_sway", () -> waitAfter(val(100))),
    on("void_i3", () -> waitAfter(val(100))),
    on("omarchy_hyprland", () -> waitAfter(val(100))));
  final Action trayMenuShown= action("trayMenuShown",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("kubuntu_plasma", () -> waitAfter(val(100))),
    on("opensuse_plasma", () -> waitAfter(val(100))),
    on("debian_cinnamon", () -> waitAfter(val(100))),
    on("lubuntu_lxqt", () -> waitAfter(val(100))),
    on("fedora_gnome", () -> waitAfter(val(100))),
    on("debian_gnome_x11", () -> waitAfter(val(100))),
    on("fedora_cosmic", () -> waitAfter(val(100))),
    on("debian_mate", () -> waitAfter(val(100))),
    on("xubuntu_xfce", () -> waitAfter(val(100))),
    on("arch_sway", () -> waitAfter(val(100))),
    on("void_i3", () -> waitAfter(val(100))),
    on("omarchy_hyprland", () -> waitAfter(val(100))));
  final Action backFromTray= action("backFromTray",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("kubuntu_plasma", () -> waitAfter(val(100))),
    on("opensuse_plasma", () -> waitAfter(val(100))),
    on("debian_cinnamon", () -> waitAfter(val(100))),
    on("lubuntu_lxqt", () -> waitAfter(val(100))),
    on("fedora_gnome", () -> waitAfter(val(100))),
    on("debian_gnome_x11", () -> waitAfter(val(100))),
    on("fedora_cosmic", () -> waitAfter(val(100))),
    on("debian_mate", () -> waitAfter(val(100))),
    on("xubuntu_xfce", () -> waitAfter(val(100))),
    on("arch_sway", () -> waitAfter(val(100))),
    on("void_i3", () -> waitAfter(val(100))),
    on("omarchy_hyprland", () -> waitAfter(val(100))));
  final Action hiddenThird= action("hiddenThird",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("kubuntu_plasma", () -> waitAfter(val(100))),
    on("opensuse_plasma", () -> waitAfter(val(100))),
    on("debian_cinnamon", () -> waitAfter(val(100))),
    on("lubuntu_lxqt", () -> waitAfter(val(100))),
    on("fedora_gnome", () -> waitAfter(val(100))),
    on("debian_gnome_x11", () -> waitAfter(val(100))),
    on("fedora_cosmic", () -> waitAfter(val(100))),
    on("debian_mate", () -> waitAfter(val(100))),
    on("xubuntu_xfce", () -> waitAfter(val(100))),
    on("arch_sway", () -> waitAfter(val(100))),
    on("void_i3", () -> waitAfter(val(100))),
    on("omarchy_hyprland", () -> waitAfter(val(100))));
  final Action trayIconRight= action("trayIconRight",
    on("ubuntu_gnome", () -> waitRightClick(val(100), val(1736), val(15))),
    on("kubuntu_plasma", () -> waitRightClick(val(100), val(1640), val(1052))),
    on("opensuse_plasma", () -> waitRightClick(val(100), val(1689), val(1057))),
    on("debian_cinnamon", () -> waitRightClick(val(100), val(1755), val(1058))),
    on("lubuntu_lxqt", () -> waitRightClick(val(100), val(1860), val(1064))),
    on("fedora_gnome", () -> waitRightClick(val(100), val(1900), val(1062))),
    on("debian_gnome_x11", () -> waitRightClick(val(100), val(1902), val(1064))),
    on("fedora_cosmic", () -> waitRightClick(val(100), val(1660), val(15))),
    on("debian_mate", () -> waitRightClick(val(100), val(1904), val(1038))),
    on("xubuntu_xfce", () -> waitRightClick(val(100), val(1783), val(12))),
    on("arch_sway", () -> waitRightClick(val(100), val(1882), val(14))),
    on("void_i3", () -> waitRightClick(val(100), val(1900), val(1062))),
    on("omarchy_hyprland", () -> waitRightClick(val(100), val(1829), val(12))));
  final Action trayMenuShownAgain= action("trayMenuShownAgain",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("kubuntu_plasma", () -> waitAfter(val(100))),
    on("opensuse_plasma", () -> waitAfter(val(100))),
    on("debian_cinnamon", () -> waitAfter(val(100))),
    on("lubuntu_lxqt", () -> waitAfter(val(100))),
    on("fedora_gnome", () -> waitAfter(val(100))),
    on("debian_gnome_x11", () -> waitAfter(val(100))),
    on("fedora_cosmic", () -> waitAfter(val(100))),
    on("debian_mate", () -> waitAfter(val(100))),
    on("xubuntu_xfce", () -> waitAfter(val(100))),
    on("arch_sway", () -> waitAfter(val(100))),
    on("void_i3", () -> waitAfter(val(100))),
    on("omarchy_hyprland", () -> waitAfter(val(100))));
  final Action trayQuit= action("trayQuit",
    on("ubuntu_gnome", () -> waitClick(val(100), val(1706), val(117))),
    on("kubuntu_plasma", () -> waitClick(val(100), val(1657), val(1007))),
    on("opensuse_plasma", () -> waitClick(val(100), val(1737), val(1015))),
    on("debian_cinnamon", () -> waitClick(val(100), val(1805), val(1023))),
    on("lubuntu_lxqt", () -> waitClick(val(100), val(1853), val(1030))),
    on("fedora_gnome", () -> waitClick(val(100), val(1850), val(1067))),
    on("debian_gnome_x11", () -> waitClick(val(100), val(1858), val(1067))),
    on("fedora_cosmic", () -> waitClick(val(100), val(1552), val(106))),
    on("debian_mate", () -> waitClick(val(100), val(1857), val(1067))),
    on("xubuntu_xfce", () -> waitClick(val(100), val(1714), val(67))),
    on("arch_sway", () -> waitClick(val(100), val(1800), val(57))),
    on("void_i3", () -> waitClick(val(100), val(1850), val(1068))),
    on("omarchy_hyprland", () -> waitClick(val(100), val(1760), val(85))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("kubuntu_plasma", () -> waitAfter(val(100))),
    on("opensuse_plasma", () -> waitAfter(val(100))),
    on("debian_cinnamon", () -> waitAfter(val(100))),
    on("lubuntu_lxqt", () -> waitAfter(val(100))),
    on("fedora_gnome", () -> waitAfter(val(100))),
    on("debian_gnome_x11", () -> waitAfter(val(100))),
    on("fedora_cosmic", () -> waitAfter(val(100))),
    on("debian_mate", () -> waitAfter(val(100))),
    on("xubuntu_xfce", () -> waitAfter(val(100))),
    on("arch_sway", () -> waitAfter(val(100))),
    on("void_i3", () -> waitAfter(val(100))),
    on("omarchy_hyprland", () -> waitAfter(val(100))));
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

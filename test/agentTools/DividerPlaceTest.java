package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DividerPlaceTest extends ManagerTest{
  final Action openHelloWorld= action("openHelloWorld",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("kubuntu_plasma", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("opensuse_plasma", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("debian_cinnamon", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("lubuntu_lxqt", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("debian_gnome_x11", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("debian_mate", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("arch_sway", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("void_i3", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("omarchy_hyprland", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action openHelloWorldMarker= action("openHelloWorldMarker",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("kubuntu_plasma", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("opensuse_plasma", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("debian_cinnamon", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("lubuntu_lxqt", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("debian_gnome_x11", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("debian_mate", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("arch_sway", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("void_i3", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("omarchy_hyprland", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action projectShown= action("projectShown",
    on("ubuntu_gnome", () -> waitAfter(val(null))),
    on("kubuntu_plasma", () -> waitAfter(val(null))),
    on("opensuse_plasma", () -> waitAfter(val(null))),
    on("debian_cinnamon", () -> waitAfter(val(null))),
    on("lubuntu_lxqt", () -> waitAfter(val(null))),
    on("fedora_gnome", () -> waitAfter(val(null))),
    on("debian_gnome_x11", () -> waitAfter(val(null))),
    on("fedora_cosmic", () -> waitAfter(val(null))),
    on("debian_mate", () -> waitAfter(val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(null))),
    on("arch_sway", () -> waitAfter(val(null))),
    on("void_i3", () -> waitAfter(val(null))),
    on("omarchy_hyprland", () -> waitAfter(val(null))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome", () -> waitAfter(val(17500))),
    on("kubuntu_plasma", () -> waitAfter(val(null))),
    on("opensuse_plasma", () -> waitAfter(val(null))),
    on("debian_cinnamon", () -> waitAfter(val(null))),
    on("lubuntu_lxqt", () -> waitAfter(val(null))),
    on("fedora_gnome", () -> waitAfter(val(null))),
    on("debian_gnome_x11", () -> waitAfter(val(null))),
    on("fedora_cosmic", () -> waitAfter(val(null))),
    on("debian_mate", () -> waitAfter(val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(21500))),
    on("arch_sway", () -> waitAfter(val(null))),
    on("void_i3", () -> waitAfter(val(null))),
    on("omarchy_hyprland", () -> waitAfter(val(null))));
  final Action tilesEnd= action("tilesEnd",
    on("ubuntu_gnome", () -> listEnd(val(200), val(700), val(385))),
    on("kubuntu_plasma", () -> listEnd(val(null), val(null), val(null))),
    on("opensuse_plasma", () -> listEnd(val(null), val(null), val(null))),
    on("debian_cinnamon", () -> listEnd(val(null), val(null), val(null))),
    on("lubuntu_lxqt", () -> listEnd(val(null), val(null), val(null))),
    on("fedora_gnome", () -> listEnd(val(null), val(null), val(null))),
    on("debian_gnome_x11", () -> listEnd(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> listEnd(val(null), val(null), val(null))),
    on("debian_mate", () -> listEnd(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> listEnd(val(133), val(680), val(318))),
    on("arch_sway", () -> listEnd(val(null), val(null), val(null))),
    on("void_i3", () -> listEnd(val(null), val(null), val(null))),
    on("omarchy_hyprland", () -> listEnd(val(null), val(null), val(null))));
  final Action managerHidden= action("managerHidden",
    on("ubuntu_gnome", () -> waitAfter(val(3500))),
    on("kubuntu_plasma", () -> waitAfter(val(null))),
    on("opensuse_plasma", () -> waitAfter(val(null))),
    on("debian_cinnamon", () -> waitAfter(val(null))),
    on("lubuntu_lxqt", () -> waitAfter(val(null))),
    on("fedora_gnome", () -> waitAfter(val(null))),
    on("debian_gnome_x11", () -> waitAfter(val(null))),
    on("fedora_cosmic", () -> waitAfter(val(null))),
    on("debian_mate", () -> waitAfter(val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(4500))),
    on("arch_sway", () -> waitAfter(val(null))),
    on("void_i3", () -> waitAfter(val(null))),
    on("omarchy_hyprland", () -> waitAfter(val(null))));
  final Action tilesWhole= action("tilesWhole",
    on("ubuntu_gnome", () -> listEnd(val(200), val(700), val(1918))),
    on("kubuntu_plasma", () -> listEnd(val(null), val(null), val(null))),
    on("opensuse_plasma", () -> listEnd(val(null), val(null), val(null))),
    on("debian_cinnamon", () -> listEnd(val(null), val(null), val(null))),
    on("lubuntu_lxqt", () -> listEnd(val(null), val(null), val(null))),
    on("fedora_gnome", () -> listEnd(val(null), val(null), val(null))),
    on("debian_gnome_x11", () -> listEnd(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> listEnd(val(null), val(null), val(null))),
    on("debian_mate", () -> listEnd(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> listEnd(val(133), val(680), val(1918))),
    on("arch_sway", () -> listEnd(val(null), val(null), val(null))),
    on("void_i3", () -> listEnd(val(null), val(null), val(null))),
    on("omarchy_hyprland", () -> listEnd(val(null), val(null), val(null))));
  final Action tilesGone= action("tilesGone",
    on("ubuntu_gnome", () -> listEnd(val(200), val(700), val(200))),
    on("kubuntu_plasma", () -> listEnd(val(null), val(null), val(null))),
    on("opensuse_plasma", () -> listEnd(val(null), val(null), val(null))),
    on("debian_cinnamon", () -> listEnd(val(null), val(null), val(null))),
    on("lubuntu_lxqt", () -> listEnd(val(null), val(null), val(null))),
    on("fedora_gnome", () -> listEnd(val(null), val(null), val(null))),
    on("debian_gnome_x11", () -> listEnd(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> listEnd(val(null), val(null), val(null))),
    on("debian_mate", () -> listEnd(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> listEnd(val(133), val(680), val(133))),
    on("arch_sway", () -> listEnd(val(null), val(null), val(null))),
    on("void_i3", () -> listEnd(val(null), val(null), val(null))),
    on("omarchy_hyprland", () -> listEnd(val(null), val(null), val(null))));
  final Action secondEnded= action("secondEnded",
    on("ubuntu_gnome", () -> waitAfter(val(15500))),
    on("kubuntu_plasma", () -> waitAfter(val(null))),
    on("opensuse_plasma", () -> waitAfter(val(null))),
    on("debian_cinnamon", () -> waitAfter(val(null))),
    on("lubuntu_lxqt", () -> waitAfter(val(null))),
    on("fedora_gnome", () -> waitAfter(val(null))),
    on("debian_gnome_x11", () -> waitAfter(val(null))),
    on("fedora_cosmic", () -> waitAfter(val(null))),
    on("debian_mate", () -> waitAfter(val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(19500))),
    on("arch_sway", () -> waitAfter(val(null))),
    on("void_i3", () -> waitAfter(val(null))),
    on("omarchy_hyprland", () -> waitAfter(val(null))));
  final Action managerBack= action("managerBack",
    on("ubuntu_gnome", () -> waitAfter(val(2500))),
    on("kubuntu_plasma", () -> waitAfter(val(null))),
    on("opensuse_plasma", () -> waitAfter(val(null))),
    on("debian_cinnamon", () -> waitAfter(val(null))),
    on("lubuntu_lxqt", () -> waitAfter(val(null))),
    on("fedora_gnome", () -> waitAfter(val(null))),
    on("debian_gnome_x11", () -> waitAfter(val(null))),
    on("fedora_cosmic", () -> waitAfter(val(null))),
    on("debian_mate", () -> waitAfter(val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(3000))),
    on("arch_sway", () -> waitAfter(val(null))),
    on("void_i3", () -> waitAfter(val(null))),
    on("omarchy_hyprland", () -> waitAfter(val(null))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome", () -> waitAfter(val(6500))),
    on("kubuntu_plasma", () -> waitAfter(val(null))),
    on("opensuse_plasma", () -> waitAfter(val(null))),
    on("debian_cinnamon", () -> waitAfter(val(null))),
    on("lubuntu_lxqt", () -> waitAfter(val(null))),
    on("fedora_gnome", () -> waitAfter(val(null))),
    on("debian_gnome_x11", () -> waitAfter(val(null))),
    on("fedora_cosmic", () -> waitAfter(val(null))),
    on("debian_mate", () -> waitAfter(val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(8000))),
    on("arch_sway", () -> waitAfter(val(null))),
    on("void_i3", () -> waitAfter(val(null))),
    on("omarchy_hyprland", () -> waitAfter(val(null))));
  final Action thirdShown= action("thirdShown",
    on("ubuntu_gnome", () -> waitAfter(val(17500))),
    on("kubuntu_plasma", () -> waitAfter(val(null))),
    on("opensuse_plasma", () -> waitAfter(val(null))),
    on("debian_cinnamon", () -> waitAfter(val(null))),
    on("lubuntu_lxqt", () -> waitAfter(val(null))),
    on("fedora_gnome", () -> waitAfter(val(null))),
    on("debian_gnome_x11", () -> waitAfter(val(null))),
    on("fedora_cosmic", () -> waitAfter(val(null))),
    on("debian_mate", () -> waitAfter(val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(22000))),
    on("arch_sway", () -> waitAfter(val(null))),
    on("void_i3", () -> waitAfter(val(null))),
    on("omarchy_hyprland", () -> waitAfter(val(null))));
  final Action thirdQuit= action("thirdQuit",
    on("ubuntu_gnome", () -> waitAfter(val(6500))),
    on("kubuntu_plasma", () -> waitAfter(val(null))),
    on("opensuse_plasma", () -> waitAfter(val(null))),
    on("debian_cinnamon", () -> waitAfter(val(null))),
    on("lubuntu_lxqt", () -> waitAfter(val(null))),
    on("fedora_gnome", () -> waitAfter(val(null))),
    on("debian_gnome_x11", () -> waitAfter(val(null))),
    on("fedora_cosmic", () -> waitAfter(val(null))),
    on("debian_mate", () -> waitAfter(val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(8000))),
    on("arch_sway", () -> waitAfter(val(null))),
    on("void_i3", () -> waitAfter(val(null))),
    on("omarchy_hyprland", () -> waitAfter(val(null))));
  final Action fourthShown= action("fourthShown",
    on("ubuntu_gnome", () -> waitAfter(val(17500))),
    on("kubuntu_plasma", () -> waitAfter(val(null))),
    on("opensuse_plasma", () -> waitAfter(val(null))),
    on("debian_cinnamon", () -> waitAfter(val(null))),
    on("lubuntu_lxqt", () -> waitAfter(val(null))),
    on("fedora_gnome", () -> waitAfter(val(null))),
    on("debian_gnome_x11", () -> waitAfter(val(null))),
    on("fedora_cosmic", () -> waitAfter(val(null))),
    on("debian_mate", () -> waitAfter(val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(22000))),
    on("arch_sway", () -> waitAfter(val(null))),
    on("void_i3", () -> waitAfter(val(null))),
    on("omarchy_hyprland", () -> waitAfter(val(null))));
  final Action projectShownFourth= action("projectShownFourth",
    on("ubuntu_gnome", () -> waitAfter(val(null))),
    on("kubuntu_plasma", () -> waitAfter(val(null))),
    on("opensuse_plasma", () -> waitAfter(val(null))),
    on("debian_cinnamon", () -> waitAfter(val(null))),
    on("lubuntu_lxqt", () -> waitAfter(val(null))),
    on("fedora_gnome", () -> waitAfter(val(null))),
    on("debian_gnome_x11", () -> waitAfter(val(null))),
    on("fedora_cosmic", () -> waitAfter(val(null))),
    on("debian_mate", () -> waitAfter(val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(null))),
    on("arch_sway", () -> waitAfter(val(null))),
    on("void_i3", () -> waitAfter(val(null))),
    on("omarchy_hyprland", () -> waitAfter(val(null))));
  final Action fourthQuit= action("fourthQuit",
    on("ubuntu_gnome", () -> waitAfter(val(6500))),
    on("kubuntu_plasma", () -> waitAfter(val(null))),
    on("opensuse_plasma", () -> waitAfter(val(null))),
    on("debian_cinnamon", () -> waitAfter(val(null))),
    on("lubuntu_lxqt", () -> waitAfter(val(null))),
    on("fedora_gnome", () -> waitAfter(val(null))),
    on("debian_gnome_x11", () -> waitAfter(val(null))),
    on("fedora_cosmic", () -> waitAfter(val(null))),
    on("debian_mate", () -> waitAfter(val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(8000))),
    on("arch_sway", () -> waitAfter(val(null))),
    on("void_i3", () -> waitAfter(val(null))),
    on("omarchy_hyprland", () -> waitAfter(val(null))));
  @Override void walk() throws Throwable{
    project("helloWorld");
    start();
    managerShown.go();
    toFiles();
    openHelloWorld.go();
    openHelloWorldMarker.go();
    projectShown.go();
    tilesEnd.go();
    closeWindow.go();
    managerHidden.go();
    tilesGone.go();
    start();
    secondEnded.go();
    managerBack.go();
    tilesEnd.go();
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    start();
    thirdShown.go();
    tilesWhole.go();
    managerMenu.go();
    quitManager.go();
    thirdQuit.go();
    start();
    fourthShown.go();
    toFiles();
    openHelloWorld.go();
    openHelloWorldMarker.go();
    projectShownFourth.go();
    tilesEnd.go();
    managerMenu.go();
    quitManager.go();
    fourthQuit.go();
    closeFiles();
  }
  void listEnd(int x, int y, int end){
    var shot= pilot.shot();
    var at= x;
    for (; at<shot.getWidth() && (shot.getRGB(at, y)&0xffffff)==0xffffff; at++){}
    assertEquals(end, at, "The tile list, white along the row y="+y+" from x="+x+", ends at x="+at);
  }
}

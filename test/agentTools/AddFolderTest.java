package agentTools;

final class AddFolderTest extends ManagerTest{
  final Action tilesArea= action("tilesArea",
    on("ubuntu_gnome", () -> region(val(69), val(95), val(128), val(85))),
    on("kubuntu_plasma", () -> region(val(3), val(54), val(128), val(85))),
    on("windows", () -> region(val(3), val(47), val(128), val(88))),
    on("opensuse_plasma", () -> region(val(3), val(54), val(128), val(85))),
    on("debian_cinnamon", () -> region(val(3), val(63), val(128), val(85))),
    on("lubuntu_lxqt", () -> region(val(3), val(56), val(128), val(85))),
    on("fedora_gnome", () -> region(val(3), val(95), val(128), val(85))),
    on("debian_gnome_x11", () -> region(val(3), val(95), val(128), val(85))),
    on("fedora_cosmic", () -> region(val(3), val(94), val(128), val(85))),
    on("debian_mate", () -> region(val(3), val(85), val(128), val(85))),
    on("xubuntu_xfce", () -> region(val(2), val(75), val(128), val(85))),
    on("arch_sway", () -> region(val(965), val(79), val(128), val(85))),
    on("void_i3", () -> region(val(965), val(42), val(128), val(85))),
    on("omarchy_hyprland", () -> region(val(970), val(62), val(128), val(85))));
  final Action centerArea= action("centerArea",
    on("ubuntu_gnome", () -> region(val(760), val(460), val(400), val(160))),
    on("kubuntu_plasma", () -> region(val(null), val(null), val(null), val(null))),
    on("windows", () -> region(val(390), val(155), val(500), val(355))),
    on("opensuse_plasma", () -> region(val(null), val(null), val(null), val(null))),
    on("debian_cinnamon", () -> region(val(null), val(null), val(null), val(null))),
    on("lubuntu_lxqt", () -> region(val(null), val(null), val(null), val(null))),
    on("fedora_gnome", () -> region(val(null), val(null), val(null), val(null))),
    on("debian_gnome_x11", () -> region(val(null), val(null), val(null), val(null))),
    on("fedora_cosmic", () -> region(val(null), val(null), val(null), val(null))),
    on("debian_mate", () -> region(val(null), val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> region(val(760), val(460), val(400), val(160))),
    on("arch_sway", () -> region(val(null), val(null), val(null), val(null))),
    on("void_i3", () -> region(val(null), val(null), val(null), val(null))),
    on("omarchy_hyprland", () -> region(val(null), val(null), val(null), val(null))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome", () -> waitAfter(val(17500))),
    on("kubuntu_plasma", () -> waitAfter(val(null))),
    on("windows", () -> waitAfter(val(8000))),//the manager starts and opens its window
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
  final Action chooserShown= action("chooserShown",
    on("ubuntu_gnome", () -> waitAfter(val(5000))),
    on("kubuntu_plasma", () -> waitAfter(val(null))),
    on("windows", () -> waitAfter(val(3000))),//the folder chooser opens
    on("opensuse_plasma", () -> waitAfter(val(null))),
    on("debian_cinnamon", () -> waitAfter(val(null))),
    on("lubuntu_lxqt", () -> waitAfter(val(null))),
    on("fedora_gnome", () -> waitAfter(val(null))),
    on("debian_gnome_x11", () -> waitAfter(val(null))),
    on("fedora_cosmic", () -> waitAfter(val(null))),
    on("debian_mate", () -> waitAfter(val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(6500))),
    on("arch_sway", () -> waitAfter(val(null))),
    on("void_i3", () -> waitAfter(val(null))),
    on("omarchy_hyprland", () -> waitAfter(val(null))));
  final Action cancel= action("cancel",
    on("ubuntu_gnome", () -> waitClick(val(0), val(1195), val(712))),
    on("kubuntu_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("windows", () -> waitClick(val(100), val(841), val(487))),
    on("opensuse_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("debian_cinnamon", () -> waitClick(val(null), val(null), val(null))),
    on("lubuntu_lxqt", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitClick(val(null), val(null), val(null))),
    on("debian_gnome_x11", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitClick(val(null), val(null), val(null))),
    on("debian_mate", () -> waitClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(1162), val(709))),
    on("arch_sway", () -> waitClick(val(null), val(null), val(null))),
    on("void_i3", () -> waitClick(val(null), val(null), val(null))),
    on("omarchy_hyprland", () -> waitClick(val(null), val(null), val(null))));
  final Action chooserGone= action("chooserGone",
    on("ubuntu_gnome", () -> waitAfter(val(3500))),
    on("kubuntu_plasma", () -> waitAfter(val(null))),
    on("windows", () -> waitAfter(val(2000))),//the folder chooser closes
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
  final Action chooserShownAgain= action("chooserShownAgain",
    on("ubuntu_gnome", () -> waitAfter(val(4500))),
    on("kubuntu_plasma", () -> waitAfter(val(null))),
    on("windows", () -> waitAfter(val(3000))),//the folder chooser opens
    on("opensuse_plasma", () -> waitAfter(val(null))),
    on("debian_cinnamon", () -> waitAfter(val(null))),
    on("lubuntu_lxqt", () -> waitAfter(val(null))),
    on("fedora_gnome", () -> waitAfter(val(null))),
    on("debian_gnome_x11", () -> waitAfter(val(null))),
    on("fedora_cosmic", () -> waitAfter(val(null))),
    on("debian_mate", () -> waitAfter(val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(5500))),
    on("arch_sway", () -> waitAfter(val(null))),
    on("void_i3", () -> waitAfter(val(null))),
    on("omarchy_hyprland", () -> waitAfter(val(null))));
  final Action fileName= action("fileName",
    on("ubuntu_gnome", () -> waitClick(val(0), val(1040), val(640))),
    on("kubuntu_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("windows", () -> waitClick(val(100), val(685), val(417))),
    on("opensuse_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("debian_cinnamon", () -> waitClick(val(null), val(null), val(null))),
    on("lubuntu_lxqt", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitClick(val(null), val(null), val(null))),
    on("debian_gnome_x11", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitClick(val(null), val(null), val(null))),
    on("debian_mate", () -> waitClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(1007), val(637))),
    on("arch_sway", () -> waitClick(val(null), val(null), val(null))),
    on("void_i3", () -> waitClick(val(null), val(null), val(null))),
    on("omarchy_hyprland", () -> waitClick(val(null), val(null), val(null))));
  final Action openButton= action("openButton",
    on("ubuntu_gnome", () -> waitClick(val(null), val(null), val(null))),
    on("kubuntu_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("windows", () -> waitClick(val(100), val(761), val(487))),
    on("opensuse_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("debian_cinnamon", () -> waitClick(val(null), val(null), val(null))),
    on("lubuntu_lxqt", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitClick(val(null), val(null), val(null))),
    on("debian_gnome_x11", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitClick(val(null), val(null), val(null))),
    on("debian_mate", () -> waitClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitClick(val(null), val(null), val(null))),
    on("arch_sway", () -> waitClick(val(null), val(null), val(null))),
    on("void_i3", () -> waitClick(val(null), val(null), val(null))),
    on("omarchy_hyprland", () -> waitClick(val(null), val(null), val(null))));
  final Action tileShown= action("tileShown",
    on("ubuntu_gnome", () -> waitAfter(val(13500))),
    on("kubuntu_plasma", () -> waitAfter(val(null))),
    on("windows", () -> waitAfter(val(6000))),//the manager reads the added folder and shows its tile
    on("opensuse_plasma", () -> waitAfter(val(null))),
    on("debian_cinnamon", () -> waitAfter(val(null))),
    on("lubuntu_lxqt", () -> waitAfter(val(null))),
    on("fedora_gnome", () -> waitAfter(val(null))),
    on("debian_gnome_x11", () -> waitAfter(val(null))),
    on("fedora_cosmic", () -> waitAfter(val(null))),
    on("debian_mate", () -> waitAfter(val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(17000))),
    on("arch_sway", () -> waitAfter(val(null))),
    on("void_i3", () -> waitAfter(val(null))),
    on("omarchy_hyprland", () -> waitAfter(val(null))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome", () -> waitAfter(val(7000))),
    on("kubuntu_plasma", () -> waitAfter(val(null))),
    on("windows", () -> waitAfter(val(4000))),//the manager closes its window and exits
    on("opensuse_plasma", () -> waitAfter(val(null))),
    on("debian_cinnamon", () -> waitAfter(val(null))),
    on("lubuntu_lxqt", () -> waitAfter(val(null))),
    on("fedora_gnome", () -> waitAfter(val(null))),
    on("debian_gnome_x11", () -> waitAfter(val(null))),
    on("fedora_cosmic", () -> waitAfter(val(null))),
    on("debian_mate", () -> waitAfter(val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(8500))),
    on("arch_sway", () -> waitAfter(val(null))),
    on("void_i3", () -> waitAfter(val(null))),
    on("omarchy_hyprland", () -> waitAfter(val(null))));
  @Override void walk() throws Throwable{
    var project= project("helloWorld");
    var bare= look();
    start();
    managerShown.go();
    var empty= look();
    managerMenu.go();
    addFolder.go();
    chooserShown.go();
    centerArea.go();
    var center= region;
    changed(empty, center);
    cancel.go();
    chooserGone.go();
    same(empty, center);
    stabilize();
    checkContent(info, "{\n  \"projects\": {},\n  \"systemExtensions\": []\n}\n");
    managerMenu.go();
    addFolder.go();
    chooserShownAgain.go();
    fileName.go();
    type(project.toString());
    openButton.go();
    tileShown.go();
    tilesArea.go();
    var tiles= region;
    changed(empty, tiles);
    stabilize();
    checkContent(info, """
      {
        "projects": {
          "hello_world": {
            "path": "Str:%s",
            "kind": "idle"
          }
        },
        "systemExtensions": []
      }
      """.formatted(slashed(project)));
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    closeFiles();
    same(bare, tiles);
  }
}

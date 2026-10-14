package agentTools;

import tools.Fs;

final class DoubleClickCompileTest extends ManagerTest{
  final Action outputArea= action("outputArea",
    on("ubuntu_gnome", () -> region(val(410), val(170), val(1000), val(90))),
    on("kubuntu_plasma", () -> region(val(344), val(129), val(1000), val(90))),
    on("windows", () -> region(val(null), val(null), val(null), val(null))),
    on("opensuse_plasma", () -> region(val(344), val(129), val(1000), val(90))),
    on("debian_cinnamon", () -> region(val(344), val(138), val(1000), val(90))),
    on("lubuntu_lxqt", () -> region(val(344), val(131), val(1000), val(90))),
    on("fedora_gnome", () -> region(val(344), val(170), val(1000), val(90))),
    on("debian_gnome_x11", () -> region(val(344), val(170), val(1000), val(90))),
    on("fedora_cosmic", () -> region(val(344), val(169), val(1000), val(90))),
    on("debian_mate", () -> region(val(344), val(160), val(1000), val(90))),
    on("xubuntu_xfce", () -> region(val(343), val(150), val(1000), val(90))),
    on("arch_sway", () -> region(val(1306), val(154), val(1000), val(90))),
    on("void_i3", () -> region(val(1306), val(117), val(1000), val(90))),
    on("omarchy_hyprland", () -> region(val(1311), val(137), val(1000), val(90))));
  final Action centerArea= action("centerArea",
    on("ubuntu_gnome", () -> region(val(760), val(460), val(400), val(160))),
    on("kubuntu_plasma", () -> region(val(null), val(null), val(null), val(null))),
    on("windows", () -> region(val(null), val(null), val(null), val(null))),
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
  final Action becomeCode= action("becomeCode",
    on("ubuntu_gnome", () -> waitClick(val(0), val(976), val(112))),
    on("kubuntu_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("windows", () -> waitClick(val(null), val(null), val(null))),
    on("opensuse_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("debian_cinnamon", () -> waitClick(val(null), val(null), val(null))),
    on("lubuntu_lxqt", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitClick(val(null), val(null), val(null))),
    on("debian_gnome_x11", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitClick(val(null), val(null), val(null))),
    on("debian_mate", () -> waitClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(982), val(93))),
    on("arch_sway", () -> waitClick(val(null), val(null), val(null))),
    on("void_i3", () -> waitClick(val(null), val(null), val(null))),
    on("omarchy_hyprland", () -> waitClick(val(null), val(null), val(null))));
  final Action openHasty= action("openHasty",
    on("ubuntu_gnome", () -> waitDoubleClick(val(100), val(798), val(380))),
    on("kubuntu_plasma", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("windows", () -> waitDoubleClick(val(null), val(null), val(null))),
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
  final Action openHastyMarker= action("openHastyMarker",
    on("ubuntu_gnome", () -> waitDoubleClick(val(100), val(911), val(380))),
    on("kubuntu_plasma", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("windows", () -> waitDoubleClick(val(null), val(null), val(null))),
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
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("kubuntu_plasma", () -> waitAfter(val(null))),
    on("windows", () -> waitAfter(val(null))),
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
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("kubuntu_plasma", () -> waitAfter(val(null))),
    on("windows", () -> waitAfter(val(null))),
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
  final Action codeShown= action("codeShown",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("kubuntu_plasma", () -> waitAfter(val(null))),
    on("windows", () -> waitAfter(val(null))),
    on("opensuse_plasma", () -> waitAfter(val(null))),
    on("debian_cinnamon", () -> waitAfter(val(null))),
    on("lubuntu_lxqt", () -> waitAfter(val(null))),
    on("fedora_gnome", () -> waitAfter(val(null))),
    on("debian_gnome_x11", () -> waitAfter(val(null))),
    on("fedora_cosmic", () -> waitAfter(val(null))),
    on("debian_mate", () -> waitAfter(val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(6000))),
    on("arch_sway", () -> waitAfter(val(null))),
    on("void_i3", () -> waitAfter(val(null))),
    on("omarchy_hyprland", () -> waitAfter(val(null))));
  final Action compileTwice= action("compileTwice",
    on("ubuntu_gnome", () -> waitDoubleClick(val(0), val(544), val(112))),
    on("kubuntu_plasma", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("windows", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("opensuse_plasma", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("debian_cinnamon", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("lubuntu_lxqt", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("debian_gnome_x11", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("debian_mate", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(0), val(488), val(93))),
    on("arch_sway", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("void_i3", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("omarchy_hyprland", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action compiled= action("compiled",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("kubuntu_plasma", () -> waitAfter(val(null))),
    on("windows", () -> waitAfter(val(null))),
    on("opensuse_plasma", () -> waitAfter(val(null))),
    on("debian_cinnamon", () -> waitAfter(val(null))),
    on("lubuntu_lxqt", () -> waitAfter(val(null))),
    on("fedora_gnome", () -> waitAfter(val(null))),
    on("debian_gnome_x11", () -> waitAfter(val(null))),
    on("fedora_cosmic", () -> waitAfter(val(null))),
    on("debian_mate", () -> waitAfter(val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(26000))),
    on("arch_sway", () -> waitAfter(val(null))),
    on("void_i3", () -> waitAfter(val(null))),
    on("omarchy_hyprland", () -> waitAfter(val(null))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("kubuntu_plasma", () -> waitAfter(val(null))),
    on("windows", () -> waitAfter(val(null))),
    on("opensuse_plasma", () -> waitAfter(val(null))),
    on("debian_cinnamon", () -> waitAfter(val(null))),
    on("lubuntu_lxqt", () -> waitAfter(val(null))),
    on("fedora_gnome", () -> waitAfter(val(null))),
    on("debian_gnome_x11", () -> waitAfter(val(null))),
    on("fedora_cosmic", () -> waitAfter(val(null))),
    on("debian_mate", () -> waitAfter(val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(9000))),
    on("arch_sway", () -> waitAfter(val(null))),
    on("void_i3", () -> waitAfter(val(null))),
    on("omarchy_hyprland", () -> waitAfter(val(null))));
  @Override void walk() throws Throwable{
    var hasty= files.resolve("hasty");
    Fs.writeUtf8(hasty.resolve("hasty.fearless"), "\n");
    Fs.writeUtf8(hasty.resolve("_hasty").resolve("_rank_app.fear"), "use base.Main as Main;\n\nGo: Main{sys -> sys.out.println \"went\"}\n");
    var bare= look();
    start();
    managerShown.go();
    toFiles();
    openHasty.go();
    openHastyMarker.go();
    projectShown.go();
    becomeCode.go();
    codeShown.go();
    var before= look();
    compileTwice.go();
    compiled.go();
    outputArea.go();
    var output= region;
    changed(before, output);
    centerArea.go();
    var center= region;
    same(before, center);
    stabilize();
    checkContent(files.resolve("hasty/.fearless_out/mains.info"), "[###]\"hasty.Go\"[###]");
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    closeFiles();
    same(bare, output);
  }
}

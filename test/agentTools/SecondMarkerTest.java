package agentTools;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;

import tools.Fs;

final class SecondMarkerTest extends ManagerTest{
  final Action tileArea= action("tileArea",
    on("ubuntu_gnome", () -> region(val(69), val(95), val(128), val(85))),
    on("kubuntu_plasma", () -> region(val(3), val(54), val(128), val(85))),
    on("windows", () -> region(val(null), val(null), val(null), val(null))),
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
  final Action outputArea= action("outputArea",
    on("ubuntu_gnome", () -> region(val(410), val(168), val(1000), val(80))),
    on("kubuntu_plasma", () -> region(val(344), val(127), val(1000), val(80))),
    on("windows", () -> region(val(null), val(null), val(null), val(null))),
    on("opensuse_plasma", () -> region(val(344), val(127), val(1000), val(80))),
    on("debian_cinnamon", () -> region(val(344), val(136), val(1000), val(80))),
    on("lubuntu_lxqt", () -> region(val(344), val(129), val(1000), val(80))),
    on("fedora_gnome", () -> region(val(344), val(168), val(1000), val(80))),
    on("debian_gnome_x11", () -> region(val(344), val(168), val(1000), val(80))),
    on("fedora_cosmic", () -> region(val(344), val(167), val(1000), val(80))),
    on("debian_mate", () -> region(val(344), val(158), val(1000), val(80))),
    on("xubuntu_xfce", () -> region(val(343), val(148), val(1000), val(80))),
    on("arch_sway", () -> region(val(1306), val(152), val(1000), val(80))),
    on("void_i3", () -> region(val(1306), val(115), val(1000), val(80))),
    on("omarchy_hyprland", () -> region(val(1311), val(135), val(1000), val(80))));
  final Action openTwice= action("openTwice",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
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
  final Action openTwiceMarker= action("openTwiceMarker",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
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
    on("ubuntu_gnome", () -> waitAfter(val(null))),
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
    on("ubuntu_gnome", () -> waitAfter(val(17500))),
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
  final Action secondNoticed= action("secondNoticed",
    on("ubuntu_gnome", () -> waitAfter(val(10000))),
    on("kubuntu_plasma", () -> waitAfter(val(null))),
    on("windows", () -> waitAfter(val(null))),
    on("opensuse_plasma", () -> waitAfter(val(null))),
    on("debian_cinnamon", () -> waitAfter(val(null))),
    on("lubuntu_lxqt", () -> waitAfter(val(null))),
    on("fedora_gnome", () -> waitAfter(val(null))),
    on("debian_gnome_x11", () -> waitAfter(val(null))),
    on("fedora_cosmic", () -> waitAfter(val(null))),
    on("debian_mate", () -> waitAfter(val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(12500))),
    on("arch_sway", () -> waitAfter(val(null))),
    on("void_i3", () -> waitAfter(val(null))),
    on("omarchy_hyprland", () -> waitAfter(val(null))));
  final Action check= action("check",
    on("ubuntu_gnome", () -> waitClick(val(0), val(527), val(113))),
    on("kubuntu_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("windows", () -> waitClick(val(null), val(null), val(null))),
    on("opensuse_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("debian_cinnamon", () -> waitClick(val(null), val(null), val(null))),
    on("lubuntu_lxqt", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitClick(val(null), val(null), val(null))),
    on("debian_gnome_x11", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitClick(val(null), val(null), val(null))),
    on("debian_mate", () -> waitClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(478), val(93))),
    on("arch_sway", () -> waitClick(val(null), val(null), val(null))),
    on("void_i3", () -> waitClick(val(null), val(null), val(null))),
    on("omarchy_hyprland", () -> waitClick(val(null), val(null), val(null))));
  final Action checked= action("checked",
    on("ubuntu_gnome", () -> waitAfter(val(4500))),
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
  final Action secondGone= action("secondGone",
    on("ubuntu_gnome", () -> waitAfter(val(10000))),
    on("kubuntu_plasma", () -> waitAfter(val(null))),
    on("windows", () -> waitAfter(val(null))),
    on("opensuse_plasma", () -> waitAfter(val(null))),
    on("debian_cinnamon", () -> waitAfter(val(null))),
    on("lubuntu_lxqt", () -> waitAfter(val(null))),
    on("fedora_gnome", () -> waitAfter(val(null))),
    on("debian_gnome_x11", () -> waitAfter(val(null))),
    on("fedora_cosmic", () -> waitAfter(val(null))),
    on("debian_mate", () -> waitAfter(val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(12500))),
    on("arch_sway", () -> waitAfter(val(null))),
    on("void_i3", () -> waitAfter(val(null))),
    on("omarchy_hyprland", () -> waitAfter(val(null))));
  final Action checkAgain= action("checkAgain",
    on("ubuntu_gnome", () -> waitClick(val(0), val(527), val(113))),
    on("kubuntu_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("windows", () -> waitClick(val(null), val(null), val(null))),
    on("opensuse_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("debian_cinnamon", () -> waitClick(val(null), val(null), val(null))),
    on("lubuntu_lxqt", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitClick(val(null), val(null), val(null))),
    on("debian_gnome_x11", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitClick(val(null), val(null), val(null))),
    on("debian_mate", () -> waitClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(478), val(93))),
    on("arch_sway", () -> waitClick(val(null), val(null), val(null))),
    on("void_i3", () -> waitClick(val(null), val(null), val(null))),
    on("omarchy_hyprland", () -> waitClick(val(null), val(null), val(null))));
  final Action checkedAgain= action("checkedAgain",
    on("ubuntu_gnome", () -> waitAfter(val(3500))),
    on("kubuntu_plasma", () -> waitAfter(val(null))),
    on("windows", () -> waitAfter(val(null))),
    on("opensuse_plasma", () -> waitAfter(val(null))),
    on("debian_cinnamon", () -> waitAfter(val(null))),
    on("lubuntu_lxqt", () -> waitAfter(val(null))),
    on("fedora_gnome", () -> waitAfter(val(null))),
    on("debian_gnome_x11", () -> waitAfter(val(null))),
    on("fedora_cosmic", () -> waitAfter(val(null))),
    on("debian_mate", () -> waitAfter(val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(4000))),
    on("arch_sway", () -> waitAfter(val(null))),
    on("void_i3", () -> waitAfter(val(null))),
    on("omarchy_hyprland", () -> waitAfter(val(null))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome", () -> waitAfter(val(7000))),
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
    var twice= files.resolve("twice");
    var second= twice.resolve("other.fearless");
    Fs.writeUtf8(twice.resolve("twice.fearless"), "\n");
    Fs.writeUtf8(twice.resolve("_twice").resolve("_rank_app.fear"), "use base.Main as Main;\n\nHello:Main{s->base.Debug#(\"twice\")}\n");
    var bare= look();
    start();
    managerShown.go();
    toFiles();
    openTwice.go();
    openTwiceMarker.go();
    projectShown.go();
    var valid= look();
    Fs.writeUtf8(second, "\n");
    stabilize();
    secondNoticed.go();
    tileArea.go();
    var tile= region;
    changed(valid, tile);
    check.go();
    checked.go();
    outputArea.go();
    var output= region;
    changed(valid, output);
    stabilize();
    var error= files.resolve("twice/.fearless_out/check_error.txt");
    checkContent(error, "More than one .fearless marker file was found in[###]keep only \"twice.fearless\".");
    var invalid= look();
    Files.delete(second);
    stabilize();
    secondGone.go();
    same(valid, tile);
    checkAgain.go();
    checkedAgain.go();
    changed(invalid, output);
    stabilize();
    assertFalse(Files.exists(error));
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    closeFiles();
    same(bare, tile);
  }
}

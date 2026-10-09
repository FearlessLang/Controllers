package agentTools;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.util.List;

import tools.Fs;

final class KindResetTest extends ManagerTest{
  static final String remembered= """
    {
      "hello_world": {
        "path": "Str:%s",
        "kind": "%s"
      }
    }
    """;
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(2000))),
    on("debian_gnome_x11",()->waitUntilTime(val(2000))),
    on("xubuntu_xfce",()->waitUntilTime(val(2000))),
    on("debian_cinnamon",()->waitUntilTime(val(2000))),
    on("debian_mate",()->waitUntilTime(val(2000))),
    on("lubuntu_lxqt",()->waitUntilTime(val(2000))),
    on("kubuntu_plasma",()->waitUntilTime(val(2000))),
    on("void_i3",()->waitUntilTime(val(2000))),
    on("arch_sway",()->waitUntilTime(val(2000))),
    on("omarchy_hyprland",()->waitUntilTime(val(2000))),
    on("fedora_gnome",()->waitUntilTime(val(2000))),
    on("opensuse_plasma",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(5500))),
    on("debian_gnome_x11",()->waitUntilTime(val(6500))),
    on("xubuntu_xfce",()->waitUntilTime(val(6500))),
    on("debian_cinnamon",()->waitUntilTime(val(6500))),
    on("debian_mate",()->waitUntilTime(val(6500))),
    on("lubuntu_lxqt",()->waitUntilTime(val(7000))),
    on("kubuntu_plasma",()->waitUntilTime(val(7000))),
    on("void_i3",()->waitUntilTime(val(6500))),
    on("arch_sway",()->waitUntilTime(val(6500))),
    on("omarchy_hyprland",()->waitUntilTime(val(6500))),
    on("fedora_gnome",()->waitUntilTime(val(6500))),
    on("opensuse_plasma",()->waitUntilTime(val(7000))));
  final Action noteShown= action("noteShown",
    on("ubuntu_gnome",()->waitUntilTime(val(19000))),
    on("debian_gnome_x11",()->waitUntilTime(val(20000))),
    on("xubuntu_xfce",()->waitUntilTime(val(20000))),
    on("debian_cinnamon",()->waitUntilTime(val(20000))),
    on("debian_mate",()->waitUntilTime(val(20000))),
    on("lubuntu_lxqt",()->waitUntilTime(val(21000))),
    on("kubuntu_plasma",()->waitUntilTime(val(21000))),
    on("void_i3",()->waitUntilTime(val(32000))),
    on("arch_sway",()->waitUntilTime(val(20000))),
    on("omarchy_hyprland",()->waitUntilTime(val(20000))),
    on("fedora_gnome",()->waitUntilTime(val(20000))),
    on("opensuse_plasma",()->waitUntilTime(val(21000))));
  final Action ok= action("ok",
    on("ubuntu_gnome",()->click(val(1952),val(1132))),
    on("debian_gnome_x11",()->click(val(1920),val(1132))),
    on("xubuntu_xfce",()->click(val(1918),val(1172))),
    on("debian_cinnamon",()->click(val(1920),val(1096))),
    on("debian_mate",()->click(val(1910),val(1152))),
    on("lubuntu_lxqt",()->click(val(1918),val(1096))),
    on("kubuntu_plasma",()->click(val(1915),val(1083))),
    on("void_i3",()->click(val(1907),val(1068))),
    on("arch_sway",()->click(val(1917),val(1148))),
    on("omarchy_hyprland",()->click(val(1910),val(1156))),
    on("fedora_gnome",()->click(val(1920),val(1132))),
    on("opensuse_plasma",()->click(val(1914),val(1088))),
    on("windows",()->click(val(639),val(367))));
  final Action managerMenu= action("managerMenu",
    on("ubuntu_gnome",()->click(val(98),val(79))),
    on("debian_gnome_x11",()->click(val(32),val(80))),
    on("xubuntu_xfce",()->click(val(74),val(120))),
    on("debian_cinnamon",()->click(val(32),val(47))),
    on("debian_mate",()->click(val(64),val(138))),
    on("lubuntu_lxqt",()->click(val(24),val(300))),
    on("kubuntu_plasma",()->click(val(30),val(39))),
    on("void_i3",()->click(val(78),val(54))),
    on("arch_sway",()->click(val(78),val(129))),
    on("omarchy_hyprland",()->click(val(146),val(112))),
    on("fedora_gnome",()->click(val(32),val(80))),
    on("opensuse_plasma",()->click(val(30),val(39))),
    on("windows",()->click(val(31),val(33))));
  final Action quitManager= action("quitManager",
    on("ubuntu_gnome",()->click(val(116),val(212))),
    on("debian_gnome_x11",()->click(val(60),val(212))),
    on("xubuntu_xfce",()->click(val(110),val(362))),
    on("debian_cinnamon",()->click(val(60),val(180))),
    on("debian_mate",()->click(val(110),val(405))),
    on("lubuntu_lxqt",()->click(val(40),val(400))),
    on("kubuntu_plasma",()->click(val(60),val(171))),
    on("void_i3",()->click(val(115),val(297))),
    on("arch_sway",()->click(val(115),val(371))),
    on("omarchy_hyprland",()->click(val(206),val(596))),
    on("fedora_gnome",()->click(val(60),val(212))),
    on("opensuse_plasma",()->click(val(60),val(171))),
    on("windows",()->click(val(61),val(167))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",()->waitUntilTime(val(25000))),
    on("debian_gnome_x11",()->waitUntilTime(val(26000))),
    on("xubuntu_xfce",()->waitUntilTime(val(26000))),
    on("debian_cinnamon",()->waitUntilTime(val(26000))),
    on("debian_mate",()->waitUntilTime(val(26000))),
    on("lubuntu_lxqt",()->waitUntilTime(val(27000))),
    on("kubuntu_plasma",()->waitUntilTime(val(27000))),
    on("void_i3",()->waitUntilTime(val(38000))),
    on("arch_sway",()->waitUntilTime(val(26000))),
    on("omarchy_hyprland",()->waitUntilTime(val(26000))),
    on("fedora_gnome",()->waitUntilTime(val(26000))),
    on("opensuse_plasma",()->waitUntilTime(val(29000))));
  final Action appsShownAgain= action("appsShownAgain",
    on("ubuntu_gnome",()->waitUntilTime(val(26500))),
    on("debian_gnome_x11",()->waitUntilTime(val(27500))),
    on("xubuntu_xfce",()->waitUntilTime(val(27500))),
    on("debian_cinnamon",()->waitUntilTime(val(27500))),
    on("debian_mate",()->waitUntilTime(val(27500))),
    on("lubuntu_lxqt",()->waitUntilTime(val(28500))),
    on("kubuntu_plasma",()->waitUntilTime(val(28500))),
    on("void_i3",()->waitUntilTime(val(39500))),
    on("arch_sway",()->waitUntilTime(val(27500))),
    on("omarchy_hyprland",()->waitUntilTime(val(27500))),
    on("fedora_gnome",()->waitUntilTime(val(34000))),
    on("opensuse_plasma",()->waitUntilTime(val(33000))));
  final Action terminalShownAgain= action("terminalShownAgain",
    on("ubuntu_gnome",()->waitUntilTime(val(30000))),
    on("debian_gnome_x11",()->waitUntilTime(val(32000))),
    on("xubuntu_xfce",()->waitUntilTime(val(32000))),
    on("debian_cinnamon",()->waitUntilTime(val(32000))),
    on("debian_mate",()->waitUntilTime(val(32000))),
    on("lubuntu_lxqt",()->waitUntilTime(val(33500))),
    on("kubuntu_plasma",()->waitUntilTime(val(33500))),
    on("void_i3",()->waitUntilTime(val(46000))),
    on("arch_sway",()->waitUntilTime(val(32000))),
    on("omarchy_hyprland",()->waitUntilTime(val(32000))),
    on("fedora_gnome",()->waitUntilTime(val(40000))),
    on("opensuse_plasma",()->waitUntilTime(val(39000))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(43000))),
    on("debian_gnome_x11",()->waitUntilTime(val(46000))),
    on("xubuntu_xfce",()->waitUntilTime(val(46000))),
    on("debian_cinnamon",()->waitUntilTime(val(46000))),
    on("debian_mate",()->waitUntilTime(val(46000))),
    on("lubuntu_lxqt",()->waitUntilTime(val(47000))),
    on("kubuntu_plasma",()->waitUntilTime(val(47000))),
    on("void_i3",()->waitUntilTime(val(70000))),
    on("arch_sway",()->waitUntilTime(val(46000))),
    on("omarchy_hyprland",()->waitUntilTime(val(46000))),
    on("fedora_gnome",()->waitUntilTime(val(56000))),
    on("opensuse_plasma",()->waitUntilTime(val(53000))));
  final Action appsShownToEnd= action("appsShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(44500))),
    on("debian_gnome_x11",()->waitUntilTime(val(47500))),
    on("xubuntu_xfce",()->waitUntilTime(val(47500))),
    on("debian_cinnamon",()->waitUntilTime(val(47500))),
    on("debian_mate",()->waitUntilTime(val(47500))),
    on("lubuntu_lxqt",()->waitUntilTime(val(48500))),
    on("kubuntu_plasma",()->waitUntilTime(val(48500))),
    on("void_i3",()->waitUntilTime(val(71500))),
    on("arch_sway",()->waitUntilTime(val(47500))),
    on("omarchy_hyprland",()->waitUntilTime(val(47500))),
    on("fedora_gnome",()->waitUntilTime(val(64000))),
    on("opensuse_plasma",()->waitUntilTime(val(55000))));
  final Action terminalShownToEnd= action("terminalShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(48000))),
    on("debian_gnome_x11",()->waitUntilTime(val(52000))),
    on("xubuntu_xfce",()->waitUntilTime(val(52000))),
    on("debian_cinnamon",()->waitUntilTime(val(52000))),
    on("debian_mate",()->waitUntilTime(val(52000))),
    on("lubuntu_lxqt",()->waitUntilTime(val(53500))),
    on("kubuntu_plasma",()->waitUntilTime(val(53500))),
    on("void_i3",()->waitUntilTime(val(78000))),
    on("arch_sway",()->waitUntilTime(val(52000))),
    on("omarchy_hyprland",()->waitUntilTime(val(52000))),
    on("fedora_gnome",()->waitUntilTime(val(70000))),
    on("opensuse_plasma",()->waitUntilTime(val(61000))));
  final Action managerEnded= action("managerEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(58000))),
    on("debian_gnome_x11",()->waitUntilTime(val(62000))),
    on("xubuntu_xfce",()->waitUntilTime(val(62000))),
    on("debian_cinnamon",()->waitUntilTime(val(62000))),
    on("debian_mate",()->waitUntilTime(val(62000))),
    on("lubuntu_lxqt",()->waitUntilTime(val(64000))),
    on("kubuntu_plasma",()->waitUntilTime(val(64000))),
    on("void_i3",()->waitUntilTime(val(90000))),
    on("arch_sway",()->waitUntilTime(val(64000))),
    on("omarchy_hyprland",()->waitUntilTime(val(64000))),
    on("fedora_gnome",()->waitUntilTime(val(82000))),
    on("opensuse_plasma",()->waitUntilTime(val(75000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var project= project("helloWorld");
    var cache= project.resolve(".fearless_out");
    Fs.writeUtf8(filesIOFolder.resolve(data).resolve("projects.info"),remembered.formatted(slashed(project),"cooked"));
    Fs.writeUtf8(cache.resolve("_map.json"),"{}\n");
    launchScript("first");
    runInTerminal(appsShown,terminalShown);
    noteShown.go();
    stabilize();
    checkContent(notes,"In projects.info the \"kind\" of \"hello_world\" was missing or not one of the kinds: \"hello_world\" is now idle, and its compiled cache is deleted.\n");
    var idle= remembered.formatted(slashed(project),"idle");
    checkContent(info,idle);
    assertFalse(Files.exists(cache));
    ok.go();
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
    launchScript("second");
    runInTerminal(appsShownAgain,terminalShownAgain);
    managerShown.go();
    stabilize();
    checkContent(notes,"");
    checkContent(info,idle);
    endScript();
    runInTerminal(appsShownToEnd,terminalShownToEnd);
    managerEnded.go();
    stabilize();
    checkContent(List.of("second.exit"),"137\n");
  }
}

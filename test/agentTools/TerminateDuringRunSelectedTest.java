package agentTools;

import tools.Fs;

final class TerminateDuringRunSelectedTest extends ManagerTest{
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
  final Action runningArea= action("runningArea",
    on("ubuntu_gnome", () -> region(val(410), val(220), val(1000), val(34))),
    on("kubuntu_plasma", () -> region(val(344), val(179), val(1000), val(34))),
    on("windows", () -> region(val(null), val(null), val(null), val(null))),
    on("opensuse_plasma", () -> region(val(344), val(179), val(1000), val(34))),
    on("debian_cinnamon", () -> region(val(344), val(188), val(1000), val(34))),
    on("lubuntu_lxqt", () -> region(val(344), val(181), val(1000), val(34))),
    on("fedora_gnome", () -> region(val(344), val(220), val(1000), val(34))),
    on("debian_gnome_x11", () -> region(val(344), val(220), val(1000), val(34))),
    on("fedora_cosmic", () -> region(val(344), val(219), val(1000), val(34))),
    on("debian_mate", () -> region(val(344), val(210), val(1000), val(34))),
    on("xubuntu_xfce", () -> region(val(343), val(200), val(1000), val(34))),
    on("arch_sway", () -> region(val(1306), val(204), val(1000), val(34))),
    on("void_i3", () -> region(val(1306), val(167), val(1000), val(34))),
    on("omarchy_hyprland", () -> region(val(1311), val(187), val(1000), val(34))));
  final Action programArea= action("programArea",
    on("ubuntu_gnome", () -> region(val(920), val(510), val(150), val(94))),
    on("kubuntu_plasma", () -> region(val(null), val(null), val(null), val(null))),
    on("windows", () -> region(val(null), val(null), val(null), val(null))),
    on("opensuse_plasma", () -> region(val(null), val(null), val(null), val(null))),
    on("debian_cinnamon", () -> region(val(null), val(null), val(null), val(null))),
    on("lubuntu_lxqt", () -> region(val(null), val(null), val(null), val(null))),
    on("fedora_gnome", () -> region(val(null), val(null), val(null), val(null))),
    on("debian_gnome_x11", () -> region(val(null), val(null), val(null), val(null))),
    on("fedora_cosmic", () -> region(val(null), val(null), val(null), val(null))),
    on("debian_mate", () -> region(val(null), val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> region(val(920), val(510), val(150), val(94))),
    on("arch_sway", () -> region(val(null), val(null), val(null), val(null))),
    on("void_i3", () -> region(val(null), val(null), val(null), val(null))),
    on("omarchy_hyprland", () -> region(val(null), val(null), val(null), val(null))));
  final Action becomeCode= action("becomeCode",
    on("ubuntu_gnome", () -> waitClick(val(0), val(975), val(113))),
    on("kubuntu_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("windows", () -> waitClick(val(null), val(null), val(null))),
    on("opensuse_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("debian_cinnamon", () -> waitClick(val(null), val(null), val(null))),
    on("lubuntu_lxqt", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitClick(val(null), val(null), val(null))),
    on("debian_gnome_x11", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitClick(val(null), val(null), val(null))),
    on("debian_mate", () -> waitClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(980), val(93))),
    on("arch_sway", () -> waitClick(val(null), val(null), val(null))),
    on("void_i3", () -> waitClick(val(null), val(null), val(null))),
    on("omarchy_hyprland", () -> waitClick(val(null), val(null), val(null))));
  final Action openChain= action("openChain",
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
  final Action openChainMarker= action("openChainMarker",
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
  final Action codeShown= action("codeShown",
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
  final Action compile= action("compile",
    on("ubuntu_gnome", () -> waitClick(val(0), val(528), val(113))),
    on("kubuntu_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("windows", () -> waitClick(val(null), val(null), val(null))),
    on("opensuse_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("debian_cinnamon", () -> waitClick(val(null), val(null), val(null))),
    on("lubuntu_lxqt", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitClick(val(null), val(null), val(null))),
    on("debian_gnome_x11", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitClick(val(null), val(null), val(null))),
    on("debian_mate", () -> waitClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(486), val(93))),
    on("arch_sway", () -> waitClick(val(null), val(null), val(null))),
    on("void_i3", () -> waitClick(val(null), val(null), val(null))),
    on("omarchy_hyprland", () -> waitClick(val(null), val(null), val(null))));
  final Action compiled= action("compiled",
    on("ubuntu_gnome", () -> waitAfter(val(21000))),
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
  final Action runV= action("runV",
    on("ubuntu_gnome", () -> waitClick(val(0), val(528), val(113))),
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
  final Action mainsShown= action("mainsShown",
    on("ubuntu_gnome", () -> waitAfter(val(3000))),
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
  final Action tickShow= action("tickShow",
    on("ubuntu_gnome", () -> waitClick(val(0), val(522), val(139))),
    on("kubuntu_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("windows", () -> waitClick(val(null), val(null), val(null))),
    on("opensuse_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("debian_cinnamon", () -> waitClick(val(null), val(null), val(null))),
    on("lubuntu_lxqt", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitClick(val(null), val(null), val(null))),
    on("debian_gnome_x11", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitClick(val(null), val(null), val(null))),
    on("debian_mate", () -> waitClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(453), val(118))),
    on("arch_sway", () -> waitClick(val(null), val(null), val(null))),
    on("void_i3", () -> waitClick(val(null), val(null), val(null))),
    on("omarchy_hyprland", () -> waitClick(val(null), val(null), val(null))));
  final Action tickTell= action("tickTell",
    on("ubuntu_gnome", () -> waitClick(val(0), val(522), val(164))),
    on("kubuntu_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("windows", () -> waitClick(val(null), val(null), val(null))),
    on("opensuse_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("debian_cinnamon", () -> waitClick(val(null), val(null), val(null))),
    on("lubuntu_lxqt", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitClick(val(null), val(null), val(null))),
    on("debian_gnome_x11", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitClick(val(null), val(null), val(null))),
    on("debian_mate", () -> waitClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(453), val(141))),
    on("arch_sway", () -> waitClick(val(null), val(null), val(null))),
    on("void_i3", () -> waitClick(val(null), val(null), val(null))),
    on("omarchy_hyprland", () -> waitClick(val(null), val(null), val(null))));
  final Action runTicked= action("runTicked",
    on("ubuntu_gnome", () -> waitClick(val(0), val(531), val(188))),
    on("kubuntu_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("windows", () -> waitClick(val(null), val(null), val(null))),
    on("opensuse_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("debian_cinnamon", () -> waitClick(val(null), val(null), val(null))),
    on("lubuntu_lxqt", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitClick(val(null), val(null), val(null))),
    on("debian_gnome_x11", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitClick(val(null), val(null), val(null))),
    on("debian_mate", () -> waitClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(464), val(163))),
    on("arch_sway", () -> waitClick(val(null), val(null), val(null))),
    on("void_i3", () -> waitClick(val(null), val(null), val(null))),
    on("omarchy_hyprland", () -> waitClick(val(null), val(null), val(null))));
  final Action programShown= action("programShown",
    on("ubuntu_gnome", () -> waitAfter(val(10500))),
    on("kubuntu_plasma", () -> waitAfter(val(null))),
    on("windows", () -> waitAfter(val(null))),
    on("opensuse_plasma", () -> waitAfter(val(null))),
    on("debian_cinnamon", () -> waitAfter(val(null))),
    on("lubuntu_lxqt", () -> waitAfter(val(null))),
    on("fedora_gnome", () -> waitAfter(val(null))),
    on("debian_gnome_x11", () -> waitAfter(val(null))),
    on("fedora_cosmic", () -> waitAfter(val(null))),
    on("debian_mate", () -> waitAfter(val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(13000))),
    on("arch_sway", () -> waitAfter(val(null))),
    on("void_i3", () -> waitAfter(val(null))),
    on("omarchy_hyprland", () -> waitAfter(val(null))));
  final Action terminate= action("terminate",
    on("ubuntu_gnome", () -> waitClick(val(0), val(528), val(113))),
    on("kubuntu_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("windows", () -> waitClick(val(null), val(null), val(null))),
    on("opensuse_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("debian_cinnamon", () -> waitClick(val(null), val(null), val(null))),
    on("lubuntu_lxqt", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitClick(val(null), val(null), val(null))),
    on("debian_gnome_x11", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitClick(val(null), val(null), val(null))),
    on("debian_mate", () -> waitClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(494), val(93))),
    on("arch_sway", () -> waitClick(val(null), val(null), val(null))),
    on("void_i3", () -> waitClick(val(null), val(null), val(null))),
    on("omarchy_hyprland", () -> waitClick(val(null), val(null), val(null))));
  final Action terminated= action("terminated",
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
    on("xubuntu_xfce", () -> waitAfter(val(5500))),
    on("arch_sway", () -> waitAfter(val(null))),
    on("void_i3", () -> waitAfter(val(null))),
    on("omarchy_hyprland", () -> waitAfter(val(null))));
  final Action tellWouldHaveRun= action("tellWouldHaveRun",
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
    var chain= files.resolve("chain");
    Fs.writeUtf8(chain.resolve("chain.fearless"), "\n");
    Fs.writeUtf8(chain.resolve("_chain").resolve("_rank_app.fear"), """
      use base.Main as Main;
      use base.Consumer as Consumer;
      use base.Frame as Frame;
      Show: Main{sys -> sys.gui.run Note}
      Tell: Main{sys -> sys.out.println("told")}
      Note: Consumer[mut Frame]{:: .title "chain" .content{:: .label{:: .text "terminate this program" } } }
      """);
    var bare= look();
    start();
    managerShown.go();
    toFiles();
    openChain.go();
    openChainMarker.go();
    projectShown.go();
    becomeCode.go();
    codeShown.go();
    compile.go();
    compiled.go();
    var before= look();
    runV.go();
    mainsShown.go();
    tickShow.go();
    tickTell.go();
    runTicked.go();
    programShown.go();
    programArea.go();
    var program= region;
    changed(before, program);
    var running= look();
    terminate.go();
    terminated.go();
    same(before, program);
    stabilize();
    checkContent(info, """
      {
        "projects": {
          "chain": {
            "path": "Str:%s",
            "kind": "code",
            "mains": ["chain.Show", "chain.Tell"]
          }
        },
        "systemExtensions": []
      }
      """.formatted(slashed(chain)));
    outputArea.go();
    var output= region;
    assert differing(running, look(), output) > 0;
    tellWouldHaveRun.go();
    runningArea.go();
    same(running, region);
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    closeFiles();
    same(bare, output);
  }
}

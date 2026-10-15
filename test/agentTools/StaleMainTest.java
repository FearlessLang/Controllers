package agentTools;

import tools.Fs;

final class StaleMainTest extends ManagerTest{
  final Action buttonArea= action("buttonArea",
    on("ubuntu_gnome", () -> region(val(517), val(102), val(30), val(20))),
    on("kubuntu_plasma", () -> region(val(451), val(61), val(30), val(20))),
    on("windows", () -> region(val(null), val(null), val(null), val(null))),
    on("opensuse_plasma", () -> region(val(451), val(61), val(30), val(20))),
    on("debian_cinnamon", () -> region(val(451), val(70), val(30), val(20))),
    on("lubuntu_lxqt", () -> region(val(451), val(63), val(30), val(20))),
    on("fedora_gnome", () -> region(val(451), val(102), val(30), val(20))),
    on("debian_gnome_x11", () -> region(val(451), val(102), val(30), val(20))),
    on("fedora_cosmic", () -> region(val(451), val(101), val(30), val(20))),
    on("debian_mate", () -> region(val(451), val(92), val(30), val(20))),
    on("xubuntu_xfce", () -> region(val(450), val(82), val(30), val(20))),
    on("arch_sway", () -> region(val(1413), val(86), val(30), val(20))),
    on("void_i3", () -> region(val(1413), val(49), val(30), val(20))),
    on("omarchy_hyprland", () -> region(val(1418), val(69), val(30), val(20))));
  final Action becomeCode= action("becomeCode",
    on("ubuntu_gnome", () -> waitClick(val(0), val(979), val(113))),
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
  static final String mains= "use base.Main as Main;\n\nFirst: Main{sys -> sys.out.println \"First\"}\nSecond: Main{sys -> sys.out.println \"Second\"}\n";
  static final String registry= """
    {
      "projects": {
        "menu": {
          "path": "Str:%s",
          "kind": "code",
          "mains": [%s]
        }
      },
      "systemExtensions": []
    }
    """;
  final Action openMenu= action("openMenu",
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
  final Action openMenuMarker= action("openMenuMarker",
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
    on("ubuntu_gnome", () -> waitClick(val(0), val(532), val(113))),
    on("kubuntu_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("windows", () -> waitClick(val(null), val(null), val(null))),
    on("opensuse_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("debian_cinnamon", () -> waitClick(val(null), val(null), val(null))),
    on("lubuntu_lxqt", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitClick(val(null), val(null), val(null))),
    on("debian_gnome_x11", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitClick(val(null), val(null), val(null))),
    on("debian_mate", () -> waitClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(487), val(93))),
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
    on("ubuntu_gnome", () -> waitClick(val(0), val(532), val(113))),
    on("kubuntu_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("windows", () -> waitClick(val(null), val(null), val(null))),
    on("opensuse_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("debian_cinnamon", () -> waitClick(val(null), val(null), val(null))),
    on("lubuntu_lxqt", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitClick(val(null), val(null), val(null))),
    on("debian_gnome_x11", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitClick(val(null), val(null), val(null))),
    on("debian_mate", () -> waitClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(480), val(93))),
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
  final Action tickSecond= action("tickSecond",
    on("ubuntu_gnome", () -> waitClick(val(0), val(526), val(164))),
    on("kubuntu_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("windows", () -> waitClick(val(null), val(null), val(null))),
    on("opensuse_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("debian_cinnamon", () -> waitClick(val(null), val(null), val(null))),
    on("lubuntu_lxqt", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitClick(val(null), val(null), val(null))),
    on("debian_gnome_x11", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitClick(val(null), val(null), val(null))),
    on("debian_mate", () -> waitClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(454), val(141))),
    on("arch_sway", () -> waitClick(val(null), val(null), val(null))),
    on("void_i3", () -> waitClick(val(null), val(null), val(null))),
    on("omarchy_hyprland", () -> waitClick(val(null), val(null), val(null))));
  final Action tickThird= action("tickThird",
    on("ubuntu_gnome", () -> waitClick(val(0), val(526), val(189))),
    on("kubuntu_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("windows", () -> waitClick(val(null), val(null), val(null))),
    on("opensuse_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("debian_cinnamon", () -> waitClick(val(null), val(null), val(null))),
    on("lubuntu_lxqt", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitClick(val(null), val(null), val(null))),
    on("debian_gnome_x11", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitClick(val(null), val(null), val(null))),
    on("debian_mate", () -> waitClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(454), val(164))),
    on("arch_sway", () -> waitClick(val(null), val(null), val(null))),
    on("void_i3", () -> waitClick(val(null), val(null), val(null))),
    on("omarchy_hyprland", () -> waitClick(val(null), val(null), val(null))));
  final Action runTicked= action("runTicked",
    on("ubuntu_gnome", () -> waitClick(val(0), val(535), val(213))),
    on("kubuntu_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("windows", () -> waitClick(val(null), val(null), val(null))),
    on("opensuse_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("debian_cinnamon", () -> waitClick(val(null), val(null), val(null))),
    on("lubuntu_lxqt", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitClick(val(null), val(null), val(null))),
    on("debian_gnome_x11", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitClick(val(null), val(null), val(null))),
    on("debian_mate", () -> waitClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(466), val(186))),
    on("arch_sway", () -> waitClick(val(null), val(null), val(null))),
    on("void_i3", () -> waitClick(val(null), val(null), val(null))),
    on("omarchy_hyprland", () -> waitClick(val(null), val(null), val(null))));
  final Action runsDone= action("runsDone",
    on("ubuntu_gnome", () -> waitAfter(val(9000))),
    on("kubuntu_plasma", () -> waitAfter(val(null))),
    on("windows", () -> waitAfter(val(null))),
    on("opensuse_plasma", () -> waitAfter(val(null))),
    on("debian_cinnamon", () -> waitAfter(val(null))),
    on("lubuntu_lxqt", () -> waitAfter(val(null))),
    on("fedora_gnome", () -> waitAfter(val(null))),
    on("debian_gnome_x11", () -> waitAfter(val(null))),
    on("fedora_cosmic", () -> waitAfter(val(null))),
    on("debian_mate", () -> waitAfter(val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(11000))),
    on("arch_sway", () -> waitAfter(val(null))),
    on("void_i3", () -> waitAfter(val(null))),
    on("omarchy_hyprland", () -> waitAfter(val(null))));
  final Action editNoticed= action("editNoticed",
    on("ubuntu_gnome", () -> waitAfter(val(11000))),
    on("kubuntu_plasma", () -> waitAfter(val(null))),
    on("windows", () -> waitAfter(val(null))),
    on("opensuse_plasma", () -> waitAfter(val(null))),
    on("debian_cinnamon", () -> waitAfter(val(null))),
    on("lubuntu_lxqt", () -> waitAfter(val(null))),
    on("fedora_gnome", () -> waitAfter(val(null))),
    on("debian_gnome_x11", () -> waitAfter(val(null))),
    on("fedora_cosmic", () -> waitAfter(val(null))),
    on("debian_mate", () -> waitAfter(val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(14000))),
    on("arch_sway", () -> waitAfter(val(null))),
    on("void_i3", () -> waitAfter(val(null))),
    on("omarchy_hyprland", () -> waitAfter(val(null))));
  final Action compileEdited= action("compileEdited",
    on("ubuntu_gnome", () -> waitClick(val(0), val(532), val(113))),
    on("kubuntu_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("windows", () -> waitClick(val(null), val(null), val(null))),
    on("opensuse_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("debian_cinnamon", () -> waitClick(val(null), val(null), val(null))),
    on("lubuntu_lxqt", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitClick(val(null), val(null), val(null))),
    on("debian_gnome_x11", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitClick(val(null), val(null), val(null))),
    on("debian_mate", () -> waitClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(487), val(93))),
    on("arch_sway", () -> waitClick(val(null), val(null), val(null))),
    on("void_i3", () -> waitClick(val(null), val(null), val(null))),
    on("omarchy_hyprland", () -> waitClick(val(null), val(null), val(null))));
  final Action editedCompiled= action("editedCompiled",
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
    var menu= files.resolve("menu");
    var source= menu.resolve("_menu").resolve("_rank_app.fear");
    Fs.writeUtf8(menu.resolve("menu.fearless"), "\n");
    Fs.writeUtf8(source, mains+"Third: Main{sys -> sys.out.println \"Third\"}\n");
    var bare= look();
    start();
    managerShown.go();
    toFiles();
    openMenu.go();
    openMenuMarker.go();
    projectShown.go();
    becomeCode.go();
    codeShown.go();
    compile.go();
    compiled.go();
    runV.go();
    mainsShown.go();
    tickSecond.go();
    tickThird.go();
    runTicked.go();
    runsDone.go();
    stabilize();
    checkContent(info, registry.formatted(slashed(menu), "\"menu.Second\", \"menu.Third\""));
    var runnable= look();
    Fs.writeUtf8(source, mains);
    stabilize();
    editNoticed.go();
    buttonArea.go();
    var button= region;
    changed(runnable, button);
    compileEdited.go();
    editedCompiled.go();
    same(runnable, button);
    stabilize();
    checkContent(info, registry.formatted(slashed(menu), "\"menu.Second\""));
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    closeFiles();
    same(bare, button);
  }
}

package agentTools;

final class EditMetadataAddProjectTest extends ManagerTest{
  final Action outputArea= action("outputArea",
    on("ubuntu_gnome", () -> region(val(410), val(168), val(1000), val(80))),
    on("kubuntu_plasma", () -> region(val(344), val(127), val(1000), val(80))),
    on("windows", () -> region(val(340), val(120), val(900), val(40))),
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
  final Action openHelloWorld= action("openHelloWorld",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("kubuntu_plasma", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("windows", () -> waitDoubleClick(val(1000), val(280), val(254))),//the file manager lists the folder
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
    on("windows", () -> waitDoubleClick(val(1000), val(265), val(254))),//the file manager lists the folder
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
    on("windows", () -> waitAfter(val(4000))),//the manager opens the project and shows its tile
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
  final Action editorShown= action("editorShown",
    on("ubuntu_gnome", () -> waitAfter(val(5500))),
    on("kubuntu_plasma", () -> waitAfter(val(null))),
    on("windows", () -> waitAfter(val(2000))),//the manager opens the editor window of the metadata
    on("opensuse_plasma", () -> waitAfter(val(null))),
    on("debian_cinnamon", () -> waitAfter(val(null))),
    on("lubuntu_lxqt", () -> waitAfter(val(null))),
    on("fedora_gnome", () -> waitAfter(val(null))),
    on("debian_gnome_x11", () -> waitAfter(val(null))),
    on("fedora_cosmic", () -> waitAfter(val(null))),
    on("debian_mate", () -> waitAfter(val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(7000))),
    on("arch_sway", () -> waitAfter(val(null))),
    on("void_i3", () -> waitAfter(val(null))),
    on("omarchy_hyprland", () -> waitAfter(val(null))));
  final Action focusText= action("focusText",
    on("ubuntu_gnome", () -> waitClick(val(0), val(940), val(660))),
    on("kubuntu_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("windows", () -> waitClick(val(100), val(640), val(400))),
    on("opensuse_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("debian_cinnamon", () -> waitClick(val(null), val(null), val(null))),
    on("lubuntu_lxqt", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitClick(val(null), val(null), val(null))),
    on("debian_gnome_x11", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitClick(val(null), val(null), val(null))),
    on("debian_mate", () -> waitClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(907), val(657))),
    on("arch_sway", () -> waitClick(val(null), val(null), val(null))),
    on("void_i3", () -> waitClick(val(null), val(null), val(null))),
    on("omarchy_hyprland", () -> waitClick(val(null), val(null), val(null))));
  final Action selectText= action("selectText",
    on("ubuntu_gnome", () -> waitDrag(val(null), val(null), val(null), val(null), val(null))),
    on("kubuntu_plasma", () -> waitDrag(val(null), val(null), val(null), val(null), val(null))),
    on("windows", () -> waitDrag(val(100), val(242), val(63), val(1000), val(400))),
    on("opensuse_plasma", () -> waitDrag(val(null), val(null), val(null), val(null), val(null))),
    on("debian_cinnamon", () -> waitDrag(val(null), val(null), val(null), val(null), val(null))),
    on("lubuntu_lxqt", () -> waitDrag(val(null), val(null), val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitDrag(val(null), val(null), val(null), val(null), val(null))),
    on("debian_gnome_x11", () -> waitDrag(val(null), val(null), val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitDrag(val(null), val(null), val(null), val(null), val(null))),
    on("debian_mate", () -> waitDrag(val(null), val(null), val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDrag(val(null), val(null), val(null), val(null), val(null))),
    on("arch_sway", () -> waitDrag(val(null), val(null), val(null), val(null), val(null))),
    on("void_i3", () -> waitDrag(val(null), val(null), val(null), val(null), val(null))),
    on("omarchy_hyprland", () -> waitDrag(val(null), val(null), val(null), val(null), val(null))));
  final Action commit= action("commit",
    on("ubuntu_gnome", () -> waitClick(val(0), val(1278), val(830))),
    on("kubuntu_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("windows", () -> waitClick(val(100), val(924), val(619))),
    on("opensuse_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("debian_cinnamon", () -> waitClick(val(null), val(null), val(null))),
    on("lubuntu_lxqt", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitClick(val(null), val(null), val(null))),
    on("debian_gnome_x11", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitClick(val(null), val(null), val(null))),
    on("debian_mate", () -> waitClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(1245), val(827))),
    on("arch_sway", () -> waitClick(val(null), val(null), val(null))),
    on("void_i3", () -> waitClick(val(null), val(null), val(null))),
    on("omarchy_hyprland", () -> waitClick(val(null), val(null), val(null))));
  final Action bothSaved= action("bothSaved",
    on("ubuntu_gnome", () -> waitAfter(val(56000))),
    on("kubuntu_plasma", () -> waitAfter(val(null))),
    on("windows", () -> waitAfter(val(4000))),//the manager saves the metadata and registers the project start
    on("opensuse_plasma", () -> waitAfter(val(null))),
    on("debian_cinnamon", () -> waitAfter(val(null))),
    on("lubuntu_lxqt", () -> waitAfter(val(null))),
    on("fedora_gnome", () -> waitAfter(val(null))),
    on("debian_gnome_x11", () -> waitAfter(val(null))),
    on("fedora_cosmic", () -> waitAfter(val(null))),
    on("debian_mate", () -> waitAfter(val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(70000))),
    on("arch_sway", () -> waitAfter(val(null))),
    on("void_i3", () -> waitAfter(val(null))),
    on("omarchy_hyprland", () -> waitAfter(val(null))));
  final Action secondTile= action("secondTile",
    on("ubuntu_gnome", () -> waitClick(val(0), val(261), val(130))),
    on("kubuntu_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("windows", () -> waitClick(val(100), val(193), val(88))),
    on("opensuse_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("debian_cinnamon", () -> waitClick(val(null), val(null), val(null))),
    on("lubuntu_lxqt", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitClick(val(null), val(null), val(null))),
    on("debian_gnome_x11", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitClick(val(null), val(null), val(null))),
    on("debian_mate", () -> waitClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(194), val(110))),
    on("arch_sway", () -> waitClick(val(null), val(null), val(null))),
    on("void_i3", () -> waitClick(val(null), val(null), val(null))),
    on("omarchy_hyprland", () -> waitClick(val(null), val(null), val(null))));
  final Action secondShown= action("secondShown",
    on("ubuntu_gnome", () -> waitAfter(val(3500))),
    on("kubuntu_plasma", () -> waitAfter(val(null))),
    on("windows", () -> waitAfter(val(6000))),//the manager opens the second project and shows it
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
  final Action check= action("check",
    on("ubuntu_gnome", () -> waitClick(val(0), val(523), val(113))),
    on("kubuntu_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("windows", () -> waitClick(val(100), val(459), val(67))),
    on("opensuse_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("debian_cinnamon", () -> waitClick(val(null), val(null), val(null))),
    on("lubuntu_lxqt", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitClick(val(null), val(null), val(null))),
    on("debian_gnome_x11", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitClick(val(null), val(null), val(null))),
    on("debian_mate", () -> waitClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(474), val(93))),
    on("arch_sway", () -> waitClick(val(null), val(null), val(null))),
    on("void_i3", () -> waitClick(val(null), val(null), val(null))),
    on("omarchy_hyprland", () -> waitClick(val(null), val(null), val(null))));
  final Action checked= action("checked",
    on("ubuntu_gnome", () -> waitAfter(val(4000))),
    on("kubuntu_plasma", () -> waitAfter(val(null))),
    on("windows", () -> waitAfter(val(4000))),//the Check runs and shows its output
    on("opensuse_plasma", () -> waitAfter(val(null))),
    on("debian_cinnamon", () -> waitAfter(val(null))),
    on("lubuntu_lxqt", () -> waitAfter(val(null))),
    on("fedora_gnome", () -> waitAfter(val(null))),
    on("debian_gnome_x11", () -> waitAfter(val(null))),
    on("fedora_cosmic", () -> waitAfter(val(null))),
    on("debian_mate", () -> waitAfter(val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(5000))),
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
    var other= project("helloStackTraces");
    var both= """
      {"projects": {"hello_world": {"path": "Str:%s", "kind": "idle"}, "start": {"path": "Str:%s", "kind": "idle"}}, "systemExtensions": []}
      """.formatted(slashed(project), slashed(other));
    var bare= look();
    start();
    managerShown.go();
    toFiles();
    openHelloWorld.go();
    openHelloWorldMarker.go();
    projectShown.go();
    managerMenu.go();
    editMetadata.go();
    editorShown.go();
    focusText.go();
    selectText.go();
    type(both.strip());
    commit.go();
    bothSaved.go();
    stabilize();
    checkContent(info, """
      {
        "projects": {
          "hello_world": {
            "path": "Str:%s",
            "kind": "idle"
          },
          "start": {
            "path": "Str:%s",
            "kind": "idle"
          }
        },
        "systemExtensions": []
      }
      """.formatted(slashed(project), slashed(other)));
    secondTile.go();
    secondShown.go();
    var before= look();
    check.go();
    checked.go();
    outputArea.go();
    var output= region;
    changed(before, output);
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    closeFiles();
    same(bare, output);
  }
}

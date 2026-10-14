package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.util.List;

import tools.Fs;

final class DropFolderTest extends ManagerTest{
  final Action tileListArea= action("tileListArea",
    on("ubuntu_gnome", () -> region(val(100), val(200), val(200), val(400))),
    on("kubuntu_plasma", () -> region(val(34), val(159), val(200), val(400))),
    on("opensuse_plasma", () -> region(val(34), val(159), val(200), val(400))),
    on("debian_cinnamon", () -> region(val(34), val(168), val(200), val(400))),
    on("lubuntu_lxqt", () -> region(val(34), val(161), val(200), val(400))),
    on("fedora_gnome", () -> region(val(34), val(200), val(200), val(400))),
    on("debian_gnome_x11", () -> region(val(34), val(200), val(200), val(400))),
    on("fedora_cosmic", () -> region(val(34), val(199), val(200), val(400))),
    on("debian_mate", () -> region(val(34), val(190), val(200), val(400))),
    on("xubuntu_xfce", () -> region(val(33), val(180), val(200), val(400))),
    on("arch_sway", () -> region(val(996), val(184), val(200), val(400))),
    on("void_i3", () -> region(val(996), val(147), val(200), val(400))),
    on("omarchy_hyprland", () -> region(val(1001), val(167), val(200), val(400))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
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
  final Action openDrops= action("openDrops",
    on("ubuntu_gnome", () -> waitDoubleClick(val(100), val(798), val(380))),
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
  final Action filesShown= action("filesShown",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
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
  final Action carryDropped= action("carryDropped",
    on("ubuntu_gnome", () -> waitDrag(val(0), val(798), val(380), val(300), val(600))),
    on("kubuntu_plasma", () -> waitDrag(val(null), val(null), val(null), val(null), val(null))),
    on("opensuse_plasma", () -> waitDrag(val(null), val(null), val(null), val(null), val(null))),
    on("debian_cinnamon", () -> waitDrag(val(null), val(null), val(null), val(null), val(null))),
    on("lubuntu_lxqt", () -> waitDrag(val(null), val(null), val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitDrag(val(null), val(null), val(null), val(null), val(null))),
    on("debian_gnome_x11", () -> waitDrag(val(null), val(null), val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitDrag(val(null), val(null), val(null), val(null), val(null))),
    on("debian_mate", () -> waitDrag(val(null), val(null), val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDrag(val(0), val(879), val(425), val(200), val(700))),
    on("arch_sway", () -> waitDrag(val(null), val(null), val(null), val(null), val(null))),
    on("void_i3", () -> waitDrag(val(null), val(null), val(null), val(null), val(null))),
    on("omarchy_hyprland", () -> waitDrag(val(null), val(null), val(null), val(null), val(null))));
  final Action droppedSaved= action("droppedSaved",
    on("ubuntu_gnome", () -> waitAfter(val(300))),
    on("kubuntu_plasma", () -> waitAfter(val(null))),
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
  final Action filesClosed= action("filesClosed",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("kubuntu_plasma", () -> waitAfter(val(null))),
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
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("kubuntu_plasma", () -> waitAfter(val(null))),
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
    var drops= files.resolve("drops");
    var dropped= drops.resolve("dropped");
    Fs.writeUtf8(dropped.resolve("dropped.fearless"), "\n");
    Fs.writeUtf8(dropped.resolve("_dropped").resolve("_rank_app.fear"), "use base.Main as Main;\n\nHello:Main{s->base.Debug#(\"dropped\")}\n");
    var bare= look();
    start();
    managerShown.go();
    toFiles();
    openDrops.go();
    filesShown.go();
    carryDropped.go();
    droppedSaved.go();
    stabilize();
    checkContent(info, """
      {
        "projects": {
          "dropped": {
            "path": "Str:%s",
            "kind": "idle"
          }
        },
        "systemExtensions": []
      }
      """.formatted(slashed(dropped)));
    assertEquals(List.of("dropped/_dropped/_rank_app.fear", "dropped/dropped.fearless"), Fs.walk(drops, s -> s.filter(Files::isRegularFile).map(p -> slashed(drops.relativize(p))).sorted().toList()));
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    closeFiles();
    filesClosed.go();
    tileListArea.go();
    same(bare, region);
  }
}

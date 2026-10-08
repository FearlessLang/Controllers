package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.List;

import tools.Fs;

final class ForgetProjectTest extends ManagerTest{
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(2000))),
    on("debian_gnome_x11",()->waitUntilTime(val(2000))),
    on("xubuntu_xfce",()->waitUntilTime(val(2000))),
    on("debian_cinnamon",()->waitUntilTime(val(2000))),
    on("debian_mate",()->waitUntilTime(val(2000))),
    on("lubuntu_lxqt",()->waitUntilTime(val(2000))),
    on("kubuntu_plasma",()->waitUntilTime(val(2000))),
    on("void_i3",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(5500))),
    on("debian_gnome_x11",()->waitUntilTime(val(6500))),
    on("xubuntu_xfce",()->waitUntilTime(val(6500))),
    on("debian_cinnamon",()->waitUntilTime(val(6500))),
    on("debian_mate",()->waitUntilTime(val(6500))),
    on("lubuntu_lxqt",()->waitUntilTime(val(7000))),
    on("kubuntu_plasma",()->waitUntilTime(val(7000))),
    on("void_i3",()->waitUntilTime(val(6500))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(18000))),
    on("debian_gnome_x11",()->waitUntilTime(val(20000))),
    on("xubuntu_xfce",()->waitUntilTime(val(20000))),
    on("debian_cinnamon",()->waitUntilTime(val(20000))),
    on("debian_mate",()->waitUntilTime(val(20000))),
    on("lubuntu_lxqt",()->waitUntilTime(val(21000))),
    on("kubuntu_plasma",()->waitUntilTime(val(21000))),
    on("void_i3",()->waitUntilTime(val(30000))));
  final Action appsShownOther= action("appsShownOther",
    on("ubuntu_gnome",()->waitUntilTime(val(19500))),
    on("debian_gnome_x11",()->waitUntilTime(val(22000))),
    on("xubuntu_xfce",()->waitUntilTime(val(22000))),
    on("debian_cinnamon",()->waitUntilTime(val(22000))),
    on("debian_mate",()->waitUntilTime(val(22000))),
    on("lubuntu_lxqt",()->waitUntilTime(val(23000))),
    on("kubuntu_plasma",()->waitUntilTime(val(23000))),
    on("void_i3",()->waitUntilTime(val(32000))));
  final Action terminalShownOther= action("terminalShownOther",
    on("ubuntu_gnome",()->waitUntilTime(val(23000))),
    on("debian_gnome_x11",()->waitUntilTime(val(26500))),
    on("xubuntu_xfce",()->waitUntilTime(val(26500))),
    on("debian_cinnamon",()->waitUntilTime(val(26500))),
    on("debian_mate",()->waitUntilTime(val(26500))),
    on("lubuntu_lxqt",()->waitUntilTime(val(28000))),
    on("kubuntu_plasma",()->waitUntilTime(val(28000))),
    on("void_i3",()->waitUntilTime(val(36500))));
  final Action otherShown= action("otherShown",
    on("ubuntu_gnome",()->waitUntilTime(val(34000))),
    on("debian_gnome_x11",()->waitUntilTime(val(40000))),
    on("xubuntu_xfce",()->waitUntilTime(val(40000))),
    on("debian_cinnamon",()->waitUntilTime(val(40000))),
    on("debian_mate",()->waitUntilTime(val(40000))),
    on("lubuntu_lxqt",()->waitUntilTime(val(42000))),
    on("kubuntu_plasma",()->waitUntilTime(val(42000))),
    on("void_i3",()->waitUntilTime(val(54000))));
  final Action projectMenu= action("projectMenu",
    on("ubuntu_gnome",()->click(val(158),val(79))),
    on("debian_gnome_x11",()->click(val(92),val(80))),
    on("xubuntu_xfce",()->click(val(210),val(120))),
    on("debian_cinnamon",()->click(val(92),val(47))),
    on("debian_mate",()->click(val(184),val(138))),
    on("lubuntu_lxqt",()->click(val(70),val(300))),
    on("kubuntu_plasma",()->click(val(90),val(39))),
    on("void_i3",()->click(val(215),val(54))),
    on("windows",()->click(val(89),val(33))));
  final Action forgetProject= action("forgetProject",
    on("ubuntu_gnome",()->click(val(180),val(237))),
    on("debian_gnome_x11",()->click(val(120),val(237))),
    on("xubuntu_xfce",()->click(val(260),val(409))),
    on("debian_cinnamon",()->click(val(120),val(205))),
    on("debian_mate",()->click(val(230),val(455))),
    on("lubuntu_lxqt",()->click(val(90),val(419))),
    on("kubuntu_plasma",()->click(val(120),val(196))),
    on("void_i3",()->click(val(268),val(343))),
    on("windows",()->click(val(111),val(191))));
  final Action forgotten= action("forgotten",
    on("ubuntu_gnome",()->waitUntilTime(val(37000))),
    on("debian_gnome_x11",()->waitUntilTime(val(43000))),
    on("xubuntu_xfce",()->waitUntilTime(val(43000))),
    on("debian_cinnamon",()->waitUntilTime(val(43000))),
    on("debian_mate",()->waitUntilTime(val(43000))),
    on("lubuntu_lxqt",()->waitUntilTime(val(45000))),
    on("kubuntu_plasma",()->waitUntilTime(val(45000))),
    on("void_i3",()->waitUntilTime(val(57000))));
  final Action appsShownToEnd= action("appsShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(38500))),
    on("debian_gnome_x11",()->waitUntilTime(val(45000))),
    on("xubuntu_xfce",()->waitUntilTime(val(45000))),
    on("debian_cinnamon",()->waitUntilTime(val(45000))),
    on("debian_mate",()->waitUntilTime(val(45000))),
    on("lubuntu_lxqt",()->waitUntilTime(val(47000))),
    on("kubuntu_plasma",()->waitUntilTime(val(47000))),
    on("void_i3",()->waitUntilTime(val(59000))));
  final Action terminalShownToEnd= action("terminalShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(42000))),
    on("debian_gnome_x11",()->waitUntilTime(val(49500))),
    on("xubuntu_xfce",()->waitUntilTime(val(49500))),
    on("debian_cinnamon",()->waitUntilTime(val(49500))),
    on("debian_mate",()->waitUntilTime(val(49500))),
    on("lubuntu_lxqt",()->waitUntilTime(val(52000))),
    on("kubuntu_plasma",()->waitUntilTime(val(52000))),
    on("void_i3",()->waitUntilTime(val(63500))));
  final Action managerEnded= action("managerEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(52000))),
    on("debian_gnome_x11",()->waitUntilTime(val(60000))),
    on("xubuntu_xfce",()->waitUntilTime(val(60000))),
    on("debian_cinnamon",()->waitUntilTime(val(60000))),
    on("debian_mate",()->waitUntilTime(val(60000))),
    on("lubuntu_lxqt",()->waitUntilTime(val(66000))),
    on("kubuntu_plasma",()->waitUntilTime(val(66000))),
    on("void_i3",()->waitUntilTime(val(77000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var project= project("helloWorld");
    var other= project("helloStackTraces");
    launchScript("first",project);
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    launchScript("second",other);
    runInTerminal(appsShownOther,terminalShownOther);
    otherShown.go();
    stabilize();
    var files= files(other);
    projectMenu.go();
    forgetProject.go();
    forgotten.go();
    stabilize();
    checkContent(info,"""
      {
        "hello_world": {
          "path": "Str:%s",
          "kind": "idle"
        }
      }
      """.formatted(slashed(project)));
    assertEquals(files,files(other));
    endScript();
    runInTerminal(appsShownToEnd,terminalShownToEnd);
    managerEnded.go();
    stabilize();
    checkContent(List.of("first.exit"),"137\n");
  }
  static List<String> files(Path other){ return Fs.walk(other,s->s.map(p->p+" "+Fs.lastModified(p)).toList()); }
}

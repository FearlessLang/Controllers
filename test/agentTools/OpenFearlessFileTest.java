package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.IntStream;

final class OpenFearlessFileTest extends ManagerTest{
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
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(18500))),
    on("debian_gnome_x11",()->waitUntilTime(val(20000))),
    on("xubuntu_xfce",()->waitUntilTime(val(20000))),
    on("debian_cinnamon",()->waitUntilTime(val(20000))),
    on("debian_mate",()->waitUntilTime(val(20000))),
    on("lubuntu_lxqt",()->waitUntilTime(val(21000))),
    on("kubuntu_plasma",()->waitUntilTime(val(21000))),
    on("void_i3",()->waitUntilTime(val(30000))),
    on("arch_sway",()->waitUntilTime(val(20000))),
    on("omarchy_hyprland",()->waitUntilTime(val(20000))),
    on("fedora_gnome",()->waitUntilTime(val(20000))),
    on("opensuse_plasma",()->waitUntilTime(val(21000))));
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
    on("ubuntu_gnome",()->waitUntilTime(val(23000))),
    on("debian_gnome_x11",()->waitUntilTime(val(24500))),
    on("xubuntu_xfce",()->waitUntilTime(val(24500))),
    on("debian_cinnamon",()->waitUntilTime(val(25500))),
    on("debian_mate",()->waitUntilTime(val(25500))),
    on("lubuntu_lxqt",()->waitUntilTime(val(26500))),
    on("kubuntu_plasma",()->waitUntilTime(val(26500))),
    on("void_i3",()->waitUntilTime(val(36000))),
    on("arch_sway",()->waitUntilTime(val(26000))),
    on("omarchy_hyprland",()->waitUntilTime(val(26000))),
    on("fedora_gnome",()->waitUntilTime(val(24500))),
    on("opensuse_plasma",()->waitUntilTime(val(26500))));
  final Action appsShownFiles= action("appsShownFiles",
    on("ubuntu_gnome",()->waitUntilTime(val(24500))),
    on("debian_gnome_x11",()->waitUntilTime(val(26000))),
    on("xubuntu_xfce",()->waitUntilTime(val(26000))),
    on("debian_cinnamon",()->waitUntilTime(val(27000))),
    on("debian_mate",()->waitUntilTime(val(27000))),
    on("lubuntu_lxqt",()->waitUntilTime(val(28000))),
    on("kubuntu_plasma",()->waitUntilTime(val(28000))),
    on("void_i3",()->waitUntilTime(val(38000))),
    on("arch_sway",()->waitUntilTime(val(28000))),
    on("omarchy_hyprland",()->waitUntilTime(val(28000))),
    on("fedora_gnome",()->waitUntilTime(val(33000))),
    on("opensuse_plasma",()->waitUntilTime(val(34000))));
  final Action terminalShownFiles= action("terminalShownFiles",
    on("ubuntu_gnome",()->waitUntilTime(val(28000))),
    on("debian_gnome_x11",()->waitUntilTime(val(30500))),
    on("xubuntu_xfce",()->waitUntilTime(val(30500))),
    on("debian_cinnamon",()->waitUntilTime(val(31500))),
    on("debian_mate",()->waitUntilTime(val(31500))),
    on("lubuntu_lxqt",()->waitUntilTime(val(33000))),
    on("kubuntu_plasma",()->waitUntilTime(val(33000))),
    on("void_i3",()->waitUntilTime(val(42500))),
    on("arch_sway",()->waitUntilTime(val(32500))),
    on("omarchy_hyprland",()->waitUntilTime(val(32500))),
    on("fedora_gnome",()->waitUntilTime(val(39000))),
    on("opensuse_plasma",()->waitUntilTime(val(40000))));
  final Action filesShown= action("filesShown",
    on("ubuntu_gnome",()->waitUntilTime(val(40000))),
    on("debian_gnome_x11",()->waitUntilTime(val(44000))),
    on("xubuntu_xfce",()->waitUntilTime(val(44000))),
    on("debian_cinnamon",()->waitUntilTime(val(44000))),
    on("debian_mate",()->waitUntilTime(val(44000))),
    on("lubuntu_lxqt",()->waitUntilTime(val(46000))),
    on("kubuntu_plasma",()->waitUntilTime(val(46000))),
    on("void_i3",()->waitUntilTime(val(56000))),
    on("arch_sway",()->waitUntilTime(val(46000))),
    on("omarchy_hyprland",()->waitUntilTime(val(46000))),
    on("fedora_gnome",()->waitUntilTime(val(54000))),
    on("opensuse_plasma",()->waitUntilTime(val(53000))));
  final Action openFile= action("openFile",
    on("ubuntu_gnome",()->doubleClick(val(1872),val(915))),
    on("debian_gnome_x11",()->doubleClick(val(1925),val(939))),
    on("xubuntu_xfce",()->doubleClick(val(2030),val(852))),
    on("debian_cinnamon",()->doubleClick(val(678),val(272))),
    on("debian_mate",()->doubleClick(val(1108),val(710))),
    on("lubuntu_lxqt",()->doubleClick(val(1977),val(971))),
    on("kubuntu_plasma",()->doubleClick(val(1885),val(848))),
    on("void_i3",()->doubleClick(val(754),val(246))),
    on("arch_sway",()->doubleClick(val(756),val(324))),
    on("omarchy_hyprland",()->doubleClick(val(1020),val(310))),
    on("fedora_gnome",()->doubleClick(val(1925),val(939))),
    on("opensuse_plasma",()->doubleClick(val(1894),val(849))),
    on("windows",()->doubleClick(val(503),val(254))));
  final Action managerOpened= action("managerOpened",
    on("ubuntu_gnome",()->waitUntilTime(val(52000))),
    on("debian_gnome_x11",()->waitUntilTime(val(60000))),
    on("xubuntu_xfce",()->waitUntilTime(val(60000))),
    on("debian_cinnamon",()->waitUntilTime(val(60000))),
    on("debian_mate",()->waitUntilTime(val(60000))),
    on("lubuntu_lxqt",()->waitUntilTime(val(62000))),
    on("kubuntu_plasma",()->waitUntilTime(val(62000))),
    on("void_i3",()->waitUntilTime(val(73000))),
    on("arch_sway",()->waitUntilTime(val(62000))),
    on("omarchy_hyprland",()->waitUntilTime(val(62000))),
    on("fedora_gnome",()->waitUntilTime(val(70000))),
    on("opensuse_plasma",()->waitUntilTime(val(69000))));
  final Action appsShownToEnd= action("appsShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(53500))),
    on("debian_gnome_x11",()->waitUntilTime(val(61500))),
    on("xubuntu_xfce",()->waitUntilTime(val(61500))),
    on("debian_cinnamon",()->waitUntilTime(val(61500))),
    on("debian_mate",()->waitUntilTime(val(61500))),
    on("lubuntu_lxqt",()->waitUntilTime(val(63500))),
    on("kubuntu_plasma",()->waitUntilTime(val(63500))),
    on("void_i3",()->waitUntilTime(val(75000))),
    on("arch_sway",()->waitUntilTime(val(64000))),
    on("omarchy_hyprland",()->waitUntilTime(val(64000))),
    on("fedora_gnome",()->waitUntilTime(val(77000))),
    on("opensuse_plasma",()->waitUntilTime(val(70500))));
  final Action terminalShownToEnd= action("terminalShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(57000))),
    on("debian_gnome_x11",()->waitUntilTime(val(66000))),
    on("xubuntu_xfce",()->waitUntilTime(val(66000))),
    on("debian_cinnamon",()->waitUntilTime(val(66000))),
    on("debian_mate",()->waitUntilTime(val(66000))),
    on("lubuntu_lxqt",()->waitUntilTime(val(68500))),
    on("kubuntu_plasma",()->waitUntilTime(val(68500))),
    on("void_i3",()->waitUntilTime(val(79500))),
    on("arch_sway",()->waitUntilTime(val(68500))),
    on("omarchy_hyprland",()->waitUntilTime(val(68500))),
    on("fedora_gnome",()->waitUntilTime(val(83000))),
    on("opensuse_plasma",()->waitUntilTime(val(76500))));
  final Action managerEnded= action("managerEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(67000))),
    on("debian_gnome_x11",()->waitUntilTime(val(76000))),
    on("xubuntu_xfce",()->waitUntilTime(val(76000))),
    on("debian_cinnamon",()->waitUntilTime(val(76000))),
    on("debian_mate",()->waitUntilTime(val(76000))),
    on("lubuntu_lxqt",()->waitUntilTime(val(79000))),
    on("kubuntu_plasma",()->waitUntilTime(val(79000))),
    on("void_i3",()->waitUntilTime(val(90000))),
    on("arch_sway",()->waitUntilTime(val(80000))),
    on("omarchy_hyprland",()->waitUntilTime(val(80000))),
    on("fedora_gnome",()->waitUntilTime(val(95000))),
    on("opensuse_plasma",()->waitUntilTime(val(88000))));
  final Action closeFiles= action("closeFiles",
    on("ubuntu_gnome",()->click(val(2374),val(844))),
    on("debian_gnome_x11",()->click(val(2341),val(844))),
    on("xubuntu_xfce",()->click(val(2538),val(623))),
    on("debian_cinnamon",()->click(val(1098),val(135))),
    on("debian_mate",()->click(val(1878),val(379))),
    on("lubuntu_lxqt",()->click(val(2149),val(887))),
    on("kubuntu_plasma",()->click(val(2286),val(737))),
    on("void_i3",()->keys(KeyEvent.VK_ALT,KeyEvent.VK_SHIFT,KeyEvent.VK_Q)),
    on("arch_sway",()->keys(KeyEvent.VK_ALT,KeyEvent.VK_SHIFT,KeyEvent.VK_Q)),
    on("omarchy_hyprland",()->keys(KeyEvent.VK_ALT,KeyEvent.VK_SHIFT,KeyEvent.VK_Q)),
    on("fedora_gnome",()->click(val(2341),val(844))),
    on("opensuse_plasma",()->click(val(2285),val(742))),
    on("windows",()->click(val(1016),val(58))));
  final Action filesClosed= action("filesClosed",
    on("ubuntu_gnome",()->waitUntilTime(val(70000))),
    on("debian_gnome_x11",()->waitUntilTime(val(78000))),
    on("xubuntu_xfce",()->waitUntilTime(val(80000))),
    on("debian_cinnamon",()->waitUntilTime(val(80000))),
    on("debian_mate",()->waitUntilTime(val(80000))),
    on("lubuntu_lxqt",()->waitUntilTime(val(83000))),
    on("kubuntu_plasma",()->waitUntilTime(val(83000))),
    on("void_i3",()->waitUntilTime(val(93000))),
    on("arch_sway",()->waitUntilTime(val(83000))),
    on("omarchy_hyprland",()->waitUntilTime(val(83000))),
    on("fedora_gnome",()->waitUntilTime(val(98000))),
    on("opensuse_plasma",()->waitUntilTime(val(92000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var project= project("helloWorld");
    launchScript("first");
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    stabilize();
    assertFalse(Files.exists(filesIOFolder.resolve(data).resolve("projects.info")));
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
    var bare= parkedShot();
    shell("nohup setsid -f xdg-open \""+project+"\" >/dev/null 2>&1\n");
    runInTerminal(appsShownFiles,terminalShownFiles);
    filesShown.go();
    var files= Pilot.changed(bare,pilot.shot(),6);
    openFile.go();
    managerOpened.go();
    stabilize();
    checkContent(info,"""
      {
        "hello_world": {
          "path": "Str:%s",
          "kind": "idle"
        }
      }
      """.formatted(slashed(project)));
    endScript();
    runInTerminal(appsShownToEnd,terminalShownToEnd);
    managerEnded.go();
    closeFiles.go();
    filesClosed.go();
    var before= pixels(bare,files);
    var after= pixels(parkedShot(),files);
    assertEquals(0L,IntStream.range(0,before.length).filter(i->!near(before[i],after[i])).count());
  }
  BufferedImage parkedShot(){
    var s= Toolkit.getDefaultToolkit().getScreenSize();
    pilot.glide(s.width/2-40,s.height/2-40,Pilot.none,s.width/2,s.height/2,Pilot.none);
    return pilot.shot();
  }
  static int[] pixels(BufferedImage img, Rectangle r){ return img.getRGB(r.x,r.y,r.width,r.height,null,0,r.width); }
  static boolean near(int p, int q){ return IntStream.of(0,8,16).allMatch(s->Math.abs((p>>s&255)-(q>>s&255))<=16); }
}

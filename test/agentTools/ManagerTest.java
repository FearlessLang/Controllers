package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import resources.ResolveResource;
import tools.Fs;
import tools.JavacTool;
import utils.Err;
import utils.OneOr;

abstract class ManagerTest{
  static{ Err.setUp(AssertionFailedError.class, Assertions::assertEquals, Assertions::assertTrue); }
  final String desk= System.getProperty("desk");
  final boolean agent= Boolean.getBoolean("agent");
  final Path channel= Path.of(System.getProperty("channelFolder"));
  final Path filesIOFolder= Path.of(System.getProperty("filesIOFolder"));
  final Pilot pilot= new Pilot();
  long last;
  int inputIndex;
  List<Object> inputs;
  abstract void walk() throws Throwable;
  @Test void test() throws Throwable{
    Fs.cleanDir(filesIOFolder);
    Fs.copyFresh(ResolveResource.managedFolderOut.resolve(app.getFileName()), app);
    last = System.currentTimeMillis();
    try{ walk(); }
    finally{
      ProcessHandle.allProcesses().filter(this::ofApp).forEach(ProcessHandle::destroyForcibly);
      inGuest("pkill -KILL -f '^"+app+"/'; true");
    }
  }
  boolean ofApp(ProcessHandle p){ return p.info().command().filter(c -> c.startsWith(app.toString())).isPresent(); }
  Action action(String name, On... ons){ return new Action(this, name, OneOr.of(name+" on "+desk, Stream.of(ons).filter(o -> o.desk().equals(desk))).steps()); }
  static On on(String desk, Run steps){ return new On(desk, steps); }
  record Action(ManagerTest m, String name, Run run){
    void go() throws Throwable{
      if (m.agent){ ask(); }
      run.run();
      m.last = System.currentTimeMillis();
    }
    private void ask(){
      m.inputIndex = 0;
      Fs.cleanDir(m.channel);
      var file= m.channel.resolve(name);
      Fs.writeUtf8(file, "");
      var text= Fs.readUtf8(file).strip();
      for (; !text.endsWith(";"); text = Fs.readUtf8(file).strip()){ Pilot.pause(100); }
      m.inputs = text.substring(0, text.length()-1).lines().map(String::strip).map(Action::input).toList();
    }
    static Object input(String s){ return s.startsWith("\"") ? s.substring(1, s.length()-1) : Integer.valueOf(s); }
  }
  interface Run{ void run() throws Throwable; }
  record On(String desk, Run steps){}
  @SuppressWarnings("unchecked")
  <T> T val(T recorded){ return agent ? (T)inputs.get(inputIndex++) : recorded; }
  void waitAfter(int millis){
    var left= last+millis-System.currentTimeMillis();
    if (left>0){ Pilot.pause((int)left); }
  }
  void click(int x, int y){ pilot.click(Pilot.Button.left, x, y); }
  void doubleClick(int x, int y){ pilot.doubleClick(x, y); }
  void rightClick(int x, int y){ pilot.click(Pilot.Button.right, x, y); }
  void drag(int x0, int y0, int x1, int y1){ pilot.drag(x0, y0, x1, y1); }
  void type(String text){ text.chars().forEach(this::type); }
  void type(int c){
    var i= "~!@#$%^&*()_+{}|:\"<>?".indexOf(c);
    if (i>=0){ pilot.chord(KeyEvent.VK_SHIFT, KeyEvent.getExtendedKeyCodeForChar("`1234567890-=[]\\;',./".charAt(i))); return; }
    var code= KeyEvent.getExtendedKeyCodeForChar(c);
    if (Character.isUpperCase(c)){ pilot.chord(KeyEvent.VK_SHIFT, code); return; }
    pilot.chord(code);
  }
  void keys(int... codes){ pilot.chord(codes); }
  void stabilize(){ inGuest("sync; echo 3 >/proc/sys/vm/drop_caches"); }
  static void inGuest(String script){
    if (!Fs.isLinux()){ return; }
    OneOr.opt("one guest alive", virsh("list", "--name").lines().filter(l -> !l.isBlank())).ifPresent(g -> inGuest(g, script));
  }
  static void inGuest(String guest, String script){
    var pid= virsh("qemu-agent-command", guest, "{\"execute\":\"guest-exec\",\"arguments\":{\"path\":\"/bin/sh\",\"arg\":[\"-c\",\""+script+"\"]}}").replaceAll("\\D", "");
    var status= "{\"execute\":\"guest-exec-status\",\"arguments\":{\"pid\":"+pid+"}}";
    var done= virsh("qemu-agent-command", guest, status);
    for (; !done.contains("\"exited\":true"); done = virsh("qemu-agent-command", guest, status)){ Pilot.pause(100); }
    assert done.contains("\"exitcode\":0");
  }
  static String virsh(String... args){
    var p= Fs.of(() -> new ProcessBuilder(Stream.concat(Stream.of("virsh", "-c", "qemu:///system"), Stream.of(args)).toList()).redirectErrorStream(true).start());
    var out= Fs.of(() -> new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
    assertEquals(0, p.onExit().join().exitValue(), out);
    return out;
  }
  void checkContent(Path file, String expected){ Err.strCmp(expected, Fs.readUtf8(file)); }
  static final String data= JavacTool.dataDirNameFor(ResolveResource.versionId);
  final Path info= filesIOFolder.resolve(data).resolve("projects.info");
  final Path app= filesIOFolder.resolve("fearlessManaged"+ResolveResource.versionId);
  final Path files= filesIOFolder.resolve("files");
  final Action managerMenu= action("managerMenu",
    on("ubuntu_gnome", () -> waitClick(val(100), val(99), val(79))),
    on("kubuntu_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("opensuse_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("debian_cinnamon", () -> waitClick(val(null), val(null), val(null))),
    on("lubuntu_lxqt", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitClick(val(null), val(null), val(null))),
    on("debian_gnome_x11", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitClick(val(null), val(null), val(null))),
    on("debian_mate", () -> waitClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitClick(val(null), val(38), val(62))),
    on("arch_sway", () -> waitClick(val(null), val(null), val(null))),
    on("void_i3", () -> waitClick(val(null), val(null), val(null))),
    on("omarchy_hyprland", () -> waitClick(val(100), val(997), val(46))));
  final Action addFolder= action("addFolder",
    on("ubuntu_gnome", () -> waitClick(val(null), val(110), val(103))),
    on("kubuntu_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("opensuse_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("debian_cinnamon", () -> waitClick(val(null), val(null), val(null))),
    on("lubuntu_lxqt", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitClick(val(null), val(null), val(null))),
    on("debian_gnome_x11", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitClick(val(null), val(null), val(null))),
    on("debian_mate", () -> waitClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitClick(val(null), val(43), val(84))),
    on("arch_sway", () -> waitClick(val(null), val(null), val(null))),
    on("void_i3", () -> waitClick(val(null), val(null), val(null))),
    on("omarchy_hyprland", () -> waitClick(val(null), val(null), val(null))));
  final Action editMetadata= action("editMetadata",
    on("ubuntu_gnome", () -> waitClick(val(null), val(143), val(124))),
    on("kubuntu_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("opensuse_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("debian_cinnamon", () -> waitClick(val(null), val(null), val(null))),
    on("lubuntu_lxqt", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitClick(val(null), val(null), val(null))),
    on("debian_gnome_x11", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitClick(val(null), val(null), val(null))),
    on("debian_mate", () -> waitClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitClick(val(null), val(76), val(102))),
    on("arch_sway", () -> waitClick(val(null), val(null), val(null))),
    on("void_i3", () -> waitClick(val(null), val(null), val(null))),
    on("omarchy_hyprland", () -> waitClick(val(null), val(null), val(null))));
  final Action connectEclipse= action("connectEclipse",
    on("ubuntu_gnome", () -> waitClick(val(null), val(126), val(145))),
    on("kubuntu_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("opensuse_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("debian_cinnamon", () -> waitClick(val(null), val(null), val(null))),
    on("lubuntu_lxqt", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitClick(val(null), val(null), val(null))),
    on("debian_gnome_x11", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitClick(val(null), val(null), val(null))),
    on("debian_mate", () -> waitClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitClick(val(null), val(59), val(122))),
    on("arch_sway", () -> waitClick(val(null), val(null), val(null))),
    on("void_i3", () -> waitClick(val(null), val(null), val(null))),
    on("omarchy_hyprland", () -> waitClick(val(null), val(null), val(null))));
  final Action resetManager= action("resetManager",
    on("ubuntu_gnome", () -> waitClick(val(null), val(90), val(191))),
    on("kubuntu_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("opensuse_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("debian_cinnamon", () -> waitClick(val(null), val(null), val(null))),
    on("lubuntu_lxqt", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitClick(val(null), val(null), val(null))),
    on("debian_gnome_x11", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitClick(val(null), val(null), val(null))),
    on("debian_mate", () -> waitClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitClick(val(null), val(23), val(164))),
    on("arch_sway", () -> waitClick(val(null), val(null), val(null))),
    on("void_i3", () -> waitClick(val(null), val(null), val(null))),
    on("omarchy_hyprland", () -> waitClick(val(null), val(null), val(null))));
  final Action quitManager= action("quitManager",
    on("ubuntu_gnome", () -> waitClick(val(100), val(88), val(212))),
    on("kubuntu_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("opensuse_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("debian_cinnamon", () -> waitClick(val(null), val(null), val(null))),
    on("lubuntu_lxqt", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitClick(val(null), val(null), val(null))),
    on("debian_gnome_x11", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitClick(val(null), val(null), val(null))),
    on("debian_mate", () -> waitClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitClick(val(null), val(21), val(182))),
    on("arch_sway", () -> waitClick(val(null), val(null), val(null))),
    on("void_i3", () -> waitClick(val(null), val(null), val(null))),
    on("omarchy_hyprland", () -> waitClick(val(100), val(985), val(167))));
  final Action closeWindow= action("closeWindow",
    on("ubuntu_gnome", () -> waitClick(val(100), val(1901), val(49))),
    on("kubuntu_plasma", () -> waitClick(val(100), val(1905), val(14))),
    on("opensuse_plasma", () -> waitClick(val(100), val(1905), val(14))),
    on("debian_cinnamon", () -> waitClick(val(100), val(1901), val(18))),
    on("lubuntu_lxqt", () -> waitClick(val(100), val(1905), val(15))),
    on("fedora_gnome", () -> waitClick(val(100), val(1901), val(50))),
    on("debian_gnome_x11", () -> waitClick(val(100), val(1901), val(49))),
    on("fedora_cosmic", () -> waitClick(val(100), val(1895), val(49))),
    on("debian_mate", () -> waitClick(val(100), val(1900), val(41))),
    on("xubuntu_xfce", () -> waitClick(val(100), val(1908), val(37))),
    on("arch_sway", () -> waitClickKeys(val(100), val(1500), val(64), KeyEvent.VK_ALT, KeyEvent.VK_SHIFT, KeyEvent.VK_Q)),//a click on an empty part of the menu bar of the manager window, then Alt+Shift+Q, the key closing a window on this desk
    on("void_i3", () -> waitClickKeys(val(100), val(1500), val(27), KeyEvent.VK_ALT, KeyEvent.VK_SHIFT, KeyEvent.VK_Q)),//a click on an empty part of the menu bar of the manager window, then Alt+Shift+Q, the key closing a window on this desk
    on("omarchy_hyprland", () -> waitClickKeys(val(100), val(1500), val(46), KeyEvent.VK_ALT, KeyEvent.VK_SHIFT, KeyEvent.VK_Q)));//a click on an empty part of the menu bar of the manager window, then Alt+Shift+Q, the key closing a window on this desk
  final Action firstTile= action("firstTile",
    on("ubuntu_gnome", () -> waitClick(val(null), val(133), val(130))),
    on("kubuntu_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("opensuse_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("debian_cinnamon", () -> waitClick(val(null), val(null), val(null))),
    on("lubuntu_lxqt", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitClick(val(null), val(null), val(null))),
    on("debian_gnome_x11", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitClick(val(null), val(null), val(null))),
    on("debian_mate", () -> waitClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitClick(val(null), val(66), val(110))),
    on("arch_sway", () -> waitClick(val(null), val(null), val(null))),
    on("void_i3", () -> waitClick(val(null), val(null), val(null))),
    on("omarchy_hyprland", () -> waitClick(val(null), val(null), val(null))));
  //the icon of the manager in the tray: a white F on an orange and blue square
  final Action trayIcon= action("trayIcon",
    on("ubuntu_gnome", () -> waitClick(val(100), val(1736), val(15))),
    on("kubuntu_plasma", () -> waitClick(val(100), val(1640), val(1052))),
    on("opensuse_plasma", () -> waitClick(val(100), val(1689), val(1057))),
    on("debian_cinnamon", () -> waitClick(val(100), val(1755), val(1058))),
    on("lubuntu_lxqt", () -> waitClick(val(100), val(1860), val(1064))),
    on("fedora_gnome", () -> waitClick(val(100), val(1900), val(1062))),
    on("debian_gnome_x11", () -> waitClick(val(100), val(1902), val(1064))),
    on("fedora_cosmic", () -> waitClick(val(100), val(1660), val(15))),
    on("debian_mate", () -> waitClick(val(100), val(1904), val(1038))),
    on("xubuntu_xfce", () -> waitClick(val(100), val(1783), val(12))),
    on("arch_sway", () -> waitClick(val(100), val(1882), val(14))),
    on("void_i3", () -> waitClick(val(100), val(1900), val(1062))),
    on("omarchy_hyprland", () -> waitClick(val(100), val(1829), val(12))));
  final Action trayShow= action("trayShow",
    on("ubuntu_gnome", () -> waitClick(val(100), val(1709), val(62))),
    on("kubuntu_plasma", () -> waitClick(val(100), val(1660), val(973))),
    on("opensuse_plasma", () -> waitClick(val(100), val(1740), val(981))),
    on("debian_cinnamon", () -> waitClick(val(100), val(1789), val(1002))),
    on("lubuntu_lxqt", () -> waitClick(val(100), val(1858), val(1042))),
    on("fedora_gnome", () -> waitClick(val(100), val(1858), val(1042))),
    on("debian_gnome_x11", () -> waitClick(val(100), val(1858), val(1042))),
    on("fedora_cosmic", () -> waitClick(val(100), val(1556), val(69))),
    on("debian_mate", () -> waitClick(val(100), val(1850), val(1042))),
    on("xubuntu_xfce", () -> waitClick(val(100), val(1718), val(41))),
    on("arch_sway", () -> waitClick(val(100), val(1804), val(31))),
    on("void_i3", () -> waitClick(val(100), val(1850), val(1045))),
    on("omarchy_hyprland", () -> waitClick(val(100), val(1760), val(55))));
  void park(){
    var s= Toolkit.getDefaultToolkit().getScreenSize();
    pilot.glide(s.width/2-40, s.height/2-40, Pilot.none, s.width/2, s.height/2, Pilot.none);
  }
  BufferedImage look(){ park(); return pilot.shot(); }
  Rectangle region;
  void region(int x, int y, int w, int h){ region = new Rectangle(x, y, w, h); }
  static int[] pixels(BufferedImage img, Rectangle r){ return img.getRGB(r.x, r.y, r.width, r.height, null, 0, r.width); }
  static boolean near(int p, int q){ return IntStream.of(0, 8, 16).allMatch(s -> Math.abs((p>>s&255)-(q>>s&255))<=16); }
  static long differing(BufferedImage a, BufferedImage b, Rectangle r){
    var pa= pixels(a, r);
    var pb= pixels(b, r);
    return IntStream.range(0, pa.length).filter(i -> !near(pa[i], pb[i])).count();
  }
  void same(BufferedImage before, Rectangle r){ assertEquals(0L, differing(before, look(), r)); }
  void changed(BufferedImage before, Rectangle r){ assertNotEquals(0L, differing(before, look(), r)); }
  Path project(String name){
    var res= files.resolve(name);
    Fs.copyFresh(ResolveResource.integrationTests.resolve(name), res);
    Fs.rmTree(res.resolve(".fearless_out"));
    return res;
  }
  void waitClick(int millis, int x, int y){ waitAfter(millis); click(x, y); }
  void waitDoubleClick(int millis, int x, int y){ waitAfter(millis); doubleClick(x, y); }
  void waitRightClick(int millis, int x, int y){ waitAfter(millis); rightClick(x, y); }
  void waitDrag(int millis, int x0, int y0, int x1, int y1){ waitAfter(millis); drag(x0, y0, x1, y1); }
  void waitKeys(int millis, int... codes){ waitAfter(millis); keys(codes); }
  void waitType(int millis, String text){ waitAfter(millis); type(text); }
  void waitClickKeys(int millis, int x, int y, int... codes){ waitClick(millis, x, y); keys(codes); }
  final Action filesIcon= action("filesIcon",
    on("ubuntu_gnome", () -> waitClick(val(100), val(33), val(128))),
    on("kubuntu_plasma", () -> waitClick(val(100), val(212), val(1052))),//Dolphin, the file manager icon of the taskbar
    on("opensuse_plasma", () -> waitClick(val(100), val(221), val(1057))),//Dolphin, the file manager icon of the taskbar
    on("debian_cinnamon", () -> waitClick(val(100), val(75), val(1057))),
    on("lubuntu_lxqt", () -> waitDoubleClick(val(100), val(75), val(380))),//the home icon tester on the desktop
    on("fedora_gnome", () -> waitClick(val(100), val(880), val(1017))),
    on("debian_gnome_x11", () -> waitClick(val(100), val(840), val(1017))),
    on("fedora_cosmic", () -> waitClick(val(100), val(958), val(1038))),
    on("debian_mate", () -> waitDoubleClick(val(100), val(88), val(72))),//the home icon of the desktop
    on("xubuntu_xfce", () -> waitClick(val(100), val(935), val(1057))),
    on("arch_sway", () -> waitType(val(100), "thunar\n")),//types thunar and Enter in the launcher
    on("void_i3", () -> waitType(val(100), "thunar\n")),//types thunar and Enter in the launcher
    on("omarchy_hyprland", () -> waitClick(val(100), val(903), val(758))));//Files, in the apps menu
  final Action pilotioBookmark= action("pilotioBookmark",
    on("ubuntu_gnome", () -> waitClick(val(100), val(613), val(743))),
    on("kubuntu_plasma", () -> waitClick(val(100), val(629), val(544))),
    on("opensuse_plasma", () -> waitClick(val(100), val(629), val(547))),
    on("debian_cinnamon", () -> waitClick(val(100), val(227), val(546))),
    on("lubuntu_lxqt", () -> waitClick(val(100), val(706), val(684))),
    on("fedora_gnome", () -> waitClick(val(100), val(580), val(743))),
    on("debian_gnome_x11", () -> waitClick(val(100), val(579), val(743))),
    on("fedora_cosmic", () -> waitClick(val(100), val(524), val(504))),
    on("debian_mate", () -> waitClick(val(100), val(229), val(632))),
    on("xubuntu_xfce", () -> waitClick(val(100), val(704), val(610))),
    on("arch_sway", () -> waitClick(val(100), val(67), val(271))),
    on("void_i3", () -> waitClick(val(100), val(66), val(151))),
    on("omarchy_hyprland", () -> waitClick(val(2000), val(76), val(462))));//the file manager window, started from the apps menu, takes up to two seconds to show its final layout
  final Action openApp= action("openApp",
    on("ubuntu_gnome", () -> waitDoubleClick(val(100), val(806), val(380))),
    on("kubuntu_plasma", () -> waitDoubleClick(val(100), val(788), val(305))),
    on("opensuse_plasma", () -> waitDoubleClick(val(100), val(797), val(305))),
    on("debian_cinnamon", () -> waitDoubleClick(val(100), val(396), val(305))),
    on("lubuntu_lxqt", () -> waitDoubleClick(val(100), val(910), val(395))),
    on("fedora_gnome", () -> waitDoubleClick(val(100), val(794), val(400))),
    on("debian_gnome_x11", () -> waitDoubleClick(val(100), val(794), val(400))),
    on("fedora_cosmic", () -> waitDoubleClick(val(100), val(882), val(294))),
    on("debian_mate", () -> waitDoubleClick(val(100), val(393), val(353))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(100), val(880), val(430))),
    on("arch_sway", () -> waitDoubleClick(val(100), val(240), val(165))),
    on("void_i3", () -> waitDoubleClick(val(100), val(241), val(122))),
    on("omarchy_hyprland", () -> waitDoubleClick(val(300), val(348), val(155))));
  final Action openAppNextToManagerFolder= action("openAppNextToManagerFolder",
    on("ubuntu_gnome", () -> waitDoubleClick(val(100), val(911), val(380))),
    on("kubuntu_plasma", () -> waitDoubleClick(val(100), val(925), val(305))),
    on("opensuse_plasma", () -> waitDoubleClick(val(100), val(934), val(310))),
    on("debian_cinnamon", () -> waitDoubleClick(val(100), val(520), val(305))),
    on("lubuntu_lxqt", () -> waitDoubleClick(val(100), val(1036), val(395))),
    on("fedora_gnome", () -> waitDoubleClick(val(100), val(965), val(400))),
    on("debian_gnome_x11", () -> waitDoubleClick(val(100), val(964), val(400))),
    on("fedora_cosmic", () -> waitDoubleClick(val(100), val(882), val(343))),
    on("debian_mate", () -> waitDoubleClick(val(100), val(553), val(353))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(100), val(1015), val(430))),
    on("arch_sway", () -> waitDoubleClick(val(100), val(376), val(165))),
    on("void_i3", () -> waitDoubleClick(val(100), val(377), val(122))),
    on("omarchy_hyprland", () -> waitDoubleClick(val(300), val(510), val(155))));
  final Action openBin= action("openBin",
    on("ubuntu_gnome", () -> waitDoubleClick(val(100), val(798), val(380))),
    on("kubuntu_plasma", () -> waitDoubleClick(val(100), val(787), val(305))),
    on("opensuse_plasma", () -> waitDoubleClick(val(100), val(797), val(305))),
    on("debian_cinnamon", () -> waitDoubleClick(val(100), val(396), val(305))),
    on("lubuntu_lxqt", () -> waitDoubleClick(val(100), val(910), val(395))),
    on("fedora_gnome", () -> waitDoubleClick(val(100), val(794), val(400))),
    on("debian_gnome_x11", () -> waitDoubleClick(val(100), val(794), val(400))),
    on("fedora_cosmic", () -> waitDoubleClick(val(100), val(818), val(294))),
    on("debian_mate", () -> waitDoubleClick(val(100), val(393), val(353))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(100), val(878), val(430))),
    on("arch_sway", () -> waitDoubleClick(val(100), val(240), val(165))),
    on("void_i3", () -> waitDoubleClick(val(100), val(241), val(122))),
    on("omarchy_hyprland", () -> waitDoubleClick(val(300), val(348), val(155))));
  //double clicks fearlessManaged0_001w, the launcher whose name ends with w
  final Action openLauncher= action("openLauncher",
    on("ubuntu_gnome", () -> waitDoubleClick(val(100), val(911), val(380))),
    on("kubuntu_plasma", () -> waitDoubleClick(val(100), val(925), val(305))),
    on("opensuse_plasma", () -> waitDoubleClick(val(100), val(934), val(310))),
    on("debian_cinnamon", () -> waitDoubleClick(val(100), val(518), val(305))),
    on("lubuntu_lxqt", () -> waitDoubleClick(val(100), val(1036), val(395))),
    on("fedora_gnome", () -> waitDoubleClick(val(100), val(964), val(400))),
    on("debian_gnome_x11", () -> waitDoubleClick(val(100), val(964), val(400))),
    on("fedora_cosmic", () -> waitDoubleClick(val(100), val(888), val(343))),
    on("debian_mate", () -> waitDoubleClick(val(100), val(553), val(353))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(100), val(1015), val(430))),
    on("arch_sway", () -> waitDoubleClick(val(100), val(376), val(165))),
    on("void_i3", () -> waitDoubleClick(val(100), val(378), val(122))),
    on("omarchy_hyprland", () -> waitDoubleClick(val(300), val(510), val(155))));
  final Action openFiles= action("openFiles",
    on("ubuntu_gnome", () -> waitDoubleClick(val(100), val(1024), val(380))),
    on("kubuntu_plasma", () -> waitDoubleClick(val(100), val(1063), val(305))),
    on("opensuse_plasma", () -> waitDoubleClick(val(100), val(1072), val(310))),
    on("debian_cinnamon", () -> waitDoubleClick(val(100), val(644), val(305))),
    on("lubuntu_lxqt", () -> waitDoubleClick(val(100), val(1163), val(395))),
    on("fedora_gnome", () -> waitDoubleClick(val(100), val(1134), val(400))),
    on("debian_gnome_x11", () -> waitDoubleClick(val(100), val(1134), val(400))),
    on("fedora_cosmic", () -> waitDoubleClick(val(100), val(821), val(392))),
    on("debian_mate", () -> waitDoubleClick(val(100), val(714), val(353))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(100), val(1150), val(430))),
    on("arch_sway", () -> waitDoubleClick(val(100), val(512), val(165))),
    on("void_i3", () -> waitDoubleClick(val(100), val(512), val(122))),
    on("omarchy_hyprland", () -> waitDoubleClick(val(300), val(664), val(155))));
  //brings to the front the file manager window that an earlier start() or toFiles() left open; on a desk that tiles the windows side by side that window is already visible, and this is a wait
  final Action fileManagerToFront= action("fileManagerToFront",
    on("ubuntu_gnome", () -> waitClick(val(100), val(33), val(128))),
    on("kubuntu_plasma", () -> waitClick(val(100), val(212), val(1052))),
    on("opensuse_plasma", () -> waitClick(val(100), val(221), val(1057))),
    on("debian_cinnamon", () -> waitClick(val(100), val(75), val(1057))),
    on("lubuntu_lxqt", () -> waitClick(val(100), val(215), val(1064))),
    on("fedora_gnome", () -> waitClick(val(100), val(460), val(600))),
    on("debian_gnome_x11", () -> waitClick(val(100), val(600), val(560))),
    on("fedora_cosmic", () -> waitClick(val(100), val(918), val(1040))),
    on("debian_mate", () -> waitClick(val(100), val(72), val(1067))),
    on("xubuntu_xfce", () -> waitClick(val(100), val(390), val(12))),
    on("arch_sway", () -> waitAfter(val(100))),
    on("void_i3", () -> waitAfter(val(100))),
    on("omarchy_hyprland", () -> waitAfter(val(100))));
  //brings the manager window to the front after a marker opens, on a desk that leaves it behind the window in use; on a desk where the manager window comes to the front by itself a wait, which is not a finding
  final Action managerToFront= action("managerToFront",
    on("ubuntu_gnome", () -> waitClick(val(100), val(33), val(387))),
    on("kubuntu_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("opensuse_plasma", () -> waitClick(val(null), val(null), val(null))),
    on("debian_cinnamon", () -> waitClick(val(null), val(null), val(null))),
    on("lubuntu_lxqt", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitClick(val(null), val(null), val(null))),
    on("debian_gnome_x11", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitClick(val(null), val(null), val(null))),
    on("debian_mate", () -> waitClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(null))),
    on("arch_sway", () -> waitAfter(val(null))),
    on("void_i3", () -> waitAfter(val(null))),
    on("omarchy_hyprland", () -> waitAfter(val(null))));
  final Action closeFileManager= action("closeFileManager",
    on("ubuntu_gnome", () -> waitClick(val(100), val(1414), val(304))),
    on("kubuntu_plasma", () -> waitClick(val(100), val(1326), val(197))),
    on("opensuse_plasma", () -> waitClick(val(100), val(1325), val(202))),
    on("debian_cinnamon", () -> waitClick(val(100), val(940), val(168))),
    on("lubuntu_lxqt", () -> waitClick(val(100), val(1265), val(282))),
    on("fedora_gnome", () -> waitClick(val(100), val(1381), val(304))),
    on("debian_gnome_x11", () -> waitClick(val(100), val(1381), val(304))),
    on("fedora_cosmic", () -> waitClick(val(100), val(1447), val(177))),
    on("debian_mate", () -> waitClick(val(100), val(939), val(190))),
    on("xubuntu_xfce", () -> waitClick(val(100), val(1269), val(311))),
    on("arch_sway", () -> waitClickKeys(val(100), val(600), val(600), KeyEvent.VK_ALT, KeyEvent.VK_SHIFT, KeyEvent.VK_Q)),//a click on an empty part of the file manager window, then Alt+Shift+Q, the key closing a window on this desk
    on("void_i3", () -> waitClickKeys(val(100), val(600), val(600), KeyEvent.VK_ALT, KeyEvent.VK_SHIFT, KeyEvent.VK_Q)),//a click on an empty part of the file manager window, then Alt+Shift+Q, the key closing a window on this desk
    on("omarchy_hyprland", () -> waitClick(val(100), val(1884), val(61))));
  //makes the files icon reachable, on a desk where filesIcon can not reach it directly; on every other desk a wait, which is not a finding
  final Action showFilesIcon= action("showFilesIcon",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("kubuntu_plasma", () -> waitAfter(val(100))),
    on("opensuse_plasma", () -> waitAfter(val(100))),
    on("debian_cinnamon", () -> waitAfter(val(100))),
    on("lubuntu_lxqt", () -> waitAfter(val(100))),
    on("fedora_gnome", () -> waitClick(val(100), val(30), val(15))),//Activities
    on("debian_gnome_x11", () -> waitClick(val(100), val(30), val(16))),//Activities
    on("fedora_cosmic", () -> waitAfter(val(100))),
    on("debian_mate", () -> waitAfter(val(100))),
    on("xubuntu_xfce", () -> waitAfter(val(100))),
    on("arch_sway", () -> waitKeys(val(100), KeyEvent.VK_ALT, KeyEvent.VK_D)),//Alt+D opens the launcher of this desk
    on("void_i3", () -> waitKeys(val(100), KeyEvent.VK_ALT, KeyEvent.VK_D)),//Alt+D opens the launcher of this desk
    on("omarchy_hyprland", () -> waitKeys(val(100), KeyEvent.VK_ALT, KeyEvent.VK_SPACE)));//Alt+Space opens the apps menu of this desk
  //makes the open file manager window reachable, on a desk where fileManagerToFront can not reach it directly; on every other desk, also where the file manager window is already visible, a wait, which is not a finding
  final Action showFileManager= action("showFileManager",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("kubuntu_plasma", () -> waitAfter(val(100))),
    on("opensuse_plasma", () -> waitAfter(val(100))),
    on("debian_cinnamon", () -> waitAfter(val(100))),
    on("lubuntu_lxqt", () -> waitAfter(val(100))),
    on("fedora_gnome", () -> waitClick(val(100), val(30), val(15))),//Activities
    on("debian_gnome_x11", () -> waitClick(val(100), val(30), val(16))),//Activities
    on("fedora_cosmic", () -> waitAfter(val(100))),
    on("debian_mate", () -> waitAfter(val(100))),
    on("xubuntu_xfce", () -> waitAfter(val(100))),
    on("arch_sway", () -> waitAfter(val(100))),
    on("void_i3", () -> waitAfter(val(100))),
    on("omarchy_hyprland", () -> waitAfter(val(100))));
  //answers what the desk asks after the double click on the launcher; on a desk that asks nothing a wait, which is not a finding
  final Action confirmLauncher= action("confirmLauncher",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("kubuntu_plasma", () -> waitClick(val(100), val(975), val(591))),//Launch, in the dialog asking to launch the file
    on("opensuse_plasma", () -> waitClick(val(100), val(955), val(580))),//Launch, in the dialog asking to launch the file
    on("debian_cinnamon", () -> waitAfter(val(100))),
    on("lubuntu_lxqt", () -> waitClick(val(100), val(797), val(524))),//Execute, in the dialog asking to execute the launcher
    on("fedora_gnome", () -> waitAfter(val(100))),
    on("debian_gnome_x11", () -> waitAfter(val(100))),
    on("fedora_cosmic", () -> waitAfter(val(100))),
    on("debian_mate", () -> waitAfter(val(100))),
    on("xubuntu_xfce", () -> waitAfter(val(100))),
    on("arch_sway", () -> waitAfter(val(100))),
    on("void_i3", () -> waitAfter(val(100))),
    on("omarchy_hyprland", () -> waitAfter(val(100))));
  boolean filesOpen;
  void toPilotio() throws Throwable{
    (filesOpen ? showFileManager : showFilesIcon).go();
    (filesOpen ? fileManagerToFront : filesIcon).go();
    filesOpen = true;
    pilotioBookmark.go();
  }
  void closeFiles() throws Throwable{
    closeFileManager.go();
    filesOpen = false;
  }
  //the actions of start(), in this order: showFilesIcon and filesIcon (showFileManager and fileManagerToFront when a file manager window is already open), pilotioBookmark, openApp (openAppNextToManagerFolder when the manager folder fearless0_001 is next to the app), openBin, openLauncher, confirmLauncher.
  //Before filesIcon the manager is not started; the manager window shows only after openLauncher and confirmLauncher.
  //start() leaves the file manager open, behind the manager window.
  void start() throws Throwable{
    toPilotio();
    (Files.exists(filesIOFolder.resolve(data)) ? openAppNextToManagerFolder : openApp).go();
    openBin.go();
    openLauncher.go();
    confirmLauncher.go();
  }
  void toFiles() throws Throwable{
    toPilotio();
    openFiles.go();
  }
  static String slashed(Path p){ return p.toString().replace('\\', '/'); }
  static String escaped(Path p){ return p.toString().replace("\\", "\\\\"); }
}

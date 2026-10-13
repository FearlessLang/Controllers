package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
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
    on("ubuntu_gnome", () -> waitClick(val(null), val(99), val(79))),
    on("lubuntu_lxqt", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitClick(val(null), val(null), val(null))),
    on("debian_mate", () -> waitClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitClick(val(null), val(38), val(62))));
  final Action addFolder= action("addFolder",
    on("ubuntu_gnome", () -> waitClick(val(null), val(110), val(103))),
    on("lubuntu_lxqt", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitClick(val(null), val(null), val(null))),
    on("debian_mate", () -> waitClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitClick(val(null), val(43), val(84))));
  final Action editMetadata= action("editMetadata",
    on("ubuntu_gnome", () -> waitClick(val(null), val(143), val(124))),
    on("lubuntu_lxqt", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitClick(val(null), val(null), val(null))),
    on("debian_mate", () -> waitClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitClick(val(null), val(76), val(102))));
  final Action connectEclipse= action("connectEclipse",
    on("ubuntu_gnome", () -> waitClick(val(null), val(126), val(145))),
    on("lubuntu_lxqt", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitClick(val(null), val(null), val(null))),
    on("debian_mate", () -> waitClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitClick(val(null), val(59), val(122))));
  final Action resetManager= action("resetManager",
    on("ubuntu_gnome", () -> waitClick(val(null), val(90), val(191))),
    on("lubuntu_lxqt", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitClick(val(null), val(null), val(null))),
    on("debian_mate", () -> waitClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitClick(val(null), val(23), val(164))));
  final Action quitManager= action("quitManager",
    on("ubuntu_gnome", () -> waitClick(val(null), val(88), val(212))),
    on("lubuntu_lxqt", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitClick(val(null), val(null), val(null))),
    on("debian_mate", () -> waitClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitClick(val(null), val(21), val(182))));
  final Action closeWindow= action("closeWindow",
    on("ubuntu_gnome", () -> waitClick(val(100), val(1901), val(49))),
    on("lubuntu_lxqt", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitClick(val(100), val(1895), val(49))),
    on("debian_mate", () -> waitClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitClick(val(100), val(1908), val(37))));
  final Action firstTile= action("firstTile",
    on("ubuntu_gnome", () -> waitClick(val(null), val(133), val(130))),
    on("lubuntu_lxqt", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitClick(val(null), val(null), val(null))),
    on("debian_mate", () -> waitClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitClick(val(null), val(66), val(110))));
  //the icon of the manager in the tray: a white F on an orange and blue square
  final Action trayIcon= action("trayIcon",
    on("ubuntu_gnome", () -> waitClick(val(100), val(1736), val(15))),
    on("lubuntu_lxqt", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitClick(val(100), val(1660), val(15))),
    on("debian_mate", () -> waitClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitClick(val(100), val(1783), val(12))));
  final Action trayShow= action("trayShow",
    on("ubuntu_gnome", () -> waitClick(val(100), val(1709), val(62))),
    on("lubuntu_lxqt", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitClick(val(100), val(1556), val(69))),
    on("debian_mate", () -> waitClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitClick(val(100), val(1718), val(41))));
  void park(){
    var s= Toolkit.getDefaultToolkit().getScreenSize();
    pilot.glide(s.width/2-40, s.height/2-40, Pilot.none, s.width/2, s.height/2, Pilot.none);
  }
  BufferedImage look(){ park(); return pilot.shot(); }
  static final Map<String,Point> content= Map.ofEntries(
    Map.entry("ubuntu_gnome", new Point(67, 92)),
    Map.entry("xubuntu_xfce", new Point(0, 72)),
    Map.entry("lubuntu_lxqt", new Point(0, 55)),
    Map.entry("fedora_gnome", new Point(0, 92)),
    Map.entry("fedora_cosmic", new Point(1, 91)),
    Map.entry("debian_mate", new Point(1, 82)));
  Rectangle inWindow(int x, int y, int w, int h){
    var at= content.get(desk);
    return new Rectangle(x-67+at.x, y-92+at.y, w, h);
  }
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
  final Action filesIcon= action("filesIcon",
    on("ubuntu_gnome", () -> waitClick(val(100), val(33), val(128))),
    on("lubuntu_lxqt", () -> waitDoubleClick(val(null), val(null), val(null))),//the home icon tester on the desktop
    on("fedora_gnome", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitClick(val(100), val(958), val(1038))),
    on("debian_mate", () -> waitDoubleClick(val(null), val(null), val(null))),//the home icon of the desktop
    on("xubuntu_xfce", () -> waitClick(val(100), val(935), val(1057))));
  final Action pilotioBookmark= action("pilotioBookmark",
    on("ubuntu_gnome", () -> waitClick(val(100), val(613), val(743))),
    on("lubuntu_lxqt", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitClick(val(100), val(524), val(504))),
    on("debian_mate", () -> waitClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitClick(val(100), val(704), val(610))));
  final Action openApp= action("openApp",
    on("ubuntu_gnome", () -> waitDoubleClick(val(100), val(806), val(380))),
    on("lubuntu_lxqt", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitDoubleClick(val(100), val(882), val(294))),
    on("debian_mate", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(100), val(880), val(430))));
  final Action openAppNextToManagerFolder= action("openAppNextToManagerFolder",
    on("ubuntu_gnome", () -> waitDoubleClick(val(100), val(911), val(380))),
    on("lubuntu_lxqt", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitDoubleClick(val(100), val(882), val(343))),
    on("debian_mate", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(100), val(1015), val(430))));
  final Action openBin= action("openBin",
    on("ubuntu_gnome", () -> waitDoubleClick(val(100), val(798), val(380))),
    on("lubuntu_lxqt", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitDoubleClick(val(100), val(818), val(294))),
    on("debian_mate", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(100), val(878), val(430))));
  //double clicks fearlessManaged0_001w, the launcher whose name ends with w
  final Action openLauncher= action("openLauncher",
    on("ubuntu_gnome", () -> waitDoubleClick(val(100), val(911), val(380))),
    on("lubuntu_lxqt", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitDoubleClick(val(100), val(888), val(343))),
    on("debian_mate", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(100), val(1015), val(430))));
  final Action openFiles= action("openFiles",
    on("ubuntu_gnome", () -> waitDoubleClick(val(100), val(1024), val(380))),
    on("lubuntu_lxqt", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitDoubleClick(val(100), val(821), val(392))),
    on("debian_mate", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(100), val(1150), val(430))));
  //brings to the front the file manager window that an earlier start() or toFiles() left open
  final Action fileManagerToFront= action("fileManagerToFront",
    on("ubuntu_gnome", () -> waitClick(val(100), val(33), val(128))),
    on("lubuntu_lxqt", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitClick(val(100), val(918), val(1040))),
    on("debian_mate", () -> waitClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitClick(val(100), val(390), val(12))));
  final Action closeFileManager= action("closeFileManager",
    on("ubuntu_gnome", () -> waitClick(val(100), val(1414), val(304))),
    on("lubuntu_lxqt", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_gnome", () -> waitClick(val(null), val(null), val(null))),
    on("fedora_cosmic", () -> waitClick(val(100), val(1447), val(177))),
    on("debian_mate", () -> waitClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitClick(val(100), val(1269), val(311))));
  //makes the files icon visible, on a desk where it is not always on screen
  final Action showFilesIcon= action("showFilesIcon",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("lubuntu_lxqt", () -> waitAfter(val(null))),
    on("fedora_gnome", () -> waitClick(val(null), val(null), val(null))),//Activities
    on("fedora_cosmic", () -> waitAfter(val(100))),
    on("debian_mate", () -> waitAfter(val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(100))));
  //answers what the desk asks after the double click on the launcher
  final Action confirmLauncher= action("confirmLauncher",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("lubuntu_lxqt", () -> waitClick(val(null), val(null), val(null))),//Execute, in the dialog asking to execute the launcher
    on("fedora_gnome", () -> waitAfter(val(null))),
    on("fedora_cosmic", () -> waitAfter(val(100))),
    on("debian_mate", () -> waitAfter(val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(100))));
  boolean filesOpen;
  void toPilotio() throws Throwable{
    showFilesIcon.go();
    (filesOpen ? fileManagerToFront : filesIcon).go();
    filesOpen = true;
    pilotioBookmark.go();
  }
  void closeFiles() throws Throwable{
    closeFileManager.go();
    filesOpen = false;
  }
  //the actions of start(), in this order: showFilesIcon, filesIcon (fileManagerToFront when a file manager window is already open), pilotioBookmark, openApp (openAppNextToManagerFolder when the manager folder fearless0_001 is next to the app), openBin, openLauncher, confirmLauncher.
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

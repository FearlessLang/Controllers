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
  static{ Err.setUp(AssertionFailedError.class,Assertions::assertEquals,Assertions::assertTrue); }
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
    Fs.copyFresh(ResolveResource.managedFolderOut.resolve(app.getFileName()),app);
    last= System.currentTimeMillis();
    try{ walk(); }
    finally{
      ProcessHandle.allProcesses().filter(this::ofApp).forEach(ProcessHandle::destroyForcibly);
      inGuest("pkill -KILL -f '^"+app+"/'; true");
    }
  }
  boolean ofApp(ProcessHandle p){ return p.info().command().filter(c->c.startsWith(app.toString())).isPresent(); }
  Action action(String name, On... ons){ return new Action(this,name,OneOr.of(name+" on "+desk,Stream.of(ons).filter(o->o.desk().equals(desk))).steps()); }
  static On on(String desk, Run steps){ return new On(desk,steps); }
  record Action(ManagerTest m, String name, Run run){
    void go() throws Throwable{
      if (m.agent){ ask(); }
      run.run();
      m.last= System.currentTimeMillis();
    }
    private void ask(){
      m.inputIndex= 0;
      Fs.cleanDir(m.channel);
      var file= m.channel.resolve(name);
      Fs.writeUtf8(file,"");
      var text= Fs.readUtf8(file).strip();
      for (; !text.endsWith(";"); text= Fs.readUtf8(file).strip()){ Pilot.pause(100); }
      m.inputs= text.substring(0,text.length()-1).lines().map(String::strip).map(Action::input).toList();
    }
    static Object input(String s){ return s.startsWith("\"") ? s.substring(1,s.length()-1) : Integer.valueOf(s); }
  }
  interface Run{ void run() throws Throwable; }
  record On(String desk, Run steps){}
  @SuppressWarnings("unchecked")
  <T> T val(T recorded){ return agent ? (T)inputs.get(inputIndex++) : recorded; }
  void waitAfter(int millis){
    var left= last+millis-System.currentTimeMillis();
    if (left>0){ Pilot.pause((int)left); }
  }
  void click(int x, int y){ pilot.click(Pilot.Button.left,x,y); }
  void doubleClick(int x, int y){ pilot.doubleClick(x,y); }
  void rightClick(int x, int y){ pilot.click(Pilot.Button.right,x,y); }
  void drag(int x0, int y0, int x1, int y1){ pilot.drag(x0,y0,x1,y1); }
  void type(String text){ text.chars().forEach(this::type); }
  void type(int c){
    var i= "~!@#$%^&*()_+{}|:\"<>?".indexOf(c);
    if (i>=0){ pilot.chord(KeyEvent.VK_SHIFT,KeyEvent.getExtendedKeyCodeForChar("`1234567890-=[]\\;',./".charAt(i))); return; }
    var code= KeyEvent.getExtendedKeyCodeForChar(c);
    if (Character.isUpperCase(c)){ pilot.chord(KeyEvent.VK_SHIFT,code); return; }
    pilot.chord(code);
  }
  void keys(int... codes){ pilot.chord(codes); }
  void stabilize(){ inGuest("sync; echo 3 >/proc/sys/vm/drop_caches"); }
  static void inGuest(String script){
    if (!Fs.isLinux()){ return; }
    OneOr.opt("one guest alive",virsh("list","--name").lines().filter(l->!l.isBlank())).ifPresent(g->inGuest(g,script));
  }
  static void inGuest(String guest, String script){
    var pid= virsh("qemu-agent-command",guest,"{\"execute\":\"guest-exec\",\"arguments\":{\"path\":\"/bin/sh\",\"arg\":[\"-c\",\""+script+"\"]}}").replaceAll("\\D","");
    var status= "{\"execute\":\"guest-exec-status\",\"arguments\":{\"pid\":"+pid+"}}";
    var done= virsh("qemu-agent-command",guest,status);
    for (; !done.contains("\"exited\":true"); done= virsh("qemu-agent-command",guest,status)){ Pilot.pause(100); }
    assert done.contains("\"exitcode\":0");
  }
  static String virsh(String... args){
    var p= Fs.of(()->new ProcessBuilder(Stream.concat(Stream.of("virsh","-c","qemu:///system"),Stream.of(args)).toList()).redirectErrorStream(true).start());
    var out= Fs.of(()->new String(p.getInputStream().readAllBytes(),StandardCharsets.UTF_8));
    assertEquals(0,p.onExit().join().exitValue(),out);
    return out;
  }
  void checkContent(Path file, String expected){ Err.strCmp(expected,Fs.readUtf8(file)); }
  static final String data= JavacTool.dataDirNameFor(ResolveResource.versionId);
  final Path info= filesIOFolder.resolve(data).resolve("projects.info");
  final Path app= filesIOFolder.resolve("fearlessManaged"+ResolveResource.versionId);
  final Path files= filesIOFolder.resolve("files");
  final Action managerMenu= action("managerMenu",
    on("ubuntu_gnome",()->waitClick(val(null),val(99),val(79))),
    on("xubuntu_xfce",()->waitClick(val(null),val(38),val(62))));
  final Action addFolder= action("addFolder",
    on("ubuntu_gnome",()->waitClick(val(null),val(110),val(103))),
    on("xubuntu_xfce",()->waitClick(val(null),val(43),val(84))));
  final Action editMetadata= action("editMetadata",
    on("ubuntu_gnome",()->waitClick(val(null),val(143),val(124))),
    on("xubuntu_xfce",()->waitClick(val(null),val(76),val(102))));
  final Action connectEclipse= action("connectEclipse",
    on("ubuntu_gnome",()->waitClick(val(null),val(126),val(145))),
    on("xubuntu_xfce",()->waitClick(val(null),val(59),val(122))));
  final Action resetManager= action("resetManager",
    on("ubuntu_gnome",()->waitClick(val(null),val(90),val(191))),
    on("xubuntu_xfce",()->waitClick(val(null),val(23),val(164))));
  final Action quitManager= action("quitManager",
    on("ubuntu_gnome",()->waitClick(val(null),val(88),val(212))),
    on("xubuntu_xfce",()->waitClick(val(null),val(21),val(182))));
  final Action closeWindow= action("closeWindow",
    on("ubuntu_gnome",()->waitClick(val(null),val(1901),val(49))),
    on("xubuntu_xfce",()->waitClick(val(null),val(1909),val(37))));
  final Action firstTile= action("firstTile",
    on("ubuntu_gnome",()->waitClick(val(null),val(133),val(130))),
    on("xubuntu_xfce",()->waitClick(val(null),val(66),val(110))));
  final Action trayIcon= action("trayIcon",
    on("ubuntu_gnome",()->waitClick(val(null),val(1736),val(15))),
    on("xubuntu_xfce",()->waitClick(val(null),val(1783),val(12))));
  final Action trayShow= action("trayShow",
    on("ubuntu_gnome",()->waitClick(val(null),val(1709),val(62))),
    on("xubuntu_xfce",()->waitClick(val(null),val(1717),val(41))));
  void park(){
    var s= Toolkit.getDefaultToolkit().getScreenSize();
    pilot.glide(s.width/2-40,s.height/2-40,Pilot.none,s.width/2,s.height/2,Pilot.none);
  }
  BufferedImage look(){ park(); return pilot.shot(); }
  static final Map<String,Point> content= Map.ofEntries(
    Map.entry("ubuntu_gnome",new Point(67,92)),
    Map.entry("xubuntu_xfce",new Point(0,72)));
  Rectangle inWindow(int x, int y, int w, int h){
    var at= content.get(desk);
    return new Rectangle(x-67+at.x,y-92+at.y,w,h);
  }
  Rectangle region;
  void region(int x, int y, int w, int h){ region= new Rectangle(x,y,w,h); }
  static int[] pixels(BufferedImage img, Rectangle r){ return img.getRGB(r.x,r.y,r.width,r.height,null,0,r.width); }
  static boolean near(int p, int q){ return IntStream.of(0,8,16).allMatch(s->Math.abs((p>>s&255)-(q>>s&255))<=16); }
  static long differing(BufferedImage a, BufferedImage b, Rectangle r){
    var pa= pixels(a,r);
    var pb= pixels(b,r);
    return IntStream.range(0,pa.length).filter(i->!near(pa[i],pb[i])).count();
  }
  void same(BufferedImage before, Rectangle r){ assertEquals(0L,differing(before,look(),r)); }
  void changed(BufferedImage before, Rectangle r){ assertNotEquals(0L,differing(before,look(),r)); }
  Path project(String name){
    var res= files.resolve(name);
    Fs.copyFresh(ResolveResource.integrationTests.resolve(name),res);
    Fs.rmTree(res.resolve(".fearless_out"));
    return res;
  }
  void waitClick(int millis, int x, int y){ waitAfter(millis); click(x,y); }
  void waitDoubleClick(int millis, int x, int y){ waitAfter(millis); doubleClick(x,y); }
  void waitRightClick(int millis, int x, int y){ waitAfter(millis); rightClick(x,y); }
  void waitDrag(int millis, int x0, int y0, int x1, int y1){ waitAfter(millis); drag(x0,y0,x1,y1); }
  final Action filesIcon= action("filesIcon",
    on("ubuntu_gnome",()->waitClick(val(100),val(33),val(128))),
    on("xubuntu_xfce",()->waitClick(val(null),val(null),val(null))));
  final Action pilotioBookmark= action("pilotioBookmark",
    on("ubuntu_gnome",()->waitClick(val(100),val(613),val(743))),
    on("xubuntu_xfce",()->waitClick(val(null),val(null),val(null))));
  final Action openApp= action("openApp",
    on("ubuntu_gnome",()->waitDoubleClick(val(100),val(798),val(375))),
    on("xubuntu_xfce",()->waitDoubleClick(val(null),val(null),val(null))));
  final Action openAppNextToManagerFolder= action("openAppNextToManagerFolder",
    on("ubuntu_gnome",()->waitDoubleClick(val(100),val(911),val(375))),
    on("xubuntu_xfce",()->waitDoubleClick(val(null),val(null),val(null))));
  final Action openBin= action("openBin",
    on("ubuntu_gnome",()->waitDoubleClick(val(100),val(798),val(378))),
    on("xubuntu_xfce",()->waitDoubleClick(val(null),val(null),val(null))));
  //double clicks fearlessManaged0_001w, the launcher whose name ends with w
  final Action openLauncher= action("openLauncher",
    on("ubuntu_gnome",()->waitDoubleClick(val(100),val(911),val(378))),
    on("xubuntu_xfce",()->waitDoubleClick(val(null),val(null),val(null))));
  final Action openFiles= action("openFiles",
    on("ubuntu_gnome",()->waitDoubleClick(val(100),val(1024),val(378))),
    on("xubuntu_xfce",()->waitDoubleClick(val(null),val(null),val(null))));
  final Action closeFileManager= action("closeFileManager",
    on("ubuntu_gnome",()->waitClick(val(100),val(1414),val(304))),
    on("xubuntu_xfce",()->waitClick(val(null),val(null),val(null))));
  void toPilotio() throws Throwable{
    filesIcon.go();
    pilotioBookmark.go();
  }
  //the actions of start(), in this order: filesIcon, pilotioBookmark, openApp (openAppNextToManagerFolder when the manager folder fearless0_001 is next to the app), openBin, openLauncher.
  //Before filesIcon the manager is not started; the manager window shows only after openLauncher.
  //start() leaves the file manager open, behind the manager window.
  void start() throws Throwable{
    toPilotio();
    (Files.exists(filesIOFolder.resolve(data)) ? openAppNextToManagerFolder : openApp).go();
    openBin.go();
    openLauncher.go();
  }
  void toFiles() throws Throwable{
    toPilotio();
    openFiles.go();
  }
  static String slashed(Path p){ return p.toString().replace('\\','/'); }
  static String escaped(Path p){ return p.toString().replace("\\","\\\\"); }
}

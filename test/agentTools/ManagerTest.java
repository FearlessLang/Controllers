package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import resources.ResolveResource;
import tools.Fs;
import tools.JavacTool;
import utils.Bug;
import utils.Err;
import utils.OneOr;

abstract class ManagerTest{
  static{ Err.setUp(AssertionFailedError.class,Assertions::assertEquals,Assertions::assertTrue); }
  final String desk= System.getProperty("desk");
  final boolean agent= Boolean.getBoolean("agent");
  final Path channel= Path.of(System.getProperty("channelFolder"));
  final Path filesIOFolder= Path.of(System.getProperty("filesIOFolder"));
  final Pilot pilot= new Pilot();
  long start;
  long looked;
  int inputIndex;
  List<Object> inputs;
  abstract void walk() throws Throwable;
  @Test void test() throws Throwable{
    start= System.currentTimeMillis();
    walk();
  }
  Action action(String name, On... ons){ return new Action(this,name,OneOr.of(name+" on "+desk,Stream.of(ons).filter(o->o.desk().equals(desk))).steps()); }
  static On on(String desk, Run steps){ return new On(desk,steps); }
  record Action(ManagerTest m, String name, Run run){
    void go() throws Throwable{
      if (!m.agent){ run.run(); return; }
      m.inputIndex= 0;
      Fs.cleanDir(m.channel);
      var file= m.channel.resolve(name);
      Fs.writeUtf8(file,"");
      var text= Fs.readUtf8(file).strip();
      for (; !text.endsWith(";"); text= Fs.readUtf8(file).strip()){ Pilot.pause(100); }
      m.inputs= text.substring(0,text.length()-1).lines().map(String::strip).map(Action::input).toList();
      run.run();
    }
    static Object input(String s){ return s.startsWith("\"") ? s.substring(1,s.length()-1) : Integer.valueOf(s); }
  }
  interface Run{ void run() throws Throwable; }
  record On(String desk, Run steps){}
  @SuppressWarnings("unchecked")
  <T> T val(T recorded){ return agent ? (T)inputs.get(inputIndex++) : recorded; }
  void waitUntilTime(int time){
    var left= start+looked+time-System.currentTimeMillis();
    if (left>0){ Pilot.pause((int)left); }
  }
  final Point clicked= new Point();
  void click(int x, int y){ clicked.setLocation(x,y); pilot.click(x,y); }
  void doubleClick(int x, int y){ pilot.doubleClick(x,y); }
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
  void stabilize(){
    if (!Fs.isLinux()){ return; }
    OneOr.opt("one guest alive",virsh("list","--name").lines().filter(l->!l.isBlank())).ifPresent(ManagerTest::flush);
  }
  static void flush(String guest){
    var pid= virsh("qemu-agent-command",guest,"{\"execute\":\"guest-exec\",\"arguments\":{\"path\":\"/bin/sh\",\"arg\":[\"-c\",\"sync; echo 3 >/proc/sys/vm/drop_caches\"]}}").replaceAll("\\D","");
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
  void checkContent(List<String> path, String expected){ Err.strCmp(expected,Fs.readUtf8(filesIOFolder.resolve(String.join("/",path)))); }
  BufferedImage look(){
    var begin= System.currentTimeMillis();
    var s= Toolkit.getDefaultToolkit().getScreenSize();
    pilot.glide(s.width/2-40,s.height/2-40,Pilot.none,s.width/2,s.height/2,Pilot.none);
    var res= pilot.shot();
    looked+= System.currentTimeMillis()-begin;
    return res;
  }
  Rectangle changed(String what, BufferedImage before, BufferedImage after){ return changed(what,before,after,6); }
  Rectangle changed(String what, BufferedImage before, BufferedImage after, int r){
    var begin= System.currentTimeMillis();
    try{ return Pilot.changed(before,after,r); }
    catch(AssertionError e){ throw new AssertionFailedError(what+": the screen looks as before"); }
    finally{ looked+= System.currentTimeMillis()-begin; }
  }
  void same(String what, BufferedImage a, BufferedImage b, Rectangle at){ assertEquals(0L,mismatches(a,b,at),what+": the screen differs in "+at); }
  void differ(String what, BufferedImage a, BufferedImage b, Rectangle at){ assertTrue(mismatches(a,b,at)>50,what+": the screen is the same in "+at); }
  long mismatches(BufferedImage a, BufferedImage b, Rectangle at){
    var begin= System.currentTimeMillis();
    var pa= a.getRGB(at.x,at.y,at.width,at.height,null,0,at.width);
    var pb= b.getRGB(at.x,at.y,at.width,at.height,null,0,at.width);
    var res= IntStream.range(0,pa.length).filter(i->!near(pa[i],pb[i])).count();
    looked+= System.currentTimeMillis()-begin;
    return res;
  }
  static boolean near(int p, int q){ return Math.abs((p&255)-(q&255))<=16 && Math.abs((p>>8&255)-(q>>8&255))<=16 && Math.abs((p>>16&255)-(q>>16&255))<=16; }
  Rectangle whiteAround(BufferedImage shot){
    var s= Toolkit.getDefaultToolkit().getScreenSize();
    return whiteAround(shot,s.width/2,s.height/2);
  }
  Rectangle whiteAround(BufferedImage shot, int x, int y){
    int l= x, r= x, t= y, b= y;
    for (; (shot.getRGB(l-1,y)&0xffffff)==0xffffff; l--){}
    for (; (shot.getRGB(r+1,y)&0xffffff)==0xffffff; r++){}
    for (; (shot.getRGB(x,t-1)&0xffffff)==0xffffff; t--){}
    for (; (shot.getRGB(x,b+1)&0xffffff)==0xffffff; b++){}
    return new Rectangle(l,t,r-l+1,b-t+1);
  }
  void plain(String what, BufferedImage shot, Rectangle at){
    var px= shot.getRGB(at.x,at.y,at.width,at.height,null,0,at.width);
    assertEquals(0L,IntStream.of(px).filter(p->!near(p,px[0])).count(),what+": the screen is not plain in "+at);
  }
  final boolean hostDesk= List.of("ubuntu_gnome","windows").contains(desk);
  void clearClipboard(){ if (hostDesk){ Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection("nothing was copied"),null); } }
  void clipboardIs(String expected) throws Throwable{ if (hostDesk){ Err.strCmp(expected,(String)Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor)); } }
  void copied(String expected) throws Throwable{
    clearClipboard();
    keys(KeyEvent.VK_CONTROL,KeyEvent.VK_A);
    keys(KeyEvent.VK_CONTROL,KeyEvent.VK_C);
    clipboardIs(expected);
  }
  static final String data= JavacTool.dataDirNameFor(ResolveResource.versionId);
  static final List<String> info= List.of(data,"projects.info");
  static final List<String> state= List.of(data,"eclipse","state.info");
  static final List<String> notes= List.of(data,"eclipse","console.txt");
  final Path app= filesIOFolder.resolve("fearlessManaged"+ResolveResource.versionId);
  final Action showApps= action("showApps",on("ubuntu_gnome",()->click(val(34),val(16))),on("debian_gnome_x11",()->click(val(1920),val(1080))),on("xubuntu_xfce",()->click(val(1920),val(1080))),on("debian_cinnamon",()->click(val(1920),val(1000))),on("debian_mate",()->click(val(1920),val(1080))),on("lubuntu_lxqt",()->click(val(1920),val(1080))),on("kubuntu_plasma",()->click(val(1920),val(1200))),on("void_i3",()->click(val(1920),val(1080))),on("arch_sway",()->click(val(1920),val(1080))),on("omarchy_hyprland",()->click(val(1920),val(1080))),on("fedora_gnome",()->doubleClick(val(1920),val(1080))),on("opensuse_plasma",()->click(val(1920),val(1080))),on("fedora_cosmic",()->click(val(1920),val(1000))),on("windows",ManagerTest::unrecorded));
  final Action openTerminal= action("openTerminal",on("ubuntu_gnome",()->type("terminal\n")),on("debian_gnome_x11",()->keys(KeyEvent.VK_CONTROL,KeyEvent.VK_ALT,KeyEvent.VK_T)),on("xubuntu_xfce",()->keys(KeyEvent.VK_CONTROL,KeyEvent.VK_ALT,KeyEvent.VK_T)),on("debian_cinnamon",()->keys(KeyEvent.VK_CONTROL,KeyEvent.VK_ALT,KeyEvent.VK_T)),on("debian_mate",()->keys(KeyEvent.VK_CONTROL,KeyEvent.VK_ALT,KeyEvent.VK_T)),on("lubuntu_lxqt",()->keys(KeyEvent.VK_CONTROL,KeyEvent.VK_ALT,KeyEvent.VK_T)),on("kubuntu_plasma",()->keys(KeyEvent.VK_CONTROL,KeyEvent.VK_ALT,KeyEvent.VK_T)),on("void_i3",()->keys(KeyEvent.VK_CONTROL,KeyEvent.VK_ALT,KeyEvent.VK_T)),on("arch_sway",()->keys(KeyEvent.VK_CONTROL,KeyEvent.VK_ALT,KeyEvent.VK_T)),on("omarchy_hyprland",()->keys(KeyEvent.VK_CONTROL,KeyEvent.VK_ALT,KeyEvent.VK_T)),on("fedora_gnome",()->keys(KeyEvent.VK_CONTROL,KeyEvent.VK_ALT,KeyEvent.VK_T)),on("opensuse_plasma",()->keys(KeyEvent.VK_CONTROL,KeyEvent.VK_ALT,KeyEvent.VK_T)),on("fedora_cosmic",()->keys(KeyEvent.VK_CONTROL,KeyEvent.VK_ALT,KeyEvent.VK_T)),on("windows",ManagerTest::unrecorded));
  final Action runShell= action("runShell",on("ubuntu_gnome",this::typeRunSh),on("debian_gnome_x11",this::typeRunSh),on("xubuntu_xfce",this::typeRunSh),on("debian_cinnamon",this::typeRunSh),on("debian_mate",this::typeRunSh),on("lubuntu_lxqt",this::typeRunSh),on("kubuntu_plasma",this::typeRunSh),on("void_i3",this::typeRunSh),on("arch_sway",this::typeRunSh),on("omarchy_hyprland",this::typeRunSh),on("fedora_gnome",this::typeRunSh),on("opensuse_plasma",this::typeRunSh),on("fedora_cosmic",this::typeRunSh),on("windows",ManagerTest::unrecorded));
  void typeRunSh(){ type("sh "+filesIOFolder.resolve("run.sh")+"; exit\n"); }
  static void unrecorded(){ throw Bug.todo(); }
  Path project(String name){
    var res= filesIOFolder.resolve(name);
    Fs.copyFresh(ResolveResource.integrationTests.resolve(name),res);
    Fs.rmTree(res.resolve(".fearless_out"));
    return res;
  }
  void noManagerData(){ Fs.rmTree(filesIOFolder.resolve(data)); }
  void shell(String script){
    Fs.writeUtf8(filesIOFolder.resolve("run.sh"),script);
    stabilize();
  }
  void launchScript(String exit, Object... args){
    Fs.rmTree(filesIOFolder.resolve(exit+".exit"));
    var line= Stream.concat(Stream.of(app.resolve("bin").resolve(app.getFileName().toString())),Stream.of(args)).map(a->"\""+a+"\"").collect(Collectors.joining(" "));
    shell("nohup setsid -f sh -c '"+line+"; echo $? >"+filesIOFolder.resolve(exit+".exit")+"' >/dev/null 2>&1\n");
  }
  void endScript(){ shell("pkill -KILL -f '^"+app+"/'\n"); }
  void runInTerminal(Action appsShown, Action terminalShown) throws Throwable{
    showApps.go();
    appsShown.go();
    openTerminal.go();
    terminalShown.go();
    runShell.go();
  }
  static String slashed(Path p){ return p.toString().replace('\\','/'); }
  static String escaped(Path p){ return p.toString().replace("\\","\\\\"); }
}

package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import tools.Fs;

final class SlowSurfacingTest extends ManagerTest{
  static final String remembered= """
    {
      "hello_world": {
        "path": "Str:%s",
        "kind": "idle"
      }
    }
    """;
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action noticeShown= action("noticeShown",
    on("ubuntu_gnome",()->waitUntilTime(val(18000))));
  final Action windowHeld= action("windowHeld",
    on("ubuntu_gnome",()->listEnd(val(200),val(1500),val(200))));
  final Action noticeLooks= action("noticeLooks",
    on("ubuntu_gnome",()->pixelIs(val(1790),val(1187),val("eeeeee"))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(21500))));
  final Action tilesEnd= action("tilesEnd",
    on("ubuntu_gnome",()->listEnd(val(200),val(1500),val(380))));
  final Action noticeGone= action("noticeGone",
    on("ubuntu_gnome",()->pixelIs(val(1790),val(1187),val("ffffff"))));
  final Action closeManager= action("closeManager",
    on("ubuntu_gnome",()->click(val(3822),val(50))));
  final Action managerHidden= action("managerHidden",
    on("ubuntu_gnome",()->waitUntilTime(val(26000))));
  final Action tilesGone= action("tilesGone",
    on("ubuntu_gnome",()->listEnd(val(200),val(1500),val(200))));
  final Action appsShownSecond= action("appsShownSecond",
    on("ubuntu_gnome",()->waitUntilTime(val(28500))));
  final Action terminalShownSecond= action("terminalShownSecond",
    on("ubuntu_gnome",()->waitUntilTime(val(32000))));
  final Action noticeShownSecond= action("noticeShownSecond",
    on("ubuntu_gnome",()->waitUntilTime(val(43000))));
  final Action quitNotice= action("quitNotice",
    on("ubuntu_gnome",()->click(val(1955),val(1187))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",()->waitUntilTime(val(46500))));
  @Override void walk() throws Throwable{
    noManagerData();
    var project= project("helloWorld");
    Fs.rmTree(filesIOFolder.resolve("resume"));
    try{ held(project); }
    finally{ resume(); }
  }
  void held(Path project) throws Throwable{
    heldLaunch("first",project);
    runInTerminal(appsShown,terminalShown);
    noticeShown.go();
    stabilize();
    managerKept(project);
    windowHeld.go();
    noticeLooks.go();
    resume();
    managerShown.go();
    tilesEnd.go();
    noticeGone.go();
    closeManager.go();
    managerHidden.go();
    tilesGone.go();
    stabilize();
    assertFalse(Files.exists(filesIOFolder.resolve("first.exit")));
    heldLaunch("second");
    runInTerminal(appsShownSecond,terminalShownSecond);
    noticeShownSecond.go();
    stabilize();
    checkContent(List.of("second.exit"),"0\n");
    managerKept(project);
    windowHeld.go();
    noticeLooks.go();
    quitNotice.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
    managerKept(project);
  }
  void heldLaunch(String exit, Object... args){
    launchScript(exit,args);
    var resume= filesIOFolder.resolve("resume");
    shell("""
      helper=$(pgrep -f '^/usr/libexec/mutter-x11-frames$' | tr '\\n' ' ')
      kill -STOP $helper
      HELPER="$helper" nohup setsid -f sh -c 'for i in $(seq 1200); do [ -e %s ] && break; sleep 0.1; done; kill -CONT $HELPER; rm -f %s' >/dev/null 2>&1
      """.formatted(resume,resume)+Fs.readUtf8(filesIOFolder.resolve("run.sh")));
  }
  void resume(){
    Fs.writeUtf8(filesIOFolder.resolve("resume"),"");
    stabilize();
  }
  void managerKept(Path project){
    assertTrue(Files.exists(filesIOFolder.resolve(data).resolve("projects.info")),"The manager deleted its manager folder while the desktop was slow to show its window");
    checkContent(info,remembered.formatted(slashed(project)));
  }
  void listEnd(int x, int y, int end){
    var shot= pilot.shot();
    var at= x;
    for (; at<shot.getWidth() && (shot.getRGB(at,y)&0xffffff)==0xffffff; at++){}
    assertEquals(end,at,"The tile list, white along the row y="+y+" from x="+x+", ends at x="+at);
  }
  void pixelIs(int x, int y, String rgb){
    assertEquals(rgb,"%06x".formatted(pilot.shot().getRGB(x,y)&0xffffff),"The screen at ("+x+","+y+")");
  }
}

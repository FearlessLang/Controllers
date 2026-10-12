package agentTools;

import java.awt.Rectangle;
import java.util.List;

final class KindWhileBusyTest extends ManagerTest{
  final Action becomeCode= header("becomeCode",970);
  final Action appsShown= action("appsShown",on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",on("ubuntu_gnome",()->waitUntilTime(val(18000))));
  final Action codeShown= action("codeShown",on("ubuntu_gnome",()->waitUntilTime(val(21000))));
  final Action compile= action("compile",on("ubuntu_gnome",()->click(val(523),val(113))));
  final Action compiled= action("compiled",on("ubuntu_gnome",()->waitUntilTime(val(36000))));
  final Action run= action("run",on("ubuntu_gnome",()->click(val(523),val(113))));
  final Action programShown= action("programShown",on("ubuntu_gnome",()->waitUntilTime(val(42000))));
  final Action backToIdleBusy= action("backToIdleBusy",on("ubuntu_gnome",()->click(val(733),val(113))));
  final Action forgetBusy= action("forgetBusy",on("ubuntu_gnome",()->click(val(892),val(113))));
  final Action ignored= action("ignored",on("ubuntu_gnome",()->waitUntilTime(val(45000))));
  final Action terminate= action("terminate",on("ubuntu_gnome",()->click(val(523),val(113))));
  final Action terminated= action("terminated",on("ubuntu_gnome",()->waitUntilTime(val(48000))));
  final Action managerQuit= action("managerQuit",on("ubuntu_gnome",()->waitUntilTime(val(53000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var gui= project("testGui1");
    launchScript("first",gui.resolve("start.fearless"));
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    becomeCode.go();
    codeShown.go();
    compile.go();
    compiled.go();
    var code= """
      {
        "projects": {
          "start": {
            "path": "Str:%s",
            "kind": "code"
          }
        },
        "systemExtensions": []
      }
      """.formatted(slashed(gui));
    var header= new Rectangle(560,100,600,26);
    var center= new Rectangle(1720,1000,400,160);
    var greyed= new Rectangle(693,102,330,22);
    var compiledLook= look();
    run.go();
    programShown.go();
    var running= look();
    changed(compiledLook,header);
    backToIdleBusy.go();
    forgetBusy.go();
    ignored.go();
    same(running,greyed);
    same(compiledLook,center);
    stabilize();
    checkContent(info,code);
    terminate.go();
    terminated.go();
    same(compiledLook,header);
    stabilize();
    checkContent(info,code);
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
  }
}

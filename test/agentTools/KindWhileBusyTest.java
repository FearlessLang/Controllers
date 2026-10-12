package agentTools;

import java.awt.Rectangle;
import java.util.List;

final class KindWhileBusyTest extends ManagerTest{
  final Action becomeCode= action("becomeCode",
    on("ubuntu_gnome",()->click(val(970),val(113))),
    on("xubuntu_xfce",()->click(val(976),val(93))));
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(3500))),
    on("xubuntu_xfce",()->waitUntilTime(val(4500))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(8500))),
    on("xubuntu_xfce",()->waitUntilTime(val(11000))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(26000))),
    on("xubuntu_xfce",()->waitUntilTime(val(32500))));
  final Action codeShown= action("codeShown",
    on("ubuntu_gnome",()->waitUntilTime(val(30500))),
    on("xubuntu_xfce",()->waitUntilTime(val(38500))));
  final Action compile= action("compile",
    on("ubuntu_gnome",()->click(val(523),val(113))),
    on("xubuntu_xfce",()->click(val(481),val(93))));
  final Action compiled= action("compiled",
    on("ubuntu_gnome",()->waitUntilTime(val(51500))),
    on("xubuntu_xfce",()->waitUntilTime(val(64500))));
  final Action run= action("run",
    on("ubuntu_gnome",()->click(val(523),val(113))),
    on("xubuntu_xfce",()->click(val(466),val(93))));
  final Action programShown= action("programShown",
    on("ubuntu_gnome",()->waitUntilTime(val(60000))),
    on("xubuntu_xfce",()->waitUntilTime(val(75000))));
  final Action backToIdleBusy= action("backToIdleBusy",
    on("ubuntu_gnome",()->click(val(733),val(113))),
    on("xubuntu_xfce",()->click(val(701),val(93))));
  final Action forgetBusy= action("forgetBusy",
    on("ubuntu_gnome",()->click(val(892),val(113))),
    on("xubuntu_xfce",()->click(val(878),val(93))));
  final Action ignored= action("ignored",
    on("ubuntu_gnome",()->waitUntilTime(val(64500))),
    on("xubuntu_xfce",()->waitUntilTime(val(81000))));
  final Action terminate= action("terminate",
    on("ubuntu_gnome",()->click(val(523),val(113))),
    on("xubuntu_xfce",()->click(val(489),val(93))));
  final Action terminated= action("terminated",
    on("ubuntu_gnome",()->waitUntilTime(val(69000))),
    on("xubuntu_xfce",()->waitUntilTime(val(86500))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",()->waitUntilTime(val(76000))),
    on("xubuntu_xfce",()->waitUntilTime(val(95000))));
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
    var button= inWindow(508,102,30,20);
    var center= new Rectangle(760,460,400,160);
    var greyed= inWindow(720,102,197,22);
    var compiledLook= look();
    run.go();
    programShown.go();
    var running= look();
    changed(compiledLook,button);
    backToIdleBusy.go();
    forgetBusy.go();
    ignored.go();
    same(running,greyed);
    same(compiledLook,center);
    stabilize();
    checkContent(info,code);
    terminate.go();
    terminated.go();
    same(compiledLook,button);
    stabilize();
    checkContent(info,code);
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
  }
}

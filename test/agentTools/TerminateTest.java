package agentTools;

import java.awt.Rectangle;
import java.util.List;

final class TerminateTest extends ManagerTest{
  final Action becomeCode= header("becomeCode",970);
  final Action appsShown= action("appsShown",on("ubuntu_gnome",()->waitUntilTime(val(3500))));
  final Action terminalShown= action("terminalShown",on("ubuntu_gnome",()->waitUntilTime(val(8500))));
  final Action managerShown= action("managerShown",on("ubuntu_gnome",()->waitUntilTime(val(26000))));
  final Action codeShown= action("codeShown",on("ubuntu_gnome",()->waitUntilTime(val(30500))));
  final Action compile= action("compile",on("ubuntu_gnome",()->click(val(523),val(113))));
  final Action compiled= action("compiled",on("ubuntu_gnome",()->waitUntilTime(val(51500))));
  final Action run= action("run",on("ubuntu_gnome",()->click(val(523),val(113))));
  final Action programShown= action("programShown",on("ubuntu_gnome",()->waitUntilTime(val(60000))));
  final Action terminate= action("terminate",on("ubuntu_gnome",()->click(val(523),val(113))));
  final Action terminated= action("terminated",on("ubuntu_gnome",()->waitUntilTime(val(64500))));
  final Action managerQuit= action("managerQuit",on("ubuntu_gnome",()->waitUntilTime(val(71500))));
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
    var program= new Rectangle(930,500,120,120);
    var before= look();
    run.go();
    programShown.go();
    changed(before,program);
    terminate.go();
    terminated.go();
    same(before,program);
    stabilize();
    checkContent(info,"""
      {
        "projects": {
          "start": {
            "path": "Str:%s",
            "kind": "code"
          }
        },
        "systemExtensions": []
      }
      """.formatted(slashed(gui)));
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
  }
}

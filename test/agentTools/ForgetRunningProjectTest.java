package agentTools;

import java.awt.Rectangle;
import java.util.List;

final class ForgetRunningProjectTest extends ManagerTest{
  final Action appsShown= action("appsShown",on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",on("ubuntu_gnome",()->waitUntilTime(val(18000))));
  final Action codeShown= action("codeShown",on("ubuntu_gnome",()->waitUntilTime(val(21000))));
  final Action compile= action("compile",on("ubuntu_gnome",()->click(val(520),val(112))));
  final Action compiled= action("compiled",on("ubuntu_gnome",()->waitUntilTime(val(36000))));
  final Action run= action("run",on("ubuntu_gnome",()->click(val(520),val(112))));
  final Action programShown= action("programShown",on("ubuntu_gnome",()->waitUntilTime(val(42000))));
  final Action refusalShown= action("refusalShown",on("ubuntu_gnome",()->waitUntilTime(val(45000))));
  final Action terminate= action("terminate",on("ubuntu_gnome",()->click(val(520),val(112))));
  final Action terminated= action("terminated",on("ubuntu_gnome",()->waitUntilTime(val(49000))));
  final Action forgotten= action("forgotten",on("ubuntu_gnome",()->waitUntilTime(val(52000))));
  final Action managerQuit= action("managerQuit",on("ubuntu_gnome",()->waitUntilTime(val(57000))));
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
    run.go();
    programShown.go();
    var running= look();
    forget.go();
    refusalShown.go();
    var center= new Rectangle(1720,1040,150,80);
    changed(running,center);
    stabilize();
    checkContent(info,code);
    closeDialog.go();
    terminate.go();
    terminated.go();
    forget.go();
    forgotten.go();
    stabilize();
    checkContent(info,"{\n  \"projects\": {},\n  \"systemExtensions\": []\n}\n");
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
  }
}

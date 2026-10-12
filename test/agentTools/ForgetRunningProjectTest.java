package agentTools;

import java.awt.Rectangle;
import java.util.List;

final class ForgetRunningProjectTest extends ManagerTest{
  final Action becomeCode= header("becomeCode",970);
  final Action forget= header("forget",873);
  final Action appsShown= action("appsShown",on("ubuntu_gnome",()->waitUntilTime(val(3500))));
  final Action terminalShown= action("terminalShown",on("ubuntu_gnome",()->waitUntilTime(val(8500))));
  final Action managerShown= action("managerShown",on("ubuntu_gnome",()->waitUntilTime(val(26000))));
  final Action codeShown= action("codeShown",on("ubuntu_gnome",()->waitUntilTime(val(30500))));
  final Action compile= action("compile",on("ubuntu_gnome",()->click(val(523),val(113))));
  final Action compiled= action("compiled",on("ubuntu_gnome",()->waitUntilTime(val(51500))));
  final Action run= action("run",on("ubuntu_gnome",()->click(val(523),val(113))));
  final Action programShown= action("programShown",on("ubuntu_gnome",()->waitUntilTime(val(60000))));
  final Action ignored= action("ignored",on("ubuntu_gnome",()->waitUntilTime(val(64500))));
  final Action terminate= action("terminate",on("ubuntu_gnome",()->click(val(523),val(113))));
  final Action terminated= action("terminated",on("ubuntu_gnome",()->waitUntilTime(val(70000))));
  final Action forgotten= action("forgotten",on("ubuntu_gnome",()->waitUntilTime(val(74500))));
  final Action managerQuit= action("managerQuit",on("ubuntu_gnome",()->waitUntilTime(val(81500))));
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
    var center= new Rectangle(760,460,400,160);
    var greyed= new Rectangle(693,102,330,22);
    var compiledLook= look();
    run.go();
    programShown.go();
    var running= look();
    forget.go();
    ignored.go();
    same(running,greyed);
    same(compiledLook,center);
    stabilize();
    checkContent(info,code);
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

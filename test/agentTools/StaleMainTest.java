package agentTools;

import java.awt.Rectangle;
import java.util.List;

import tools.Fs;

final class StaleMainTest extends ManagerTest{
  static final String mains= "use base.Main as Main;\n\nFirst: Main{sys -> sys.out.println \"First\"}\nSecond: Main{sys -> sys.out.println \"Second\"}\n";
  static final String registry= """
    {
      "projects": {
        "menu": {
          "path": "Str:%s",
          "kind": "code",
          "mains": [%s]
        }
      },
      "systemExtensions": []
    }
    """;
  final Action appsShown= action("appsShown",on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",on("ubuntu_gnome",()->waitUntilTime(val(18000))));
  final Action codeShown= action("codeShown",on("ubuntu_gnome",()->waitUntilTime(val(21000))));
  final Action compile= action("compile",on("ubuntu_gnome",()->click(val(530),val(112))));
  final Action compiled= action("compiled",on("ubuntu_gnome",()->waitUntilTime(val(36000))));
  final Action runV= action("runV",on("ubuntu_gnome",()->click(val(530),val(112))));
  final Action mainsShown= action("mainsShown",on("ubuntu_gnome",()->waitUntilTime(val(37500))));
  final Action tickSecond= action("tickSecond",on("ubuntu_gnome",()->click(val(540),val(166))));
  final Action tickThird= action("tickThird",on("ubuntu_gnome",()->click(val(540),val(190))));
  final Action runTicked= action("runTicked",on("ubuntu_gnome",()->click(val(530),val(215))));
  final Action runsDone= action("runsDone",on("ubuntu_gnome",()->waitUntilTime(val(44000))));
  final Action editNoticed= action("editNoticed",on("ubuntu_gnome",()->waitUntilTime(val(52000))));
  final Action compileEdited= action("compileEdited",on("ubuntu_gnome",()->click(val(530),val(112))));
  final Action editedCompiled= action("editedCompiled",on("ubuntu_gnome",()->waitUntilTime(val(67000))));
  final Action managerQuit= action("managerQuit",on("ubuntu_gnome",()->waitUntilTime(val(72000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var menu= filesIOFolder.resolve("menu");
    var source= menu.resolve("_menu").resolve("_rank_app.fear");
    Fs.rmTree(menu);
    Fs.writeUtf8(menu.resolve("menu.fearless"),"\n");
    Fs.writeUtf8(source,mains+"Third: Main{sys -> sys.out.println \"Third\"}\n");
    launchScript("first",menu.resolve("menu.fearless"));
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    becomeCode.go();
    codeShown.go();
    compile.go();
    compiled.go();
    runV.go();
    mainsShown.go();
    tickSecond.go();
    tickThird.go();
    runTicked.go();
    runsDone.go();
    stabilize();
    checkContent(info,registry.formatted(slashed(menu),"\"menu.Second\", \"menu.Third\""));
    var button= new Rectangle(500,100,160,24);
    var runnable= look();
    Fs.writeUtf8(source,mains);
    stabilize();
    editNoticed.go();
    changed(runnable,button);
    compileEdited.go();
    editedCompiled.go();
    same(runnable,button);
    stabilize();
    checkContent(info,registry.formatted(slashed(menu),"\"menu.Second\""));
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
  }
}

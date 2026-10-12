package agentTools;

import java.util.List;

import tools.Fs;

final class StaleMainTest extends ManagerTest{
  final Action becomeCode= action("becomeCode",
    on("ubuntu_gnome",()->click(val(979),val(113))),
    on("xubuntu_xfce",()->click(val(982),val(93))));
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
    on("ubuntu_gnome",()->click(val(532),val(113))),
    on("xubuntu_xfce",()->click(val(487),val(93))));
  final Action compiled= action("compiled",
    on("ubuntu_gnome",()->waitUntilTime(val(51500))),
    on("xubuntu_xfce",()->waitUntilTime(val(64500))));
  final Action runV= action("runV",
    on("ubuntu_gnome",()->click(val(532),val(113))),
    on("xubuntu_xfce",()->click(val(480),val(93))));
  final Action mainsShown= action("mainsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(54500))),
    on("xubuntu_xfce",()->waitUntilTime(val(68500))));
  final Action tickSecond= action("tickSecond",
    on("ubuntu_gnome",()->click(val(526),val(164))),
    on("xubuntu_xfce",()->click(val(454),val(141))));
  final Action tickThird= action("tickThird",
    on("ubuntu_gnome",()->click(val(526),val(189))),
    on("xubuntu_xfce",()->click(val(454),val(164))));
  final Action runTicked= action("runTicked",
    on("ubuntu_gnome",()->click(val(535),val(213))),
    on("xubuntu_xfce",()->click(val(466),val(186))));
  final Action runsDone= action("runsDone",
    on("ubuntu_gnome",()->waitUntilTime(val(63500))),
    on("xubuntu_xfce",()->waitUntilTime(val(79500))));
  final Action editNoticed= action("editNoticed",
    on("ubuntu_gnome",()->waitUntilTime(val(74500))),
    on("xubuntu_xfce",()->waitUntilTime(val(93500))));
  final Action compileEdited= action("compileEdited",
    on("ubuntu_gnome",()->click(val(532),val(113))),
    on("xubuntu_xfce",()->click(val(487),val(93))));
  final Action editedCompiled= action("editedCompiled",
    on("ubuntu_gnome",()->waitUntilTime(val(95500))),
    on("xubuntu_xfce",()->waitUntilTime(val(119500))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",()->waitUntilTime(val(102500))),
    on("xubuntu_xfce",()->waitUntilTime(val(128500))));
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
    var button= inWindow(517,102,30,20);
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

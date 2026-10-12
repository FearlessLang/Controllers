package agentTools;

import java.awt.Rectangle;
import java.util.List;

import tools.Fs;

final class TerminateDuringRunSelectedTest extends ManagerTest{
  final Action becomeCode= action("becomeCode",
    on("ubuntu_gnome",()->click(val(975),val(113))),
    on("xubuntu_xfce",()->click(val(980),val(93))));
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
    on("ubuntu_gnome",()->click(val(528),val(113))),
    on("xubuntu_xfce",()->click(val(486),val(93))));
  final Action compiled= action("compiled",
    on("ubuntu_gnome",()->waitUntilTime(val(51500))),
    on("xubuntu_xfce",()->waitUntilTime(val(64500))));
  final Action runV= action("runV",
    on("ubuntu_gnome",()->click(val(528),val(113))),
    on("xubuntu_xfce",()->click(val(478),val(93))));
  final Action mainsShown= action("mainsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(54500))),
    on("xubuntu_xfce",()->waitUntilTime(val(68500))));
  final Action tickShow= action("tickShow",
    on("ubuntu_gnome",()->click(val(522),val(139))),
    on("xubuntu_xfce",()->click(val(453),val(118))));
  final Action tickTell= action("tickTell",
    on("ubuntu_gnome",()->click(val(522),val(164))),
    on("xubuntu_xfce",()->click(val(453),val(141))));
  final Action runTicked= action("runTicked",
    on("ubuntu_gnome",()->click(val(531),val(188))),
    on("xubuntu_xfce",()->click(val(464),val(163))));
  final Action programShown= action("programShown",
    on("ubuntu_gnome",()->waitUntilTime(val(65000))),
    on("xubuntu_xfce",()->waitUntilTime(val(81500))));
  final Action terminate= action("terminate",
    on("ubuntu_gnome",()->click(val(528),val(113))),
    on("xubuntu_xfce",()->click(val(494),val(93))));
  final Action terminated= action("terminated",
    on("ubuntu_gnome",()->waitUntilTime(val(69500))),
    on("xubuntu_xfce",()->waitUntilTime(val(87000))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",()->waitUntilTime(val(76500))),
    on("xubuntu_xfce",()->waitUntilTime(val(96000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var chain= filesIOFolder.resolve("chain");
    Fs.rmTree(chain);
    Fs.writeUtf8(chain.resolve("chain.fearless"),"\n");
    Fs.writeUtf8(chain.resolve("_chain").resolve("_rank_app.fear"),"""
      use base.Main as Main;
      use base.Consumer as Consumer;
      use base.Frame as Frame;
      Show: Main{sys -> sys.gui.run Note}
      Tell: Main{sys -> sys.out.println("told")}
      Note: Consumer[mut Frame]{:: .title "chain" .content{:: .label{:: .text "terminate this program" } } }
      """);
    launchScript("first",chain.resolve("chain.fearless"));
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    becomeCode.go();
    codeShown.go();
    compile.go();
    compiled.go();
    var program= new Rectangle(920,510,150,94);
    var output= inWindow(410,168,1000,80);
    var before= look();
    runV.go();
    mainsShown.go();
    tickShow.go();
    tickTell.go();
    runTicked.go();
    programShown.go();
    changed(before,program);
    var running= look();
    terminate.go();
    terminated.go();
    same(before,program);
    stabilize();
    checkContent(info,"""
      {
        "projects": {
          "chain": {
            "path": "Str:%s",
            "kind": "code",
            "mains": ["chain.Show", "chain.Tell"]
          }
        },
        "systemExtensions": []
      }
      """.formatted(slashed(chain)));
    assert differing(running,look(),output) > 0;
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
  }
}

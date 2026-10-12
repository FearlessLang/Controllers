package agentTools;

import java.awt.Rectangle;
import java.util.List;

import tools.Fs;

final class TerminateDuringRunSelectedTest extends ManagerTest{
  final Action becomeCode= header("becomeCode",975);
  final Action appsShown= action("appsShown",on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",on("ubuntu_gnome",()->waitUntilTime(val(18000))));
  final Action codeShown= action("codeShown",on("ubuntu_gnome",()->waitUntilTime(val(21000))));
  final Action compile= action("compile",on("ubuntu_gnome",()->click(val(528),val(113))));
  final Action compiled= action("compiled",on("ubuntu_gnome",()->waitUntilTime(val(36000))));
  final Action runV= action("runV",on("ubuntu_gnome",()->click(val(528),val(113))));
  final Action mainsShown= action("mainsShown",on("ubuntu_gnome",()->waitUntilTime(val(37500))));
  final Action tickShow= action("tickShow",on("ubuntu_gnome",()->click(val(522),val(139))));
  final Action tickTell= action("tickTell",on("ubuntu_gnome",()->click(val(522),val(164))));
  final Action runTicked= action("runTicked",on("ubuntu_gnome",()->click(val(531),val(188))));
  final Action programShown= action("programShown",on("ubuntu_gnome",()->waitUntilTime(val(45000))));
  final Action terminate= action("terminate",on("ubuntu_gnome",()->click(val(528),val(113))));
  final Action terminated= action("terminated",on("ubuntu_gnome",()->waitUntilTime(val(48000))));
  final Action managerQuit= action("managerQuit",on("ubuntu_gnome",()->waitUntilTime(val(53000))));
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
    var program= new Rectangle(1880,1050,150,94);
    var output= new Rectangle(410,168,1000,80);
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

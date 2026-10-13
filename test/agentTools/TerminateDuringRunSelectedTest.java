package agentTools;

import java.awt.Rectangle;

import tools.Fs;

final class TerminateDuringRunSelectedTest extends ManagerTest{
  final Action becomeCode= action("becomeCode",
    on("ubuntu_gnome", () -> click(val(975), val(113))),
    on("xubuntu_xfce", () -> click(val(980), val(93))));
  final Action openChain= action("openChain",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action openChainMarker= action("openChainMarker",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action projectShown= action("projectShown",
    on("ubuntu_gnome", () -> waitAfter(val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(null))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome", () -> waitAfter(val(17500))),
    on("xubuntu_xfce", () -> waitAfter(val(21500))));
  final Action codeShown= action("codeShown",
    on("ubuntu_gnome", () -> waitAfter(val(4500))),
    on("xubuntu_xfce", () -> waitAfter(val(6000))));
  final Action compile= action("compile",
    on("ubuntu_gnome", () -> click(val(528), val(113))),
    on("xubuntu_xfce", () -> click(val(486), val(93))));
  final Action compiled= action("compiled",
    on("ubuntu_gnome", () -> waitAfter(val(21000))),
    on("xubuntu_xfce", () -> waitAfter(val(26000))));
  final Action runV= action("runV",
    on("ubuntu_gnome", () -> click(val(528), val(113))),
    on("xubuntu_xfce", () -> click(val(478), val(93))));
  final Action mainsShown= action("mainsShown",
    on("ubuntu_gnome", () -> waitAfter(val(3000))),
    on("xubuntu_xfce", () -> waitAfter(val(4000))));
  final Action tickShow= action("tickShow",
    on("ubuntu_gnome", () -> click(val(522), val(139))),
    on("xubuntu_xfce", () -> click(val(453), val(118))));
  final Action tickTell= action("tickTell",
    on("ubuntu_gnome", () -> click(val(522), val(164))),
    on("xubuntu_xfce", () -> click(val(453), val(141))));
  final Action runTicked= action("runTicked",
    on("ubuntu_gnome", () -> click(val(531), val(188))),
    on("xubuntu_xfce", () -> click(val(464), val(163))));
  final Action programShown= action("programShown",
    on("ubuntu_gnome", () -> waitAfter(val(10500))),
    on("xubuntu_xfce", () -> waitAfter(val(13000))));
  final Action terminate= action("terminate",
    on("ubuntu_gnome", () -> click(val(528), val(113))),
    on("xubuntu_xfce", () -> click(val(494), val(93))));
  final Action terminated= action("terminated",
    on("ubuntu_gnome", () -> waitAfter(val(4500))),
    on("xubuntu_xfce", () -> waitAfter(val(5500))));
  final Action tellWouldHaveRun= action("tellWouldHaveRun",
    on("ubuntu_gnome", () -> waitAfter(val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(null))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome", () -> waitAfter(val(7000))),
    on("xubuntu_xfce", () -> waitAfter(val(9000))));
  @Override void walk() throws Throwable{
    var chain= files.resolve("chain");
    Fs.writeUtf8(chain.resolve("chain.fearless"), "\n");
    Fs.writeUtf8(chain.resolve("_chain").resolve("_rank_app.fear"), """
      use base.Main as Main;
      use base.Consumer as Consumer;
      use base.Frame as Frame;
      Show: Main{sys -> sys.gui.run Note}
      Tell: Main{sys -> sys.out.println("told")}
      Note: Consumer[mut Frame]{:: .title "chain" .content{:: .label{:: .text "terminate this program" } } }
      """);
    var bare= look();
    start();
    managerShown.go();
    toFiles();
    openChain.go();
    openChainMarker.go();
    closeFiles();
    projectShown.go();
    becomeCode.go();
    codeShown.go();
    compile.go();
    compiled.go();
    var program= new Rectangle(920, 510, 150, 94);
    var output= inWindow(410, 168, 1000, 80);
    var before= look();
    runV.go();
    mainsShown.go();
    tickShow.go();
    tickTell.go();
    runTicked.go();
    programShown.go();
    changed(before, program);
    var running= look();
    terminate.go();
    terminated.go();
    same(before, program);
    stabilize();
    checkContent(info, """
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
    assert differing(running, look(), output) > 0;
    tellWouldHaveRun.go();
    same(running, inWindow(410, 220, 1000, 34));
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    same(bare, output);
  }
}

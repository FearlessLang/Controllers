package agentTools;

import java.util.List;

import tools.Fs;

final class TerminateDuringRunSelectedTest extends ManagerTest{
  static final List<String> console= List.of(data,"eclipse","chain","console.txt");
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(18000))));
  final Action becomeCode= action("becomeCode",
    on("ubuntu_gnome",()->click(val(719),val(139))),
    on("windows",()->click(val(332),val(94))));
  final Action codeShown= action("codeShown",
    on("ubuntu_gnome",()->waitUntilTime(val(21500))));
  final Action compile= action("compile",
    on("ubuntu_gnome",()->click(val(483),val(111))),
    on("windows",()->click(val(102),val(67))));
  final Action mainsShown= action("mainsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(35000))));
  final Action all= action("all",
    on("ubuntu_gnome",()->click(val(431),val(165))),
    on("windows",()->click(val(45),val(118))));
  final Action bothSaved= action("bothSaved",
    on("ubuntu_gnome",()->waitUntilTime(val(37000))));
  final Action runSelected= action("runSelected",
    on("ubuntu_gnome",()->click(val(483),val(111))),
    on("windows",()->click(val(102),val(67))));
  final Action programShown= action("programShown",
    on("ubuntu_gnome",()->waitUntilTime(val(42000))));
  final Action terminate= action("terminate",
    on("ubuntu_gnome",()->click(val(483),val(111))),
    on("windows",()->click(val(102),val(67))));
  final Action terminated= action("terminated",
    on("ubuntu_gnome",()->waitUntilTime(val(45000))));
  final Action appsShownToEnd= action("appsShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(46500))));
  final Action terminalShownToEnd= action("terminalShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(50000))));
  final Action managerEnded= action("managerEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(60000))));
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
    launchScript("first",chain);
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    becomeCode.go();
    codeShown.go();
    compile.go();
    mainsShown.go();
    all.go();
    bothSaved.go();
    var both= """
      {
        "chain": {
          "path": "Str:%s",
          "kind": "code",
          "mains": ["chain.Show", "chain.Tell"]
        }
      }
      """.formatted(slashed(chain));
    stabilize();
    checkContent(info,both);
    var idle= look();
    runSelected.go();
    programShown.go();
    var program= changed("Run selected opens the window of the first main",idle,look());
    stabilize();
    checkContent(console,"--- compiling chain ---\n--- compile done ---\n--- running chain.Show ---[###]");
    terminate.go();
    terminated.go();
    same("Terminate closes the window of the program",idle,look(),program);
    stabilize();
    checkContent(state,"""
      {
        "chain": {
          "folder": "Str:%s",
          "kind": "code",
          "running": "",
          "runs": "1",
          "lastRun": "chain.Show",
          "exit": "143",
          "mains": {
            "chain.Show": "_chain/_rank_app.fear",
            "chain.Tell": "_chain/_rank_app.fear"
          },
          "problem": {}
        }
      }
      """.formatted(escaped(chain)));
    checkContent(console,"""
      --- compiling chain ---
      --- compile done ---
      --- running chain.Show ---[###]
      --- terminating chain.Show ---[###]
      --- chain.Show exited with 143 after [###]s ---
      """);
    checkContent(info,both);
    endScript();
    runInTerminal(appsShownToEnd,terminalShownToEnd);
    managerEnded.go();
    stabilize();
    checkContent(List.of("first.exit"),"137\n");
  }
}

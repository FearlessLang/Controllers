package agentTools;

import java.util.List;

import tools.Fs;

final class ClearOutputWhileRunningTest extends ManagerTest{
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(18500))));
  final Action becomeCode= action("becomeCode",
    on("ubuntu_gnome",()->click(val(719),val(139))),
    on("windows",()->click(val(332),val(94))));
  final Action codeShown= action("codeShown",
    on("ubuntu_gnome",()->waitUntilTime(val(22000))));
  final Action compile= action("compile",
    on("ubuntu_gnome",()->click(val(483),val(111))),
    on("windows",()->click(val(102),val(67))));
  final Action mainShown= action("mainShown",
    on("ubuntu_gnome",()->waitUntilTime(val(37000))));
  final Action run= action("run",
    on("ubuntu_gnome",()->click(val(483),val(111))),
    on("windows",()->click(val(102),val(67))));
  final Action programShown= action("programShown",
    on("ubuntu_gnome",()->waitUntilTime(val(43000))));
  final Action clearOutput= action("clearOutput",
    on("ubuntu_gnome",()->click(val(3784),val(289))),
    on("windows",()->click(val(1227),val(237))));
  final Action outputCleared= action("outputCleared",
    on("ubuntu_gnome",()->waitUntilTime(val(45500))));
  final Action bringProgramBack= action("bringProgramBack",
    on("ubuntu_gnome",()->click(val(32),val(450))),
    on("windows",()->click(val(880),val(696))));
  final Action programBack= action("programBack",
    on("ubuntu_gnome",()->waitUntilTime(val(47500))));
  final Action closeProgram= action("closeProgram",
    on("ubuntu_gnome",()->click(val(1997),val(1074))),
    on("windows",()->click(val(678),val(312))));
  final Action programEnded= action("programEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(50000))));
  final Action appsShownToEnd= action("appsShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(51500))));
  final Action terminalShownToEnd= action("terminalShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(55000))));
  final Action managerEnded= action("managerEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(65000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var talk= filesIOFolder.resolve("talk");
    var console= List.of(data,"eclipse","talk","console.txt");
    Fs.rmTree(talk);
    Fs.writeUtf8(talk.resolve("talk.fearless"),"\n");
    Fs.writeUtf8(talk.resolve("_talk").resolve("_rank_app.fear"),"""
      use base.Main as Main;
      use base.Block as Block;
      use base.Consumer as Consumer;
      use base.Frame as Frame;

      Show: Main{sys -> Block#(sys.out.println "before", sys.gui.run Note, sys.out.println "after")}
      Note: Consumer[mut Frame]{:: .title "talk" .content{:: .label{:: .text "close this window" } } }
      """);
    launchScript("first",talk);
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    becomeCode.go();
    codeShown.go();
    compile.go();
    mainShown.go();
    run.go();
    programShown.go();
    stabilize();
    checkContent(console,"--- compiling talk ---\n--- compile done ---\n--- running talk.Show ---\nbefore\n");
    clearOutput.go();
    outputCleared.go();
    stabilize();
    checkContent(console,"");
    checkContent(state,"""
      {
        "talk": {
          "folder": "Str:%s",
          "kind": "code",
          "running": "talk.Show",
          "runs": "[###]",
          "lastRun": "[###]",
          "exit": "[###]",
          "mains": {
            "talk.Show": "_talk/_rank_app.fear"
          },
          "problem": {}
        }
      }
      """.formatted(escaped(talk)));
    bringProgramBack.go();
    programBack.go();
    closeProgram.go();
    programEnded.go();
    stabilize();
    checkContent(state,"""
      {
        "talk": {
          "folder": "Str:%s",
          "kind": "code",
          "running": "",
          "runs": "1",
          "lastRun": "talk.Show",
          "exit": "0",
          "mains": {
            "talk.Show": "_talk/_rank_app.fear"
          },
          "problem": {}
        }
      }
      """.formatted(escaped(talk)));
    checkContent(console,"""
      after
      --- talk.Show exited with 0 after [###]s ---
      """);
    endScript();
    runInTerminal(appsShownToEnd,terminalShownToEnd);
    managerEnded.go();
    stabilize();
    checkContent(List.of("first.exit"),"137\n");
  }
}

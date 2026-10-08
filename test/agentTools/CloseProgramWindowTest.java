package agentTools;

import java.awt.event.KeyEvent;
import java.util.List;

import tools.Fs;

final class CloseProgramWindowTest extends ManagerTest{
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(18500))));
  final Action focusTiles= action("focusTiles",
    on("ubuntu_gnome",()->click(val(200),val(1500))),
    on("windows",()->click(val(200),val(400))));
  final Action becomeCode= action("becomeCode",
    on("ubuntu_gnome",()->click(val(400),val(139))),
    on("windows",()->click(val(332),val(94))));
  final Action codeShown= action("codeShown",
    on("ubuntu_gnome",()->waitUntilTime(val(22000))));
  final Action compile= action("compile",
    on("ubuntu_gnome",()->click(val(164),val(111))),
    on("windows",()->click(val(102),val(67))));
  final Action mainShown= action("mainShown",
    on("ubuntu_gnome",()->waitUntilTime(val(37000))));
  final Action run= action("run",
    on("ubuntu_gnome",()->click(val(164),val(111))),
    on("windows",()->click(val(102),val(67))));
  final Action programShown= action("programShown",
    on("ubuntu_gnome",()->waitUntilTime(val(43000))));
  final Action closeProgram= action("closeProgram",
    on("ubuntu_gnome",()->click(val(1996),val(1075))),
    on("windows",()->click(val(678),val(312))));
  final Action programEnded= action("programEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(45500))));
  final Action appsShownToEnd= action("appsShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(47000))));
  final Action terminalShownToEnd= action("terminalShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(50500))));
  final Action managerEnded= action("managerEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(60500))));
  @Override void walk() throws Throwable{
    noManagerData();
    var shut= filesIOFolder.resolve("shut");
    var console= List.of(data,"eclipse","shut","console.txt");
    Fs.rmTree(shut);
    Fs.writeUtf8(shut.resolve("shut.fearless"),"\n");
    Fs.writeUtf8(shut.resolve("_shut").resolve("_rank_app.fear"),"""
      use base.Main as Main;
      use base.Consumer as Consumer;
      use base.Frame as Frame;

      Show: Main{sys -> sys.gui.run Note}
      Note: Consumer[mut Frame]{:: .title "shut" .content{:: .label{:: .text "close this window" } } }
      """);
    launchScript("first",shut);
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    focusTiles.go();
    keys(KeyEvent.VK_F8);
    keys(KeyEvent.VK_HOME);
    becomeCode.go();
    codeShown.go();
    compile.go();
    mainShown.go();
    stabilize();
    checkContent(console,"--- compiling shut ---\n--- compile done ---\n");
    run.go();
    programShown.go();
    stabilize();
    checkContent(console,"--- compiling shut ---\n--- compile done ---\n--- running shut.Show ---\n");
    checkContent(state,"""
      {
        "shut": {
          "folder": "Str:%s",
          "kind": "code",
          "running": "shut.Show",
          "runs": "[###]",
          "lastRun": "[###]",
          "exit": "[###]",
          "mains": {
            "shut.Show": "_shut/_rank_app.fear"
          },
          "problem": {}
        }
      }
      """.formatted(escaped(shut)));
    closeProgram.go();
    programEnded.go();
    stabilize();
    checkContent(state,"""
      {
        "shut": {
          "folder": "Str:%s",
          "kind": "code",
          "running": "",
          "runs": "1",
          "lastRun": "shut.Show",
          "exit": "0",
          "mains": {
            "shut.Show": "_shut/_rank_app.fear"
          },
          "problem": {}
        }
      }
      """.formatted(escaped(shut)));
    checkContent(console,"""
      --- compiling shut ---
      --- compile done ---
      --- running shut.Show ---
      --- shut.Show exited with 0 after [###]s ---
      """);
    endScript();
    runInTerminal(appsShownToEnd,terminalShownToEnd);
    managerEnded.go();
    stabilize();
    checkContent(List.of("first.exit"),"137\n");
  }
}

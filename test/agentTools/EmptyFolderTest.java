package agentTools;

import java.awt.event.KeyEvent;
import java.util.List;

import tools.Fs;

final class EmptyFolderTest extends ManagerTest{
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(18500))));
  final Action focusTiles= action("focusTiles",
    on("ubuntu_gnome",()->click(val(200),val(1500))),
    on("windows",()->click(val(200),val(400))));
  final Action compile= action("compile",
    on("ubuntu_gnome",()->click(val(164),val(111))),
    on("windows",()->click(val(102),val(67))));
  final Action compiled= action("compiled",
    on("ubuntu_gnome",()->waitUntilTime(val(34500))));
  final Action run= action("run",
    on("ubuntu_gnome",()->click(val(164),val(111))),
    on("windows",()->click(val(102),val(67))));
  final Action runDone= action("runDone",
    on("ubuntu_gnome",()->waitUntilTime(val(39500))));
  final Action appsShownToEnd= action("appsShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(41000))));
  final Action terminalShownToEnd= action("terminalShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(44500))));
  final Action managerEnded= action("managerEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(54500))));
  @Override void walk() throws Throwable{
    noManagerData();
    var empty= filesIOFolder.resolve("empty");
    Fs.rmTree(empty);
    Fs.ensureDir(empty);
    launchScript("first",empty);
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    stabilize();
    checkContent(List.of("empty","empty.fearless"),"\n");
    checkContent(List.of("empty","_empty","_rank_app.fear"),"""
      use base.Main as Main;
      use base.Lists as List;
      use base.Num as Num;
      use base.Void as Void;
      use base.Str as Str;

      Hello: Main { sys -> sys.out.println("Hello World!") }
      """);
    checkContent(info,"""
      {
        "empty": {
          "path": "Str:%s",
          "kind": "code"
        }
      }
      """.formatted(slashed(empty)));
    focusTiles.go();
    keys(KeyEvent.VK_F8);
    keys(KeyEvent.VK_HOME);
    compile.go();
    compiled.go();
    var known= """
      {
        "empty": {
          "folder": "Str:%s",
          "kind": "code",
          "running": "",
          "runs": "%s",
          "lastRun": "%s",
          "exit": "%s",
          "mains": {
            "empty.Hello": "_empty/_rank_app.fear"
          },
          "problem": {}
        }
      }
      """;
    stabilize();
    checkContent(state,known.formatted(escaped(empty),"0","","-1"));
    run.go();
    runDone.go();
    stabilize();
    checkContent(state,known.formatted(escaped(empty),"1","empty.Hello","0"));
    checkContent(List.of(data,"eclipse","empty","console.txt"),"""
      --- compiling empty ---
      --- compile done ---
      --- running empty.Hello ---
      Hello World!
      --- empty.Hello exited with 0 after [###]s ---
      """);
    endScript();
    runInTerminal(appsShownToEnd,terminalShownToEnd);
    managerEnded.go();
    stabilize();
    checkContent(List.of("first.exit"),"137\n");
  }
}

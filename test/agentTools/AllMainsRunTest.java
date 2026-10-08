package agentTools;

import java.awt.event.KeyEvent;
import java.util.List;

final class AllMainsRunTest extends ManagerTest{
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
  final Action mainsShown= action("mainsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(36000))));
  final Action all= action("all",
    on("ubuntu_gnome",()->click(val(112),val(165))),
    on("windows",()->click(val(45),val(118))));
  final Action allSaved= action("allSaved",
    on("ubuntu_gnome",()->waitUntilTime(val(38500))));
  final Action runSelected= action("runSelected",
    on("ubuntu_gnome",()->click(val(164),val(111))),
    on("windows",()->click(val(102),val(67))));
  final Action runsDone= action("runsDone",
    on("ubuntu_gnome",()->waitUntilTime(val(60000))));
  final Action none= action("none",
    on("ubuntu_gnome",()->click(val(162),val(165))),
    on("windows",()->click(val(93),val(118))));
  final Action noneSaved= action("noneSaved",
    on("ubuntu_gnome",()->waitUntilTime(val(62500))));
  final Action appsShownToEnd= action("appsShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(64000))));
  final Action terminalShownToEnd= action("terminalShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(67500))));
  final Action managerEnded= action("managerEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(77500))));
  @Override void walk() throws Throwable{
    noManagerData();
    var project= project("helloWorld");
    launchScript("first",project);
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    focusTiles.go();
    keys(KeyEvent.VK_F8);
    keys(KeyEvent.VK_HOME);
    becomeCode.go();
    codeShown.go();
    compile.go();
    mainsShown.go();
    all.go();
    allSaved.go();
    stabilize();
    checkContent(info,"""
      {
        "hello_world": {
          "path": "Str:%s",
          "kind": "code",
          "mains": ["hello.Hello1", "hello.Hello3", "hello.Hello4", "hello.Hello5", "hello.Hello6"]
        }
      }
      """.formatted(slashed(project)));
    runSelected.go();
    runsDone.go();
    stabilize();
    checkContent(state,"""
      {
        "hello_world": {
          "folder": "Str:%s",
          "kind": "code",
          "running": "",
          "runs": "5",
          "lastRun": "hello.Hello6",
          "exit": "[###]",
          "mains": {
            "hello.Hello1": "_hello/_rank_app.fear",
            "hello.Hello3": "_hello/_rank_app.fear",
            "hello.Hello4": "_hello/_rank_app.fear",
            "hello.Hello5": "_hello/_rank_app.fear",
            "hello.Hello6": "_hello/_rank_app.fear"
          },
          "problem": {}
        }
      }
      """.formatted(escaped(project)));
    checkContent(List.of(data,"eclipse","hello_world","console.txt"),"""
      --- compiling helloWorld ---
      --- compile done ---
      --- running hello.Hello1 ---
      hello world 3
      --- hello.Hello1 exited with 0 after [###]s ---
      --- running hello.Hello3 ---
      [Hi]
      --- hello.Hello3 exited with 0 after [###]s ---
      --- running hello.Hello4 ---
      [1, 2, 3, 4]
      --- hello.Hello4 exited with 0 after [###]s ---
      --- running hello.Hello5 ---
      [11, 12, 13, 14]
      --- hello.Hello5 exited with 0 after [###]s ---
      --- running hello.Hello6 ---
      AAAAh
      imm Bar.bar error line: 16 in file _hello/_rank_app.fear
      imm Foo.foo error line: 15 in file _hello/_rank_app.fear
      imm Hello6.main(_) error line: 14 in file _hello/_rank_app.fear
      --- hello.Hello6 exited with [###] after [###]s ---
      """);
    none.go();
    noneSaved.go();
    stabilize();
    checkContent(info,"""
      {
        "hello_world": {
          "path": "Str:%s",
          "kind": "code"
        }
      }
      """.formatted(slashed(project)));
    endScript();
    runInTerminal(appsShownToEnd,terminalShownToEnd);
    managerEnded.go();
    stabilize();
    checkContent(List.of("first.exit"),"137\n");
  }
}

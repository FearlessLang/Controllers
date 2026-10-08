package agentTools;

import java.awt.event.KeyEvent;
import java.util.List;

final class RunSelectedTest extends ManagerTest{
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(18000))));
  final Action focusTiles= action("focusTiles",
    on("ubuntu_gnome",()->click(val(200),val(1500))),
    on("windows",()->click(val(200),val(400))));
  final Action becomeCode= action("becomeCode",
    on("ubuntu_gnome",()->click(val(400),val(139))),
    on("windows",()->click(val(332),val(94))));
  final Action codeShown= action("codeShown",
    on("ubuntu_gnome",()->waitUntilTime(val(21500))));
  final Action compile= action("compile",
    on("ubuntu_gnome",()->click(val(164),val(111))),
    on("windows",()->click(val(102),val(67))));
  final Action mainsShown= action("mainsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(35000))));
  final Action tickHello4= action("tickHello4",
    on("ubuntu_gnome",()->click(val(99),val(240))),
    on("windows",()->click(val(31),val(190))));
  final Action hello4Saved= action("hello4Saved",
    on("ubuntu_gnome",()->waitUntilTime(val(37000))));
  final Action tickHello1= action("tickHello1",
    on("ubuntu_gnome",()->click(val(99),val(190))),
    on("windows",()->click(val(31),val(142))));
  final Action hello1Saved= action("hello1Saved",
    on("ubuntu_gnome",()->waitUntilTime(val(39000))));
  final Action runSelected= action("runSelected",
    on("ubuntu_gnome",()->click(val(164),val(111))),
    on("windows",()->click(val(102),val(67))));
  final Action runsDone= action("runsDone",
    on("ubuntu_gnome",()->waitUntilTime(val(46000))));
  final Action appsShownToEnd= action("appsShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(47500))));
  final Action terminalShownToEnd= action("terminalShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(51000))));
  final Action managerEnded= action("managerEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(61000))));
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
    var registry= """
      {
        "hello_world": {
          "path": "Str:%s",
          "kind": "code",
          "mains": [%s]
        }
      }
      """;
    tickHello4.go();
    hello4Saved.go();
    stabilize();
    checkContent(info,registry.formatted(slashed(project),"\"hello.Hello4\""));
    tickHello1.go();
    hello1Saved.go();
    stabilize();
    checkContent(info,registry.formatted(slashed(project),"\"hello.Hello1\", \"hello.Hello4\""));
    runSelected.go();
    runsDone.go();
    stabilize();
    checkContent(state,"""
      {
        "hello_world": {
          "folder": "Str:%s",
          "kind": "code",
          "running": "",
          "runs": "2",
          "lastRun": "hello.Hello4",
          "exit": "0",
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
      --- running hello.Hello4 ---
      [1, 2, 3, 4]
      --- hello.Hello4 exited with 0 after [###]s ---
      """);
    endScript();
    runInTerminal(appsShownToEnd,terminalShownToEnd);
    managerEnded.go();
    stabilize();
    checkContent(List.of("first.exit"),"137\n");
  }
}

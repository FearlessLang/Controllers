package agentTools;

import java.util.List;

final class TerminateTest extends ManagerTest{
  static final List<String> console= List.of(data,"eclipse","start","console.txt");
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
  final Action mainShown= action("mainShown",
    on("ubuntu_gnome",()->waitUntilTime(val(36000))));
  final Action run= action("run",
    on("ubuntu_gnome",()->click(val(483),val(111))),
    on("windows",()->click(val(102),val(67))));
  final Action programShown= action("programShown",
    on("ubuntu_gnome",()->waitUntilTime(val(41000))));
  final Action terminate= action("terminate",
    on("ubuntu_gnome",()->click(val(483),val(111))),
    on("windows",()->click(val(102),val(67))));
  final Action terminated= action("terminated",
    on("ubuntu_gnome",()->waitUntilTime(val(44000))));
  final Action appsShownToEnd= action("appsShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(45500))));
  final Action terminalShownToEnd= action("terminalShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(49000))));
  final Action managerEnded= action("managerEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(59000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var gui= project("testGui1");
    launchScript("first",gui);
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    becomeCode.go();
    codeShown.go();
    compile.go();
    mainShown.go();
    stabilize();
    checkContent(console,"--- compiling testGui1 ---\n--- compile done ---\n");
    var idle= look();
    run.go();
    programShown.go();
    var program= changed("Run opens the window of the program",idle,look());
    stabilize();
    checkContent(console,"--- compiling testGui1 ---\n--- compile done ---\n--- running gui_example.Foo ---[###]");
    terminate.go();
    terminated.go();
    same("Terminate closes the window of the program",idle,look(),program);
    stabilize();
    checkContent(state,"""
      {
        "start": {
          "folder": "Str:%s",
          "kind": "code",
          "running": "",
          "runs": "1",
          "lastRun": "gui_example.Foo",
          "exit": "143",
          "mains": {
            "gui_example.Foo": "_gui_example/_rank_app.fear"
          },
          "problem": {}
        }
      }
      """.formatted(escaped(gui)));
    checkContent(console,"""
      --- compiling testGui1 ---
      --- compile done ---
      --- running gui_example.Foo ---[###]
      --- terminating gui_example.Foo ---[###]
      --- gui_example.Foo exited with 143 after [###]s ---
      """);
    endScript();
    runInTerminal(appsShownToEnd,terminalShownToEnd);
    managerEnded.go();
    stabilize();
    checkContent(List.of("first.exit"),"137\n");
  }
}

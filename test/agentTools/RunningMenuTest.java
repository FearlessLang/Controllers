package agentTools;

import java.awt.event.KeyEvent;
import java.util.List;

final class RunningMenuTest extends ManagerTest{
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(18000))));
  final Action runningMenu= action("runningMenu",
    on("ubuntu_gnome",()->click(val(218),val(79))),
    on("windows",()->click(val(146),val(33))));
  final Action menuShown= action("menuShown",
    on("ubuntu_gnome",()->waitUntilTime(val(20500))));
  final Action becomeCode= action("becomeCode",
    on("ubuntu_gnome",()->click(val(719),val(139))),
    on("windows",()->click(val(332),val(94))));
  final Action codeShown= action("codeShown",
    on("ubuntu_gnome",()->waitUntilTime(val(23000))));
  final Action compile= action("compile",
    on("ubuntu_gnome",()->click(val(483),val(111))),
    on("windows",()->click(val(102),val(67))));
  final Action mainShown= action("mainShown",
    on("ubuntu_gnome",()->waitUntilTime(val(37000))));
  final Action run= action("run",
    on("ubuntu_gnome",()->click(val(483),val(111))),
    on("windows",()->click(val(102),val(67))));
  final Action programShown= action("programShown",
    on("ubuntu_gnome",()->waitUntilTime(val(42000))));
  final Action appsShownSecond= action("appsShownSecond",
    on("ubuntu_gnome",()->waitUntilTime(val(44000))));
  final Action terminalShownSecond= action("terminalShownSecond",
    on("ubuntu_gnome",()->waitUntilTime(val(47500))));
  final Action secondShown= action("secondShown",
    on("ubuntu_gnome",()->waitUntilTime(val(58500))));
  final Action runningMenuAgain= action("runningMenuAgain",
    on("ubuntu_gnome",()->click(val(218),val(79))),
    on("windows",()->click(val(146),val(33))));
  final Action menuShownAgain= action("menuShownAgain",
    on("ubuntu_gnome",()->waitUntilTime(val(60000))));
  final Action chooseProgram= action("chooseProgram",
    on("ubuntu_gnome",()->click(val(260),val(103))),
    on("windows",()->click(val(188),val(57))));
  final Action programChosen= action("programChosen",
    on("ubuntu_gnome",()->waitUntilTime(val(62000))));
  final Action terminate= action("terminate",
    on("ubuntu_gnome",()->click(val(483),val(111))),
    on("windows",()->click(val(102),val(67))));
  final Action terminated= action("terminated",
    on("ubuntu_gnome",()->waitUntilTime(val(64500))));
  final Action runningMenuLast= action("runningMenuLast",
    on("ubuntu_gnome",()->click(val(218),val(79))),
    on("windows",()->click(val(146),val(33))));
  final Action menuShownLast= action("menuShownLast",
    on("ubuntu_gnome",()->waitUntilTime(val(66000))));
  final Action appsShownToEnd= action("appsShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(68000))));
  final Action terminalShownToEnd= action("terminalShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(71500))));
  final Action managerEnded= action("managerEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(81500))));
  @Override void walk() throws Throwable{
    noManagerData();
    var gui= project("testGui1");
    var project= project("helloWorld");
    launchScript("first",gui);
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    runningMenu.go();
    menuShown.go();
    keys(KeyEvent.VK_ESCAPE);
    becomeCode.go();
    codeShown.go();
    compile.go();
    mainShown.go();
    run.go();
    programShown.go();
    launchScript("second",project);
    runInTerminal(appsShownSecond,terminalShownSecond);
    secondShown.go();
    stabilize();
    checkContent(List.of("second.exit"),"0\n");
    runningMenuAgain.go();
    menuShownAgain.go();
    chooseProgram.go();
    programChosen.go();
    terminate.go();
    terminated.go();
    stabilize();
    checkContent(state,"""
      {
        "start": {
          "folder": "Str:%s",
          "kind": "code",
          "running": "",
          "runs": "1",
          "lastRun": "gui_example.Foo",
          "exit": "[###]",
          "mains": {
            "gui_example.Foo": "_gui_example/_rank_app.fear"
          },
          "problem": {}
        },
        "hello_world": {
          "folder": "Str:%s",
          "kind": "idle",
          "running": "",
          "runs": "0",
          "lastRun": "",
          "exit": "-1",
          "mains": {},
          "problem": {}
        }
      }
      """.formatted(escaped(gui),escaped(project)));
    runningMenuLast.go();
    menuShownLast.go();
    keys(KeyEvent.VK_ESCAPE);
    endScript();
    runInTerminal(appsShownToEnd,terminalShownToEnd);
    managerEnded.go();
    stabilize();
    checkContent(List.of("first.exit"),"137\n");
  }
}

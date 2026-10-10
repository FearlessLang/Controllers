package agentTools;

import java.awt.event.KeyEvent;
import java.util.List;

final class StaleMainNoteTest extends ManagerTest{
  static final List<String> console= List.of(data,"eclipse","hello_world","console.txt");
  static final String registry= """
    {
      "hello_world": {
        "path": "Str:%s",
        "kind": "code",
        "mains": [%s]
      }
    }
    """;
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
  final Action managerMenu= action("managerMenu",
    on("ubuntu_gnome",()->click(val(98),val(79))),
    on("windows",()->click(val(31),val(33))));
  final Action editMetadata= action("editMetadata",
    on("ubuntu_gnome",()->click(val(128),val(103))),
    on("windows",()->click(val(61),val(58))));
  final Action editorShown= action("editorShown",
    on("ubuntu_gnome",()->waitUntilTime(val(38000))));
  final Action focusText= action("focusText",
    on("ubuntu_gnome",()->click(val(1900),val(1200))),
    on("windows",()->click(val(700),val(400))));
  final Action commit= action("commit",
    on("ubuntu_gnome",()->click(val(2238),val(1370))),
    on("windows",()->click(val(924),val(619))));
  final Action committed= action("committed",
    on("ubuntu_gnome",()->waitUntilTime(val(77000))));
  final Action runSelected= action("runSelected",
    on("ubuntu_gnome",()->click(val(483),val(111))),
    on("windows",()->click(val(102),val(67))));
  final Action refused= action("refused",
    on("ubuntu_gnome",()->waitUntilTime(val(79500))));
  final Action runSelectedAgain= action("runSelectedAgain",
    on("ubuntu_gnome",()->click(val(483),val(111))),
    on("windows",()->click(val(102),val(67))));
  final Action runDone= action("runDone",
    on("ubuntu_gnome",()->waitUntilTime(val(84500))));
  final Action appsShownToEnd= action("appsShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(86000))));
  final Action terminalShownToEnd= action("terminalShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(89500))));
  final Action managerEnded= action("managerEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(99500))));
  @Override void walk() throws Throwable{
    noManagerData();
    var project= project("helloWorld");
    var both= registry.formatted(slashed(project),"\"hello.Hello1\", \"hello.Nope\"");
    launchScript("first",project);
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    becomeCode.go();
    codeShown.go();
    compile.go();
    mainsShown.go();
    managerMenu.go();
    editMetadata.go();
    editorShown.go();
    focusText.go();
    keys(KeyEvent.VK_CONTROL,KeyEvent.VK_A);
    type(both);
    commit.go();
    committed.go();
    var compiled= """
      --- compiling helloWorld ---
      --- compile done ---
      """;
    stabilize();
    checkContent(info,both);
    checkContent(console,compiled);
    runSelected.go();
    refused.go();
    var nothing= compiled+"--- nothing to run: the selected \"hello.Nope\" are not mains of this project; they are removed from the selected mains ---\n";
    stabilize();
    checkContent(console,nothing);
    checkContent(info,registry.formatted(slashed(project),"\"hello.Hello1\""));
    runSelectedAgain.go();
    runDone.go();
    stabilize();
    checkContent(console,nothing+"""
      --- running hello.Hello1 ---
      hello world 3
      --- hello.Hello1 exited with 0 after [###]s ---
      """);
    checkContent(state,"""
      {
        "hello_world": {
          "folder": "Str:%s",
          "kind": "code",
          "running": "",
          "runs": "1",
          "lastRun": "hello.Hello1",
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
    endScript();
    runInTerminal(appsShownToEnd,terminalShownToEnd);
    managerEnded.go();
    stabilize();
    checkContent(List.of("first.exit"),"137\n");
  }
}

package agentTools;

import java.util.List;

import tools.Fs;

final class StaleMainTest extends ManagerTest{
  static final List<String> console= List.of(data,"eclipse","menu","console.txt");
  static final String mains= "use base.Main as Main;\n\nFirst: Main{sys -> sys.out.println \"First\"}\nSecond: Main{sys -> sys.out.println \"Second\"}\n";
  static final String compiled= "--- compiling menu ---\n--- compile done ---\n";
  static final String registry= """
    {
      "menu": {
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
  final Action tickSecond= action("tickSecond",
    on("ubuntu_gnome",()->click(val(418),val(215))),
    on("windows",()->click(val(31),val(166))));
  final Action secondSaved= action("secondSaved",
    on("ubuntu_gnome",()->waitUntilTime(val(37000))));
  final Action tickThird= action("tickThird",
    on("ubuntu_gnome",()->click(val(418),val(240))),
    on("windows",()->click(val(31),val(190))));
  final Action thirdSaved= action("thirdSaved",
    on("ubuntu_gnome",()->waitUntilTime(val(39000))));
  final Action mainsForgotten= action("mainsForgotten",
    on("ubuntu_gnome",()->waitUntilTime(val(44000))));
  final Action compileEdited= action("compileEdited",
    on("ubuntu_gnome",()->click(val(483),val(111))),
    on("windows",()->click(val(102),val(67))));
  final Action editedCompiled= action("editedCompiled",
    on("ubuntu_gnome",()->waitUntilTime(val(58000))));
  final Action runSelected= action("runSelected",
    on("ubuntu_gnome",()->click(val(483),val(111))),
    on("windows",()->click(val(102),val(67))));
  final Action runDone= action("runDone",
    on("ubuntu_gnome",()->waitUntilTime(val(63000))));
  final Action appsShownToEnd= action("appsShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(64500))));
  final Action terminalShownToEnd= action("terminalShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(68000))));
  final Action managerEnded= action("managerEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(78000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var menu= filesIOFolder.resolve("menu");
    var source= menu.resolve("_menu").resolve("_rank_app.fear");
    Fs.rmTree(menu);
    Fs.writeUtf8(menu.resolve("menu.fearless"),"\n");
    Fs.writeUtf8(source,mains+"Third: Main{sys -> sys.out.println \"Third\"}\n");
    launchScript("first",menu);
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    becomeCode.go();
    codeShown.go();
    compile.go();
    mainsShown.go();
    tickSecond.go();
    secondSaved.go();
    stabilize();
    checkContent(info,registry.formatted(slashed(menu),"\"menu.Second\""));
    tickThird.go();
    thirdSaved.go();
    var both= registry.formatted(slashed(menu),"\"menu.Second\", \"menu.Third\"");
    stabilize();
    checkContent(info,both);
    Fs.writeUtf8(source,mains);
    stabilize();
    mainsForgotten.go();
    stabilize();
    checkContent(info,both);
    checkContent(console,compiled);
    compileEdited.go();
    editedCompiled.go();
    stabilize();
    checkContent(console,compiled+compiled);
    checkContent(info,registry.formatted(slashed(menu),"\"menu.Second\""));
    runSelected.go();
    runDone.go();
    stabilize();
    checkContent(state,"""
      {
        "menu": {
          "folder": "Str:%s",
          "kind": "code",
          "running": "",
          "runs": "1",
          "lastRun": "menu.Second",
          "exit": "0",
          "mains": {
            "menu.First": "_menu/_rank_app.fear",
            "menu.Second": "_menu/_rank_app.fear"
          },
          "problem": {}
        }
      }
      """.formatted(escaped(menu)));
    checkContent(console,compiled+compiled+"""
      --- running menu.Second ---
      Second
      --- menu.Second exited with 0 after [###]s ---
      """);
    endScript();
    runInTerminal(appsShownToEnd,terminalShownToEnd);
    managerEnded.go();
    stabilize();
    checkContent(List.of("first.exit"),"137\n");
  }
}

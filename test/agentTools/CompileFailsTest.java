package agentTools;

import java.util.List;

import tools.Fs;

final class CompileFailsTest extends ManagerTest{
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
  final Action compileFailed= action("compileFailed",
    on("ubuntu_gnome",()->waitUntilTime(val(36000))));
  final Action compileFixed= action("compileFixed",
    on("ubuntu_gnome",()->click(val(483),val(111))),
    on("windows",()->click(val(102),val(67))));
  final Action mainShown= action("mainShown",
    on("ubuntu_gnome",()->waitUntilTime(val(51000))));
  final Action appsShownToEnd= action("appsShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(52500))));
  final Action terminalShownToEnd= action("terminalShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(56000))));
  final Action managerEnded= action("managerEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(66000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var broken= filesIOFolder.resolve("broken");
    var source= broken.resolve("_broken").resolve("_rank_app.fear");
    var console= List.of(data,"eclipse","broken","console.txt");
    Fs.rmTree(broken);
    Fs.writeUtf8(broken.resolve("broken.fearless"),"\n");
    Fs.writeUtf8(source,"use base.Main as Main;\n\nHello:Main{s->Nope.nope}\n");
    launchScript("first",broken);
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    becomeCode.go();
    codeShown.go();
    compile.go();
    compileFailed.go();
    var failed= """
      --- compiling broken ---
      In file: fear:/_broken/_rank_app.fear

      003| Hello:Main{s->Nope.nope}
         |               ^^^^^

      While inspecting a type name
      Type "Nope" is not declared in package "broken" and is not made visible via "use".
      In scope: "Hello", "Main".
      Error 7 WellFormedness
      --- compile failed with 1 ---
      """;
    stabilize();
    checkContent(console,failed);
    checkContent(state,"""
      {
        "broken": {
          "folder": "Str:%s",
          "kind": "code",
          "running": "",
          "runs": "0",
          "lastRun": "",
          "exit": "-1",
          "mains": {},
          "problem": {
            "file": "_broken/_rank_app.fear",
            "line": "003",
            "message": "Str:In file: fear:/_broken/_rank_app.fear\\n\\n003| Hello:Main{s->Nope.nope}\\n   |               ^^^^^\\n\\nWhile inspecting a type name\\nType \\"Nope\\" is not declared in package \\"broken\\" and is not made visible via \\"use\\".\\nIn scope: \\"Hello\\", \\"Main\\".\\nError 7 WellFormedness\\n"
          }
        }
      }
      """.formatted(escaped(broken)));
    Fs.writeUtf8(source,"use base.Main as Main;\n\nHello:Main{s->base.Debug#(\"fixed\")}\n");
    compileFixed.go();
    mainShown.go();
    stabilize();
    checkContent(console,failed+"--- compiling broken ---\n--- compile done ---\n");
    checkContent(state,"""
      {
        "broken": {
          "folder": "Str:%s",
          "kind": "code",
          "running": "",
          "runs": "0",
          "lastRun": "",
          "exit": "-1",
          "mains": {
            "broken.Hello": "_broken/_rank_app.fear"
          },
          "problem": {}
        }
      }
      """.formatted(escaped(broken)));
    endScript();
    runInTerminal(appsShownToEnd,terminalShownToEnd);
    managerEnded.go();
    stabilize();
    checkContent(List.of("first.exit"),"137\n");
  }
}

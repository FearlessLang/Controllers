package agentTools;

import java.util.List;

import tools.Fs;

final class RecompileAfterEditTest extends ManagerTest{
  static final List<String> console= List.of(data,"eclipse","draft","console.txt");
  static final String first= "use base.Main as Main;\n\nFirst:Main{s->base.Debug#(\"first\")}\n";
  static final String compiled= "--- compiling draft ---\n--- compile done ---\n";
  static final String states= """
    {
      "draft": {
        "folder": "Str:%s",
        "kind": "code",
        "running": "",
        "runs": "0",
        "lastRun": "",
        "exit": "-1",
        "mains": %s,
        "problem": {}
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
  final Action firstCompiled= action("firstCompiled",
    on("ubuntu_gnome",()->waitUntilTime(val(35000))));
  final Action editNoticed= action("editNoticed",
    on("ubuntu_gnome",()->waitUntilTime(val(40000))));
  final Action compileEdited= action("compileEdited",
    on("ubuntu_gnome",()->click(val(483),val(111))),
    on("windows",()->click(val(102),val(67))));
  final Action editedCompiled= action("editedCompiled",
    on("ubuntu_gnome",()->waitUntilTime(val(54000))));
  final Action appsShownToEnd= action("appsShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(55500))));
  final Action terminalShownToEnd= action("terminalShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(59000))));
  final Action managerEnded= action("managerEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(69000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var draft= filesIOFolder.resolve("draft");
    var source= draft.resolve("_draft").resolve("_rank_app.fear");
    Fs.rmTree(draft);
    Fs.writeUtf8(draft.resolve("draft.fearless"),"\n");
    Fs.writeUtf8(source,first);
    launchScript("first",draft);
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    becomeCode.go();
    codeShown.go();
    compile.go();
    firstCompiled.go();
    stabilize();
    checkContent(console,compiled);
    checkContent(state,states.formatted(escaped(draft),"""
      {
            "draft.First": "_draft/_rank_app.fear"
          }"""));
    Fs.writeUtf8(source,first+"Second:Main{s->base.Debug#(\"second\")}\n");
    stabilize();
    editNoticed.go();
    stabilize();
    checkContent(state,states.formatted(escaped(draft),"{}"));
    checkContent(console,compiled);
    compileEdited.go();
    editedCompiled.go();
    stabilize();
    checkContent(console,compiled+compiled);
    checkContent(state,states.formatted(escaped(draft),"""
      {
            "draft.First": "_draft/_rank_app.fear",
            "draft.Second": "_draft/_rank_app.fear"
          }"""));
    endScript();
    runInTerminal(appsShownToEnd,terminalShownToEnd);
    managerEnded.go();
    stabilize();
    checkContent(List.of("first.exit"),"137\n");
  }
}

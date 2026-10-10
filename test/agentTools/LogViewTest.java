package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.util.List;

import tools.Fs;
import utils.Err;

final class LogViewTest extends ManagerTest{
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(18500))));
  final Action becomeCode= action("becomeCode",
    on("ubuntu_gnome",()->click(val(719),val(140))),
    on("windows",()->click(val(332),val(94))));
  final Action codeShown= action("codeShown",
    on("ubuntu_gnome",()->waitUntilTime(val(21600))));
  final Action compile= action("compile",
    on("ubuntu_gnome",()->click(val(483),val(111))),
    on("windows",()->click(val(102),val(67))));
  final Action compileShown= action("compileShown",
    on("ubuntu_gnome",()->waitUntilTime(val(36200))));
  final Action openLogs= action("openLogs",
    on("ubuntu_gnome",()->click(val(447),val(253))),
    on("windows",()->click(val(62),val(203))));
  final Action run= action("run",
    on("ubuntu_gnome",()->click(val(483),val(111))),
    on("windows",()->click(val(102),val(67))));
  final Action ranOnce= action("ranOnce",
    on("ubuntu_gnome",()->waitUntilTime(val(41500))));
  final Action runAgain= action("runAgain",
    on("ubuntu_gnome",()->click(val(483),val(111))),
    on("windows",()->click(val(102),val(67))));
  final Action ranTwice= action("ranTwice",
    on("ubuntu_gnome",()->waitUntilTime(val(46100))));
  final Action olderLog= action("olderLog",
    on("ubuntu_gnome",()->click(val(569),val(296))),
    on("windows",()->click(val(100),val(244))));
  final Action view= action("view",
    on("ubuntu_gnome",()->click(val(3688),val(251))),
    on("windows",()->click(val(1129),val(201))));
  final Action dialogShown= action("dialogShown",
    on("ubuntu_gnome",()->waitUntilTime(val(48800))));
  final Action focusText= action("focusText",
    on("ubuntu_gnome",()->click(val(2118),val(1100))),
    on("windows",()->click(val(640),val(350))));
  final Action ok= action("ok",
    on("ubuntu_gnome",()->click(val(2118),val(1340))),
    on("windows",()->click(val(644),val(579))));
  final Action dialogGone= action("dialogGone",
    on("ubuntu_gnome",()->waitUntilTime(val(51400))));
  final Action newerLog= action("newerLog",
    on("ubuntu_gnome",()->click(val(569),val(278))),
    on("windows",()->click(val(100),val(226))));
  final Action viewNewer= action("viewNewer",
    on("ubuntu_gnome",()->click(val(3688),val(251))),
    on("windows",()->click(val(1129),val(201))));
  final Action newerDialogShown= action("newerDialogShown",
    on("ubuntu_gnome",()->waitUntilTime(val(54100))));
  final Action focusNewerText= action("focusNewerText",
    on("ubuntu_gnome",()->click(val(2118),val(1100))),
    on("windows",()->click(val(640),val(350))));
  final Action okNewer= action("okNewer",
    on("ubuntu_gnome",()->click(val(2118),val(1340))),
    on("windows",()->click(val(644),val(579))));
  final Action newerDialogGone= action("newerDialogGone",
    on("ubuntu_gnome",()->waitUntilTime(val(56700))));
  final Action appsShownToEnd= action("appsShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(58200))));
  final Action terminalShownToEnd= action("terminalShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(61700))));
  final Action managerEnded= action("managerEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(71700))));
  @Override void walk() throws Throwable{
    noManagerData();
    var journal= filesIOFolder.resolve("journal");
    Fs.rmTree(journal);
    Fs.writeUtf8(journal.resolve("journal.fearless"),"\n");
    Fs.writeUtf8(journal.resolve("_journal").resolve("_rank_app.fear"),"""
      use base.Main as Main;
      use base.Block as Block;
      use base.FileLog as FileLog;

      Diary: FileLog{"diary"}
      Write: Main{sys -> Block#.do{Diary.log("dear diary")}.do{Diary.log("good night")}.done}
      """);
    launchScript("first",journal);
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    becomeCode.go();
    codeShown.go();
    stabilize();
    checkContent(info,"""
      {
        "journal": {
          "path": "Str:%s",
          "kind": "code"
        }
      }
      """.formatted(slashed(journal)));
    compile.go();
    compileShown.go();
    stabilize();
    checkContent(state,"[###]\"journal.Write\": \"_journal/_rank_app.fear\"[###]");
    openLogs.go();
    var ran= """
      {
        "journal": {
          "folder": "Str:%s",
          "kind": "code",
          "running": "",
          "runs": "%s",
          "lastRun": "journal.Write",
          "exit": "0",
          "mains": {
            "journal.Write": "_journal/_rank_app.fear"
          },
          "problem": {}
        }
      }
      """;
    run.go();
    ranOnce.go();
    stabilize();
    checkContent(state,ran.formatted(escaped(journal),"1"));
    runAgain.go();
    ranTwice.go();
    stabilize();
    checkContent(state,ran.formatted(escaped(journal),"2"));
    var logs= "journal/.out/logs/journal";
    var both= Fs.walk(filesIOFolder.resolve(logs),s->s.filter(Files::isRegularFile).map(p->p.getFileName().toString()).sorted().toList());
    assertEquals(2,both.size());
    for (var log: both){
      Err.strCmp("Diary$[###].log",log);
      checkContent(List.of(logs,log),"[###] dear diary\n[###] good night\n");
    }
    olderLog.go();
    var listed= look();
    view.go();
    dialogShown.go();
    var dialog= changed("View opens the log in a dialog",listed,look());
    focusText.go();
    copied("[###] dear diary\n[###] good night");
    ok.go();
    dialogGone.go();
    same("OK closes the dialog",listed,look(),dialog);
    newerLog.go();
    var listedNewer= look();
    viewNewer.go();
    newerDialogShown.go();
    var newerDialog= changed("View opens the newer log in a dialog",listedNewer,look());
    focusNewerText.go();
    copied("[###] dear diary\n[###] good night");
    okNewer.go();
    newerDialogGone.go();
    same("OK closes the dialog",listedNewer,look(),newerDialog);
    endScript();
    runInTerminal(appsShownToEnd,terminalShownToEnd);
    managerEnded.go();
    stabilize();
    checkContent(List.of("first.exit"),"137\n");
  }
}

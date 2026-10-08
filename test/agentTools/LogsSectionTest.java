package agentTools;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.util.List;

import tools.Fs;
import utils.Err;
import utils.OneOr;

final class LogsSectionTest extends ManagerTest{
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
    on("ubuntu_gnome",()->click(val(400),val(140))),
    on("windows",()->click(val(332),val(94))));
  final Action codeShown= action("codeShown",
    on("ubuntu_gnome",()->waitUntilTime(val(21600))));
  final Action compile= action("compile",
    on("ubuntu_gnome",()->click(val(164),val(111))),
    on("windows",()->click(val(102),val(67))));
  final Action compileShown= action("compileShown",
    on("ubuntu_gnome",()->waitUntilTime(val(36200))));
  final Action openLogs= action("openLogs",
    on("ubuntu_gnome",()->click(val(128),val(253))),
    on("windows",()->click(val(62),val(203))));
  final Action run= action("run",
    on("ubuntu_gnome",()->click(val(164),val(111))),
    on("windows",()->click(val(102),val(67))));
  final Action ran= action("ran",
    on("ubuntu_gnome",()->waitUntilTime(val(41500))));
  final Action firstLog= action("firstLog",
    on("ubuntu_gnome",()->click(val(250),val(278))),
    on("windows",()->click(val(100),val(226))));
  final Action copy= action("copy",
    on("ubuntu_gnome",()->click(val(3739),val(251))),
    on("windows",()->click(val(1181),val(201))));
  final Action delete= action("delete",
    on("ubuntu_gnome",()->click(val(3796),val(251))),
    on("windows",()->click(val(1237),val(201))));
  final Action confirmShown= action("confirmShown",
    on("ubuntu_gnome",()->waitUntilTime(val(44800))));
  final Action yes= action("yes",
    on("ubuntu_gnome",()->click(val(1933),val(1150))),
    on("windows",()->click(val(619),val(378))));
  final Action deleted= action("deleted",
    on("ubuntu_gnome",()->waitUntilTime(val(46900))));
  final Action appsShownToEnd= action("appsShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(48400))));
  final Action terminalShownToEnd= action("terminalShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(51900))));
  final Action managerEnded= action("managerEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(61900))));
  @Override void walk() throws Throwable{
    noManagerData();
    var diary= filesIOFolder.resolve("diary");
    Fs.rmTree(diary);
    Fs.writeUtf8(diary.resolve("diary.fearless"),"\n");
    Fs.writeUtf8(diary.resolve("_diary").resolve("_rank_app.fear"),"""
      use base.Main as Main;
      use base.FileLog as FileLog;

      Diary: FileLog{"diary"}
      Write: Main{sys -> Diary.log("dear diary")}
      """);
    launchScript("first",diary);
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    focusTiles.go();
    keys(KeyEvent.VK_F8);
    keys(KeyEvent.VK_HOME);
    becomeCode.go();
    codeShown.go();
    stabilize();
    checkContent(info,"""
      {
        "diary": {
          "path": "Str:%s",
          "kind": "code"
        }
      }
      """.formatted(slashed(diary)));
    compile.go();
    compileShown.go();
    stabilize();
    checkContent(state,"[###]\"diary.Write\": \"_diary/_rank_app.fear\"[###]");
    openLogs.go();
    run.go();
    ran.go();
    stabilize();
    checkContent(state,"""
      {
        "diary": {
          "folder": "Str:%s",
          "kind": "code",
          "running": "",
          "runs": "1",
          "lastRun": "diary.Write",
          "exit": "0",
          "mains": {
            "diary.Write": "_diary/_rank_app.fear"
          },
          "problem": {}
        }
      }
      """.formatted(escaped(diary)));
    var logs= "diary/.out/logs/diary";
    var log= Fs.walk(filesIOFolder.resolve(logs),s->OneOr.of("one log",s.filter(Files::isRegularFile)));
    Err.strCmp("Diary$[###].log",log.getFileName().toString());
    checkContent(List.of(logs,log.getFileName().toString()),"[###] dear diary\n");
    firstLog.go();
    copy.go();
    delete.go();
    confirmShown.go();
    yes.go();
    deleted.go();
    stabilize();
    assertFalse(Files.exists(log));
    endScript();
    runInTerminal(appsShownToEnd,terminalShownToEnd);
    managerEnded.go();
    stabilize();
    checkContent(List.of("first.exit"),"137\n");
  }
}

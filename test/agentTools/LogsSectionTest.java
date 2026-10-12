package agentTools;

import java.awt.Rectangle;
import java.nio.file.Files;
import java.util.List;

import tools.Fs;
import utils.Err;
import utils.OneOr;

final class LogsSectionTest extends ManagerTest{
  final Action becomeCode= action("becomeCode",
    on("ubuntu_gnome",()->click(val(971),val(113))),
    on("xubuntu_xfce",()->click(val(978),val(93))));
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(3500))),
    on("xubuntu_xfce",()->waitUntilTime(val(4500))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(8500))),
    on("xubuntu_xfce",()->waitUntilTime(val(11000))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(26000))),
    on("xubuntu_xfce",()->waitUntilTime(val(32500))));
  final Action codeShown= action("codeShown",
    on("ubuntu_gnome",()->waitUntilTime(val(30500))),
    on("xubuntu_xfce",()->waitUntilTime(val(38500))));
  final Action compile= action("compile",
    on("ubuntu_gnome",()->click(val(524),val(113))),
    on("xubuntu_xfce",()->click(val(483),val(93))));
  final Action compiled= action("compiled",
    on("ubuntu_gnome",()->waitUntilTime(val(51500))),
    on("xubuntu_xfce",()->waitUntilTime(val(64500))));
  final Action openLogs= action("openLogs",
    on("ubuntu_gnome",()->click(val(450),val(387))),
    on("xubuntu_xfce",()->click(val(383),val(367))));
  final Action logsShown= action("logsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(54500))),
    on("xubuntu_xfce",()->waitUntilTime(val(68500))));
  final Action run= action("run",
    on("ubuntu_gnome",()->click(val(524),val(113))),
    on("xubuntu_xfce",()->click(val(468),val(93))));
  final Action ran= action("ran",
    on("ubuntu_gnome",()->waitUntilTime(val(62000))),
    on("xubuntu_xfce",()->waitUntilTime(val(77500))));
  final Action logsButton= action("logsButton",
    on("ubuntu_gnome",()->click(val(1880),val(386))),
    on("xubuntu_xfce",()->click(val(1813),val(366))));
  final Action filesShown= action("filesShown",
    on("ubuntu_gnome",()->waitUntilTime(val(67500))),
    on("xubuntu_xfce",()->waitUntilTime(val(84500))));
  final Action closeFiles= action("closeFiles",
    on("ubuntu_gnome",()->click(val(1414),val(303))),
    on("xubuntu_xfce",()->click(val(1275),val(309))));
  final Action filesClosed= action("filesClosed",
    on("ubuntu_gnome",()->waitUntilTime(val(71000))),
    on("xubuntu_xfce",()->waitUntilTime(val(89000))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",()->waitUntilTime(val(78000))),
    on("xubuntu_xfce",()->waitUntilTime(val(97500))));
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
    launchScript("first",diary.resolve("diary.fearless"));
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    becomeCode.go();
    codeShown.go();
    compile.go();
    compiled.go();
    openLogs.go();
    logsShown.go();
    var list= inWindow(410,400,1000,60);
    var empty= look();
    run.go();
    ran.go();
    changed(empty,list);
    stabilize();
    var logs= "diary/.out/logs/diary";
    var log= Fs.walk(filesIOFolder.resolve(logs),s->OneOr.of("one log",s.filter(Files::isRegularFile)));
    Err.strCmp("Diary$[###].log",log.getFileName().toString());
    checkContent(List.of(logs,log.getFileName().toString()),"[###] dear diary\n");
    var listed= look();
    logsButton.go();
    filesShown.go();
    changed(listed,new Rectangle(640,360,600,400));
    closeFiles.go();
    filesClosed.go();
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
  }
}

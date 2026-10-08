package agentTools;

import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.util.List;

import tools.Fs;

final class SecondMarkerTest extends ManagerTest{
  static final List<String> console= List.of(data,"eclipse","twice","console.txt");
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(18000))));
  final Action focusTiles= action("focusTiles",
    on("ubuntu_gnome",()->click(val(200),val(1500))),
    on("windows",()->click(val(200),val(400))));
  final Action secondNoticed= action("secondNoticed",
    on("ubuntu_gnome",()->waitUntilTime(val(21000))));
  final Action projectMenu= action("projectMenu",
    on("ubuntu_gnome",()->click(val(158),val(79))),
    on("windows",()->click(val(89),val(33))));
  final Action errorReport= action("errorReport",
    on("ubuntu_gnome",()->click(val(180),val(212))),
    on("windows",()->click(val(111),val(166))));
  final Action reportShown= action("reportShown",
    on("ubuntu_gnome",()->waitUntilTime(val(23700))));
  final Action focusReport= action("focusReport",
    on("ubuntu_gnome",()->click(val(1970),val(1100))),
    on("windows",()->click(val(660),val(300))));
  final Action ok= action("ok",
    on("ubuntu_gnome",()->click(val(1953),val(1330))),
    on("windows",()->click(val(639),val(568))));
  final Action reportClosed= action("reportClosed",
    on("ubuntu_gnome",()->waitUntilTime(val(26000))));
  final Action check= action("check",
    on("ubuntu_gnome",()->click(val(164),val(111))),
    on("windows",()->click(val(102),val(67))));
  final Action checked= action("checked",
    on("ubuntu_gnome",()->waitUntilTime(val(28500))));
  final Action openInformation= action("openInformation",
    on("ubuntu_gnome",()->click(val(150),val(167))),
    on("windows",()->click(val(79),val(120))));
  final Action secondGone= action("secondGone",
    on("ubuntu_gnome",()->waitUntilTime(val(31000))));
  final Action checkAgain= action("checkAgain",
    on("ubuntu_gnome",()->click(val(164),val(111))),
    on("windows",()->click(val(102),val(67))));
  final Action checkedAgain= action("checkedAgain",
    on("ubuntu_gnome",()->waitUntilTime(val(33500))));
  final Action appsShownToEnd= action("appsShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(35000))));
  final Action terminalShownToEnd= action("terminalShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(38500))));
  final Action managerEnded= action("managerEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(48500))));
  @Override void walk() throws Throwable{
    noManagerData();
    var twice= filesIOFolder.resolve("twice");
    var second= twice.resolve("other.fearless");
    Fs.rmTree(twice);
    Fs.writeUtf8(twice.resolve("twice.fearless"),"\n");
    Fs.writeUtf8(twice.resolve("_twice").resolve("_rank_app.fear"),"use base.Main as Main;\n\nHello:Main{s->base.Debug#(\"twice\")}\n");
    launchScript("first",twice);
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    focusTiles.go();
    keys(KeyEvent.VK_F8);
    keys(KeyEvent.VK_HOME);
    Fs.writeUtf8(second,"\n");
    stabilize();
    secondNoticed.go();
    projectMenu.go();
    errorReport.go();
    reportShown.go();
    focusReport.go();
    keys(KeyEvent.VK_CONTROL,KeyEvent.VK_A);
    keys(KeyEvent.VK_CONTROL,KeyEvent.VK_C);
    ok.go();
    reportClosed.go();
    check.go();
    checked.go();
    var problem= """
      More than one .fearless marker file was found in
      %s
      A project folder holds exactly one, and its name is the project name: keep only "twice.fearless".""".formatted(twice);
    stabilize();
    checkContent(console,problem+"\n");
    Files.delete(second);
    stabilize();
    openInformation.go();
    secondGone.go();
    checkAgain.go();
    checkedAgain.go();
    stabilize();
    checkContent(console,problem+"\n--- ok: no problem found ---\n");
    endScript();
    runInTerminal(appsShownToEnd,terminalShownToEnd);
    managerEnded.go();
    stabilize();
    checkContent(List.of("first.exit"),"137\n");
  }
}

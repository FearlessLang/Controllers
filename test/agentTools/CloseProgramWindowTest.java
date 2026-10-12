package agentTools;

import java.awt.Rectangle;
import java.util.List;

import tools.Fs;

final class CloseProgramWindowTest extends ManagerTest{
  final Action becomeCode= action("becomeCode",
    on("ubuntu_gnome",()->click(val(967),val(113))),
    on("xubuntu_xfce",()->click(val(972),val(93))));
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
    on("ubuntu_gnome",()->click(val(520),val(113))),
    on("xubuntu_xfce",()->click(val(477),val(93))));
  final Action compiled= action("compiled",
    on("ubuntu_gnome",()->waitUntilTime(val(51500))),
    on("xubuntu_xfce",()->waitUntilTime(val(64500))));
  final Action run= action("run",
    on("ubuntu_gnome",()->click(val(520),val(113))),
    on("xubuntu_xfce",()->click(val(462),val(93))));
  final Action programShown= action("programShown",
    on("ubuntu_gnome",()->waitUntilTime(val(60000))),
    on("xubuntu_xfce",()->waitUntilTime(val(75000))));
  final Action closeProgram= action("closeProgram",
    on("ubuntu_gnome",()->click(val(1037),val(527))),
    on("xubuntu_xfce",()->click(val(1012),val(524))));
  final Action programEnded= action("programEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(64500))),
    on("xubuntu_xfce",()->waitUntilTime(val(81000))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",()->waitUntilTime(val(71500))),
    on("xubuntu_xfce",()->waitUntilTime(val(89500))));
  @Override void walk() throws Throwable{
    noManagerData();
    var shut= filesIOFolder.resolve("shut");
    Fs.rmTree(shut);
    Fs.writeUtf8(shut.resolve("shut.fearless"),"\n");
    Fs.writeUtf8(shut.resolve("_shut").resolve("_rank_app.fear"),"""
      use base.Main as Main;
      use base.Consumer as Consumer;
      use base.Frame as Frame;

      Show: Main{sys -> sys.gui.run Note}
      Note: Consumer[mut Frame]{:: .title "shut" .content{:: .label{:: .text "close this window" } } }
      """);
    launchScript("first",shut.resolve("shut.fearless"));
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    becomeCode.go();
    codeShown.go();
    compile.go();
    compiled.go();
    var program= new Rectangle(930,510,126,94);
    var button= inWindow(505,102,30,20);
    var before= look();
    run.go();
    programShown.go();
    changed(before,program);
    changed(before,button);
    closeProgram.go();
    programEnded.go();
    same(before,program);
    same(before,button);
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
  }
}

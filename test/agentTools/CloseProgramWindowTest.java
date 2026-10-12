package agentTools;

import java.awt.Rectangle;
import java.util.List;

import tools.Fs;

final class CloseProgramWindowTest extends ManagerTest{
  final Action becomeCode= header("becomeCode",967);
  final Action appsShown= action("appsShown",on("ubuntu_gnome",()->waitUntilTime(val(3500))));
  final Action terminalShown= action("terminalShown",on("ubuntu_gnome",()->waitUntilTime(val(8500))));
  final Action managerShown= action("managerShown",on("ubuntu_gnome",()->waitUntilTime(val(26000))));
  final Action codeShown= action("codeShown",on("ubuntu_gnome",()->waitUntilTime(val(30500))));
  final Action compile= action("compile",on("ubuntu_gnome",()->click(val(520),val(113))));
  final Action compiled= action("compiled",on("ubuntu_gnome",()->waitUntilTime(val(51500))));
  final Action run= action("run",on("ubuntu_gnome",()->click(val(520),val(113))));
  final Action programShown= action("programShown",on("ubuntu_gnome",()->waitUntilTime(val(60000))));
  final Action closeProgram= action("closeProgram",on("ubuntu_gnome",()->click(val(1037),val(527))));
  final Action programEnded= action("programEnded",on("ubuntu_gnome",()->waitUntilTime(val(64500))));
  final Action managerQuit= action("managerQuit",on("ubuntu_gnome",()->waitUntilTime(val(71500))));
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
    var button= new Rectangle(505,102,30,20);
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

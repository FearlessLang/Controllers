package agentTools;

import java.awt.Rectangle;
import java.util.List;

import tools.Fs;

final class CloseProgramWindowTest extends ManagerTest{
  final Action becomeCode= header("becomeCode",967);
  final Action appsShown= action("appsShown",on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",on("ubuntu_gnome",()->waitUntilTime(val(18000))));
  final Action codeShown= action("codeShown",on("ubuntu_gnome",()->waitUntilTime(val(21000))));
  final Action compile= action("compile",on("ubuntu_gnome",()->click(val(520),val(113))));
  final Action compiled= action("compiled",on("ubuntu_gnome",()->waitUntilTime(val(36000))));
  final Action run= action("run",on("ubuntu_gnome",()->click(val(520),val(113))));
  final Action programShown= action("programShown",on("ubuntu_gnome",()->waitUntilTime(val(42000))));
  final Action closeProgram= action("closeProgram",on("ubuntu_gnome",()->click(val(1997),val(1067))));
  final Action programEnded= action("programEnded",on("ubuntu_gnome",()->waitUntilTime(val(45000))));
  final Action managerQuit= action("managerQuit",on("ubuntu_gnome",()->waitUntilTime(val(50000))));
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
    var program= new Rectangle(1890,1050,126,94);
    var button= new Rectangle(500,100,160,24);
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

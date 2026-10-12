package agentTools;

import java.awt.Rectangle;
import java.util.List;

import tools.Fs;

final class DoubleClickCompileTest extends ManagerTest{
  final Action becomeCode= header("becomeCode",976);
  final Action appsShown= action("appsShown",on("ubuntu_gnome",()->waitUntilTime(val(3500))));
  final Action terminalShown= action("terminalShown",on("ubuntu_gnome",()->waitUntilTime(val(8500))));
  final Action managerShown= action("managerShown",on("ubuntu_gnome",()->waitUntilTime(val(26000))));
  final Action codeShown= action("codeShown",on("ubuntu_gnome",()->waitUntilTime(val(30500))));
  final Action compileTwice= action("compileTwice",on("ubuntu_gnome",()->doubleClick(val(529),val(113))));
  final Action compiled= action("compiled",on("ubuntu_gnome",()->waitUntilTime(val(51500))));
  final Action managerQuit= action("managerQuit",on("ubuntu_gnome",()->waitUntilTime(val(58500))));
  @Override void walk() throws Throwable{
    noManagerData();
    var hasty= filesIOFolder.resolve("hasty");
    Fs.rmTree(hasty);
    Fs.writeUtf8(hasty.resolve("hasty.fearless"),"\n");
    Fs.writeUtf8(hasty.resolve("_hasty").resolve("_rank_app.fear"),"use base.Main as Main;\n\nGo: Main{sys -> sys.out.println \"went\"}\n");
    launchScript("first",hasty.resolve("hasty.fearless"));
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    becomeCode.go();
    codeShown.go();
    var center= new Rectangle(760,460,400,160);
    var output= new Rectangle(410,170,1000,90);
    var before= look();
    compileTwice.go();
    compiled.go();
    changed(before,output);
    same(before,center);
    stabilize();
    checkContent(List.of("hasty",".fearless_out","mains.info"),"[###]\"hasty.Go\"[###]");
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
  }
}

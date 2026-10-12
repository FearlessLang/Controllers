package agentTools;

import java.awt.Rectangle;
import java.util.List;

import tools.Fs;

final class OutputClearTest extends ManagerTest{
  final Action becomeCode= header("becomeCode",962);
  final Action appsShown= action("appsShown",on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",on("ubuntu_gnome",()->waitUntilTime(val(18000))));
  final Action codeShown= action("codeShown",on("ubuntu_gnome",()->waitUntilTime(val(21000))));
  final Action compile= action("compile",on("ubuntu_gnome",()->click(val(515),val(113))));
  final Action compiled= action("compiled",on("ubuntu_gnome",()->waitUntilTime(val(36000))));
  final Action clear= action("clear",on("ubuntu_gnome",()->click(val(3800),val(150))));
  final Action cleared= action("cleared",on("ubuntu_gnome",()->waitUntilTime(val(37500))));
  final Action run= action("run",on("ubuntu_gnome",()->click(val(515),val(113))));
  final Action ran= action("ran",on("ubuntu_gnome",()->waitUntilTime(val(43000))));
  final Action clearAgain= action("clearAgain",on("ubuntu_gnome",()->click(val(3800),val(150))));
  final Action clearedAgain= action("clearedAgain",on("ubuntu_gnome",()->waitUntilTime(val(44500))));
  final Action managerQuit= action("managerQuit",on("ubuntu_gnome",()->waitUntilTime(val(49500))));
  @Override void walk() throws Throwable{
    noManagerData();
    var said= filesIOFolder.resolve("said");
    Fs.rmTree(said);
    Fs.writeUtf8(said.resolve("said.fearless"),"\n");
    Fs.writeUtf8(said.resolve("_said").resolve("_rank_app.fear"),"use base.Main as Main;\n\nSay: Main{sys -> sys.out.println \"said once\"}\n");
    launchScript("first",said.resolve("said.fearless"));
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    becomeCode.go();
    codeShown.go();
    var output= new Rectangle(410,170,1000,90);
    var empty= look();
    compile.go();
    compiled.go();
    changed(empty,output);
    clear.go();
    cleared.go();
    same(empty,output);
    run.go();
    ran.go();
    changed(empty,output);
    clearAgain.go();
    clearedAgain.go();
    same(empty,output);
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
  }
}

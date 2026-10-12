package agentTools;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.awt.Rectangle;
import java.nio.file.Files;
import java.util.List;

import tools.Fs;

final class TileDoubleClickTest extends ManagerTest{
  final Action becomeCode= header("becomeCode",989);
  final Action appsShown= action("appsShown",on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",on("ubuntu_gnome",()->waitUntilTime(val(18000))));
  final Action codeShown= action("codeShown",on("ubuntu_gnome",()->waitUntilTime(val(21000))));
  final Action tileTwice= action("tileTwice",on("ubuntu_gnome",()->doubleClick(val(133),val(130))));
  final Action notRun= action("notRun",on("ubuntu_gnome",()->waitUntilTime(val(25000))));
  final Action compile= action("compile",on("ubuntu_gnome",()->click(val(542),val(113))));
  final Action compiled= action("compiled",on("ubuntu_gnome",()->waitUntilTime(val(40000))));
  final Action tileTwiceCompiled= action("tileTwiceCompiled",on("ubuntu_gnome",()->doubleClick(val(133),val(130))));
  final Action ran= action("ran",on("ubuntu_gnome",()->waitUntilTime(val(46000))));
  final Action managerQuit= action("managerQuit",on("ubuntu_gnome",()->waitUntilTime(val(51000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var tapped= filesIOFolder.resolve("tapped");
    Fs.rmTree(tapped);
    Fs.writeUtf8(tapped.resolve("tapped.fearless"),"\n");
    Fs.writeUtf8(tapped.resolve("_tapped").resolve("_rank_app.fear"),"use base.Main as Main;\n\nGo: Main{sys -> sys.out.println \"went\"}\n");
    launchScript("first",tapped.resolve("tapped.fearless"));
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    becomeCode.go();
    codeShown.go();
    var output= new Rectangle(410,168,1000,80);
    var header= new Rectangle(450,100,700,26);
    var before= look();
    tileTwice.go();
    notRun.go();
    same(before,output);
    same(before,header);
    stabilize();
    assertFalse(Files.exists(tapped.resolve(".fearless_out").resolve("mains.info")));
    compile.go();
    compiled.go();
    var compiledLook= look();
    tileTwiceCompiled.go();
    ran.go();
    changed(compiledLook,output);
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
  }
}

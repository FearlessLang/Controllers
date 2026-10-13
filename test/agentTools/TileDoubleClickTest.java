package agentTools;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.util.List;

import tools.Fs;

final class TileDoubleClickTest extends ManagerTest{
  final Action becomeCode= action("becomeCode",
    on("ubuntu_gnome",()->click(val(989),val(113))),
    on("xubuntu_xfce",()->click(val(998),val(93))));
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitAfter(val(3500))),
    on("xubuntu_xfce",()->waitAfter(val(4500))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitAfter(val(5000))),
    on("xubuntu_xfce",()->waitAfter(val(6500))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitAfter(val(17500))),
    on("xubuntu_xfce",()->waitAfter(val(21500))));
  final Action codeShown= action("codeShown",
    on("ubuntu_gnome",()->waitAfter(val(4500))),
    on("xubuntu_xfce",()->waitAfter(val(6000))));
  final Action tileTwice= action("tileTwice",
    on("ubuntu_gnome",()->doubleClick(val(133),val(130))),
    on("xubuntu_xfce",()->doubleClick(val(66),val(110))));
  final Action notRun= action("notRun",
    on("ubuntu_gnome",()->waitAfter(val(5500))),
    on("xubuntu_xfce",()->waitAfter(val(6500))));
  final Action compile= action("compile",
    on("ubuntu_gnome",()->click(val(542),val(113))),
    on("xubuntu_xfce",()->click(val(503),val(93))));
  final Action compiled= action("compiled",
    on("ubuntu_gnome",()->waitAfter(val(21000))),
    on("xubuntu_xfce",()->waitAfter(val(26500))));
  final Action tileTwiceCompiled= action("tileTwiceCompiled",
    on("ubuntu_gnome",()->doubleClick(val(133),val(130))),
    on("xubuntu_xfce",()->doubleClick(val(66),val(110))));
  final Action ran= action("ran",
    on("ubuntu_gnome",()->waitAfter(val(8500))),
    on("xubuntu_xfce",()->waitAfter(val(10500))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",()->waitAfter(val(7000))),
    on("xubuntu_xfce",()->waitAfter(val(9000))));
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
    var output= inWindow(410,168,1000,80);
    var button= inWindow(527,102,30,20);
    var before= look();
    tileTwice.go();
    notRun.go();
    same(before,output);
    same(before,button);
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

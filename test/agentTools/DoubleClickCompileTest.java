package agentTools;

import java.awt.Rectangle;

import tools.Fs;

final class DoubleClickCompileTest extends ManagerTest{
  final Action becomeCode= action("becomeCode",
    on("ubuntu_gnome",() -> click(val(976),val(113))),
    on("xubuntu_xfce",() -> click(val(982),val(93))));
  final Action openHasty= action("openHasty",
    on("ubuntu_gnome",() -> waitDoubleClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",() -> waitDoubleClick(val(null),val(null),val(null))));
  final Action openHastyMarker= action("openHastyMarker",
    on("ubuntu_gnome",() -> waitDoubleClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",() -> waitDoubleClick(val(null),val(null),val(null))));
  final Action projectShown= action("projectShown",
    on("ubuntu_gnome",() -> waitAfter(val(null))),
    on("xubuntu_xfce",() -> waitAfter(val(null))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",() -> waitAfter(val(17500))),
    on("xubuntu_xfce",() -> waitAfter(val(21500))));
  final Action codeShown= action("codeShown",
    on("ubuntu_gnome",() -> waitAfter(val(4500))),
    on("xubuntu_xfce",() -> waitAfter(val(6000))));
  final Action compileTwice= action("compileTwice",
    on("ubuntu_gnome",() -> doubleClick(val(529),val(113))),
    on("xubuntu_xfce",() -> doubleClick(val(488),val(93))));
  final Action compiled= action("compiled",
    on("ubuntu_gnome",() -> waitAfter(val(21000))),
    on("xubuntu_xfce",() -> waitAfter(val(26000))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",() -> waitAfter(val(7000))),
    on("xubuntu_xfce",() -> waitAfter(val(9000))));
  @Override void walk() throws Throwable{
    var hasty= files.resolve("hasty");
    Fs.writeUtf8(hasty.resolve("hasty.fearless"),"\n");
    Fs.writeUtf8(hasty.resolve("_hasty").resolve("_rank_app.fear"),"use base.Main as Main;\n\nGo: Main{sys -> sys.out.println \"went\"}\n");
    var bare= look();
    start();
    managerShown.go();
    toFiles();
    openHasty.go();
    openHastyMarker.go();
    closeFiles();
    projectShown.go();
    becomeCode.go();
    codeShown.go();
    var center= new Rectangle(760,460,400,160);
    var output= inWindow(410,170,1000,90);
    var before= look();
    compileTwice.go();
    compiled.go();
    changed(before,output);
    same(before,center);
    stabilize();
    checkContent(files.resolve("hasty/.fearless_out/mains.info"),"[###]\"hasty.Go\"[###]");
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    same(bare,output);
  }
}

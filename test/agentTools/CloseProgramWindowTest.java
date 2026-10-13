package agentTools;

import java.awt.Rectangle;

import tools.Fs;

final class CloseProgramWindowTest extends ManagerTest{
  final Action becomeCode= action("becomeCode",
    on("ubuntu_gnome", () -> click(val(967), val(113))),
    on("xubuntu_xfce", () -> click(val(972), val(93))));
  final Action openShut= action("openShut",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action openShutMarker= action("openShutMarker",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action projectShown= action("projectShown",
    on("ubuntu_gnome", () -> waitAfter(val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(null))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome", () -> waitAfter(val(17500))),
    on("xubuntu_xfce", () -> waitAfter(val(21500))));
  final Action codeShown= action("codeShown",
    on("ubuntu_gnome", () -> waitAfter(val(4500))),
    on("xubuntu_xfce", () -> waitAfter(val(6000))));
  final Action compile= action("compile",
    on("ubuntu_gnome", () -> click(val(520), val(113))),
    on("xubuntu_xfce", () -> click(val(477), val(93))));
  final Action compiled= action("compiled",
    on("ubuntu_gnome", () -> waitAfter(val(21000))),
    on("xubuntu_xfce", () -> waitAfter(val(26000))));
  final Action run= action("run",
    on("ubuntu_gnome", () -> click(val(520), val(113))),
    on("xubuntu_xfce", () -> click(val(462), val(93))));
  final Action programShown= action("programShown",
    on("ubuntu_gnome", () -> waitAfter(val(8500))),
    on("xubuntu_xfce", () -> waitAfter(val(10500))));
  final Action closeProgram= action("closeProgram",
    on("ubuntu_gnome", () -> click(val(1037), val(527))),
    on("xubuntu_xfce", () -> click(val(1012), val(524))));
  final Action programEnded= action("programEnded",
    on("ubuntu_gnome", () -> waitAfter(val(4500))),
    on("xubuntu_xfce", () -> waitAfter(val(6000))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome", () -> waitAfter(val(7000))),
    on("xubuntu_xfce", () -> waitAfter(val(8500))));
  @Override void walk() throws Throwable{
    var shut= files.resolve("shut");
    Fs.writeUtf8(shut.resolve("shut.fearless"), "\n");
    Fs.writeUtf8(shut.resolve("_shut").resolve("_rank_app.fear"), """
      use base.Main as Main;
      use base.Consumer as Consumer;
      use base.Frame as Frame;

      Show: Main{sys -> sys.gui.run Note}
      Note: Consumer[mut Frame]{:: .title "shut" .content{:: .label{:: .text "close this window" } } }
      """);
    var bare= look();
    start();
    managerShown.go();
    toFiles();
    openShut.go();
    openShutMarker.go();
    closeFiles();
    projectShown.go();
    becomeCode.go();
    codeShown.go();
    compile.go();
    compiled.go();
    var program= new Rectangle(930, 510, 126, 94);
    var button= inWindow(505, 102, 30, 20);
    var before= look();
    run.go();
    programShown.go();
    changed(before, program);
    changed(before, button);
    closeProgram.go();
    programEnded.go();
    same(before, program);
    same(before, button);
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    same(bare, button);
  }
}

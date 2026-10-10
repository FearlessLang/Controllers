package agentTools;

import java.awt.Rectangle;
import java.util.List;

import tools.Fs;

final class SortByCompiledTest extends ManagerTest{
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(18000))));
  final Action appsShownSecond= action("appsShownSecond",
    on("ubuntu_gnome",()->waitUntilTime(val(19500))));
  final Action terminalShownSecond= action("terminalShownSecond",
    on("ubuntu_gnome",()->waitUntilTime(val(23000))));
  final Action secondShown= action("secondShown",
    on("ubuntu_gnome",()->waitUntilTime(val(34000))));
  Rectangle cell;
  final Action tileCell= action("tileCell",
    on("ubuntu_gnome",()->cell= new Rectangle(val(74),val(149),val(128),val(88))));
  final Action orderBy= action("orderBy",
    on("ubuntu_gnome",()->click(val(165),val(130))),
    on("windows",()->click(val(108),val(82))));
  final Action compiled= action("compiled",
    on("ubuntu_gnome",()->click(val(150),val(191))),
    on("windows",()->click(val(97),val(141))));
  final Action sorted= action("sorted",
    on("ubuntu_gnome",()->waitUntilTime(val(37000))));
  final Action compile= action("compile",
    on("ubuntu_gnome",()->click(val(483),val(111))),
    on("windows",()->click(val(102),val(67))));
  final Action alphaCompiled= action("alphaCompiled",
    on("ubuntu_gnome",()->waitUntilTime(val(51000))));
  final Action appsShownToEnd= action("appsShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(53000))));
  final Action terminalShownToEnd= action("terminalShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(56500))));
  final Action managerEnded= action("managerEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(66500))));
  @Override void walk() throws Throwable{
    noManagerData();
    var order= filesIOFolder.resolve("order");
    var zeta= order.resolve("zeta");
    var alpha= order.resolve("alpha");
    Fs.rmTree(order);
    Fs.ensureDir(zeta);
    Fs.ensureDir(alpha);
    launchScript("first",zeta);
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    launchScript("second",alpha);
    runInTerminal(appsShownSecond,terminalShownSecond);
    secondShown.go();
    stabilize();
    checkContent(List.of("second.exit"),"0\n");
    checkContent(info,"""
      {
        "zeta": {
          "path": "Str:%s",
          "kind": "code"
        },
        "alpha": {
          "path": "Str:%s",
          "kind": "code"
        }
      }
      """.formatted(slashed(zeta),slashed(alpha)));
    tileCell.go();
    var first= new Rectangle(cell.x+4,cell.y+4,30,6);
    var second= new Rectangle(cell.x+cell.width+4,cell.y+4,30,6);
    var byName= look();
    orderBy.go();
    compiled.go();
    sorted.go();
    var byCompiled= look();
    differ("Order by Compiled takes the selection from the first tile",byName,byCompiled,first);
    differ("Order by Compiled gives the selection to the second tile",byName,byCompiled,second);
    compile.go();
    alphaCompiled.go();
    stabilize();
    checkContent(List.of(data,"activity.txt"),"-1 -1 Str:"+zeta+"\n[###] -1 Str:"+alpha+"\n");
    var afterCompile= look();
    same("The compile of alpha puts its tile first again",byName,afterCompile,first);
    same("The compile of alpha puts the other tile second again",byName,afterCompile,second);
    endScript();
    runInTerminal(appsShownToEnd,terminalShownToEnd);
    managerEnded.go();
    stabilize();
    checkContent(List.of("first.exit"),"137\n");
  }
}

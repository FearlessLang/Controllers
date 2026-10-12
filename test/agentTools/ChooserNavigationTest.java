package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Rectangle;
import java.nio.file.Files;
import java.util.List;

import tools.Fs;

final class ChooserNavigationTest extends ManagerTest{
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(3500))),
    on("xubuntu_xfce",()->waitUntilTime(val(4500))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(8500))),
    on("xubuntu_xfce",()->waitUntilTime(val(11000))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(26000))),
    on("xubuntu_xfce",()->waitUntilTime(val(32500))));
  final Action chooserShown= action("chooserShown",
    on("ubuntu_gnome",()->waitUntilTime(val(31000))),
    on("xubuntu_xfce",()->waitUntilTime(val(39000))));
  final Action upLevel= action("upLevel",
    on("ubuntu_gnome",()->click(val(1109),val(436))),
    on("xubuntu_xfce",()->click(val(1085),val(427))));
  final Action homeLeft= action("homeLeft",
    on("ubuntu_gnome",()->waitUntilTime(val(34000))),
    on("xubuntu_xfce",()->waitUntilTime(val(42500))));
  final Action rootShown= action("rootShown",
    on("ubuntu_gnome",()->waitUntilTime(val(37000))),
    on("xubuntu_xfce",()->waitUntilTime(val(46500))));
  final Action openData= action("openData",
    on("ubuntu_gnome",()->doubleClick(val(790),val(539))),
    on("xubuntu_xfce",()->doubleClick(val(745),val(505))));
  final Action dataShown= action("dataShown",
    on("ubuntu_gnome",()->waitUntilTime(val(40000))),
    on("xubuntu_xfce",()->waitUntilTime(val(50000))));
  final Action openFilesIO= action("openFilesIO",
    on("ubuntu_gnome",()->doubleClick(val(940),val(473))),
    on("xubuntu_xfce",()->doubleClick(val(745),val(485))));
  final Action filesIOShown= action("filesIOShown",
    on("ubuntu_gnome",()->waitUntilTime(val(43000))),
    on("xubuntu_xfce",()->waitUntilTime(val(54000))));
  final Action openAttic= action("openAttic",
    on("ubuntu_gnome",()->doubleClick(val(790),val(473))),
    on("xubuntu_xfce",()->doubleClick(val(745),val(465))));
  final Action atticShown= action("atticShown",
    on("ubuntu_gnome",()->waitUntilTime(val(46000))),
    on("xubuntu_xfce",()->waitUntilTime(val(57500))));
  final Action pickInner= action("pickInner",
    on("ubuntu_gnome",()->click(val(790),val(473))),
    on("xubuntu_xfce",()->click(val(745),val(465))));
  final Action open= action("open",
    on("ubuntu_gnome",()->click(val(1116),val(712))),
    on("xubuntu_xfce",()->click(val(1080),val(705))));
  final Action tileShown= action("tileShown",
    on("ubuntu_gnome",()->waitUntilTime(val(53000))),
    on("xubuntu_xfce",()->waitUntilTime(val(66500))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",()->waitUntilTime(val(60000))),
    on("xubuntu_xfce",()->waitUntilTime(val(75000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var attic= filesIOFolder.resolve("attic");
    var inner= attic.resolve("inner");
    Fs.rmTree(attic);
    Fs.writeUtf8(inner.resolve("inner.fearless"),"\n");
    Fs.writeUtf8(inner.resolve("_inner").resolve("_rank_app.fear"),"use base.Main as Main;\n\nHello:Main{s->base.Debug#(\"inner\")}\n");
    var files= Fs.walk(attic,s->s.sorted().toList());
    launchScript("first");
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    var center= new Rectangle(760,460,400,160);
    var empty= look();
    managerMenu.go();
    addFolder.go();
    chooserShown.go();
    var chooser= look();
    changed(empty,center);
    upLevel.go();
    homeLeft.go();
    upLevel.go();
    rootShown.go();
    openData.go();
    dataShown.go();
    openFilesIO.go();
    filesIOShown.go();
    openAttic.go();
    atticShown.go();
    stabilize();
    checkContent(info,"{\n  \"projects\": {},\n  \"systemExtensions\": []\n}\n");
    assertEquals(files,Fs.walk(attic,s->s.sorted().toList()));
    changed(empty,center);
    var title= new Rectangle(740,380,500,40);
    same(chooser,title);
    pickInner.go();
    open.go();
    tileShown.go();
    changed(chooser,title);
    stabilize();
    checkContent(info,"""
      {
        "projects": {
          "inner": {
            "path": "Str:%s",
            "kind": "idle"
          }
        },
        "systemExtensions": []
      }
      """.formatted(slashed(inner)));
    assertEquals(List.of(),Fs.walk(attic,s->s.filter(p->p.getParent().equals(attic) && Files.isRegularFile(p)).toList()));
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
  }
}

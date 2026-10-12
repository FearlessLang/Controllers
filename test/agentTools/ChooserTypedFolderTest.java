package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Rectangle;
import java.nio.file.Files;
import java.util.List;

import tools.Fs;

final class ChooserTypedFolderTest extends ManagerTest{
  final Action appsShown= action("appsShown",on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",on("ubuntu_gnome",()->waitUntilTime(val(18000))));
  final Action chooserShown= action("chooserShown",on("ubuntu_gnome",()->waitUntilTime(val(21500))));
  final Action fileName= action("fileName",on("ubuntu_gnome",()->click(val(2000),val(1180))));
  final Action shelfShown= action("shelfShown",on("ubuntu_gnome",()->waitUntilTime(val(30000))));
  final Action innerTyped= action("innerTyped",on("ubuntu_gnome",()->click(val(2000),val(1180))));
  final Action tileShown= action("tileShown",on("ubuntu_gnome",()->waitUntilTime(val(36000))));
  final Action managerQuit= action("managerQuit",on("ubuntu_gnome",()->waitUntilTime(val(41000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var shelf= filesIOFolder.resolve("shelf");
    var inner= shelf.resolve("inner");
    Fs.rmTree(shelf);
    Fs.writeUtf8(inner.resolve("inner.fearless"),"\n");
    Fs.writeUtf8(inner.resolve("_inner").resolve("_rank_app.fear"),"use base.Main as Main;\n\nHello:Main{s->base.Debug#(\"inner\")}\n");
    var files= Fs.walk(shelf,s->s.sorted().toList());
    launchScript("first");
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    var center= new Rectangle(1720,1000,400,160);
    var empty= look();
    managerMenu.go();
    addFolder.go();
    chooserShown.go();
    var chooser= look();
    changed(empty,center);
    fileName.go();
    type(shelf+"\n");
    shelfShown.go();
    stabilize();
    checkContent(info,"{\n  \"projects\": {},\n  \"systemExtensions\": []\n}\n");
    assertEquals(files,Fs.walk(shelf,s->s.sorted().toList()));
    changed(empty,center);
    same(chooser,new Rectangle(1700,920,500,40));
    innerTyped.go();
    type("inner\n");
    tileShown.go();
    same(empty,center);
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
    assertEquals(List.of(),Fs.walk(shelf,s->s.filter(p->p.getParent().equals(shelf) && Files.isRegularFile(p)).toList()));
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
  }
}

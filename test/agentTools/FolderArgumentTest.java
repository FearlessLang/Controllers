package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Rectangle;
import java.util.List;

import tools.Fs;

final class FolderArgumentTest extends ManagerTest{
  final Action appsShown= action("appsShown",on("ubuntu_gnome",()->waitUntilTime(val(3500))));
  final Action terminalShown= action("terminalShown",on("ubuntu_gnome",()->waitUntilTime(val(8500))));
  final Action managerShown= action("managerShown",on("ubuntu_gnome",()->waitUntilTime(val(26000))));
  final Action appsShownSecond= action("appsShownSecond",on("ubuntu_gnome",()->waitUntilTime(val(29000))));
  final Action terminalShownSecond= action("terminalShownSecond",on("ubuntu_gnome",()->waitUntilTime(val(34000))));
  final Action registered= action("registered",on("ubuntu_gnome",()->waitUntilTime(val(45000))));
  final Action managerQuit= action("managerQuit",on("ubuntu_gnome",()->waitUntilTime(val(52000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var loose= filesIOFolder.resolve("loose");
    Fs.rmTree(loose);
    Fs.writeUtf8(loose.resolve("readme.txt"),"notes\n");
    launchScript("first");
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    var tiles= new Rectangle(69,95,256,85);
    var center= new Rectangle(760,460,400,160);
    var empty= look();
    launchScript("second",loose);
    runInTerminal(appsShownSecond,terminalShownSecond);
    registered.go();
    changed(empty,tiles);
    same(empty,center);
    stabilize();
    checkContent(List.of("second.exit"),"0\n");
    checkContent(info,"""
      {
        "projects": {
          "loose": {
            "path": "Str:%s",
            "kind": "idle"
          }
        },
        "systemExtensions": []
      }
      """.formatted(slashed(loose)));
    assertEquals(List.of("loose.fearless","readme.txt"),Fs.walk(loose,s->s.filter(p->!p.equals(loose)).map(p->p.getFileName().toString()).sorted().toList()));
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
  }
}

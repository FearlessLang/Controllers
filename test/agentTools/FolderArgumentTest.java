package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Rectangle;
import java.util.List;

import tools.Fs;

final class FolderArgumentTest extends ManagerTest{
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitAfter(val(3500))),
    on("xubuntu_xfce",()->waitAfter(val(4500))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitAfter(val(5000))),
    on("xubuntu_xfce",()->waitAfter(val(6500))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitAfter(val(17500))),
    on("xubuntu_xfce",()->waitAfter(val(21500))));
  final Action appsShownSecond= action("appsShownSecond",
    on("ubuntu_gnome",()->waitAfter(val(3000))),
    on("xubuntu_xfce",()->waitAfter(val(4000))));
  final Action terminalShownSecond= action("terminalShownSecond",
    on("ubuntu_gnome",()->waitAfter(val(5000))),
    on("xubuntu_xfce",()->waitAfter(val(6000))));
  final Action registered= action("registered",
    on("ubuntu_gnome",()->waitAfter(val(11000))),
    on("xubuntu_xfce",()->waitAfter(val(14000))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",()->waitAfter(val(7000))),
    on("xubuntu_xfce",()->waitAfter(val(8500))));
  @Override void walk() throws Throwable{
    noManagerData();
    var loose= filesIOFolder.resolve("loose");
    Fs.rmTree(loose);
    Fs.writeUtf8(loose.resolve("readme.txt"),"notes\n");
    launchScript("first");
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    var tiles= inWindow(69,95,256,85);
    var empty= look();
    launchScript("second",loose);
    runInTerminal(appsShownSecond,terminalShownSecond);
    registered.go();
    changed(empty,tiles);
    var shown= look();
    assertEquals(0L,differing(shown.getSubimage(760,460,400,160),shown.getSubimage(760,860,400,160),new Rectangle(0,0,400,160)));
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

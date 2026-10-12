package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Rectangle;
import java.nio.file.Path;
import java.util.List;

import tools.Fs;

final class NestedFolderTest extends ManagerTest{
  final Path nest= filesIOFolder.resolve("nest");
  final Path inner= nest.resolve("inner");
  final Path source= inner.resolve("_inner");
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(3500))),
    on("xubuntu_xfce",()->waitUntilTime(val(4500))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(8500))),
    on("xubuntu_xfce",()->waitUntilTime(val(11000))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(26000))),
    on("xubuntu_xfce",()->waitUntilTime(val(32500))));
  final Action appsShownNest= action("appsShownNest",
    on("ubuntu_gnome",()->waitUntilTime(val(29000))),
    on("xubuntu_xfce",()->waitUntilTime(val(36500))));
  final Action terminalShownNest= action("terminalShownNest",
    on("ubuntu_gnome",()->waitUntilTime(val(34000))),
    on("xubuntu_xfce",()->waitUntilTime(val(42500))));
  final Action noteShown= action("noteShown",
    on("ubuntu_gnome",()->waitUntilTime(val(44000))),
    on("xubuntu_xfce",()->waitUntilTime(val(55000))));
  final Action appsShownSource= action("appsShownSource",
    on("ubuntu_gnome",()->waitUntilTime(val(47500))),
    on("xubuntu_xfce",()->waitUntilTime(val(59500))));
  final Action terminalShownSource= action("terminalShownSource",
    on("ubuntu_gnome",()->waitUntilTime(val(52500))),
    on("xubuntu_xfce",()->waitUntilTime(val(66000))));
  final Action noteShownAgain= action("noteShownAgain",
    on("ubuntu_gnome",()->waitUntilTime(val(63000))),
    on("xubuntu_xfce",()->waitUntilTime(val(79000))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",()->waitUntilTime(val(71500))),
    on("xubuntu_xfce",()->waitUntilTime(val(89500))));
  @Override void walk() throws Throwable{
    noManagerData();
    Fs.rmTree(nest);
    Fs.writeUtf8(inner.resolve("inner.fearless"),"\n");
    Fs.writeUtf8(nest.resolve("nest.fearless"),"\n");
    Fs.writeUtf8(source.resolve("_rank_app.fear"),"use base.Main as Main;\n\nHello:Main{s->base.Debug#(\"inner\")}\n");
    Fs.writeUtf8(source.resolve("source.fearless"),"\n");
    var files= tree();
    var remembered= """
      {
        "projects": {
          "inner": {
            "path": "Str:%s",
            "kind": "idle"
          }
        },
        "systemExtensions": []
      }
      """.formatted(slashed(inner));
    launchScript("first",inner.resolve("inner.fearless"));
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    stabilize();
    checkContent(info,remembered);
    var center= new Rectangle(760,460,400,160);
    var before= look();
    launchScript("second",nest.resolve("nest.fearless"));
    runInTerminal(appsShownNest,terminalShownNest);
    noteShown.go();
    changed(before,center);
    stabilize();
    checkContent(List.of("second.exit"),"0\n");
    checkContent(info,remembered);
    assertEquals(files,tree());
    closeDialog.go();
    launchScript("third",source.resolve("source.fearless"));
    runInTerminal(appsShownSource,terminalShownSource);
    noteShownAgain.go();
    changed(before,center);
    stabilize();
    checkContent(List.of("third.exit"),"0\n");
    checkContent(info,remembered);
    assertEquals(files,tree());
    closeDialog.go();
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
  }
  List<Path> tree(){ return Fs.walk(nest,s->s.sorted().toList()); }
}

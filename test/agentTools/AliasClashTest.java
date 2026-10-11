package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Rectangle;
import java.nio.file.Files;
import java.util.List;

import tools.Fs;

final class AliasClashTest extends ManagerTest{
  final Action appsShown= action("appsShown",on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",on("ubuntu_gnome",()->waitUntilTime(val(18000))));
  final Action appsShownSecond= action("appsShownSecond",on("ubuntu_gnome",()->waitUntilTime(val(19500))));
  final Action terminalShownSecond= action("terminalShownSecond",on("ubuntu_gnome",()->waitUntilTime(val(23000))));
  final Action secondShown= action("secondShown",on("ubuntu_gnome",()->waitUntilTime(val(31000))));
  final Action check= action("check",on("ubuntu_gnome",()->click(val(530),val(112))));
  final Action checked= action("checked",on("ubuntu_gnome",()->waitUntilTime(val(34000))));
  final Action managerQuit= action("managerQuit",on("ubuntu_gnome",()->waitUntilTime(val(39000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var twins= filesIOFolder.resolve("twins");
    var first= twins.resolve("a").resolve("twin");
    var second= twins.resolve("b").resolve("twin");
    Fs.rmTree(twins);
    for (var f: List.of(first,second)){
      Fs.writeUtf8(f.resolve("twin.fearless"),"\n");
      Fs.writeUtf8(f.resolve("_twin").resolve("_rank_app.fear"),"use base.Main as Main;\n\nHello:Main{s->base.Debug#(\"twin\")}\n");
    }
    launchScript("first",first.resolve("twin.fearless"));
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    launchScript("second",second.resolve("twin.fearless"));
    runInTerminal(appsShownSecond,terminalShownSecond);
    secondShown.go();
    stabilize();
    checkContent(List.of("second.exit"),"0\n");
    checkContent(info,"""
      {
        "projects": {
          "twin": {
            "path": "Str:%s",
            "kind": "idle"
          },
          "twin2": {
            "path": "Str:%s",
            "kind": "idle"
          }
        },
        "systemExtensions": []
      }
      """.formatted(slashed(first),slashed(second)));
    assertEquals(List.of("a/twin/_twin/_rank_app.fear","a/twin/twin.fearless","b/twin/_twin/_rank_app.fear","b/twin/twin2.fearless"),
      Fs.walk(twins,s->s.filter(Files::isRegularFile).map(p->slashed(twins.relativize(p))).sorted().toList()));
    checkContent(List.of("twins","b","twin","twin2.fearless"),"\n");
    var output= new Rectangle(410,190,1000,60);
    var before= look();
    check.go();
    checked.go();
    changed(before,output);
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
  }
}

package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.util.List;

import tools.Fs;

final class AliasClashTest extends ManagerTest{
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(18500))));
  final Action focusTiles= action("focusTiles",
    on("ubuntu_gnome",()->click(val(200),val(1500))),
    on("windows",()->click(val(200),val(400))));
  final Action appsShownSecond= action("appsShownSecond",
    on("ubuntu_gnome",()->waitUntilTime(val(21000))));
  final Action terminalShownSecond= action("terminalShownSecond",
    on("ubuntu_gnome",()->waitUntilTime(val(24500))));
  final Action noteShown= action("noteShown",
    on("ubuntu_gnome",()->waitUntilTime(val(36500))));
  final Action ok= action("ok",
    on("ubuntu_gnome",()->click(val(1952),val(1213))),
    on("windows",()->click(val(639),val(443))));
  final Action noteGone= action("noteGone",
    on("ubuntu_gnome",()->waitUntilTime(val(38000))));
  final Action check= action("check",
    on("ubuntu_gnome",()->click(val(164),val(111))),
    on("windows",()->click(val(102),val(67))));
  final Action checked= action("checked",
    on("ubuntu_gnome",()->waitUntilTime(val(52000))));
  final Action appsShownToEnd= action("appsShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(53500))));
  final Action terminalShownToEnd= action("terminalShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(57000))));
  final Action managerEnded= action("managerEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(67000))));
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
    launchScript("first",first);
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    stabilize();
    checkContent(info,"""
      {
        "twin": {
          "path": "Str:%s",
          "kind": "idle"
        }
      }
      """.formatted(slashed(first)));
    focusTiles.go();
    keys(KeyEvent.VK_F8);
    keys(KeyEvent.VK_HOME);
    launchScript("second",second);
    runInTerminal(appsShownSecond,terminalShownSecond);
    noteShown.go();
    stabilize();
    checkContent(List.of("second.exit"),"0\n");
    checkContent(notes,"""
      Fearless keeps track of this project folder as "twin2", not as "twin".

      The manager registered:
        %s
      A project name uses only lowercase letters, digits and underscores,
      starts with a letter or an underscore, is not a name the file system
      reserves ("con", "prn", "aux", "nul", "com1" to "com9", "lpt1" to "lpt9"),
      and is not the name of another project Fearless keeps track of.

      The marker file "twin2.fearless" in that folder holds the name: rename it to change the name.
      """.formatted(second));
    checkContent(info,"""
      {
        "twin": {
          "path": "Str:%s",
          "kind": "idle"
        },
        "twin2": {
          "path": "Str:%s",
          "kind": "idle"
        }
      }
      """.formatted(slashed(first),slashed(second)));
    assertEquals(List.of("a/twin/_twin/_rank_app.fear","a/twin/twin.fearless","b/twin/_twin/_rank_app.fear","b/twin/twin2.fearless"),
      Fs.walk(twins,s->s.filter(Files::isRegularFile).map(p->slashed(twins.relativize(p))).sorted().toList()));
    checkContent(List.of("twins","b","twin","twin2.fearless"),"\n");
    ok.go();
    noteGone.go();
    check.go();
    checked.go();
    stabilize();
    checkContent(List.of(data,"eclipse","twin2","console.txt"),"--- ok: no problem found ---\n");
    checkContent(List.of(data,"eclipse","twin","console.txt"),"");
    endScript();
    runInTerminal(appsShownToEnd,terminalShownToEnd);
    managerEnded.go();
    stabilize();
    checkContent(List.of("first.exit"),"137\n");
  }
}

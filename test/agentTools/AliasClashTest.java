package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.util.List;

import tools.Fs;

final class AliasClashTest extends ManagerTest{
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome", () -> waitAfter(val(17500))),
    on("xubuntu_xfce", () -> waitAfter(val(21500))));
  final Action openTwins= action("openTwins",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action openA= action("openA",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action openB= action("openB",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action openTwin= action("openTwin",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action openTwinMarker= action("openTwinMarker",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action firstShown= action("firstShown",
    on("ubuntu_gnome", () -> waitAfter(val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(null))));
  final Action secondShown= action("secondShown",
    on("ubuntu_gnome", () -> waitAfter(val(11000))),
    on("xubuntu_xfce", () -> waitAfter(val(14000))));
  final Action check= action("check",
    on("ubuntu_gnome", () -> waitClick(val(0), val(529), val(113))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(468), val(93))));
  final Action checked= action("checked",
    on("ubuntu_gnome", () -> waitAfter(val(4500))),
    on("xubuntu_xfce", () -> waitAfter(val(5500))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome", () -> waitAfter(val(7000))),
    on("xubuntu_xfce", () -> waitAfter(val(9000))));
  @Override void walk() throws Throwable{
    var twins= files.resolve("twins");
    var first= twins.resolve("a").resolve("twin");
    var second= twins.resolve("b").resolve("twin");
    for (var f : List.of(first, second)){
      Fs.writeUtf8(f.resolve("twin.fearless"), "\n");
      Fs.writeUtf8(f.resolve("_twin").resolve("_rank_app.fear"), "use base.Main as Main;\n\nHello:Main{s->base.Debug#(\"twin\")}\n");
    }
    var bare= look();
    start();
    managerShown.go();
    toFiles();
    openTwins.go();
    openA.go();
    openTwin.go();
    openTwinMarker.go();
    closeFiles();
    firstShown.go();
    toFiles();
    openTwins.go();
    openB.go();
    openTwin.go();
    openTwinMarker.go();
    closeFiles();
    secondShown.go();
    stabilize();
    checkContent(info, """
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
      """.formatted(slashed(first), slashed(second)));
    assertEquals(List.of("a/twin/_twin/_rank_app.fear", "a/twin/twin.fearless", "b/twin/_twin/_rank_app.fear", "b/twin/twin2.fearless"),
      Fs.walk(twins, s -> s.filter(Files::isRegularFile).map(p -> slashed(twins.relativize(p))).sorted().toList()));
    checkContent(files.resolve("twins/b/twin/twin2.fearless"), "\n");
    var output= inWindow(410, 168, 1000, 80);
    var before= look();
    check.go();
    checked.go();
    changed(before, output);
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    same(bare, output);
  }
}

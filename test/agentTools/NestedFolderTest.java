package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Rectangle;
import java.nio.file.Path;
import java.util.List;

import tools.Fs;

final class NestedFolderTest extends ManagerTest{
  final Path nest= files.resolve("nest");
  final Path inner= nest.resolve("inner");
  final Path source= inner.resolve("_inner");
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome", () -> waitAfter(val(17500))),
    on("xubuntu_xfce", () -> waitAfter(val(21500))));
  final Action noteShown= action("noteShown",
    on("ubuntu_gnome", () -> waitAfter(val(10000))),
    on("xubuntu_xfce", () -> waitAfter(val(12500))));
  final Action noteShownAgain= action("noteShownAgain",
    on("ubuntu_gnome", () -> waitAfter(val(10500))),
    on("xubuntu_xfce", () -> waitAfter(val(13000))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome", () -> waitAfter(val(8500))),
    on("xubuntu_xfce", () -> waitAfter(val(10500))));
  final Action openNest= action("openNest",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action openInner= action("openInner",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action openInnerMarker= action("openInnerMarker",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action projectShown= action("projectShown",
    on("ubuntu_gnome", () -> waitAfter(val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(null))));
  final Action openNestAgain= action("openNestAgain",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action openNestMarker= action("openNestMarker",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action okButton= action("okButton",
    on("ubuntu_gnome", () -> waitClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitClick(val(null), val(null), val(null))));
  final Action openNestThird= action("openNestThird",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action openInnerAgain= action("openInnerAgain",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action openUnderscoreInner= action("openUnderscoreInner",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action openSourceMarker= action("openSourceMarker",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action okButtonAgain= action("okButtonAgain",
    on("ubuntu_gnome", () -> waitClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitClick(val(null), val(null), val(null))));
  @Override void walk() throws Throwable{
    Fs.writeUtf8(inner.resolve("inner.fearless"), "\n");
    Fs.writeUtf8(nest.resolve("nest.fearless"), "\n");
    Fs.writeUtf8(source.resolve("_rank_app.fear"), "use base.Main as Main;\n\nHello:Main{s->base.Debug#(\"inner\")}\n");
    Fs.writeUtf8(source.resolve("source.fearless"), "\n");
    var tree= tree();
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
    start();
    managerShown.go();
    toFiles();
    openNest.go();
    openInner.go();
    openInnerMarker.go();
    closeFiles();
    projectShown.go();
    stabilize();
    checkContent(info, remembered);
    var center= new Rectangle(760, 460, 400, 160);
    var before= look();
    toFiles();
    openNestAgain.go();
    openNestMarker.go();
    closeFiles();
    noteShown.go();
    changed(before, center);
    stabilize();
    checkContent(info, remembered);
    assertEquals(tree, tree());
    okButton.go();
    toFiles();
    openNestThird.go();
    openInnerAgain.go();
    openUnderscoreInner.go();
    openSourceMarker.go();
    closeFiles();
    noteShownAgain.go();
    changed(before, center);
    stabilize();
    checkContent(info, remembered);
    assertEquals(tree, tree());
    okButtonAgain.go();
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
  }
  List<Path> tree(){ return Fs.walk(nest, s -> s.sorted().toList()); }
}

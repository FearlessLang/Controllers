package agentTools;

import java.awt.Rectangle;

final class EditMetadataConflictTest extends ManagerTest{
  static final String two= """
    {
      "projects": {
        "hello_world": {
          "path": "Str:%s",
          "kind": "%s"
        },
        "start": {
          "path": "Str:%s",
          "kind": "idle"
        }
      },
      "systemExtensions": []
    }
    """;
  final Action openHelloWorld= action("openHelloWorld",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action openHelloWorldMarker= action("openHelloWorldMarker",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action projectShown= action("projectShown",
    on("ubuntu_gnome", () -> waitAfter(val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(null))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome", () -> waitAfter(val(17500))),
    on("xubuntu_xfce", () -> waitAfter(val(21500))));
  final Action editorShown= action("editorShown",
    on("ubuntu_gnome", () -> waitAfter(val(5500))),
    on("xubuntu_xfce", () -> waitAfter(val(7000))));
  final Action kindIdle= action("kindIdle",
    on("ubuntu_gnome", () -> waitDoubleClick(val(0), val(730), val(376))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(0), val(697), val(373))));
  final Action openHelloStackTraces= action("openHelloStackTraces",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action openStartMarker= action("openStartMarker",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action secondHanded= action("secondHanded",
    on("ubuntu_gnome", () -> waitAfter(val(11000))),
    on("xubuntu_xfce", () -> waitAfter(val(14000))));
  final Action commit= action("commit",
    on("ubuntu_gnome", () -> waitClick(val(0), val(1278), val(830))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(1245), val(827))));
  final Action noteShown= action("noteShown",
    on("ubuntu_gnome", () -> waitAfter(val(4000))),
    on("xubuntu_xfce", () -> waitAfter(val(5000))));
  final Action okButton= action("okButton",
    on("ubuntu_gnome", () -> waitClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitClick(val(null), val(null), val(null))));
  final Action close= action("close",
    on("ubuntu_gnome", () -> waitClick(val(0), val(1357), val(830))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(1324), val(827))));
  final Action editorClosed= action("editorClosed",
    on("ubuntu_gnome", () -> waitAfter(val(4000))),
    on("xubuntu_xfce", () -> waitAfter(val(5000))));
  final Action editorShownAgain= action("editorShownAgain",
    on("ubuntu_gnome", () -> waitAfter(val(4500))),
    on("xubuntu_xfce", () -> waitAfter(val(5500))));
  final Action kindIdleAgain= action("kindIdleAgain",
    on("ubuntu_gnome", () -> waitDoubleClick(val(0), val(730), val(376))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(0), val(697), val(373))));
  final Action commitAgain= action("commitAgain",
    on("ubuntu_gnome", () -> waitClick(val(0), val(1278), val(830))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(1245), val(827))));
  final Action codeSaved= action("codeSaved",
    on("ubuntu_gnome", () -> waitAfter(val(8500))),
    on("xubuntu_xfce", () -> waitAfter(val(10500))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome", () -> waitAfter(val(7000))),
    on("xubuntu_xfce", () -> waitAfter(val(9000))));
  @Override void walk() throws Throwable{
    var project= project("helloWorld");
    var other= project("helloStackTraces");
    var bare= look();
    start();
    managerShown.go();
    toFiles();
    openHelloWorld.go();
    openHelloWorldMarker.go();
    projectShown.go();
    managerMenu.go();
    editMetadata.go();
    editorShown.go();
    kindIdle.go();
    type("code");
    toFiles();
    openHelloStackTraces.go();
    openStartMarker.go();
    secondHanded.go();
    var both= two.formatted(slashed(project), "idle", slashed(other));
    stabilize();
    checkContent(info, both);
    var center= new Rectangle(760, 460, 400, 160);
    var editor= look();
    commit.go();
    noteShown.go();
    changed(editor, center);
    stabilize();
    checkContent(info, both);
    okButton.go();
    same(editor, center);
    close.go();
    editorClosed.go();
    changed(editor, center);
    managerMenu.go();
    editMetadata.go();
    editorShownAgain.go();
    kindIdleAgain.go();
    type("code");
    commitAgain.go();
    codeSaved.go();
    stabilize();
    checkContent(info, two.formatted(slashed(project), "code", slashed(other)));
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    closeFiles();
    same(bare, center);
  }
}

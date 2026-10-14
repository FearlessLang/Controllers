package agentTools;

import java.awt.Rectangle;

final class EditMetadataInvalidTest extends ManagerTest{
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
  final Action commit= action("commit",
    on("ubuntu_gnome", () -> waitClick(val(0), val(1278), val(830))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(1245), val(827))));
  final Action noteShown= action("noteShown",
    on("ubuntu_gnome", () -> waitAfter(val(8500))),
    on("xubuntu_xfce", () -> waitAfter(val(10500))));
  final Action okButton= action("okButton",
    on("ubuntu_gnome", () -> waitClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitClick(val(null), val(null), val(null))));
  final Action close= action("close",
    on("ubuntu_gnome", () -> waitClick(val(0), val(1357), val(830))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(1324), val(827))));
  final Action editorClosed= action("editorClosed",
    on("ubuntu_gnome", () -> waitAfter(val(4500))),
    on("xubuntu_xfce", () -> waitAfter(val(6000))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome", () -> waitAfter(val(7000))),
    on("xubuntu_xfce", () -> waitAfter(val(8500))));
  @Override void walk() throws Throwable{
    var project= project("helloWorld");
    var idle= """
      {
        "projects": {
          "hello_world": {
            "path": "Str:%s",
            "kind": "idle"
          }
        },
        "systemExtensions": []
      }
      """.formatted(slashed(project));
    var bare= look();
    start();
    managerShown.go();
    toFiles();
    openHelloWorld.go();
    openHelloWorldMarker.go();
    closeFiles();
    projectShown.go();
    managerMenu.go();
    editMetadata.go();
    editorShown.go();
    kindIdle.go();
    type("lazy");
    var editor= look();
    commit.go();
    noteShown.go();
    var center= new Rectangle(760, 460, 400, 160);
    changed(editor, center);
    stabilize();
    checkContent(info, idle);
    okButton.go();
    same(editor, center);
    close.go();
    editorClosed.go();
    changed(editor, center);
    stabilize();
    checkContent(info, idle);
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    same(bare, center);
  }
}

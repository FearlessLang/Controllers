package agentTools;

final class EditMetadataCommitTest extends ManagerTest{
  final Action editorArea= action("editorArea",
    on("ubuntu_gnome", () -> region(val(640), val(360), val(600), val(300))),
    on("xubuntu_xfce", () -> region(val(640), val(360), val(600), val(300))));
  final Action backToIdle= action("backToIdle",
    on("ubuntu_gnome", () -> waitClick(val(0), val(780), val(113))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(749), val(93))));
  static final String registry= """
    {
      "projects": {
        "hello_world": {
          "path": "Str:%s",
          "kind": "%s"
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
  final Action commit= action("commit",
    on("ubuntu_gnome", () -> waitClick(val(0), val(1278), val(830))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(1245), val(827))));
  final Action codeSaved= action("codeSaved",
    on("ubuntu_gnome", () -> waitAfter(val(8500))),
    on("xubuntu_xfce", () -> waitAfter(val(10500))));
  final Action idleSaved= action("idleSaved",
    on("ubuntu_gnome", () -> waitAfter(val(4500))),
    on("xubuntu_xfce", () -> waitAfter(val(6000))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome", () -> waitAfter(val(7000))),
    on("xubuntu_xfce", () -> waitAfter(val(8500))));
  @Override void walk() throws Throwable{
    var project= project("helloWorld");
    var bare= look();
    start();
    managerShown.go();
    toFiles();
    openHelloWorld.go();
    openHelloWorldMarker.go();
    projectShown.go();
    stabilize();
    checkContent(info, registry.formatted(slashed(project), "idle"));
    managerMenu.go();
    editMetadata.go();
    editorShown.go();
    var editor= look();
    kindIdle.go();
    type("code");
    commit.go();
    codeSaved.go();
    editorArea.go();
    changed(editor, region);
    stabilize();
    checkContent(info, registry.formatted(slashed(project), "code"));
    backToIdle.go();
    idleSaved.go();
    stabilize();
    checkContent(info, registry.formatted(slashed(project), "idle"));
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    closeFiles();
    same(bare, inWindow(100, 200, 200, 400));
  }
}

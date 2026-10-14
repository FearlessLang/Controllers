package agentTools;

final class EditMetadataAddProjectTest extends ManagerTest{
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
  final Action focusText= action("focusText",
    on("ubuntu_gnome", () -> waitClick(val(0), val(940), val(660))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(907), val(657))));
  final Action selectText= action("selectText",
    on("ubuntu_gnome", () -> waitDrag(val(null), val(null), val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDrag(val(null), val(null), val(null), val(null), val(null))));
  final Action commit= action("commit",
    on("ubuntu_gnome", () -> waitClick(val(0), val(1278), val(830))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(1245), val(827))));
  final Action bothSaved= action("bothSaved",
    on("ubuntu_gnome", () -> waitAfter(val(56000))),
    on("xubuntu_xfce", () -> waitAfter(val(70000))));
  final Action secondTile= action("secondTile",
    on("ubuntu_gnome", () -> waitClick(val(0), val(261), val(130))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(194), val(110))));
  final Action secondShown= action("secondShown",
    on("ubuntu_gnome", () -> waitAfter(val(3500))),
    on("xubuntu_xfce", () -> waitAfter(val(4500))));
  final Action check= action("check",
    on("ubuntu_gnome", () -> waitClick(val(0), val(523), val(113))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(474), val(93))));
  final Action checked= action("checked",
    on("ubuntu_gnome", () -> waitAfter(val(4000))),
    on("xubuntu_xfce", () -> waitAfter(val(5000))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome", () -> waitAfter(val(7000))),
    on("xubuntu_xfce", () -> waitAfter(val(8500))));
  @Override void walk() throws Throwable{
    var project= project("helloWorld");
    var other= project("helloStackTraces");
    var both= """
      {"projects": {"hello_world": {"path": "Str:%s", "kind": "idle"}, "start": {"path": "Str:%s", "kind": "idle"}}, "systemExtensions": []}
      """.formatted(slashed(project), slashed(other));
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
    focusText.go();
    selectText.go();
    type(both.strip());
    commit.go();
    bothSaved.go();
    stabilize();
    checkContent(info, """
      {
        "projects": {
          "hello_world": {
            "path": "Str:%s",
            "kind": "idle"
          },
          "start": {
            "path": "Str:%s",
            "kind": "idle"
          }
        },
        "systemExtensions": []
      }
      """.formatted(slashed(project), slashed(other)));
    secondTile.go();
    secondShown.go();
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

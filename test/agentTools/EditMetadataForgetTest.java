package agentTools;

final class EditMetadataForgetTest extends ManagerTest{
  final Action openHelloWorld= action("openHelloWorld",
    on("ubuntu_gnome",() -> waitDoubleClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",() -> waitDoubleClick(val(null),val(null),val(null))));
  final Action openHelloWorldMarker= action("openHelloWorldMarker",
    on("ubuntu_gnome",() -> waitDoubleClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",() -> waitDoubleClick(val(null),val(null),val(null))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",() -> waitAfter(val(17500))),
    on("xubuntu_xfce",() -> waitAfter(val(21500))));
  final Action openHelloStackTraces= action("openHelloStackTraces",
    on("ubuntu_gnome",() -> waitDoubleClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",() -> waitDoubleClick(val(null),val(null),val(null))));
  final Action openStartMarker= action("openStartMarker",
    on("ubuntu_gnome",() -> waitDoubleClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",() -> waitDoubleClick(val(null),val(null),val(null))));
  final Action secondShown= action("secondShown",
    on("ubuntu_gnome",() -> waitAfter(val(11000))),
    on("xubuntu_xfce",() -> waitAfter(val(14000))));
  final Action editorShown= action("editorShown",
    on("ubuntu_gnome",() -> waitAfter(val(5500))),
    on("xubuntu_xfce",() -> waitAfter(val(7000))));
  final Action focusText= action("focusText",
    on("ubuntu_gnome",() -> click(val(940),val(660))),
    on("xubuntu_xfce",() -> click(val(907),val(657))));
  final Action selectText= action("selectText",
    on("ubuntu_gnome",() -> waitDrag(val(null),val(null),val(null),val(null),val(null))),
    on("xubuntu_xfce",() -> waitDrag(val(null),val(null),val(null),val(null),val(null))));
  final Action commit= action("commit",
    on("ubuntu_gnome",() -> click(val(1278),val(830))),
    on("xubuntu_xfce",() -> click(val(1245),val(827))));
  final Action onlySaved= action("onlySaved",
    on("ubuntu_gnome",() -> waitAfter(val(43000))),
    on("xubuntu_xfce",() -> waitAfter(val(53500))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",() -> waitAfter(val(7000))),
    on("xubuntu_xfce",() -> waitAfter(val(9000))));
  @Override void walk() throws Throwable{
    var project= project("helloWorld");
    var other= project("helloStackTraces");
    var only= """
      {"projects": {"hello_world": {"path": "Str:%s", "kind": "idle"}}, "systemExtensions": []}
      """.formatted(slashed(project));
    var bare= look();
    start();
    managerShown.go();
    toFiles();
    openHelloWorld.go();
    openHelloWorldMarker.go();
    closeFiles();
    toFiles();
    openHelloStackTraces.go();
    openStartMarker.go();
    closeFiles();
    secondShown.go();
    managerMenu.go();
    editMetadata.go();
    editorShown.go();
    focusText.go();
    selectText.go();
    type(only.strip());
    commit.go();
    onlySaved.go();
    stabilize();
    checkContent(info,"""
      {
        "projects": {
          "hello_world": {
            "path": "Str:%s",
            "kind": "idle"
          }
        },
        "systemExtensions": []
      }
      """.formatted(slashed(project)));
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    same(bare,inWindow(100,200,200,400));
  }
}

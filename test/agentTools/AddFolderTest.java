package agentTools;

import java.awt.Rectangle;

final class AddFolderTest extends ManagerTest{
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome", () -> waitAfter(val(17500))),
    on("xubuntu_xfce", () -> waitAfter(val(21500))));
  final Action chooserShown= action("chooserShown",
    on("ubuntu_gnome", () -> waitAfter(val(5000))),
    on("xubuntu_xfce", () -> waitAfter(val(6500))));
  final Action cancel= action("cancel",
    on("ubuntu_gnome", () -> click(val(1195), val(712))),
    on("xubuntu_xfce", () -> click(val(1162), val(709))));
  final Action chooserGone= action("chooserGone",
    on("ubuntu_gnome", () -> waitAfter(val(3500))),
    on("xubuntu_xfce", () -> waitAfter(val(4500))));
  final Action chooserShownAgain= action("chooserShownAgain",
    on("ubuntu_gnome", () -> waitAfter(val(4500))),
    on("xubuntu_xfce", () -> waitAfter(val(5500))));
  final Action fileName= action("fileName",
    on("ubuntu_gnome", () -> click(val(1040), val(640))),
    on("xubuntu_xfce", () -> click(val(1007), val(637))));
  final Action openButton= action("openButton",
    on("ubuntu_gnome", () -> waitClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitClick(val(null), val(null), val(null))));
  final Action tileShown= action("tileShown",
    on("ubuntu_gnome", () -> waitAfter(val(13500))),
    on("xubuntu_xfce", () -> waitAfter(val(17000))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome", () -> waitAfter(val(7000))),
    on("xubuntu_xfce", () -> waitAfter(val(8500))));
  @Override void walk() throws Throwable{
    var project= project("helloWorld");
    var bare= look();
    start();
    managerShown.go();
    var center= new Rectangle(760, 460, 400, 160);
    var tiles= inWindow(69, 95, 128, 85);
    var empty= look();
    managerMenu.go();
    addFolder.go();
    chooserShown.go();
    changed(empty, center);
    cancel.go();
    chooserGone.go();
    same(empty, center);
    stabilize();
    checkContent(info, "{\n  \"projects\": {},\n  \"systemExtensions\": []\n}\n");
    managerMenu.go();
    addFolder.go();
    chooserShownAgain.go();
    fileName.go();
    type(project.toString());
    openButton.go();
    tileShown.go();
    changed(empty, tiles);
    stabilize();
    checkContent(info, """
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
    same(bare, tiles);
  }
}

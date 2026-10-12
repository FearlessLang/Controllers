package agentTools;

import java.awt.Rectangle;
import java.util.List;

final class AddFolderTest extends ManagerTest{
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(3500))),
    on("xubuntu_xfce",()->waitUntilTime(val(4500))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(8500))),
    on("xubuntu_xfce",()->waitUntilTime(val(11000))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(26000))),
    on("xubuntu_xfce",()->waitUntilTime(val(32500))));
  final Action chooserShown= action("chooserShown",
    on("ubuntu_gnome",()->waitUntilTime(val(31000))),
    on("xubuntu_xfce",()->waitUntilTime(val(39000))));
  final Action cancel= action("cancel",
    on("ubuntu_gnome",()->click(val(1195),val(712))),
    on("xubuntu_xfce",()->click(val(1162),val(709))));
  final Action chooserGone= action("chooserGone",
    on("ubuntu_gnome",()->waitUntilTime(val(34500))),
    on("xubuntu_xfce",()->waitUntilTime(val(43500))));
  final Action chooserShownAgain= action("chooserShownAgain",
    on("ubuntu_gnome",()->waitUntilTime(val(39000))),
    on("xubuntu_xfce",()->waitUntilTime(val(49000))));
  final Action fileName= action("fileName",
    on("ubuntu_gnome",()->click(val(1040),val(640))),
    on("xubuntu_xfce",()->click(val(1007),val(637))));
  final Action tileShown= action("tileShown",
    on("ubuntu_gnome",()->waitUntilTime(val(52500))),
    on("xubuntu_xfce",()->waitUntilTime(val(66000))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",()->waitUntilTime(val(59500))),
    on("xubuntu_xfce",()->waitUntilTime(val(74500))));
  @Override void walk() throws Throwable{
    noManagerData();
    var project= project("helloWorld");
    launchScript("first");
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    var center= new Rectangle(760,460,400,160);
    var tiles= inWindow(69,95,128,85);
    var empty= look();
    managerMenu.go();
    addFolder.go();
    chooserShown.go();
    changed(empty,center);
    cancel.go();
    chooserGone.go();
    same(empty,center);
    stabilize();
    checkContent(info,"{\n  \"projects\": {},\n  \"systemExtensions\": []\n}\n");
    managerMenu.go();
    addFolder.go();
    chooserShownAgain.go();
    fileName.go();
    type(project+"\n");
    tileShown.go();
    changed(empty,tiles);
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
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
  }
}

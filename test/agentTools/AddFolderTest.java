package agentTools;

import java.awt.Rectangle;
import java.util.List;

final class AddFolderTest extends ManagerTest{
  final Action appsShown= action("appsShown",on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",on("ubuntu_gnome",()->waitUntilTime(val(18000))));
  final Action chooserShown= action("chooserShown",on("ubuntu_gnome",()->waitUntilTime(val(21500))));
  final Action cancel= action("cancel",on("ubuntu_gnome",()->click(val(2155),val(1252))));
  final Action chooserGone= action("chooserGone",on("ubuntu_gnome",()->waitUntilTime(val(23500))));
  final Action chooserShownAgain= action("chooserShownAgain",on("ubuntu_gnome",()->waitUntilTime(val(26500))));
  final Action fileName= action("fileName",on("ubuntu_gnome",()->click(val(2000),val(1180))));
  final Action tileShown= action("tileShown",on("ubuntu_gnome",()->waitUntilTime(val(36000))));
  final Action managerQuit= action("managerQuit",on("ubuntu_gnome",()->waitUntilTime(val(41000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var project= project("helloWorld");
    launchScript("first");
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    var center= new Rectangle(1720,1000,400,160);
    var tiles= new Rectangle(69,95,128,85);
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

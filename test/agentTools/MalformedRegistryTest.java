package agentTools;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Rectangle;
import java.nio.file.Files;

import tools.Fs;

final class MalformedRegistryTest extends ManagerTest{
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",() -> waitAfter(val(17500))),
    on("xubuntu_xfce",() -> waitAfter(val(21500))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",() -> waitAfter(val(7000))),
    on("xubuntu_xfce",() -> waitAfter(val(9000))));
  final Action errorShown= action("errorShown",
    on("ubuntu_gnome",() -> waitAfter(val(19000))),
    on("xubuntu_xfce",() -> waitAfter(val(24000))));
  final Action errorDismissed= action("errorDismissed",
    on("ubuntu_gnome",() -> waitAfter(val(4500))),
    on("xubuntu_xfce",() -> waitAfter(val(5500))));
  final Action freshShown= action("freshShown",
    on("ubuntu_gnome",() -> waitAfter(val(18000))),
    on("xubuntu_xfce",() -> waitAfter(val(22500))));
  final Action freshQuit= action("freshQuit",
    on("ubuntu_gnome",() -> waitAfter(val(7000))),
    on("xubuntu_xfce",() -> waitAfter(val(9000))));
  final Action openHelloWorld= action("openHelloWorld",
    on("ubuntu_gnome",() -> waitDoubleClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",() -> waitDoubleClick(val(null),val(null),val(null))));
  final Action openHelloWorldMarker= action("openHelloWorldMarker",
    on("ubuntu_gnome",() -> waitDoubleClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",() -> waitDoubleClick(val(null),val(null),val(null))));
  final Action projectShown= action("projectShown",
    on("ubuntu_gnome",() -> waitAfter(val(null))),
    on("xubuntu_xfce",() -> waitAfter(val(null))));
  final Action okButton= action("okButton",
    on("ubuntu_gnome",() -> waitClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",() -> waitClick(val(null),val(null),val(null))));
  @Override void walk() throws Throwable{
    var project= project("helloWorld");
    var remembered= """
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
    start();
    managerShown.go();
    toFiles();
    openHelloWorld.go();
    openHelloWorldMarker.go();
    closeFiles();
    projectShown.go();
    stabilize();
    checkContent(info,remembered);
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    Fs.writeUtf8(info,remembered.substring(0,remembered.length()-2));
    stabilize();
    var before= look();
    var center= new Rectangle(760,460,400,160);
    start();
    errorShown.go();
    changed(before,center);
    stabilize();
    assertTrue(Files.exists(filesIOFolder.resolve(data)));
    okButton.go();
    errorDismissed.go();
    same(before,center);
    stabilize();
    assertFalse(Files.exists(filesIOFolder.resolve(data)));
    start();
    freshShown.go();
    stabilize();
    checkContent(info,"{\n  \"projects\": {},\n  \"systemExtensions\": []\n}\n");
    managerMenu.go();
    quitManager.go();
    freshQuit.go();
  }
}

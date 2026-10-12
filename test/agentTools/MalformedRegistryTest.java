package agentTools;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Rectangle;
import java.nio.file.Files;
import java.util.List;

import tools.Fs;

final class MalformedRegistryTest extends ManagerTest{
  final Action appsShown= action("appsShown",on("ubuntu_gnome",()->waitUntilTime(val(3500))));
  final Action terminalShown= action("terminalShown",on("ubuntu_gnome",()->waitUntilTime(val(8500))));
  final Action managerShown= action("managerShown",on("ubuntu_gnome",()->waitUntilTime(val(26000))));
  final Action managerQuit= action("managerQuit",on("ubuntu_gnome",()->waitUntilTime(val(33000))));
  final Action appsShownMalformed= action("appsShownMalformed",on("ubuntu_gnome",()->waitUntilTime(val(36500))));
  final Action terminalShownMalformed= action("terminalShownMalformed",on("ubuntu_gnome",()->waitUntilTime(val(41500))));
  final Action errorShown= action("errorShown",on("ubuntu_gnome",()->waitUntilTime(val(60500))));
  final Action errorDismissed= action("errorDismissed",on("ubuntu_gnome",()->waitUntilTime(val(65000))));
  final Action appsShownFresh= action("appsShownFresh",on("ubuntu_gnome",()->waitUntilTime(val(68500))));
  final Action terminalShownFresh= action("terminalShownFresh",on("ubuntu_gnome",()->waitUntilTime(val(73500))));
  final Action freshShown= action("freshShown",on("ubuntu_gnome",()->waitUntilTime(val(91500))));
  final Action freshQuit= action("freshQuit",on("ubuntu_gnome",()->waitUntilTime(val(98500))));
  @Override void walk() throws Throwable{
    noManagerData();
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
    launchScript("first",project.resolve("hello_world.fearless"));
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    stabilize();
    checkContent(info,remembered);
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
    Fs.writeUtf8(filesIOFolder.resolve(data).resolve("projects.info"),remembered.substring(0,remembered.length()-2));
    var before= look();
    launchScript("second");
    runInTerminal(appsShownMalformed,terminalShownMalformed);
    errorShown.go();
    changed(before,new Rectangle(760,460,400,160));
    stabilize();
    assertFalse(Files.exists(filesIOFolder.resolve("second.exit")));
    assertTrue(Files.exists(filesIOFolder.resolve(data)));
    closeDialog.go();
    errorDismissed.go();
    stabilize();
    checkContent(List.of("second.exit"),"1\n");
    assertFalse(Files.exists(filesIOFolder.resolve(data)));
    launchScript("third");
    runInTerminal(appsShownFresh,terminalShownFresh);
    freshShown.go();
    stabilize();
    checkContent(info,"{\n  \"projects\": {},\n  \"systemExtensions\": []\n}\n");
    managerMenu.go();
    quitManager.go();
    freshQuit.go();
    stabilize();
    checkContent(List.of("third.exit"),"0\n");
  }
}

package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.image.BufferedImage;
import java.util.List;

final class OpenFearlessFileTest extends ManagerTest{
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(3500))),
    on("xubuntu_xfce",()->waitUntilTime(val(4500))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(8500))),
    on("xubuntu_xfce",()->waitUntilTime(val(11000))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(26000))),
    on("xubuntu_xfce",()->waitUntilTime(val(32500))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",()->waitUntilTime(val(33000))),
    on("xubuntu_xfce",()->waitUntilTime(val(41500))));
  final Action appsShownFiles= action("appsShownFiles",
    on("ubuntu_gnome",()->waitUntilTime(val(36000))),
    on("xubuntu_xfce",()->waitUntilTime(val(45000))));
  final Action terminalShownFiles= action("terminalShownFiles",
    on("ubuntu_gnome",()->waitUntilTime(val(41000))),
    on("xubuntu_xfce",()->waitUntilTime(val(51500))));
  final Action filesShown= action("filesShown",
    on("ubuntu_gnome",()->waitUntilTime(val(58000))),
    on("xubuntu_xfce",()->waitUntilTime(val(72500))));
  final Action openFile= action("openFile",
    on("ubuntu_gnome",()->doubleClick(val(912),val(375))),
    on("xubuntu_xfce",()->doubleClick(val(1015),val(425))));
  final Action managerOpened= action("managerOpened",
    on("ubuntu_gnome",()->waitUntilTime(val(79000))),
    on("xubuntu_xfce",()->waitUntilTime(val(99000))));
  final Action managerQuitAgain= action("managerQuitAgain",
    on("ubuntu_gnome",()->waitUntilTime(val(86000))),
    on("xubuntu_xfce",()->waitUntilTime(val(107500))));
  final Action closeFiles= action("closeFiles",
    on("ubuntu_gnome",()->click(val(1414),val(303))),
    on("xubuntu_xfce",()->click(val(1275),val(309))));
  final Action filesClosed= action("filesClosed",
    on("ubuntu_gnome",()->waitUntilTime(val(90500))),
    on("xubuntu_xfce",()->waitUntilTime(val(113500))));
  @Override void walk() throws Throwable{
    noManagerData();
    var project= project("helloWorld");
    launchScript("first");
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    stabilize();
    checkContent(info,"{\n  \"projects\": {},\n  \"systemExtensions\": []\n}\n");
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
    BufferedImage bare= look();
    shell("nohup setsid -f xdg-open \""+project+"\" >/dev/null 2>&1\n");
    runInTerminal(appsShownFiles,terminalShownFiles);
    filesShown.go();
    var files= Pilot.changed(bare,pilot.shot(),6);
    openFile.go();
    managerOpened.go();
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
    managerQuitAgain.go();
    closeFiles.go();
    filesClosed.go();
    assertEquals(0L,differing(bare,look(),files));
  }
}

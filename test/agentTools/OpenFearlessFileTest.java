package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.image.BufferedImage;
import java.util.List;

final class OpenFearlessFileTest extends ManagerTest{
  final Action appsShown= action("appsShown",on("ubuntu_gnome",()->waitUntilTime(val(3500))));
  final Action terminalShown= action("terminalShown",on("ubuntu_gnome",()->waitUntilTime(val(8500))));
  final Action managerShown= action("managerShown",on("ubuntu_gnome",()->waitUntilTime(val(26000))));
  final Action managerQuit= action("managerQuit",on("ubuntu_gnome",()->waitUntilTime(val(33000))));
  final Action appsShownFiles= action("appsShownFiles",on("ubuntu_gnome",()->waitUntilTime(val(36000))));
  final Action terminalShownFiles= action("terminalShownFiles",on("ubuntu_gnome",()->waitUntilTime(val(41000))));
  final Action filesShown= action("filesShown",on("ubuntu_gnome",()->waitUntilTime(val(58000))));
  final Action openFile= action("openFile",on("ubuntu_gnome",()->doubleClick(val(912),val(375))));
  final Action managerOpened= action("managerOpened",on("ubuntu_gnome",()->waitUntilTime(val(79000))));
  final Action managerQuitAgain= action("managerQuitAgain",on("ubuntu_gnome",()->waitUntilTime(val(86000))));
  final Action closeFiles= action("closeFiles",on("ubuntu_gnome",()->click(val(1414),val(303))));
  final Action filesClosed= action("filesClosed",on("ubuntu_gnome",()->waitUntilTime(val(90500))));
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

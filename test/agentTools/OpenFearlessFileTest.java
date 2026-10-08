package agentTools;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.util.List;

final class OpenFearlessFileTest extends ManagerTest{
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(18500))));
  final Action managerMenu= action("managerMenu",
    on("ubuntu_gnome",()->click(val(98),val(79))),
    on("windows",()->click(val(31),val(33))));
  final Action quitManager= action("quitManager",
    on("ubuntu_gnome",()->click(val(116),val(212))),
    on("windows",()->click(val(61),val(167))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",()->waitUntilTime(val(21500))));
  final Action appsShownFiles= action("appsShownFiles",
    on("ubuntu_gnome",()->waitUntilTime(val(23000))));
  final Action terminalShownFiles= action("terminalShownFiles",
    on("ubuntu_gnome",()->waitUntilTime(val(26500))));
  final Action filesShown= action("filesShown",
    on("ubuntu_gnome",()->waitUntilTime(val(38500))));
  final Action openFile= action("openFile",
    on("ubuntu_gnome",()->doubleClick(val(1872),val(915))),
    on("windows",()->doubleClick(val(503),val(254))));
  final Action managerOpened= action("managerOpened",
    on("ubuntu_gnome",()->waitUntilTime(val(52000))));
  final Action appsShownToEnd= action("appsShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(53500))));
  final Action terminalShownToEnd= action("terminalShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(57000))));
  final Action managerEnded= action("managerEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(67000))));
  final Action closeFiles= action("closeFiles",
    on("ubuntu_gnome",()->click(val(2374),val(844))),
    on("windows",()->click(val(1016),val(58))));
  @Override void walk() throws Throwable{
    noManagerData();
    var project= project("helloWorld");
    launchScript("first");
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    stabilize();
    assertFalse(Files.exists(filesIOFolder.resolve(data).resolve("projects.info")));
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
    shell("xdg-open \""+project+"\"\n");
    runInTerminal(appsShownFiles,terminalShownFiles);
    filesShown.go();
    openFile.go();
    managerOpened.go();
    stabilize();
    checkContent(info,"""
      {
        "hello_world": {
          "path": "Str:%s",
          "kind": "idle"
        }
      }
      """.formatted(slashed(project)));
    endScript();
    runInTerminal(appsShownToEnd,terminalShownToEnd);
    managerEnded.go();
    closeFiles.go();
  }
}

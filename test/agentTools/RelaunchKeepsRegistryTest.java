package agentTools;

import java.awt.event.KeyEvent;
import java.util.List;

final class RelaunchKeepsRegistryTest extends ManagerTest{
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(18000))));
  final Action focusTiles= action("focusTiles",
    on("ubuntu_gnome",()->click(val(200),val(1500))),
    on("windows",()->click(val(200),val(400))));
  final Action becomeCode= action("becomeCode",
    on("ubuntu_gnome",()->click(val(400),val(139))),
    on("windows",()->click(val(332),val(94))));
  final Action codeShown= action("codeShown",
    on("ubuntu_gnome",()->waitUntilTime(val(21500))));
  final Action managerMenu= action("managerMenu",
    on("ubuntu_gnome",()->click(val(98),val(79))),
    on("windows",()->click(val(31),val(33))));
  final Action quitManager= action("quitManager",
    on("ubuntu_gnome",()->click(val(116),val(212))),
    on("windows",()->click(val(61),val(167))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",()->waitUntilTime(val(24500))));
  final Action appsShownAgain= action("appsShownAgain",
    on("ubuntu_gnome",()->waitUntilTime(val(26500))));
  final Action terminalShownAgain= action("terminalShownAgain",
    on("ubuntu_gnome",()->waitUntilTime(val(30000))));
  final Action managerShownAgain= action("managerShownAgain",
    on("ubuntu_gnome",()->waitUntilTime(val(43000))));
  final Action selectTile= action("selectTile",
    on("ubuntu_gnome",()->click(val(138),val(190))),
    on("windows",()->click(val(71),val(145))));
  final Action tileSelected= action("tileSelected",
    on("ubuntu_gnome",()->waitUntilTime(val(45000))));
  final Action focusTilesAgain= action("focusTilesAgain",
    on("ubuntu_gnome",()->click(val(200),val(1500))),
    on("windows",()->click(val(200),val(400))));
  final Action appsShownToEnd= action("appsShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(47500))));
  final Action terminalShownToEnd= action("terminalShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(51000))));
  final Action managerEnded= action("managerEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(61000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var project= project("helloWorld");
    launchScript("first",project);
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    focusTiles.go();
    keys(KeyEvent.VK_F8);
    keys(KeyEvent.VK_HOME);
    becomeCode.go();
    codeShown.go();
    var remembered= """
      {
        "hello_world": {
          "path": "Str:%s",
          "kind": "code"
        }
      }
      """.formatted(slashed(project));
    stabilize();
    checkContent(info,remembered);
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
    launchScript("second");
    runInTerminal(appsShownAgain,terminalShownAgain);
    managerShownAgain.go();
    stabilize();
    checkContent(info,remembered);
    selectTile.go();
    tileSelected.go();
    focusTilesAgain.go();
    keys(KeyEvent.VK_F8);
    keys(KeyEvent.VK_HOME);
    endScript();
    runInTerminal(appsShownToEnd,terminalShownToEnd);
    managerEnded.go();
    stabilize();
    checkContent(List.of("second.exit"),"137\n");
  }
}

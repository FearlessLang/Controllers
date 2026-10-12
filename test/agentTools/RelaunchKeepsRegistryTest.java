package agentTools;

import java.awt.Rectangle;
import java.util.List;

final class RelaunchKeepsRegistryTest extends ManagerTest{
  final Action becomeCode= header("becomeCode",1027);
  final Action appsShown= action("appsShown",on("ubuntu_gnome",()->waitUntilTime(val(3500))));
  final Action terminalShown= action("terminalShown",on("ubuntu_gnome",()->waitUntilTime(val(8500))));
  final Action managerShown= action("managerShown",on("ubuntu_gnome",()->waitUntilTime(val(26000))));
  final Action codeShown= action("codeShown",on("ubuntu_gnome",()->waitUntilTime(val(30500))));
  final Action managerQuit= action("managerQuit",on("ubuntu_gnome",()->waitUntilTime(val(36000))));
  final Action appsShownAgain= action("appsShownAgain",on("ubuntu_gnome",()->waitUntilTime(val(39500))));
  final Action terminalShownAgain= action("terminalShownAgain",on("ubuntu_gnome",()->waitUntilTime(val(44500))));
  final Action managerShownAgain= action("managerShownAgain",on("ubuntu_gnome",()->waitUntilTime(val(62000))));
  final Action tileSelected= action("tileSelected",on("ubuntu_gnome",()->waitUntilTime(val(66000))));
  final Action managerQuitAgain= action("managerQuitAgain",on("ubuntu_gnome",()->waitUntilTime(val(73000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var project= project("helloWorld");
    launchScript("first",project.resolve("hello_world.fearless"));
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    becomeCode.go();
    codeShown.go();
    var panel= new Rectangle(410,95,420,60);
    var selected= look();
    var remembered= """
      {
        "projects": {
          "hello_world": {
            "path": "Str:%s",
            "kind": "code"
          }
        },
        "systemExtensions": []
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
    changed(selected,panel);
    stabilize();
    checkContent(info,remembered);
    firstTile.go();
    tileSelected.go();
    same(selected,panel);
    managerMenu.go();
    quitManager.go();
    managerQuitAgain.go();
    stabilize();
    checkContent(List.of("second.exit"),"0\n");
  }
}

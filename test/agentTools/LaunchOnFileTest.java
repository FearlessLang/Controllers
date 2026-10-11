package agentTools;

import java.awt.Rectangle;
import java.util.List;

final class LaunchOnFileTest extends ManagerTest{
  final Action appsShown= action("appsShown",on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",on("ubuntu_gnome",()->waitUntilTime(val(18000))));
  final Action appsShownOnFolder= action("appsShownOnFolder",on("ubuntu_gnome",()->waitUntilTime(val(19500))));
  final Action terminalShownOnFolder= action("terminalShownOnFolder",on("ubuntu_gnome",()->waitUntilTime(val(23000))));
  final Action refusalShown= action("refusalShown",on("ubuntu_gnome",()->waitUntilTime(val(30000))));
  final Action managerQuit= action("managerQuit",on("ubuntu_gnome",()->waitUntilTime(val(36000))));
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
    var before= look();
    launchScript("second",project);
    runInTerminal(appsShownOnFolder,terminalShownOnFolder);
    refusalShown.go();
    var center= new Rectangle(1720,1000,400,160);
    changed(before,center);
    stabilize();
    checkContent(List.of("second.exit"),"0\n");
    closeDialog.go();
    same(before,center);
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
    checkContent(info,remembered);
  }
}

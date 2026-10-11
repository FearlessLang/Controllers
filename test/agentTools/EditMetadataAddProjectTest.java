package agentTools;

import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.List;

final class EditMetadataAddProjectTest extends ManagerTest{
  final Action appsShown= action("appsShown",on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",on("ubuntu_gnome",()->waitUntilTime(val(18000))));
  final Action editorShown= action("editorShown",on("ubuntu_gnome",()->waitUntilTime(val(22000))));
  final Action focusText= action("focusText",on("ubuntu_gnome",()->click(val(1900),val(1200))));
  final Action commit= action("commit",on("ubuntu_gnome",()->click(val(2238),val(1370))));
  final Action bothSaved= action("bothSaved",on("ubuntu_gnome",()->waitUntilTime(val(70000))));
  final Action secondTile= action("secondTile",on("ubuntu_gnome",()->click(val(261),val(130))));
  final Action secondShown= action("secondShown",on("ubuntu_gnome",()->waitUntilTime(val(72000))));
  final Action check= action("check",on("ubuntu_gnome",()->click(val(520),val(112))));
  final Action checked= action("checked",on("ubuntu_gnome",()->waitUntilTime(val(74500))));
  final Action managerQuit= action("managerQuit",on("ubuntu_gnome",()->waitUntilTime(val(79500))));
  @Override void walk() throws Throwable{
    noManagerData();
    var project= project("helloWorld");
    var other= project("helloStackTraces");
    var both= """
      {"projects": {"hello_world": {"path": "Str:%s", "kind": "idle"}, "start": {"path": "Str:%s", "kind": "idle"}}, "systemExtensions": []}
      """.formatted(slashed(project),slashed(other));
    launchScript("first",project.resolve("hello_world.fearless"));
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    managerMenu.go();
    editMetadata.go();
    editorShown.go();
    focusText.go();
    keys(KeyEvent.VK_CONTROL,KeyEvent.VK_A);
    type(both.strip());
    commit.go();
    bothSaved.go();
    stabilize();
    checkContent(info,"""
      {
        "projects": {
          "hello_world": {
            "path": "Str:%s",
            "kind": "idle"
          },
          "start": {
            "path": "Str:%s",
            "kind": "idle"
          }
        },
        "systemExtensions": []
      }
      """.formatted(slashed(project),slashed(other)));
    secondTile.go();
    secondShown.go();
    var output= new Rectangle(410,190,1000,60);
    var before= look();
    check.go();
    checked.go();
    changed(before,output);
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
  }
}

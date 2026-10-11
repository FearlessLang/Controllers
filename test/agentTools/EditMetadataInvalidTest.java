package agentTools;

import java.awt.Rectangle;
import java.util.List;

final class EditMetadataInvalidTest extends ManagerTest{
  final Action appsShown= action("appsShown",on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",on("ubuntu_gnome",()->waitUntilTime(val(18000))));
  final Action editorShown= action("editorShown",on("ubuntu_gnome",()->waitUntilTime(val(22000))));
  final Action kindIdle= action("kindIdle",on("ubuntu_gnome",()->doubleClick(val(1690),val(916))));
  final Action commit= action("commit",on("ubuntu_gnome",()->click(val(2238),val(1370))));
  final Action noteShown= action("noteShown",on("ubuntu_gnome",()->waitUntilTime(val(28000))));
  final Action close= action("close",on("ubuntu_gnome",()->click(val(2317),val(1370))));
  final Action editorClosed= action("editorClosed",on("ubuntu_gnome",()->waitUntilTime(val(31000))));
  final Action managerQuit= action("managerQuit",on("ubuntu_gnome",()->waitUntilTime(val(36000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var project= project("helloWorld");
    var idle= """
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
    managerMenu.go();
    editMetadata.go();
    editorShown.go();
    kindIdle.go();
    type("lazy");
    var editor= look();
    commit.go();
    noteShown.go();
    var center= new Rectangle(1720,1000,400,160);
    changed(editor,center);
    stabilize();
    checkContent(info,idle);
    closeDialog.go();
    same(editor,center);
    close.go();
    editorClosed.go();
    changed(editor,center);
    stabilize();
    checkContent(info,idle);
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
  }
}

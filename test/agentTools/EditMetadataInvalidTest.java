package agentTools;

import java.awt.Rectangle;
import java.util.List;

final class EditMetadataInvalidTest extends ManagerTest{
  final Action appsShown= action("appsShown",on("ubuntu_gnome",()->waitUntilTime(val(3500))));
  final Action terminalShown= action("terminalShown",on("ubuntu_gnome",()->waitUntilTime(val(8500))));
  final Action managerShown= action("managerShown",on("ubuntu_gnome",()->waitUntilTime(val(26000))));
  final Action editorShown= action("editorShown",on("ubuntu_gnome",()->waitUntilTime(val(31500))));
  final Action kindIdle= action("kindIdle",on("ubuntu_gnome",()->doubleClick(val(730),val(376))));
  final Action commit= action("commit",on("ubuntu_gnome",()->click(val(1278),val(830))));
  final Action noteShown= action("noteShown",on("ubuntu_gnome",()->waitUntilTime(val(40000))));
  final Action close= action("close",on("ubuntu_gnome",()->click(val(1357),val(830))));
  final Action editorClosed= action("editorClosed",on("ubuntu_gnome",()->waitUntilTime(val(44500))));
  final Action managerQuit= action("managerQuit",on("ubuntu_gnome",()->waitUntilTime(val(51500))));
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
    var center= new Rectangle(760,460,400,160);
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

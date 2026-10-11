package agentTools;

import java.awt.Rectangle;
import java.util.List;

final class EditMetadataConflictTest extends ManagerTest{
  static final String two= """
    {
      "projects": {
        "hello_world": {
          "path": "Str:%s",
          "kind": "%s"
        },
        "start": {
          "path": "Str:%s",
          "kind": "idle"
        }
      },
      "systemExtensions": []
    }
    """;
  final Action appsShown= action("appsShown",on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",on("ubuntu_gnome",()->waitUntilTime(val(18000))));
  final Action editorShown= action("editorShown",on("ubuntu_gnome",()->waitUntilTime(val(22000))));
  final Action kindIdle= action("kindIdle",on("ubuntu_gnome",()->doubleClick(val(1690),val(916))));
  final Action appsShownSecond= action("appsShownSecond",on("ubuntu_gnome",()->waitUntilTime(val(25500))));
  final Action terminalShownSecond= action("terminalShownSecond",on("ubuntu_gnome",()->waitUntilTime(val(29000))));
  final Action secondHanded= action("secondHanded",on("ubuntu_gnome",()->waitUntilTime(val(37000))));
  final Action commit= action("commit",on("ubuntu_gnome",()->click(val(2238),val(1370))));
  final Action noteShown= action("noteShown",on("ubuntu_gnome",()->waitUntilTime(val(39500))));
  final Action close= action("close",on("ubuntu_gnome",()->click(val(2317),val(1370))));
  final Action editorClosed= action("editorClosed",on("ubuntu_gnome",()->waitUntilTime(val(42000))));
  final Action editorShownAgain= action("editorShownAgain",on("ubuntu_gnome",()->waitUntilTime(val(45000))));
  final Action kindIdleAgain= action("kindIdleAgain",on("ubuntu_gnome",()->doubleClick(val(1690),val(916))));
  final Action commitAgain= action("commitAgain",on("ubuntu_gnome",()->click(val(2238),val(1370))));
  final Action codeSaved= action("codeSaved",on("ubuntu_gnome",()->waitUntilTime(val(51000))));
  final Action managerQuit= action("managerQuit",on("ubuntu_gnome",()->waitUntilTime(val(56000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var project= project("helloWorld");
    var other= project("helloStackTraces");
    launchScript("first",project.resolve("hello_world.fearless"));
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    managerMenu.go();
    editMetadata.go();
    editorShown.go();
    kindIdle.go();
    type("code");
    launchScript("second",other.resolve("start.fearless"));
    runInTerminal(appsShownSecond,terminalShownSecond);
    secondHanded.go();
    var both= two.formatted(slashed(project),"idle",slashed(other));
    stabilize();
    checkContent(List.of("second.exit"),"0\n");
    checkContent(info,both);
    var center= new Rectangle(1720,1000,400,160);
    var editor= look();
    commit.go();
    noteShown.go();
    changed(editor,center);
    stabilize();
    checkContent(info,both);
    closeDialog.go();
    same(editor,center);
    close.go();
    editorClosed.go();
    changed(editor,center);
    managerMenu.go();
    editMetadata.go();
    editorShownAgain.go();
    kindIdleAgain.go();
    type("code");
    commitAgain.go();
    codeSaved.go();
    stabilize();
    checkContent(info,two.formatted(slashed(project),"code",slashed(other)));
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
  }
}

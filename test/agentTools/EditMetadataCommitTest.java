package agentTools;

import java.util.List;

final class EditMetadataCommitTest extends ManagerTest{
  final Action backToIdle= header("backToIdle",780);
  static final String registry= """
    {
      "projects": {
        "hello_world": {
          "path": "Str:%s",
          "kind": "%s"
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
  final Action commit= action("commit",on("ubuntu_gnome",()->click(val(2238),val(1370))));
  final Action codeSaved= action("codeSaved",on("ubuntu_gnome",()->waitUntilTime(val(28000))));
  final Action idleSaved= action("idleSaved",on("ubuntu_gnome",()->waitUntilTime(val(31000))));
  final Action managerQuit= action("managerQuit",on("ubuntu_gnome",()->waitUntilTime(val(36000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var project= project("helloWorld");
    launchScript("first",project.resolve("hello_world.fearless"));
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    stabilize();
    checkContent(info,registry.formatted(slashed(project),"idle"));
    managerMenu.go();
    editMetadata.go();
    editorShown.go();
    var editor= look();
    kindIdle.go();
    type("code");
    commit.go();
    codeSaved.go();
    changed(editor,new java.awt.Rectangle(1600,900,600,300));
    stabilize();
    checkContent(info,registry.formatted(slashed(project),"code"));
    backToIdle.go();
    idleSaved.go();
    stabilize();
    checkContent(info,registry.formatted(slashed(project),"idle"));
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
  }
}

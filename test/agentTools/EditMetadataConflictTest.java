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
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(3500))),
    on("xubuntu_xfce",()->waitUntilTime(val(4500))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(8500))),
    on("xubuntu_xfce",()->waitUntilTime(val(11000))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(26000))),
    on("xubuntu_xfce",()->waitUntilTime(val(32500))));
  final Action editorShown= action("editorShown",
    on("ubuntu_gnome",()->waitUntilTime(val(31500))),
    on("xubuntu_xfce",()->waitUntilTime(val(39500))));
  final Action kindIdle= action("kindIdle",
    on("ubuntu_gnome",()->doubleClick(val(730),val(376))),
    on("xubuntu_xfce",()->doubleClick(val(697),val(373))));
  final Action appsShownSecond= action("appsShownSecond",
    on("ubuntu_gnome",()->waitUntilTime(val(36500))),
    on("xubuntu_xfce",()->waitUntilTime(val(46000))));
  final Action terminalShownSecond= action("terminalShownSecond",
    on("ubuntu_gnome",()->waitUntilTime(val(41500))),
    on("xubuntu_xfce",()->waitUntilTime(val(52000))));
  final Action secondHanded= action("secondHanded",
    on("ubuntu_gnome",()->waitUntilTime(val(52500))),
    on("xubuntu_xfce",()->waitUntilTime(val(66000))));
  final Action commit= action("commit",
    on("ubuntu_gnome",()->click(val(1278),val(830))),
    on("xubuntu_xfce",()->click(val(1245),val(827))));
  final Action noteShown= action("noteShown",
    on("ubuntu_gnome",()->waitUntilTime(val(56500))),
    on("xubuntu_xfce",()->waitUntilTime(val(71000))));
  final Action close= action("close",
    on("ubuntu_gnome",()->click(val(1357),val(830))),
    on("xubuntu_xfce",()->click(val(1324),val(827))));
  final Action editorClosed= action("editorClosed",
    on("ubuntu_gnome",()->waitUntilTime(val(60500))),
    on("xubuntu_xfce",()->waitUntilTime(val(76000))));
  final Action editorShownAgain= action("editorShownAgain",
    on("ubuntu_gnome",()->waitUntilTime(val(65000))),
    on("xubuntu_xfce",()->waitUntilTime(val(81500))));
  final Action kindIdleAgain= action("kindIdleAgain",
    on("ubuntu_gnome",()->doubleClick(val(730),val(376))),
    on("xubuntu_xfce",()->doubleClick(val(697),val(373))));
  final Action commitAgain= action("commitAgain",
    on("ubuntu_gnome",()->click(val(1278),val(830))),
    on("xubuntu_xfce",()->click(val(1245),val(827))));
  final Action codeSaved= action("codeSaved",
    on("ubuntu_gnome",()->waitUntilTime(val(73500))),
    on("xubuntu_xfce",()->waitUntilTime(val(92000))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",()->waitUntilTime(val(80500))),
    on("xubuntu_xfce",()->waitUntilTime(val(101000))));
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
    var center= new Rectangle(760,460,400,160);
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

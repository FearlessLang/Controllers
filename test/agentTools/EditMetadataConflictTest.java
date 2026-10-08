package agentTools;

import java.awt.event.KeyEvent;
import java.util.List;

final class EditMetadataConflictTest extends ManagerTest{
  static final String one= """
    {
      "hello_world": {
        "path": "Str:%s",
        "kind": "%s"
      }
    }
    """;
  static final String two= """
    {
      "hello_world": {
        "path": "Str:%s",
        "kind": "%s"
      },
      "start": {
        "path": "Str:%s",
        "kind": "idle"
      }
    }
    """;
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(18500))));
  final Action managerMenu= action("managerMenu",
    on("ubuntu_gnome",()->click(val(98),val(79))),
    on("windows",()->click(val(31),val(33))));
  final Action editMetadata= action("editMetadata",
    on("ubuntu_gnome",()->click(val(128),val(103))),
    on("windows",()->click(val(61),val(58))));
  final Action editorShown= action("editorShown",
    on("ubuntu_gnome",()->waitUntilTime(val(21500))));
  final Action kindIdle= action("kindIdle",
    on("ubuntu_gnome",()->doubleClick(val(1672),val(898))),
    on("windows",()->doubleClick(val(360),val(123))));
  final Action appsShownSecond= action("appsShownSecond",
    on("ubuntu_gnome",()->waitUntilTime(val(24500))));
  final Action terminalShownSecond= action("terminalShownSecond",
    on("ubuntu_gnome",()->waitUntilTime(val(28000))));
  final Action secondHanded= action("secondHanded",
    on("ubuntu_gnome",()->waitUntilTime(val(39000))));
  final Action commit= action("commit",
    on("ubuntu_gnome",()->click(val(2238),val(1370))),
    on("windows",()->click(val(924),val(619))));
  final Action noteShown= action("noteShown",
    on("ubuntu_gnome",()->waitUntilTime(val(41500))));
  final Action ok= action("ok",
    on("ubuntu_gnome",()->click(val(1953),val(1136))),
    on("windows",()->click(val(639),val(369))));
  final Action focusText= action("focusText",
    on("ubuntu_gnome",()->click(val(1900),val(1200))),
    on("windows",()->click(val(700),val(400))));
  final Action close= action("close",
    on("ubuntu_gnome",()->click(val(2317),val(1370))),
    on("windows",()->click(val(1003),val(619))));
  final Action editorClosed= action("editorClosed",
    on("ubuntu_gnome",()->waitUntilTime(val(45500))));
  final Action managerMenuAgain= action("managerMenuAgain",
    on("ubuntu_gnome",()->click(val(98),val(79))),
    on("windows",()->click(val(31),val(33))));
  final Action editMetadataAgain= action("editMetadataAgain",
    on("ubuntu_gnome",()->click(val(128),val(103))),
    on("windows",()->click(val(61),val(58))));
  final Action editorShownAgain= action("editorShownAgain",
    on("ubuntu_gnome",()->waitUntilTime(val(48500))));
  final Action focusTextAgain= action("focusTextAgain",
    on("ubuntu_gnome",()->click(val(1900),val(1200))),
    on("windows",()->click(val(700),val(400))));
  final Action kindIdleAgain= action("kindIdleAgain",
    on("ubuntu_gnome",()->doubleClick(val(1672),val(898))),
    on("windows",()->doubleClick(val(360),val(123))));
  final Action commitAgain= action("commitAgain",
    on("ubuntu_gnome",()->click(val(2238),val(1370))),
    on("windows",()->click(val(924),val(619))));
  final Action codeSaved= action("codeSaved",
    on("ubuntu_gnome",()->waitUntilTime(val(53000))));
  final Action appsShownToEnd= action("appsShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(54500))));
  final Action terminalShownToEnd= action("terminalShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(58000))));
  final Action managerEnded= action("managerEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(68000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var project= project("helloWorld");
    var other= project("helloStackTraces");
    launchScript("first",project);
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    stabilize();
    checkContent(info,one.formatted(slashed(project),"idle"));
    managerMenu.go();
    editMetadata.go();
    editorShown.go();
    kindIdle.go();
    type("code");
    launchScript("second",other);
    runInTerminal(appsShownSecond,terminalShownSecond);
    secondHanded.go();
    var both= two.formatted(slashed(project),"idle",slashed(other));
    stabilize();
    checkContent(List.of("second.exit"),"0\n");
    checkContent(info,both);
    commit.go();
    noteShown.go();
    stabilize();
    checkContent(notes,"""
      The project metadata is not committed: projects.info changed while it was edited.
      Close the editor, and choose Edit project metadata again to edit what projects.info holds now.
      """);
    checkContent(info,both);
    ok.go();
    focusText.go();
    keys(KeyEvent.VK_CONTROL,KeyEvent.VK_A);
    keys(KeyEvent.VK_CONTROL,KeyEvent.VK_C);
    close.go();
    editorClosed.go();
    managerMenuAgain.go();
    editMetadataAgain.go();
    editorShownAgain.go();
    focusTextAgain.go();
    keys(KeyEvent.VK_CONTROL,KeyEvent.VK_A);
    keys(KeyEvent.VK_CONTROL,KeyEvent.VK_C);
    kindIdleAgain.go();
    type("code");
    commitAgain.go();
    codeSaved.go();
    stabilize();
    checkContent(info,two.formatted(slashed(project),"code",slashed(other)));
    endScript();
    runInTerminal(appsShownToEnd,terminalShownToEnd);
    managerEnded.go();
    stabilize();
    checkContent(List.of("first.exit"),"137\n");
  }
}

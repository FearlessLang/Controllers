package agentTools;

import java.awt.event.KeyEvent;
import java.util.List;

final class EditMetadataCommitTest extends ManagerTest{
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
    on("ubuntu_gnome",()->waitUntilTime(val(23000))));
  final Action focusText= action("focusText",
    on("ubuntu_gnome",()->click(val(1900),val(1200))),
    on("windows",()->click(val(700),val(400))));
  final Action kindIdle= action("kindIdle",
    on("ubuntu_gnome",()->doubleClick(val(1672),val(898))),
    on("windows",()->doubleClick(val(360),val(123))));
  final Action commit= action("commit",
    on("ubuntu_gnome",()->click(val(2238),val(1370))),
    on("windows",()->click(val(924),val(619))));
  final Action codeSaved= action("codeSaved",
    on("ubuntu_gnome",()->waitUntilTime(val(27500))));
  final Action backToIdle= action("backToIdle",
    on("ubuntu_gnome",()->click(val(457),val(140))),
    on("windows",()->click(val(70),val(94))));
  final Action idleSaved= action("idleSaved",
    on("ubuntu_gnome",()->waitUntilTime(val(30000))));
  final Action appsShownToEnd= action("appsShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(31500))));
  final Action terminalShownToEnd= action("terminalShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(35000))));
  final Action managerEnded= action("managerEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(45000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var project= project("helloWorld");
    launchScript("first",project);
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    var registry= """
      {
        "hello_world": {
          "path": "Str:%s",
          "kind": "%s"
        }
      }
      """;
    stabilize();
    checkContent(info,registry.formatted(slashed(project),"idle"));
    managerMenu.go();
    editMetadata.go();
    editorShown.go();
    focusText.go();
    keys(KeyEvent.VK_CONTROL,KeyEvent.VK_A);
    keys(KeyEvent.VK_CONTROL,KeyEvent.VK_C);
    kindIdle.go();
    type("code");
    commit.go();
    codeSaved.go();
    stabilize();
    checkContent(info,registry.formatted(slashed(project),"code"));
    backToIdle.go();
    idleSaved.go();
    stabilize();
    checkContent(info,registry.formatted(slashed(project),"idle"));
    endScript();
    runInTerminal(appsShownToEnd,terminalShownToEnd);
    managerEnded.go();
    stabilize();
    checkContent(List.of("first.exit"),"137\n");
  }
}

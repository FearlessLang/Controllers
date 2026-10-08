package agentTools;

import java.awt.event.KeyEvent;
import java.util.List;

final class EditMetadataInvalidTest extends ManagerTest{
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(18500))));
  final Action focusTiles= action("focusTiles",
    on("ubuntu_gnome",()->click(val(200),val(1500))),
    on("windows",()->click(val(200),val(400))));
  final Action managerMenu= action("managerMenu",
    on("ubuntu_gnome",()->click(val(98),val(79))),
    on("windows",()->click(val(31),val(33))));
  final Action editMetadata= action("editMetadata",
    on("ubuntu_gnome",()->click(val(128),val(103))),
    on("windows",()->click(val(61),val(58))));
  final Action editorShown= action("editorShown",
    on("ubuntu_gnome",()->waitUntilTime(val(22500))));
  final Action kindIdle= action("kindIdle",
    on("ubuntu_gnome",()->doubleClick(val(1672),val(898))),
    on("windows",()->doubleClick(val(360),val(123))));
  final Action commit= action("commit",
    on("ubuntu_gnome",()->click(val(2238),val(1370))),
    on("windows",()->click(val(924),val(619))));
  final Action noteShown= action("noteShown",
    on("ubuntu_gnome",()->waitUntilTime(val(27000))));
  final Action ok= action("ok",
    on("ubuntu_gnome",()->click(val(1952),val(1183))),
    on("windows",()->click(val(639),val(414))));
  final Action focusText= action("focusText",
    on("ubuntu_gnome",()->click(val(1900),val(1200))),
    on("windows",()->click(val(700),val(400))));
  final Action close= action("close",
    on("ubuntu_gnome",()->click(val(2317),val(1370))),
    on("windows",()->click(val(1003),val(619))));
  final Action editorClosed= action("editorClosed",
    on("ubuntu_gnome",()->waitUntilTime(val(31000))));
  final Action appsShownToEnd= action("appsShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(32500))));
  final Action terminalShownToEnd= action("terminalShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(36000))));
  final Action managerEnded= action("managerEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(46000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var project= project("helloWorld");
    launchScript("first",project);
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    var idle= """
      {
        "hello_world": {
          "path": "Str:%s",
          "kind": "idle"
        }
      }
      """.formatted(slashed(project));
    stabilize();
    checkContent(info,idle);
    focusTiles.go();
    managerMenu.go();
    editMetadata.go();
    editorShown.go();
    kindIdle.go();
    type("lazy");
    commit.go();
    noteShown.go();
    var note= """
      In file: %s

      004|     "kind": "lazy"
         |             ^^^^^^

      While inspecting the file
      "kind" must be one of "idle", "code", "data:readOnly" or "data:readWrite", not "lazy".
      """.formatted(filesIOFolder.resolve(String.join("/",info)));
    stabilize();
    checkContent(notes,note);
    checkContent(info,idle);
    ok.go();
    focusText.go();
    keys(KeyEvent.VK_CONTROL,KeyEvent.VK_A);
    keys(KeyEvent.VK_CONTROL,KeyEvent.VK_C);
    close.go();
    editorClosed.go();
    stabilize();
    checkContent(info,idle);
    checkContent(notes,note);
    endScript();
    runInTerminal(appsShownToEnd,terminalShownToEnd);
    managerEnded.go();
    stabilize();
    checkContent(List.of("first.exit"),"137\n");
  }
}

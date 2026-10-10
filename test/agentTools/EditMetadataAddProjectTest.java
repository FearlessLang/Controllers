package agentTools;

import java.awt.event.KeyEvent;
import java.util.List;

final class EditMetadataAddProjectTest extends ManagerTest{
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
  final Action focusText= action("focusText",
    on("ubuntu_gnome",()->click(val(1900),val(1200))),
    on("windows",()->click(val(700),val(400))));
  final Action commit= action("commit",
    on("ubuntu_gnome",()->click(val(2238),val(1370))),
    on("windows",()->click(val(924),val(619))));
  final Action bothSaved= action("bothSaved",
    on("ubuntu_gnome",()->waitUntilTime(val(76500))));
  final Action selectSecond= action("selectSecond",
    on("ubuntu_gnome",()->click(val(265),val(190))),
    on("windows",()->click(val(199),val(145))));
  final Action check= action("check",
    on("ubuntu_gnome",()->click(val(483),val(111))),
    on("windows",()->click(val(102),val(67))));
  final Action checked= action("checked",
    on("ubuntu_gnome",()->waitUntilTime(val(92000))));
  final Action appsShownToRestart= action("appsShownToRestart",
    on("ubuntu_gnome",()->waitUntilTime(val(93500))));
  final Action terminalShownToRestart= action("terminalShownToRestart",
    on("ubuntu_gnome",()->waitUntilTime(val(97000))));
  final Action managerEndedToRestart= action("managerEndedToRestart",
    on("ubuntu_gnome",()->waitUntilTime(val(107000))));
  final Action appsShownSecond= action("appsShownSecond",
    on("ubuntu_gnome",()->waitUntilTime(val(108500))));
  final Action terminalShownSecond= action("terminalShownSecond",
    on("ubuntu_gnome",()->waitUntilTime(val(112000))));
  final Action managerShownSecond= action("managerShownSecond",
    on("ubuntu_gnome",()->waitUntilTime(val(125000))));
  final Action appsShownThird= action("appsShownThird",
    on("ubuntu_gnome",()->waitUntilTime(val(126500))));
  final Action terminalShownThird= action("terminalShownThird",
    on("ubuntu_gnome",()->waitUntilTime(val(130000))));
  final Action thirdHanded= action("thirdHanded",
    on("ubuntu_gnome",()->waitUntilTime(val(141000))));
  final Action appsShownToEnd= action("appsShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(142500))));
  final Action terminalShownToEnd= action("terminalShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(146000))));
  final Action managerEnded= action("managerEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(156000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var project= project("helloWorld");
    var other= project("helloStackTraces");
    var both= """
      {
        "hello_world": {
          "path": "Str:%s",
          "kind": "idle"
        },
        "start": {
          "path": "Str:%s",
          "kind": "idle"
        }
      }
      """.formatted(slashed(project),slashed(other));
    launchScript("first",project);
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    var closed= look();
    managerMenu.go();
    editMetadata.go();
    editorShown.go();
    var editor= changed("Edit project metadata opens the editor",closed,look());
    focusText.go();
    keys(KeyEvent.VK_CONTROL,KeyEvent.VK_A);
    type(both);
    var typed= look();
    commit.go();
    bothSaved.go();
    var committed= look();
    differ("Commit closes the editor",typed,committed,editor);
    changed("Commit adds the tile of the second project",closed,committed);
    stabilize();
    checkContent(info,both);
    selectSecond.go();
    check.go();
    checked.go();
    stabilize();
    checkContent(List.of(data,"eclipse","start","console.txt"),"--- ok: no problem found ---\n");
    checkContent(List.of(data,"eclipse","hello_world","console.txt"),"");
    endScript();
    runInTerminal(appsShownToRestart,terminalShownToRestart);
    managerEndedToRestart.go();
    stabilize();
    checkContent(List.of("first.exit"),"137\n");
    noManagerData();
    launchScript("second",project);
    runInTerminal(appsShownSecond,terminalShownSecond);
    managerShownSecond.go();
    launchScript("third",other);
    runInTerminal(appsShownThird,terminalShownThird);
    thirdHanded.go();
    stabilize();
    checkContent(List.of("third.exit"),"0\n");
    checkContent(info,both);
    endScript();
    runInTerminal(appsShownToEnd,terminalShownToEnd);
    managerEnded.go();
    stabilize();
    checkContent(List.of("second.exit"),"137\n");
  }
}

package agentTools;

import java.awt.event.KeyEvent;
import java.util.List;

final class EditMetadataForgetTest extends ManagerTest{
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(18500))));
  final Action appsShownSecond= action("appsShownSecond",
    on("ubuntu_gnome",()->waitUntilTime(val(20000))));
  final Action terminalShownSecond= action("terminalShownSecond",
    on("ubuntu_gnome",()->waitUntilTime(val(23500))));
  final Action secondShown= action("secondShown",
    on("ubuntu_gnome",()->waitUntilTime(val(34500))));
  final Action managerMenu= action("managerMenu",
    on("ubuntu_gnome",()->click(val(98),val(79))),
    on("windows",()->click(val(31),val(33))));
  final Action editMetadata= action("editMetadata",
    on("ubuntu_gnome",()->click(val(128),val(103))),
    on("windows",()->click(val(61),val(58))));
  final Action editorShown= action("editorShown",
    on("ubuntu_gnome",()->waitUntilTime(val(37500))));
  final Action focusText= action("focusText",
    on("ubuntu_gnome",()->click(val(1900),val(1200))),
    on("windows",()->click(val(700),val(400))));
  final Action commit= action("commit",
    on("ubuntu_gnome",()->click(val(2238),val(1370))),
    on("windows",()->click(val(924),val(619))));
  final Action onlySaved= action("onlySaved",
    on("ubuntu_gnome",()->waitUntilTime(val(68500))));
  final Action projectMenu= action("projectMenu",
    on("ubuntu_gnome",()->click(val(158),val(79))),
    on("windows",()->click(val(89),val(33))));
  final Action appsShownToRestart= action("appsShownToRestart",
    on("ubuntu_gnome",()->waitUntilTime(val(71000))));
  final Action terminalShownToRestart= action("terminalShownToRestart",
    on("ubuntu_gnome",()->waitUntilTime(val(74500))));
  final Action managerEndedToRestart= action("managerEndedToRestart",
    on("ubuntu_gnome",()->waitUntilTime(val(84500))));
  final Action appsShownThird= action("appsShownThird",
    on("ubuntu_gnome",()->waitUntilTime(val(86000))));
  final Action terminalShownThird= action("terminalShownThird",
    on("ubuntu_gnome",()->waitUntilTime(val(89500))));
  final Action managerShownAgain= action("managerShownAgain",
    on("ubuntu_gnome",()->waitUntilTime(val(102500))));
  final Action appsShownFourth= action("appsShownFourth",
    on("ubuntu_gnome",()->waitUntilTime(val(104000))));
  final Action terminalShownFourth= action("terminalShownFourth",
    on("ubuntu_gnome",()->waitUntilTime(val(107500))));
  final Action fourthShown= action("fourthShown",
    on("ubuntu_gnome",()->waitUntilTime(val(118500))));
  final Action projectMenuAgain= action("projectMenuAgain",
    on("ubuntu_gnome",()->click(val(158),val(79))),
    on("windows",()->click(val(89),val(33))));
  final Action forgetProject= action("forgetProject",
    on("ubuntu_gnome",()->click(val(180),val(237))),
    on("windows",()->click(val(111),val(191))));
  final Action forgotten= action("forgotten",
    on("ubuntu_gnome",()->waitUntilTime(val(121500))));
  final Action projectMenuOnceMore= action("projectMenuOnceMore",
    on("ubuntu_gnome",()->click(val(158),val(79))),
    on("windows",()->click(val(89),val(33))));
  final Action appsShownToEnd= action("appsShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(124000))));
  final Action terminalShownToEnd= action("terminalShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(127500))));
  final Action managerEnded= action("managerEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(137500))));
  @Override void walk() throws Throwable{
    noManagerData();
    var project= project("helloWorld");
    var other= project("helloStackTraces");
    var only= """
      {
        "hello_world": {
          "path": "Str:%s",
          "kind": "idle"
        }
      }
      """.formatted(slashed(project));
    launchScript("first",project);
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    launchScript("second",other);
    runInTerminal(appsShownSecond,terminalShownSecond);
    secondShown.go();
    managerMenu.go();
    editMetadata.go();
    editorShown.go();
    focusText.go();
    keys(KeyEvent.VK_CONTROL,KeyEvent.VK_A);
    type(only);
    commit.go();
    onlySaved.go();
    stabilize();
    checkContent(info,only);
    projectMenu.go();
    endScript();
    runInTerminal(appsShownToRestart,terminalShownToRestart);
    managerEndedToRestart.go();
    stabilize();
    checkContent(List.of("first.exit"),"137\n");
    noManagerData();
    launchScript("third",project);
    runInTerminal(appsShownThird,terminalShownThird);
    managerShownAgain.go();
    launchScript("fourth",other);
    runInTerminal(appsShownFourth,terminalShownFourth);
    fourthShown.go();
    projectMenuAgain.go();
    forgetProject.go();
    forgotten.go();
    stabilize();
    checkContent(info,only);
    projectMenuOnceMore.go();
    endScript();
    runInTerminal(appsShownToEnd,terminalShownToEnd);
    managerEnded.go();
    stabilize();
    checkContent(List.of("third.exit"),"137\n");
  }
}

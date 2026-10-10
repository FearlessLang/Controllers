package agentTools;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.util.List;

final class AddFolderTest extends ManagerTest{
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(18500))));
  final Action projectMenu= action("projectMenu",
    on("ubuntu_gnome",()->click(val(158),val(79))),
    on("windows",()->click(val(89),val(33))));
  final Action addFolder= action("addFolder",
    on("ubuntu_gnome",()->click(val(180),val(103))),
    on("windows",()->click(val(111),val(57))));
  final Action chooserShown= action("chooserShown",
    on("ubuntu_gnome",()->waitUntilTime(val(22000))));
  final Action cancel= action("cancel",
    on("ubuntu_gnome",()->click(val(2155),val(1252))),
    on("windows",()->click(val(841),val(487))));
  final Action chooserGone= action("chooserGone",
    on("ubuntu_gnome",()->waitUntilTime(val(23500))));
  final Action projectMenuAgain= action("projectMenuAgain",
    on("ubuntu_gnome",()->click(val(158),val(79))),
    on("windows",()->click(val(89),val(33))));
  final Action addFolderAgain= action("addFolderAgain",
    on("ubuntu_gnome",()->click(val(180),val(103))),
    on("windows",()->click(val(111),val(57))));
  final Action chooserShownAgain= action("chooserShownAgain",
    on("ubuntu_gnome",()->waitUntilTime(val(26500))));
  final Action fileName= action("fileName",
    on("ubuntu_gnome",()->click(val(2000),val(1180))),
    on("windows",()->click(val(685),val(416))));
  final Action tileShown= action("tileShown",
    on("ubuntu_gnome",()->waitUntilTime(val(39000))));
  final Action appsShownToRestart= action("appsShownToRestart",
    on("ubuntu_gnome",()->waitUntilTime(val(40500))));
  final Action terminalShownToRestart= action("terminalShownToRestart",
    on("ubuntu_gnome",()->waitUntilTime(val(44000))));
  final Action managerEndedToRestart= action("managerEndedToRestart",
    on("ubuntu_gnome",()->waitUntilTime(val(54000))));
  final Action appsShownAgain= action("appsShownAgain",
    on("ubuntu_gnome",()->waitUntilTime(val(55500))));
  final Action terminalShownAgain= action("terminalShownAgain",
    on("ubuntu_gnome",()->waitUntilTime(val(59000))));
  final Action managerShownAgain= action("managerShownAgain",
    on("ubuntu_gnome",()->waitUntilTime(val(72000))));
  final Action appsShownToEnd= action("appsShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(73500))));
  final Action terminalShownToEnd= action("terminalShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(77000))));
  final Action managerEnded= action("managerEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(87000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var project= project("helloWorld");
    launchScript("first");
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    var closed= look();
    projectMenu.go();
    addFolder.go();
    chooserShown.go();
    var chooser= changed("Add folder opens the chooser",closed,look());
    cancel.go();
    chooserGone.go();
    same("Cancel closes the chooser",closed,look(),chooser);
    stabilize();
    assertFalse(Files.exists(filesIOFolder.resolve(String.join("/",info))));
    projectMenuAgain.go();
    addFolderAgain.go();
    chooserShownAgain.go();
    differ("Add folder opens the chooser again",closed,look(),chooser);
    fileName.go();
    type(project+"\n");
    var remembered= """
      {
        "hello_world": {
          "path": "Str:%s",
          "kind": "idle"
        }
      }
      """.formatted(slashed(project));
    tileShown.go();
    stabilize();
    checkContent(info,remembered);
    endScript();
    runInTerminal(appsShownToRestart,terminalShownToRestart);
    managerEndedToRestart.go();
    stabilize();
    checkContent(List.of("first.exit"),"137\n");
    noManagerData();
    launchScript("second",project);
    runInTerminal(appsShownAgain,terminalShownAgain);
    managerShownAgain.go();
    stabilize();
    checkContent(info,remembered);
    endScript();
    runInTerminal(appsShownToEnd,terminalShownToEnd);
    managerEnded.go();
    stabilize();
    checkContent(List.of("second.exit"),"137\n");
  }
}

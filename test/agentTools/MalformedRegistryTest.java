package agentTools;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.util.List;

import tools.Fs;

final class MalformedRegistryTest extends ManagerTest{
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(18500))));
  final Action appsShownToEnd= action("appsShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(20000))));
  final Action terminalShownToEnd= action("terminalShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(23500))));
  final Action managerEnded= action("managerEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(33500))));
  final Action appsShownMalformed= action("appsShownMalformed",
    on("ubuntu_gnome",()->waitUntilTime(val(35000))));
  final Action terminalShownMalformed= action("terminalShownMalformed",
    on("ubuntu_gnome",()->waitUntilTime(val(38500))));
  final Action errorShown= action("errorShown",
    on("ubuntu_gnome",()->waitUntilTime(val(52500))));
  final Action ok= action("ok",
    on("ubuntu_gnome",()->click(val(1952),val(1279))),
    on("windows",()->click(val(639),val(506))));
  final Action errorDismissed= action("errorDismissed",
    on("ubuntu_gnome",()->waitUntilTime(val(54600))));
  final Action appsShownFresh= action("appsShownFresh",
    on("ubuntu_gnome",()->waitUntilTime(val(56100))));
  final Action terminalShownFresh= action("terminalShownFresh",
    on("ubuntu_gnome",()->waitUntilTime(val(59600))));
  final Action freshShown= action("freshShown",
    on("ubuntu_gnome",()->waitUntilTime(val(72600))));
  final Action appsShownToEndFresh= action("appsShownToEndFresh",
    on("ubuntu_gnome",()->waitUntilTime(val(74100))));
  final Action terminalShownToEndFresh= action("terminalShownToEndFresh",
    on("ubuntu_gnome",()->waitUntilTime(val(77600))));
  final Action freshEnded= action("freshEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(87600))));
  @Override void walk() throws Throwable{
    noManagerData();
    var project= project("helloWorld");
    var remembered= """
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
    stabilize();
    checkContent(info,remembered);
    endScript();
    runInTerminal(appsShownToEnd,terminalShownToEnd);
    managerEnded.go();
    stabilize();
    checkContent(List.of("first.exit"),"137\n");
    Fs.writeUtf8(filesIOFolder.resolve(data).resolve("projects.info"),remembered.substring(0,remembered.length()-2));
    launchScript("second");
    runInTerminal(appsShownMalformed,terminalShownMalformed);
    errorShown.go();
    stabilize();
    assertFalse(Files.exists(filesIOFolder.resolve("second.exit")));
    assertFalse(Files.exists(filesIOFolder.resolve(data)));
    ok.go();
    errorDismissed.go();
    stabilize();
    checkContent(List.of("second.exit"),"1\n");
    launchScript("third");
    runInTerminal(appsShownFresh,terminalShownFresh);
    freshShown.go();
    stabilize();
    checkContent(state,"{}\n");
    endScript();
    runInTerminal(appsShownToEndFresh,terminalShownToEndFresh);
    freshEnded.go();
    stabilize();
    checkContent(List.of("third.exit"),"137\n");
  }
}

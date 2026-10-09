package agentTools;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.util.List;

import tools.Fs;

final class KindResetTest extends ManagerTest{
  static final String remembered= """
    {
      "hello_world": {
        "path": "Str:%s",
        "kind": "%s"
      }
    }
    """;
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(2000))),
    on("debian_gnome_x11",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(5500))),
    on("debian_gnome_x11",()->waitUntilTime(val(6500))));
  final Action noteShown= action("noteShown",
    on("ubuntu_gnome",()->waitUntilTime(val(19000))),
    on("debian_gnome_x11",()->waitUntilTime(val(20000))));
  final Action ok= action("ok",
    on("ubuntu_gnome",()->click(val(1952),val(1132))),
    on("debian_gnome_x11",()->click(val(1920),val(1132))),
    on("windows",()->click(val(639),val(367))));
  final Action managerMenu= action("managerMenu",
    on("ubuntu_gnome",()->click(val(98),val(79))),
    on("debian_gnome_x11",()->click(val(32),val(80))),
    on("windows",()->click(val(31),val(33))));
  final Action quitManager= action("quitManager",
    on("ubuntu_gnome",()->click(val(116),val(212))),
    on("debian_gnome_x11",()->click(val(60),val(212))),
    on("windows",()->click(val(61),val(167))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",()->waitUntilTime(val(25000))),
    on("debian_gnome_x11",()->waitUntilTime(val(26000))));
  final Action appsShownAgain= action("appsShownAgain",
    on("ubuntu_gnome",()->waitUntilTime(val(26500))),
    on("debian_gnome_x11",()->waitUntilTime(val(27500))));
  final Action terminalShownAgain= action("terminalShownAgain",
    on("ubuntu_gnome",()->waitUntilTime(val(30000))),
    on("debian_gnome_x11",()->waitUntilTime(val(32000))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(43000))),
    on("debian_gnome_x11",()->waitUntilTime(val(46000))));
  final Action appsShownToEnd= action("appsShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(44500))),
    on("debian_gnome_x11",()->waitUntilTime(val(47500))));
  final Action terminalShownToEnd= action("terminalShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(48000))),
    on("debian_gnome_x11",()->waitUntilTime(val(52000))));
  final Action managerEnded= action("managerEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(58000))),
    on("debian_gnome_x11",()->waitUntilTime(val(62000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var project= project("helloWorld");
    var cache= project.resolve(".fearless_out");
    Fs.writeUtf8(filesIOFolder.resolve(data).resolve("projects.info"),remembered.formatted(slashed(project),"cooked"));
    Fs.writeUtf8(cache.resolve("_map.json"),"{}\n");
    launchScript("first");
    runInTerminal(appsShown,terminalShown);
    noteShown.go();
    stabilize();
    checkContent(notes,"In projects.info the \"kind\" of \"hello_world\" was missing or not one of the kinds: \"hello_world\" is now idle, and its compiled cache is deleted.\n");
    var idle= remembered.formatted(slashed(project),"idle");
    checkContent(info,idle);
    assertFalse(Files.exists(cache));
    ok.go();
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
    launchScript("second");
    runInTerminal(appsShownAgain,terminalShownAgain);
    managerShown.go();
    stabilize();
    checkContent(notes,"");
    checkContent(info,idle);
    endScript();
    runInTerminal(appsShownToEnd,terminalShownToEnd);
    managerEnded.go();
    stabilize();
    checkContent(List.of("second.exit"),"137\n");
  }
}

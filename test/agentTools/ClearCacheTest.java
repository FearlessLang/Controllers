package agentTools;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.util.List;

final class ClearCacheTest extends ManagerTest{
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(18500))));
  final Action focusTiles= action("focusTiles",
    on("ubuntu_gnome",()->click(val(200),val(1500))),
    on("windows",()->click(val(200),val(400))));
  final Action becomeCode= action("becomeCode",
    on("ubuntu_gnome",()->click(val(400),val(139))),
    on("windows",()->click(val(332),val(94))));
  final Action codeShown= action("codeShown",
    on("ubuntu_gnome",()->waitUntilTime(val(22000))));
  final Action compile= action("compile",
    on("ubuntu_gnome",()->click(val(164),val(111))),
    on("windows",()->click(val(102),val(67))));
  final Action mainsShown= action("mainsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(36500))));
  final Action projectMenu= action("projectMenu",
    on("ubuntu_gnome",()->click(val(158),val(79))),
    on("windows",()->click(val(89),val(33))));
  final Action clearCache= action("clearCache",
    on("ubuntu_gnome",()->click(val(180),val(128))),
    on("windows",()->click(val(111),val(82))));
  final Action cacheCleared= action("cacheCleared",
    on("ubuntu_gnome",()->waitUntilTime(val(39500))));
  final Action appsShownToEnd= action("appsShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(41000))));
  final Action terminalShownToEnd= action("terminalShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(44500))));
  final Action managerEnded= action("managerEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(54500))));
  @Override void walk() throws Throwable{
    noManagerData();
    var project= project("helloWorld");
    var console= List.of(data,"eclipse","hello_world","console.txt");
    var compiled= "--- compiling helloWorld ---\n--- compile done ---\n";
    launchScript("first",project);
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    focusTiles.go();
    keys(KeyEvent.VK_F8);
    keys(KeyEvent.VK_HOME);
    becomeCode.go();
    codeShown.go();
    compile.go();
    mainsShown.go();
    stabilize();
    checkContent(console,compiled);
    assertTrue(Files.isDirectory(project.resolve(".fearless_out")));
    checkContent(state,"""
      {
        "hello_world": {
          "folder": "Str:%s",
          "kind": "code",
          "running": "",
          "runs": "0",
          "lastRun": "",
          "exit": "-1",
          "mains": {
            "hello.Hello1": "_hello/_rank_app.fear",
            "hello.Hello3": "_hello/_rank_app.fear",
            "hello.Hello4": "_hello/_rank_app.fear",
            "hello.Hello5": "_hello/_rank_app.fear",
            "hello.Hello6": "_hello/_rank_app.fear"
          },
          "problem": {}
        }
      }
      """.formatted(escaped(project)));
    projectMenu.go();
    clearCache.go();
    cacheCleared.go();
    stabilize();
    checkContent(state,"""
      {
        "hello_world": {
          "folder": "Str:%s",
          "kind": "code",
          "running": "",
          "runs": "0",
          "lastRun": "",
          "exit": "-1",
          "mains": {},
          "problem": {}
        }
      }
      """.formatted(escaped(project)));
    assertFalse(Files.exists(project.resolve(".fearless_out")));
    checkContent(console,compiled);
    endScript();
    runInTerminal(appsShownToEnd,terminalShownToEnd);
    managerEnded.go();
    stabilize();
    checkContent(List.of("first.exit"),"137\n");
  }
}

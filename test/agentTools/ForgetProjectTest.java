package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.List;

import tools.Fs;

final class ForgetProjectTest extends ManagerTest{
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(18000))));
  final Action appsShownOther= action("appsShownOther",
    on("ubuntu_gnome",()->waitUntilTime(val(19500))));
  final Action terminalShownOther= action("terminalShownOther",
    on("ubuntu_gnome",()->waitUntilTime(val(23000))));
  final Action otherShown= action("otherShown",
    on("ubuntu_gnome",()->waitUntilTime(val(34000))));
  final Action projectMenu= action("projectMenu",
    on("ubuntu_gnome",()->click(val(158),val(79))),
    on("windows",()->click(val(89),val(33))));
  final Action forgetProject= action("forgetProject",
    on("ubuntu_gnome",()->click(val(180),val(237))),
    on("windows",()->click(val(111),val(191))));
  final Action forgotten= action("forgotten",
    on("ubuntu_gnome",()->waitUntilTime(val(37000))));
  final Action appsShownToEnd= action("appsShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(38500))));
  final Action terminalShownToEnd= action("terminalShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(42000))));
  final Action managerEnded= action("managerEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(52000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var project= project("helloWorld");
    var other= project("helloStackTraces");
    launchScript("first",project);
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    launchScript("second",other);
    runInTerminal(appsShownOther,terminalShownOther);
    otherShown.go();
    stabilize();
    var files= files(other);
    projectMenu.go();
    forgetProject.go();
    forgotten.go();
    stabilize();
    checkContent(info,"""
      {
        "hello_world": {
          "path": "Str:%s",
          "kind": "idle"
        }
      }
      """.formatted(slashed(project)));
    assertEquals(files,files(other));
    endScript();
    runInTerminal(appsShownToEnd,terminalShownToEnd);
    managerEnded.go();
    stabilize();
    checkContent(List.of("first.exit"),"137\n");
  }
  static List<String> files(Path other){ return Fs.walk(other,s->s.map(p->p+" "+Fs.lastModified(p)).toList()); }
}

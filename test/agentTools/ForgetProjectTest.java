package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.List;

import tools.Fs;

final class ForgetProjectTest extends ManagerTest{
  final Action forgetIdle= header("forgetIdle",1132);
  final Action appsShown= action("appsShown",on("ubuntu_gnome",()->waitUntilTime(val(3500))));
  final Action terminalShown= action("terminalShown",on("ubuntu_gnome",()->waitUntilTime(val(8500))));
  final Action managerShown= action("managerShown",on("ubuntu_gnome",()->waitUntilTime(val(26000))));
  final Action appsShownOther= action("appsShownOther",on("ubuntu_gnome",()->waitUntilTime(val(29000))));
  final Action terminalShownOther= action("terminalShownOther",on("ubuntu_gnome",()->waitUntilTime(val(34000))));
  final Action otherShown= action("otherShown",on("ubuntu_gnome",()->waitUntilTime(val(45000))));
  final Action forgotten= action("forgotten",on("ubuntu_gnome",()->waitUntilTime(val(49500))));
  final Action managerQuit= action("managerQuit",on("ubuntu_gnome",()->waitUntilTime(val(56500))));
  @Override void walk() throws Throwable{
    noManagerData();
    var project= project("helloWorld");
    var other= project("helloStackTraces");
    launchScript("first",project.resolve("hello_world.fearless"));
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    launchScript("second",other.resolve("start.fearless"));
    runInTerminal(appsShownOther,terminalShownOther);
    otherShown.go();
    stabilize();
    var files= files(other);
    forgetIdle.go();
    forgotten.go();
    stabilize();
    checkContent(info,"""
      {
        "projects": {
          "hello_world": {
            "path": "Str:%s",
            "kind": "idle"
          }
        },
        "systemExtensions": []
      }
      """.formatted(slashed(project)));
    assertEquals(files,files(other));
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
  }
  static List<String> files(Path other){ return Fs.walk(other,s->s.map(p->p+" "+Fs.lastModified(p)).toList()); }
}

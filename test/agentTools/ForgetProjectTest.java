package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.List;

import tools.Fs;

final class ForgetProjectTest extends ManagerTest{
  final Action forgetIdle= action("forgetIdle",
    on("ubuntu_gnome",()->click(val(1132),val(113))),
    on("xubuntu_xfce",()->click(val(1158),val(93))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitAfter(val(17500))),
    on("xubuntu_xfce",()->waitAfter(val(21500))));
  final Action otherShown= action("otherShown",
    on("ubuntu_gnome",()->waitAfter(val(11000))),
    on("xubuntu_xfce",()->waitAfter(val(14000))));
  final Action forgotten= action("forgotten",
    on("ubuntu_gnome",()->waitAfter(val(4500))),
    on("xubuntu_xfce",()->waitAfter(val(5500))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",()->waitAfter(val(7000))),
    on("xubuntu_xfce",()->waitAfter(val(9000))));
  final Action openHelloWorld= action("openHelloWorld",
    on("ubuntu_gnome",()->waitDoubleClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",()->waitDoubleClick(val(null),val(null),val(null))));
  final Action openHelloWorldMarker= action("openHelloWorldMarker",
    on("ubuntu_gnome",()->waitDoubleClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",()->waitDoubleClick(val(null),val(null),val(null))));
  final Action openHelloStackTraces= action("openHelloStackTraces",
    on("ubuntu_gnome",()->waitDoubleClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",()->waitDoubleClick(val(null),val(null),val(null))));
  final Action openStartMarker= action("openStartMarker",
    on("ubuntu_gnome",()->waitDoubleClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",()->waitDoubleClick(val(null),val(null),val(null))));
  @Override void walk() throws Throwable{
    var project= project("helloWorld");
    var other= project("helloStackTraces");
    start();
    managerShown.go();
    toFiles();
    openHelloWorld.go();
    openHelloWorldMarker.go();
    closeFiles();
    toFiles();
    openHelloStackTraces.go();
    openStartMarker.go();
    closeFiles();
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
  }
  static List<String> files(Path other){ return Fs.walk(other,s->s.map(p->p+" "+Fs.lastModified(p)).toList()); }
}

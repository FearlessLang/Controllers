package agentTools;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.util.List;

final class ClearCacheTest extends ManagerTest{
  final Action becomeCode= action("becomeCode",
    on("ubuntu_gnome",()->click(val(1027),val(113))),
    on("xubuntu_xfce",()->click(val(1040),val(93))));
  final Action clean= action("clean",
    on("ubuntu_gnome",()->click(val(1010),val(113))),
    on("xubuntu_xfce",()->click(val(1007),val(93))));
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitAfter(val(3500))),
    on("xubuntu_xfce",()->waitAfter(val(4500))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitAfter(val(5000))),
    on("xubuntu_xfce",()->waitAfter(val(6500))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitAfter(val(17500))),
    on("xubuntu_xfce",()->waitAfter(val(21500))));
  final Action codeShown= action("codeShown",
    on("ubuntu_gnome",()->waitAfter(val(4500))),
    on("xubuntu_xfce",()->waitAfter(val(6000))));
  final Action compile= action("compile",
    on("ubuntu_gnome",()->click(val(580),val(113))),
    on("xubuntu_xfce",()->click(val(545),val(93))));
  final Action compiled= action("compiled",
    on("ubuntu_gnome",()->waitAfter(val(21000))),
    on("xubuntu_xfce",()->waitAfter(val(26000))));
  final Action cacheCleared= action("cacheCleared",
    on("ubuntu_gnome",()->waitAfter(val(4500))),
    on("xubuntu_xfce",()->waitAfter(val(5500))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",()->waitAfter(val(7000))),
    on("xubuntu_xfce",()->waitAfter(val(9000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var project= project("helloWorld");
    launchScript("first",project.resolve("hello_world.fearless"));
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    becomeCode.go();
    codeShown.go();
    var notCompiled= look();
    compile.go();
    compiled.go();
    var button= inWindow(565,102,30,20);
    changed(notCompiled,button);
    stabilize();
    assertTrue(Files.isDirectory(project.resolve(".fearless_out")));
    clean.go();
    cacheCleared.go();
    same(notCompiled,button);
    stabilize();
    assertFalse(Files.exists(project.resolve(".fearless_out")));
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
  }
}

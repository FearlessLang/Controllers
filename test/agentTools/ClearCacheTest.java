package agentTools;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Rectangle;
import java.nio.file.Files;
import java.util.List;

final class ClearCacheTest extends ManagerTest{
  final Action becomeCode= header("becomeCode",1027);
  final Action clean= header("clean",1010);
  final Action appsShown= action("appsShown",on("ubuntu_gnome",()->waitUntilTime(val(3500))));
  final Action terminalShown= action("terminalShown",on("ubuntu_gnome",()->waitUntilTime(val(8500))));
  final Action managerShown= action("managerShown",on("ubuntu_gnome",()->waitUntilTime(val(26000))));
  final Action codeShown= action("codeShown",on("ubuntu_gnome",()->waitUntilTime(val(30500))));
  final Action compile= action("compile",on("ubuntu_gnome",()->click(val(580),val(113))));
  final Action compiled= action("compiled",on("ubuntu_gnome",()->waitUntilTime(val(51500))));
  final Action cacheCleared= action("cacheCleared",on("ubuntu_gnome",()->waitUntilTime(val(56000))));
  final Action managerQuit= action("managerQuit",on("ubuntu_gnome",()->waitUntilTime(val(63000))));
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
    var button= new Rectangle(565,102,30,20);
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

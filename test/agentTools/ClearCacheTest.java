package agentTools;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;

final class ClearCacheTest extends ManagerTest{
  final Action becomeCode= action("becomeCode",
    on("ubuntu_gnome", () -> waitClick(val(0), val(1027), val(112))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(1040), val(93))));
  final Action clean= action("clean",
    on("ubuntu_gnome", () -> waitClick(val(0), val(1006), val(112))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(1007), val(93))));
  final Action openHelloWorld= action("openHelloWorld",
    on("ubuntu_gnome", () -> waitDoubleClick(val(100), val(798), val(380))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action openHelloWorldMarker= action("openHelloWorldMarker",
    on("ubuntu_gnome", () -> waitDoubleClick(val(100), val(911), val(380))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action projectShown= action("projectShown",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("xubuntu_xfce", () -> waitAfter(val(null))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("xubuntu_xfce", () -> waitAfter(val(21500))));
  final Action codeShown= action("codeShown",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("xubuntu_xfce", () -> waitAfter(val(6000))));
  final Action compile= action("compile",
    on("ubuntu_gnome", () -> waitClick(val(0), val(595), val(112))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(545), val(93))));
  final Action compiled= action("compiled",
    on("ubuntu_gnome", () -> waitAfter(val(2000))),//the compile of hello_world runs in its own JVM
    on("xubuntu_xfce", () -> waitAfter(val(26000))));
  final Action cacheCleared= action("cacheCleared",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("xubuntu_xfce", () -> waitAfter(val(5500))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("xubuntu_xfce", () -> waitAfter(val(9000))));
  @Override void walk() throws Throwable{
    var project= project("helloWorld");
    var bare= look();
    start();
    managerShown.go();
    toFiles();
    openHelloWorld.go();
    openHelloWorldMarker.go();
    projectShown.go();
    becomeCode.go();
    codeShown.go();
    var notCompiled= look();
    compile.go();
    compiled.go();
    var button= inWindow(565, 102, 30, 20);
    changed(notCompiled, button);
    stabilize();
    assertTrue(Files.isDirectory(project.resolve(".fearless_out")));
    clean.go();
    cacheCleared.go();
    same(notCompiled, button);
    stabilize();
    assertFalse(Files.exists(project.resolve(".fearless_out")));
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    closeFiles();
    same(bare, button);
  }
}

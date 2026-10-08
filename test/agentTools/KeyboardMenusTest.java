package agentTools;

import java.awt.event.KeyEvent;
import java.util.List;

final class KeyboardMenusTest extends ManagerTest{
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(18000))));
  final Action appsShownHanded= action("appsShownHanded",
    on("ubuntu_gnome",()->waitUntilTime(val(19500))));
  final Action terminalShownHanded= action("terminalShownHanded",
    on("ubuntu_gnome",()->waitUntilTime(val(23000))));
  final Action handed= action("handed",
    on("ubuntu_gnome",()->waitUntilTime(val(34000))));
  final Action focusTiles= action("focusTiles",
    on("ubuntu_gnome",()->click(val(200),val(1500))),
    on("windows",()->click(val(200),val(400))));
  final Action projectMenu= action("projectMenu",
    on("ubuntu_gnome",()->click(val(158),val(79))),
    on("windows",()->click(val(89),val(33))));
  final Action menuOpened= action("menuOpened",
    on("ubuntu_gnome",()->waitUntilTime(val(35700))));
  final Action itemHighlighted= action("itemHighlighted",
    on("ubuntu_gnome",()->waitUntilTime(val(36200))));
  final Action menuClosed= action("menuClosed",
    on("ubuntu_gnome",()->waitUntilTime(val(36700))));
  final Action menuReopened= action("menuReopened",
    on("ubuntu_gnome",()->waitUntilTime(val(37400))));
  final Action forgotten= action("forgotten",
    on("ubuntu_gnome",()->waitUntilTime(val(39500))));
  final Action dialogShown= action("dialogShown",
    on("ubuntu_gnome",()->waitUntilTime(val(42000))));
  final Action dialogClosed= action("dialogClosed",
    on("ubuntu_gnome",()->waitUntilTime(val(43500))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",()->waitUntilTime(val(48000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var project= project("helloWorld");
    launchScript("first");
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    launchScript("second",project);
    runInTerminal(appsShownHanded,terminalShownHanded);
    handed.go();
    stabilize();
    checkContent(List.of("second.exit"),"0\n");
    checkContent(info,"""
      {
        "hello_world": {
          "path": "Str:%s",
          "kind": "idle"
        }
      }
      """.formatted(slashed(project)));
    focusTiles.go();
    projectMenu.go();
    menuOpened.go();
    keys(KeyEvent.VK_DOWN);
    itemHighlighted.go();
    keys(KeyEvent.VK_ESCAPE);
    menuClosed.go();
    keys(KeyEvent.VK_ALT,KeyEvent.VK_P);
    menuReopened.go();
    keys(KeyEvent.VK_UP);
    keys(KeyEvent.VK_ENTER);
    forgotten.go();
    stabilize();
    checkContent(info,"{}\n");
    keys(KeyEvent.VK_ALT,KeyEvent.VK_M);
    keys(KeyEvent.VK_DOWN);
    keys(KeyEvent.VK_ENTER);
    dialogShown.go();
    keys(KeyEvent.VK_SHIFT,KeyEvent.VK_TAB);
    keys(KeyEvent.VK_CONTROL,KeyEvent.VK_A);
    keys(KeyEvent.VK_CONTROL,KeyEvent.VK_C);
    keys(KeyEvent.VK_ESCAPE);
    dialogClosed.go();
    keys(KeyEvent.VK_ALT,KeyEvent.VK_M);
    keys(KeyEvent.VK_UP);
    keys(KeyEvent.VK_ENTER);
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
  }
}

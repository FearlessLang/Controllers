package agentTools;

import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.List;

final class KeyboardMenusTest extends ManagerTest{
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(3500))),
    on("xubuntu_xfce",()->waitUntilTime(val(4500))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(8500))),
    on("xubuntu_xfce",()->waitUntilTime(val(11000))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(26000))),
    on("xubuntu_xfce",()->waitUntilTime(val(32500))));
  final Action menuOpened= action("menuOpened",
    on("ubuntu_gnome",()->waitUntilTime(val(29000))),
    on("xubuntu_xfce",()->waitUntilTime(val(36500))));
  final Action menuClosed= action("menuClosed",
    on("ubuntu_gnome",()->waitUntilTime(val(31500))),
    on("xubuntu_xfce",()->waitUntilTime(val(39500))));
  final Action editorShown= action("editorShown",
    on("ubuntu_gnome",()->waitUntilTime(val(35500))),
    on("xubuntu_xfce",()->waitUntilTime(val(44500))));
  final Action editorClosed= action("editorClosed",
    on("ubuntu_gnome",()->waitUntilTime(val(39000))),
    on("xubuntu_xfce",()->waitUntilTime(val(49000))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",()->waitUntilTime(val(46000))),
    on("xubuntu_xfce",()->waitUntilTime(val(57500))));
  @Override void walk() throws Throwable{
    noManagerData();
    var project= project("helloWorld");
    launchScript("first",project.resolve("hello_world.fearless"));
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    var menu= inWindow(70,95,150,120);
    var center= new Rectangle(760,460,400,160);
    var closed= look();
    keys(KeyEvent.VK_ALT,KeyEvent.VK_M);
    menuOpened.go();
    changed(closed,menu);
    keys(KeyEvent.VK_ESCAPE);
    menuClosed.go();
    same(closed,menu);
    keys(KeyEvent.VK_ALT,KeyEvent.VK_M);
    keys(KeyEvent.VK_DOWN);
    keys(KeyEvent.VK_ENTER);
    editorShown.go();
    changed(closed,center);
    keys(KeyEvent.VK_ALT,KeyEvent.VK_F4);
    editorClosed.go();
    same(closed,center);
    keys(KeyEvent.VK_ALT,KeyEvent.VK_M);
    keys(KeyEvent.VK_UP);
    keys(KeyEvent.VK_ENTER);
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
  }
}

package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.KeyEvent;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import tools.Fs;

final class FileAssociationTest extends ManagerTest{
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(3500))),
    on("xubuntu_xfce",()->waitUntilTime(val(4500))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(8500))),
    on("xubuntu_xfce",()->waitUntilTime(val(11000))));
  final Action iconArea= action("iconArea",
    on("ubuntu_gnome",()->region(val(880),val(340),val(64),val(64))),
    on("xubuntu_xfce",()->region(val(985),val(396),val(60),val(60))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(26000))),
    on("xubuntu_xfce",()->waitUntilTime(val(32500))));
  final Action appsShownFiles= action("appsShownFiles",
    on("ubuntu_gnome",()->waitUntilTime(val(29000))),
    on("xubuntu_xfce",()->waitUntilTime(val(36500))));
  final Action terminalShownFiles= action("terminalShownFiles",
    on("ubuntu_gnome",()->waitUntilTime(val(34000))),
    on("xubuntu_xfce",()->waitUntilTime(val(42500))));
  final Action filesShown= action("filesShown",
    on("ubuntu_gnome",()->waitUntilTime(val(49500))),
    on("xubuntu_xfce",()->waitUntilTime(val(62000))));
  final Action focusFiles= action("focusFiles",
    on("ubuntu_gnome",()->click(val(1240),val(660))),
    on("xubuntu_xfce",()->click(val(1200),val(700))));
  final Action associatedShown= action("associatedShown",
    on("ubuntu_gnome",()->waitUntilTime(val(53000))),
    on("xubuntu_xfce",()->waitUntilTime(val(66500))));
  final Action managerMenuBehind= action("managerMenuBehind",
    on("ubuntu_gnome",()->click(val(99),val(79))),
    on("xubuntu_xfce",()->click(val(38),val(62))));
  final Action resetAsked= action("resetAsked",
    on("ubuntu_gnome",()->waitUntilTime(val(58000))),
    on("xubuntu_xfce",()->waitUntilTime(val(72500))));
  final Action cancelReset= action("cancelReset",
    on("ubuntu_gnome",()->click(val(1026),val(596))),
    on("xubuntu_xfce",()->click(val(993),val(593))));
  final Action resetCancelled= action("resetCancelled",
    on("ubuntu_gnome",()->waitUntilTime(val(61500))),
    on("xubuntu_xfce",()->waitUntilTime(val(77000))));
  final Action resetAskedAgain= action("resetAskedAgain",
    on("ubuntu_gnome",()->waitUntilTime(val(65500))),
    on("xubuntu_xfce",()->waitUntilTime(val(82000))));
  final Action confirmReset= action("confirmReset",
    on("ubuntu_gnome",()->click(val(958),val(596))),
    on("xubuntu_xfce",()->click(val(925),val(593))));
  final Action managerReset= action("managerReset",
    on("ubuntu_gnome",()->waitUntilTime(val(70000))),
    on("xubuntu_xfce",()->waitUntilTime(val(87500))));
  final Action genericShown= action("genericShown",
    on("ubuntu_gnome",()->waitUntilTime(val(77000))),
    on("xubuntu_xfce",()->waitUntilTime(val(96500))));
  final Action appsShownAgain= action("appsShownAgain",
    on("ubuntu_gnome",()->waitUntilTime(val(80000))),
    on("xubuntu_xfce",()->waitUntilTime(val(100000))));
  final Action terminalShownAgain= action("terminalShownAgain",
    on("ubuntu_gnome",()->waitUntilTime(val(85000))),
    on("xubuntu_xfce",()->waitUntilTime(val(106500))));
  final Action managerShownAgain= action("managerShownAgain",
    on("ubuntu_gnome",()->waitUntilTime(val(103000))),
    on("xubuntu_xfce",()->waitUntilTime(val(129000))));
  final Action associatedAgain= action("associatedAgain",
    on("ubuntu_gnome",()->waitUntilTime(val(120000))),
    on("xubuntu_xfce",()->waitUntilTime(val(150000))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",()->waitUntilTime(val(127000))),
    on("xubuntu_xfce",()->waitUntilTime(val(159000))));
  final Action closeFiles= action("closeFiles",
    on("ubuntu_gnome",()->click(val(1414),val(303))),
    on("xubuntu_xfce",()->click(val(1275),val(309))));
  @Override void walk() throws Throwable{
    noManagerData();
    var project= project("helloWorld");
    iconArea.go();
    var icon= region;
    launchScript("first");
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    shell("nohup setsid -f xdg-open \""+project+"\" >/dev/null 2>&1\n");
    runInTerminal(appsShownFiles,terminalShownFiles);
    filesShown.go();
    reload(associatedShown);
    var associated= look();
    managerMenuBehind.go();
    resetManager.go();
    resetAsked.go();
    cancelReset.go();
    resetCancelled.go();
    stabilize();
    assertTrue(Files.exists(filesIOFolder.resolve(data).resolve("projects.info")));
    assertFalse(Files.exists(filesIOFolder.resolve("first.exit")));
    managerMenu.go();
    resetManager.go();
    resetAskedAgain.go();
    confirmReset.go();
    managerReset.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
    assertFalse(Files.exists(filesIOFolder.resolve(data)));
    reload(genericShown);
    changed(associated,icon);
    launchScript("second");
    runInTerminal(appsShownAgain,terminalShownAgain);
    managerShownAgain.go();
    associatedAgain.go();
    checkDefault();
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("second.exit"),"0\n");
    closeFiles.go();
  }
  void checkDefault() throws Throwable{
    var expected= app.getFileName()+".desktop";
    if (!desk.equals("ubuntu_gnome")){ inGuest("su tester -c 'xdg-mime query default application/x-fearless' | grep -qx "+expected); return; }
    var query= Fs.of(()->new ProcessBuilder("xdg-mime","query","default","application/x-fearless").start());
    assertEquals(expected+"\n",new String(query.getInputStream().readAllBytes(),StandardCharsets.UTF_8));
  }
  void reload(Action reloaded) throws Throwable{
    focusFiles.go();
    keys(KeyEvent.VK_F5);
    reloaded.go();
  }
}

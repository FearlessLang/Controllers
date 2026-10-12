package agentTools;

import java.util.List;

final class ShowProjectTest extends ManagerTest{
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(3500))),
    on("xubuntu_xfce",()->waitUntilTime(val(4500))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(8500))),
    on("xubuntu_xfce",()->waitUntilTime(val(11000))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(26000))),
    on("xubuntu_xfce",()->waitUntilTime(val(32500))));
  final Action appsShownSecond= action("appsShownSecond",
    on("ubuntu_gnome",()->waitUntilTime(val(29000))),
    on("xubuntu_xfce",()->waitUntilTime(val(36500))));
  final Action terminalShownSecond= action("terminalShownSecond",
    on("ubuntu_gnome",()->waitUntilTime(val(34000))),
    on("xubuntu_xfce",()->waitUntilTime(val(42500))));
  final Action secondShown= action("secondShown",
    on("ubuntu_gnome",()->waitUntilTime(val(45000))),
    on("xubuntu_xfce",()->waitUntilTime(val(56500))));
  final Action appsShownThird= action("appsShownThird",
    on("ubuntu_gnome",()->waitUntilTime(val(48000))),
    on("xubuntu_xfce",()->waitUntilTime(val(60000))));
  final Action terminalShownThird= action("terminalShownThird",
    on("ubuntu_gnome",()->waitUntilTime(val(53000))),
    on("xubuntu_xfce",()->waitUntilTime(val(66500))));
  final Action thirdHanded= action("thirdHanded",
    on("ubuntu_gnome",()->waitUntilTime(val(64000))),
    on("xubuntu_xfce",()->waitUntilTime(val(80000))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",()->waitUntilTime(val(71000))),
    on("xubuntu_xfce",()->waitUntilTime(val(89000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var project= project("helloWorld");
    var other= project("helloStackTraces");
    launchScript("first",project.resolve("hello_world.fearless"));
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    var name= inWindow(450,95,110,32);
    var tiles= inWindow(69,95,256,85);
    var hello= look();
    launchScript("second",other.resolve("start.fearless"));
    runInTerminal(appsShownSecond,terminalShownSecond);
    secondShown.go();
    changed(hello,name);
    var start= look();
    launchScript("third","FearlessCommandShow","hello_world");
    runInTerminal(appsShownThird,terminalShownThird);
    thirdHanded.go();
    same(hello,name);
    changed(start,tiles);
    stabilize();
    checkContent(List.of("second.exit"),"0\n");
    checkContent(List.of("third.exit"),"0\n");
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
  }
}

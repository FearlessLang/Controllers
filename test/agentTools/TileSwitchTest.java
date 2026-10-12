package agentTools;

import java.util.List;

final class TileSwitchTest extends ManagerTest{
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(3500))),
    on("xubuntu_xfce",()->waitUntilTime(val(4500))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(8500))),
    on("xubuntu_xfce",()->waitUntilTime(val(11000))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(26000))),
    on("xubuntu_xfce",()->waitUntilTime(val(32500))));
  final Action checkHello= action("checkHello",
    on("ubuntu_gnome",()->click(val(580),val(113))),
    on("xubuntu_xfce",()->click(val(538),val(93))));
  final Action helloChecked= action("helloChecked",
    on("ubuntu_gnome",()->waitUntilTime(val(30500))),
    on("xubuntu_xfce",()->waitUntilTime(val(38500))));
  final Action appsShownSecond= action("appsShownSecond",
    on("ubuntu_gnome",()->waitUntilTime(val(33500))),
    on("xubuntu_xfce",()->waitUntilTime(val(42000))));
  final Action terminalShownSecond= action("terminalShownSecond",
    on("ubuntu_gnome",()->waitUntilTime(val(38500))),
    on("xubuntu_xfce",()->waitUntilTime(val(48500))));
  final Action secondShown= action("secondShown",
    on("ubuntu_gnome",()->waitUntilTime(val(49500))),
    on("xubuntu_xfce",()->waitUntilTime(val(62000))));
  final Action helloTile= action("helloTile",
    on("ubuntu_gnome",()->click(val(133),val(130))),
    on("xubuntu_xfce",()->click(val(66),val(110))));
  final Action helloShown= action("helloShown",
    on("ubuntu_gnome",()->waitUntilTime(val(53500))),
    on("xubuntu_xfce",()->waitUntilTime(val(67000))));
  final Action startTile= action("startTile",
    on("ubuntu_gnome",()->click(val(261),val(130))),
    on("xubuntu_xfce",()->click(val(194),val(110))));
  final Action startShown= action("startShown",
    on("ubuntu_gnome",()->waitUntilTime(val(57500))),
    on("xubuntu_xfce",()->waitUntilTime(val(72000))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",()->waitUntilTime(val(64500))),
    on("xubuntu_xfce",()->waitUntilTime(val(81000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var project= project("helloWorld");
    var other= project("helloStackTraces");
    launchScript("first",project.resolve("hello_world.fearless"));
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    var panel= inWindow(410,95,1000,155);
    var name= inWindow(450,95,110,32);
    var output= inWindow(410,168,1000,80);
    var helloEmpty= look();
    checkHello.go();
    helloChecked.go();
    changed(helloEmpty,panel);
    var hello= look();
    launchScript("second",other.resolve("start.fearless"));
    runInTerminal(appsShownSecond,terminalShownSecond);
    secondShown.go();
    changed(hello,panel);
    var start= look();
    stabilize();
    checkContent(List.of("second.exit"),"0\n");
    helloTile.go();
    helloShown.go();
    same(hello,name);
    same(hello,output);
    startTile.go();
    startShown.go();
    same(start,name);
    same(start,output);
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
  }
}

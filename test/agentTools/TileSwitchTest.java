package agentTools;

import java.awt.Rectangle;
import java.util.List;

final class TileSwitchTest extends ManagerTest{
  final Action appsShown= action("appsShown",on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",on("ubuntu_gnome",()->waitUntilTime(val(18000))));
  final Action checkHello= action("checkHello",on("ubuntu_gnome",()->click(val(580),val(112))));
  final Action helloChecked= action("helloChecked",on("ubuntu_gnome",()->waitUntilTime(val(21000))));
  final Action appsShownSecond= action("appsShownSecond",on("ubuntu_gnome",()->waitUntilTime(val(22500))));
  final Action terminalShownSecond= action("terminalShownSecond",on("ubuntu_gnome",()->waitUntilTime(val(26000))));
  final Action secondShown= action("secondShown",on("ubuntu_gnome",()->waitUntilTime(val(34000))));
  final Action helloTile= action("helloTile",on("ubuntu_gnome",()->click(val(133),val(130))));
  final Action helloShown= action("helloShown",on("ubuntu_gnome",()->waitUntilTime(val(36500))));
  final Action startTile= action("startTile",on("ubuntu_gnome",()->click(val(261),val(130))));
  final Action startShown= action("startShown",on("ubuntu_gnome",()->waitUntilTime(val(39000))));
  final Action managerQuit= action("managerQuit",on("ubuntu_gnome",()->waitUntilTime(val(44000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var project= project("helloWorld");
    var other= project("helloStackTraces");
    launchScript("first",project.resolve("hello_world.fearless"));
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    var panel= new Rectangle(410,95,1000,155);
    var name= new Rectangle(450,95,110,32);
    var output= new Rectangle(410,190,1000,60);
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

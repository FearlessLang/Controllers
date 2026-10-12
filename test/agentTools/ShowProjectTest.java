package agentTools;

import java.awt.Rectangle;
import java.util.List;

final class ShowProjectTest extends ManagerTest{
  final Action appsShown= action("appsShown",on("ubuntu_gnome",()->waitUntilTime(val(3500))));
  final Action terminalShown= action("terminalShown",on("ubuntu_gnome",()->waitUntilTime(val(8500))));
  final Action managerShown= action("managerShown",on("ubuntu_gnome",()->waitUntilTime(val(26000))));
  final Action appsShownSecond= action("appsShownSecond",on("ubuntu_gnome",()->waitUntilTime(val(29000))));
  final Action terminalShownSecond= action("terminalShownSecond",on("ubuntu_gnome",()->waitUntilTime(val(34000))));
  final Action secondShown= action("secondShown",on("ubuntu_gnome",()->waitUntilTime(val(45000))));
  final Action appsShownThird= action("appsShownThird",on("ubuntu_gnome",()->waitUntilTime(val(48000))));
  final Action terminalShownThird= action("terminalShownThird",on("ubuntu_gnome",()->waitUntilTime(val(53000))));
  final Action thirdHanded= action("thirdHanded",on("ubuntu_gnome",()->waitUntilTime(val(64000))));
  final Action managerQuit= action("managerQuit",on("ubuntu_gnome",()->waitUntilTime(val(71000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var project= project("helloWorld");
    var other= project("helloStackTraces");
    launchScript("first",project.resolve("hello_world.fearless"));
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    var name= new Rectangle(450,95,110,32);
    var tiles= new Rectangle(69,95,256,85);
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

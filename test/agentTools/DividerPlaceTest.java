package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.util.List;

final class DividerPlaceTest extends ManagerTest{
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(18000))));
  final Action tilesEnd= action("tilesEnd",
    on("ubuntu_gnome",()->listEnd(val(200),val(1500),val(380))));
  final Action closeManager= action("closeManager",
    on("ubuntu_gnome",()->click(val(3822),val(50))));
  final Action managerHidden= action("managerHidden",
    on("ubuntu_gnome",()->waitUntilTime(val(20000))));
  final Action tilesGone= action("tilesGone",
    on("ubuntu_gnome",()->listEnd(val(200),val(1500),val(200))));
  final Action appsShownSecond= action("appsShownSecond",
    on("ubuntu_gnome",()->waitUntilTime(val(21500))));
  final Action terminalShownSecond= action("terminalShownSecond",
    on("ubuntu_gnome",()->waitUntilTime(val(25000))));
  final Action secondEnded= action("secondEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(36000))));
  final Action managerBack= action("managerBack",
    on("ubuntu_gnome",()->waitUntilTime(val(37000))));
  final Action managerMenu= action("managerMenu",
    on("ubuntu_gnome",()->click(val(98),val(79))));
  final Action quitManager= action("quitManager",
    on("ubuntu_gnome",()->click(val(116),val(212))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",()->waitUntilTime(val(41500))));
  final Action appsShownThird= action("appsShownThird",
    on("ubuntu_gnome",()->waitUntilTime(val(43000))));
  final Action terminalShownThird= action("terminalShownThird",
    on("ubuntu_gnome",()->waitUntilTime(val(46500))));
  final Action thirdShown= action("thirdShown",
    on("ubuntu_gnome",()->waitUntilTime(val(59000))));
  final Action thirdQuit= action("thirdQuit",
    on("ubuntu_gnome",()->waitUntilTime(val(63500))));
  final Action appsShownFourth= action("appsShownFourth",
    on("ubuntu_gnome",()->waitUntilTime(val(65000))));
  final Action terminalShownFourth= action("terminalShownFourth",
    on("ubuntu_gnome",()->waitUntilTime(val(68500))));
  final Action fourthShown= action("fourthShown",
    on("ubuntu_gnome",()->waitUntilTime(val(81000))));
  final Action fourthQuit= action("fourthQuit",
    on("ubuntu_gnome",()->waitUntilTime(val(85500))));
  @Override void walk() throws Throwable{
    noManagerData();
    var project= project("helloWorld");
    launchScript("first",project);
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    tilesEnd.go();
    closeManager.go();
    managerHidden.go();
    tilesGone.go();
    stabilize();
    assertFalse(Files.exists(filesIOFolder.resolve("first.exit")));
    launchScript("second");
    runInTerminal(appsShownSecond,terminalShownSecond);
    secondEnded.go();
    stabilize();
    checkContent(List.of("second.exit"),"0\n");
    managerBack.go();
    tilesEnd.go();
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
    launchScript("third");
    runInTerminal(appsShownThird,terminalShownThird);
    thirdShown.go();
    tilesEnd.go();
    managerMenu.go();
    quitManager.go();
    thirdQuit.go();
    stabilize();
    checkContent(List.of("third.exit"),"0\n");
    launchScript("fourth",project);
    runInTerminal(appsShownFourth,terminalShownFourth);
    fourthShown.go();
    tilesEnd.go();
    managerMenu.go();
    quitManager.go();
    fourthQuit.go();
    stabilize();
    checkContent(List.of("fourth.exit"),"0\n");
  }
  void listEnd(int x, int y, int end){
    var shot= pilot.shot();
    var at= x;
    for (; at<shot.getWidth() && (shot.getRGB(at,y)&0xffffff)==0xffffff; at++){}
    assertEquals(end,at,"The tile list, white along the row y="+y+" from x="+x+", ends at x="+at);
  }
}

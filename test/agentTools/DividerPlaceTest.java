package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.util.List;

final class DividerPlaceTest extends ManagerTest{
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(3500))),
    on("xubuntu_xfce",()->waitUntilTime(val(4500))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(8500))),
    on("xubuntu_xfce",()->waitUntilTime(val(11000))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(26000))),
    on("xubuntu_xfce",()->waitUntilTime(val(32500))));
  final Action tilesEnd= action("tilesEnd",
    on("ubuntu_gnome",()->listEnd(val(200),val(700),val(385))),
    on("xubuntu_xfce",()->listEnd(val(133),val(680),val(318))));
  final Action managerHidden= action("managerHidden",
    on("ubuntu_gnome",()->waitUntilTime(val(29500))),
    on("xubuntu_xfce",()->waitUntilTime(val(37000))));
  final Action tilesWhole= action("tilesWhole",
    on("ubuntu_gnome",()->listEnd(val(200),val(700),val(1918))),
    on("xubuntu_xfce",()->listEnd(val(133),val(680),val(1918))));
  final Action tilesGone= action("tilesGone",
    on("ubuntu_gnome",()->listEnd(val(200),val(700),val(200))),
    on("xubuntu_xfce",()->listEnd(val(133),val(680),val(133))));
  final Action appsShownSecond= action("appsShownSecond",
    on("ubuntu_gnome",()->waitUntilTime(val(32500))),
    on("xubuntu_xfce",()->waitUntilTime(val(41000))));
  final Action terminalShownSecond= action("terminalShownSecond",
    on("ubuntu_gnome",()->waitUntilTime(val(37500))),
    on("xubuntu_xfce",()->waitUntilTime(val(47000))));
  final Action secondEnded= action("secondEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(53000))),
    on("xubuntu_xfce",()->waitUntilTime(val(66500))));
  final Action managerBack= action("managerBack",
    on("ubuntu_gnome",()->waitUntilTime(val(55500))),
    on("xubuntu_xfce",()->waitUntilTime(val(69500))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",()->waitUntilTime(val(62000))),
    on("xubuntu_xfce",()->waitUntilTime(val(77500))));
  final Action appsShownThird= action("appsShownThird",
    on("ubuntu_gnome",()->waitUntilTime(val(65000))),
    on("xubuntu_xfce",()->waitUntilTime(val(81500))));
  final Action terminalShownThird= action("terminalShownThird",
    on("ubuntu_gnome",()->waitUntilTime(val(70000))),
    on("xubuntu_xfce",()->waitUntilTime(val(87500))));
  final Action thirdShown= action("thirdShown",
    on("ubuntu_gnome",()->waitUntilTime(val(87500))),
    on("xubuntu_xfce",()->waitUntilTime(val(109500))));
  final Action thirdQuit= action("thirdQuit",
    on("ubuntu_gnome",()->waitUntilTime(val(94000))),
    on("xubuntu_xfce",()->waitUntilTime(val(117500))));
  final Action appsShownFourth= action("appsShownFourth",
    on("ubuntu_gnome",()->waitUntilTime(val(97000))),
    on("xubuntu_xfce",()->waitUntilTime(val(121500))));
  final Action terminalShownFourth= action("terminalShownFourth",
    on("ubuntu_gnome",()->waitUntilTime(val(102000))),
    on("xubuntu_xfce",()->waitUntilTime(val(127500))));
  final Action fourthShown= action("fourthShown",
    on("ubuntu_gnome",()->waitUntilTime(val(119500))),
    on("xubuntu_xfce",()->waitUntilTime(val(149500))));
  final Action fourthQuit= action("fourthQuit",
    on("ubuntu_gnome",()->waitUntilTime(val(126000))),
    on("xubuntu_xfce",()->waitUntilTime(val(157500))));
  @Override void walk() throws Throwable{
    noManagerData();
    var project= project("helloWorld");
    launchScript("first",project.resolve("hello_world.fearless"));
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    tilesEnd.go();
    closeWindow.go();
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
    tilesWhole.go();
    managerMenu.go();
    quitManager.go();
    thirdQuit.go();
    stabilize();
    checkContent(List.of("third.exit"),"0\n");
    launchScript("fourth",project.resolve("hello_world.fearless"));
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

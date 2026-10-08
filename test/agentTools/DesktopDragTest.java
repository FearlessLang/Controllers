package agentTools;

import java.nio.file.Files;
import java.util.List;

import tools.Fs;

final class DesktopDragTest extends ManagerTest{
  final Action deskShown= action("deskShown",
    on("ubuntu_gnome",()->waitUntilTime(val(2500))),
    on("windows",()->waitUntilTime(val(2500))));
  final Action openTest1= action("openTest1",
    on("ubuntu_gnome",()->doubleClick(val(1822),val(784))),
    on("windows",()->doubleClick(val(37),val(30))));
  final Action test1Shown= action("test1Shown",
    on("ubuntu_gnome",()->waitUntilTime(val(5700))),
    on("windows",()->waitUntilTime(val(5700))));
  final Action placeTest1= action("placeTest1",
    on("ubuntu_gnome",()->drag(val(1596),val(842),val(700),val(842))),
    on("windows",()->drag(val(750),val(62),val(550),val(62))));
  final Action test1Placed= action("test1Placed",
    on("ubuntu_gnome",()->waitUntilTime(val(9700))),
    on("windows",()->waitUntilTime(val(9700))));
  final Action sendTest1Away= action("sendTest1Away",
    on("ubuntu_gnome",()->click(val(1404),val(843))),
    on("windows",()->click(val(724),val(58))));
  final Action test1Away= action("test1Away",
    on("ubuntu_gnome",()->waitUntilTime(val(12700))),
    on("windows",()->waitUntilTime(val(12700))));
  final Action openTest2= action("openTest2",
    on("ubuntu_gnome",()->doubleClick(val(1822),val(546))),
    on("windows",()->doubleClick(val(37),val(135))));
  final Action test2Shown= action("test2Shown",
    on("ubuntu_gnome",()->waitUntilTime(val(15700))),
    on("windows",()->waitUntilTime(val(15700))));
  final Action placeTest2= action("placeTest2",
    on("ubuntu_gnome",()->drag(val(1596),val(842),val(2600),val(842))),
    on("windows",()->drag(val(600),val(90),val(800),val(90))));
  final Action test2Placed= action("test2Placed",
    on("ubuntu_gnome",()->waitUntilTime(val(19700))),
    on("windows",()->waitUntilTime(val(19700))));
  final Action showOpenWindows= action("showOpenWindows",
    on("ubuntu_gnome",()->click(val(32),val(128))),
    on("windows",()->click(val(975),val(696))));
  final Action openWindowsShown= action("openWindowsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(22700))),
    on("windows",()->waitUntilTime(val(22700))));
  final Action pickTest1= action("pickTest1",
    on("ubuntu_gnome",()->click(val(2000),val(640))),
    on("windows",()->click(val(710),val(696))));
  final Action test1Back= action("test1Back",
    on("ubuntu_gnome",()->waitUntilTime(val(25700))),
    on("windows",()->waitUntilTime(val(25700))));
  final Action carryFile= action("carryFile",
    on("ubuntu_gnome",()->drag(val(862),val(916),val(3000),val(1100))),
    on("windows",()->drag(val(282),val(226),val(950),val(300))));
  final Action fileCarried= action("fileCarried",
    on("ubuntu_gnome",()->waitUntilTime(val(31000))),
    on("windows",()->waitUntilTime(val(31000))));
  final Action closeTest1= action("closeTest1",
    on("ubuntu_gnome",()->click(val(1479),val(843))),
    on("windows",()->click(val(816),val(58))));
  final Action test1Closed= action("test1Closed",
    on("ubuntu_gnome",()->waitUntilTime(val(34000))),
    on("windows",()->waitUntilTime(val(34000))));
  final Action closeTest2= action("closeTest2",
    on("ubuntu_gnome",()->click(val(3379),val(843))),
    on("windows",()->click(val(1042),val(83))));
  static final String content= "carried across the desk\n";
  @Override void walk() throws Throwable{
    var test1= filesIOFolder.resolve("test1");
    var test2= filesIOFolder.resolve("test2");
    Fs.rmTree(test1);
    Fs.rmTree(test2);
    Fs.writeUtf8(test1.resolve("example.txt"),content);
    Fs.ensureDir(test2);
    stabilize();
    deskShown.go();
    openTest1.go();
    test1Shown.go();
    placeTest1.go();
    test1Placed.go();
    sendTest1Away.go();
    test1Away.go();
    openTest2.go();
    test2Shown.go();
    placeTest2.go();
    test2Placed.go();
    showOpenWindows.go();
    openWindowsShown.go();
    pickTest1.go();
    test1Back.go();
    carryFile.go();
    fileCarried.go();
    closeTest1.go();
    test1Closed.go();
    closeTest2.go();
    stabilize();
    checkContent(List.of("test2","example.txt"),content);
    assert !Files.exists(test1.resolve("example.txt"));
    Fs.rmTree(test1);
    Fs.rmTree(test2);
  }
}

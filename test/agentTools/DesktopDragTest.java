package agentTools;

import java.nio.file.Files;
import java.nio.file.Path;

import tools.Fs;
import utils.Err;

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
    var desk= hostDesk ? Path.of(System.getProperty("user.home"),"Desktop") : filesIOFolder;
    var test1= desk.resolve("test1");
    var test2= desk.resolve("test2");
    Fs.rmTree(test1);
    Fs.rmTree(test2);
    Fs.writeUtf8(test1.resolve("example.txt"),content);
    Fs.ensureDir(test2);
    stabilize();
    deskShown.go();
    var bare= look();
    openTest1.go();
    test1Shown.go();
    var opened= look();
    changed("A double click on the icon of test1 opens its window",bare,opened);
    placeTest1.go();
    test1Placed.go();
    var placed= look();
    changed("Carrying the title bar moves the window of test1",opened,placed);
    var window1= changed("The window of test1 stands at its new place",bare,placed);
    sendTest1Away.go();
    test1Away.go();
    same("The minimize button sends the window of test1 away",bare,look(),window1);
    openTest2.go();
    test2Shown.go();
    var opened2= look();
    changed("A double click on the icon of test2 opens its window",bare,opened2);
    placeTest2.go();
    test2Placed.go();
    var placed2= look();
    changed("Carrying the title bar moves the window of test2",opened2,placed2);
    var window2= changed("The window of test2 stands at its new place",bare,placed2);
    showOpenWindows.go();
    openWindowsShown.go();
    pickTest1.go();
    test1Back.go();
    var both= look();
    differ("The bar of open windows brings the window of test1 back",placed2,both,window1);
    carryFile.go();
    fileCarried.go();
    closeTest1.go();
    test1Closed.go();
    same("The close button closes the window of test1",placed2,look(),window1);
    closeTest2.go();
    same("The close button closes the window of test2",bare,look(),window2);
    stabilize();
    Err.strCmp(content,Fs.readUtf8(test2.resolve("example.txt")));
    assert !Files.exists(test1.resolve("example.txt"));
    Fs.rmTree(test1);
    Fs.rmTree(test2);
  }
}

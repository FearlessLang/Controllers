package agentTools;

import java.nio.file.Files;
import java.util.List;

import tools.Fs;

final class DesktopDragTest extends ManagerTest{
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitAfter(val(3500))),
    on("xubuntu_xfce",()->waitAfter(val(4500))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitAfter(val(5000))),
    on("xubuntu_xfce",()->waitAfter(val(6500))));
  final Action test1Shown= action("test1Shown",
    on("ubuntu_gnome",()->waitAfter(val(10500))),
    on("xubuntu_xfce",()->waitAfter(val(13000))));
  final Action appsShownSecond= action("appsShownSecond",
    on("ubuntu_gnome",()->waitAfter(val(3500))),
    on("xubuntu_xfce",()->waitAfter(val(4500))));
  final Action terminalShownSecond= action("terminalShownSecond",
    on("ubuntu_gnome",()->waitAfter(val(5500))),
    on("xubuntu_xfce",()->waitAfter(val(6500))));
  final Action test2Shown= action("test2Shown",
    on("ubuntu_gnome",()->waitAfter(val(10000))),
    on("xubuntu_xfce",()->waitAfter(val(12500))));
  final Action raiseTest1= action("raiseTest1",
    on("ubuntu_gnome",()->click(val(1320),val(303))),
    on("xubuntu_xfce",()->drag(val(900),val(309),val(1500),val(309))));
  final Action test1Raised= action("test1Raised",
    on("ubuntu_gnome",()->waitAfter(val(3500))),
    on("xubuntu_xfce",()->waitAfter(val(4500))));
  final Action carryFile= action("carryFile",
    on("ubuntu_gnome",()->drag(val(799),val(375),val(1465),val(600))),
    on("xubuntu_xfce",()->drag(val(879),val(425),val(1600),val(600))));
  final Action fileCarried= action("fileCarried",
    on("ubuntu_gnome",()->waitAfter(val(7000))),
    on("xubuntu_xfce",()->waitAfter(val(9000))));
  final Action closeTest1= action("closeTest1",
    on("ubuntu_gnome",()->click(val(1414),val(303))),
    on("xubuntu_xfce",()->click(val(1275),val(309))));
  final Action test1Closed= action("test1Closed",
    on("ubuntu_gnome",()->waitAfter(val(3500))),
    on("xubuntu_xfce",()->waitAfter(val(4000))));
  final Action closeTest2= action("closeTest2",
    on("ubuntu_gnome",()->click(val(1464),val(354))),
    on("xubuntu_xfce",()->click(val(1875),val(309))));
  final Action test2Closed= action("test2Closed",
    on("ubuntu_gnome",()->waitAfter(val(3500))),
    on("xubuntu_xfce",()->waitAfter(val(4500))));
  static final String content= "carried across the desk\n";
  @Override void walk() throws Throwable{
    var test1= filesIOFolder.resolve("test1");
    var test2= filesIOFolder.resolve("test2");
    Fs.rmTree(test1);
    Fs.rmTree(test2);
    Fs.writeUtf8(test1.resolve("example.txt"),content);
    Fs.ensureDir(test2);
    shell("nohup setsid -f xdg-open '"+test1+"' >/dev/null 2>&1\n");
    runInTerminal(appsShown,terminalShown);
    test1Shown.go();
    shell("nohup setsid -f xdg-open '"+test2+"' >/dev/null 2>&1\n");
    runInTerminal(appsShownSecond,terminalShownSecond);
    test2Shown.go();
    raiseTest1.go();
    test1Raised.go();
    carryFile.go();
    fileCarried.go();
    closeTest1.go();
    test1Closed.go();
    closeTest2.go();
    test2Closed.go();
    stabilize();
    checkContent(List.of("test2","example.txt"),content);
    assert !Files.exists(test1.resolve("example.txt"));
    Fs.rmTree(test1);
    Fs.rmTree(test2);
  }
}

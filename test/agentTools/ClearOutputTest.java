package agentTools;

import java.awt.event.KeyEvent;
import java.util.List;

final class ClearOutputTest extends ManagerTest{
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(2000))),
    on("debian_gnome_x11",()->waitUntilTime(val(2000))),
    on("xubuntu_xfce",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(5500))),
    on("debian_gnome_x11",()->waitUntilTime(val(6500))),
    on("xubuntu_xfce",()->waitUntilTime(val(6500))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(18500))),
    on("debian_gnome_x11",()->waitUntilTime(val(20000))),
    on("xubuntu_xfce",()->waitUntilTime(val(20000))));
  final Action focusTiles= action("focusTiles",
    on("ubuntu_gnome",()->click(val(200),val(1500))),
    on("debian_gnome_x11",()->click(val(134),val(1500))),
    on("xubuntu_xfce",()->click(val(268),val(1500))),
    on("windows",()->click(val(200),val(400))));
  final Action closeInformation= action("closeInformation",
    on("ubuntu_gnome",()->click(val(150),val(167))),
    on("debian_gnome_x11",()->click(val(84),val(167))),
    on("xubuntu_xfce",()->click(val(176),val(288))),
    on("windows",()->click(val(79),val(120))));
  final Action check= action("check",
    on("ubuntu_gnome",()->click(val(164),val(111))),
    on("debian_gnome_x11",()->click(val(102),val(112))),
    on("xubuntu_xfce",()->click(val(210),val(185))),
    on("windows",()->click(val(102),val(67))));
  final Action checked= action("checked",
    on("ubuntu_gnome",()->waitUntilTime(val(34000))),
    on("debian_gnome_x11",()->waitUntilTime(val(35500))),
    on("xubuntu_xfce",()->waitUntilTime(val(35500))));
  final Action checkAgain= action("checkAgain",
    on("ubuntu_gnome",()->click(val(164),val(111))),
    on("debian_gnome_x11",()->click(val(102),val(112))),
    on("xubuntu_xfce",()->click(val(210),val(185))),
    on("windows",()->click(val(102),val(67))));
  final Action checkedAgain= action("checkedAgain",
    on("ubuntu_gnome",()->waitUntilTime(val(48000))),
    on("debian_gnome_x11",()->waitUntilTime(val(49500))),
    on("xubuntu_xfce",()->waitUntilTime(val(49500))));
  final Action clearOutput= action("clearOutput",
    on("ubuntu_gnome",()->click(val(3785),val(233))),
    on("debian_gnome_x11",()->click(val(3785),val(233))),
    on("xubuntu_xfce",()->click(val(3714),val(412))),
    on("windows",()->click(val(1227),val(184))));
  final Action outputCleared= action("outputCleared",
    on("ubuntu_gnome",()->waitUntilTime(val(50500))),
    on("debian_gnome_x11",()->waitUntilTime(val(52000))),
    on("xubuntu_xfce",()->waitUntilTime(val(52000))));
  final Action checkAfterClear= action("checkAfterClear",
    on("ubuntu_gnome",()->click(val(164),val(111))),
    on("debian_gnome_x11",()->click(val(102),val(112))),
    on("xubuntu_xfce",()->click(val(210),val(185))),
    on("windows",()->click(val(102),val(67))));
  final Action checkedAfterClear= action("checkedAfterClear",
    on("ubuntu_gnome",()->waitUntilTime(val(64500))),
    on("debian_gnome_x11",()->waitUntilTime(val(66000))),
    on("xubuntu_xfce",()->waitUntilTime(val(66000))));
  final Action appsShownToEnd= action("appsShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(66000))),
    on("debian_gnome_x11",()->waitUntilTime(val(67500))),
    on("xubuntu_xfce",()->waitUntilTime(val(67500))));
  final Action terminalShownToEnd= action("terminalShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(69500))),
    on("debian_gnome_x11",()->waitUntilTime(val(72000))),
    on("xubuntu_xfce",()->waitUntilTime(val(72000))));
  final Action managerEnded= action("managerEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(79500))),
    on("debian_gnome_x11",()->waitUntilTime(val(82000))),
    on("xubuntu_xfce",()->waitUntilTime(val(82000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var project= project("helloWorld");
    var console= List.of(data,"eclipse","hello_world","console.txt");
    var ok= "--- ok: no problem found ---\n";
    launchScript("first",project);
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    focusTiles.go();
    keys(KeyEvent.VK_F8);
    keys(KeyEvent.VK_HOME);
    closeInformation.go();
    stabilize();
    checkContent(console,"");
    check.go();
    checked.go();
    stabilize();
    checkContent(console,ok);
    checkAgain.go();
    checkedAgain.go();
    stabilize();
    checkContent(console,ok+ok);
    clearOutput.go();
    outputCleared.go();
    stabilize();
    checkContent(console,"");
    checkAfterClear.go();
    checkedAfterClear.go();
    stabilize();
    checkContent(console,ok);
    endScript();
    runInTerminal(appsShownToEnd,terminalShownToEnd);
    managerEnded.go();
    stabilize();
    checkContent(List.of("first.exit"),"137\n");
  }
}

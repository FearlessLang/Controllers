package agentTools;

import java.awt.event.KeyEvent;
import java.util.List;

final class ClearOutputTest extends ManagerTest{
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(18500))));
  final Action focusTiles= action("focusTiles",
    on("ubuntu_gnome",()->click(val(200),val(1500))),
    on("windows",()->click(val(200),val(400))));
  final Action closeInformation= action("closeInformation",
    on("ubuntu_gnome",()->click(val(150),val(167))),
    on("windows",()->click(val(79),val(120))));
  final Action check= action("check",
    on("ubuntu_gnome",()->click(val(164),val(111))),
    on("windows",()->click(val(102),val(67))));
  final Action checked= action("checked",
    on("ubuntu_gnome",()->waitUntilTime(val(34000))));
  final Action checkAgain= action("checkAgain",
    on("ubuntu_gnome",()->click(val(164),val(111))),
    on("windows",()->click(val(102),val(67))));
  final Action checkedAgain= action("checkedAgain",
    on("ubuntu_gnome",()->waitUntilTime(val(48000))));
  final Action clearOutput= action("clearOutput",
    on("ubuntu_gnome",()->click(val(3785),val(233))),
    on("windows",()->click(val(1227),val(184))));
  final Action outputCleared= action("outputCleared",
    on("ubuntu_gnome",()->waitUntilTime(val(50500))));
  final Action checkAfterClear= action("checkAfterClear",
    on("ubuntu_gnome",()->click(val(164),val(111))),
    on("windows",()->click(val(102),val(67))));
  final Action checkedAfterClear= action("checkedAfterClear",
    on("ubuntu_gnome",()->waitUntilTime(val(64500))));
  final Action appsShownToEnd= action("appsShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(66000))));
  final Action terminalShownToEnd= action("terminalShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(69500))));
  final Action managerEnded= action("managerEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(79500))));
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

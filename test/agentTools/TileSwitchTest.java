package agentTools;

import java.util.List;

final class TileSwitchTest extends ManagerTest{
  static final List<String> helloConsole= List.of(data,"eclipse","hello_world","console.txt");
  static final List<String> startConsole= List.of(data,"eclipse","start","console.txt");
  static final String ok= "--- ok: no problem found ---\n";
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(18000))));
  final Action closeInformation= action("closeInformation",
    on("ubuntu_gnome",()->click(val(469),val(167))),
    on("windows",()->click(val(79),val(120))));
  final Action check= action("check",
    on("ubuntu_gnome",()->click(val(483),val(111))),
    on("windows",()->click(val(102),val(67))));
  final Action checkedOnce= action("checkedOnce",
    on("ubuntu_gnome",()->waitUntilTime(val(30500))));
  final Action checkAgain= action("checkAgain",
    on("ubuntu_gnome",()->click(val(483),val(111))),
    on("windows",()->click(val(102),val(67))));
  final Action checkedTwice= action("checkedTwice",
    on("ubuntu_gnome",()->waitUntilTime(val(41500))));
  final Action appsShownSecond= action("appsShownSecond",
    on("ubuntu_gnome",()->waitUntilTime(val(43000))));
  final Action terminalShownSecond= action("terminalShownSecond",
    on("ubuntu_gnome",()->waitUntilTime(val(46500))));
  final Action secondHandled= action("secondHandled",
    on("ubuntu_gnome",()->waitUntilTime(val(57500))));
  final Action checkStart= action("checkStart",
    on("ubuntu_gnome",()->click(val(483),val(111))),
    on("windows",()->click(val(102),val(67))));
  final Action startChecked= action("startChecked",
    on("ubuntu_gnome",()->waitUntilTime(val(68500))));
  final Action helloTile= action("helloTile",
    on("ubuntu_gnome",()->click(val(138),val(192))),
    on("windows",()->click(val(71),val(145))));
  final Action helloShown= action("helloShown",
    on("ubuntu_gnome",()->waitUntilTime(val(71000))));
  final Action clearOutput= action("clearOutput",
    on("ubuntu_gnome",()->click(val(3785),val(233))),
    on("windows",()->click(val(1227),val(184))));
  final Action cleared= action("cleared",
    on("ubuntu_gnome",()->waitUntilTime(val(74500))));
  final Action startTile= action("startTile",
    on("ubuntu_gnome",()->click(val(265),val(192))),
    on("windows",()->click(val(199),val(145))));
  final Action startShown= action("startShown",
    on("ubuntu_gnome",()->waitUntilTime(val(77000))));
  final Action appsShownToEnd= action("appsShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(79500))));
  final Action terminalShownToEnd= action("terminalShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(83000))));
  final Action managerEnded= action("managerEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(93000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var project= project("helloWorld");
    var other= project("helloStackTraces");
    launchScript("first",project);
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    closeInformation.go();
    var empty= look();
    var output= whiteAround(empty);
    check.go();
    checkedOnce.go();
    stabilize();
    checkContent(helloConsole,ok);
    var once= look();
    differ("Check shows its result in the Output",empty,once,output);
    checkAgain.go();
    checkedTwice.go();
    stabilize();
    checkContent(helloConsole,ok+ok);
    var twice= look();
    differ("A second Check adds a line to the Output",once,twice,output);
    launchScript("second",other);
    runInTerminal(appsShownSecond,terminalShownSecond);
    secondHandled.go();
    stabilize();
    checkContent(List.of("second.exit"),"0\n");
    checkContent(startConsole,"");
    differ("The panel of the second project replaces the first",twice,look(),output);
    checkStart.go();
    startChecked.go();
    stabilize();
    checkContent(startConsole,ok);
    checkContent(helloConsole,ok+ok);
    same("The Output of the second project shows its check as the first project showed its own",once,look(),output);
    helloTile.go();
    helloShown.go();
    same("A click on the first tile shows its Output as it was left",twice,look(),output);
    clearOutput.go();
    cleared.go();
    stabilize();
    checkContent(helloConsole,"");
    checkContent(startConsole,ok);
    same("Clear output empties the Output of the first project",empty,look(),output);
    startTile.go();
    startShown.go();
    same("A click on the second tile shows its Output as it was left",once,look(),output);
    endScript();
    runInTerminal(appsShownToEnd,terminalShownToEnd);
    managerEnded.go();
    stabilize();
    checkContent(List.of("first.exit"),"137\n");
  }
}

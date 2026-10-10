package agentTools;

import java.util.List;

final class ForgetRunningProjectTest extends ManagerTest{
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(18000))));
  final Action appsShownGui= action("appsShownGui",
    on("ubuntu_gnome",()->waitUntilTime(val(20500))));
  final Action terminalShownGui= action("terminalShownGui",
    on("ubuntu_gnome",()->waitUntilTime(val(24000))));
  final Action panelShown= action("panelShown",
    on("ubuntu_gnome",()->waitUntilTime(val(35500))));
  final Action becomeCode= action("becomeCode",
    on("ubuntu_gnome",()->click(val(719),val(139))),
    on("windows",()->click(val(332),val(94))));
  final Action codeShown= action("codeShown",
    on("ubuntu_gnome",()->waitUntilTime(val(38000))));
  final Action compile= action("compile",
    on("ubuntu_gnome",()->click(val(483),val(111))),
    on("windows",()->click(val(102),val(67))));
  final Action mainShown= action("mainShown",
    on("ubuntu_gnome",()->waitUntilTime(val(53000))));
  final Action run= action("run",
    on("ubuntu_gnome",()->click(val(483),val(111))),
    on("windows",()->click(val(102),val(67))));
  final Action programShown= action("programShown",
    on("ubuntu_gnome",()->waitUntilTime(val(58000))));
  final Action projectMenu= action("projectMenu",
    on("ubuntu_gnome",()->click(val(158),val(79))),
    on("windows",()->click(val(89),val(33))));
  final Action forgetProject= action("forgetProject",
    on("ubuntu_gnome",()->click(val(180),val(237))),
    on("windows",()->click(val(111),val(191))));
  final Action forgotten= action("forgotten",
    on("ubuntu_gnome",()->waitUntilTime(val(61000))));
  final Action appsShownToEnd= action("appsShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(62500))));
  final Action terminalShownToEnd= action("terminalShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(66000))));
  final Action managerEnded= action("managerEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(76000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var gui= project("testGui1");
    launchScript("first");
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    var empty= look();
    launchScript("second",gui);
    runInTerminal(appsShownGui,terminalShownGui);
    panelShown.go();
    stabilize();
    checkContent(List.of("second.exit"),"0\n");
    becomeCode.go();
    codeShown.go();
    compile.go();
    mainShown.go();
    var idle= look();
    run.go();
    programShown.go();
    var program= changed("Run opens the window of the program",idle,look());
    projectMenu.go();
    forgetProject.go();
    forgotten.go();
    same("Forget project closes the window of the program and empties the panel",empty,look(),program);
    stabilize();
    checkContent(state,"{}\n");
    checkContent(info,"{}\n");
    endScript();
    runInTerminal(appsShownToEnd,terminalShownToEnd);
    managerEnded.go();
    stabilize();
    checkContent(List.of("first.exit"),"137\n");
  }
}

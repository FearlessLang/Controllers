package agentTools;

import java.util.List;

import tools.Fs;

final class OutputClearTest extends ManagerTest{
  final Action becomeCode= action("becomeCode",
    on("ubuntu_gnome",()->click(val(962),val(113))),
    on("xubuntu_xfce",()->click(val(968),val(93))));
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(3500))),
    on("xubuntu_xfce",()->waitUntilTime(val(4500))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(8500))),
    on("xubuntu_xfce",()->waitUntilTime(val(11000))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(26000))),
    on("xubuntu_xfce",()->waitUntilTime(val(32500))));
  final Action codeShown= action("codeShown",
    on("ubuntu_gnome",()->waitUntilTime(val(30500))),
    on("xubuntu_xfce",()->waitUntilTime(val(38500))));
  final Action compile= action("compile",
    on("ubuntu_gnome",()->click(val(515),val(113))),
    on("xubuntu_xfce",()->click(val(473),val(93))));
  final Action compiled= action("compiled",
    on("ubuntu_gnome",()->waitUntilTime(val(51500))),
    on("xubuntu_xfce",()->waitUntilTime(val(64500))));
  final Action clear= action("clear",
    on("ubuntu_gnome",()->click(val(1880),val(150))),
    on("xubuntu_xfce",()->click(val(1877),val(130))));
  final Action cleared= action("cleared",
    on("ubuntu_gnome",()->waitUntilTime(val(54500))),
    on("xubuntu_xfce",()->waitUntilTime(val(68500))));
  final Action run= action("run",
    on("ubuntu_gnome",()->click(val(515),val(113))),
    on("xubuntu_xfce",()->click(val(458),val(93))));
  final Action ran= action("ran",
    on("ubuntu_gnome",()->waitUntilTime(val(62000))),
    on("xubuntu_xfce",()->waitUntilTime(val(77500))));
  final Action clearAgain= action("clearAgain",
    on("ubuntu_gnome",()->click(val(1880),val(150))),
    on("xubuntu_xfce",()->click(val(1877),val(130))));
  final Action clearedAgain= action("clearedAgain",
    on("ubuntu_gnome",()->waitUntilTime(val(65000))),
    on("xubuntu_xfce",()->waitUntilTime(val(81500))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",()->waitUntilTime(val(72000))),
    on("xubuntu_xfce",()->waitUntilTime(val(90000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var said= filesIOFolder.resolve("said");
    Fs.rmTree(said);
    Fs.writeUtf8(said.resolve("said.fearless"),"\n");
    Fs.writeUtf8(said.resolve("_said").resolve("_rank_app.fear"),"use base.Main as Main;\n\nSay: Main{sys -> sys.out.println \"said once\"}\n");
    launchScript("first",said.resolve("said.fearless"));
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    becomeCode.go();
    codeShown.go();
    var output= inWindow(410,170,1000,90);
    var empty= look();
    compile.go();
    compiled.go();
    changed(empty,output);
    clear.go();
    cleared.go();
    same(empty,output);
    run.go();
    ran.go();
    changed(empty,output);
    clearAgain.go();
    clearedAgain.go();
    same(empty,output);
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
  }
}

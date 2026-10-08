package agentTools;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.util.List;

import tools.Fs;

final class BrowseFilesTest extends ManagerTest{
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(18500))));
  final Action projectMenu= action("projectMenu",
    on("ubuntu_gnome",()->click(val(158),val(79))),
    on("windows",()->click(val(89),val(33))));
  final Action browseFiles= action("browseFiles",
    on("ubuntu_gnome",()->click(val(180),val(149))),
    on("windows",()->click(val(111),val(103))));
  final Action filesShown= action("filesShown",
    on("ubuntu_gnome",()->waitUntilTime(val(22500))));
  final Action closeFiles= action("closeFiles",
    on("ubuntu_gnome",()->click(val(2374),val(843))),
    on("windows",()->click(val(1016),val(58))));
  final Action filesClosed= action("filesClosed",
    on("ubuntu_gnome",()->waitUntilTime(val(24000))));
  final Action tileChanged= action("tileChanged",
    on("ubuntu_gnome",()->waitUntilTime(val(26000))));
  final Action projectMenuAgain= action("projectMenuAgain",
    on("ubuntu_gnome",()->click(val(158),val(79))),
    on("windows",()->click(val(89),val(33))));
  final Action browseFilesAgain= action("browseFilesAgain",
    on("ubuntu_gnome",()->click(val(180),val(149))),
    on("windows",()->click(val(111),val(103))));
  final Action noteShown= action("noteShown",
    on("ubuntu_gnome",()->waitUntilTime(val(29000))));
  final Action ok= action("ok",
    on("ubuntu_gnome",()->click(val(1952),val(1136))),
    on("windows",()->click(val(639),val(369))));
  final Action noteGone= action("noteGone",
    on("ubuntu_gnome",()->waitUntilTime(val(30500))));
  final Action appsShownToEnd= action("appsShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(32000))));
  final Action terminalShownToEnd= action("terminalShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(35500))));
  final Action managerEnded= action("managerEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(45500))));
  @Override void walk() throws Throwable{
    noManagerData();
    var browsed= filesIOFolder.resolve("browsed");
    var moved= filesIOFolder.resolve("browsed2");
    Fs.rmTree(browsed);
    Fs.rmTree(moved);
    Fs.writeUtf8(browsed.resolve("browsed.fearless"),"\n");
    Fs.writeUtf8(browsed.resolve("_browsed").resolve("_rank_app.fear"),"use base.Main as Main;\n\nHello:Main{s->base.Debug#(\"browsed\")}\n");
    launchScript("first",browsed);
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    projectMenu.go();
    browseFiles.go();
    filesShown.go();
    closeFiles.go();
    filesClosed.go();
    Files.move(browsed,moved);
    tileChanged.go();
    projectMenuAgain.go();
    browseFilesAgain.go();
    noteShown.go();
    stabilize();
    checkContent(notes,"Nothing is opened: nothing exists at\n"+browsed+"\n");
    ok.go();
    noteGone.go();
    stabilize();
    assertFalse(Files.exists(browsed));
    endScript();
    runInTerminal(appsShownToEnd,terminalShownToEnd);
    managerEnded.go();
    stabilize();
    checkContent(List.of("first.exit"),"137\n");
  }
}

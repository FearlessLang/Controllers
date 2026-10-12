package agentTools;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.awt.Rectangle;
import java.nio.file.Files;
import java.util.List;

import tools.Fs;

final class BrowseFilesTest extends ManagerTest{
  final Action appsShown= action("appsShown",on("ubuntu_gnome",()->waitUntilTime(val(3500))));
  final Action terminalShown= action("terminalShown",on("ubuntu_gnome",()->waitUntilTime(val(8500))));
  final Action managerShown= action("managerShown",on("ubuntu_gnome",()->waitUntilTime(val(26000))));
  final Action browse= action("browse",on("ubuntu_gnome",()->click(val(1097),val(113))));
  final Action filesShown= action("filesShown",on("ubuntu_gnome",()->waitUntilTime(val(32500))));
  final Action closeFiles= action("closeFiles",on("ubuntu_gnome",()->click(val(1414),val(303))));
  final Action filesClosed= action("filesClosed",on("ubuntu_gnome",()->waitUntilTime(val(36000))));
  final Action tileChanged= action("tileChanged",on("ubuntu_gnome",()->waitUntilTime(val(42500))));
  final Action browseAgain= action("browseAgain",on("ubuntu_gnome",()->click(val(1097),val(113))));
  final Action noteShown= action("noteShown",on("ubuntu_gnome",()->waitUntilTime(val(46000))));
  final Action managerQuit= action("managerQuit",on("ubuntu_gnome",()->waitUntilTime(val(53000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var browsed= filesIOFolder.resolve("browsed");
    var moved= filesIOFolder.resolve("browsed2");
    Fs.rmTree(browsed);
    Fs.rmTree(moved);
    Fs.writeUtf8(browsed.resolve("browsed.fearless"),"\n");
    Fs.writeUtf8(browsed.resolve("_browsed").resolve("_rank_app.fear"),"use base.Main as Main;\n\nHello:Main{s->base.Debug#(\"browsed\")}\n");
    launchScript("first",browsed.resolve("browsed.fearless"));
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    var center= new Rectangle(760,460,400,160);
    var before= look();
    browse.go();
    filesShown.go();
    changed(before,center);
    closeFiles.go();
    filesClosed.go();
    same(before,center);
    Files.move(browsed,moved);
    tileChanged.go();
    browseAgain.go();
    noteShown.go();
    changed(before,center);
    stabilize();
    assertFalse(Files.exists(browsed));
    closeDialog.go();
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
  }
}

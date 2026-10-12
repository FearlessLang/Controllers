package agentTools;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.awt.Rectangle;
import java.nio.file.Files;
import java.util.List;

import tools.Fs;

final class RefusedFolderTest extends ManagerTest{
  final Action appsShown= action("appsShown",on("ubuntu_gnome",()->waitUntilTime(val(3500))));
  final Action terminalShown= action("terminalShown",on("ubuntu_gnome",()->waitUntilTime(val(8500))));
  final Action noteShown= action("noteShown",on("ubuntu_gnome",()->waitUntilTime(val(27500))));
  final Action appsShownAgain= action("appsShownAgain",on("ubuntu_gnome",()->waitUntilTime(val(31000))));
  final Action terminalShownAgain= action("terminalShownAgain",on("ubuntu_gnome",()->waitUntilTime(val(36000))));
  final Action noteShownAgain= action("noteShownAgain",on("ubuntu_gnome",()->waitUntilTime(val(46500))));
  final Action managerQuit= action("managerQuit",on("ubuntu_gnome",()->waitUntilTime(val(55000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var gone= filesIOFolder.resolve("gone");
    var folder= filesIOFolder.resolve(data);
    var center= new Rectangle(760,460,400,160);
    Fs.rmTree(gone);
    var before= look();
    launchScript("first",gone.resolve("gone.fearless"));
    runInTerminal(appsShown,terminalShown);
    noteShown.go();
    changed(before,center);
    stabilize();
    checkContent(info,"{\n  \"projects\": {},\n  \"systemExtensions\": []\n}\n");
    assertFalse(Files.exists(gone));
    closeDialog.go();
    Fs.writeUtf8(folder.resolve("inside.fearless"),"\n");
    launchScript("second",folder.resolve("inside.fearless"));
    runInTerminal(appsShownAgain,terminalShownAgain);
    noteShownAgain.go();
    changed(before,center);
    stabilize();
    checkContent(List.of("second.exit"),"0\n");
    checkContent(info,"{\n  \"projects\": {},\n  \"systemExtensions\": []\n}\n");
    closeDialog.go();
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
  }
}

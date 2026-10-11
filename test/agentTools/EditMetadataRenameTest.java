package agentTools;

import java.awt.Rectangle;
import java.nio.file.Files;
import java.util.List;

import tools.Fs;

final class EditMetadataRenameTest extends ManagerTest{
  final Action appsShown= action("appsShown",on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",on("ubuntu_gnome",()->waitUntilTime(val(18000))));
  final Action editorShown= action("editorShown",on("ubuntu_gnome",()->waitUntilTime(val(22000))));
  final Action nameOld= action("nameOld",on("ubuntu_gnome",()->doubleClick(val(1606),val(882))));
  final Action commit= action("commit",on("ubuntu_gnome",()->click(val(2238),val(1370))));
  final Action renamed= action("renamed",on("ubuntu_gnome",()->waitUntilTime(val(29000))));
  final Action markerSeen= action("markerSeen",on("ubuntu_gnome",()->waitUntilTime(val(36000))));
  final Action managerQuit= action("managerQuit",on("ubuntu_gnome",()->waitUntilTime(val(41000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var old= filesIOFolder.resolve("old");
    Fs.rmTree(old);
    Fs.writeUtf8(old.resolve("old.fearless"),"\n");
    Fs.writeUtf8(old.resolve("_old").resolve("_rank_app.fear"),"use base.Main as Main;\n\nHello:Main{s->base.Debug#(\"old\")}\n");
    launchScript("first",old.resolve("old.fearless"));
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    var tile= new Rectangle(69,95,128,85);
    var valid= look();
    managerMenu.go();
    editMetadata.go();
    editorShown.go();
    nameOld.go();
    type("fresh");
    commit.go();
    renamed.go();
    changed(valid,tile);
    stabilize();
    checkContent(info,"""
      {
        "projects": {
          "fresh": {
            "path": "Str:%s",
            "kind": "idle"
          }
        },
        "systemExtensions": []
      }
      """.formatted(slashed(old)));
    var invalid= look();
    Files.move(old.resolve("old.fearless"),old.resolve("fresh.fearless"));
    stabilize();
    markerSeen.go();
    changed(invalid,tile);
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
  }
}

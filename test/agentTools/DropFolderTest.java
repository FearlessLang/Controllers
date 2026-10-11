package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.util.List;

import tools.Fs;

final class DropFolderTest extends ManagerTest{
  final Action appsShown= action("appsShown",on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",on("ubuntu_gnome",()->waitUntilTime(val(18000))));
  final Action appsShownFiles= action("appsShownFiles",on("ubuntu_gnome",()->waitUntilTime(val(19500))));
  final Action terminalShownFiles= action("terminalShownFiles",on("ubuntu_gnome",()->waitUntilTime(val(23000))));
  final Action filesShown= action("filesShown",on("ubuntu_gnome",()->waitUntilTime(val(34000))));
  final Action carryDropped= action("carryDropped",on("ubuntu_gnome",()->drag(val(1759),val(915),val(200),val(1500))));
  final Action droppedSaved= action("droppedSaved",on("ubuntu_gnome",()->waitUntilTime(val(41000))));
  final Action closeFiles= action("closeFiles",on("ubuntu_gnome",()->click(val(2374),val(843))));
  final Action filesClosed= action("filesClosed",on("ubuntu_gnome",()->waitUntilTime(val(49000))));
  final Action managerQuit= action("managerQuit",on("ubuntu_gnome",()->waitUntilTime(val(46000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var drops= filesIOFolder.resolve("drops");
    var dropped= drops.resolve("dropped");
    Fs.rmTree(drops);
    Fs.writeUtf8(dropped.resolve("dropped.fearless"),"\n");
    Fs.writeUtf8(dropped.resolve("_dropped").resolve("_rank_app.fear"),"use base.Main as Main;\n\nHello:Main{s->base.Debug#(\"dropped\")}\n");
    launchScript("first");
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    shell("nohup setsid -f xdg-open '"+drops+"' >/dev/null 2>&1\n");
    runInTerminal(appsShownFiles,terminalShownFiles);
    filesShown.go();
    carryDropped.go();
    droppedSaved.go();
    stabilize();
    checkContent(info,"""
      {
        "projects": {
          "dropped": {
            "path": "Str:%s",
            "kind": "idle"
          }
        },
        "systemExtensions": []
      }
      """.formatted(slashed(dropped)));
    assertEquals(List.of("dropped/_dropped/_rank_app.fear","dropped/dropped.fearless"),Fs.walk(drops,s->s.filter(Files::isRegularFile).map(p->slashed(drops.relativize(p))).sorted().toList()));
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
    closeFiles.go();
    filesClosed.go();
  }
}

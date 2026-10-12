package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.util.List;

import tools.Fs;

final class DropFolderTest extends ManagerTest{
  final Action appsShown= action("appsShown",on("ubuntu_gnome",()->waitUntilTime(val(3500))));
  final Action terminalShown= action("terminalShown",on("ubuntu_gnome",()->waitUntilTime(val(8500))));
  final Action managerShown= action("managerShown",on("ubuntu_gnome",()->waitUntilTime(val(26000))));
  final Action appsShownFiles= action("appsShownFiles",on("ubuntu_gnome",()->waitUntilTime(val(29000))));
  final Action terminalShownFiles= action("terminalShownFiles",on("ubuntu_gnome",()->waitUntilTime(val(34000))));
  final Action filesShown= action("filesShown",on("ubuntu_gnome",()->waitUntilTime(val(49500))));
  final Action carryDropped= action("carryDropped",on("ubuntu_gnome",()->drag(val(799),val(375),val(200),val(700))));
  final Action droppedSaved= action("droppedSaved",on("ubuntu_gnome",()->waitUntilTime(val(59500))));
  final Action closeFiles= action("closeFiles",on("ubuntu_gnome",()->click(val(1414),val(303))));
  final Action filesClosed= action("filesClosed",on("ubuntu_gnome",()->waitUntilTime(val(71000))));
  final Action managerQuit= action("managerQuit",on("ubuntu_gnome",()->waitUntilTime(val(66500))));
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

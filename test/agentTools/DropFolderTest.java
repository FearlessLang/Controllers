package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.util.List;

import tools.Fs;

final class DropFolderTest extends ManagerTest{
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(18500))));
  final Action appsShownFiles= action("appsShownFiles",
    on("ubuntu_gnome",()->waitUntilTime(val(20000))));
  final Action terminalShownFiles= action("terminalShownFiles",
    on("ubuntu_gnome",()->waitUntilTime(val(23500))));
  final Action filesShown= action("filesShown",
    on("ubuntu_gnome",()->waitUntilTime(val(36500))));
  final Action carryDropped= action("carryDropped",
    on("ubuntu_gnome",()->drag(val(1759),val(915),val(200),val(1500))),
    on("windows",()->drag(val(475),val(226),val(130),val(300))));
  final Action droppedSaved= action("droppedSaved",
    on("ubuntu_gnome",()->waitUntilTime(val(41500))));
  final Action appsShownToRestart= action("appsShownToRestart",
    on("ubuntu_gnome",()->waitUntilTime(val(43000))));
  final Action terminalShownToRestart= action("terminalShownToRestart",
    on("ubuntu_gnome",()->waitUntilTime(val(46500))));
  final Action managerEndedToRestart= action("managerEndedToRestart",
    on("ubuntu_gnome",()->waitUntilTime(val(56500))));
  final Action closeFiles= action("closeFiles",
    on("ubuntu_gnome",()->click(val(2374),val(843))),
    on("windows",()->click(val(1016),val(58))));
  final Action filesClosed= action("filesClosed",
    on("ubuntu_gnome",()->waitUntilTime(val(59000))));
  final Action appsShownSecond= action("appsShownSecond",
    on("ubuntu_gnome",()->waitUntilTime(val(60500))));
  final Action terminalShownSecond= action("terminalShownSecond",
    on("ubuntu_gnome",()->waitUntilTime(val(64000))));
  final Action managerShownSecond= action("managerShownSecond",
    on("ubuntu_gnome",()->waitUntilTime(val(77000))));
  final Action appsShownToEnd= action("appsShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(78500))));
  final Action terminalShownToEnd= action("terminalShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(82000))));
  final Action managerEnded= action("managerEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(92000))));
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
    var remembered= """
      {
        "dropped": {
          "path": "Str:%s",
          "kind": "idle"
        }
      }
      """.formatted(slashed(dropped));
    stabilize();
    checkContent(info,remembered);
    assertEquals(List.of("dropped/_dropped/_rank_app.fear","dropped/dropped.fearless"),Fs.walk(drops,s->s.filter(Files::isRegularFile).map(p->slashed(drops.relativize(p))).sorted().toList()));
    endScript();
    runInTerminal(appsShownToRestart,terminalShownToRestart);
    managerEndedToRestart.go();
    stabilize();
    checkContent(List.of("first.exit"),"137\n");
    noManagerData();
    closeFiles.go();
    filesClosed.go();
    launchScript("second",dropped);
    runInTerminal(appsShownSecond,terminalShownSecond);
    managerShownSecond.go();
    stabilize();
    checkContent(info,remembered);
    endScript();
    runInTerminal(appsShownToEnd,terminalShownToEnd);
    managerEnded.go();
    stabilize();
    checkContent(List.of("second.exit"),"137\n");
  }
}

package agentTools;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.util.List;

import tools.Fs;

final class FolderGoneTest extends ManagerTest{
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(18000))));
  final Action goneNoticed= action("goneNoticed",
    on("ubuntu_gnome",()->waitUntilTime(val(21000))));
  final Action projectMenu= action("projectMenu",
    on("ubuntu_gnome",()->click(val(158),val(79))),
    on("windows",()->click(val(89),val(33))));
  final Action errorReport= action("errorReport",
    on("ubuntu_gnome",()->click(val(180),val(212))),
    on("windows",()->click(val(111),val(166))));
  final Action reportShown= action("reportShown",
    on("ubuntu_gnome",()->waitUntilTime(val(24000))));
  final Action focusReport= action("focusReport",
    on("ubuntu_gnome",()->click(val(1970),val(1100))),
    on("windows",()->click(val(660),val(300))));
  final Action ok= action("ok",
    on("ubuntu_gnome",()->click(val(1953),val(1323))),
    on("windows",()->click(val(639),val(568))));
  final Action reportClosed= action("reportClosed",
    on("ubuntu_gnome",()->waitUntilTime(val(27000))));
  final Action tileBack= action("tileBack",
    on("ubuntu_gnome",()->waitUntilTime(val(30000))));
  final Action appsShownToEnd= action("appsShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(31500))));
  final Action terminalShownToEnd= action("terminalShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(35000))));
  final Action managerEnded= action("managerEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(45000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var gone= filesIOFolder.resolve("gone");
    var moved= filesIOFolder.resolve("gone2");
    Fs.rmTree(gone);
    Fs.rmTree(moved);
    Fs.writeUtf8(gone.resolve("gone.fearless"),"\n");
    Fs.writeUtf8(gone.resolve("_gone").resolve("_rank_app.fear"),"use base.Main as Main;\n\nHello:Main{s->base.Debug#(\"gone\")}\n");
    launchScript("first",gone);
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    Files.move(gone,moved);
    stabilize();
    goneNoticed.go();
    projectMenu.go();
    errorReport.go();
    reportShown.go();
    focusReport.go();
    keys(KeyEvent.VK_CONTROL,KeyEvent.VK_A);
    keys(KeyEvent.VK_CONTROL,KeyEvent.VK_C);
    ok.go();
    reportClosed.go();
    stabilize();
    checkContent(info,"""
      {
        "gone": {
          "path": "Str:%s",
          "kind": "idle"
        }
      }
      """.formatted(slashed(gone)));
    assertFalse(Files.exists(gone));
    Files.move(moved,gone);
    stabilize();
    tileBack.go();
    endScript();
    runInTerminal(appsShownToEnd,terminalShownToEnd);
    managerEnded.go();
    stabilize();
    checkContent(List.of("first.exit"),"137\n");
  }
}

package agentTools;

import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.util.List;

import tools.Fs;

final class EditMetadataRenameTest extends ManagerTest{
  static final String fine= "--- ok: no problem found ---\n";
  static final List<String> console= List.of(data,"eclipse","fresh","console.txt");
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(18500))));
  final Action focusTiles= action("focusTiles",
    on("ubuntu_gnome",()->click(val(200),val(1500))),
    on("windows",()->click(val(200),val(400))));
  final Action check= action("check",
    on("ubuntu_gnome",()->click(val(164),val(111))),
    on("windows",()->click(val(102),val(67))));
  final Action checked= action("checked",
    on("ubuntu_gnome",()->waitUntilTime(val(33500))));
  final Action managerMenu= action("managerMenu",
    on("ubuntu_gnome",()->click(val(98),val(79))),
    on("windows",()->click(val(31),val(33))));
  final Action editMetadata= action("editMetadata",
    on("ubuntu_gnome",()->click(val(128),val(103))),
    on("windows",()->click(val(61),val(58))));
  final Action editorShown= action("editorShown",
    on("ubuntu_gnome",()->waitUntilTime(val(37000))));
  final Action nameOld= action("nameOld",
    on("ubuntu_gnome",()->doubleClick(val(1589),val(864))),
    on("windows",()->doubleClick(val(274),val(87))));
  final Action commit= action("commit",
    on("ubuntu_gnome",()->click(val(2238),val(1370))),
    on("windows",()->click(val(924),val(619))));
  final Action renamed= action("renamed",
    on("ubuntu_gnome",()->waitUntilTime(val(41000))));
  final Action projectMenu= action("projectMenu",
    on("ubuntu_gnome",()->click(val(158),val(79))),
    on("windows",()->click(val(89),val(33))));
  final Action errorReport= action("errorReport",
    on("ubuntu_gnome",()->click(val(180),val(212))),
    on("windows",()->click(val(111),val(166))));
  final Action reportShown= action("reportShown",
    on("ubuntu_gnome",()->waitUntilTime(val(44000))));
  final Action focusReport= action("focusReport",
    on("ubuntu_gnome",()->click(val(1970),val(1100))),
    on("windows",()->click(val(660),val(300))));
  final Action ok= action("ok",
    on("ubuntu_gnome",()->click(val(1953),val(1323))),
    on("windows",()->click(val(639),val(568))));
  final Action openInformation= action("openInformation",
    on("ubuntu_gnome",()->click(val(150),val(167))),
    on("windows",()->click(val(79),val(120))));
  final Action markerSeen= action("markerSeen",
    on("ubuntu_gnome",()->waitUntilTime(val(48500))));
  final Action checkAgain= action("checkAgain",
    on("ubuntu_gnome",()->click(val(164),val(111))),
    on("windows",()->click(val(102),val(67))));
  final Action checkedAgain= action("checkedAgain",
    on("ubuntu_gnome",()->waitUntilTime(val(62000))));
  final Action appsShownToEnd= action("appsShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(63500))));
  final Action terminalShownToEnd= action("terminalShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(67000))));
  final Action managerEnded= action("managerEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(77000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var old= filesIOFolder.resolve("old");
    Fs.rmTree(old);
    Fs.writeUtf8(old.resolve("old.fearless"),"\n");
    Fs.writeUtf8(old.resolve("_old").resolve("_rank_app.fear"),"use base.Main as Main;\n\nHello:Main{s->base.Debug#(\"old\")}\n");
    launchScript("first",old);
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    focusTiles.go();
    keys(KeyEvent.VK_F8);
    keys(KeyEvent.VK_HOME);
    check.go();
    checked.go();
    stabilize();
    checkContent(List.of(data,"eclipse","old","console.txt"),fine);
    managerMenu.go();
    editMetadata.go();
    editorShown.go();
    nameOld.go();
    type("fresh");
    commit.go();
    renamed.go();
    stabilize();
    checkContent(info,"""
      {
        "fresh": {
          "path": "Str:%s",
          "kind": "idle"
        }
      }
      """.formatted(slashed(old)));
    checkContent(console,fine);
    projectMenu.go();
    errorReport.go();
    reportShown.go();
    focusReport.go();
    keys(KeyEvent.VK_CONTROL,KeyEvent.VK_A);
    keys(KeyEvent.VK_CONTROL,KeyEvent.VK_C);
    ok.go();
    openInformation.go();
    Files.move(old.resolve("old.fearless"),old.resolve("fresh.fearless"));
    stabilize();
    markerSeen.go();
    checkAgain.go();
    checkedAgain.go();
    stabilize();
    checkContent(console,fine+fine);
    endScript();
    runInTerminal(appsShownToEnd,terminalShownToEnd);
    managerEnded.go();
    stabilize();
    checkContent(List.of("first.exit"),"137\n");
  }
}

package agentTools;

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
  final Action check= action("check",
    on("ubuntu_gnome",()->click(val(483),val(111))),
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
    on("ubuntu_gnome",()->click(val(469),val(167))),
    on("windows",()->click(val(79),val(120))));
  final Action markerSeen= action("markerSeen",
    on("ubuntu_gnome",()->waitUntilTime(val(51500))));
  final Action checkAgain= action("checkAgain",
    on("ubuntu_gnome",()->click(val(483),val(111))),
    on("windows",()->click(val(102),val(67))));
  final Action checkedAgain= action("checkedAgain",
    on("ubuntu_gnome",()->waitUntilTime(val(65000))));
  final Action appsShownToEnd= action("appsShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(66500))));
  final Action terminalShownToEnd= action("terminalShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(70000))));
  final Action managerEnded= action("managerEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(80000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var old= filesIOFolder.resolve("old");
    Fs.rmTree(old);
    Fs.writeUtf8(old.resolve("old.fearless"),"\n");
    Fs.writeUtf8(old.resolve("_old").resolve("_rank_app.fear"),"use base.Main as Main;\n\nHello:Main{s->base.Debug#(\"old\")}\n");
    launchScript("first",old);
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    check.go();
    checked.go();
    stabilize();
    checkContent(List.of(data,"eclipse","old","console.txt"),fine);
    var valid= look();
    managerMenu.go();
    editMetadata.go();
    editorShown.go();
    var editor= changed("Edit project metadata opens the editor",valid,look());
    nameOld.go();
    type("fresh");
    var typed= look();
    commit.go();
    renamed.go();
    var invalid= look();
    differ("Commit closes the editor",typed,invalid,editor);
    var badge= changed("The tile shows the missing marker",valid,invalid,3);
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
    var report= changed("Error report opens the dialog",invalid,look());
    focusReport.go();
    copied("The marker file \"fresh.fearless\" is missing from\n"+old+"\nRestore it, or forget and re-add this project folder.");
    ok.go();
    same("OK closes the dialog",invalid,look(),report);
    var collapsed= look();
    openInformation.go();
    changed("Information opens its section",collapsed,look());
    Files.move(old.resolve("old.fearless"),old.resolve("fresh.fearless"));
    stabilize();
    markerSeen.go();
    differ("The tile is valid again by itself",invalid,look(),badge);
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

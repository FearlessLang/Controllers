package agentTools;

import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import tools.Fs;

final class RawStateTest extends ManagerTest{
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(18500))));
  final Action managerMenu= action("managerMenu",
    on("ubuntu_gnome",()->click(val(98),val(79))),
    on("windows",()->click(val(31),val(33))));
  final Action showRawState= action("showRawState",
    on("ubuntu_gnome",()->click(val(128),val(124))),
    on("windows",()->click(val(61),val(78))));
  final Action dialogShown= action("dialogShown",
    on("ubuntu_gnome",()->waitUntilTime(val(21200))));
  final Action focusText= action("focusText",
    on("ubuntu_gnome",()->click(val(1970),val(1100))),
    on("windows",()->click(val(660),val(300))));
  final Action ok= action("ok",
    on("ubuntu_gnome",()->click(val(1952),val(1323))),
    on("windows",()->click(val(639),val(569))));
  final Action appsShownNotes= action("appsShownNotes",
    on("ubuntu_gnome",()->waitUntilTime(val(24300))));
  final Action terminalShownNotes= action("terminalShownNotes",
    on("ubuntu_gnome",()->waitUntilTime(val(27800))));
  final Action notesHanded= action("notesHanded",
    on("ubuntu_gnome",()->waitUntilTime(val(38800))));
  final Action appsShownTally= action("appsShownTally",
    on("ubuntu_gnome",()->waitUntilTime(val(40300))));
  final Action terminalShownTally= action("terminalShownTally",
    on("ubuntu_gnome",()->waitUntilTime(val(43800))));
  final Action tallyHanded= action("tallyHanded",
    on("ubuntu_gnome",()->waitUntilTime(val(54800))));
  final Action managerMenuAgain= action("managerMenuAgain",
    on("ubuntu_gnome",()->click(val(98),val(79))),
    on("windows",()->click(val(31),val(33))));
  final Action showRawStateAgain= action("showRawStateAgain",
    on("ubuntu_gnome",()->click(val(128),val(124))),
    on("windows",()->click(val(61),val(78))));
  final Action dialogShownAgain= action("dialogShownAgain",
    on("ubuntu_gnome",()->waitUntilTime(val(57500))));
  final Action focusTextAgain= action("focusTextAgain",
    on("ubuntu_gnome",()->click(val(1970),val(1100))),
    on("windows",()->click(val(660),val(300))));
  final Action okAgain= action("okAgain",
    on("ubuntu_gnome",()->click(val(1952),val(1323))),
    on("windows",()->click(val(639),val(569))));
  final Action appsShownToEnd= action("appsShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(60600))));
  final Action terminalShownToEnd= action("terminalShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(64100))));
  final Action managerEnded= action("managerEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(74100))));
  @Override void walk() throws Throwable{
    noManagerData();
    var notesFolder= filesIOFolder.resolve("notes");
    var tally= filesIOFolder.resolve("tally");
    Fs.rmTree(notesFolder);
    Fs.rmTree(tally);
    var when= DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
    var changed= FileTime.from(ZonedDateTime.parse("2026-01-02 03:04:05",when).toInstant());
    var marker= notesFolder.resolve("notes.fearless");
    var source= notesFolder.resolve("_notes").resolve("_rank_app.fear");
    Fs.writeUtf8(marker,"\n");
    Fs.writeUtf8(source,"use base.Main as Main;\n\nHello:Main{s->base.Debug#(\"notes\")}\n");
    for (var p: List.of(marker,source)){ Files.setLastModifiedTime(p,changed); }
    Fs.ensureDir(tally);
    launchScript("first");
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    var closed= look();
    managerMenu.go();
    showRawState.go();
    dialogShown.go();
    var dialog= changed("Show raw project state opens the dialog",closed,look());
    focusText.go();
    copied("<nothing registered>");
    ok.go();
    same("OK closes the dialog",closed,look(),dialog);
    launchScript("second",notesFolder);
    runInTerminal(appsShownNotes,terminalShownNotes);
    notesHanded.go();
    stabilize();
    checkContent(List.of("second.exit"),"0\n");
    launchScript("third",tally);
    runInTerminal(appsShownTally,terminalShownTally);
    tallyHanded.go();
    stabilize();
    checkContent(List.of("third.exit"),"0\n");
    checkContent(info,"""
      {
        "notes": {
          "path": "Str:%s",
          "kind": "idle"
        },
        "tally": {
          "path": "Str:%s",
          "kind": "code"
        }
      }
      """.formatted(slashed(notesFolder),slashed(tally)));
    var closedAgain= look();
    managerMenuAgain.go();
    showRawStateAgain.go();
    dialogShownAgain.go();
    var dialogAgain= changed("Show raw project state opens the dialog again",closedAgain,look());
    focusTextAgain.go();
    copied("""
      Folder          %s
      Name            notes
      Kind            idle
      Files           2
      Total size      61 bytes
      Last modified   2026-01-02 03:04:05
      Job             none
      Problems        none

      Folder          %s
      Name            tally
      Kind            code
      [###]
      Job             none
      Problems        none
      """.formatted(notesFolder,tally));
    okAgain.go();
    same("OK closes the dialog",closedAgain,look(),dialogAgain);
    endScript();
    runInTerminal(appsShownToEnd,terminalShownToEnd);
    managerEnded.go();
    stabilize();
    checkContent(List.of("first.exit"),"137\n");
  }
}

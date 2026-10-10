package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.List;

import tools.Fs;

final class NestedFolderTest extends ManagerTest{
  final Path nest= filesIOFolder.resolve("nest");
  final Path inner= nest.resolve("inner");
  final Path source= inner.resolve("_inner");
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(18500))));
  final Action appsShownNest= action("appsShownNest",
    on("ubuntu_gnome",()->waitUntilTime(val(20600))));
  final Action terminalShownNest= action("terminalShownNest",
    on("ubuntu_gnome",()->waitUntilTime(val(24100))));
  final Action noteShown= action("noteShown",
    on("ubuntu_gnome",()->waitUntilTime(val(36100))));
  final Action ok= action("ok",
    on("ubuntu_gnome",()->click(val(1952),val(1223))),
    on("windows",()->click(val(639),val(453))));
  final Action appsShownSource= action("appsShownSource",
    on("ubuntu_gnome",()->waitUntilTime(val(38200))));
  final Action terminalShownSource= action("terminalShownSource",
    on("ubuntu_gnome",()->waitUntilTime(val(41700))));
  final Action noteShownAgain= action("noteShownAgain",
    on("ubuntu_gnome",()->waitUntilTime(val(53700))));
  final Action okAgain= action("okAgain",
    on("ubuntu_gnome",()->click(val(1952),val(1223))),
    on("windows",()->click(val(639),val(453))));
  final Action appsShownToEnd= action("appsShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(55800))));
  final Action terminalShownToEnd= action("terminalShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(59300))));
  final Action managerEnded= action("managerEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(69300))));
  @Override void walk() throws Throwable{
    noManagerData();
    Fs.rmTree(nest);
    Fs.writeUtf8(inner.resolve("inner.fearless"),"\n");
    Fs.writeUtf8(source.resolve("_rank_app.fear"),"use base.Main as Main;\n\nHello:Main{s->base.Debug#(\"inner\")}\n");
    var files= tree();
    var remembered= """
      {
        "inner": {
          "path": "Str:%s",
          "kind": "idle"
        }
      }
      """.formatted(slashed(inner));
    launchScript("first",inner);
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    stabilize();
    checkContent(info,remembered);
    var closed= look();
    launchScript("second",nest);
    runInTerminal(appsShownNest,terminalShownNest);
    noteShown.go();
    var shown= changed("The note opens",closed,look());
    stabilize();
    checkContent(List.of("second.exit"),"0\n");
    checkContent(notes,note(nest));
    checkContent(info,remembered);
    assertEquals(files,tree());
    ok.go();
    same("OK closes the note",closed,look(),shown);
    launchScript("third",source);
    runInTerminal(appsShownSource,terminalShownSource);
    noteShownAgain.go();
    var shownAgain= changed("The second note opens",closed,look());
    stabilize();
    checkContent(List.of("third.exit"),"0\n");
    checkContent(notes,note(nest)+note(source));
    checkContent(info,remembered);
    assertEquals(files,tree());
    okAgain.go();
    same("OK closes the second note",closed,look(),shownAgain);
    endScript();
    runInTerminal(appsShownToEnd,terminalShownToEnd);
    managerEnded.go();
    stabilize();
    checkContent(List.of("first.exit"),"137\n");
  }
  String note(Path folder){
    return """
      Fearless cannot keep track of this project folder.

      The manager was asked to register:
        %s
      Fearless is already keeping track of:
        %s
      One of the two is inside the other. Fearless keeps track of project folders
      that do not overlap, so that every file belongs to exactly one project.

      Use the folder Fearless already keeps track of, or make Fearless forget that
      folder first, and then register this one again.
      """.formatted(folder,inner);
  }
  List<Path> tree(){ return Fs.walk(nest,s->s.sorted().toList()); }
}

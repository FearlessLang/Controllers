package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;

import tools.Fs;

/// Show raw project state says <nothing registered> while no project is registered, and once projects are registered it lists the information of each, in the order they were registered, separated by a blank line: folder, name, kind, the count, total size and last change of its files, and for a code project also its cache, compile and run times, packages, mains and links, then its job and problems.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, nothing is registered for .fearless, the folder notes beside the manager holds only its marker and one source file, both last changed at 2026-01-02 03:04:05, and the folder tally beside the manager exists and holds nothing.
/// Action 1: run the launcher: the manager window opens with no tile.
/// Action 2: choose Show raw project state... in its Manager menu: a dialog opens.
/// Action 3: click in its text, select all of it and copy it: the text copied is <nothing registered>.
/// Action 4: press OK: the dialog goes away.
/// Action 5: run the launcher on notes, then on tally: each ends at once, and the manager remembers notes as an idle project and tally as a code project.
/// Action 6: choose Show raw project state... in its Manager menu: a dialog opens.
/// Action 7: click in its text, select all of it and copy it: the text copied is the information of notes, a blank line, then the information of tally.
/// Action 8: press OK: the dialog goes away.
/// Action 9: end the manager.
final class RawStateTest extends ManagerTest{
  static final Path notes= data.resolveSibling("notes");
  static final Path tally= data.resolveSibling("tally");
  static final DateTimeFormatter when= DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
  final At managerShown= new At("managerShown",on("ubuntu-gnome",3000));
  final Area dialog= new Area("dialog",on("ubuntu-gnome",1600,850,700,25));
  final Click managerMenu= new Click("managerMenu",on("ubuntu-gnome",98,79));
  final Click showRawState= new Click("showRawState",on("ubuntu-gnome",128,124));
  final Click focusText= new Click("focusText",on("ubuntu-gnome",1970,1100));
  final Click ok= new Click("ok",on("ubuntu-gnome",1952,1323));
  final Click managerMenuAgain= new Click("managerMenuAgain",on("ubuntu-gnome",98,79));
  final Click showRawStateAgain= new Click("showRawStateAgain",on("ubuntu-gnome",128,124));
  final Click focusTextAgain= new Click("focusTextAgain",on("ubuntu-gnome",1970,1100));
  final Click okAgain= new Click("okAgain",on("ubuntu-gnome",1952,1323));
  @Override protected void walk() throws Exception{
    clean();
    var changed= FileTime.from(ZonedDateTime.parse("2026-01-02 03:04:05",when).toInstant());
    var marker= notes.resolve("notes.fearless");
    var source= notes.resolve("_notes").resolve("_rank_app.fear");
    Fs.writeUtf8(marker,"\n");
    Fs.writeUtf8(source,"use base.Main as Main;\n\nHello:Main{s->base.Debug#(\"notes\")}\n");
    for (var p: List.of(marker,source)){ Files.setLastModifiedTime(p,changed); }
    Fs.ensureDir(tally);
    launch();
    managerShown.go();
    var where= dialog.aim();
    var behind= pixels(where);
    managerMenu.go();
    showRawState.go();
    until(()->!Arrays.equals(behind,pixels(where)));
    focusText.go();
    assertEquals("<nothing registered>",copied());
    ok.go();
    until(()->Arrays.equals(behind,pixels(where)));
    for (var f: List.of(notes,tally)){
      var run= launch(f.toString());
      until(()->!run.isAlive());
      assertEquals(0,run.exitValue());
    }
    assertEquals("""
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
      """.formatted(notes,tally),Fs.readUtf8(info));
    var registered= pixels(where);
    managerMenuAgain.go();
    showRawStateAgain.go();
    until(()->!Arrays.equals(registered,pixels(where)));
    focusTextAgain.go();
    var written= when.format(Instant.ofEpochMilli(Stream.of(tally.resolve("tally.fearless"),tally.resolve("_tally").resolve("_rank_app.fear")).mapToLong(Fs::lastModified).max().getAsLong()));
    assertEquals("""
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
      Files           2
      Total size      169 bytes
      Last modified   %s
      Compiled cache  needs compiling
      Last compile    never
      Last run        never
      Packages        tally
      Mains selected  <none>
      Reads           <none>
      Edits           <none>
      Job             none
      Problems        none""".formatted(notes,tally,written),copied());
    okAgain.go();
    until(()->Arrays.equals(registered,pixels(where)));
    stopManagers();
  }
  private Object copied() throws Exception{
    pilot.chord(KeyEvent.VK_CONTROL,KeyEvent.VK_A);
    pilot.chord(KeyEvent.VK_CONTROL,KeyEvent.VK_C);
    return Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
  }
  @Override @AfterEach void clean() throws Exception{
    super.clean();
    Fs.rmTree(notes);
    Fs.rmTree(tally);
  }
}

package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;

import tools.Fs;

/// A code project edits an editable data project through a link typed in the write field of its Links section, and the manager remembers it; the same type typed also in the read field is refused with a note, since a type edited is also read; emptying the write field removes the link.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, nothing is registered for .fearless, and the folder ledger beside the manager holds only the folders vault and teller, each holding only its marker and one source file.
/// Action 1: run the launcher on vault: the manager window opens showing vault, an idle project.
/// Action 2: click the empty space below the tiles, and move the divider between tiles and panel as far left as it goes with the keyboard.
/// Action 3: press Become editable data: the manager remembers vault as an editable data project.
/// Action 4: run the launcher on teller: it ends at once, the manager remembers teller as an idle project besides vault, and the window shows teller.
/// Action 5: press Become code: the manager remembers teller as a code project.
/// Action 6: open the Links section: it opens below the mains.
/// Action 7: click the write field of vault, type Coin and press Enter: the manager remembers that teller edits the type Coin of vault.
/// Action 8: click the read field of vault, type Coin and press Enter: a note says exactly why Coin is not also read, and the manager still remembers only that teller edits Coin of vault.
/// Action 9: press OK: the note goes away.
/// Action 10: click the read field of vault, empty it and press Enter: the manager still remembers only that teller edits Coin of vault.
/// Action 11: click the write field of vault, empty it and press Enter: the manager remembers teller as a code project with no link, exactly as after action 5, and the Links section looks exactly as right after action 6.
/// Action 12: end the manager.
final class LinkWriteTest extends ManagerTest{
  static final Path ledger= data.resolveSibling("ledger");
  static final Path vault= ledger.resolve("vault");
  static final Path teller= ledger.resolve("teller");
  final At managerShown= new At("managerShown",on("ubuntu-gnome",3000));
  final Click focusTiles= new Click("focusTiles",on("ubuntu-gnome",200,1500));
  final Click becomeEditableData= new Click("becomeEditableData",on("ubuntu-gnome",272,140));
  final Area head= new Area("head",on("ubuntu-gnome",80,94,1320,78));
  final At tellerShown= new At("tellerShown",on("ubuntu-gnome",500));
  final Click becomeCode= new Click("becomeCode",on("ubuntu-gnome",400,140));
  final Area below= new Area("below",on("ubuntu-gnome",80,180,1320,100));
  final Click openLinks= new Click("openLinks",on("ubuntu-gnome",130,191));
  final Click writeField= new Click("writeField",on("ubuntu-gnome",442,233));
  final Click readField= new Click("readField",on("ubuntu-gnome",240,233));
  final At noteShown= new At("noteShown",on("ubuntu-gnome",1000));
  final Click ok= new Click("ok",on("ubuntu-gnome",1952,1183));
  final Click readFieldAgain= new Click("readFieldAgain",on("ubuntu-gnome",240,233));
  final Click writeFieldAgain= new Click("writeFieldAgain",on("ubuntu-gnome",442,233));
  @Override protected void walk() throws Exception{
    clean();
    Fs.writeUtf8(vault.resolve("vault.fearless"),"\n");
    Fs.writeUtf8(vault.resolve("_vault").resolve("_rank_app.fear"),"Coin:{}\n");
    Fs.writeUtf8(teller.resolve("teller.fearless"),"\n");
    Fs.writeUtf8(teller.resolve("_teller").resolve("_rank_app.fear"),"use base.Main as Main;\n\nHello:Main{s->base.Debug#(\"teller\")}\n");
    launch(vault.toString());
    managerShown.go();
    focusTiles.go();
    pilot.chord(KeyEvent.VK_F8);
    pilot.chord(KeyEvent.VK_HOME);
    becomeEditableData.go();
    until(()->Fs.readUtf8(info).contains("\"data:readWrite\""));
    look();
    var top= head.aim();
    var before= pixels(top);
    var run= new ProcessBuilder(launcher.toString(),teller.toString()).start();
    until(()->!run.isAlive());
    assertEquals(0,run.exitValue());
    until(()->!Arrays.equals(before,pixels(top)));
    tellerShown.go();
    becomeCode.go();
    until(()->Fs.readUtf8(info).contains("\"code\""));
    var unlinked= registry("");
    assertEquals(unlinked,Fs.readUtf8(info));
    look();
    var at= below.aim();
    var closed= pixels(at);
    openLinks.go();
    look();
    until(()->!Arrays.equals(closed,pixels(at)));
    var opened= pixels(at);
    writeField.go();
    type();
    until(()->Fs.readUtf8(info).contains("\"edits\""));
    var edits= registry("""
      ,
          "edits": {
            "vault": ["Coin"]
          }""");
    assertEquals(edits,Fs.readUtf8(info));
    readField.go();
    type();
    until(()->Files.exists(notes) && !Fs.readUtf8(notes).isEmpty());
    noteShown.go();
    assertEquals("""
      In file: %s

      013|       "vault": ["Coin"]
         |                 ^^^^^^

      While inspecting the file
      "Coin" is in both "reads"."vault" and "edits"."vault": a type name in "edits" also reads, so it is not repeated in "reads"; a type name in "reads" only reads.
      """.formatted(info),Fs.readUtf8(notes));
    assertEquals(edits,Fs.readUtf8(info));
    ok.go();
    readFieldAgain.go();
    empty();
    assertEquals(edits,Fs.readUtf8(info));
    writeFieldAgain.go();
    empty();
    until(()->!Fs.readUtf8(info).contains("\"edits\""));
    assertEquals(unlinked,Fs.readUtf8(info));
    look();
    until(()->Arrays.equals(opened,pixels(at)));
    stopManagers();
  }
  private void type(){
    pilot.chord(KeyEvent.VK_SHIFT,KeyEvent.VK_C);
    IntStream.of(KeyEvent.VK_O,KeyEvent.VK_I,KeyEvent.VK_N,KeyEvent.VK_ENTER).forEach(pilot::chord);
  }
  private void empty(){
    pilot.chord(KeyEvent.VK_CONTROL,KeyEvent.VK_A);
    pilot.chord(KeyEvent.VK_BACK_SPACE);
    pilot.chord(KeyEvent.VK_ENTER);
  }
  private static String registry(String links){
    return """
      {
        "vault": {
          "path": "Str:%s",
          "kind": "data:readWrite"
        },
        "teller": {
          "path": "Str:%s",
          "kind": "code"%s
        }
      }
      """.formatted(vault,teller,links);
  }
  @Override @AfterEach void clean() throws Exception{
    super.clean();
    Fs.rmTree(ledger);
  }
}

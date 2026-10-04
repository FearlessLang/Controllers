package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;

import tools.Fs;

/// A code project reads a data project through a link typed in its Links section, and the manager remembers it; once the data project goes back to idle, the link is broken and the code project refuses to compile, saying why.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, nothing is registered for .fearless, and the folder links beside the manager holds only the folders store and reader, each holding only its marker and one source file.
/// Action 1: run the launcher on store: the manager window opens showing store, an idle project.
/// Action 2: click the empty space below the tiles, and move the divider between tiles and panel as far left as it goes with the keyboard.
/// Action 3: press Become data: the manager remembers store as a read only data project.
/// Action 4: run the launcher on reader: it ends at once, the manager remembers reader as an idle project besides store, and the window shows reader.
/// Action 5: press Become code: the manager remembers reader as a code project.
/// Action 6: open the Links section: it opens below the mains.
/// Action 7: click the read field of store, type Stock and press Enter: the manager remembers that reader reads the type Stock of store.
/// Action 8: run the launcher on store: it ends at once and the window shows store again.
/// Action 9: press Back to idle: the manager remembers store as idle, and still remembers that reader reads Stock of store.
/// Action 10: run the launcher on reader: it ends at once and the window shows reader again.
/// Action 11: press Compile: the Output says the link of reader to store is broken because store is idle, and nothing is compiled.
/// Action 12: end the manager.
final class LinkReadTest extends ManagerTest{
  static final Path links= data.resolveSibling("links");
  static final Path store= links.resolve("store");
  static final Path reader= links.resolve("reader");
  static final Path console= data.resolve("eclipse").resolve("reader").resolve("console.txt");
  final At managerShown= new At("managerShown",on("ubuntu-gnome",3000));
  final Click focusTiles= new Click("focusTiles",on("ubuntu-gnome",200,1500));
  final Click becomeData= new Click("becomeData",on("ubuntu-gnome",141,140));
  final Area head= new Area("head",on("ubuntu-gnome",80,94,1320,78));
  final At readerShown= new At("readerShown",on("ubuntu-gnome",500));
  final Click becomeCode= new Click("becomeCode",on("ubuntu-gnome",400,140));
  final Area below= new Area("below",on("ubuntu-gnome",80,180,1320,100));
  final Click openLinks= new Click("openLinks",on("ubuntu-gnome",130,191));
  final Click readField= new Click("readField",on("ubuntu-gnome",240,233));
  final At storeShown= new At("storeShown",on("ubuntu-gnome",500));
  final Click backToIdle= new Click("backToIdle",on("ubuntu-gnome",138,140));
  final At readerShownAgain= new At("readerShownAgain",on("ubuntu-gnome",500));
  final Click compile= new Click("compile",on("ubuntu-gnome",164,111));
  @Override protected void walk() throws Exception{
    clean();
    Fs.writeUtf8(store.resolve("store.fearless"),"\n");
    Fs.writeUtf8(store.resolve("_store").resolve("_rank_app.fear"),"Stock:{}\n");
    Fs.writeUtf8(reader.resolve("reader.fearless"),"\n");
    Fs.writeUtf8(reader.resolve("_reader").resolve("_rank_app.fear"),"use base.Main as Main;\n\nHello:Main{s->base.Debug#(\"reader\")}\n");
    launch(store.toString());
    managerShown.go();
    focusTiles.go();
    pilot.chord(KeyEvent.VK_F8);
    pilot.chord(KeyEvent.VK_HOME);
    becomeData.go();
    until(()->Fs.readUtf8(info).contains("\"data:readOnly\""));
    var top= head.aim();
    handOver(reader,readerShown,top);
    becomeCode.go();
    until(()->Fs.readUtf8(info).contains("\"code\""));
    look();
    var at= below.aim();
    var closed= pixels(at);
    openLinks.go();
    look();
    until(()->!Arrays.equals(closed,pixels(at)));
    readField.go();
    pilot.chord(KeyEvent.VK_SHIFT,KeyEvent.VK_S);
    for (var k: List.of(KeyEvent.VK_T,KeyEvent.VK_O,KeyEvent.VK_C,KeyEvent.VK_K,KeyEvent.VK_ENTER)){ pilot.chord(k); }
    until(()->Fs.readUtf8(info).contains("\"reads\""));
    assertEquals(registry("data:readOnly"),Fs.readUtf8(info));
    handOver(store,storeShown,top);
    backToIdle.go();
    until(()->!Fs.readUtf8(info).contains("\"data:readOnly\""));
    assertEquals(registry("idle"),Fs.readUtf8(info));
    handOver(reader,readerShownAgain,top);
    compile.go();
    until(()->!Fs.readUtf8(console).isEmpty());
    assertEquals("""
      "reads" refers to "store", but the kind of "store" is "idle"; "reads" accepts only "data:readOnly" or "data:readWrite".
      """,Fs.readUtf8(console));
    assertFalse(Files.exists(reader.resolve(".fearless_out")));
    stopManagers();
  }
  private void handOver(Path folder, At shown, int[] at) throws Exception{
    look();
    var before= pixels(at);
    var run= new ProcessBuilder(launcher.toString(),folder.toString()).start();
    until(()->!run.isAlive());
    assertEquals(0,run.exitValue());
    until(()->!Arrays.equals(before,pixels(at)));
    shown.go();
  }
  private static String registry(String storeKind){
    return """
      {
        "store": {
          "path": "Str:%s",
          "kind": "%s"
        },
        "reader": {
          "path": "Str:%s",
          "kind": "code",
          "reads": {
            "store": ["Stock"]
          }
        }
      }
      """.formatted(store,storeKind,reader);
  }
  @Override @AfterEach void clean() throws Exception{
    super.clean();
    Fs.rmTree(links);
  }
}

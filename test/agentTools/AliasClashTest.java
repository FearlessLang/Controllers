package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;

import tools.Fs;

/// A folder whose marker names it as a project already registered is registered under a free name: the launcher run on it ends at once, the manager renames the marker in that folder to the new name and shows a note saying so, and the project then works under that name.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, nothing is registered for .fearless, and the folder twins beside the manager holds only the folders a/twin and b/twin, each holding only the marker twin.fearless and one source file.
/// Action 1: run the launcher on a/twin: the manager window opens showing twin, and the manager remembers a/twin as the idle project twin.
/// Action 2: click the empty space below the tiles, and move the divider between tiles and panel as far left as it goes with the keyboard.
/// Action 3: run the launcher on b/twin: it ends at once, a note shows saying b/twin is kept as twin2, the manager remembers b/twin as the idle project twin2 besides twin, and the marker of b/twin is now twin2.fearless while a/twin is untouched.
/// Action 4: press OK: the note goes away.
/// Action 5: press Check: the Output of twin2 says no problem was found, and the Output of twin stays empty.
/// Action 6: end the manager.
final class AliasClashTest extends ManagerTest{
  static final Path twins= data.resolveSibling("twins");
  static final Path first= twins.resolve("a").resolve("twin");
  static final Path second= twins.resolve("b").resolve("twin");
  final At managerShown= new At("managerShown",linux(3000));
  final Click focusTiles= new Click("focusTiles",linux(200,1500));
  final Area window= new Area("window",linux(68,32,3772,2098));
  final At noteShown= new At("noteShown",linux(1000));
  final Click ok= new Click("ok",linux(1952,1213));
  final Click check= new Click("check",linux(164,111));
  @Override protected void walk() throws Exception{
    clean();
    for (var f: List.of(first,second)){
      Fs.writeUtf8(f.resolve("twin.fearless"),"\n");
      Fs.writeUtf8(f.resolve("_twin").resolve("_rank_app.fear"),"use base.Main as Main;\n\nHello:Main{s->base.Debug#(\"twin\")}\n");
    }
    launch(first.toString());
    managerShown.go();
    assertEquals("""
      {
        "twin": {
          "path": "Str:%s",
          "kind": "idle"
        }
      }
      """.formatted(first),Fs.readUtf8(info));
    focusTiles.go();
    pilot.chord(KeyEvent.VK_F8);
    pilot.chord(KeyEvent.VK_HOME);
    look();
    var at= window.aim();
    var before= pixels(at);
    var run= new ProcessBuilder(launcher.toString(),second.toString()).start();
    until(()->!run.isAlive());
    assertEquals(0,run.exitValue());
    until(()->!Arrays.equals(before,pixels(at)));
    noteShown.go();
    assertEquals("""
      Fearless keeps track of this project folder as "twin2", not as "twin".

      The manager registered:
        %s
      A project name uses only lowercase letters, digits and underscores,
      starts with a letter or an underscore, is not a name the file system
      reserves ("con", "prn", "aux", "nul", "com1" to "com9", "lpt1" to "lpt9"),
      and is not the name of another project Fearless keeps track of.

      The marker file "twin2.fearless" in that folder holds the name: rename it to change the name.
      """.formatted(second),Fs.readUtf8(data.resolve("eclipse").resolve("console.txt")));
    assertEquals("""
      {
        "twin": {
          "path": "Str:%s",
          "kind": "idle"
        },
        "twin2": {
          "path": "Str:%s",
          "kind": "idle"
        }
      }
      """.formatted(first,second),Fs.readUtf8(info));
    assertEquals(List.of("a/twin/_twin/_rank_app.fear","a/twin/twin.fearless","b/twin/_twin/_rank_app.fear","b/twin/twin2.fearless"),
      Fs.walk(twins,s->s.filter(Files::isRegularFile).map(p->twins.relativize(p).toString()).sorted().toList()));
    assertEquals("\n",Fs.readUtf8(second.resolve("twin2.fearless")));
    var shown= pixels(at);
    ok.go();
    look();
    until(()->!Arrays.equals(shown,pixels(at)));
    check.go();
    var console= data.resolve("eclipse").resolve("twin2").resolve("console.txt");
    until(()->!Fs.readUtf8(console).isEmpty());
    assertEquals("--- ok: no problem found ---\n",Fs.readUtf8(console));
    assertEquals("",Fs.readUtf8(data.resolve("eclipse").resolve("twin").resolve("console.txt")));
    stopManagers();
  }
  @Override @AfterEach void clean() throws Exception{
    super.clean();
    Fs.rmTree(twins);
  }
}

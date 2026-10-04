package agentTools;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;

import tools.Fs;

/// Ordering the tiles by Compiled puts the project compiled last first, while projects never compiled keep the order they were registered in; a compile moves its project to the front at once.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, nothing is registered for .fearless, and the folder order beside the manager holds only the empty folders zeta and alpha.
/// Action 1: run the launcher on zeta: the manager window opens with one tile, zeta selected.
/// Action 2: run the launcher on alpha: it ends at once, a second tile appears, and the manager remembers zeta then alpha, both code projects.
/// Action 3: choose Compiled in Order by: the two tiles swap places, each looking exactly as the other did in its place.
/// Action 4: move the divider between tiles and panel as far left as it goes with the keyboard, and press Compile: the manager knows alpha.Hello as the main of alpha, and remembers a compile of alpha and none of zeta.
/// Action 5: move the divider as far right as it goes with the keyboard: the tiles swap back, zeta looking exactly as it did in second place, alpha first and looking compiled.
/// Action 6: end the manager.
final class SortByCompiledTest extends ManagerTest{
  static final Path order= data.resolveSibling("order");
  static final Path zeta= order.resolve("zeta");
  static final Path alpha= order.resolve("alpha");
  final At managerShown= new At("managerShown",linux(3000),windows(3000));
  final Area firstTile= new Area("firstTile",linux(74,149,128,88),windows(7,101,127,88));
  final Area secondTile= new Area("secondTile",linux(202,149,128,88),windows(135,101,127,88));
  final Click orderBy= new Click("orderBy",linux(165,130),windows(108,82));
  final Click compiled= new Click("compiled",linux(150,191),windows(97,141));
  final Click compile= new Click("compile",linux(164,111),windows(102,67));
  @Override protected void walk() throws Exception{
    clean();
    Fs.ensureDir(zeta);
    Fs.ensureDir(alpha);
    launch(zeta.toString());
    managerShown.go();
    look();
    var first= firstTile.aim();
    var second= secondTile.aim();
    var empty= pixels(second);
    var run= new ProcessBuilder(launcher.toString(),alpha.toString()).start();
    until(()->!run.isAlive());
    assertEquals(0,run.exitValue());
    look();
    until(()->!Arrays.equals(empty,pixels(second)));
    assertEquals("""
      {
        "zeta": {
          "path": "Str:%s",
          "kind": "code"
        },
        "alpha": {
          "path": "Str:%s",
          "kind": "code"
        }
      }
      """.formatted(slashed(zeta),slashed(alpha)),Fs.readUtf8(info));
    var alphaFirst= pixels(first);
    var zetaSecond= pixels(second);
    orderBy.go();
    compiled.go();
    look();
    until(()->Arrays.equals(zetaSecond,pixels(first)));
    assertArrayEquals(alphaFirst,pixels(second));
    pilot.chord(KeyEvent.VK_F8);
    pilot.chord(KeyEvent.VK_HOME);
    var before= System.currentTimeMillis();
    compile.go();
    until(()->Fs.readUtf8(state).contains("\"alpha.Hello\""));
    var activity= Fs.readUtf8(data.resolve("activity.txt")).lines().toList();
    assertEquals("-1 -1 Str:"+zeta,activity.get(0));
    var stamp= Long.parseLong(activity.get(1).split(" ")[0]);
    assertEquals(stamp+" -1 Str:"+alpha,activity.get(1));
    assert stamp>=before && stamp<=System.currentTimeMillis();
    pilot.chord(KeyEvent.VK_F8);
    pilot.chord(KeyEvent.VK_END);
    look();
    until(()->Arrays.equals(zetaSecond,pixels(second)));
    assertFalse(Arrays.equals(alphaFirst,pixels(first)));
    stopManagers();
  }
  @Override @AfterEach void clean() throws Exception{
    super.clean();
    Fs.rmTree(order);
  }
}

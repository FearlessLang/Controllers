package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.opentest4j.AssertionFailedError;

import tools.Fs;
import utils.Err;

/// A ticked main that the source no longer declares stays selected until the next compile, which drops it from the selected mains without a word while the other ticked main stays ticked, and Run selected then runs only that one.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, nothing is registered for .fearless, and the folder menu beside the manager holds only its marker and one source file with three mains, First, Second and Third, each printing its own name.
/// Action 1: run the launcher on menu: the manager window opens showing menu, an idle project.
/// Action 2: click the empty space below the tiles, and move the divider between tiles and panel as far left as it goes with the keyboard.
/// Action 3: press Become code: the manager remembers menu as a code project.
/// Action 4: press Compile: the manager knows the three mains of menu, and the panel shows them, none ticked.
/// Action 5: tick menu.Second: the manager remembers it as the one selected main.
/// Action 6: tick menu.Third: the manager remembers menu.Second then menu.Third as the selected mains.
/// Action 7: save the source file without Third: the manager by itself knows no main of menu any more, still remembers menu.Second and menu.Third as selected, and the Output says nothing new.
/// Action 8: press Compile: the Output says a second compile is done and nothing else, the manager knows menu.First and menu.Second and remembers only menu.Second as selected, and the panel shows those two mains exactly as the first two were shown before, menu.Second ticked.
/// Action 9: press Run selected: the Output shows menu.Second running, printing its name and exiting with 0, and nothing about menu.Third, and the manager counts one run, of menu.Second, exited with 0.
/// Action 10: end the manager.
final class StaleMainTest extends ManagerTest{
  static{ Err.setUp(AssertionFailedError.class,Assertions::assertEquals,Assertions::assertTrue); }
  static final Path menu= data.resolveSibling("menu");
  static final Path source= menu.resolve("_menu").resolve("_rank_app.fear");
  static final Path console= data.resolve("eclipse").resolve("menu").resolve("console.txt");
  static final String mains= "use base.Main as Main;\n\nFirst: Main{sys -> sys.out.println \"First\"}\nSecond: Main{sys -> sys.out.println \"Second\"}\n";
  static final String compiled= "--- compiling menu ---\n--- compile done ---\n";
  static final String registry= """
    {
      "menu": {
        "path": "Str:%s",
        "kind": "code",
        "mains": [%s]
      }
    }
    """;
  final At managerShown= new At("managerShown",on("ubuntu-gnome",3000));
  final Click focusTiles= new Click("focusTiles",on("ubuntu-gnome",200,1500));
  final Click becomeCode= new Click("becomeCode",on("ubuntu-gnome",400,139));
  final Click compile= new Click("compile",on("ubuntu-gnome",164,111));
  final At mainsShown= new At("mainsShown",on("ubuntu-gnome",1000));
  final Click tickSecond= new Click("tickSecond",on("ubuntu-gnome",99,215));
  final Click tickThird= new Click("tickThird",on("ubuntu-gnome",99,240));
  final Area rows= new Area("rows",on("ubuntu-gnome",90,180,210,50));
  final Click compileEdited= new Click("compileEdited",on("ubuntu-gnome",164,111));
  final Click runSelected= new Click("runSelected",on("ubuntu-gnome",164,111));
  @Override protected void walk() throws Exception{
    clean();
    Fs.writeUtf8(menu.resolve("menu.fearless"),"\n");
    Fs.writeUtf8(source,mains+"Third: Main{sys -> sys.out.println \"Third\"}\n");
    launch(menu.toString());
    managerShown.go();
    focusTiles.go();
    pilot.chord(KeyEvent.VK_F8);
    pilot.chord(KeyEvent.VK_HOME);
    becomeCode.go();
    until(()->Fs.readUtf8(info).contains("\"code\""));
    compile.go();
    until(()->Fs.readUtf8(state).contains("menu.Third"));
    mainsShown.go();
    tickSecond.go();
    until(()->Fs.readUtf8(info).contains("menu.Second"));
    assertEquals(registry.formatted(menu,"\"menu.Second\""),Fs.readUtf8(info));
    tickThird.go();
    until(()->Fs.readUtf8(info).contains("menu.Third"));
    var both= registry.formatted(menu,"\"menu.Second\", \"menu.Third\"");
    assertEquals(both,Fs.readUtf8(info));
    look();
    var at= rows.aim();
    var ticked= pixels(at);
    Fs.writeUtf8(source,mains);
    until(()->!Fs.readUtf8(state).contains("menu.First"));
    assertEquals(both,Fs.readUtf8(info));
    assertEquals(compiled,Fs.readUtf8(console));
    compileEdited.go();
    until(()->Fs.readUtf8(state).contains("menu.Second"));
    assertEquals(compiled+compiled,Fs.readUtf8(console));
    assertEquals(registry.formatted(menu,"\"menu.Second\""),Fs.readUtf8(info));
    look();
    until(()->Arrays.equals(ticked,pixels(at)));
    runSelected.go();
    until(()->Fs.readUtf8(state).contains("\"exit\": \"0\""));
    assertEquals("""
      {
        "menu": {
          "folder": "Str:%s",
          "kind": "code",
          "running": "",
          "runs": "1",
          "lastRun": "menu.Second",
          "exit": "0",
          "mains": {
            "menu.First": "_menu/_rank_app.fear",
            "menu.Second": "_menu/_rank_app.fear"
          },
          "problem": {}
        }
      }
      """.formatted(menu),Fs.readUtf8(state));
    Err.strCmp(compiled+compiled+"""
      --- running menu.Second ---
      Second
      --- menu.Second exited with 0 after [###]s ---
      """,Fs.readUtf8(console));
    stopManagers();
  }
  @Override @AfterEach void clean() throws Exception{
    super.clean();
    Fs.rmTree(menu);
  }
}

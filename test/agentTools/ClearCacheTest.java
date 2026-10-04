package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import tools.Fs;

/// Clearing the cache of a compiled project deletes what the compile wrote into its folder, and the manager shows the project needing a compile again, as it did before the compile, while the Output of the compile stays.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, helloWorld was never compiled, and nothing is registered for .fearless.
/// Action 1: run the launcher on helloWorld: the manager window opens showing helloWorld selected, an idle project.
/// Action 2: click the empty space below the tiles, and move the divider between tiles and panel as far left as it goes with the keyboard.
/// Action 3: press Become code: the manager remembers helloWorld as a code project.
/// Action 4: press Compile: the Output says the compile is done, helloWorld holds the compiled cache, the manager knows the mains of helloWorld, and the panel shows them.
/// Action 5: choose Clear cache in its Project menu: the compiled cache is gone from helloWorld, the manager knows no mains of helloWorld, the top of the panel offers Compile and says helloWorld needs compiling exactly as before the compile, and the Output still says the compile is done.
/// Action 6: end the manager.
final class ClearCacheTest extends ManagerTest{
  static final Path console= data.resolve("eclipse").resolve("hello_world").resolve("console.txt");
  static final Path cache= project.resolve(".fearless_out");
  final At managerShown= new At("managerShown",on("ubuntu-gnome",3000),on("omarchy-hyprland",2000),on("debian-gnome-x11",2500),on("kubuntu-plasma",2500),on("xubuntu-xfce",2500),on("debian-cinnamon",2500));
  final Click focusTiles= new Click("focusTiles",on("ubuntu-gnome",200,1500),on("omarchy-hyprland",200,450),on("debian-gnome-x11",200,810),on("kubuntu-plasma",200,769),on("xubuntu-xfce",200,791),on("debian-cinnamon",200,778));
  final Click becomeCode= new Click("becomeCode",on("ubuntu-gnome",400,139),on("omarchy-hyprland",338,89),on("debian-gnome-x11",334,140),on("kubuntu-plasma",334,99),on("xubuntu-xfce",381,119),on("debian-cinnamon",334,108));
  final At codeShown= new At("codeShown",on("ubuntu-gnome",1000),on("omarchy-hyprland",1500),on("debian-gnome-x11",1000),on("kubuntu-plasma",1000),on("xubuntu-xfce",1000),on("debian-cinnamon",1000));
  final Area head= new Area("head",on("ubuntu-gnome",80,94,1320,78),on("omarchy-hyprland",18,44,900,78),on("debian-gnome-x11",14,95,1320,78),on("kubuntu-plasma",14,54,1320,78),on("xubuntu-xfce",20,75,1320,78),on("debian-cinnamon",14,63,1320,78));
  final Click compile= new Click("compile",on("ubuntu-gnome",164,111),on("omarchy-hyprland",102,61),on("debian-gnome-x11",98,112),on("kubuntu-plasma",98,71),on("xubuntu-xfce",105,92),on("debian-cinnamon",98,80));
  final At mainsShown= new At("mainsShown",on("ubuntu-gnome",1000),on("omarchy-hyprland",2000),on("debian-gnome-x11",1500),on("kubuntu-plasma",1500),on("xubuntu-xfce",1500),on("debian-cinnamon",1500));
  final Click projectMenu= new Click("projectMenu",on("ubuntu-gnome",158,79),on("omarchy-hyprland",96,30),on("debian-gnome-x11",92,80),on("kubuntu-plasma",92,39),on("xubuntu-xfce",105,60),on("debian-cinnamon",92,47));
  final Click clearCache= new Click("clearCache",on("ubuntu-gnome",180,128),on("omarchy-hyprland",118,78),on("debian-gnome-x11",114,129),on("kubuntu-plasma",114,88),on("xubuntu-xfce",120,105),on("debian-cinnamon",114,97));
  @Override protected void walk() throws Exception{
    clean();
    launch(project.toString());
    managerShown.go();
    focusTiles.go();
    pilot.chord(KeyEvent.VK_F8);
    pilot.chord(KeyEvent.VK_HOME);
    becomeCode.go();
    until(()->Fs.readUtf8(info).contains("\"code\""));
    codeShown.go();
    look();
    var at= head.aim();
    var code= pixels(at);
    var noMains= Fs.readUtf8(state);
    compile.go();
    until(()->Fs.readUtf8(console).contains("--- compile "));
    var compiled= "--- compiling helloWorld ---\n--- compile done ---\n";
    assertEquals(compiled,Fs.readUtf8(console));
    assertTrue(Files.isDirectory(cache));
    until(()->!noMains.equals(Fs.readUtf8(state)));
    assertEquals("""
      {
        "hello_world": {
          "folder": "Str:%s",
          "kind": "code",
          "running": "",
          "runs": "0",
          "lastRun": "",
          "exit": "-1",
          "mains": {
            "hello.Hello1": "_hello/_rank_app.fear",
            "hello.Hello3": "_hello/_rank_app.fear",
            "hello.Hello4": "_hello/_rank_app.fear",
            "hello.Hello5": "_hello/_rank_app.fear",
            "hello.Hello6": "_hello/_rank_app.fear"
          },
          "problem": {}
        }
      }
      """.formatted(project),Fs.readUtf8(state));
    mainsShown.go();
    look();
    assertFalse(Arrays.equals(code,pixels(at)));
    projectMenu.go();
    clearCache.go();
    until(()->noMains.equals(Fs.readUtf8(state)));
    assertFalse(Files.exists(cache));
    look();
    until(()->Arrays.equals(code,pixels(at)));
    assertEquals(compiled,Fs.readUtf8(console));
    stopManagers();
  }
}

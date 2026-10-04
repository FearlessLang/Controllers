package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;

import tools.Fs;

/// A compile that fails says why in the Output, tells the manager the file and line of the problem, and leaves the panel saying the project is invalid; once the source is fixed, Compile succeeds and the problem is gone.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, nothing is registered for .fearless, and the folder broken beside the manager holds only its marker and one source file whose main uses a type that does not exist.
/// Action 1: run the launcher on broken: the manager window opens showing broken, an idle project.
/// Action 2: click the empty space below the tiles, and move the divider between tiles and panel as far left as it goes with the keyboard.
/// Action 3: press Become code: the manager remembers broken as a code project.
/// Action 4: press Compile: the Output says the compile failed and why, the manager knows the file and line of the problem and no main, and the mains row of the panel changes.
/// Action 5: replace the source file with one whose main prints a text.
/// Action 6: press Compile: the Output says the compile is done, the manager knows the main of broken and no problem, and the mains row of the panel changes again.
/// Action 7: end the manager.
final class CompileFailsTest extends ManagerTest{
  static final Path broken= data.resolveSibling("broken");
  static final Path source= broken.resolve("_broken").resolve("_rank_app.fear");
  static final Path console= data.resolve("eclipse").resolve("broken").resolve("console.txt");
  final At managerShown= new At("managerShown",on("ubuntu-gnome",3000),on("debian-mate",2500),on("void-i3",3000),on("omarchy-hyprland",2000),on("debian-gnome-x11",2500),on("kubuntu-plasma",2500),on("xubuntu-xfce",2500),on("debian-cinnamon",2500));
  final Click focusTiles= new Click("focusTiles",on("ubuntu-gnome",200,1500),on("debian-mate",200,800),on("void-i3",200,759),on("omarchy-hyprland",200,450),on("debian-gnome-x11",200,810),on("kubuntu-plasma",200,769),on("xubuntu-xfce",200,791),on("debian-cinnamon",200,778));
  final Click becomeCode= new Click("becomeCode",on("ubuntu-gnome",400,139),on("debian-mate",334,130),on("void-i3",334,89),on("omarchy-hyprland",338,89),on("debian-gnome-x11",334,140),on("kubuntu-plasma",334,99),on("xubuntu-xfce",381,119),on("debian-cinnamon",334,108));
  final At codeShown= new At("codeShown",on("ubuntu-gnome",1000),on("debian-mate",1000),on("void-i3",1500),on("omarchy-hyprland",1500),on("debian-gnome-x11",1000),on("kubuntu-plasma",1000),on("xubuntu-xfce",1000),on("debian-cinnamon",1000));
  final Area rows= new Area("rows",on("ubuntu-gnome",80,128,1320,45),on("debian-mate",14,119,1320,45),on("void-i3",14,78,1320,45),on("omarchy-hyprland",18,78,900,45),on("debian-gnome-x11",14,129,1320,45),on("kubuntu-plasma",14,88,1320,45),on("xubuntu-xfce",20,109,1320,45),on("debian-cinnamon",14,97,1320,45));
  final Click compile= new Click("compile",on("ubuntu-gnome",164,111),on("debian-mate",98,102),on("void-i3",98,61),on("omarchy-hyprland",102,61),on("debian-gnome-x11",98,112),on("kubuntu-plasma",98,71),on("xubuntu-xfce",105,92),on("debian-cinnamon",98,80));
  final Click compileFixed= new Click("compileFixed",on("ubuntu-gnome",164,111),on("debian-mate",98,102),on("void-i3",98,61),on("omarchy-hyprland",102,61),on("debian-gnome-x11",98,112),on("kubuntu-plasma",98,71),on("xubuntu-xfce",105,92),on("debian-cinnamon",98,80));
  final At mainShown= new At("mainShown",on("ubuntu-gnome",1000),on("debian-mate",1500),on("void-i3",2000),on("omarchy-hyprland",2000),on("debian-gnome-x11",1500),on("kubuntu-plasma",1500),on("xubuntu-xfce",1500),on("debian-cinnamon",1500));
  @Override protected void walk() throws Exception{
    clean();
    Fs.writeUtf8(broken.resolve("broken.fearless"),"\n");
    Fs.writeUtf8(source,"use base.Main as Main;\n\nHello:Main{s->Nope.nope}\n");
    launch(broken.toString());
    managerShown.go();
    focusTiles.go();
    pilot.chord(KeyEvent.VK_F8);
    pilot.chord(KeyEvent.VK_HOME);
    becomeCode.go();
    until(()->Fs.readUtf8(info).contains("\"code\""));
    codeShown.go();
    look();
    var at= rows.aim();
    var code= pixels(at);
    compile.go();
    until(()->Fs.readUtf8(state).contains("\"line\""));
    var failed= """
      --- compiling broken ---
      In file: fear:/_broken/_rank_app.fear

      003| Hello:Main{s->Nope.nope}
         |               ^^^^^

      While inspecting a type name
      Type "Nope" is not declared in package "broken" and is not made visible via "use".
      In scope: "Hello", "Main".
      Error 7 WellFormedness
      --- compile failed with 1 ---
      """;
    assertEquals(failed,Fs.readUtf8(console));
    assertEquals("""
      {
        "broken": {
          "folder": "Str:%s",
          "kind": "code",
          "running": "",
          "runs": "0",
          "lastRun": "",
          "exit": "-1",
          "mains": {},
          "problem": {
            "file": "_broken/_rank_app.fear",
            "line": "003",
            "message": "Str:In file: fear:/_broken/_rank_app.fear\\n\\n003| Hello:Main{s->Nope.nope}\\n   |               ^^^^^\\n\\nWhile inspecting a type name\\nType \\"Nope\\" is not declared in package \\"broken\\" and is not made visible via \\"use\\".\\nIn scope: \\"Hello\\", \\"Main\\".\\nError 7 WellFormedness\\n"
          }
        }
      }
      """.formatted(broken),Fs.readUtf8(state));
    look();
    until(()->!Arrays.equals(code,pixels(at)));
    var invalid= pixels(at);
    Fs.writeUtf8(source,"use base.Main as Main;\n\nHello:Main{s->base.Debug#(\"fixed\")}\n");
    compileFixed.go();
    until(()->Fs.readUtf8(state).contains("broken.Hello"));
    assertEquals(failed+"--- compiling broken ---\n--- compile done ---\n",Fs.readUtf8(console));
    assertEquals("""
      {
        "broken": {
          "folder": "Str:%s",
          "kind": "code",
          "running": "",
          "runs": "0",
          "lastRun": "",
          "exit": "-1",
          "mains": {
            "broken.Hello": "_broken/_rank_app.fear"
          },
          "problem": {}
        }
      }
      """.formatted(broken),Fs.readUtf8(state));
    mainShown.go();
    look();
    assertFalse(Arrays.equals(code,pixels(at)));
    assertFalse(Arrays.equals(invalid,pixels(at)));
    stopManagers();
  }
  @Override @AfterEach void clean() throws Exception{
    super.clean();
    Fs.rmTree(broken);
  }
}

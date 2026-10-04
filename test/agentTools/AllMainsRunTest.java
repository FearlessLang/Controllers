package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.event.KeyEvent;
import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.opentest4j.AssertionFailedError;

import tools.Fs;
import utils.Err;

/// All ticks every main of a compiled project, and Run selected runs them all, one after the other in the order they are listed, a main that fails showing its error and its exit code in the Output like any other; None then unticks them all and leaves nothing to run.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, helloWorld was never compiled, and nothing is registered for .fearless.
/// Action 1: run the launcher on helloWorld: the manager window opens showing helloWorld selected, an idle project.
/// Action 2: click the empty space below the tiles, and move the divider between tiles and panel as far left as it goes with the keyboard.
/// Action 3: press Become code: the manager remembers helloWorld as a code project.
/// Action 4: press Compile: the manager knows the five mains of helloWorld, and the panel shows them, none ticked, with Run selected disabled.
/// Action 5: press All: the manager remembers all five mains as selected, in the order they are listed, and the panel shows them ticked.
/// Action 6: press Run selected: the Output shows the five mains running in that order, each printing its line and exiting with 0, except hello.Hello6, which prints its error and the lines where it was raised, then its exit code; the manager counts five runs, the last of hello.Hello6.
/// Action 7: press None: the manager remembers helloWorld exactly as before All, with no selected main, and the mains and Run selected look exactly as right after the compile.
/// Action 8: end the manager.
final class AllMainsRunTest extends ManagerTest{
  static{ Err.setUp(AssertionFailedError.class,Assertions::assertEquals,Assertions::assertTrue); }
  final At managerShown= new At("managerShown",on("ubuntu-gnome",3000),on("debian-mate",2500),on("void-i3",3000),on("omarchy-hyprland",2000),on("debian-gnome-x11",2500),on("kubuntu-plasma",2500),on("xubuntu-xfce",2500),on("debian-cinnamon",2500));
  final Click focusTiles= new Click("focusTiles",on("ubuntu-gnome",200,1500),on("debian-mate",200,800),on("void-i3",200,759),on("omarchy-hyprland",200,450),on("debian-gnome-x11",200,810),on("kubuntu-plasma",200,769),on("xubuntu-xfce",200,791),on("debian-cinnamon",200,778));
  final Click becomeCode= new Click("becomeCode",on("ubuntu-gnome",400,139),on("debian-mate",334,130),on("void-i3",334,89),on("omarchy-hyprland",338,89),on("debian-gnome-x11",334,140),on("kubuntu-plasma",334,99),on("xubuntu-xfce",381,119),on("debian-cinnamon",334,108));
  final At codeShown= new At("codeShown",on("ubuntu-gnome",1000),on("debian-mate",1000),on("void-i3",1500),on("omarchy-hyprland",1500),on("debian-gnome-x11",1000),on("kubuntu-plasma",1000),on("xubuntu-xfce",1000),on("debian-cinnamon",1000));
  final Click compile= new Click("compile",on("ubuntu-gnome",164,111),on("debian-mate",98,102),on("void-i3",98,61),on("omarchy-hyprland",102,61),on("debian-gnome-x11",98,112),on("kubuntu-plasma",98,71),on("xubuntu-xfce",105,92),on("debian-cinnamon",98,80));
  final At mainsShown= new At("mainsShown",on("ubuntu-gnome",1000),on("debian-mate",1500),on("void-i3",2000),on("omarchy-hyprland",2000),on("debian-gnome-x11",1500),on("kubuntu-plasma",1500),on("xubuntu-xfce",1500),on("debian-cinnamon",1500));
  final Area ticks= new Area("ticks",on("ubuntu-gnome",90,180,210,70),on("debian-mate",24,171,210,70),on("void-i3",24,130,210,70),on("omarchy-hyprland",28,130,210,70),on("debian-gnome-x11",24,181,210,70),on("kubuntu-plasma",24,140,210,70),on("xubuntu-xfce",24,155,210,70),on("debian-cinnamon",24,149,210,70));
  final Area runButton= new Area("runButton",on("ubuntu-gnome",134,98,114,30),on("debian-mate",68,89,114,30),on("void-i3",68,48,114,30),on("omarchy-hyprland",72,48,114,30),on("debian-gnome-x11",68,99,114,30),on("kubuntu-plasma",68,58,114,30),on("xubuntu-xfce",68,82,120,20),on("debian-cinnamon",68,67,114,30));
  final Click all= new Click("all",on("ubuntu-gnome",112,165),on("debian-mate",46,156),on("void-i3",46,115),on("omarchy-hyprland",50,115),on("debian-gnome-x11",46,166),on("kubuntu-plasma",46,125),on("xubuntu-xfce",46,142),on("debian-cinnamon",46,134));
  final Click runSelected= new Click("runSelected",on("ubuntu-gnome",164,111),on("debian-mate",98,102),on("void-i3",98,61),on("omarchy-hyprland",102,61),on("debian-gnome-x11",98,112),on("kubuntu-plasma",98,71),on("xubuntu-xfce",105,92),on("debian-cinnamon",98,80));
  final Click none= new Click("none",on("ubuntu-gnome",162,165),on("debian-mate",96,156),on("void-i3",96,115),on("omarchy-hyprland",100,115),on("debian-gnome-x11",96,166),on("kubuntu-plasma",96,125),on("xubuntu-xfce",99,142),on("debian-cinnamon",96,134));
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
    var noMains= Fs.readUtf8(state);
    compile.go();
    until(()->!noMains.equals(Fs.readUtf8(state)));
    mainsShown.go();
    look();
    var boxes= ticks.aim();
    var button= runButton.aim();
    var unticked= pixels(boxes);
    var disabled= pixels(button);
    var code= Fs.readUtf8(info);
    all.go();
    until(()->Fs.readUtf8(info).contains("hello.Hello6"));
    assertEquals("""
      {
        "hello_world": {
          "path": "Str:%s",
          "kind": "code",
          "mains": ["hello.Hello1", "hello.Hello3", "hello.Hello4", "hello.Hello5", "hello.Hello6"]
        }
      }
      """.formatted(project),Fs.readUtf8(info));
    look();
    until(()->!Arrays.equals(unticked,pixels(boxes)));
    runSelected.go();
    until(()->Fs.readUtf8(state).contains("\"runs\": \"5\"") && !Fs.readUtf8(state).contains("\"exit\": \"-1\""));
    Err.strCmp("""
      {
        "hello_world": {
          "folder": "Str:%s",
          "kind": "code",
          "running": "",
          "runs": "5",
          "lastRun": "hello.Hello6",
          "exit": "[###]",
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
    Err.strCmp("""
      --- compiling helloWorld ---
      --- compile done ---
      --- running hello.Hello1 ---
      hello world 3
      --- hello.Hello1 exited with 0 after [###]s ---
      --- running hello.Hello3 ---
      [Hi]
      --- hello.Hello3 exited with 0 after [###]s ---
      --- running hello.Hello4 ---
      [1, 2, 3, 4]
      --- hello.Hello4 exited with 0 after [###]s ---
      --- running hello.Hello5 ---
      [11, 12, 13, 14]
      --- hello.Hello5 exited with 0 after [###]s ---
      --- running hello.Hello6 ---
      AAAAh
      imm Bar.bar error line: 16 in file _hello/_rank_app.fear
      imm Foo.foo error line: 15 in file _hello/_rank_app.fear
      imm Hello6.main(_) error line: 14 in file _hello/_rank_app.fear
      --- hello.Hello6 exited with [###] after [###]s ---
      """,Fs.readUtf8(data.resolve("eclipse").resolve("hello_world").resolve("console.txt")));
    none.go();
    until(()->code.equals(Fs.readUtf8(info)));
    look();
    until(()->Arrays.equals(unticked,pixels(boxes)) && Arrays.equals(disabled,pixels(button)));
    stopManagers();
  }
}

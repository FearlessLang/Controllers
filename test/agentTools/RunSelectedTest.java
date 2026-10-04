package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.event.KeyEvent;

import org.junit.jupiter.api.Assertions;
import org.opentest4j.AssertionFailedError;

import tools.Fs;
import utils.Err;

/// Ticking mains of a compiled project saves them in the order the project lists them, whatever order they were ticked in, and Run selected runs each ticked main once, in that order, one after the other, with its output in the Output.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, helloWorld was never compiled, and nothing is registered for .fearless.
/// Action 1: run the launcher on helloWorld: the manager window opens showing helloWorld selected, an idle project.
/// Action 2: click the empty space below the tiles, and move the divider between tiles and panel as far left as it goes with the keyboard.
/// Action 3: press Become code: the manager remembers helloWorld as a code project.
/// Action 4: press Compile: the manager knows the mains of helloWorld, and the panel shows them, none ticked.
/// Action 5: tick hello.Hello4: the manager remembers it as the one selected main.
/// Action 6: tick hello.Hello1: the manager remembers hello.Hello1 then hello.Hello4 as the selected mains.
/// Action 7: press Run selected: the Output shows hello.Hello1 running, printing its line and exiting with 0, then hello.Hello4 doing the same, and the manager counts two runs, the last of hello.Hello4, exited with 0.
/// Action 8: end the manager.
final class RunSelectedTest extends ManagerTest{
  static{ Err.setUp(AssertionFailedError.class,Assertions::assertEquals,Assertions::assertTrue); }
  final At managerShown= new At("managerShown",on("ubuntu-gnome",3000),on("debian-mate",2500),on("void-i3",3000),on("omarchy-hyprland",2000),on("debian-gnome-x11",2500),on("kubuntu-plasma",2500),on("xubuntu-xfce",2500),on("debian-cinnamon",2500));
  final Click focusTiles= new Click("focusTiles",on("ubuntu-gnome",200,1500),on("debian-mate",200,800),on("void-i3",200,759),on("omarchy-hyprland",200,450),on("debian-gnome-x11",200,810),on("kubuntu-plasma",200,769),on("xubuntu-xfce",200,791),on("debian-cinnamon",200,778));
  final Click becomeCode= new Click("becomeCode",on("ubuntu-gnome",400,139),on("debian-mate",334,130),on("void-i3",334,89),on("omarchy-hyprland",338,89),on("debian-gnome-x11",334,140),on("kubuntu-plasma",334,99),on("xubuntu-xfce",381,119),on("debian-cinnamon",334,108));
  final At codeShown= new At("codeShown",on("ubuntu-gnome",1000),on("debian-mate",1000),on("void-i3",1500),on("omarchy-hyprland",1500),on("debian-gnome-x11",1000),on("kubuntu-plasma",1000),on("xubuntu-xfce",1000),on("debian-cinnamon",1000));
  final Click compile= new Click("compile",on("ubuntu-gnome",164,111),on("debian-mate",98,102),on("void-i3",98,61),on("omarchy-hyprland",102,61),on("debian-gnome-x11",98,112),on("kubuntu-plasma",98,71),on("xubuntu-xfce",105,92),on("debian-cinnamon",98,80));
  final At mainsShown= new At("mainsShown",on("ubuntu-gnome",1000),on("debian-mate",1500),on("void-i3",2000),on("omarchy-hyprland",2000),on("debian-gnome-x11",1500),on("kubuntu-plasma",1500),on("xubuntu-xfce",1500),on("debian-cinnamon",1500));
  final Click tickHello4= new Click("tickHello4",on("ubuntu-gnome",99,240),on("debian-mate",33,231),on("void-i3",34,178),on("omarchy-hyprland",38,178),on("debian-gnome-x11",33,241),on("kubuntu-plasma",33,200),on("xubuntu-xfce",32,211),on("debian-cinnamon",33,209));
  final Click tickHello1= new Click("tickHello1",on("ubuntu-gnome",99,190),on("debian-mate",33,181),on("void-i3",34,132),on("omarchy-hyprland",38,132),on("debian-gnome-x11",33,191),on("kubuntu-plasma",33,150),on("xubuntu-xfce",32,165),on("debian-cinnamon",33,159));
  final Click runSelected= new Click("runSelected",on("ubuntu-gnome",164,111),on("debian-mate",98,102),on("void-i3",98,61),on("omarchy-hyprland",102,61),on("debian-gnome-x11",98,112),on("kubuntu-plasma",98,71),on("xubuntu-xfce",105,92),on("debian-cinnamon",98,80));
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
    var registry= """
      {
        "hello_world": {
          "path": "Str:%s",
          "kind": "code",
          "mains": [%s]
        }
      }
      """;
    tickHello4.go();
    until(()->Fs.readUtf8(info).contains("hello.Hello4"));
    assertEquals(registry.formatted(project,"\"hello.Hello4\""),Fs.readUtf8(info));
    tickHello1.go();
    until(()->Fs.readUtf8(info).contains("hello.Hello1"));
    assertEquals(registry.formatted(project,"\"hello.Hello1\", \"hello.Hello4\""),Fs.readUtf8(info));
    runSelected.go();
    until(()->Fs.readUtf8(state).contains("\"runs\": \"2\"") && Fs.readUtf8(state).contains("\"exit\": \"0\""));
    assertEquals("""
      {
        "hello_world": {
          "folder": "Str:%s",
          "kind": "code",
          "running": "",
          "runs": "2",
          "lastRun": "hello.Hello4",
          "exit": "0",
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
      --- running hello.Hello4 ---
      [1, 2, 3, 4]
      --- hello.Hello4 exited with 0 after [###]s ---
      """,Fs.readUtf8(data.resolve("eclipse").resolve("hello_world").resolve("console.txt")));
    stopManagers();
  }
}

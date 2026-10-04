package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.KeyEvent;
import java.util.Arrays;

import tools.Fs;
import utils.OneOr;

/// Forgetting a project while one of its programs runs ends that program: its window goes away, the manager keeps running and remembers no project, and its window is exactly as before the project was registered.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, helloWorld and testGui1 were never compiled, and nothing is registered for .fearless.
/// Action 1: run the launcher: the manager window opens with no tile.
/// Action 2: click the empty space below the tiles, and move the divider between tiles and panel as far left as it goes with the keyboard.
/// Action 3: run the launcher on testGui1: the window shows testGui1, an idle project.
/// Action 4: press Become code: the manager remembers testGui1 as a code project.
/// Action 5: press Compile: the manager knows the main of testGui1.
/// Action 6: press Run: the window of the program opens.
/// Action 7: choose Forget project in the Project menu: the program ends and its window goes away, the manager still runs and remembers no project, and its window is exactly as before testGui1 was registered.
/// Action 8: end the manager.
final class ForgetRunningProjectTest extends ManagerTest{
  final At managerShown= new At("managerShown",on("ubuntu-gnome",3000));
  final Click focusTiles= new Click("focusTiles",on("ubuntu-gnome",200,1500));
  final Area window= new Area("window",on("ubuntu-gnome",78,32,3762,2098));
  final At panelShown= new At("panelShown",on("ubuntu-gnome",300));
  final Click becomeCode= new Click("becomeCode",on("ubuntu-gnome",400,139));
  final Click compile= new Click("compile",on("ubuntu-gnome",164,111));
  final At mainShown= new At("mainShown",on("ubuntu-gnome",1000));
  final Area programWindow= new Area("programWindow",on("ubuntu-gnome",1915,1075,10,10));
  final Click run= new Click("run",on("ubuntu-gnome",164,111));
  final Click projectMenu= new Click("projectMenu",on("ubuntu-gnome",158,79));
  final Click forgetProject= new Click("forgetProject",on("ubuntu-gnome",180,237));
  @Override protected void walk() throws Exception{
    clean();
    var manager= launch();
    managerShown.go();
    focusTiles.go();
    pilot.chord(KeyEvent.VK_F8);
    pilot.chord(KeyEvent.VK_HOME);
    look();
    var all= window.aim();
    var empty= pixels(all);
    var handover= launch(gui.toString());
    until(()->!handover.isAlive());
    panelShown.go();
    becomeCode.go();
    until(()->Fs.readUtf8(info).contains("\"code\""));
    compile.go();
    until(()->Fs.readUtf8(state).contains("gui_example.Foo"));
    mainShown.go();
    look();
    var shown= programWindow.aim();
    var desk= pixels(shown);
    run.go();
    until(()->!Arrays.equals(desk,pixels(shown)));
    var program= OneOr.of("program",manager.descendants());
    projectMenu.go();
    forgetProject.go();
    until(()->!program.isAlive());
    until(()->Fs.readUtf8(state).equals("{}\n"));
    assertEquals("{}\n",Fs.readUtf8(info));
    assertTrue(manager.isAlive());
    look();
    until(()->Arrays.equals(empty,pixels(all)));
    stopManagers();
  }
}

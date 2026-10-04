package agentTools;

import java.awt.event.KeyEvent;
import java.util.Arrays;

import tools.Fs;

/// The Running menu lists a program the manager runs, and choosing it there shows the project running it; before the run and after it ends, the menu says nothing runs.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, helloWorld and testGui1 were never compiled, and nothing is registered for .fearless.
/// Action 1: run the launcher on testGui1: the manager window opens showing testGui1, an idle project.
/// Action 2: click the empty space below the tiles, and move the divider between tiles and panel as far left as it goes with the keyboard.
/// Action 3: open the Running menu: it says nothing runs.
/// Action 4: close it: the window is exactly as before.
/// Action 5: press Become code: the manager remembers testGui1 as a code project.
/// Action 6: press Compile: the manager knows the main of testGui1.
/// Action 7: press Run: the window of the program opens, and the top of the panel changes.
/// Action 8: run the launcher on helloWorld: the panel shows helloWorld, so its top changes again.
/// Action 9: open the Running menu: it opens, and it no longer looks as it did before the run.
/// Action 10: choose the program listed in it: the panel shows testGui1 again, its top exactly as it was while the program ran.
/// Action 11: press Terminate: the program ends, and the manager knows it no longer runs.
/// Action 12: open the Running menu: it looks exactly as it did before the run.
/// Action 13: end the manager.
final class RunningMenuTest extends ManagerTest{
  final At managerShown= new At("managerShown",linux(3000),windows(3000));
  final Click focusTiles= new Click("focusTiles",linux(200,1500),windows(200,400));
  final Area window= new Area("window",linux(68,32,3772,2098),windows(0,24,1280,624));
  final Click runningMenu= new Click("runningMenu",linux(218,79),windows(146,33));
  final At menuShown= new At("menuShown",linux(500),windows(500));
  final Area menu= new Area("menu",linux(187,69,137,47),windows(116,22,127,47));
  final Click becomeCode= new Click("becomeCode",linux(400,139),windows(332,94));
  final Click compile= new Click("compile",linux(164,111),windows(102,67));
  final At mainShown= new At("mainShown",linux(1000),windows(1000));
  final Area head= new Area("head",linux(80,94,1320,78),windows(22,50,600,60));
  final Area programWindow= new Area("programWindow",linux(1915,1075,10,10),windows(635,355,10,10));
  final Click run= new Click("run",linux(164,111),windows(102,67));
  final Click runningMenuAgain= new Click("runningMenuAgain",linux(218,79),windows(146,33));
  final Click chooseProgram= new Click("chooseProgram",linux(260,103),windows(188,57));
  final Click terminate= new Click("terminate",linux(164,111),windows(102,67));
  final Click runningMenuLast= new Click("runningMenuLast",linux(218,79),windows(146,33));
  @Override protected void walk() throws Exception{
    clean();
    var manager= launch(gui.toString());
    managerShown.go();
    focusTiles.go();
    pilot.chord(KeyEvent.VK_F8);
    pilot.chord(KeyEvent.VK_HOME);
    look();
    var all= window.aim();
    var before= pixels(all);
    runningMenu.go();
    menuShown.go();
    look();
    var list= menu.aim();
    var nothing= pixels(list);
    pilot.chord(KeyEvent.VK_ESCAPE);
    look();
    until(()->Arrays.equals(before,pixels(all)));
    becomeCode.go();
    until(()->Fs.readUtf8(info).contains("\"code\""));
    compile.go();
    until(()->Fs.readUtf8(state).contains("gui_example.Foo"));
    mainShown.go();
    look();
    var at= head.aim();
    var ready= pixels(at);
    var shown= programWindow.aim();
    var desk= pixels(shown);
    run.go();
    until(()->!Arrays.equals(desk,pixels(shown)));
    var program= program(manager);
    look();
    until(()->!Arrays.equals(ready,pixels(at)));
    var running= pixels(at);
    launch(project.toString());
    look();
    until(()->!Arrays.equals(running,pixels(at)));
    var closed= pixels(list);
    runningMenuAgain.go();
    look();
    until(()->!Arrays.equals(closed,pixels(list)) && !Arrays.equals(nothing,pixels(list)));
    chooseProgram.go();
    look();
    until(()->Arrays.equals(running,pixels(at)));
    terminate.go();
    until(()->!program.isAlive());
    until(()->Fs.readUtf8(state).contains("\"running\": \"\""));
    runningMenuLast.go();
    look();
    until(()->Arrays.equals(nothing,pixels(list)));
    stopManagers();
  }
}

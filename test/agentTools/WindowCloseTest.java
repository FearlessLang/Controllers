package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Closing the manager window only hides it: the manager keeps running, a second launch hands it over and ends at once, the window comes back as it was, and Quit manager ends the manager.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, helloWorld was never compiled, and nothing is registered for .fearless.
/// Action 1: run the launcher on helloWorld: the manager window opens showing helloWorld.
/// Action 2: close the manager window: the desk shows its background again, and the manager keeps running.
/// Action 3: run the launcher again: it ends at once, and the manager window comes back showing what it showed before.
/// Action 4: choose Quit manager in its Manager menu: the manager ends.
final class WindowCloseTest extends ManagerTest{
  final At managerShown= new At("managerShown",linux(2000),windows(2000));
  final Click closeManager= new Click("closeManager",linux(3822,50),windows(1255,11));
  final At managerBack= new At("managerBack",linux(1000),windows(1000));
  final Click managerMenu= new Click("managerMenu",linux(98,79),windows(31,33));
  final Click quitManager= new Click("quitManager",linux(128,191),windows(61,146));
  @Override protected void walk() throws Exception{
    clean();
    var desk= look();
    var run= launch(project.toString());
    managerShown.go();
    var shown= look();
    closeManager.go();
    until(()->same(desk,look()));
    assertTrue(run.isAlive());
    var again= launch();
    until(()->!again.isAlive());
    assertEquals(0,again.exitValue());
    managerBack.go();
    assertTrue(same(shown,look()));
    managerMenu.go();
    quitManager.go();
    until(()->!run.isAlive());
    assertEquals(0,run.exitValue());
  }
}

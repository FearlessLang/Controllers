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
  final At managerShown= new At("managerShown",on("ubuntu-gnome",2000),on("debian-mate",2500),on("debian-gnome-x11",2500),on("kubuntu-plasma",2500),on("xubuntu-xfce",2500),on("debian-cinnamon",2500),on("lubuntu-lxqt",2500));
  final Click closeManager= new Click("closeManager",on("ubuntu-gnome",3822,50),on("debian-mate",1900,41),on("debian-gnome-x11",1901,50),on("kubuntu-plasma",1905,14),on("xubuntu-xfce",1909,37),on("debian-cinnamon",1901,18),on("lubuntu-lxqt",1906,16));
  final At managerBack= new At("managerBack",on("ubuntu-gnome",1000),on("debian-mate",1500),on("debian-gnome-x11",1500),on("kubuntu-plasma",1500),on("xubuntu-xfce",1500),on("debian-cinnamon",1500),on("lubuntu-lxqt",1500));
  final Click managerMenu= new Click("managerMenu",on("ubuntu-gnome",98,79),on("debian-mate",32,69),on("debian-gnome-x11",32,79),on("kubuntu-plasma",32,38),on("xubuntu-xfce",37,60),on("debian-cinnamon",32,47),on("lubuntu-lxqt",32,40));
  final Click quitManager= new Click("quitManager",on("ubuntu-gnome",128,212),on("debian-mate",50,202),on("debian-gnome-x11",50,212),on("kubuntu-plasma",50,171),on("xubuntu-xfce",50,181),on("debian-cinnamon",50,180),on("lubuntu-lxqt",50,173));
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

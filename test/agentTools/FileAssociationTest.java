package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Desktop;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

/// The manager makes the desk open .fearless files with it and forgets that again when asked, and a file already on screen in the file manager shows each change once its window is reloaded.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, helloWorld was never compiled, nothing is registered for .fearless, and the file manager remembers no place for its windows.
/// Action 1: open the file manager on helloWorld, and reload it: hello_world.fearless shows a generic icon.
/// Action 2: run the launcher: the manager window opens.
/// Action 3: send the manager window away, and reload the same file manager window: hello_world.fearless shows the Fearless icon.
/// Action 4: bring the manager window back from the bar of open windows, choose Forget association in its Manager menu and answer Yes: the manager ends.
/// Action 5: reload it again: hello_world.fearless shows a generic icon again.
/// Action 6: run the launcher: the manager window opens.
/// Action 7: send the manager window away and reload it again: hello_world.fearless shows the Fearless icon again.
/// Action 8: end the manager and close the file manager window.
///
/// A file manager keeps the icon it first drew for a file even after the desk learns a new one, so every look starts by reloading the window.
final class FileAssociationTest extends ManagerTest{
  final At filesShown= new At("filesShown",linux(3000),windows(4000));
  final Click focusFiles= new Click("focusFiles",linux(2200,1200),windows(700,450));
  final At genericShown= new At("genericShown",linux(3000),windows(2000));
  final Area fileIcon= new Area("fileIcon",linux(1836,880,72,60),windows(428,242,24,24));
  final At managerShown= new At("managerShown",linux(2000),windows(2000));
  final Click sendManagerAway= new Click("sendManagerAway",linux(3754,48),windows(1162,11));
  final At managerAway= new At("managerAway",linux(2000),windows(2000));
  final At iconChanged= new At("iconChanged",linux(3000),windows(3000));
  final Click bringManagerBack= new Click("bringManagerBack",linux(32,386),windows(860,696));
  final At managerBack= new At("managerBack",linux(3000),windows(3000));
  final Click managerMenu= new Click("managerMenu",linux(98,79),windows(31,33));
  final Click forgetAssociation= new Click("forgetAssociation",linux(128,170),windows(61,145));
  final At dialogShown= new At("dialogShown",linux(3000),windows(3000));
  final Click yes= new Click("yes",linux(1928,1154),windows(614,387));
  final At iconReverted= new At("iconReverted",linux(3000),windows(3000));
  final At managerShownAgain= new At("managerShownAgain",linux(2000),windows(2000));
  final Click sendManagerAwayAgain= new Click("sendManagerAwayAgain",linux(3754,48),windows(1162,11));
  final At managerAwayAgain= new At("managerAwayAgain",linux(2000),windows(2000));
  final At iconChangedAgain= new At("iconChangedAgain",linux(3000),windows(3000));
  final Click closeFiles= new Click("closeFiles",linux(2374,842),windows(1016,58));
  @Override protected void walk() throws Exception{
    clean();
    forgetWindowPlaces();
    Desktop.getDesktop().open(project.toFile());
    filesShown.go();
    var generic= look(genericShown);
    var run= launch();
    managerShown.go();
    sendManagerAway.go();
    managerAway.go();
    var fearless= look(iconChanged);
    assertFalse(same(generic,fearless));
    bringManagerBack.go();
    managerBack.go();
    managerMenu.go();
    forgetAssociation.go();
    dialogShown.go();
    yes.go();
    until(()->!run.isAlive());
    assertEquals(0,run.exitValue());
    assertTrue(same(generic,look(iconReverted)));
    launch();
    managerShownAgain.go();
    sendManagerAwayAgain.go();
    managerAwayAgain.go();
    assertTrue(same(fearless,look(iconChangedAgain)));
    stopManagers();
    closeFiles.go();
  }
  private BufferedImage look(At reloaded){
    focusFiles.go();
    pilot.chord(KeyEvent.VK_F5);
    reloaded.go();
    look();
    return fileIcon.shot();
  }
}

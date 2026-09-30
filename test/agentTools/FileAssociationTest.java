package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;

import resources.ResolveResource;
import tools.Fs;
import tools.JavacTool;
import utils.Bug;

/// The manager makes the desk open .fearless files with it and forgets that again when asked, and a file already on screen in the file manager shows each change once its window is reloaded.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, helloWorld was never compiled, and nothing is registered for .fearless.
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
final class FileAssociationTest extends PilotTest{
  final At filesShown= new At("filesShown",linux(3000),windows(0));
  final Click focusFiles= new Click("focusFiles",linux(2200,1200),windows(0,0));
  final At genericShown= new At("genericShown",linux(6000),windows(0));
  final Area fileIcon= new Area("fileIcon",linux(1836,880,72,60),windows(0,0,0,0));
  final At managerShown= new At("managerShown",linux(22000),windows(0));
  final Click sendManagerAway= new Click("sendManagerAway",linux(3754,48),windows(0,0));
  final At managerAway= new At("managerAway",linux(24000),windows(0));
  final At iconChanged= new At("iconChanged",linux(27000),windows(0));
  final Click bringManagerBack= new Click("bringManagerBack",linux(32,386),windows(0,0));
  final At managerBack= new At("managerBack",linux(30000),windows(0));
  final Click managerMenu= new Click("managerMenu",linux(98,79),windows(0,0));
  final Click forgetAssociation= new Click("forgetAssociation",linux(128,170),windows(0,0));
  final At dialogShown= new At("dialogShown",linux(33000),windows(0));
  final Click yes= new Click("yes",linux(1928,1154),windows(0,0));
  final At iconReverted= new At("iconReverted",linux(40000),windows(0));
  final At managerShownAgain= new At("managerShownAgain",linux(58000),windows(0));
  final Click sendManagerAwayAgain= new Click("sendManagerAwayAgain",linux(3754,48),windows(0,0));
  final At managerAwayAgain= new At("managerAwayAgain",linux(60000),windows(0));
  final At iconChangedAgain= new At("iconChangedAgain",linux(63000),windows(0));
  final Click closeFiles= new Click("closeFiles",linux(2374,842),windows(0,0));
  private static final Path app= ResolveResource.managedFolderOut.resolve("fearlessManaged"+ResolveResource.versionId);
  private static final Path launcher= Fs.isWindows() ? app.resolve(app.getFileName()+".exe") : app.resolve("bin").resolve(app.getFileName().toString());
  private static final Path project= ResolveResource.integrationTests.resolve("helloWorld");
  @Override protected void walk() throws Exception{
    clean();
    Desktop.getDesktop().open(project.toFile());
    filesShown.go();
    var generic= look(genericShown);
    var before= pilot.shot();
    var run= launch();
    managerShown.go();
    opened(before);
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
    assertTrue(run.waitFor(1,TimeUnit.MINUTES));
    assertEquals(0,run.exitValue());
    assertTrue(same(generic,look(iconReverted)));
    before= pilot.shot();
    launch();
    managerShownAgain.go();
    opened(before);
    sendManagerAwayAgain.go();
    managerAwayAgain.go();
    assertTrue(same(fearless,look(iconChangedAgain)));
    stopManagers();
    closeFiles.go();
  }
  private static Process launch() throws Exception{ return new ProcessBuilder(launcher.toString()).redirectOutput(Redirect.DISCARD).redirectError(Redirect.DISCARD).start(); }
  private void opened(BufferedImage before){
    var r= Pilot.changed(before,pilot.shot(),6);
    assertTrue(r.width>=400 && r.height>=300,r::toString);
  }
  private BufferedImage look(At reloaded){
    focusFiles.go();
    pilot.chord(KeyEvent.VK_F5);
    reloaded.go();
    var s= Toolkit.getDefaultToolkit().getScreenSize();
    pilot.glide(s.width-1,s.height/2,Pilot.none,s.width-1,s.height/2,Pilot.none);
    return fileIcon.shot();
  }
  private static boolean same(BufferedImage a, BufferedImage b){
    var pa= a.getRGB(0,0,a.getWidth(),a.getHeight(),null,0,a.getWidth());
    var pb= b.getRGB(0,0,b.getWidth(),b.getHeight(),null,0,b.getWidth());
    assert pa.length==pb.length;
    var diff= IntStream.range(0,pa.length).filter(i->Math.abs((pa[i]&0xff)-(pb[i]&0xff))+Math.abs((pa[i]>>8&0xff)-(pb[i]>>8&0xff))+Math.abs((pa[i]>>16&0xff)-(pb[i]>>16&0xff))>30).count();
    return diff*20<pa.length;
  }
  private static void clean() throws Exception{
    stopManagers();
    Fs.rmTree(app.resolveSibling(JavacTool.dataDirNameFor(ResolveResource.versionId)));
    Fs.rmTree(project.resolve(".fearless_out"));
    if (Fs.isWindows()){ throw Bug.todo(); }
    var share= Path.of(System.getProperty("user.home"),".local","share");
    for (var dir: List.of(share.resolve("applications"),share.resolve("mime").resolve("packages"))){
      Fs.walkV(dir,s->s.filter(p->p.getFileName().toString().contains("earless")).toList().forEach(p->Fs.ofV(()->Files.delete(p))));
    }
    assertEquals(0,new ProcessBuilder("update-mime-database",share.resolve("mime").toString()).start().waitFor());
    assertEquals(0,new ProcessBuilder("update-desktop-database",share.resolve("applications").toString()).start().waitFor());
  }
  @AfterEach void stop(){ stopManagers(); }
  private static void stopManagers(){ ProcessHandle.allProcesses().filter(p->p.info().command().filter(launcher.toString()::equals).isPresent()).forEach(FileAssociationTest::kill); }
  private static void kill(ProcessHandle p){ p.destroyForcibly(); p.onExit().join(); }
}

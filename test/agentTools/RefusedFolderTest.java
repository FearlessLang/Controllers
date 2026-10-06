package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import tools.Fs;

/// The launcher run on a folder that cannot be a project starts the manager, or hands the folder to the running one, and the manager refuses it with a note saying why: nothing exists there, it is the manager folder, or it is the root of the file system. Nothing is registered and nothing is created, and once a note is dismissed the window is exactly as before.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, nothing is registered for .fearless, and nothing exists at gone beside the manager.
/// Action 1: run the launcher on gone: the manager window opens with a note saying nothing exists there, the manager remembers no project, and nothing exists at gone.
/// Action 2: press OK: the note goes away.
/// Action 3: run the launcher on the data folder of the manager: it ends at once, a note shows saying that folder is the manager folder, and the manager remembers no project.
/// Action 4: press OK: the note goes away and the window is exactly as before.
/// Action 5: run the launcher on the root of the file system: it ends at once, a note shows saying the root cannot be a project, and the manager remembers no project.
/// Action 6: press OK: the note goes away and the window is exactly as before.
/// Action 7: end the manager.
final class RefusedFolderTest extends ManagerTest{
  static final Path gone= data.resolveSibling("gone");
  final At noteShown= new At("noteShown",linux(3000),windows(3000));
  final Area window= new Area("window",linux(68,32,3772,2098),windows(0,24,1280,624));
  final Click ok= new Click("ok",linux(1952,1146),windows(639,378));
  final At noteShownAgain= new At("noteShownAgain",linux(1000),windows(1000));
  final Click okAgain= new Click("okAgain",linux(1952,1194),windows(639,425));
  final At rootNoteShown= new At("rootNoteShown",linux(1000),windows(1000));
  final Click rootOk= new Click("rootOk",linux(1952,1184),windows(639,414));
  int[] at;
  int[] before;
  @Override protected void walk() throws Exception{
    clean();
    Fs.rmTree(gone);
    var missing= "The manager was asked to register\n"+gone+"\nbut nothing exists there: register an existing folder, or a file inside one.\n";
    launch(gone.toString());
    until(()->Files.exists(notes) && Fs.readUtf8(notes).equals(missing));
    noteShown.go();
    assertFalse(Files.exists(info));
    assertFalse(Files.exists(gone));
    look();
    at= window.aim();
    var noted= pixels(at);
    ok.go();
    look();
    until(()->!Arrays.equals(noted,pixels(at)));
    before= pixels(at);
    var manager= missing+"""
      Fearless cannot keep track of this folder as a project.

      The folder is:
        %s
      The manager folder of this Fearless is:
        %s
      The manager folder holds what Fearless remembers about your projects: it is
      never part of a project, and no project is inside it.
      """.formatted(data,data);
    refused(data,noteShownAgain,manager);
    dismissed(okAgain);
    var root= Path.of("/").toAbsolutePath();
    refused(root,rootNoteShown,manager+"""
      Fearless cannot keep track of the root of a drive or of the file system as a
      project.

      The folder is:
        %s
      Put the project in a folder inside it, and make that folder the project
      folder.
      """.formatted(root));
    dismissed(rootOk);
    stopManagers();
  }
  private void refused(Path folder, At shown, String said) throws Exception{
    var run= new ProcessBuilder(launcher.toString(),folder.toString()).start();
    until(()->!run.isAlive());
    assertEquals(0,run.exitValue());
    until(()->!Arrays.equals(before,pixels(at)));
    shown.go();
    assertEquals(said,Fs.readUtf8(notes));
    assertFalse(Files.exists(info));
  }
  private void dismissed(Click dismiss){
    dismiss.go();
    look();
    until(()->Arrays.equals(before,pixels(at)));
  }
}

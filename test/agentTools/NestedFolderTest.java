package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;

import tools.Fs;

/// A folder that holds a registered project folder, or lies inside one, is refused: the launcher run on it ends at once, the manager shows a note naming both folders, registers nothing and writes nothing into either folder, and once the note is dismissed the window is exactly as before.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, nothing is registered for .fearless, and the folder nest beside the manager holds only the folder inner, which holds only its marker and one source file.
/// Action 1: run the launcher on inner: the manager window opens showing inner, and the manager remembers inner as an idle project.
/// Action 2: click the empty space below the tiles.
/// Action 3: run the launcher on nest: it ends at once, a note shows saying nest and inner overlap, the manager still remembers only inner, and no file was added to nest.
/// Action 4: press OK: the note goes away and the window is exactly as before.
/// Action 5: run the launcher on the source folder inside inner: it ends at once, a note shows saying that folder and inner overlap, the manager still remembers only inner, and no file was added to nest.
/// Action 6: press OK: the note goes away and the window is exactly as before.
/// Action 7: end the manager.
final class NestedFolderTest extends ManagerTest{
  static final Path nest= data.resolveSibling("nest");
  static final Path inner= nest.resolve("inner");
  static final Path source= inner.resolve("_inner");
  final At managerShown= new At("managerShown",on("ubuntu-gnome",3000));
  final Click focusTiles= new Click("focusTiles",on("ubuntu-gnome",200,1500));
  final Area window= new Area("window",on("ubuntu-gnome",68,32,3772,2098));
  final At noteShown= new At("noteShown",on("ubuntu-gnome",1000));
  final Click ok= new Click("ok",on("ubuntu-gnome",1952,1223));
  final At noteShownAgain= new At("noteShownAgain",on("ubuntu-gnome",1000));
  final Click okAgain= new Click("okAgain",on("ubuntu-gnome",1952,1223));
  static final String remembered= """
    {
      "inner": {
        "path": "Str:%s",
        "kind": "idle"
      }
    }
    """.formatted(inner);
  List<Path> files;
  int[] at;
  int[] before;
  @Override protected void walk() throws Exception{
    clean();
    Fs.writeUtf8(inner.resolve("inner.fearless"),"\n");
    Fs.writeUtf8(source.resolve("_rank_app.fear"),"use base.Main as Main;\n\nHello:Main{s->base.Debug#(\"inner\")}\n");
    files= tree();
    launch(inner.toString());
    managerShown.go();
    assertEquals(remembered,Fs.readUtf8(info));
    focusTiles.go();
    look();
    at= window.aim();
    before= pixels(at);
    refused(nest,noteShown,"");
    dismissed(ok);
    refused(source,noteShownAgain,note(nest));
    dismissed(okAgain);
    stopManagers();
  }
  private void refused(Path folder, At shown, String earlier) throws Exception{
    var run= new ProcessBuilder(launcher.toString(),folder.toString()).start();
    until(()->!run.isAlive());
    assertEquals(0,run.exitValue());
    until(()->!Arrays.equals(before,pixels(at)));
    shown.go();
    assertEquals(earlier+note(folder),Fs.readUtf8(data.resolve("eclipse").resolve("console.txt")));
    assertEquals(remembered,Fs.readUtf8(info));
    assertEquals(files,tree());
  }
  private void dismissed(Click dismiss){
    dismiss.go();
    look();
    until(()->Arrays.equals(before,pixels(at)));
  }
  private static String note(Path folder){
    return """
      Fearless cannot keep track of this project folder.

      The manager was asked to register:
        %s
      Fearless is already keeping track of:
        %s
      One of the two is inside the other. Fearless keeps track of project folders
      that do not overlap, so that every file belongs to exactly one project.

      Use the folder Fearless already keeps track of, or make Fearless forget that
      folder first, and then register this one again.
      """.formatted(folder,inner);
  }
  private static List<Path> tree(){ return Fs.walk(nest,s->s.sorted().toList()); }
  @Override @AfterEach void clean() throws Exception{
    super.clean();
    Fs.rmTree(nest);
  }
}

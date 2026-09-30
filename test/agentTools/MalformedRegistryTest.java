package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import tools.Fs;

/// A file of the remembered projects that is not well formed stops the manager at start: an error shows, and once it is dismissed the launcher ends with exit 1, leaving the file as it was and nothing registered for .fearless. Once the file is mended, the next start remembers the project the file names and takes up the start left by the refused one.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the data folder of the manager holds only the file of the projects it remembers, naming helloWorld as hello_world with the kind idle but missing its last closing brace, helloWorld was never compiled, and nothing is registered for .fearless.
/// Action 1: run the launcher: an error shows in the middle of the screen, the launcher waits for it to be dismissed, and the file of the remembered projects is unchanged.
/// Action 2: press OK: the launcher ends with exit 1, the desk shows its background again, the file is unchanged, nothing is registered for .fearless, and the start the launcher asked for is left waiting in the data folder.
/// Action 3: add the missing closing brace at the end of the file.
/// Action 4: run the launcher: the manager window opens, no start is left waiting, the manager shows no note, publishes hello_world as its only project, idle, and remembers exactly what the file says.
/// Action 5: end the manager.
final class MalformedRegistryTest extends ManagerTest{
  static final String remembered= """
    {
      "hello_world": {
        "path": "Str:%s",
        "kind": "idle"
      }
    }
    """.formatted(project);
  static final String broken= remembered.substring(0,remembered.length()-2);
  final Area error= new Area("error",linux(1915,1075,10,10));
  final At errorShown= new At("errorShown",linux(1000));
  final Click ok= new Click("ok",linux(1952,1212));
  @Override protected void walk() throws Exception{
    clean();
    Fs.writeUtf8(info,broken);
    var desk= look();
    var at= error.aim();
    var before= pixels(at);
    var run= new ProcessBuilder(launcher.toString()).start();
    until(()->!Arrays.equals(before,pixels(at)));
    errorShown.go();
    assertTrue(run.isAlive());
    assertEquals(broken,Fs.readUtf8(info));
    ok.go();
    until(()->!run.isAlive());
    assertEquals(1,run.exitValue());
    until(()->same(desk,look()));
    assertEquals(broken,Fs.readUtf8(info));
    for (var dir: List.of(share.resolve("applications"),share.resolve("mime").resolve("packages"))){ assertEquals(List.of(),Fs.walk(dir,s->s.filter(p->p.getFileName().toString().contains("earless")).toList())); }
    assertEquals(1,waiting());
    Fs.writeUtf8(info,remembered);
    launch();
    until(()->Files.exists(state) && waiting()==0);
    assertEquals("",Fs.readUtf8(notes));
    assertEquals("""
      {
        "hello_world": {
          "folder": "Str:%s",
          "kind": "idle",
          "running": "",
          "runs": "0",
          "lastRun": "",
          "exit": "-1",
          "mains": {},
          "problem": {}
        }
      }
      """.formatted(project),Fs.readUtf8(state));
    assertEquals(remembered,Fs.readUtf8(info));
    stopManagers();
  }
  private static long waiting(){ return Fs.walk(data.resolve("messages"),s->s.filter(p->p.toString().endsWith(".msg")).count()); }
}

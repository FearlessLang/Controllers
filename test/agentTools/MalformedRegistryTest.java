package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import tools.Fs;

/// A file of the remembered projects that is not well formed stops the manager at start: before the error shows, the manager deletes its data folder and every file association of Fearless, and once the error is dismissed the launcher ends with exit 1. The next start is the start of a fresh copy.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, helloWorld was never compiled, and nothing is registered for .fearless.
/// Action 1: run the launcher on helloWorld: the manager window opens, remembers hello_world as an idle project, and registers .fearless.
/// Action 2: end the manager: the desk shows its background again.
/// Action 3: remove the last closing brace of the file of the remembered projects.
/// Action 4: run the launcher: an error shows in the middle of the screen, the launcher waits for it to be dismissed, the manager has no data folder, and nothing is registered for .fearless.
/// Action 5: press OK: the launcher ends with exit 1, and the desk shows its background again.
/// Action 6: run the launcher: the manager window opens remembering no project, and registers .fearless again.
/// Action 7: end the manager.
final class MalformedRegistryTest extends ManagerTest{
  static final String remembered= """
    {
      "hello_world": {
        "path": "Str:%s",
        "kind": "idle"
      }
    }
    """.formatted(project);
  final Area error= new Area("error",linux(1915,1075,10,10));
  final At errorShown= new At("errorShown",linux(1000));
  final Click ok= new Click("ok",linux(1952,1279));
  @Override protected void walk() throws Exception{
    clean();
    var desk= look();
    launch(project.toString());
    until(()->Files.exists(info) && Fs.readUtf8(info).equals(remembered) && !registered().isEmpty());
    stopManagers();
    until(()->same(desk,look()));
    Fs.writeUtf8(info,remembered.substring(0,remembered.length()-2));
    var at= error.aim();
    var run= new ProcessBuilder(launcher.toString()).start();
    until(()->!Arrays.equals(pixels(desk,at),pixels(at)));
    errorShown.go();
    assertTrue(run.isAlive());
    assertFalse(Files.exists(data));
    assertEquals(List.of(),registered());
    ok.go();
    until(()->!run.isAlive());
    assertEquals(1,run.exitValue());
    until(()->same(desk,look()));
    launch();
    until(()->Files.exists(state) && !registered().isEmpty());
    assertEquals("{}\n",Fs.readUtf8(state));
    stopManagers();
  }
  private static List<Path> registered(){ return Stream.of(share.resolve("applications"),share.resolve("mime").resolve("packages")).flatMap(d->Fs.walk(d,s->s.filter(p->p.getFileName().toString().contains("earless")).toList()).stream()).toList(); }
  private static int[] pixels(BufferedImage img, int[] at){ return img.getRGB(at[0],at[1],at[2],at[3],null,0,at[2]); }
}

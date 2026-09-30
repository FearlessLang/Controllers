package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import tools.Fs;

/// Starting the manager with two arguments is refused: an error shows, and once it is dismissed the launcher ends with exit 1, having started no manager, made no data folder and registered nothing for .fearless.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, helloWorld was never compiled, and nothing is registered for .fearless.
/// Action 1: run the launcher on helloWorld and helloStackTraces together: an error shows in the middle of the screen, the launcher waits for it to be dismissed, and the manager has no data folder.
/// Action 2: press OK: the launcher ends with exit 1, the desk shows its background again, the manager still has no data folder, and nothing is registered for .fearless.
final class TooManyArgumentsTest extends ManagerTest{
  final Area error= new Area("error",linux(1915,1075,10,10));
  final At errorShown= new At("errorShown",linux(1000));
  final Click ok= new Click("ok",linux(1952,1212));
  @Override protected void walk() throws Exception{
    clean();
    var desk= look();
    var at= error.aim();
    var before= pixels(at);
    var run= new ProcessBuilder(launcher.toString(),project.toString(),other.toString()).start();
    until(()->!Arrays.equals(before,pixels(at)));
    errorShown.go();
    assertTrue(run.isAlive());
    assertFalse(Files.exists(data));
    ok.go();
    until(()->!run.isAlive());
    assertEquals(1,run.exitValue());
    until(()->same(desk,look()));
    assertFalse(Files.exists(data));
    for (var dir: List.of(share.resolve("applications"),share.resolve("mime").resolve("packages"))){ assertEquals(List.of(),Fs.walk(dir,s->s.filter(p->p.getFileName().toString().contains("earless")).toList())); }
  }
}

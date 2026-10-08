package agentTools;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.util.List;

final class TooManyArgumentsTest extends ManagerTest{
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action errorShown= action("errorShown",
    on("ubuntu_gnome",()->waitUntilTime(val(19500))));
  final Action ok= action("ok",
    on("ubuntu_gnome",()->click(val(1952),val(1212))),
    on("windows",()->click(val(639),val(441))));
  final Action launcherEnded= action("launcherEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(23000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var project= project("helloWorld");
    var other= project("helloStackTraces");
    launchScript("first",project,other);
    runInTerminal(appsShown,terminalShown);
    errorShown.go();
    stabilize();
    assertFalse(Files.exists(filesIOFolder.resolve("first.exit")));
    assertFalse(Files.exists(filesIOFolder.resolve(data)));
    ok.go();
    launcherEnded.go();
    stabilize();
    checkContent(List.of("first.exit"),"1\n");
    assertFalse(Files.exists(filesIOFolder.resolve(data)));
  }
}

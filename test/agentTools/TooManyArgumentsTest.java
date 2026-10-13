package agentTools;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.awt.Rectangle;
import java.nio.file.Files;
import java.util.List;

final class TooManyArgumentsTest extends ManagerTest{
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitAfter(val(3500))),
    on("xubuntu_xfce",()->waitAfter(val(4500))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitAfter(val(5000))),
    on("xubuntu_xfce",()->waitAfter(val(6500))));
  final Action errorShown= action("errorShown",
    on("ubuntu_gnome",()->waitAfter(val(19500))),
    on("xubuntu_xfce",()->waitAfter(val(24000))));
  final Action errorClosed= action("errorClosed",
    on("ubuntu_gnome",()->waitAfter(val(3000))),
    on("xubuntu_xfce",()->waitAfter(val(4000))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",()->waitAfter(val(7000))),
    on("xubuntu_xfce",()->waitAfter(val(8500))));
  @Override void walk() throws Throwable{
    noManagerData();
    var project= project("helloWorld");
    var other= project("helloStackTraces");
    var center= new Rectangle(760,460,400,160);
    var before= look();
    launchScript("first",project,other);
    runInTerminal(appsShown,terminalShown);
    errorShown.go();
    changed(before,center);
    stabilize();
    assertFalse(Files.exists(filesIOFolder.resolve("first.exit")));
    checkContent(info,"{\n  \"projects\": {},\n  \"systemExtensions\": []\n}\n");
    var error= look();
    closeDialog.go();
    errorClosed.go();
    changed(error,center);
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
  }
}

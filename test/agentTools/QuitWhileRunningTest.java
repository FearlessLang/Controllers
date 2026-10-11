package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Rectangle;
import java.util.List;

final class QuitWhileRunningTest extends ManagerTest{
  final Action appsShown= action("appsShown",on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",on("ubuntu_gnome",()->waitUntilTime(val(18000))));
  final Action codeShown= action("codeShown",on("ubuntu_gnome",()->waitUntilTime(val(21000))));
  final Action compile= action("compile",on("ubuntu_gnome",()->click(val(520),val(112))));
  final Action compiled= action("compiled",on("ubuntu_gnome",()->waitUntilTime(val(36000))));
  final Action run= action("run",on("ubuntu_gnome",()->click(val(520),val(112))));
  final Action programShown= action("programShown",on("ubuntu_gnome",()->waitUntilTime(val(42000))));
  final Action managerQuit= action("managerQuit",on("ubuntu_gnome",()->waitUntilTime(val(47000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var gui= project("testGui1");
    launchScript("first",gui.resolve("start.fearless"));
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    becomeCode.go();
    codeShown.go();
    compile.go();
    compiled.go();
    var program= new Rectangle(1890,1040,120,120);
    var before= look();
    run.go();
    programShown.go();
    changed(before,program);
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
    changed(before,new Rectangle(100,200,200,400));
    assertEquals(List.of(),ProcessHandle.allProcesses().filter(h->h.info().command().orElse("").endsWith("java")).map(h->h.info().commandLine().orElse("")).filter(c->c.contains("gui_example.Foo")).toList());
  }
}

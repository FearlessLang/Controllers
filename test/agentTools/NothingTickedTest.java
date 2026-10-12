package agentTools;

import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.List;

final class NothingTickedTest extends ManagerTest{
  final Action becomeCode= header("becomeCode",1027);
  final Action appsShown= action("appsShown",on("ubuntu_gnome",()->waitUntilTime(val(3500))));
  final Action terminalShown= action("terminalShown",on("ubuntu_gnome",()->waitUntilTime(val(8500))));
  final Action managerShown= action("managerShown",on("ubuntu_gnome",()->waitUntilTime(val(26000))));
  final Action codeShown= action("codeShown",on("ubuntu_gnome",()->waitUntilTime(val(30500))));
  final Action compile= action("compile",on("ubuntu_gnome",()->click(val(580),val(113))));
  final Action compiled= action("compiled",on("ubuntu_gnome",()->waitUntilTime(val(51500))));
  final Action runV= action("runV",on("ubuntu_gnome",()->click(val(580),val(113))));
  final Action mainsShown= action("mainsShown",on("ubuntu_gnome",()->waitUntilTime(val(54500))));
  final Action tickHello3= action("tickHello3",on("ubuntu_gnome",()->click(val(574),val(164))));
  final Action untickHello3= action("untickHello3",on("ubuntu_gnome",()->click(val(574),val(164))));
  final Action runNothing= action("runNothing",on("ubuntu_gnome",()->click(val(583),val(263))));
  final Action nothingRan= action("nothingRan",on("ubuntu_gnome",()->waitUntilTime(val(62000))));
  final Action closeMains= action("closeMains",on("ubuntu_gnome",()->keys(KeyEvent.VK_ESCAPE)));
  final Action managerQuit= action("managerQuit",on("ubuntu_gnome",()->waitUntilTime(val(69000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var project= project("helloWorld");
    launchScript("first",project.resolve("hello_world.fearless"));
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    becomeCode.go();
    codeShown.go();
    compile.go();
    compiled.go();
    var output= new Rectangle(660,170,1000,90);
    var runButton= new Rectangle(562,252,50,24);
    var mains= new Rectangle(562,125,100,150);
    var closed= look();
    runV.go();
    mainsShown.go();
    var none= look();
    changed(closed,mains);
    tickHello3.go();
    changed(none,runButton);
    untickHello3.go();
    same(none,runButton);
    runNothing.go();
    nothingRan.go();
    same(none,runButton);
    changed(closed,mains);
    same(closed,output);
    closeMains.go();
    same(closed,mains);
    stabilize();
    checkContent(info,"""
      {
        "projects": {
          "hello_world": {
            "path": "Str:%s",
            "kind": "code"
          }
        },
        "systemExtensions": []
      }
      """.formatted(slashed(project)));
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
  }
}

package agentTools;

import java.awt.Rectangle;
import java.util.List;

final class AllMainsRunTest extends ManagerTest{
  final Action becomeCode= header("becomeCode",1027);
  final Action appsShown= action("appsShown",on("ubuntu_gnome",()->waitUntilTime(val(3500))));
  final Action terminalShown= action("terminalShown",on("ubuntu_gnome",()->waitUntilTime(val(8500))));
  final Action managerShown= action("managerShown",on("ubuntu_gnome",()->waitUntilTime(val(26000))));
  final Action codeShown= action("codeShown",on("ubuntu_gnome",()->waitUntilTime(val(30500))));
  final Action compile= action("compile",on("ubuntu_gnome",()->click(val(580),val(113))));
  final Action compiled= action("compiled",on("ubuntu_gnome",()->waitUntilTime(val(51500))));
  final Action runV= action("runV",on("ubuntu_gnome",()->click(val(580),val(113))));
  final Action mainsShown= action("mainsShown",on("ubuntu_gnome",()->waitUntilTime(val(54500))));
  final Action tick= action("tick",on("ubuntu_gnome",()->List.of(140,166,190,216,241).forEach(y->click(val(574),val(y)))));
  final Action runTicked= action("runTicked",on("ubuntu_gnome",()->click(val(584),val(265))));
  final Action runsDone= action("runsDone",on("ubuntu_gnome",()->waitUntilTime(val(75000))));
  final Action mainsShownAgain= action("mainsShownAgain",on("ubuntu_gnome",()->waitUntilTime(val(78500))));
  final Action closeMains= action("closeMains",on("ubuntu_gnome",()->keys(java.awt.event.KeyEvent.VK_ESCAPE)));
  final Action managerQuit= action("managerQuit",on("ubuntu_gnome",()->waitUntilTime(val(85500))));
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
    var output= new Rectangle(410,300,1000,120);
    var before= look();
    runV.go();
    mainsShown.go();
    var unticked= look();
    tick.go();
    runTicked.go();
    runsDone.go();
    changed(before,output);
    stabilize();
    checkContent(info,"""
      {
        "projects": {
          "hello_world": {
            "path": "Str:%s",
            "kind": "code",
            "mains": ["hello.Hello1", "hello.Hello3", "hello.Hello4", "hello.Hello5", "hello.Hello6"]
          }
        },
        "systemExtensions": []
      }
      """.formatted(slashed(project)));
    runV.go();
    mainsShownAgain.go();
    changed(unticked,new Rectangle(566,132,16,116));
    closeMains.go();
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
  }
}

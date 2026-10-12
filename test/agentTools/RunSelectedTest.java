package agentTools;

import java.awt.Rectangle;
import java.util.List;

final class RunSelectedTest extends ManagerTest{
  final Action becomeCode= header("becomeCode",1027);
  final Action appsShown= action("appsShown",on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",on("ubuntu_gnome",()->waitUntilTime(val(18000))));
  final Action codeShown= action("codeShown",on("ubuntu_gnome",()->waitUntilTime(val(21000))));
  final Action compile= action("compile",on("ubuntu_gnome",()->click(val(580),val(113))));
  final Action compiled= action("compiled",on("ubuntu_gnome",()->waitUntilTime(val(36000))));
  final Action runV= action("runV",on("ubuntu_gnome",()->click(val(580),val(113))));
  final Action mainsShown= action("mainsShown",on("ubuntu_gnome",()->waitUntilTime(val(37500))));
  final Action tickHello4= action("tickHello4",on("ubuntu_gnome",()->click(val(574),val(189))));
  final Action tickHello1= action("tickHello1",on("ubuntu_gnome",()->click(val(574),val(139))));
  final Action runTicked= action("runTicked",on("ubuntu_gnome",()->click(val(584),val(265))));
  final Action runsDone= action("runsDone",on("ubuntu_gnome",()->waitUntilTime(val(44000))));
  final Action managerQuit= action("managerQuit",on("ubuntu_gnome",()->waitUntilTime(val(49000))));
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
    stabilize();
    checkContent(List.of("helloWorld",".fearless_out","mains.info"),"[###]\"hello.Hello1\": [###]");
    var output= new Rectangle(410,168,1000,120);
    var before= look();
    runV.go();
    mainsShown.go();
    tickHello4.go();
    tickHello1.go();
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
            "mains": ["hello.Hello1", "hello.Hello4"]
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

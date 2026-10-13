package agentTools;

import java.util.List;

final class AllMainsRunTest extends ManagerTest{
  final Action becomeCode= action("becomeCode",
    on("ubuntu_gnome",()->click(val(1027),val(113))),
    on("xubuntu_xfce",()->click(val(1040),val(93))));
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitAfter(val(3500))),
    on("xubuntu_xfce",()->waitAfter(val(4500))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitAfter(val(5000))),
    on("xubuntu_xfce",()->waitAfter(val(6500))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitAfter(val(17500))),
    on("xubuntu_xfce",()->waitAfter(val(21500))));
  final Action codeShown= action("codeShown",
    on("ubuntu_gnome",()->waitAfter(val(4500))),
    on("xubuntu_xfce",()->waitAfter(val(6000))));
  final Action compile= action("compile",
    on("ubuntu_gnome",()->click(val(580),val(113))),
    on("xubuntu_xfce",()->click(val(545),val(93))));
  final Action compiled= action("compiled",
    on("ubuntu_gnome",()->waitAfter(val(21000))),
    on("xubuntu_xfce",()->waitAfter(val(26000))));
  final Action runV= action("runV",
    on("ubuntu_gnome",()->click(val(580),val(113))),
    on("xubuntu_xfce",()->click(val(538),val(93))));
  final Action mainsShown= action("mainsShown",
    on("ubuntu_gnome",()->waitAfter(val(3000))),
    on("xubuntu_xfce",()->waitAfter(val(4000))));
  final Action tick= action("tick",
    on("ubuntu_gnome",()->List.of(140,166,190,216,241).forEach(y->click(val(574),val(y)))),
    on("xubuntu_xfce",()->List.of(118,141,164,187,210).forEach(y->click(val(512),val(y)))));
  final Action runTicked= action("runTicked",
    on("ubuntu_gnome",()->click(val(584),val(265))),
    on("xubuntu_xfce",()->click(val(524),val(232))));
  final Action runsDone= action("runsDone",
    on("ubuntu_gnome",()->waitAfter(val(20500))),
    on("xubuntu_xfce",()->waitAfter(val(25500))));
  final Action mainsShownAgain= action("mainsShownAgain",
    on("ubuntu_gnome",()->waitAfter(val(3500))),
    on("xubuntu_xfce",()->waitAfter(val(4500))));
  final Action closeMains= action("closeMains",
    on("ubuntu_gnome",()->keys(java.awt.event.KeyEvent.VK_ESCAPE)),
    on("xubuntu_xfce",()->keys(java.awt.event.KeyEvent.VK_ESCAPE)));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",()->waitAfter(val(7000))),
    on("xubuntu_xfce",()->waitAfter(val(8500))));
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
    var output= inWindow(410,300,1000,120);
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
    changed(unticked,inWindow(566,132,16,116));
    closeMains.go();
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
  }
}

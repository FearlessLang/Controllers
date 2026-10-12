package agentTools;

import java.util.List;

final class RunSelectedTest extends ManagerTest{
  final Action becomeCode= action("becomeCode",
    on("ubuntu_gnome",()->click(val(1027),val(113))),
    on("xubuntu_xfce",()->click(val(1040),val(93))));
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(3500))),
    on("xubuntu_xfce",()->waitUntilTime(val(4500))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(8500))),
    on("xubuntu_xfce",()->waitUntilTime(val(11000))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(26000))),
    on("xubuntu_xfce",()->waitUntilTime(val(32500))));
  final Action codeShown= action("codeShown",
    on("ubuntu_gnome",()->waitUntilTime(val(30500))),
    on("xubuntu_xfce",()->waitUntilTime(val(38500))));
  final Action compile= action("compile",
    on("ubuntu_gnome",()->click(val(580),val(113))),
    on("xubuntu_xfce",()->click(val(545),val(93))));
  final Action compiled= action("compiled",
    on("ubuntu_gnome",()->waitUntilTime(val(51500))),
    on("xubuntu_xfce",()->waitUntilTime(val(64500))));
  final Action runV= action("runV",
    on("ubuntu_gnome",()->click(val(580),val(113))),
    on("xubuntu_xfce",()->click(val(538),val(93))));
  final Action mainsShown= action("mainsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(54500))),
    on("xubuntu_xfce",()->waitUntilTime(val(68500))));
  final Action tickHello4= action("tickHello4",
    on("ubuntu_gnome",()->click(val(574),val(189))),
    on("xubuntu_xfce",()->click(val(512),val(164))));
  final Action tickHello1= action("tickHello1",
    on("ubuntu_gnome",()->click(val(574),val(139))),
    on("xubuntu_xfce",()->click(val(512),val(118))));
  final Action runTicked= action("runTicked",
    on("ubuntu_gnome",()->click(val(584),val(265))),
    on("xubuntu_xfce",()->click(val(524),val(232))));
  final Action runsDone= action("runsDone",
    on("ubuntu_gnome",()->waitUntilTime(val(63500))),
    on("xubuntu_xfce",()->waitUntilTime(val(79500))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",()->waitUntilTime(val(70500))),
    on("xubuntu_xfce",()->waitUntilTime(val(88500))));
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
    var output= inWindow(410,168,1000,120);
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

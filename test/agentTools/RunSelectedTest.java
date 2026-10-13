package agentTools;

final class RunSelectedTest extends ManagerTest{
  final Action becomeCode= action("becomeCode",
    on("ubuntu_gnome",()->click(val(1027),val(113))),
    on("xubuntu_xfce",()->click(val(1040),val(93))));
  final Action openHelloWorld= action("openHelloWorld",
    on("ubuntu_gnome",()->waitDoubleClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",()->waitDoubleClick(val(null),val(null),val(null))));
  final Action openHelloWorldMarker= action("openHelloWorldMarker",
    on("ubuntu_gnome",()->waitDoubleClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",()->waitDoubleClick(val(null),val(null),val(null))));
  final Action projectShown= action("projectShown",
    on("ubuntu_gnome",()->waitAfter(val(null))),
    on("xubuntu_xfce",()->waitAfter(val(null))));
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
    on("ubuntu_gnome",()->waitAfter(val(9000))),
    on("xubuntu_xfce",()->waitAfter(val(11000))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",()->waitAfter(val(7000))),
    on("xubuntu_xfce",()->waitAfter(val(9000))));
  @Override void walk() throws Throwable{
    var project= project("helloWorld");
    var bare= look();
    start();
    managerShown.go();
    toFiles();
    openHelloWorld.go();
    openHelloWorldMarker.go();
    closeFileManager.go();
    projectShown.go();
    becomeCode.go();
    codeShown.go();
    compile.go();
    compiled.go();
    stabilize();
    checkContent(files.resolve("helloWorld/.fearless_out/mains.info"),"[###]\"hello.Hello1\": [###]");
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
    same(bare,output);
  }
}

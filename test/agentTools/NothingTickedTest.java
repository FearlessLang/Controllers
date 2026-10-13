package agentTools;

final class NothingTickedTest extends ManagerTest{
  final Action becomeCode= action("becomeCode",
    on("ubuntu_gnome",()->click(val(1027),val(113))),
    on("xubuntu_xfce",()->click(val(1040),val(93))));
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
  final Action tickHello3= action("tickHello3",
    on("ubuntu_gnome",()->click(val(574),val(164))),
    on("xubuntu_xfce",()->click(val(512),val(141))));
  final Action untickHello3= action("untickHello3",
    on("ubuntu_gnome",()->click(val(574),val(164))),
    on("xubuntu_xfce",()->click(val(512),val(141))));
  final Action runNothing= action("runNothing",
    on("ubuntu_gnome",()->click(val(583),val(263))),
    on("xubuntu_xfce",()->click(val(524),val(232))));
  final Action nothingRan= action("nothingRan",
    on("ubuntu_gnome",()->waitAfter(val(7500))),
    on("xubuntu_xfce",()->waitAfter(val(9000))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",()->waitAfter(val(7000))),
    on("xubuntu_xfce",()->waitAfter(val(9000))));
  final Action openHelloWorld= action("openHelloWorld",
    on("ubuntu_gnome",()->waitDoubleClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",()->waitDoubleClick(val(null),val(null),val(null))));
  final Action openHelloWorldMarker= action("openHelloWorldMarker",
    on("ubuntu_gnome",()->waitDoubleClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",()->waitDoubleClick(val(null),val(null),val(null))));
  final Action projectShown= action("projectShown",
    on("ubuntu_gnome",()->waitAfter(val(null))),
    on("xubuntu_xfce",()->waitAfter(val(null))));
  final Action closeMains= action("closeMains",
    on("ubuntu_gnome",()->waitClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",()->waitClick(val(null),val(null),val(null))));
  @Override void walk() throws Throwable{
    var project= project("helloWorld");
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
    var output= inWindow(692,170,968,90);
    var runButton= inWindow(562,252,50,24);
    var mains= inWindow(562,125,100,150);
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
  }
}

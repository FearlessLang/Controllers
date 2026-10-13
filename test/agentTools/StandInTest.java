package agentTools;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.awt.Rectangle;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Stream;

import tools.Fs;

final class StandInTest extends ManagerTest{
  final Action trayOff= action("trayOff",
    on("ubuntu_gnome",()->extensions("disable")),
    on("xubuntu_xfce",()->session("xfce4-panel --quit")));
  final Action trayOn= action("trayOn",
    on("ubuntu_gnome",()->extensions("enable")),
    on("xubuntu_xfce",()->session("setsid -f xfce4-panel >/dev/null 2>&1")));
  final Action closeWindowBare= action("closeWindowBare",
    on("ubuntu_gnome",()->click(val(1902),val(50))),
    on("xubuntu_xfce",()->click(val(1909),val(10))));
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitAfter(val(4500))),
    on("xubuntu_xfce",()->waitAfter(val(6000))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitAfter(val(5000))),
    on("xubuntu_xfce",()->waitAfter(val(6000))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitAfter(val(17500))),
    on("xubuntu_xfce",()->waitAfter(val(22000))));
  final Action managerHidden= action("managerHidden",
    on("ubuntu_gnome",()->waitAfter(val(3500))),
    on("xubuntu_xfce",()->waitAfter(val(4500))));
  final Action standIn= action("standIn",
    on("ubuntu_gnome",()->click(val(1904),val(1064))),
    on("xubuntu_xfce",()->click(val(1904),val(1064))));
  final Action menuShown= action("menuShown",
    on("ubuntu_gnome",()->waitAfter(val(3500))),
    on("xubuntu_xfce",()->waitAfter(val(4000))));
  final Action showItem= action("showItem",
    on("ubuntu_gnome",()->click(val(1860),val(1041))),
    on("xubuntu_xfce",()->click(val(1850),val(1045))));
  final Action managerBack= action("managerBack",
    on("ubuntu_gnome",()->waitAfter(val(4500))),
    on("xubuntu_xfce",()->waitAfter(val(6000))));
  final Action hiddenAgain= action("hiddenAgain",
    on("ubuntu_gnome",()->waitAfter(val(3500))),
    on("xubuntu_xfce",()->waitAfter(val(4000))));
  final Action standInRight= action("standInRight",
    on("ubuntu_gnome",()->rightClick(val(1904),val(1064))),
    on("xubuntu_xfce",()->rightClick(val(1904),val(1064))));
  final Action menuShownAgain= action("menuShownAgain",
    on("ubuntu_gnome",()->waitAfter(val(3500))),
    on("xubuntu_xfce",()->waitAfter(val(4500))));
  final Action quitItem= action("quitItem",
    on("ubuntu_gnome",()->click(val(1860),val(1066))),
    on("xubuntu_xfce",()->click(val(1850),val(1068))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",()->waitAfter(val(5500))),
    on("xubuntu_xfce",()->waitAfter(val(7000))));
  void extensions(String what){
    var p= Fs.of(()->new ProcessBuilder(Stream.of("gnome-extensions",what,"ubuntu-appindicators@ubuntu.com").toList()).inheritIO().start());
    assert p.onExit().join().exitValue() == 0;
    Pilot.pause(2000);
  }
  void session(String command){
    inGuest("su tester -c 'DISPLAY=:0 DBUS_SESSION_BUS_ADDRESS=unix:path=/run/user/1000/bus "+command+"'");
    Pilot.pause(2000);
  }
  @Override void walk() throws Throwable{
    noManagerData();
    var project= project("helloWorld");
    var tiles= inWindow(100,200,200,400);
    var corner= new Rectangle(1888,1048,32,32);
    trayOff.go();
    try{
      launchScript("first",project.resolve("hello_world.fearless"));
      runInTerminal(appsShown,terminalShown);
      managerShown.go();
      var shown= look();
      closeWindowBare.go();
      managerHidden.go();
      changed(shown,tiles);
      changed(shown,corner);
      var hidden= look();
      stabilize();
      assertFalse(Files.exists(filesIOFolder.resolve("first.exit")));
      standIn.go();
      menuShown.go();
      changed(hidden,corner);
      showItem.go();
      managerBack.go();
      same(shown,tiles);
      changed(hidden,corner);
      closeWindowBare.go();
      hiddenAgain.go();
      same(hidden,corner);
      standInRight.go();
      menuShownAgain.go();
      changed(hidden,corner);
      quitItem.go();
      managerQuit.go();
      stabilize();
      checkContent(List.of("first.exit"),"0\n");
    }
    finally{ trayOn.go(); }
  }
}

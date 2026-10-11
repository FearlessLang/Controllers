package agentTools;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.awt.Rectangle;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Stream;

import tools.Fs;

final class StandInTest extends ManagerTest{
  final Action trayOff= action("trayOff",on("ubuntu_gnome",()->extensions("disable")));
  final Action trayOn= action("trayOn",on("ubuntu_gnome",()->extensions("enable")));
  final Action appsShown= action("appsShown",on("ubuntu_gnome",()->waitUntilTime(val(3000))));
  final Action terminalShown= action("terminalShown",on("ubuntu_gnome",()->waitUntilTime(val(6500))));
  final Action managerShown= action("managerShown",on("ubuntu_gnome",()->waitUntilTime(val(19000))));
  final Action managerHidden= action("managerHidden",on("ubuntu_gnome",()->waitUntilTime(val(21000))));
  final Action standIn= action("standIn",on("ubuntu_gnome",()->click(val(3824),val(2144))));
  final Action managerBack= action("managerBack",on("ubuntu_gnome",()->waitUntilTime(val(25000))));
  final Action hiddenAgain= action("hiddenAgain",on("ubuntu_gnome",()->waitUntilTime(val(27000))));
  final Action rightPress= action("rightPress",on("ubuntu_gnome",()->{ pilot.glide(3824,2144,Pilot.none,3824,2144,java.util.Set.of(Pilot.Button.right)); pilot.glide(3824,2144,java.util.Set.of(Pilot.Button.right),3824,2144,Pilot.none); }));
  final Action menuShown= action("menuShown",on("ubuntu_gnome",()->waitUntilTime(val(29000))));
  final Action quitItem= action("quitItem",on("ubuntu_gnome",()->click(val(3770),val(2148))));
  final Action managerQuit= action("managerQuit",on("ubuntu_gnome",()->waitUntilTime(val(33000))));
  void extensions(String what){
    var p= Fs.of(()->new ProcessBuilder(Stream.of("gnome-extensions",what,"ubuntu-appindicators@ubuntu.com").toList()).inheritIO().start());
    assert p.onExit().join().exitValue() == 0;
    Pilot.pause(2000);
  }
  @Override void walk() throws Throwable{
    noManagerData();
    var project= project("helloWorld");
    var tiles= new Rectangle(100,200,200,400);
    var corner= new Rectangle(3808,2128,32,32);
    trayOff.go();
    try{
      launchScript("first",project.resolve("hello_world.fearless"));
      runInTerminal(appsShown,terminalShown);
      managerShown.go();
      var shown= look();
      closeWindow.go();
      managerHidden.go();
      changed(shown,tiles);
      changed(shown,corner);
      var hidden= look();
      stabilize();
      assertFalse(Files.exists(filesIOFolder.resolve("first.exit")));
      standIn.go();
      managerBack.go();
      same(shown,tiles);
      changed(hidden,corner);
      closeWindow.go();
      hiddenAgain.go();
      same(hidden,corner);
      rightPress.go();
      menuShown.go();
      quitItem.go();
      managerQuit.go();
      stabilize();
      checkContent(List.of("first.exit"),"0\n");
    }
    finally{ trayOn.go(); }
  }
}

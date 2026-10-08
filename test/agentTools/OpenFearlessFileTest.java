package agentTools;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.util.List;

final class OpenFearlessFileTest extends ManagerTest{
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(2000))),
    on("debian_gnome_x11",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(5500))),
    on("debian_gnome_x11",()->waitUntilTime(val(6500))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(18500))),
    on("debian_gnome_x11",()->waitUntilTime(val(20000))));
  final Action managerMenu= action("managerMenu",
    on("ubuntu_gnome",()->click(val(98),val(79))),
    on("debian_gnome_x11",()->click(val(32),val(80))),
    on("windows",()->click(val(31),val(33))));
  final Action quitManager= action("quitManager",
    on("ubuntu_gnome",()->click(val(116),val(212))),
    on("debian_gnome_x11",()->click(val(60),val(212))),
    on("windows",()->click(val(61),val(167))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",()->waitUntilTime(val(21500))),
    on("debian_gnome_x11",()->waitUntilTime(val(24500))));
  final Action appsShownFiles= action("appsShownFiles",
    on("ubuntu_gnome",()->waitUntilTime(val(23000))),
    on("debian_gnome_x11",()->waitUntilTime(val(26000))));
  final Action terminalShownFiles= action("terminalShownFiles",
    on("ubuntu_gnome",()->waitUntilTime(val(26500))),
    on("debian_gnome_x11",()->waitUntilTime(val(30500))));
  final Action filesShown= action("filesShown",
    on("ubuntu_gnome",()->waitUntilTime(val(38500))),
    on("debian_gnome_x11",()->waitUntilTime(val(44000))));
  final Action openFile= action("openFile",
    on("ubuntu_gnome",()->doubleClick(val(1872),val(915))),
    on("debian_gnome_x11",()->doubleClick(val(1925),val(939))),
    on("windows",()->doubleClick(val(503),val(254))));
  final Action managerOpened= action("managerOpened",
    on("ubuntu_gnome",()->waitUntilTime(val(52000))),
    on("debian_gnome_x11",()->waitUntilTime(val(60000))));
  final Action appsShownToEnd= action("appsShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(53500))),
    on("debian_gnome_x11",()->waitUntilTime(val(61500))));
  final Action terminalShownToEnd= action("terminalShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(57000))),
    on("debian_gnome_x11",()->waitUntilTime(val(66000))));
  final Action managerEnded= action("managerEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(67000))),
    on("debian_gnome_x11",()->waitUntilTime(val(76000))));
  final Action closeFiles= action("closeFiles",
    on("ubuntu_gnome",()->click(val(2374),val(844))),
    on("debian_gnome_x11",()->click(val(2341),val(844))),
    on("windows",()->click(val(1016),val(58))));
  final Action filesClosed= action("filesClosed",
    on("ubuntu_gnome",()->waitUntilTime(val(69000))),
    on("debian_gnome_x11",()->waitUntilTime(val(78000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var project= project("helloWorld");
    launchScript("first");
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    stabilize();
    assertFalse(Files.exists(filesIOFolder.resolve(data).resolve("projects.info")));
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
    var bare= pilot.shot();
    shell("xdg-open \""+project+"\"\n");
    runInTerminal(appsShownFiles,terminalShownFiles);
    filesShown.go();
    var files= Pilot.changed(bare,pilot.shot(),6);
    openFile.go();
    managerOpened.go();
    stabilize();
    checkContent(info,"""
      {
        "hello_world": {
          "path": "Str:%s",
          "kind": "idle"
        }
      }
      """.formatted(slashed(project)));
    endScript();
    runInTerminal(appsShownToEnd,terminalShownToEnd);
    managerEnded.go();
    closeFiles.go();
    filesClosed.go();
    assertArrayEquals(pixels(bare,files),pixels(pilot.shot(),files));
  }
  static int[] pixels(BufferedImage img, Rectangle r){ return img.getRGB(r.x,r.y,r.width,r.height,null,0,r.width); }
}

package agentTools;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import tools.Fs;

final class ProjectIconTest extends ManagerTest{
  final Action openPic= action("openPic",
    on("ubuntu_gnome",()->waitDoubleClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",()->waitDoubleClick(val(null),val(null),val(null))));
  final Action openPicMarker= action("openPicMarker",
    on("ubuntu_gnome",()->waitDoubleClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",()->waitDoubleClick(val(null),val(null),val(null))));
  final Action projectShown= action("projectShown",
    on("ubuntu_gnome",()->waitAfter(val(null))),
    on("xubuntu_xfce",()->waitAfter(val(null))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitAfter(val(17500))),
    on("xubuntu_xfce",()->waitAfter(val(21500))));
  final Action ownIconShown= action("ownIconShown",
    on("ubuntu_gnome",()->waitAfter(val(7000))),
    on("xubuntu_xfce",()->waitAfter(val(9000))));
  final Action invalidShown= action("invalidShown",
    on("ubuntu_gnome",()->waitAfter(val(7000))),
    on("xubuntu_xfce",()->waitAfter(val(8500))));
  final Action check= action("check",
    on("ubuntu_gnome",()->click(val(504),val(113))),
    on("xubuntu_xfce",()->click(val(454),val(93))));
  final Action checked= action("checked",
    on("ubuntu_gnome",()->waitAfter(val(4500))),
    on("xubuntu_xfce",()->waitAfter(val(6000))));
  final Action ownIconBack= action("ownIconBack",
    on("ubuntu_gnome",()->waitAfter(val(8500))),
    on("xubuntu_xfce",()->waitAfter(val(10500))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",()->waitAfter(val(7000))),
    on("xubuntu_xfce",()->waitAfter(val(8500))));
  @Override void walk() throws Throwable{
    var pic= files.resolve("pic");
    var icons= pic.resolve(".config").resolve("icon");
    Fs.writeUtf8(pic.resolve("pic.fearless"),"\n");
    Fs.writeUtf8(pic.resolve("_pic").resolve("_rank_app.fear"),"use base.Main as Main;\n\nHello:Main{s->base.Debug#(\"pic\")}\n");
    var bare= look();
    start();
    managerShown.go();
    toFiles();
    openPic.go();
    openPicMarker.go();
    closeFileManager.go();
    projectShown.go();
    var tile= inWindow(69,95,128,85);
    var output= inWindow(410,168,1000,80);
    var madeUp= look();
    png(icons.resolve("face.png"),0xC03090);
    stabilize();
    ownIconShown.go();
    changed(madeUp,tile);
    var own= look();
    png(icons.resolve("other.png"),0x30C090);
    stabilize();
    invalidShown.go();
    changed(own,tile);
    check.go();
    checked.go();
    changed(own,output);
    Files.delete(icons.resolve("other.png"));
    stabilize();
    ownIconBack.go();
    same(own,tile);
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    same(bare,tile);
  }
  static void png(Path file, int rgb) throws Exception{
    var img= new BufferedImage(48,48,BufferedImage.TYPE_INT_RGB);
    var g= img.createGraphics();
    g.setColor(new Color(rgb));
    g.fillRect(0,0,48,48);
    g.dispose();
    Fs.ensureDir(file.getParent());
    assertTrue(ImageIO.write(img,"png",file.toFile()));
  }
}

package agentTools;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.imageio.ImageIO;

import tools.Fs;

final class ProjectIconTest extends ManagerTest{
  final Action appsShown= action("appsShown",on("ubuntu_gnome",()->waitUntilTime(val(3500))));
  final Action terminalShown= action("terminalShown",on("ubuntu_gnome",()->waitUntilTime(val(8500))));
  final Action managerShown= action("managerShown",on("ubuntu_gnome",()->waitUntilTime(val(26000))));
  final Action ownIconShown= action("ownIconShown",on("ubuntu_gnome",()->waitUntilTime(val(33000))));
  final Action invalidShown= action("invalidShown",on("ubuntu_gnome",()->waitUntilTime(val(40000))));
  final Action check= action("check",on("ubuntu_gnome",()->click(val(504),val(113))));
  final Action checked= action("checked",on("ubuntu_gnome",()->waitUntilTime(val(44500))));
  final Action ownIconBack= action("ownIconBack",on("ubuntu_gnome",()->waitUntilTime(val(53000))));
  final Action managerQuit= action("managerQuit",on("ubuntu_gnome",()->waitUntilTime(val(60000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var pic= filesIOFolder.resolve("pic");
    var icons= pic.resolve(".config").resolve("icon");
    Fs.rmTree(pic);
    Fs.writeUtf8(pic.resolve("pic.fearless"),"\n");
    Fs.writeUtf8(pic.resolve("_pic").resolve("_rank_app.fear"),"use base.Main as Main;\n\nHello:Main{s->base.Debug#(\"pic\")}\n");
    launchScript("first",pic.resolve("pic.fearless"));
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    var tile= new Rectangle(69,95,128,85);
    var output= new Rectangle(410,168,1000,80);
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
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
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

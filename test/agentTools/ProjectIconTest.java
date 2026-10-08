package agentTools;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.imageio.ImageIO;

import tools.Fs;

final class ProjectIconTest extends ManagerTest{
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(18500))));
  final Action ownIconShown= action("ownIconShown",
    on("ubuntu_gnome",()->waitUntilTime(val(20000))));
  final Action madeUpIconShown= action("madeUpIconShown",
    on("ubuntu_gnome",()->waitUntilTime(val(21500))));
  final Action projectMenu= action("projectMenu",
    on("ubuntu_gnome",()->click(val(158),val(79))),
    on("windows",()->click(val(89),val(33))));
  final Action errorReport= action("errorReport",
    on("ubuntu_gnome",()->click(val(180),val(212))),
    on("windows",()->click(val(111),val(166))));
  final Action reportShown= action("reportShown",
    on("ubuntu_gnome",()->waitUntilTime(val(24200))));
  final Action focusReport= action("focusReport",
    on("ubuntu_gnome",()->click(val(1970),val(1100))),
    on("windows",()->click(val(660),val(300))));
  final Action ok= action("ok",
    on("ubuntu_gnome",()->click(val(1953),val(1323))),
    on("windows",()->click(val(639),val(568))));
  final Action reportGone= action("reportGone",
    on("ubuntu_gnome",()->waitUntilTime(val(26800))));
  final Action ownIconBack= action("ownIconBack",
    on("ubuntu_gnome",()->waitUntilTime(val(28300))));
  final Action appsShownToEnd= action("appsShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(29800))));
  final Action terminalShownToEnd= action("terminalShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(33300))));
  final Action managerEnded= action("managerEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(43300))));
  @Override void walk() throws Throwable{
    noManagerData();
    var pic= filesIOFolder.resolve("pic");
    var icons= pic.resolve(".config").resolve("icon");
    Fs.rmTree(pic);
    Fs.writeUtf8(pic.resolve("pic.fearless"),"\n");
    Fs.writeUtf8(pic.resolve("_pic").resolve("_rank_app.fear"),"use base.Main as Main;\n\nHello:Main{s->base.Debug#(\"pic\")}\n");
    launchScript("first",pic);
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    png(icons.resolve("face.png"),0xC03090);
    stabilize();
    ownIconShown.go();
    png(icons.resolve("other.png"),0x30C090);
    stabilize();
    madeUpIconShown.go();
    projectMenu.go();
    errorReport.go();
    reportShown.go();
    focusReport.go();
    keys(KeyEvent.VK_CONTROL,KeyEvent.VK_A);
    keys(KeyEvent.VK_CONTROL,KeyEvent.VK_C);
    ok.go();
    reportGone.go();
    Files.delete(icons.resolve("other.png"));
    stabilize();
    ownIconBack.go();
    endScript();
    runInTerminal(appsShownToEnd,terminalShownToEnd);
    managerEnded.go();
    stabilize();
    checkContent(List.of("first.exit"),"137\n");
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

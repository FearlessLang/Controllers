package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.image.BufferedImage;
import java.util.List;

import javax.imageio.ImageIO;

import tools.Fs;

final class ShortcutFileTest extends ManagerTest{
  final Action becomeCode= action("becomeCode",
    on("ubuntu_gnome",()->click(val(1012),val(113))),
    on("xubuntu_xfce",()->click(val(1024),val(93))));
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
    on("ubuntu_gnome",()->click(val(565),val(113))),
    on("xubuntu_xfce",()->click(val(529),val(93))));
  final Action compiled= action("compiled",
    on("ubuntu_gnome",()->waitUntilTime(val(51500))),
    on("xubuntu_xfce",()->waitUntilTime(val(64500))));
  final Action shortcutIcon= action("shortcutIcon",
    on("ubuntu_gnome",()->doubleClick(val(476),val(162))),
    on("xubuntu_xfce",()->doubleClick(val(409),val(142))));
  final Action ranFromManager= action("ranFromManager",
    on("ubuntu_gnome",()->waitUntilTime(val(58500))),
    on("xubuntu_xfce",()->waitUntilTime(val(73500))));
  final Action appsShownFiles= action("appsShownFiles",
    on("ubuntu_gnome",()->waitUntilTime(val(61500))),
    on("xubuntu_xfce",()->waitUntilTime(val(77000))));
  final Action terminalShownFiles= action("terminalShownFiles",
    on("ubuntu_gnome",()->waitUntilTime(val(66500))),
    on("xubuntu_xfce",()->waitUntilTime(val(83500))));
  final Action filesShown= action("filesShown",
    on("ubuntu_gnome",()->waitUntilTime(val(82000))),
    on("xubuntu_xfce",()->waitUntilTime(val(102500))));
  final Action openShortcut= action("openShortcut",
    on("ubuntu_gnome",()->doubleClick(val(912),val(375))),
    on("xubuntu_xfce",()->doubleClick(val(1015),val(425))));
  final Action ranFromFiles= action("ranFromFiles",
    on("ubuntu_gnome",()->waitUntilTime(val(92000))),
    on("xubuntu_xfce",()->waitUntilTime(val(115000))));
  final Action closeFiles= action("closeFiles",
    on("ubuntu_gnome",()->click(val(1414),val(303))),
    on("xubuntu_xfce",()->click(val(1275),val(309))));
  final Action filesClosed= action("filesClosed",
    on("ubuntu_gnome",()->waitUntilTime(val(95500))),
    on("xubuntu_xfce",()->waitUntilTime(val(119500))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",()->waitUntilTime(val(102500))),
    on("xubuntu_xfce",()->waitUntilTime(val(128500))));
  @Override void walk() throws Throwable{
    noManagerData();
    var shortcuts= filesIOFolder.resolve("shortcuts");
    Fs.rmTree(shortcuts);
    Fs.writeUtf8(shortcuts.resolve("shortcuts.fearless"),"\n");
    Fs.writeUtf8(shortcuts.resolve("_hello").resolve("_rank_app.fear"),"use base.Main as Main;\nuse base.Shortcut as Shortcut;\n\nHello:Main, Shortcut[IconsHello]{s->base.Debug#(`hello`)}\n");
    Fs.ensureDir(shortcuts.resolve("_hello").resolve("icons"));
    ImageIO.write(new BufferedImage(64,64,BufferedImage.TYPE_INT_RGB),"png",shortcuts.resolve("_hello").resolve("icons").resolve("hello.png").toFile());
    launchScript("first",shortcuts.resolve("shortcuts.fearless"));
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    becomeCode.go();
    codeShown.go();
    compile.go();
    compiled.go();
    stabilize();
    assertEquals(List.of("hello.fapp"),Fs.walk(shortcuts,s->s.filter(p->p.getParent().equals(shortcuts)).map(p->p.getFileName().toString().replaceAll("[0-9]{3}$","")).filter(n->n.startsWith("hello.")).toList()));
    var output= inWindow(410,256,1000,60);
    var compiledLook= look();
    shortcutIcon.go();
    ranFromManager.go();
    changed(compiledLook,output);
    var ran= look();
    shell("nohup setsid -f xdg-open \""+shortcuts+"\" >/dev/null 2>&1\n");
    runInTerminal(appsShownFiles,terminalShownFiles);
    filesShown.go();
    openShortcut.go();
    ranFromFiles.go();
    closeFiles.go();
    filesClosed.go();
    stabilize();
    assert differing(ran,look(),output) == 0;
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
  }
}

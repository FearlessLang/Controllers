package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.imageio.ImageIO;

import tools.Fs;

final class ShortcutFileTest extends ManagerTest{
  static final List<String> console= List.of(data,"eclipse","shortcuts","console.txt");
  static final String compiled= "--- compiling shortcuts ---\n--- compile done ---\n";
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(18000))));
  final Action focusTiles= action("focusTiles",
    on("windows",()->click(val(200),val(400))));
  final Action becomeCode= action("becomeCode",
    on("windows",()->click(val(332),val(94))));
  final Action codeShown= action("codeShown",
    on("ubuntu_gnome",()->waitUntilTime(val(21500))));
  final Action compile= action("compile",
    on("windows",()->click(val(102),val(67))));
  final Action compileDone= action("compileDone",
    on("ubuntu_gnome",()->waitUntilTime(val(35000))));
  final Action appsShownFiles= action("appsShownFiles",
    on("ubuntu_gnome",()->waitUntilTime(val(36500))));
  final Action terminalShownFiles= action("terminalShownFiles",
    on("ubuntu_gnome",()->waitUntilTime(val(40000))));
  final Action filesShown= action("filesShown",
    on("ubuntu_gnome",()->waitUntilTime(val(53000))));
  final Action openShortcut= action("openShortcut",
    on("windows",()->doubleClick(val(500),val(283))));
  final Action shortcutRan= action("shortcutRan",
    on("ubuntu_gnome",()->waitUntilTime(val(60000))));
  final Action appsShownToEnd= action("appsShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(61500))));
  final Action terminalShownToEnd= action("terminalShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(65000))));
  final Action managerEnded= action("managerEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(75000))));
  final Action closeFiles= action("closeFiles",
    on("windows",()->click(val(1016),val(58))));
  @Override void walk() throws Throwable{
    noManagerData();
    var shortcuts= filesIOFolder.resolve("shortcuts");
    Fs.rmTree(shortcuts);
    Fs.writeUtf8(shortcuts.resolve("shortcuts.fearless"),"\n");
    Fs.writeUtf8(shortcuts.resolve("_hello").resolve("_rank_app.fear"),"use base.Main as Main;\nuse base.Shortcut as Shortcut;\n\nHello:Main, Shortcut[IconsHello]{s->base.Debug#(`hello`)}\n");
    Fs.ensureDir(shortcuts.resolve("_hello").resolve("icons"));
    ImageIO.write(new BufferedImage(64,64,BufferedImage.TYPE_INT_RGB),"png",shortcuts.resolve("_hello").resolve("icons").resolve("hello.png").toFile());
    launchScript("first",shortcuts);
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    focusTiles.go();
    keys(KeyEvent.VK_F8);
    keys(KeyEvent.VK_HOME);
    becomeCode.go();
    codeShown.go();
    compile.go();
    compileDone.go();
    stabilize();
    checkContent(console,compiled);
    assertEquals(List.of("hello.fapp"),Fs.walk(shortcuts,s->s.filter(p->p.getParent().equals(shortcuts)).map(p->p.getFileName().toString().replaceAll("[0-9]{3}$","")).filter(n->n.startsWith("hello.")).toList()));
    shell("xdg-open \""+shortcuts+"\"\n");
    runInTerminal(appsShownFiles,terminalShownFiles);
    filesShown.go();
    openShortcut.go();
    shortcutRan.go();
    stabilize();
    checkContent(console,compiled+"--- running hello.Hello ---\nhello\n--- hello.Hello exited with 0 after [###]s ---\n");
    endScript();
    runInTerminal(appsShownToEnd,terminalShownToEnd);
    managerEnded.go();
    closeFiles.go();
    stabilize();
    checkContent(List.of("first.exit"),"137\n");
  }
}

package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.image.BufferedImage;
import java.util.List;

import javax.imageio.ImageIO;

import tools.Fs;

final class ShortcutFileTest extends ManagerTest{
  final Action becomeCode= action("becomeCode",
    on("ubuntu_gnome",() -> click(val(1012),val(113))),
    on("xubuntu_xfce",() -> click(val(1024),val(93))));
  final Action openShortcuts= action("openShortcuts",
    on("ubuntu_gnome",() -> waitDoubleClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",() -> waitDoubleClick(val(null),val(null),val(null))));
  final Action openShortcutsMarker= action("openShortcutsMarker",
    on("ubuntu_gnome",() -> waitDoubleClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",() -> waitDoubleClick(val(null),val(null),val(null))));
  final Action projectShown= action("projectShown",
    on("ubuntu_gnome",() -> waitAfter(val(null))),
    on("xubuntu_xfce",() -> waitAfter(val(null))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",() -> waitAfter(val(17500))),
    on("xubuntu_xfce",() -> waitAfter(val(21500))));
  final Action codeShown= action("codeShown",
    on("ubuntu_gnome",() -> waitAfter(val(4500))),
    on("xubuntu_xfce",() -> waitAfter(val(6000))));
  final Action compile= action("compile",
    on("ubuntu_gnome",() -> click(val(565),val(113))),
    on("xubuntu_xfce",() -> click(val(529),val(93))));
  final Action compiled= action("compiled",
    on("ubuntu_gnome",() -> waitAfter(val(21000))),
    on("xubuntu_xfce",() -> waitAfter(val(26000))));
  final Action shortcutIcon= action("shortcutIcon",
    on("ubuntu_gnome",() -> doubleClick(val(476),val(162))),
    on("xubuntu_xfce",() -> doubleClick(val(409),val(142))));
  final Action ranFromManager= action("ranFromManager",
    on("ubuntu_gnome",() -> waitAfter(val(7000))),
    on("xubuntu_xfce",() -> waitAfter(val(9000))));
  final Action clearOutput= action("clearOutput",
    on("ubuntu_gnome",() -> waitClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",() -> waitClick(val(null),val(null),val(null))));
  final Action outputCleared= action("outputCleared",
    on("ubuntu_gnome",() -> waitAfter(val(null))),
    on("xubuntu_xfce",() -> waitAfter(val(null))));
  final Action openHelloFapp= action("openHelloFapp",
    on("ubuntu_gnome",() -> waitDoubleClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",() -> waitDoubleClick(val(null),val(null),val(null))));
  final Action ranFromFiles= action("ranFromFiles",
    on("ubuntu_gnome",() -> waitAfter(val(10000))),
    on("xubuntu_xfce",() -> waitAfter(val(12500))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",() -> waitAfter(val(7000))),
    on("xubuntu_xfce",() -> waitAfter(val(9000))));
  @Override void walk() throws Throwable{
    var shortcuts= files.resolve("shortcuts");
    Fs.writeUtf8(shortcuts.resolve("shortcuts.fearless"),"\n");
    Fs.writeUtf8(shortcuts.resolve("_hello").resolve("_rank_app.fear"),"use base.Main as Main;\nuse base.Shortcut as Shortcut;\n\nHello:Main, Shortcut[IconsHello]{s->base.Debug#(`hello`)}\n");
    Fs.ensureDir(shortcuts.resolve("_hello").resolve("icons"));
    ImageIO.write(new BufferedImage(64,64,BufferedImage.TYPE_INT_RGB),"png",shortcuts.resolve("_hello").resolve("icons").resolve("hello.png").toFile());
    var bare= look();
    start();
    managerShown.go();
    toFiles();
    openShortcuts.go();
    openShortcutsMarker.go();
    closeFiles();
    projectShown.go();
    becomeCode.go();
    codeShown.go();
    compile.go();
    compiled.go();
    stabilize();
    assertEquals(List.of("hello.fapp"),Fs.walk(shortcuts,s -> s.filter(p -> p.getParent().equals(shortcuts)).map(p -> p.getFileName().toString().replaceAll("[0-9]{3}$","")).filter(n -> n.startsWith("hello.")).toList()));
    var output= inWindow(410,256,1000,60);
    var compiledLook= look();
    shortcutIcon.go();
    ranFromManager.go();
    changed(compiledLook,output);
    clearOutput.go();
    outputCleared.go();
    var cleared= look();
    toFiles();
    openShortcuts.go();
    openHelloFapp.go();
    closeFiles();
    ranFromFiles.go();
    changed(cleared,output);
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    same(bare,output);
  }
}

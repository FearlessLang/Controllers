package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Rectangle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import tools.Fs;
import utils.OneOr;

final class ConnectEclipseTest extends ManagerTest{
  final Path ide= filesIOFolder.resolve("ide");
  final Path elsewhere= filesIOFolder.resolve("elsewhere");
  final Path dropins= ide.resolve("dropins");
  final Path plugins= dropins.resolve("fearless").resolve("plugins");
  final Path lib= app.resolve("lib").resolve("app");
  final Path shipped= lib.resolve("eclipsePlugin");
  final Action appsShown= action("appsShown",on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",on("ubuntu_gnome",()->waitUntilTime(val(18000))));
  final Action chooserShown= action("chooserShown",on("ubuntu_gnome",()->waitUntilTime(val(21500))));
  final Action fileName= action("fileName",on("ubuntu_gnome",()->click(val(2000),val(1180))));
  final Action noteShown= action("noteShown",on("ubuntu_gnome",()->waitUntilTime(val(33000))));
  final Action chooserShownAgain= action("chooserShownAgain",on("ubuntu_gnome",()->waitUntilTime(val(37000))));
  final Action connected= action("connected",on("ubuntu_gnome",()->waitUntilTime(val(48000))));
  final Action managerQuit= action("managerQuit",on("ubuntu_gnome",()->waitUntilTime(val(53000))));
  @Override void walk() throws Throwable{
    noManagerData();
    Fs.rmTree(ide);
    Fs.rmTree(elsewhere);
    Fs.ensureDir(elsewhere.resolve("nothing"));
    Fs.writeUtf8(ide.resolve(".eclipseproduct"),"name=Eclipse Platform\n");
    Fs.writeUtf8(plugins.resolve("fearlessPluginProject_3.2.0.jar"),"older\n");
    var jar= Fs.walk(shipped,s->OneOr.of("one plugin",s.filter(Files::isRegularFile))).getFileName().toString();
    launchScript("first");
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    var center= new Rectangle(1720,1000,400,160);
    var before= look();
    managerMenu.go();
    connectEclipse.go();
    chooserShown.go();
    fileName.go();
    type(elsewhere+"\n");
    noteShown.go();
    changed(before,center);
    stabilize();
    assertEquals(List.of(".eclipseproduct","dropins/fearless/plugins/fearlessPluginProject_3.2.0.jar"),files());
    closeDialog.go();
    managerMenu.go();
    connectEclipse.go();
    chooserShownAgain.go();
    fileName.go();
    type(dropins+"\n");
    connected.go();
    same(before,center);
    stabilize();
    assertEquals(List.of(".eclipseproduct","dropins/fearless/manager.info","dropins/fearless/plugins/"+jar),files());
    assertEquals(-1L,Files.mismatch(shipped.resolve(jar),plugins.resolve(jar)));
    checkContent(List.of("ide","dropins","fearless","manager.info"),"""
      {
        "program": "Str:%s",
        "launcher": "Str:%s",
        "manager": "Str:%s",
        "baseCache": "Str:%s"
      }
      """.formatted(escaped(app),escaped(app.resolve("bin").resolve(app.getFileName())),escaped(filesIOFolder.resolve(data)),escaped(lib.resolve("stdLib").resolve("baseCache"))));
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
  }
  List<String> files(){ return Fs.walk(ide,s->s.filter(Files::isRegularFile).map(p->slashed(ide.relativize(p))).sorted().toList()); }
}

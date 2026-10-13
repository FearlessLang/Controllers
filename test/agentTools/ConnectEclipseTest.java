package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Rectangle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import tools.Fs;
import utils.OneOr;

final class ConnectEclipseTest extends ManagerTest{
  final Path ide= files.resolve("ide");
  final Path elsewhere= files.resolve("elsewhere");
  final Path dropins= ide.resolve("dropins");
  final Path plugins= dropins.resolve("fearless").resolve("plugins");
  final Path lib= app.resolve("lib").resolve("app");
  final Path shipped= lib.resolve("eclipsePlugin");
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitAfter(val(17500))),
    on("xubuntu_xfce",()->waitAfter(val(21500))));
  final Action chooserShown= action("chooserShown",
    on("ubuntu_gnome",()->waitAfter(val(5000))),
    on("xubuntu_xfce",()->waitAfter(val(6500))));
  final Action fileName= action("fileName",
    on("ubuntu_gnome",()->click(val(1040),val(640))),
    on("xubuntu_xfce",()->click(val(1007),val(637))));
  final Action openButton= action("openButton",
    on("ubuntu_gnome",()->waitClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",()->waitClick(val(null),val(null),val(null))));
  final Action noteShown= action("noteShown",
    on("ubuntu_gnome",()->waitAfter(val(16000))),
    on("xubuntu_xfce",()->waitAfter(val(20000))));
  final Action okButton= action("okButton",
    on("ubuntu_gnome",()->waitClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",()->waitClick(val(null),val(null),val(null))));
  final Action chooserShownAgain= action("chooserShownAgain",
    on("ubuntu_gnome",()->waitAfter(val(5500))),
    on("xubuntu_xfce",()->waitAfter(val(7000))));
  final Action connected= action("connected",
    on("ubuntu_gnome",()->waitAfter(val(15500))),
    on("xubuntu_xfce",()->waitAfter(val(19000))));
  final Action okButtonConnected= action("okButtonConnected",
    on("ubuntu_gnome",()->waitClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",()->waitClick(val(null),val(null),val(null))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",()->waitAfter(val(7000))),
    on("xubuntu_xfce",()->waitAfter(val(9000))));
  @Override void walk() throws Throwable{
    Fs.ensureDir(elsewhere.resolve("nothing"));
    Fs.writeUtf8(ide.resolve(".eclipseproduct"),"name=Eclipse Platform\n");
    Fs.writeUtf8(plugins.resolve("fearlessPluginProject_3.2.0.jar"),"older\n");
    var jar= Fs.walk(shipped,s->OneOr.of("one plugin",s.filter(Files::isRegularFile))).getFileName().toString();
    var bare= look();
    start();
    managerShown.go();
    var center= new Rectangle(760,460,400,160);
    var before= look();
    managerMenu.go();
    connectEclipse.go();
    chooserShown.go();
    fileName.go();
    type(elsewhere.toString());
    openButton.go();
    noteShown.go();
    changed(before,center);
    stabilize();
    assertEquals(List.of(".eclipseproduct","dropins/fearless/plugins/fearlessPluginProject_3.2.0.jar"),ideFiles());
    okButton.go();
    managerMenu.go();
    connectEclipse.go();
    chooserShownAgain.go();
    fileName.go();
    type(dropins.toString());
    openButton.go();
    connected.go();
    changed(before,center);
    stabilize();
    assertEquals(List.of(".eclipseproduct","dropins/fearless/manager.info","dropins/fearless/plugins/"+jar),ideFiles());
    assertEquals(-1L,Files.mismatch(shipped.resolve(jar),plugins.resolve(jar)));
    checkContent(files.resolve("ide/dropins/fearless/manager.info"),"""
      {
        "program": "Str:%s",
        "launcher": "Str:%s",
        "manager": "Str:%s",
        "baseCache": "Str:%s"
      }
      """.formatted(escaped(app),escaped(app.resolve("bin").resolve(app.getFileName())),escaped(filesIOFolder.resolve(data)),escaped(lib.resolve("stdLib").resolve("baseCache"))));
    okButtonConnected.go();
    same(before,center);
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    same(bare,center);
  }
  List<String> ideFiles(){ return Fs.walk(ide,s->s.filter(Files::isRegularFile).map(p->slashed(ide.relativize(p))).sorted().toList()); }
}

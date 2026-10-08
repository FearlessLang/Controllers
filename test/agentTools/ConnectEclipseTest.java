package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import tools.Fs;
import utils.OneOr;

final class ConnectEclipseTest extends ManagerTest{
  final Path ide= filesIOFolder.resolve("ide");
  final Path dropins= ide.resolve("dropins");
  final Path plugins= dropins.resolve("fearless").resolve("plugins");
  final Path lib= app.resolve("lib").resolve("app");
  final Path shipped= lib.resolve("eclipsePlugin");
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(18500))));
  final Action managerMenu= action("managerMenu",
    on("ubuntu_gnome",()->click(val(98),val(79))),
    on("windows",()->click(val(31),val(33))));
  final Action connectEclipse= action("connectEclipse",
    on("ubuntu_gnome",()->click(val(128),val(166))),
    on("windows",()->click(val(61),val(121))));
  final Action chooserShown= action("chooserShown",
    on("ubuntu_gnome",()->waitUntilTime(val(21500))));
  final Action fileName= action("fileName",
    on("ubuntu_gnome",()->click(val(2000),val(1180))),
    on("windows",()->click(val(685),val(416))));
  final Action noteShown= action("noteShown",
    on("ubuntu_gnome",()->waitUntilTime(val(37000))));
  final Action ok= action("ok",
    on("ubuntu_gnome",()->click(val(1952),val(1164))),
    on("windows",()->click(val(639),val(396))));
  final Action managerMenuAgain= action("managerMenuAgain",
    on("ubuntu_gnome",()->click(val(98),val(79))),
    on("windows",()->click(val(31),val(33))));
  final Action connectEclipseAgain= action("connectEclipseAgain",
    on("ubuntu_gnome",()->click(val(128),val(166))),
    on("windows",()->click(val(61),val(121))));
  final Action chooserShownAgain= action("chooserShownAgain",
    on("ubuntu_gnome",()->waitUntilTime(val(40500))));
  final Action fileNameAgain= action("fileNameAgain",
    on("ubuntu_gnome",()->click(val(2000),val(1180))),
    on("windows",()->click(val(685),val(416))));
  final Action noteShownAgain= action("noteShownAgain",
    on("ubuntu_gnome",()->waitUntilTime(val(54000))));
  final Action okAgain= action("okAgain",
    on("ubuntu_gnome",()->click(val(1952),val(1174))),
    on("windows",()->click(val(639),val(405))));
  final Action appsShownToEnd= action("appsShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(56500))));
  final Action terminalShownToEnd= action("terminalShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(60000))));
  final Action managerEnded= action("managerEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(70000))));
  @Override void walk() throws Throwable{
    noManagerData();
    Fs.rmTree(ide);
    Fs.writeUtf8(ide.resolve(".eclipseproduct"),"name=Eclipse Platform\n");
    Fs.writeUtf8(plugins.resolve("fearlessPluginProject_3.2.0.jar"),"older\n");
    var jar= Fs.walk(shipped,s->OneOr.of("one plugin",s.filter(Files::isRegularFile))).getFileName().toString();
    launchScript("first");
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    managerMenu.go();
    connectEclipse.go();
    chooserShown.go();
    fileName.go();
    type(dropins+"\n");
    noteShown.go();
    var none= """
      Eclipse is not connected: no Eclipse installation, a folder holding the file ".eclipseproduct", is in
        %s
      or in its folders "eclipse" or "Contents/Eclipse", or in those of a folder of it.

      Select the Eclipse program, the folder holding it, or the folder Eclipse was unzipped into.
      """.formatted(dropins);
    stabilize();
    checkContent(notes,none);
    assertEquals(List.of(".eclipseproduct","dropins/fearless/plugins/fearlessPluginProject_3.2.0.jar"),files());
    ok.go();
    managerMenuAgain.go();
    connectEclipseAgain.go();
    chooserShownAgain.go();
    fileNameAgain.go();
    type(ide+"\n");
    noteShownAgain.go();
    stabilize();
    checkContent(notes,none+"""
      Eclipse is now connected:
      %s

      Restart Eclipse: every project this manager knows appears in its Fearless
      perspective. File > New makes a project, Project > Build compiles it, the
      Run button runs it, and the Terminate button of its console stops it.
      """.formatted(ide));
    assertEquals(List.of(".eclipseproduct","dropins/fearless/manager.info","dropins/fearless/plugins/"+jar),files());
    assertEquals(-1L,Files.mismatch(shipped.resolve(jar),plugins.resolve(jar)));
    checkContent(List.of("ide","dropins","fearless","manager.info"),"""
      {
        "manager": "Str:%s",
        "baseCache": "Str:%s"
      }
      """.formatted(escaped(filesIOFolder.resolve(data)),escaped(lib.resolve("stdLib").resolve("baseCache"))));
    okAgain.go();
    endScript();
    runInTerminal(appsShownToEnd,terminalShownToEnd);
    managerEnded.go();
    stabilize();
    checkContent(List.of("first.exit"),"137\n");
  }
  List<String> files(){ return Fs.walk(ide,s->s.filter(Files::isRegularFile).map(p->slashed(ide.relativize(p))).sorted().toList()); }
}

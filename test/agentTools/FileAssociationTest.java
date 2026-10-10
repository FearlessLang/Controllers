package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import tools.Fs;

final class FileAssociationTest extends ManagerTest{
  final Action appsShownForFiles= action("appsShownForFiles",
    on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShownForFiles= action("terminalShownForFiles",
    on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action filesShown= action("filesShown",
    on("ubuntu_gnome",()->waitUntilTime(val(18000))));
  final Action focusFiles= action("focusFiles",
    on("ubuntu_gnome",()->click(val(2200),val(1200))),
    on("windows",()->click(val(700),val(450))));
  final Action genericShown= action("genericShown",
    on("ubuntu_gnome",()->waitUntilTime(val(22000))));
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(23500))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(27000))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(40000))));
  final Action sendManagerAway= action("sendManagerAway",
    on("ubuntu_gnome",()->click(val(3754),val(48))),
    on("windows",()->click(val(1162),val(11))));
  final Action managerAway= action("managerAway",
    on("ubuntu_gnome",()->waitUntilTime(val(43000))));
  final Action iconChanged= action("iconChanged",
    on("ubuntu_gnome",()->waitUntilTime(val(47000))));
  final Action bringManagerBack= action("bringManagerBack",
    on("ubuntu_gnome",()->click(val(32),val(386))),
    on("windows",()->click(val(860),val(696))));
  final Action managerBack= action("managerBack",
    on("ubuntu_gnome",()->waitUntilTime(val(51000))));
  final Action managerMenu= action("managerMenu",
    on("ubuntu_gnome",()->click(val(98),val(79))),
    on("windows",()->click(val(31),val(33))));
  final Action forgetAssociation= action("forgetAssociation",
    on("ubuntu_gnome",()->click(val(128),val(191))),
    on("windows",()->click(val(61),val(145))));
  final Action dialogShown= action("dialogShown",
    on("ubuntu_gnome",()->waitUntilTime(val(55500))));
  final Action yes= action("yes",
    on("ubuntu_gnome",()->click(val(1928),val(1154))),
    on("windows",()->click(val(614),val(387))));
  final Action managerForgot= action("managerForgot",
    on("ubuntu_gnome",()->waitUntilTime(val(58500))));
  final Action iconReverted= action("iconReverted",
    on("ubuntu_gnome",()->waitUntilTime(val(62500))));
  final Action appsShownAgain= action("appsShownAgain",
    on("ubuntu_gnome",()->waitUntilTime(val(64000))));
  final Action terminalShownAgain= action("terminalShownAgain",
    on("ubuntu_gnome",()->waitUntilTime(val(67500))));
  final Action managerShownAgain= action("managerShownAgain",
    on("ubuntu_gnome",()->waitUntilTime(val(80500))));
  final Action sendManagerAwayAgain= action("sendManagerAwayAgain",
    on("ubuntu_gnome",()->click(val(3754),val(48))),
    on("windows",()->click(val(1162),val(11))));
  final Action managerAwayAgain= action("managerAwayAgain",
    on("ubuntu_gnome",()->waitUntilTime(val(83500))));
  final Action iconChangedAgain= action("iconChangedAgain",
    on("ubuntu_gnome",()->waitUntilTime(val(87500))));
  final Action appsShownToEnd= action("appsShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(89000))));
  final Action terminalShownToEnd= action("terminalShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(92500))));
  final Action managerEnded= action("managerEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(102500))));
  final Action closeFiles= action("closeFiles",
    on("ubuntu_gnome",()->click(val(2374),val(842))),
    on("windows",()->click(val(1016),val(58))));
  @Override void walk() throws Throwable{
    noManagerData();
    noAssociation();
    var project= project("helloWorld");
    var bare= look();
    shell("nohup setsid -f xdg-open \""+project+"\" >/dev/null 2>&1\n");
    runInTerminal(appsShownForFiles,terminalShownForFiles);
    filesShown.go();
    var files= changed("Opening the folder shows the file manager window",bare,look());
    reload(genericShown);
    var generic= look();
    launchScript("first");
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    sendManagerAway.go();
    managerAway.go();
    reload(iconChanged);
    var fearless= look();
    var icon= changed("The manager makes the desk show the Fearless icon on hello_world.fearless",generic,fearless);
    bringManagerBack.go();
    managerBack.go();
    managerMenu.go();
    forgetAssociation.go();
    dialogShown.go();
    yes.go();
    managerForgot.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
    reload(iconReverted);
    same("Forget association brings the generic icon back",generic,look(),icon);
    launchScript("second");
    runInTerminal(appsShownAgain,terminalShownAgain);
    managerShownAgain.go();
    sendManagerAwayAgain.go();
    managerAwayAgain.go();
    reload(iconChangedAgain);
    same("The next start shows the Fearless icon again",fearless,look(),icon);
    endScript();
    runInTerminal(appsShownToEnd,terminalShownToEnd);
    managerEnded.go();
    stabilize();
    checkContent(List.of("second.exit"),"137\n");
    closeFiles.go();
    same("Closing the file manager window shows the desk as before",bare,look(),files);
  }
  void noAssociation() throws Throwable{
    if (!hostDesk){ return; }
    var share= Path.of(System.getProperty("user.home"),".local","share");
    for (var dir: List.of(share.resolve("applications"),share.resolve("mime").resolve("packages"))){
      Fs.walkV(dir,s->s.filter(p->p.getFileName().toString().contains("earless")).toList().forEach(p->Fs.ofV(()->Files.delete(p))));
    }
    assertEquals(0,new ProcessBuilder("update-mime-database",share.resolve("mime").toString()).start().waitFor());
    assertEquals(0,new ProcessBuilder("update-desktop-database",share.resolve("applications").toString()).start().waitFor());
  }
  void reload(Action reloaded) throws Throwable{
    focusFiles.go();
    keys(KeyEvent.VK_F5);
    reloaded.go();
  }
}

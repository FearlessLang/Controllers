package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import tools.Fs;

final class FileAssociationTest extends ManagerTest{
  final Action appsShown= action("appsShown",on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",on("ubuntu_gnome",()->waitUntilTime(val(18000))));
  final Action appsShownFiles= action("appsShownFiles",on("ubuntu_gnome",()->waitUntilTime(val(19500))));
  final Action terminalShownFiles= action("terminalShownFiles",on("ubuntu_gnome",()->waitUntilTime(val(23000))));
  final Action filesShown= action("filesShown",on("ubuntu_gnome",()->waitUntilTime(val(34000))));
  final Action focusFiles= action("focusFiles",on("ubuntu_gnome",()->click(val(2200),val(1200))));
  final Action associatedShown= action("associatedShown",on("ubuntu_gnome",()->waitUntilTime(val(36000))));
  final Action managerMenuBehind= action("managerMenuBehind",on("ubuntu_gnome",()->click(val(99),val(79))));
  final Action managerReset= action("managerReset",on("ubuntu_gnome",()->waitUntilTime(val(41000))));
  final Action genericShown= action("genericShown",on("ubuntu_gnome",()->waitUntilTime(val(46000))));
  final Action appsShownAgain= action("appsShownAgain",on("ubuntu_gnome",()->waitUntilTime(val(47500))));
  final Action terminalShownAgain= action("terminalShownAgain",on("ubuntu_gnome",()->waitUntilTime(val(51000))));
  final Action managerShownAgain= action("managerShownAgain",on("ubuntu_gnome",()->waitUntilTime(val(64000))));
  final Action associatedAgain= action("associatedAgain",on("ubuntu_gnome",()->waitUntilTime(val(76000))));
  final Action managerQuit= action("managerQuit",on("ubuntu_gnome",()->waitUntilTime(val(81000))));
  final Action closeFiles= action("closeFiles",on("ubuntu_gnome",()->click(val(2374),val(843))));
  @Override void walk() throws Throwable{
    noManagerData();
    var project= project("helloWorld");
    var icon= new Rectangle(1840,880,64,64);
    launchScript("first");
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    shell("nohup setsid -f xdg-open \""+project+"\" >/dev/null 2>&1\n");
    runInTerminal(appsShownFiles,terminalShownFiles);
    filesShown.go();
    reload(associatedShown);
    var associated= look();
    managerMenuBehind.go();
    resetManager.go();
    managerReset.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
    assertFalse(Files.exists(filesIOFolder.resolve(data)));
    reload(genericShown);
    changed(associated,icon);
    launchScript("second");
    runInTerminal(appsShownAgain,terminalShownAgain);
    managerShownAgain.go();
    associatedAgain.go();
    var query= Fs.of(()->new ProcessBuilder("xdg-mime","query","default","application/x-fearless").start());
    assertEquals(app.getFileName()+".desktop\n",new String(query.getInputStream().readAllBytes(),StandardCharsets.UTF_8));
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("second.exit"),"0\n");
    closeFiles.go();
  }
  void reload(Action reloaded) throws Throwable{
    focusFiles.go();
    keys(KeyEvent.VK_F5);
    reloaded.go();
  }
}

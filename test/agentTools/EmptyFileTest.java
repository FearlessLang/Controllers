package agentTools;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.imageio.ImageIO;

import tools.Fs;

final class EmptyFileTest extends ManagerTest{
  final Action becomeCode= header("becomeCode",985);
  final Action appsShown= action("appsShown",on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",on("ubuntu_gnome",()->waitUntilTime(val(18000))));
  final Action codeShown= action("codeShown",on("ubuntu_gnome",()->waitUntilTime(val(21000))));
  final Action compile= action("compile",on("ubuntu_gnome",()->click(val(538),val(113))));
  final Action compiled= action("compiled",on("ubuntu_gnome",()->waitUntilTime(val(36000))));
  final Action appsShownEmpty= action("appsShownEmpty",on("ubuntu_gnome",()->waitUntilTime(val(37500))));
  final Action terminalShownEmpty= action("terminalShownEmpty",on("ubuntu_gnome",()->waitUntilTime(val(41000))));
  final Action noteShown= action("noteShown",on("ubuntu_gnome",()->waitUntilTime(val(49000))));
  final Action appsShownNote= action("appsShownNote",on("ubuntu_gnome",()->waitUntilTime(val(51000))));
  final Action terminalShownNote= action("terminalShownNote",on("ubuntu_gnome",()->waitUntilTime(val(54500))));
  final Action noteRun= action("noteRun",on("ubuntu_gnome",()->waitUntilTime(val(64000))));
  final Action managerQuit= action("managerQuit",on("ubuntu_gnome",()->waitUntilTime(val(69000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var opens= filesIOFolder.resolve("blanks");
    var icons= opens.resolve("_blanks").resolve("icons");
    Fs.rmTree(opens);
    Fs.writeUtf8(opens.resolve("blanks.fearless"),"\n");
    Fs.writeUtf8(opens.resolve("_blanks").resolve("_rank_app.fear"),"use base.Main as Main;\nuse base.OpenWith as OpenWith;\n\nRead:Main, OpenWith[IconsRead,`ffile321`]{s->base.Debug#(`read`)}\n");
    Fs.ensureDir(icons);
    Fs.ofV(()->ImageIO.write(new BufferedImage(64,64,BufferedImage.TYPE_INT_RGB),"png",icons.resolve("read.png").toFile()));
    Fs.writeUtf8(opens.resolve("blank.ffile321"),"");
    Fs.writeUtf8(opens.resolve("note.ffile321"),"dear reader\n");
    launchScript("first",opens.resolve("blanks.fearless"));
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    becomeCode.go();
    codeShown.go();
    compile.go();
    compiled.go();
    var center= new Rectangle(1720,1000,400,160);
    var output= new Rectangle(410,170,1000,90);
    var compiledLook= look();
    launchScript("second",opens.resolve("blank.ffile321"));
    runInTerminal(appsShownEmpty,terminalShownEmpty);
    noteShown.go();
    changed(compiledLook,center);
    same(compiledLook,output);
    stabilize();
    checkContent(List.of("second.exit"),"0\n");
    closeDialog.go();
    same(compiledLook,center);
    launchScript("third",opens.resolve("note.ffile321"));
    runInTerminal(appsShownNote,terminalShownNote);
    noteRun.go();
    same(compiledLook,center);
    changed(compiledLook,output);
    stabilize();
    checkContent(List.of("third.exit"),"0\n");
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
  }
}

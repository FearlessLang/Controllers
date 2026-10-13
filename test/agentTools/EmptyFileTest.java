package agentTools;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.imageio.ImageIO;

import tools.Fs;

final class EmptyFileTest extends ManagerTest{
  final Action becomeCode= action("becomeCode",
    on("ubuntu_gnome",()->click(val(985),val(113))),
    on("xubuntu_xfce",()->click(val(992),val(93))));
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitAfter(val(3500))),
    on("xubuntu_xfce",()->waitAfter(val(4500))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitAfter(val(5000))),
    on("xubuntu_xfce",()->waitAfter(val(6500))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitAfter(val(17500))),
    on("xubuntu_xfce",()->waitAfter(val(21500))));
  final Action codeShown= action("codeShown",
    on("ubuntu_gnome",()->waitAfter(val(4500))),
    on("xubuntu_xfce",()->waitAfter(val(6000))));
  final Action compile= action("compile",
    on("ubuntu_gnome",()->click(val(538),val(113))),
    on("xubuntu_xfce",()->click(val(498),val(93))));
  final Action compiled= action("compiled",
    on("ubuntu_gnome",()->waitAfter(val(21000))),
    on("xubuntu_xfce",()->waitAfter(val(26000))));
  final Action appsShownEmpty= action("appsShownEmpty",
    on("ubuntu_gnome",()->waitAfter(val(3000))),
    on("xubuntu_xfce",()->waitAfter(val(4000))));
  final Action terminalShownEmpty= action("terminalShownEmpty",
    on("ubuntu_gnome",()->waitAfter(val(5000))),
    on("xubuntu_xfce",()->waitAfter(val(6000))));
  final Action noteShown= action("noteShown",
    on("ubuntu_gnome",()->waitAfter(val(11000))),
    on("xubuntu_xfce",()->waitAfter(val(14000))));
  final Action appsShownNote= action("appsShownNote",
    on("ubuntu_gnome",()->waitAfter(val(3500))),
    on("xubuntu_xfce",()->waitAfter(val(4000))));
  final Action terminalShownNote= action("terminalShownNote",
    on("ubuntu_gnome",()->waitAfter(val(5000))),
    on("xubuntu_xfce",()->waitAfter(val(6500))));
  final Action noteRun= action("noteRun",
    on("ubuntu_gnome",()->waitAfter(val(13500))),
    on("xubuntu_xfce",()->waitAfter(val(17000))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",()->waitAfter(val(7000))),
    on("xubuntu_xfce",()->waitAfter(val(8500))));
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
    var center= new Rectangle(760,460,400,160);
    var output= inWindow(410,170,1000,90);
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

package agentTools;

import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.imageio.ImageIO;

import tools.Fs;

final class OpenWithChoiceTest extends ManagerTest{
  final Action appsShown= action("appsShown",on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",on("ubuntu_gnome",()->waitUntilTime(val(18000))));
  final Action codeShown= action("codeShown",on("ubuntu_gnome",()->waitUntilTime(val(21000))));
  final Action compile= action("compile",on("ubuntu_gnome",()->click(val(530),val(112))));
  final Action compiled= action("compiled",on("ubuntu_gnome",()->waitUntilTime(val(36000))));
  final Action appsShownFiles= action("appsShownFiles",on("ubuntu_gnome",()->waitUntilTime(val(37500))));
  final Action terminalShownFiles= action("terminalShownFiles",on("ubuntu_gnome",()->waitUntilTime(val(41000))));
  final Action filesShown= action("filesShown",on("ubuntu_gnome",()->waitUntilTime(val(52000))));
  final Action openNote= action("openNote",on("ubuntu_gnome",()->doubleClick(val(1984),val(915))));
  final Action chooserShown= action("chooserShown",on("ubuntu_gnome",()->waitUntilTime(val(57000))));
  final Action ranSecond= action("ranSecond",on("ubuntu_gnome",()->waitUntilTime(val(63000))));
  final Action closeFiles= action("closeFiles",on("ubuntu_gnome",()->click(val(2374),val(843))));
  final Action filesClosed= action("filesClosed",on("ubuntu_gnome",()->waitUntilTime(val(70000))));
  final Action managerQuit= action("managerQuit",on("ubuntu_gnome",()->waitUntilTime(val(67000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var choice= filesIOFolder.resolve("choice");
    Fs.rmTree(choice);
    Fs.writeUtf8(choice.resolve("choice.fearless"),"\n");
    Fs.writeUtf8(choice.resolve("_hello").resolve("_rank_app.fear"),"use base.Main as Main;\nuse base.OpenWith as OpenWith;\n\nFirst:Main, OpenWith[IconsHello,`ffile123`]{s->base.Debug#(`first`)}\nSecond:Main, OpenWith[IconsHello,`ffile123`]{s->base.Debug#(`second`)}\n");
    Fs.ensureDir(choice.resolve("_hello").resolve("icons"));
    Fs.ofV(()->ImageIO.write(new BufferedImage(64,64,BufferedImage.TYPE_INT_RGB),"png",choice.resolve("_hello").resolve("icons").resolve("hello.png").toFile()));
    Fs.writeUtf8(choice.resolve("note.ffile123"),"dear reader\n");
    launchScript("first",choice.resolve("choice.fearless"));
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    becomeCode.go();
    codeShown.go();
    compile.go();
    compiled.go();
    shell("nohup setsid -f xdg-open \""+choice+"\" >/dev/null 2>&1\n");
    runInTerminal(appsShownFiles,terminalShownFiles);
    filesShown.go();
    var files= look();
    openNote.go();
    chooserShown.go();
    changed(files,new Rectangle(1800,1000,240,100));
    keys(KeyEvent.VK_DOWN);
    keys(KeyEvent.VK_ENTER);
    ranSecond.go();
    stabilize();
    checkContent(info,"""
      {
        "projects": {
          "choice": {
            "path": "Str:%s",
            "kind": "code",
            "mains": ["hello.Second"]
          }
        },
        "systemExtensions": []
      }
      """.formatted(slashed(choice)));
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
    closeFiles.go();
    filesClosed.go();
  }
}

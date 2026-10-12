package agentTools;

import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.imageio.ImageIO;

import tools.Fs;

final class OpenWithChoiceTest extends ManagerTest{
  final Action becomeCode= action("becomeCode",
    on("ubuntu_gnome",()->click(val(983),val(113))),
    on("xubuntu_xfce",()->click(val(990),val(93))));
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(3500))),
    on("xubuntu_xfce",()->waitUntilTime(val(4500))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(8500))),
    on("xubuntu_xfce",()->waitUntilTime(val(11000))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(26000))),
    on("xubuntu_xfce",()->waitUntilTime(val(32500))));
  final Action codeShown= action("codeShown",
    on("ubuntu_gnome",()->waitUntilTime(val(30500))),
    on("xubuntu_xfce",()->waitUntilTime(val(38500))));
  final Action compile= action("compile",
    on("ubuntu_gnome",()->click(val(536),val(113))),
    on("xubuntu_xfce",()->click(val(496),val(93))));
  final Action compiled= action("compiled",
    on("ubuntu_gnome",()->waitUntilTime(val(51500))),
    on("xubuntu_xfce",()->waitUntilTime(val(64500))));
  final Action appsShownFiles= action("appsShownFiles",
    on("ubuntu_gnome",()->waitUntilTime(val(54500))),
    on("xubuntu_xfce",()->waitUntilTime(val(68500))));
  final Action terminalShownFiles= action("terminalShownFiles",
    on("ubuntu_gnome",()->waitUntilTime(val(59500))),
    on("xubuntu_xfce",()->waitUntilTime(val(74500))));
  final Action filesShown= action("filesShown",
    on("ubuntu_gnome",()->waitUntilTime(val(75000))),
    on("xubuntu_xfce",()->waitUntilTime(val(94000))));
  final Action openNote= action("openNote",
    on("ubuntu_gnome",()->doubleClick(val(1024),val(375))),
    on("xubuntu_xfce",()->doubleClick(val(1151),val(425))));
  final Action chooserShown= action("chooserShown",
    on("ubuntu_gnome",()->waitUntilTime(val(82000))),
    on("xubuntu_xfce",()->waitUntilTime(val(102500))));
  final Action ranSecond= action("ranSecond",
    on("ubuntu_gnome",()->waitUntilTime(val(90500))),
    on("xubuntu_xfce",()->waitUntilTime(val(113500))));
  final Action closeFiles= action("closeFiles",
    on("ubuntu_gnome",()->click(val(1414),val(303))),
    on("xubuntu_xfce",()->click(val(1275),val(309))));
  final Action filesClosed= action("filesClosed",
    on("ubuntu_gnome",()->waitUntilTime(val(100500))),
    on("xubuntu_xfce",()->waitUntilTime(val(126000))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",()->waitUntilTime(val(96000))),
    on("xubuntu_xfce",()->waitUntilTime(val(120000))));
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
    changed(files,new Rectangle(840,460,240,100));
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

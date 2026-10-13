package agentTools;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import tools.Fs;

final class OpenWithChoiceTest extends ManagerTest{
  final Action becomeCode= action("becomeCode",
    on("ubuntu_gnome",()->click(val(983),val(113))),
    on("xubuntu_xfce",()->click(val(990),val(93))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitAfter(val(17500))),
    on("xubuntu_xfce",()->waitAfter(val(21500))));
  final Action codeShown= action("codeShown",
    on("ubuntu_gnome",()->waitAfter(val(4500))),
    on("xubuntu_xfce",()->waitAfter(val(6000))));
  final Action compile= action("compile",
    on("ubuntu_gnome",()->click(val(536),val(113))),
    on("xubuntu_xfce",()->click(val(496),val(93))));
  final Action compiled= action("compiled",
    on("ubuntu_gnome",()->waitAfter(val(21000))),
    on("xubuntu_xfce",()->waitAfter(val(26000))));
  final Action filesShown= action("filesShown",
    on("ubuntu_gnome",()->waitAfter(val(15500))),
    on("xubuntu_xfce",()->waitAfter(val(19500))));
  final Action chooserShown= action("chooserShown",
    on("ubuntu_gnome",()->waitAfter(val(7000))),
    on("xubuntu_xfce",()->waitAfter(val(8500))));
  final Action ranSecond= action("ranSecond",
    on("ubuntu_gnome",()->waitAfter(val(8500))),
    on("xubuntu_xfce",()->waitAfter(val(11000))));
  final Action filesClosed= action("filesClosed",
    on("ubuntu_gnome",()->waitAfter(val(4500))),
    on("xubuntu_xfce",()->waitAfter(val(6000))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",()->waitAfter(val(5500))),
    on("xubuntu_xfce",()->waitAfter(val(6500))));
  final Action openChoice= action("openChoice",
    on("ubuntu_gnome",()->waitDoubleClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",()->waitDoubleClick(val(null),val(null),val(null))));
  final Action openChoiceMarker= action("openChoiceMarker",
    on("ubuntu_gnome",()->waitDoubleClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",()->waitDoubleClick(val(null),val(null),val(null))));
  final Action projectShown= action("projectShown",
    on("ubuntu_gnome",()->waitAfter(val(null))),
    on("xubuntu_xfce",()->waitAfter(val(null))));
  final Action openChoiceAgain= action("openChoiceAgain",
    on("ubuntu_gnome",()->waitDoubleClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",()->waitDoubleClick(val(null),val(null),val(null))));
  final Action openNote= action("openNote",
    on("ubuntu_gnome",()->waitDoubleClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",()->waitDoubleClick(val(null),val(null),val(null))));
  final Action secondChoice= action("secondChoice",
    on("ubuntu_gnome",()->waitClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",()->waitClick(val(null),val(null),val(null))));
  final Action runButton= action("runButton",
    on("ubuntu_gnome",()->waitClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",()->waitClick(val(null),val(null),val(null))));
  @Override void walk() throws Throwable{
    var choice= files.resolve("choice");
    Fs.writeUtf8(choice.resolve("choice.fearless"),"\n");
    Fs.writeUtf8(choice.resolve("_hello").resolve("_rank_app.fear"),"use base.Main as Main;\nuse base.OpenWith as OpenWith;\n\nFirst:Main, OpenWith[IconsHello,`ffile123`]{s->base.Debug#(`first`)}\nSecond:Main, OpenWith[IconsHello,`ffile123`]{s->base.Debug#(`second`)}\n");
    Fs.ensureDir(choice.resolve("_hello").resolve("icons"));
    Fs.ofV(()->ImageIO.write(new BufferedImage(64,64,BufferedImage.TYPE_INT_RGB),"png",choice.resolve("_hello").resolve("icons").resolve("hello.png").toFile()));
    Fs.writeUtf8(choice.resolve("note.ffile123"),"dear reader\n");
    start();
    managerShown.go();
    toFiles();
    openChoice.go();
    openChoiceMarker.go();
    closeFileManager.go();
    projectShown.go();
    becomeCode.go();
    codeShown.go();
    compile.go();
    compiled.go();
    toFiles();
    openChoiceAgain.go();
    filesShown.go();
    var listing= look();
    openNote.go();
    chooserShown.go();
    changed(listing,new Rectangle(840,460,240,100));
    secondChoice.go();
    runButton.go();
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
    closeFileManager.go();
    filesClosed.go();
  }
}

package agentTools;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import tools.Fs;

final class SystemExtensionTest extends ManagerTest{
  final Action becomeCode= action("becomeCode",
    on("ubuntu_gnome",() -> click(val(980),val(113))),
    on("xubuntu_xfce",() -> click(val(986),val(93))));
  final Action openOpens= action("openOpens",
    on("ubuntu_gnome",() -> waitDoubleClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",() -> waitDoubleClick(val(null),val(null),val(null))));
  final Action openOpensMarker= action("openOpensMarker",
    on("ubuntu_gnome",() -> waitDoubleClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",() -> waitDoubleClick(val(null),val(null),val(null))));
  final Action projectShown= action("projectShown",
    on("ubuntu_gnome",() -> waitAfter(val(null))),
    on("xubuntu_xfce",() -> waitAfter(val(null))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",() -> waitAfter(val(17500))),
    on("xubuntu_xfce",() -> waitAfter(val(21500))));
  final Action codeShown= action("codeShown",
    on("ubuntu_gnome",() -> waitAfter(val(4500))),
    on("xubuntu_xfce",() -> waitAfter(val(6000))));
  final Action compile= action("compile",
    on("ubuntu_gnome",() -> click(val(533),val(113))),
    on("xubuntu_xfce",() -> click(val(492),val(93))));
  final Action questionShown= action("questionShown",
    on("ubuntu_gnome",() -> waitAfter(val(21000))),
    on("xubuntu_xfce",() -> waitAfter(val(26000))));
  final Action yesButton= action("yesButton",
    on("ubuntu_gnome",() -> waitClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",() -> waitClick(val(null),val(null),val(null))));
  final Action allowed= action("allowed",
    on("ubuntu_gnome",() -> waitAfter(val(4500))),
    on("xubuntu_xfce",() -> waitAfter(val(5500))));
  final Action compileAgain= action("compileAgain",
    on("ubuntu_gnome",() -> click(val(533),val(113))),
    on("xubuntu_xfce",() -> click(val(492),val(93))));
  final Action compiled= action("compiled",
    on("ubuntu_gnome",() -> waitAfter(val(21000))),
    on("xubuntu_xfce",() -> waitAfter(val(26500))));
  final Action openLetter= action("openLetter",
    on("ubuntu_gnome",() -> waitDoubleClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",() -> waitDoubleClick(val(null),val(null),val(null))));
  final Action letterRun= action("letterRun",
    on("ubuntu_gnome",() -> waitAfter(val(10000))),
    on("xubuntu_xfce",() -> waitAfter(val(12500))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",() -> waitAfter(val(7000))),
    on("xubuntu_xfce",() -> waitAfter(val(9000))));
  @Override void walk() throws Throwable{
    var opens= files.resolve("opens");
    var icons= opens.resolve("_hello").resolve("icons");
    Fs.writeUtf8(opens.resolve("opens.fearless"),"\n");
    Fs.writeUtf8(opens.resolve("_hello").resolve("_rank_app.fear"),"use base.Main as Main;\nuse base.OpenWith as OpenWith;\n\nHello:Main, OpenWith[IconsHello,\"fzz\"]{s->base.Debug#(`hello`)}\n");
    Fs.ensureDir(icons);
    Fs.ofV(() -> ImageIO.write(new BufferedImage(64,64,BufferedImage.TYPE_INT_RGB),"png",icons.resolve("hello.png").toFile()));
    Fs.writeUtf8(opens.resolve("letter.fzz"),"dear reader\n");
    var bare= look();
    start();
    managerShown.go();
    toFiles();
    openOpens.go();
    openOpensMarker.go();
    closeFiles();
    projectShown.go();
    becomeCode.go();
    codeShown.go();
    var center= new Rectangle(760,460,400,160);
    var before= look();
    compile.go();
    questionShown.go();
    changed(before,center);
    stabilize();
    checkContent(files.resolve("opens/.fearless_out/compile_error.txt"),"The system extension \"fzz\" of [###] of main \"hello.Hello\" is not in \"systemExtensions\" of projects.info.");
    yesButton.go();
    allowed.go();
    stabilize();
    checkContent(info,"""
      {
        "projects": {
          "opens": {
            "path": "Str:%s",
            "kind": "code"
          }
        },
        "systemExtensions": ["fzz"]
      }
      """.formatted(slashed(opens)));
    compileAgain.go();
    compiled.go();
    var output= inWindow(410,168,1000,80);
    var compiledLook= look();
    toFiles();
    openOpens.go();
    openLetter.go();
    closeFiles();
    letterRun.go();
    changed(compiledLook,output);
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    same(bare,output);
  }
}

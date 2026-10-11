package agentTools;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.imageio.ImageIO;

import tools.Fs;

final class SystemExtensionTest extends ManagerTest{
  final Action appsShown= action("appsShown",on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",on("ubuntu_gnome",()->waitUntilTime(val(18000))));
  final Action codeShown= action("codeShown",on("ubuntu_gnome",()->waitUntilTime(val(21000))));
  final Action compile= action("compile",on("ubuntu_gnome",()->click(val(530),val(112))));
  final Action questionShown= action("questionShown",on("ubuntu_gnome",()->waitUntilTime(val(36000))));
  final Action allowed= action("allowed",on("ubuntu_gnome",()->waitUntilTime(val(39000))));
  final Action compileAgain= action("compileAgain",on("ubuntu_gnome",()->click(val(530),val(112))));
  final Action compiled= action("compiled",on("ubuntu_gnome",()->waitUntilTime(val(54000))));
  final Action appsShownFiles= action("appsShownFiles",on("ubuntu_gnome",()->waitUntilTime(val(55500))));
  final Action terminalShownFiles= action("terminalShownFiles",on("ubuntu_gnome",()->waitUntilTime(val(59000))));
  final Action filesShown= action("filesShown",on("ubuntu_gnome",()->waitUntilTime(val(70000))));
  final Action openLetter= action("openLetter",on("ubuntu_gnome",()->doubleClick(val(1872),val(915))));
  final Action letterRun= action("letterRun",on("ubuntu_gnome",()->waitUntilTime(val(77000))));
  final Action closeFiles= action("closeFiles",on("ubuntu_gnome",()->click(val(2374),val(843))));
  final Action filesClosed= action("filesClosed",on("ubuntu_gnome",()->waitUntilTime(val(79000))));
  final Action managerQuit= action("managerQuit",on("ubuntu_gnome",()->waitUntilTime(val(84000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var opens= filesIOFolder.resolve("opens");
    var icons= opens.resolve("_hello").resolve("icons");
    Fs.rmTree(opens);
    Fs.writeUtf8(opens.resolve("opens.fearless"),"\n");
    Fs.writeUtf8(opens.resolve("_hello").resolve("_rank_app.fear"),"use base.Main as Main;\nuse base.OpenWith as OpenWith;\n\nHello:Main, OpenWith[IconsHello,\"fzz\"]{s->base.Debug#(`hello`)}\n");
    Fs.ensureDir(icons);
    Fs.ofV(()->ImageIO.write(new BufferedImage(64,64,BufferedImage.TYPE_INT_RGB),"png",icons.resolve("hello.png").toFile()));
    Fs.writeUtf8(opens.resolve("letter.fzz"),"dear reader\n");
    launchScript("first",opens.resolve("opens.fearless"));
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    becomeCode.go();
    codeShown.go();
    var center= new Rectangle(1720,1000,400,160);
    var before= look();
    compile.go();
    questionShown.go();
    changed(before,center);
    stabilize();
    checkContent(List.of("opens",".fearless_out","compile_error.txt"),"The system extension \"fzz\" of [###] of main \"hello.Hello\" is not in \"systemExtensions\" of projects.info.");
    closeDialog.go();
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
    var output= new Rectangle(410,190,1000,60);
    var compiledLook= look();
    shell("nohup setsid -f xdg-open \""+opens+"\" >/dev/null 2>&1\n");
    runInTerminal(appsShownFiles,terminalShownFiles);
    filesShown.go();
    openLetter.go();
    letterRun.go();
    closeFiles.go();
    filesClosed.go();
    changed(compiledLook,output);
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
  }
}

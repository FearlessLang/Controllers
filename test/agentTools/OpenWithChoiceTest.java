package agentTools;

import java.awt.image.BufferedImage;
import java.util.List;

import javax.imageio.ImageIO;

import tools.Fs;

final class OpenWithChoiceTest extends ManagerTest{
  final List<String> console= List.of(data,"eclipse","choice","console.txt");
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(18500))));
  final Action becomeCode= action("becomeCode",
    on("ubuntu_gnome",()->click(val(719),val(139))),
    on("windows",()->click(val(332),val(94))));
  final Action codeShown= action("codeShown",
    on("ubuntu_gnome",()->waitUntilTime(val(21600))));
  final Action compile= action("compile",
    on("ubuntu_gnome",()->click(val(483),val(111))),
    on("windows",()->click(val(102),val(67))));
  final Action compiled= action("compiled",
    on("ubuntu_gnome",()->waitUntilTime(val(35200))));
  final Action appsShownFiles= action("appsShownFiles",
    on("ubuntu_gnome",()->waitUntilTime(val(36700))));
  final Action terminalShownFiles= action("terminalShownFiles",
    on("ubuntu_gnome",()->waitUntilTime(val(40200))));
  final Action filesShown= action("filesShown",
    on("ubuntu_gnome",()->waitUntilTime(val(53200))));
  final Action openNote= action("openNote",
    on("ubuntu_gnome",()->doubleClick(val(1984),val(915))),
    on("windows",()->doubleClick(val(480),val(312))));
  final Action chooserShown= action("chooserShown",
    on("ubuntu_gnome",()->waitUntilTime(val(55700))));
  final Action pickSecond= action("pickSecond",
    on("ubuntu_gnome",()->click(val(1950),val(1012))),
    on("windows",()->click(val(650),val(247))));
  final Action runPicked= action("runPicked",
    on("ubuntu_gnome",()->click(val(2008),val(1251))),
    on("windows",()->click(val(698),val(485))));
  final Action ranSecond= action("ranSecond",
    on("ubuntu_gnome",()->waitUntilTime(val(60900))));
  final Action appsShownToEnd= action("appsShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(62400))));
  final Action terminalShownToEnd= action("terminalShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(65900))));
  final Action managerEnded= action("managerEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(75900))));
  final Action closeFiles= action("closeFiles",
    on("ubuntu_gnome",()->click(val(2374),val(843))),
    on("windows",()->click(val(1016),val(58))));
  @Override void walk() throws Throwable{
    noManagerData();
    var choice= filesIOFolder.resolve("choice");
    Fs.rmTree(choice);
    Fs.writeUtf8(choice.resolve("choice.fearless"),"\n");
    Fs.writeUtf8(choice.resolve("_hello").resolve("_rank_app.fear"),"use base.Main as Main;\nuse base.OpenWith as OpenWith;\n\nFirst:Main, OpenWith[IconsHello,`ffile123`]{s->base.Debug#(`first`)}\nSecond:Main, OpenWith[IconsHello,`ffile123`]{s->base.Debug#(`second`)}\n");
    Fs.ensureDir(choice.resolve("_hello").resolve("icons"));
    Fs.ofV(()->ImageIO.write(new BufferedImage(64,64,BufferedImage.TYPE_INT_RGB),"png",choice.resolve("_hello").resolve("icons").resolve("hello.png").toFile()));
    Fs.writeUtf8(choice.resolve("note.ffile123"),"dear reader\n");
    var bare= look();
    launchScript("first",choice);
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    becomeCode.go();
    codeShown.go();
    stabilize();
    checkContent(info,"""
      {
        "choice": {
          "path": "Str:%s",
          "kind": "code"
        }
      }
      """.formatted(slashed(choice)));
    compile.go();
    compiled.go();
    stabilize();
    checkContent(console,"--- compiling choice ---\n--- compile done ---\n");
    var manager= look();
    shell("nohup setsid -f xdg-open \""+choice+"\" >/dev/null 2>&1\n");
    runInTerminal(appsShownFiles,terminalShownFiles);
    filesShown.go();
    var files= changed("Opening the folder shows the file manager window",manager,look());
    openNote.go();
    chooserShown.go();
    pickSecond.go();
    runPicked.go();
    ranSecond.go();
    stabilize();
    checkContent(console,"""
      --- compiling choice ---
      --- compile done ---
      --- running hello.Second ---
      second
      --- hello.Second exited with 0 after [###]s ---
      """);
    endScript();
    runInTerminal(appsShownToEnd,terminalShownToEnd);
    managerEnded.go();
    stabilize();
    checkContent(List.of("first.exit"),"137\n");
    closeFiles.go();
    same("Closing the file manager window shows the desk as before",bare,look(),files);
  }
}

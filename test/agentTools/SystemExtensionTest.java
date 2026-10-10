package agentTools;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.util.List;

import javax.imageio.ImageIO;

import tools.Fs;

final class SystemExtensionTest extends ManagerTest{
  static final List<String> console= List.of(data,"eclipse","opens","console.txt");
  static final List<String> extensions= List.of(data,"extensions.info");
  static final String compiled= "--- compiling opens ---\n--- compile done ---\n";
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(18000))));
  final Action becomeCode= action("becomeCode",
    on("ubuntu_gnome",()->click(val(719),val(139))),
    on("windows",()->click(val(332),val(94))));
  final Action codeShown= action("codeShown",
    on("ubuntu_gnome",()->waitUntilTime(val(21500))));
  final Action compile= action("compile",
    on("ubuntu_gnome",()->click(val(483),val(111))),
    on("windows",()->click(val(102),val(67))));
  final Action questionShown= action("questionShown",
    on("ubuntu_gnome",()->waitUntilTime(val(38500))));
  final Action allow= action("allow",
    on("ubuntu_gnome",()->click(val(1890),val(1165))),
    on("windows",()->click(val(579),val(397))));
  final Action compileDone= action("compileDone",
    on("ubuntu_gnome",()->waitUntilTime(val(47000))));
  final Action appsShownForFiles= action("appsShownForFiles",
    on("ubuntu_gnome",()->waitUntilTime(val(48500))));
  final Action terminalShownForFiles= action("terminalShownForFiles",
    on("ubuntu_gnome",()->waitUntilTime(val(52000))));
  final Action filesShown= action("filesShown",
    on("ubuntu_gnome",()->waitUntilTime(val(65000))));
  final Action openLetter= action("openLetter",
    on("ubuntu_gnome",()->doubleClick(val(1872),val(915))),
    on("windows",()->doubleClick(val(480),val(283))));
  final Action letterRun= action("letterRun",
    on("ubuntu_gnome",()->waitUntilTime(val(81000))));
  final Action closeFiles= action("closeFiles",
    on("ubuntu_gnome",()->click(val(2374),val(843))),
    on("windows",()->click(val(1016),val(58))));
  final Action managerMenu= action("managerMenu",
    on("ubuntu_gnome",()->click(val(98),val(79))),
    on("windows",()->click(val(31),val(33))));
  final Action systemExtensions= action("systemExtensions",
    on("ubuntu_gnome",()->click(val(128),val(145))),
    on("windows",()->click(val(61),val(100))));
  final Action extensionsShown= action("extensionsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(84500))));
  final Action pickExtension= action("pickExtension",
    on("ubuntu_gnome",()->click(val(1750),val(988))),
    on("windows",()->click(val(450),val(223))));
  final Action removeExtension= action("removeExtension",
    on("ubuntu_gnome",()->click(val(2175),val(1234))),
    on("windows",()->click(val(857),val(469))));
  final Action extensionRemoved= action("extensionRemoved",
    on("ubuntu_gnome",()->waitUntilTime(val(87500))));
  final Action closeExtensions= action("closeExtensions",
    on("ubuntu_gnome",()->click(val(2195),val(958))),
    on("windows",()->click(val(883),val(197))));
  final Action managerMenuAgain= action("managerMenuAgain",
    on("ubuntu_gnome",()->click(val(98),val(79))),
    on("windows",()->click(val(31),val(33))));
  final Action quitManager= action("quitManager",
    on("ubuntu_gnome",()->click(val(116),val(212))),
    on("windows",()->click(val(61),val(167))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",()->waitUntilTime(val(92500))));
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
    launchScript("first",opens);
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    becomeCode.go();
    codeShown.go();
    stabilize();
    assertFalse(Files.exists(filesIOFolder.resolve(String.join("/",extensions))));
    compile.go();
    questionShown.go();
    allow.go();
    compileDone.go();
    stabilize();
    checkContent(console,compiled);
    checkContent(extensions,"[\"fzz\"]\n");
    var manager= look();
    shell("nohup setsid -f xdg-open \""+opens+"\" >/dev/null 2>&1\n");
    runInTerminal(appsShownForFiles,terminalShownForFiles);
    filesShown.go();
    var files= changed("Opening the folder shows the file manager window",manager,look());
    openLetter.go();
    letterRun.go();
    stabilize();
    checkContent(console,compiled+"""
      --- running hello.Hello ---
      hello
      --- hello.Hello exited with 0 after [###]s ---
      """);
    closeFiles.go();
    var withoutFiles= look();
    same("Closing the file manager window shows the manager window as before",manager,withoutFiles,files);
    managerMenu.go();
    systemExtensions.go();
    extensionsShown.go();
    var window= changed("System extensions opens its window",withoutFiles,look());
    pickExtension.go();
    removeExtension.go();
    extensionRemoved.go();
    stabilize();
    checkContent(extensions,"[]\n");
    checkContent(console,compiled+"""
      --- running hello.Hello ---
      hello
      --- hello.Hello exited with 0 after [###]s ---
      The system extension \"fzz\" of [###] of main \"hello.Hello\" is not allowed in this manager.
      The project claims no extension until it is compiled again.
      """);
    closeExtensions.go();
    same("The close button closes the window",withoutFiles,look(),window);
    managerMenuAgain.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
  }
}

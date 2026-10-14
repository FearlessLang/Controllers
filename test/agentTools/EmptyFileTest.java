package agentTools;

import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import tools.Fs;

final class EmptyFileTest extends ManagerTest{
  final Action centerArea= action("centerArea",
    on("ubuntu_gnome", () -> region(val(760), val(460), val(400), val(160))),
    on("xubuntu_xfce", () -> region(val(760), val(460), val(400), val(160))));
  final Action becomeCode= action("becomeCode",
    on("ubuntu_gnome", () -> waitClick(val(0), val(985), val(113))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(992), val(93))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome", () -> waitAfter(val(17500))),
    on("xubuntu_xfce", () -> waitAfter(val(21500))));
  final Action codeShown= action("codeShown",
    on("ubuntu_gnome", () -> waitAfter(val(4500))),
    on("xubuntu_xfce", () -> waitAfter(val(6000))));
  final Action compile= action("compile",
    on("ubuntu_gnome", () -> waitClick(val(0), val(538), val(113))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(498), val(93))));
  final Action compiled= action("compiled",
    on("ubuntu_gnome", () -> waitAfter(val(21000))),
    on("xubuntu_xfce", () -> waitAfter(val(26000))));
  final Action noteRun= action("noteRun",
    on("ubuntu_gnome", () -> waitAfter(val(13500))),
    on("xubuntu_xfce", () -> waitAfter(val(17000))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome", () -> waitAfter(val(7000))),
    on("xubuntu_xfce", () -> waitAfter(val(8500))));
  final Action openBlanks= action("openBlanks",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action openBlanksMarker= action("openBlanksMarker",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action projectShown= action("projectShown",
    on("ubuntu_gnome", () -> waitAfter(val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(null))));
  final Action openBlanksAgain= action("openBlanksAgain",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action openNote= action("openNote",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  @Override void walk() throws Throwable{
    var opens= files.resolve("blanks");
    var icons= opens.resolve("_blanks").resolve("icons");
    Fs.writeUtf8(opens.resolve("blanks.fearless"), "\n");
    Fs.writeUtf8(opens.resolve("_blanks").resolve("_rank_app.fear"), "use base.Main as Main;\nuse base.OpenWith as OpenWith;\n\nRead:Main, OpenWith[IconsRead,`ffile321`]{s->base.Debug#(`read`)}\n");
    Fs.ensureDir(icons);
    Fs.ofV(() -> ImageIO.write(new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB), "png", icons.resolve("read.png").toFile()));
    Fs.writeUtf8(opens.resolve("note.ffile321"), "dear reader\n");
    start();
    managerShown.go();
    toFiles();
    openBlanks.go();
    openBlanksMarker.go();
    projectShown.go();
    becomeCode.go();
    codeShown.go();
    compile.go();
    compiled.go();
    centerArea.go();
    var center= region;
    var output= inWindow(410, 170, 1000, 90);
    var compiledLook= look();
    toFiles();
    openBlanksAgain.go();
    openNote.go();
    noteRun.go();
    same(compiledLook, center);
    changed(compiledLook, output);
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    closeFiles();
  }
}

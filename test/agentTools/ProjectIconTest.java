package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterEach;

import tools.Fs;

/// A project shows its own icon, the one PNG file of its folder .config/icon, on its tile: the manager notices the file by itself; a second PNG file there makes the project invalid, its tile going back to the made up icon and its Error report saying why; once that file is gone the tile shows its own icon again, exactly as before.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, nothing is registered for .fearless, and the folder pic beside the manager holds only its marker and one source file.
/// Action 1: run the launcher on pic: the manager window opens showing pic, an idle project, whose tile shows an icon made up from its name.
/// Action 2: save a PNG file of one colour as .config/icon/face.png in pic: the icon of the tile becomes that colour by itself.
/// Action 3: save a second PNG file of another colour beside it: the icon of the tile is by itself the made up one again, and the tile looks different from its first look.
/// Action 4: choose Error report in its Project menu: a dialog opens.
/// Action 5: click in its text, select all of it and copy it: the text copied says more than one PNG file was found for the icon, where, and names both files.
/// Action 6: press OK: the dialog goes away.
/// Action 7: delete the second PNG file: the tile is back by itself exactly as it was with one PNG file.
/// Action 8: end the manager.
final class ProjectIconTest extends ManagerTest{
  static final Path pic= data.resolveSibling("pic");
  static final Path icons= pic.resolve(".config").resolve("icon");
  final At managerShown= new At("managerShown",on("ubuntu-gnome",3000));
  final Area icon= new Area("icon",on("ubuntu-gnome",114,159,28,28));
  final Area tile= new Area("tile",on("ubuntu-gnome",74,149,128,88));
  final Area dialog= new Area("dialog",on("ubuntu-gnome",1600,840,700,30));
  final Click projectMenu= new Click("projectMenu",on("ubuntu-gnome",158,79));
  final Click errorReport= new Click("errorReport",on("ubuntu-gnome",180,212));
  final Click focusReport= new Click("focusReport",on("ubuntu-gnome",1970,1100));
  final Click ok= new Click("ok",on("ubuntu-gnome",1953,1323));
  @Override protected void walk() throws Exception{
    clean();
    Fs.writeUtf8(pic.resolve("pic.fearless"),"\n");
    Fs.writeUtf8(pic.resolve("_pic").resolve("_rank_app.fear"),"use base.Main as Main;\n\nHello:Main{s->base.Debug#(\"pic\")}\n");
    launch(pic.toString());
    managerShown.go();
    look();
    var at= icon.aim();
    var made= pixels(at);
    var cell= tile.aim();
    var first= pixels(cell);
    png(icons.resolve("face.png"),0xC03090);
    until(()->Arrays.stream(pixels(at)).allMatch(p->p==0xffC03090));
    var own= pixels(cell);
    png(icons.resolve("other.png"),0x30C090);
    until(()->Arrays.equals(made,pixels(at)));
    assertFalse(Arrays.equals(first,pixels(cell)));
    var where= dialog.aim();
    var behind= pixels(where);
    projectMenu.go();
    errorReport.go();
    until(()->!Arrays.equals(behind,pixels(where)));
    focusReport.go();
    pilot.chord(KeyEvent.VK_CONTROL,KeyEvent.VK_A);
    pilot.chord(KeyEvent.VK_CONTROL,KeyEvent.VK_C);
    assertEquals("""
      More than one .png file was found for this project's icon.

      Looked in:
        %s

      Found:
        face.png
        other.png

      Keep exactly one .png file there.
      """.formatted(icons),Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor));
    ok.go();
    until(()->Arrays.equals(behind,pixels(where)));
    Files.delete(icons.resolve("other.png"));
    until(()->Arrays.equals(own,pixels(cell)));
    stopManagers();
  }
  static void png(Path file, int rgb) throws Exception{
    var img= new BufferedImage(48,48,BufferedImage.TYPE_INT_RGB);
    var g= img.createGraphics();
    g.setColor(new Color(rgb));
    g.fillRect(0,0,48,48);
    g.dispose();
    Fs.ensureDir(file.getParent());
    assertTrue(ImageIO.write(img,"png",file.toFile()));
  }
  @Override @AfterEach void clean() throws Exception{
    super.clean();
    Fs.rmTree(pic);
  }
}

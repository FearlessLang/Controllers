package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Desktop;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import tools.Fs;
import utils.OneOr;

/// Drives the real desktop: two folder windows are opened side by side and a file is dragged from one to the other.
final class DesktopDragTest{
  private static final Path root= Path.of(System.getProperty("java.io.tmpdir"),"desktopDragTest");
  private static final Path source= root.resolve("test_file_source");
  private static final Path destination= root.resolve("test_file_destination");
  private static final String content= "dragged across the desktop\n";
  @Test void aFileDraggedBetweenTwoFolderWindowsMovesThere(){
    Fs.ensureDir(source);
    Fs.ensureDir(destination);
    Fs.writeUtf8(source.resolve("example.txt"),content);
    var pilot= new Pilot();
    pilot.showDesktop();
    var screen= pilot.shot();
    int w= screen.getWidth(), h= screen.getHeight();
    var src= new Rectangle(w*5/100,h*5/100,w*43/100,h*85/100);
    var dst= new Rectangle(w*52/100,h*5/100,w*43/100,h*85/100);
    place(pilot,source,src);
    place(pilot,destination,dst);
    var item= item(pilot,src);
    pilot.drag(centerX(item),centerY(item),centerX(dst),centerY(dst));
    Pilot.pause(2000);
    close(pilot,src);
    close(pilot,dst);
    var landed= OneOr.of("the dragged file",Fs.walk(root,s->s.filter(this::isTheFile).toList()).stream());
    Fs.ofV(()->Files.delete(landed));
    Fs.ofV(()->Files.delete(source));
    Fs.ofV(()->Files.delete(destination));
    Fs.ofV(()->Files.delete(root));
    assertEquals(destination.resolve("example.txt"),landed);
  }
  private boolean isTheFile(Path p){ return p.getFileName().toString().equals("example.txt") && Fs.readUtf8(p).equals(content); }
  /// Opens the folder and puts its window on target, then checks that target is now a window.
  private static void place(Pilot pilot, Path folder, Rectangle target){
    var before= pilot.shot();
    Fs.ofV(()->Desktop.getDesktop().open(folder.toFile()));
    Pilot.pause(3000);
    var win= Pilot.changed(before,pilot.shot(),6);
    assert win.width<before.getWidth() && win.height<before.getHeight();
    pilot.place(win,target);
    var filled= Pilot.changed(crop(before,target),crop(pilot.shot(),target),6);
    boolean targetIsAWindow= filled.width>target.width/2 && filled.height>target.height/2;
    assert targetIsAWindow;
  }
  private static Rectangle item(Pilot pilot, Rectangle win){
    pilot.click(centerX(win),centerY(win));
    var top= new Rectangle(win.x,win.y,win.width,win.height/2);
    var before= crop(pilot.shot(),top);
    pilot.chord(KeyEvent.VK_CONTROL,KeyEvent.VK_A);
    var res= Pilot.changed(before,crop(pilot.shot(),top),0);
    res.translate(win.x,win.y);
    return res;
  }
  private static void close(Pilot pilot, Rectangle win){
    pilot.click(centerX(win),centerY(win));
    pilot.chord(KeyEvent.VK_ALT,KeyEvent.VK_F4);
  }
  private static BufferedImage crop(BufferedImage img, Rectangle r){ return img.getSubimage(r.x,r.y,r.width,r.height); }
  private static int centerX(Rectangle r){ return r.x+r.width/2; }
  private static int centerY(Rectangle r){ return r.y+r.height/2; }
}

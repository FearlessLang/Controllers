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

/// Drives the real desktop: one folder window is opened and the file in it is dragged onto the folder listed beside it.
final class DesktopDragTest{
  private static final Path root= Path.of(System.getProperty("java.io.tmpdir"),"desktopDragTest");
  private static final Path destination= root.resolve("destination");
  private static final String content= "dragged across the desktop\n";
  private final Pilot pilot= new Pilot();
  private Rectangle win;
  private BufferedImage unselected;
  @Test void aFileDraggedOntoAFolderMovesInsideIt(){
    Fs.ensureDir(destination);
    Fs.writeUtf8(root.resolve("example.txt"),content);
    var before= pilot.shot();
    Fs.ofV(()->Desktop.getDesktop().open(root.toFile()));
    var opened= raise(before);
    win= Pilot.changed(before,opened,6);
    assert win.width<before.getWidth() && win.height<before.getHeight();
    unselected= crop(opened,win);
    var file= row(KeyEvent.VK_DOWN);
    var folder= row(KeyEvent.VK_UP);
    pilot.drag(iconX(file),iconY(file),iconX(folder),iconY(folder));
    Pilot.pause(2000);
    pilot.chord(KeyEvent.VK_CONTROL,KeyEvent.VK_W);
    var landed= OneOr.of("the dragged file",Fs.walk(root,s->s.filter(this::isTheFile).toList()).stream());
    Fs.ofV(()->Files.delete(landed));
    Fs.ofV(()->Files.delete(destination));
    Fs.ofV(()->Files.delete(root));
    assertEquals(destination.resolve("example.txt"),landed);
  }
  private boolean isTheFile(Path p){ return p.getFileName().toString().equals("example.txt") && Fs.readUtf8(p).equals(content); }
  /// A window opened behind whatever already holds focus gets no keys of its own: the pointer has to land on it first, a click being the one gesture no window can refuse. Whatever changed first, the window itself or only its taskbar button, is where that click goes; the shot taken after it is the one the window rect gets measured from.
  private BufferedImage raise(BufferedImage before){
    Pilot.pause(400);
    var spot= Pilot.changed(before,pilot.shot(),6);
    pilot.click(spot.x+spot.width/2,spot.y+spot.height/2);
    return pilot.shot();
  }
  /// The row the selection lands on after the key, measured against the same unselected window: the folder holds two entries, so down reaches the lower one and up the upper one wherever the selection starts.
  private Rectangle row(int key){
    pilot.chord(key);
    Pilot.pause(400);
    var r= Pilot.changed(unselected,crop(pilot.shot(),win),0);
    r.translate(win.x,win.y);
    return r;
  }
  /// Where a person aims at an entry: its icon, a square as tall as the row sitting at the row's start. The rest of the row is not the entry, and a press out there sweeps a selection band across the list instead of taking hold of anything.
  private static int iconX(Rectangle row){ return row.x+row.height/2; }
  private static int iconY(Rectangle row){ return row.y+row.height/2; }
  private static BufferedImage crop(BufferedImage img, Rectangle r){ return img.getSubimage(r.x,r.y,r.width,r.height); }
}

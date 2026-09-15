package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import tools.Fs;
import utils.OneOr;

/// A file is carried from one folder window into another, on the desk itself, the way a person does it.
///
/// Prerequisite: the desk shows its background with no window over it, and holds nothing called test1 or test2.
///
/// Action 1: make two folders on the desk, test1 and test2.
/// Action 2: put a file example.txt inside test1.
/// Action 3: open test1 by clicking its icon on the desk.
/// Action 4: carry the window of test1 by its title bar into the left part of the screen.
/// Action 5: send the window of test1 away, leaving it open.
/// Action 6: open test2 by clicking its icon on the desk.
/// Action 7: carry the window of test2 by its title bar into the right part of the screen, clear of where test1's window stands.
/// Action 8: ask the bar of open windows to show the windows of the program holding these two.
/// Action 9: pick test1 out of them, which brings its window back beside test2's.
/// Action 10: carry example.txt out of test1's window and into test2's.
/// Action 11: close the window of test1.
/// Action 12: close the window of test2.
/// Action 13: read the two folders back: example.txt is in test2 and nowhere else.
///
/// The windows are only ever carried, and never against an edge of the screen: a window let go at an edge is how a desk is asked to fill half the screen, and the desk then offers to fill the other half with something else, which is a conversation this test has no business starting. They are never resized either, which matters more: the program that shows folders opens its next window at the size the last one was left, so a test that resizes a window leaves the run after it aiming at a window that is no longer the shape it was measured on.
final class DesktopDragTest{
  private static final int[] onLinux= {
    1852,892,
    637,304,179,304,
    883,304,
    1852,660,
    637,304,1079,304,
    33,129,
    575,560,
    340,377,1520,700,
    956,304,
    1856,304};
  private static final int[] onWindows= {
    36,28,
    300,14,300,300,
    727,15,
    36,135,
    435,14,800,300,
    950,696,
    380,696,
    505,182,1050,300,
    535,24,
    1250,15};
  private static final Path desk= Path.of(System.getProperty("user.home"),"Desktop");
  private static final Path test1= desk.resolve("test1");
  private static final Path test2= desk.resolve("test2");
  private static final String content= "carried across the desk\n";
  /// How long an action waits for the desk to finish drawing what the one before it started, before aiming at anything.
  private static final int settle= 2500;
  private final Pilot pilot= new Pilot();
  private int[] aim;
  private int at;
  @Test void aFileIsCarriedFromOneFolderWindowIntoTheOther(){
    aim= Fs.isWindows() ? onWindows : onLinux;
    if (aim == null){ Assumptions.abort("This desk has no recording yet: walk the actions by hand and write down where each one aims."); }
    assert !Files.exists(test1) && !Files.exists(test2);
    Fs.ensureDir(test1); Fs.ensureDir(test2);
    Fs.writeUtf8(test1.resolve("example.txt"),content);
    open();
    drag();
    click();
    open();
    drag();
    click();
    click();
    drag();
    click();
    click();
    assertEquals(test2.resolve("example.txt"),landed());
  }
  private void open(){ Pilot.pause(settle); pilot.doubleClick(aim[at++],aim[at++]); }
  private void click(){ Pilot.pause(settle); pilot.click(aim[at++],aim[at++]); }
  private void drag(){ Pilot.pause(settle); pilot.drag(aim[at++],aim[at++],aim[at++],aim[at++]); }
  private Path landed(){
    var res= OneOr.of("example.txt",Stream.of(test1,test2).map(d->d.resolve("example.txt")).filter(this::isTheFile));
    Fs.ofV(()->Files.delete(res));
    Fs.ofV(()->Files.delete(test1));
    Fs.ofV(()->Files.delete(test2));
    return res;
  }
  private boolean isTheFile(Path p){ return Files.exists(p) && Fs.readUtf8(p).equals(content); }
}

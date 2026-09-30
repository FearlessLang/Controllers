package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

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
final class DesktopDragTest extends PilotTest{
  final At deskShown= new At("deskShown",linux(2500),windows(2500));
  final DoubleClick openTest1= new DoubleClick("openTest1",linux(1822,784),windows(36,28));
  final At test1Shown= new At("test1Shown",linux(5700),windows(5700));
  final Drag placeTest1= new Drag("placeTest1",linux(1596,842,700,842),windows(300,14,300,300));
  final At test1Placed= new At("test1Placed",linux(9700),windows(9700));
  final Click sendTest1Away= new Click("sendTest1Away",linux(1404,843),windows(727,15));
  final At test1Away= new At("test1Away",linux(12700),windows(12700));
  final DoubleClick openTest2= new DoubleClick("openTest2",linux(1822,546),windows(36,135));
  final At test2Shown= new At("test2Shown",linux(15700),windows(15700));
  final Drag placeTest2= new Drag("placeTest2",linux(1596,842,2600,842),windows(435,14,800,300));
  final At test2Placed= new At("test2Placed",linux(19700),windows(19700));
  final Click showOpenWindows= new Click("showOpenWindows",linux(32,128),windows(950,696));
  final At openWindowsShown= new At("openWindowsShown",linux(22700),windows(22700));
  final Click pickTest1= new Click("pickTest1",linux(2000,640),windows(380,696));
  final At test1Back= new At("test1Back",linux(25700),windows(25700));
  final Drag carryFile= new Drag("carryFile",linux(862,916,3000,1100),windows(505,182,1050,300));
  final At fileCarried= new At("fileCarried",linux(31000),windows(31000));
  final Click closeTest1= new Click("closeTest1",linux(1479,843),windows(535,24));
  final At test1Closed= new At("test1Closed",linux(34000),windows(34000));
  final Click closeTest2= new Click("closeTest2",linux(3379,843),windows(1250,15));
  private static final Path desk= Path.of(System.getProperty("user.home"),"Desktop");
  private static final Path test1= desk.resolve("test1");
  private static final Path test2= desk.resolve("test2");
  private static final String content= "carried across the desk\n";
  @Override protected void walk(){
    assert !Files.exists(test1) && !Files.exists(test2);
    Fs.ensureDir(test1); Fs.ensureDir(test2);
    Fs.writeUtf8(test1.resolve("example.txt"),content);
    deskShown.go();
    openTest1.go();
    test1Shown.go();
    placeTest1.go();
    test1Placed.go();
    sendTest1Away.go();
    test1Away.go();
    openTest2.go();
    test2Shown.go();
    placeTest2.go();
    test2Placed.go();
    showOpenWindows.go();
    openWindowsShown.go();
    pickTest1.go();
    test1Back.go();
    carryFile.go();
    fileCarried.go();
    closeTest1.go();
    test1Closed.go();
    closeTest2.go();
    assertEquals(test2.resolve("example.txt"),landed());
  }
  private Path landed(){
    var res= OneOr.of("example.txt",Stream.of(test1,test2).map(d->d.resolve("example.txt")).filter(this::isTheFile));
    Fs.ofV(()->Files.delete(res));
    Fs.ofV(()->Files.delete(test1));
    Fs.ofV(()->Files.delete(test2));
    return res;
  }
  private boolean isTheFile(Path p){ return Files.exists(p) && Fs.readUtf8(p).equals(content); }
}

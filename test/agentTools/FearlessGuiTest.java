package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import resources.ResolveResource;
import tools.Fs;
import utils.Err;

/// The Fearless program StandardLibrary/integrationTests/testGuiPilot, run by the portable Fearless that DeployPortableFearless.java builds, is used the way a person uses it, and what it reports when ESCAPE ends it is read back.
///
/// Every test starts the same way. Action 1: start the program and wait for its window. Action 2: click the empty top left corner of the window, which gives it the keyboard.
/// Every test ends the same way: press ESCAPE, and the program ends reporting the last thing it saw.
///
/// The program paints everything a test aims at in a colour of its own, so every aim is read off a screen shot and no recording is needed. The title bar is the band between the top of what the window's appearance changed on the screen and the top of the program's content.
final class FearlessGuiTest{
  static{ Err.setUp(AssertionFailedError.class,Assertions::assertEquals,Assertions::assertTrue); }
  private static final String app= "fearlessBin"+ResolveResource.versionId;
  private static final Path launcher= Fs.isWindows()
    ? ResolveResource.portableFolderOut.resolve(app).resolve(app+".exe")
    : ResolveResource.portableFolderOut.resolve(app).resolve("bin").resolve(app);
  private static final Path project= ResolveResource.integrationTests.resolve("testGuiPilot");
  private static final int window= 0xFAECD6, swapKeys= 0x60C8FA, fix= 0xFA965A, corner= 0xAA6EF0, swapContent= 0xF05AAA,
    grow= 0x78DC78, clear= 0x5A82FA, replace= 0xFADC3C, nudge= 0x3CD2C8, shift= 0x8C643C, painted= 0xE6E63C, foreground= 0x145AC8,
    twice= 0xDC50DC, boxP= 0x64A03C, boxQ= 0x3C3CB4, boxR= 0xB43C3C, moved= 0xC87828, movedRect= 0x28C878;
  private final Pilot pilot= new Pilot();
  private final Path out= Fs.of(()->Files.createTempFile("testGuiPilot",".txt"));
  private Process run;
  private BufferedImage desk;
  private Rectangle content;
  private int titleBar;
  /// Action 3: hold SPACE down.
  /// Action 4: let SPACE go two seconds later.
  /// Action 5: hold SPACE down again.
  /// Action 6: click Swap keys, which replaces the key map while SPACE is down with one that knows only ESCAPE.
  /// Action 7: let SPACE go two seconds later: the map that saw SPACE go down twice also saw it go up twice.
  @Test void aKeyHeldWhileTheKeyMapIsReplacedIsReleasedOnce() throws InterruptedException{
    start();
    pilot.keys(KeyEvent.VK_SPACE);
    letGo();
    pilot.keys(KeyEvent.VK_SPACE);
    click(swapKeys);
    letGo();
    ends("SPACE pressed 2, released 2");
  }
  /// Action 3: hold Q and W down.
  /// Action 4: click Swap keys, which replaces the key map while Q and W are down: the old map lets both go at once. Letting the first go makes the band around Twice paint an error; letting the second go waits half a second and takes the error away, so the error shows only if a frame lands between the two.
  /// Action 5: let Q and W go two seconds later.
  @Test void theKeysLetGoByAKeyMapSwapShowTogether() throws InterruptedException{
    start();
    pilot.keys(KeyEvent.VK_Q,KeyEvent.VK_W);
    click(swapKeys);
    letGo();
    ends("Q and W released");
  }
  /// Action 3: carry the window by its title bar until its bottom right corner is near the screen's.
  /// Action 4: click Fix, which asks for half the screen as the window size: the program never placed the window, so where the person put it is no reason to fail.
  @Test void aWindowCarriedAwayStillTakesTheSizeTheProgramAsksFor() throws InterruptedException{
    start();
    pilot.drag((int)content.getCenterX(),titleBar,desk.getWidth()-40-content.width/2,titleBar+desk.getHeight()-40-content.y-content.height);
    click(fix);
    ends("Fix ran");
  }
  /// Action 3: double click the title bar, which makes the window as large as the screen.
  /// Action 4: click Corner, which places the window a quarter of the screen away from its top left: the window the program sized fits there, so the size the person gave it is no reason to fail.
  @Test void aWindowMadeLargerStillGoesWhereTheProgramPlacesIt() throws InterruptedException{
    start();
    pilot.doubleClick((int)content.getCenterX(),titleBar);
    click(corner);
    ends("Corner ran");
  }
  /// Action 3: click Grow at its left edge and keep still: the label beside it grows under the pointer.
  @Test void hoverFollowsANeighbourGrowingUnderAStillPointer() throws InterruptedException{
    start();
    var at= find(grow);
    pilot.click(at.x+1,(int)at.getCenterY());
    ends("hover: label I");
  }
  /// Action 3: click Clear and keep still: a box takes the place of Clear under the pointer.
  @Test void hoverFollowsAClearedPane() throws InterruptedException{
    start();
    click(clear);
    ends("hover: box J");
  }
  /// Action 3: click Replace and keep still: the border's center slot is replaced under the pointer.
  @Test void hoverFollowsAReplacedBorderSlot() throws InterruptedException{
    start();
    click(replace);
    ends("hover: box K");
  }
  /// Action 3: click Nudge and keep still: the program moves the window 100 pixels sideways, so a box beside Nudge comes under the pointer.
  @Test void hoverFollowsAWindowTheProgramMoves() throws InterruptedException{
    start();
    click(nudge);
    ends("hover: [###] box L");
  }
  /// Action 3: click Swap content and keep still: the whole content is replaced under the pointer.
  @Test void hoverFollowsSwappedContent() throws InterruptedException{
    start();
    click(swapContent);
    ends("hover: new content");
  }
  /// Action 3: click Shift and keep still: its task clears the pane around Shift, adds a box that would be under the pointer, waits half a second, and adds a second box that shifts the first one away from the pointer. The pointer is only ever over a box in a layout the task never finished, so no box is entered.
  @Test void hoverSeesOnlyTheLayoutOfAFinishedTask() throws InterruptedException{
    start();
    click(shift);
    ends("Shift ran");
  }
  /// Action 3: click Twice: its first action makes the band around it paint an error, its second waits half a second and takes the error away, so the error shows only if a frame lands between the two actions of one click.
  @Test void theActionsOfOneClickShowTogether() throws InterruptedException{
    start();
    click(twice);
    ends("Twice ran");
  }
  /// Action 3: click box P: its first press handler makes the band around it paint an error, its second waits half a second and takes the error away, so the error shows only if a frame lands between the two handlers of one press.
  @Test void theHandlersOfOnePressShowTogether() throws InterruptedException{
    start();
    click(boxP);
    ends("pressed box P");
  }
  /// Action 3: glide from the middle of box Q to the middle of box R beside it: leaving Q makes the band around them paint an error, entering R waits half a second and takes the error away, so the error shows only if a frame lands between leaving one and entering the other.
  @Test void leavingAndEnteringShowTogether() throws InterruptedException{
    start();
    var q= find(boxQ);
    var r= find(boxR);
    pilot.glide((int)q.getCenterX(),(int)q.getCenterY(),Pilot.none,(int)r.getCenterX(),(int)r.getCenterY(),Pilot.none);
    ends("hover: box R");
  }
  /// Action 3: look at the middle of the small yellow pane: its painter draws there without choosing a colour, so in the pane's foreground.
  @Test void aPanePainterStartsWithTheForeground() throws InterruptedException{
    start();
    var at= find(painted);
    assertEquals(Integer.toHexString(foreground),Integer.toHexString(pilot.shot().getRGB((int)at.getCenterX(),(int)at.getCenterY())&0xffffff));
    ends("nothing seen");
  }
  /// Action 3: look at the middle of the small orange pane: its painter moves the position of its graphics to there in one statement, and draws a green rectangle through the same graphics in the next one.
  @Test void aPositionMovesTheGraphicsItIsCalledOn() throws InterruptedException{
    start();
    var at= find(moved);
    assertEquals(Integer.toHexString(movedRect),Integer.toHexString(pilot.shot().getRGB((int)at.getCenterX(),(int)at.getCenterY())&0xffffff));
    ends("nothing seen");
  }
  private void start(){
    desk= pilot.shot();
    pilot.glide(desk.getWidth()-1,desk.getHeight()/2,Pilot.none,desk.getWidth()-1,desk.getHeight()/2,Pilot.none);
    desk= pilot.shot();
    run= Fs.of(()->new ProcessBuilder(launcher.toString(),project.toString()).redirectErrorStream(true).redirectOutput(out.toFile()).start());
    long end= System.nanoTime()+TimeUnit.MINUTES.toNanos(2);
    for (content= find(window); content.isEmpty(); content= find(window)){
      assertTrue(run.isAlive() && System.nanoTime()<end,()->Fs.readUtf8(out));
      Pilot.pause(500);
    }
    Pilot.pause(1000);
    titleBar= (Pilot.changed(desk,pilot.shot(),6).y+content.y)/2;
    pilot.click(content.x+2,content.y+2);
  }
  private void letGo(){ Pilot.pause(2000); pilot.keys(); }
  private void click(int rgb){
    var at= find(rgb);
    assert !at.isEmpty();
    pilot.click((int)at.getCenterX(),(int)at.getCenterY());
  }
  private Rectangle find(int rgb){
    var img= pilot.shot();
    int w= img.getWidth(), h= img.getHeight(), x0= w, y0= h, x1= -1, y1= -1;
    var px= img.getRGB(0,0,w,h,null,0,w);
    for (int i= 0; i<px.length; i++){
      if ((px[i]&0xffffff)!=rgb){ continue; }
      x0= Math.min(x0,i%w); x1= Math.max(x1,i%w); y0= Math.min(y0,i/w); y1= Math.max(y1,i/w);
    }
    return new Rectangle(x0,y0,x1-x0+1,y1-y0+1);
  }
  private void ends(String expected) throws InterruptedException{
    Pilot.pause(1000);
    pilot.chord(KeyEvent.VK_ESCAPE);
    assertTrue(run.waitFor(1,TimeUnit.MINUTES));
    Err.strCmp(expected+"\nmut Pilot.accept(_) error line: 29 in file _pilot/_rank_app.fear",Fs.readUtf8(out));
  }
  @AfterEach void stop(){
    run.descendants().forEach(ProcessHandle::destroyForcibly);
    run.destroyForcibly();
    Fs.ofV(()->Files.delete(out));
  }
}

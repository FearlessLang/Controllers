package gui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPopupMenu;

import org.junit.jupiter.api.Test;

final class StandInEntryTest{
  final ArrayList<String> log= new ArrayList<>();
  StandIn.Entry entry(){
    var res= new StandIn.Entry("Quit manager",() -> log.add("chosen"));
    res.setSize(100,20);
    return res;
  }
  static void mouse(StandIn.Entry e, int id, int x, int button){
    e.processMouseEvent(new MouseEvent(e,id,System.currentTimeMillis(),0,x,10,1,false,button));
  }
  static void click(StandIn.Entry e, int button){
    mouse(e,MouseEvent.MOUSE_PRESSED,50,button);
    mouse(e,MouseEvent.MOUSE_RELEASED,50,button);
  }
  @Test void aClickWithAnyButtonChoosesTheEntry(){
    var e= entry();
    click(e,MouseEvent.BUTTON1);
    click(e,MouseEvent.BUTTON3);
    assertEquals(List.of("chosen","chosen"),log);
  }
  @Test void aReleaseAloneDoesNothing(){
    var e= entry();
    mouse(e,MouseEvent.MOUSE_RELEASED,50,MouseEvent.BUTTON1);
    assertEquals(List.of(),log);
  }
  @Test void aPressReleasedOutsideTheEntryDoesNothing(){
    var e= entry();
    mouse(e,MouseEvent.MOUSE_PRESSED,50,MouseEvent.BUTTON1);
    mouse(e,MouseEvent.MOUSE_RELEASED,150,MouseEvent.BUTTON1);
    mouse(e,MouseEvent.MOUSE_RELEASED,50,MouseEvent.BUTTON1);
    assertEquals(List.of(),log);
  }
  @Test void onlyAClickChoosesTheEntry(){
    entry().doClick();
    assertEquals(List.of(),log);
  }
  @Test void aClickInTheFirstHalfSecondDoesNothing() throws InterruptedException{
    var e= entry();
    new JPopupMenu().add(e);
    click(e,MouseEvent.BUTTON1);
    assertEquals(List.of(),log);
    Thread.sleep(500);
    click(e,MouseEvent.BUTTON1);
    assertEquals(List.of("chosen"),log);
  }
}

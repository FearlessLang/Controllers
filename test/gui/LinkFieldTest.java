package gui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

/// When a field of the Links area sends its edit.
final class LinkFieldTest{
  @SuppressWarnings("serial")
  private static final class Field extends JTextField{ void key(KeyEvent e){ processKeyEvent(e); } }
  private final Field field= new Field();
  private final ArrayList<String> sent= new ArrayList<>();
  LinkFieldTest(){ Panel.sendsOn(field,() -> sent.add(field.getText())); }
  private void type(String keys) throws Exception{
    SwingUtilities.invokeAndWait(() -> keys.chars().forEach(c -> field.key(new KeyEvent(field,KeyEvent.KEY_TYPED,0,0,KeyEvent.VK_UNDEFINED,(char)c))));
  }
  private List<String> sent() throws Exception{
    var res= new ArrayList<String>();
    SwingUtilities.invokeAndWait(() -> res.addAll(sent));
    return res;
  }
  @Test void aCharacterNoTypeNameHoldsSendsAtOnceAndIsNotAdded() throws Exception{
    type("Coin_2' Bag!");
    assertEquals(List.of("Coin_2' Bag"),sent());
    type("\u00e9");
    assertEquals(List.of("Coin_2' Bag","Coin_2' Bag"),sent());
  }
  @Test void enterAndEscSendAtOnce() throws Exception{
    type("Coin\n");
    assertEquals(List.of("Coin"),sent());
    type("s\u001b");
    assertEquals(List.of("Coin","Coins"),sent());
  }
  @Test void oneSecondWithNoKeyTypedSends() throws Exception{
    type("Co");
    Thread.sleep(700);
    type("in\b");
    Thread.sleep(700);
    assertEquals(List.of(),sent());
    Thread.sleep(800);
    assertEquals(List.of("Coin"),sent());
  }
}

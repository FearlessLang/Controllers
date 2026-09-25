package gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import controller.Manager.State;
import controller.Registry.Kind;
import tools.Fs;
import utils.Box;
import utils.ThrowingConsumer;

/// Renders the tiles grid off screen and checks it actually draws, without a real display.
final class TilesShotTest{
  private static State ten(Path dir){
    var names= Stream.of("someproject","otherproject","map_editor","webshop","hello","sudoku","payroll","tetris","notes","weather");
    return new State(names.map(n->IconsTest.project(Fs.of(()->java.nio.file.Files.createDirectories(dir.resolve(n))),Kind.dataReadOnly)).toList(),Optional.of(dir.resolve("hello")));
  }
  @Test void theRegisteredFoldersLookLikeAGridOfTiles(@TempDir Path dir){
    var tiles= onEdt(()->new Tiles(_->{}));
    onEdt(()->{ tiles.render(ten(dir)); return null; });
    assertTrue(colours(shoot(tiles,760,420)) > 40,"the tiles drew nothing");
    assertEquals("hello",onEdt(()->tiles.list.getSelectedValue().alias()));
  }
  @Test void reorderingKeepsTheTilesDrawn(@TempDir Path dir){
    var tiles= onEdt(()->new Tiles(_->{}));
    onEdt(()->{ tiles.render(ten(dir)); tiles.sort.setSelectedItem(Tiles.Sort.Modified); return null; });
    assertTrue(colours(shoot(tiles,760,420)) > 40);
  }
  @Test void clickingATileAsksToSelectItsProject(@TempDir Path dir){
    var picked= new ArrayList<String>();
    var tiles= onEdt(()->new Tiles(picked::add));
    onEdt(()->{ tiles.render(ten(dir)); return null; });
    shoot(tiles,760,420);
    onEdt(()->{
      var at= tiles.list.getCellBounds(2,2);
      tiles.list.dispatchEvent(new MouseEvent(tiles.list,MouseEvent.MOUSE_CLICKED,0,0,at.x+5,at.y+5,1,false));
      return null;
    });
    assertEquals(List.of(onEdt(()->tiles.list.getModel().getElementAt(2).alias())),picked);
  }
  private static BufferedImage shoot(JComponent c, int w, int h){
    return onEdt(()->{
      c.setSize(w,h);
      layout(c);
      var res= new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
      var g= res.createGraphics();
      g.setColor(Color.white);
      g.fillRect(0,0,w,h);
      c.printAll(g);
      g.dispose();
      return res;
    });
  }
  private static void layout(Component c){
    if (!(c instanceof Container p)){ return; }
    p.doLayout();
    for (var kid: p.getComponents()){ layout(kid); }
  }
  private static long colours(BufferedImage img){
    return java.util.Arrays.stream(img.getRGB(0,0,img.getWidth(),img.getHeight(),null,0,img.getWidth())).distinct().count();
  }
  private static <T> T onEdt(Supplier<T> f){
    var out= new Box<T>(null);
    ThrowingConsumer.of(SwingUtilities::invokeAndWait).accept(()->out.set(f.get()));
    return out.get();
  }
}

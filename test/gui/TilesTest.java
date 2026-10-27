package gui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import controller.Manager.State;
import controller.Registry.Kind;
import tools.Fs;

/// The grid of tiles, from the State handed to it.
final class TilesTest{
  private static Path folder(Path dir, String name){
    var res= dir.resolve(name);
    Fs.ensureDir(res);
    return res;
  }
  private static void mouse(Tiles tiles, int id, int x, int y){
    tiles.list.dispatchEvent(new MouseEvent(tiles.list, id, System.currentTimeMillis(), 0, x, y, 1, false, MouseEvent.BUTTON1));
  }
  @Test void aClickBesideTheTilesLeavesTheTileOfTheShownProjectHighlighted(@TempDir Path dir){
    var a= IconsTest.project(folder(dir, "a"), Kind.idle);
    var b= IconsTest.project(folder(dir, "b"), Kind.idle);
    var tiles= new Tiles();
    tiles.list.setSize(600, 200);
    tiles.render(new State(List.of(a, b), Optional.of(a.folder()), List.of()));
    assertEquals(a, tiles.list.getSelectedValue());
    mouse(tiles, MouseEvent.MOUSE_PRESSED, 500, 40);
    mouse(tiles, MouseEvent.MOUSE_RELEASED, 500, 40);
    mouse(tiles, MouseEvent.MOUSE_CLICKED, 500, 40);
    assertEquals(a, tiles.list.getSelectedValue());
  }
}

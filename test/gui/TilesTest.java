package gui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import javax.swing.JLabel;

import org.junit.jupiter.api.Test;
import tempDirs.RealTempDir;

import controller.Manager.State;
import controller.Project;
import controller.Registry.Kind;
import tools.Fs;

final class TilesTest{
  @Test void onlyTheTileOfTheShownProjectIsHighlightedWhateverTheListSelects(@RealTempDir Path dir){
    Fs.ensureDir(dir.resolve("a"));
    Fs.ensureDir(dir.resolve("b"));
    var a= IconsTest.project(dir.resolve("a"), Kind.idle);
    var b= IconsTest.project(dir.resolve("b"), Kind.idle);
    var tiles= new Tiles();
    tiles.render(new State(List.of(a, b), Optional.of(a.folder()), List.of()));
    tiles.list.setSelectedIndex(1);
    assertEquals(List.of(true, false), List.of(highlighted(tiles, a, 0), highlighted(tiles, b, 1)));
  }
  private static boolean highlighted(Tiles tiles, Project p, int i){
    return ((JLabel)tiles.list.getCellRenderer().getListCellRendererComponent(tiles.list, p, i, tiles.list.isSelectedIndex(i), false)).getBorder() != null;
  }
}

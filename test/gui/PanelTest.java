package gui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Component;
import java.awt.Container;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.swing.JLabel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opentest4j.AssertionFailedError;

import controller.Facts;
import controller.Project;
import controller.Registry.Entry;
import controller.Registry.Kind;
import coordinator.MainsInfo;
import tools.Fs;
import utils.Err;

/// What the project area shows, from the projects handed to it.
final class PanelTest{
  static{ Err.setUp(AssertionFailedError.class, Assertions::assertEquals, Assertions::assertTrue); }
  private static final MainsInfo diary= new MainsInfo(Map.of("hello.Hello", new MainsInfo.Main(List.of(), List.of(), List.of())), List.of(), List.of("hello.Diary"), List.of());
  private static Project project(Path folder, Kind kind, Map<String,String> mounts, Optional<MainsInfo> mains){
    var alias= folder.getFileName().toString();
    var entry= new Entry(alias, folder.toAbsolutePath().normalize(), kind, List.of(), mounts, Map.of());
    return new Project(entry, Facts.of(entry.path(), alias, kind, _ -> new coordinator.Coordinator.State.OutOfDate()).orElseThrow(), mains, Project.noClaims, "");
  }
  private static Stream<Component> all(Component c){
    if (!(c instanceof Container k)){ return Stream.of(c); }
    return Stream.concat(Stream.of(c), Stream.of(k.getComponents()).flatMap(PanelTest::all));
  }
  private static List<String> brokenMounts(Panel panel){
    return all(panel.root).filter(c -> c instanceof JLabel).map(c -> ((JLabel)c).getText()).filter(t -> t != null && t.startsWith("The mount of")).toList();
  }
  @Test void aShortcutWithNoValidFileNameShowsNoShortcutFile(@TempDir Path dir){
    var folder= dir.resolve("hello");
    Fs.writeUtf8(folder.resolve("hello.fearless"), "");
    var claim= new MainsInfo.Claim("base.IconsConflict", "icons/conflict.png", "", "", "", "fapp010");
    var claims= new MainsInfo(Map.of("hello.Foo'", new MainsInfo.Main(List.of(claim), List.of(), List.of(claim))), List.of(), List.of(), List.of());
    var p= project(folder, Kind.code, Map.of(), Optional.of(claims));
    var withClaims= new Project(p.entry(), p.facts(), p.mains(), claims, "");
    var panel= new Panel(_ -> {});
    panel.render(withClaims, List.of(withClaims), 800);
    assertEquals(List.of(), all(panel.root).filter(c -> c.getName() != null && c.getName().startsWith("shortcut ")).toList());
  }
  @Test void theDataAreaShowsTheMountBrokenOnceTheMountedDataProjectBecomesInvalid(@TempDir Path dir){
    var notesFolder= dir.resolve("notes");
    Fs.writeUtf8(notesFolder.resolve("notes.fearless"), "");
    Fs.writeUtf8(notesFolder.resolve("readme.txt"), "hi\n");
    Fs.writeUtf8(dir.resolve("hello").resolve("hello.fearless"), "");
    var hello= project(dir.resolve("hello"), Kind.code, Map.of("hello.Diary", "notes"), Optional.of(diary));
    var notes= project(notesFolder, Kind.dataRead, Map.of(), Optional.empty());
    var panel= new Panel(_ -> {});
    panel.render(hello, List.of(hello, notes), 800);
    assertEquals(List.of(), brokenMounts(panel));
    Fs.ofV(() -> Files.delete(notesFolder.resolve("notes.fearless")));
    var gone= project(notesFolder, Kind.dataRead, Map.of(), Optional.empty());
    panel.render(hello, List.of(hello, gone), 800);
    assertEquals(1, brokenMounts(panel).size());
    Err.strCmp("The mount of \"notes\" on \"hello.Diary\" is broken: the content of project \"notes\" is invalid: The marker file \"notes.fearless\" is missing from[###]", brokenMounts(panel).getFirst());
  }
}

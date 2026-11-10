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
    return new Project(entry, Facts.of(entry.path(), alias, kind, _ -> new coordinator.Coordinator.State.OutOfDate(), java.time.Instant.MAX).orElseThrow(), mains, Project.noClaims, "", List.of());
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
    var withClaims= new Project(p.entry(), p.facts(), p.mains(), claims, "", List.of());
    var panel= new Panel(_ -> {});
    panel.render(withClaims, List.of(withClaims), 800);
    assertEquals(List.of(), all(panel.root).filter(c -> c.getName() != null && c.getName().startsWith("shortcut ")).toList());
  }
  private static final String goodBadLater= """
PLAN\tRUN\t_hello/_rank_app.fear\tGood\tGood at line: 3\t3
<testcase classname="Good" name="Good at line: 3" file="_hello/_rank_app.fear" line="3"></testcase>
<testcase classname="Bad" name="Bad at line: 5" file="_hello/_rank_app.fear" line="5">
<failure>
<expected>7</expected>
<actual>6</actual>
Expected: 7 Actual: 6 &lt;here&gt;
second line
</failure>
</testcase>
PLAN\tDISABLED\t_hello/_rank_app.fear\tLater\tLater at line: 9\t9
""";
  @Test void theTestsAreaShowsTheNewestUnitTestLogWithAMarkOnEachTestCase(@TempDir Path dir){
    var folder= dir.resolve("hello");
    Fs.writeUtf8(folder.resolve("hello.fearless"), "");
    Fs.writeUtf8(folder.resolve(".out").resolve("logs").resolve("_base").resolve("unit_test_log$20261103_000000_000Z.log"), goodBadLater);
    var hello= project(folder, Kind.code, Map.of(), Optional.empty());
    var panel= new Panel(_ -> {});
    panel.render(hello, List.of(hello), 800);
    assertEquals("""
failed unit test log of 2026-11-03T00:00:00Z: 1 passed, 1 failed, 1 disabled
  passed Good at line: 3
  failed Bad at line: 5
     Expected: 7 Actual: 6 <here>
     second line
  disabled Later at line: 9
""", tests(panel));
  }
  @Test void theTestsAreaShowsEachUnitTestLogOfTheRunUnderTheMainThatWroteIt(@TempDir Path dir){
    var folder= dir.resolve("hello");
    Fs.writeUtf8(folder.resolve("hello.fearless"), "");
    var logs= folder.resolve(".out").resolve("logs").resolve("_base");
    Fs.writeUtf8(logs.resolve("unit_test_log$20261103_000001_000Z.log"), "<testcase classname=\"A\" name=\"A at line: 1\"></testcase>\n");
    Fs.writeUtf8(logs.resolve("unit_test_log$20261103_000003_000Z.log"), goodBadLater);
    var entry= new Entry("hello", folder.toAbsolutePath().normalize(), Kind.code, List.of(), Map.of(), Map.of());
    var start= java.time.Instant.parse("2026-11-03T00:00:00Z");
    var facts= Facts.of(entry.path(), "hello", Kind.code, _ -> new coordinator.Coordinator.State.OutOfDate(), start).orElseThrow();
    var ran= List.of(new Project.Ran("hello.Auto", start), new Project.Ran("hello.Mine", start.plusSeconds(2)));
    var hello= new Project(entry, facts, Optional.empty(), Project.noClaims, "", ran);
    var panel= new Panel(_ -> {});
    panel.render(hello, List.of(hello), 800);
    assertEquals("""
passed hello.Auto: 1 passed, 0 failed, 0 disabled
  passed A at line: 1
failed hello.Mine: 1 passed, 1 failed, 1 disabled
  passed Good at line: 3
  failed Bad at line: 5
     Expected: 7 Actual: 6 <here>
     second line
  disabled Later at line: 9
""", tests(panel));
  }
  @Test void aRunWritingNoUnitTestLogShowsNoTest(@TempDir Path dir){
    var folder= dir.resolve("hello");
    Fs.writeUtf8(folder.resolve("hello.fearless"), "");
    Fs.writeUtf8(folder.resolve(".out").resolve("logs").resolve("_base").resolve("unit_test_log$20261103_000001_000Z.log"), goodBadLater);
    var entry= new Entry("hello", folder.toAbsolutePath().normalize(), Kind.code, List.of(), Map.of(), Map.of());
    var start= java.time.Instant.parse("2026-11-03T00:00:05Z");
    var facts= Facts.of(entry.path(), "hello", Kind.code, _ -> new coordinator.Coordinator.State.OutOfDate(), start).orElseThrow();
    var hello= new Project(entry, facts, Optional.empty(), Project.noClaims, "", List.of(new Project.Ran("hello.Hello", start)));
    var panel= new Panel(_ -> {});
    panel.render(hello, List.of(hello), 800);
    assertEquals("", tests(panel));
  }
  @Test void aDamagedUnitTestLogStillShowsInTheTestsArea(@TempDir Path dir){
    var folder= dir.resolve("hello");
    Fs.writeUtf8(folder.resolve("hello.fearless"), "");
    var logs= folder.resolve(".out").resolve("logs").resolve("_base");
    Fs.writeUtf8(logs.resolve("unit_test_log$20261103_000000_000Z.log"), "<testcase classname=\"Bad\" name=\"Bad at line: 5\">\n<failure>\ncut short\n</testcase>\n");
    var hello= project(folder, Kind.code, Map.of(), Optional.empty());
    var panel= new Panel(_ -> {});
    panel.render(hello, List.of(hello), 800);
    assertEquals("failed unit test log of 2026-11-03T00:00:00Z: 0 passed, 1 failed, 0 disabled\n  failed Bad at line: 5\n     cut short\n", tests(panel));
    Fs.ofV(() -> Files.write(logs.resolve("unit_test_log$20261103_000000_001Z.log"), new byte[]{(byte)0xFF, '\n'}));
    var again= project(folder, Kind.code, Map.of(), Optional.empty());
    assertEquals(Optional.empty(), again.facts().problem());
    panel.render(again, List.of(again), 800);
    assertEquals("passed unit test log of 2026-11-03T00:00:00.001Z: 0 passed, 0 failed, 0 disabled\n", tests(panel));
  }
  /// every row of the Tests tree, expanded or not, with its mark
  private static String tests(Panel panel){ return rows((javax.swing.tree.DefaultMutableTreeNode)panel.testTree.getModel().getRoot(), -1); }
  private static String rows(javax.swing.tree.DefaultMutableTreeNode n, int depth){
    var row= (Panel.TestRow)n.getUserObject();
    var mark= row.mark().map(c -> c == Panel.passed ? "passed " : c == Panel.failed ? "failed " : "disabled ").orElse(" ");
    var own= depth < 0 ? "" : "  ".repeat(depth)+mark+row.text()+"\n";
    return own+java.util.Collections.list(n.children()).stream().map(c -> rows((javax.swing.tree.DefaultMutableTreeNode)c, depth+1)).collect(java.util.stream.Collectors.joining());
  }
  @Test void theStatusLineSaysTheStateTheProblemAndTheBrokenMounts(@TempDir Path dir){
    Fs.writeUtf8(dir.resolve("hello").resolve("hello.fearless"), "");
    var hello= project(dir.resolve("hello"), Kind.code, Map.of("hello.Diary", "notes"), Optional.empty());
    var panel= new Panel(_ -> {});
    panel.render(hello, List.of(hello), 800);
    assertEquals(List.of("code: compilation cache out of date; the mount on hello.Diary is broken (see Data)"), statuses(panel));
    Fs.writeUtf8(dir.resolve("hello").resolve("other.fearless"), "");
    var twice= project(dir.resolve("hello"), Kind.code, Map.of(), Optional.empty());
    panel.render(twice, List.of(twice), 800);
    Err.strCmp("code: invalid content: [###]", statuses(panel).getFirst());
  }
  private static List<String> statuses(Panel panel){ return all(panel.root).filter(c -> "status".equals(c.getName())).map(c -> ((JLabel)c).getText()).toList(); }
  @Test void aMountOfAProjectThatIsNotRegisteredIsMarkedAlsoBeforeACompilation(@TempDir Path dir){
    Fs.writeUtf8(dir.resolve("hello").resolve("hello.fearless"), "");
    var hello= project(dir.resolve("hello"), Kind.code, Map.of("hello.Diary", "notes"), Optional.empty());
    var panel= new Panel(_ -> {});
    panel.render(hello, List.of(hello), 800);
    assertEquals(List.of("The mount of \"notes\" on \"hello.Diary\" is broken: no project \"notes\" is registered."), brokenMounts(panel));
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
    Fs.writeUtf8(notesFolder.resolve("other.fearless"), "");
    var gone= project(notesFolder, Kind.dataRead, Map.of(), Optional.empty());
    panel.render(hello, List.of(hello, gone), 800);
    assertEquals(1, brokenMounts(panel).size());
    Err.strCmp("The mount of \"notes\" on \"hello.Diary\" is broken: the content of project \"notes\" is invalid: More than one .fearless marker file was found in[###]", brokenMounts(panel).getFirst());
  }
}

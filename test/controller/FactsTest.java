package controller;

import static controller.Errs.err;
import static controller.Errs.same;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.Locale;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import controller.Registry.Kind;
import coordinator.Coordinator;
import fileSupport.LogFiles;
import tools.Fs;
import utils.Range;

final class FactsTest{
  static Path project(Path dir, String name){
    var res= dir.resolve(name);
    Fs.writeUtf8(res.resolve("_hello").resolve("_rank_app.fear"), "use base.Main as Main;\n");
    Fs.writeUtf8(res.resolve("readme"), "hi\n");
    Fs.writeUtf8(res.resolve(name.toLowerCase(Locale.ROOT)+".fearless"), "");
    backdated(res);
    return res;
  }
  static void backdated(Path folder){
    var old= FileTime.fromMillis(System.currentTimeMillis()-3_600_000);
    Fs.walkV(folder, s -> s.filter(Files::isRegularFile).forEach(f -> Fs.ofV(() -> Files.setLastModifiedTime(f, old))));
  }
  private static void at(Path file, String content, long stamp){ FakeCache.at(file, content, stamp); }
  /// The real Coordinator.check, over a base cache holding a base.json with no type.
  static Facts facts(Path project, String alias, Kind kind){
    var base= project.getParent().resolve("baseCache");
    FakeCache.at(base.resolve("base.json"), FakeCache.json(FakeCache.manifest("{}")), 0);
    var c= new Coordinator(){ @Override public Optional<Path> baseCachePath(){ return Optional.of(base); } };
    return Facts.of(project, alias, kind, c::check);
  }
  static boolean upToDate(Facts f){ return f.stale().isEmpty(); }
  //A log or an Eclipse report lands after the build stamp, inside the project folder
  @Test void recognisesThePackagesOfTheProject(@TempDir Path dir){
    var project= project(dir, "someProject");
    Fs.writeUtf8(project.resolve("_other").resolve("_rank_app.fear"), "");
    assertTrue(facts(project, "someproject", Kind.code).problem().isEmpty());
  }
  @Test void brokenNamesAreReportedNotHidden(@TempDir Path dir){
    var project= project(dir, "someProject");
    Fs.writeUtf8(project.resolve("_hello").resolve("Bad.fear"), "");
    var facts= facts(project, "someproject", Kind.code);
    assertFalse(facts.problem().isEmpty());
    assertTrue(facts.problem().orElseThrow().contains("Bad.fear"));
  }
  @Test void withNoCacheThereIsSomethingToCompileAndNothingToRun(@TempDir Path dir){
    var facts= facts(project(dir, "someProject"), "someproject", Kind.code);
    assertEquals(Optional.of("The compilation cache of this project is out of date."), facts.stale());
    assertEquals(Optional.empty(), facts.mains());
  }
  @Test void aCodeProjectWithNoPackageUsesOnlyBaseAndIsUpToDate(@TempDir Path dir){
    var project= dir.resolve("someProject");
    Fs.writeUtf8(project.resolve("readme"), "hi\n");
    Fs.writeUtf8(project.resolve("someproject.fearless"), "");
    backdated(project);
    var facts= facts(project, "someproject", Kind.code);
    assertEquals(Optional.empty(), facts.problem());
    assertTrue(upToDate(facts));
  }
  @Test void anOutFolderHoldingNoPackageIsOutOfDate(@TempDir Path dir){
    var project= project(dir, "someProject");
    Fs.writeUtf8(project.resolve(Facts.outDir).resolve("_error.txt"), "broken");
    var facts= facts(project, "someproject", Kind.code);
    assertEquals(Optional.of("The compilation cache of this project is out of date."), facts.stale());
  }
  @Test void aShortcutFileAtTheTopLevelKeepsTheCacheUpToDateAndTheProjectValid(@TempDir Path dir){
    var project= project(dir, "someProject");
    at(project.resolve("bar.fapp042"), "", FakeCache.after(project)+5000);
    assertEquals(Optional.empty(), facts(project, "someproject", Kind.code).problem());
  }
  @Test void aDataFolderNeedsNoPackageStructure(@TempDir Path dir){
    var project= dir.resolve("publicFiles");
    Fs.writeUtf8(project.resolve("readme.txt"), "hi\n");
    Fs.writeUtf8(project.resolve("publicfiles.fearless"), "");
    var facts= facts(project, "publicfiles", Kind.dataRead);
    assertEquals(Optional.empty(), facts.problem());
    assertEquals(Optional.empty(), facts.stale());
  }
  @Test void aFolderWithoutItsMarkerIsInvalid(@TempDir Path dir){
    var project= project(dir, "someProject");
    same("[###]\"other.fearless\" is missing[###]", facts(project, "other", Kind.code).problem().orElseThrow());
  }
  @Test void aMissingFolderIsAProblemNotACrash(@TempDir Path dir){
    same("""
The folder of this project does not exist:
[###]gone
Restore it, or forget this project.""", facts(dir.resolve("gone"), "gone", Kind.code).problem().orElseThrow());
  }
  @Test void aFolderDeletedWhileItIsCheckedIsMissing(@TempDir Path dir){
    assertTimeoutPreemptively(Duration.ofSeconds(60), () -> {
      for (var i : Range.of(0, 20)){
        var project= project(dir, "p"+i);
        IntStream.range(0, 300).forEach(j -> Fs.writeUtf8(project.resolve("_hello").resolve("f"+j+".fear"), ""));
        var gone= Thread.startVirtualThread(() -> Fs.rmTree(project));
        facts(project, "p"+i, Kind.code);
        gone.join();
        same("The folder of this project does not exist:[###]", facts(project, "p"+i, Kind.code).problem().orElseThrow());
      }
    });
  }
  @Test void aFileDatedAtTheStartOf1970IsAProblem(@TempDir Path dir){
    var project= project(dir, "someProject");
    Fs.ofV(() -> Files.setLastModifiedTime(project.resolve("_hello").resolve("_rank_app.fear"), FileTime.fromMillis(-5000)));
    same("[###]Path: \"_hello/_rank_app.fear\"\n\nThis file is dated 19[###]", facts(project, "someproject", Kind.code).problem().orElseThrow());
  }
  @Test void anUnreadableFolderIsAProblemNotACrash(@TempDir Path dir){
    var project= project(dir, "someProject");
    var hidden= project.resolve("_hello");
    Disk.noRead(hidden);
    Assumptions.assumeFalse(Files.isReadable(hidden));
    same("""
The folder of this project cannot be read:
[###]someProject
access denied: [###]_hello
Give Fearless access to it, or forget this project.""", facts(project, "someproject", Kind.code).problem().orElseThrow());
    Disk.allow(hidden);
  }
  @Test void theIconIsTheSameWhileItsFileIs(@TempDir Path dir){
    var project= project(dir, "someProject");
    var png= project.resolve(".config").resolve("icon").resolve("a.png");
    Fs.ensureDir(png.getParent());
    Fs.ofV(() -> ImageIO.write(new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB), "png", png.toFile()));
    var first= facts(project, "someproject", Kind.code);
    assertEquals(first, facts(project, "someproject", Kind.code));
    assertSame(first.icon().orElseThrow().image(), facts(project, "someproject", Kind.code).icon().orElseThrow().image());
    assertEquals(4, first.icon().orElseThrow().image().getWidth());
    Fs.ofV(() -> ImageIO.write(new BufferedImage(6, 6, BufferedImage.TYPE_INT_RGB), "png", png.toFile()));
    Fs.ofV(() -> Files.setLastModifiedTime(png, FileTime.fromMillis(Fs.lastModified(png)+1000)));
    assertEquals(6, facts(project, "someproject", Kind.code).icon().orElseThrow().image().getWidth());
  }
  @Test void twoIconsAreAProjectProblem(@TempDir Path dir){
    var project= project(dir, "someProject");
    Fs.writeUtf8(project.resolve(".config").resolve("icon").resolve("a.png"), "");
    Fs.writeUtf8(project.resolve(".config").resolve("icon").resolve("b.png"), "");
    err("""
More than one .png file was found for this project's icon.

Looked in:
[###]icon

Found:
  a.png
  b.png

Keep exactly one .png file there.
""", () -> Facts.icon(project));
    assertFalse(facts(project, "someproject", Kind.code).problem().isEmpty());
  }
  @Test void anIconThatIsNotAnImageIsAProjectProblem(@TempDir Path dir){
    var project= project(dir, "someProject");
    Fs.writeUtf8(project.resolve(".config").resolve("icon").resolve("a.png"), "not an image");
    err("""
The icon of this project is not a PNG image Fearless can read.

File:
[###]a.png

The icon of a project is the only .png file in its folder ".config/icon".
Replace that file with a PNG image, or remove it.
""", () -> Facts.icon(project));
    assertTrue(facts(project, "someproject", Kind.code).problem().orElseThrow().startsWith("The icon of this project is not a PNG image"));
  }
  @Test void aProjectWithBrokenNamesKeepsItsIcon(@TempDir Path dir){
    var project= project(dir, "someProject");
    Fs.writeUtf8(project.resolve("_hello").resolve("Bad.fear"), "");
    var png= project.resolve(".config").resolve("icon").resolve("a.png");
    Fs.ensureDir(png.getParent());
    Fs.ofV(() -> ImageIO.write(new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB), "png", png.toFile()));
    var facts= facts(project, "someproject", Kind.code);
    assertTrue(facts.problem().orElseThrow().contains("Bad.fear"));
    assertEquals(png, facts.icon().orElseThrow().file());
  }
  @Test void aDataFolderStillRejectsUnsafeNames(@TempDir Path dir){
    var project= dir.resolve("publicFiles");
    Fs.writeUtf8(project.resolve("Bad Name.txt"), "hi\n");
    assertFalse(facts(project, "publicfiles", Kind.dataRead).problem().isEmpty());
  }
}
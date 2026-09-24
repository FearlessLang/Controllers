package controller;

import static controller.Errs.err;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import controller.Registry.Kind;
import fileSupport.LogFiles;
import tools.Fs;

final class FactsTest{
  static Path project(Path dir, String name){
    var res= dir.resolve(name);
    Fs.writeUtf8(res.resolve("_hello").resolve("_rank_app.fear"),"use base.Main as Main;\n");
    Fs.writeUtf8(res.resolve("readme"),"hi\n");
    return res;
  }
  static void cache(Path project, String pkg, long stamp){
    at(project.resolve(".fearless_out").resolve(pkg+".built"),"fear:/_"+pkg+"/_rank_app.fear",stamp);
    at(project.resolve(".fearless_out").resolve(pkg+".json"),"{}\n",stamp);
  }
  private static void at(Path file, String content, long stamp){
    Fs.writeUtf8(file,content);
    Fs.ofV(()->Files.setLastModifiedTime(file,FileTime.fromMillis(stamp)));
  }
  static long after(Path project){ return Facts.of(project,Kind.code).modified()+1000; }
  @Test void countsAndSizesTheSourceFiles(@TempDir Path dir){
    var facts= Facts.of(project(dir,"someProject"),Kind.code);
    assertEquals(2,facts.files());
    assertEquals("use base.Main as Main;\n".length()+"hi\n".length(),facts.bytes());
    assertTrue(facts.modified() > 0);
  }
  @Test void theCompiledCacheIsNotCountedAsSource(@TempDir Path dir){
    var project= project(dir,"someProject");
    cache(project,"hello",after(project));
    assertEquals(2,Facts.of(project,Kind.code).files());
  }
  //A log or an Eclipse report lands after the build stamp, inside the project folder
  @Test void whatFearlessWritesAboutAProjectIsNotCountedAsSource(@TempDir Path dir){
    var project= project(dir,"someProject");
    var stamp= after(project);
    cache(project,"hello",stamp);
    var written= project.resolve(LogFiles.runDir);
    at(written.resolve("eclipse").resolve("problems.txt"),"",stamp+1000);
    at(written.resolve("logs").resolve("_base").resolve("unit_test_log.log"),"x\n",stamp+1000);
    var facts= Facts.of(project,Kind.code);
    assertEquals(2,facts.files());
    assertTrue(facts.cacheUpToDate());
  }
  @Test void recognisesThePackagesOfTheProject(@TempDir Path dir){
    var project= project(dir,"someProject");
    Fs.writeUtf8(project.resolve("_other").resolve("_rank_app.fear"),"");
    var facts= Facts.of(project,Kind.code);
    assertEquals(List.of("hello","other"),facts.pkgs().stream().sorted().toList());
    assertTrue(facts.valid());
  }
  @Test void brokenNamesAreReportedNotHidden(@TempDir Path dir){
    var project= project(dir,"someProject");
    Fs.writeUtf8(project.resolve("_hello").resolve("Bad.fear"),"");
    var facts= Facts.of(project,Kind.code);
    assertFalse(facts.valid());
    assertEquals(List.of(),facts.pkgs());
    assertTrue(facts.problem().orElseThrow().contains("Bad.fear"));
  }
  @Test void withNoCacheThereIsSomethingToCompileAndNothingToRun(@TempDir Path dir){
    var facts= Facts.of(project(dir,"someProject"),Kind.code);
    assertFalse(facts.hasCache());
    assertFalse(facts.cacheUpToDate());
  }
  @Test void aCacheNewerThanEverySourceIsUpToDate(@TempDir Path dir){
    var project= project(dir,"someProject");
    cache(project,"hello",after(project));
    var facts= Facts.of(project,Kind.code);
    assertTrue(facts.hasCache());
    assertTrue(facts.cacheUpToDate());
  }
  @Test void aPackageBuiltBeforeOneOfItsFilesChangedIsStale(@TempDir Path dir){
    var project= project(dir,"someProject");
    Fs.writeUtf8(project.resolve("_other").resolve("_rank_app.fear"),"");
    cache(project,"hello",after(project));
    cache(project,"other",1000);
    assertFalse(Facts.of(project,Kind.code).cacheUpToDate());
  }
  @Test void eachPackageIsComparedWithItsOwnFilesOnly(@TempDir Path dir){
    var project= project(dir,"someProject");
    var other= project.resolve("_other").resolve("_rank_app.fear");
    at(other,"",5000);
    at(project.resolve("_hello").resolve("_rank_app.fear"),"use base.Main as Main;\n",1000);
    cache(project,"hello",2000);
    cache(project,"other",6000);
    assertTrue(Facts.of(project,Kind.code).cacheUpToDate());
  }
  @Test void aFileOutsideEveryPackageNeverMakesTheCacheStale(@TempDir Path dir){
    var project= project(dir,"someProject");
    var stamp= after(project);
    cache(project,"hello",stamp);
    at(project.resolve("readme"),"edited\n",stamp+5000);
    at(project.resolve("someproject.fearless"),"",stamp+5000);
    assertTrue(Facts.of(project,Kind.code).cacheUpToDate());
  }
  @Test void aFileAddedToAPackageMakesItStaleWhateverItsTime(@TempDir Path dir){
    var project= project(dir,"someProject");
    cache(project,"hello",after(project));
    at(project.resolve("_hello").resolve("more.fear"),"",1000);
    assertFalse(Facts.of(project,Kind.code).cacheUpToDate());
  }
  @Test void aDataFolderNeedsNoPackageStructure(@TempDir Path dir){
    var project= dir.resolve("publicFiles");
    Fs.writeUtf8(project.resolve("readme.txt"),"hi\n");
    var facts= Facts.of(project,Kind.dataReadOnly);
    assertTrue(facts.valid());
    assertEquals(List.of(),facts.pkgs());
  }
  @Test void twoIconsAreAProjectProblem(@TempDir Path dir){
    var project= project(dir,"someProject");
    Fs.writeUtf8(project.resolve(".config").resolve("icon").resolve("a.png"),"");
    Fs.writeUtf8(project.resolve(".config").resolve("icon").resolve("b.png"),"");
    err("""
      More than one .png file was found for this project's icon.

      Looked in:
      [###]icon

      Found:
        a.png
        b.png

      Keep exactly one .png file there.
      """,()->Facts.icon(project));
    assertFalse(Facts.of(project,Kind.code).valid());
  }
  @Test void anIconThatIsNotAnImageIsAProjectProblem(@TempDir Path dir){
    var project= project(dir,"someProject");
    Fs.writeUtf8(project.resolve(".config").resolve("icon").resolve("a.png"),"not an image");
    err("""
      The icon of this project is not a PNG image Fearless can read.

      File:
      [###]a.png

      The icon of a project is the only .png file in its ".config/icon" folder:
      replace that file with a PNG image, or remove it.
      """,()->Facts.icon(project));
    assertTrue(Facts.of(project,Kind.code).problem().orElseThrow().startsWith("The icon of this project is not a PNG image"));
  }
  @Test void aDataFolderStillRejectsUnsafeNames(@TempDir Path dir){
    var project= dir.resolve("publicFiles");
    Fs.writeUtf8(project.resolve("Bad Name.txt"),"hi\n");
    assertFalse(Facts.of(project,Kind.dataReadOnly).valid());
  }
}
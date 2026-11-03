package suggest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import coordinator.Coordinator;
import suggest.Cached.State;
import tools.Fs;

/// Cached.state against Coordinator.check, over a base cache holding a base.json with no type and a base.jar of time 1000.
final class CachedTest{
  static final long old= System.currentTimeMillis()-3_600_000;
  static String q(String s){ return "\""+s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")+"\""; }
  static String json(String snapshot){ return "{\"inputsSnapshot\":"+q(snapshot)+",\n\"manifest\":"+q("{\n  \"mains\": {},\n  \"attenuators\": [],\n  \"dataRead\": [],\n  \"dataEdit\": []\n}\n")+",\n\"types\":[]}\n"; }
  static void at(Path file, String content, long stamp){
    Fs.writeUtf8(file, content);
    Fs.ofV(() -> Files.setLastModifiedTime(file, FileTime.fromMillis(stamp)));
  }
  static Path base(Path dir){
    var base= dir.resolve("baseCache");
    at(base.resolve("base.json"), json(""), 1000);
    at(base.resolve("base.jar"), "", 1000);
    return base;
  }
  /// the package lib of rank core, and the package app of rank app, with a map directive of app overriding the one of lib
  static Path project(Path dir){
    var res= dir.resolve("p");
    at(res.resolve("p.fearless"), "", old);
    at(res.resolve("_lib").resolve("_rank_core.fear"), "map y as _base in _app;\n", old);
    at(res.resolve("_app").resolve("_rank_app.fear"), "use base.Main as Main;\nmap y as _lib in _app instead of _base;\n", old);
    at(res.resolve("_app").resolve("a.fear"), "", old);
    return res;
  }
  static void compiled(Path project){
    var out= project.resolve(".fearless_out");
    at(out.resolve("_compile_start.txt"), "", old+500);
    at(out.resolve("lib.json"), json("fear:/_lib/_rank_core.fear\n---\nbase 1000\n---\n"), old+1000);
    at(out.resolve("lib.jar"), "", old+1000);
    at(out.resolve("app.json"), json("fear:/_app/_rank_app.fear\nfear:/_app/a.fear\n---\nbase 1000\nlib "+(old+1000)+"\n---\ny lib"), old+2000);
    at(out.resolve("app.jar"), "", old+2000);
  }
  static void same(State expected, Path project, Path base){
    var c= new Coordinator(){ @Override public Optional<Path> baseCachePath(){ return Optional.of(base); } };
    var coordinator= switch (c.check(project)){
      case Coordinator.State.Invalid _ -> State.invalid;
      case Coordinator.State.OutOfDate _ -> State.outOfDate;
      case Coordinator.State.Compiled _ -> State.compiled;
    };
    assertEquals(expected, coordinator);
    assertEquals(expected, Cached.state(project, base));
  }
  @Test void aCacheTheCoordinatorFindsCompiledIsCompiled(@TempDir Path dir){
    var project= project(dir);
    compiled(project);
    same(State.compiled, project, base(dir));
  }
  @Test void withNoCacheTheProjectIsOutOfDate(@TempDir Path dir){ same(State.outOfDate, project(dir), base(dir)); }
  @Test void aProjectWithNoPackageAndNoCacheIsCompiled(@TempDir Path dir){
    var project= dir.resolve("p");
    at(project.resolve("p.fearless"), "", old);
    same(State.compiled, project, base(dir));
  }
  @Test void aFileNewerThanTheStartOfTheLastCompileMakesTheProjectOutOfDate(@TempDir Path dir){
    var project= project(dir);
    compiled(project);
    at(project.resolve("_app").resolve("a.fear"), "", old+3000);
    same(State.outOfDate, project, base(dir));
  }
  @Test void anUnfinishedFileMakesTheProjectOutOfDate(@TempDir Path dir){
    var project= project(dir);
    compiled(project);
    at(project.resolve(".fearless_out").resolve("_unfinished.txt"), "", old);
    same(State.outOfDate, project, base(dir));
  }
  @Test void anOtherFileMakesTheProjectOutOfDate(@TempDir Path dir){
    var project= project(dir);
    compiled(project);
    at(project.resolve(".fearless_out").resolve("junk.txt"), "", old);
    same(State.outOfDate, project, base(dir));
  }
  @Test void theErrorFileAndTheIconsAreNoOtherFiles(@TempDir Path dir){
    var project= project(dir);
    compiled(project);
    at(project.resolve(".fearless_out").resolve("_error.txt"), "broken", old);
    at(project.resolve(".fearless_out").resolve("icons").resolve("a.png"), "", old);
    at(project.resolve(".fearless_out").resolve("app.html"), "", old);
    same(State.compiled, project, base(dir));
  }
  @Test void aChangedMapDirectiveWithAnOldTimeMakesTheProjectOutOfDate(@TempDir Path dir){
    var project= project(dir);
    compiled(project);
    at(project.resolve("_app").resolve("_rank_app.fear"), "map z as _base in _app;\nmap y as _lib in _app instead of _base;\n", old);
    same(State.outOfDate, project, base(dir));
  }
  @Test void aLowerJarWithAnotherTimeMakesTheProjectOutOfDate(@TempDir Path dir){
    var project= project(dir);
    compiled(project);
    Fs.ofV(() -> Files.setLastModifiedTime(project.resolve(".fearless_out").resolve("lib.jar"), FileTime.fromMillis(old+1500)));
    same(State.outOfDate, project, base(dir));
  }
  @Test void aRemovedPackageMakesTheProjectOutOfDate(@TempDir Path dir){
    var project= project(dir);
    compiled(project);
    Fs.rmTree(project.resolve("_lib"));
    at(project.resolve("_app").resolve("_rank_app.fear"), "", old);
    same(State.outOfDate, project, base(dir));
  }
  @Test void aMalformedJsonMakesTheProjectOutOfDate(@TempDir Path dir){
    var project= project(dir);
    compiled(project);
    at(project.resolve(".fearless_out").resolve("app.json"), "{", old+2000);
    assertEquals(State.outOfDate, Cached.state(project, base(dir)));
  }
  @Test void aFearFileOutsideOfAPackageIsInvalid(@TempDir Path dir){
    var project= project(dir);
    at(project.resolve("top.fear"), "", old);
    same(State.invalid, project, base(dir));
  }
  @Test void twoRankFilesInAPackageAreInvalid(@TempDir Path dir){
    var project= project(dir);
    at(project.resolve("_app").resolve("_rank_tool.fear"), "", old);
    same(State.invalid, project, base(dir));
  }
  @Test void aPackageInsideAZipIsCompiledWhenItsSnapshotNamesTheFilesInsideTheZip(@TempDir Path dir){
    var project= dir.resolve("p");
    at(project.resolve("p.fearless"), "", old);
    var bytes= new ByteArrayOutputStream();
    Fs.ofV(() -> {
      try (var zip= new ZipOutputStream(bytes)){
        var e= new ZipEntry("_zp/_rank_core.fear");
        e.setTime(old);
        zip.putNextEntry(e);
        zip.write("".getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
      }
    });
    var file= project.resolve("extra.zip");
    Fs.ofV(() -> Files.write(file, bytes.toByteArray()));
    Fs.ofV(() -> Files.setLastModifiedTime(file, FileTime.fromMillis(old)));
    var out= project.resolve(".fearless_out");
    at(out.resolve("_compile_start.txt"), "", old+500);
    at(out.resolve("zp.json"), json("fear:/extra/_zp/_rank_core.fear\n---\nbase 1000\n---\n"), old+1000);
    at(out.resolve("zp.jar"), "", old+1000);
    same(State.compiled, project, base(dir));
  }
  @Test void theRankFileOfAPackageIsFoundUnderAnyFolderOfThePackage(@TempDir Path dir){
    var project= dir.resolve("p");
    at(project.resolve("p.fearless"), "", old);
    at(project.resolve("a").resolve("_app").resolve("sub").resolve("_rank_app.fear"), "use base.Str as Str;\n", old);
    at(project.resolve("b").resolve("_app").resolve("x.fear"), "", old);
    assertEquals("use base.Str as Str;\n", Cached.rankText(project, "app"));
    assertEquals("", Cached.rankText(project, "lib"));
  }
  @Test void theMapDirectivesOfAPackageAreThoseOfTheRankFileOfHighestRank(@TempDir Path dir){
    var project= project(dir);
    assertEquals(Map.of("y", "lib"), Cached.packages(project, "app"));
    assertEquals(Map.of(), Cached.packages(project, "lib"));
  }
}

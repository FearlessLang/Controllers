package controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import coordinator.Coordinator;
import coordinator.MainsInfo;
import tools.Fs;

/// A compilation cache in the layout of CoordinatorDesign, written by the tests, and the check of it the tests use in place of Coordinator.check:
/// compiled when every <pkg>.jar is not older than every file of a package of the project and no _unfinished.txt is there.
final class FakeCache{
  static String manifest(String mains){ return "{\n  \"mains\": "+mains.strip()+",\n  \"attenuators\": [],\n  \"dataRead\": [],\n  \"dataEdit\": []\n}\n"; }
  static String json(String manifest){ return "{\"inputsSnapshot\":\"\",\n\"manifest\":"+q(manifest)+",\n\"types\":[]}\n"; }
  private static String q(String s){ return "\""+s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")+"\""; }
  static void write(Path project, String pkg, long stamp){ write(project, pkg, stamp, "{}"); }
  static void write(Path project, String pkg, long stamp, String mains){
    var out= project.resolve(Coordinator.outDir);
    at(out.resolve(pkg+".json"), json(manifest(mains)), stamp);
    at(out.resolve(pkg+".jar"), "", stamp);
  }
  static void at(Path file, String content, long stamp){
    Fs.writeUtf8(file, content);
    Fs.ofV(() -> Files.setLastModifiedTime(file, FileTime.fromMillis(stamp)));
  }
  static long after(Path project){ return Fs.walk(project, s -> s.filter(Files::isRegularFile).mapToLong(Fs::lastModified).max().orElse(0))+1; }
  static Coordinator.State check(Path project, Function<Path,Optional<MainsInfo>> read){
    var out= project.resolve(Coordinator.outDir);
    if (!Fs.isDirectory(out) || Fs.exists(out.resolve("_unfinished.txt"))){ return new Coordinator.State.OutOfDate(); }
    var jars= Fs.walk(out, s -> s.filter(p -> p.getFileName().toString().endsWith(".jar")).mapToLong(Fs::lastModified).toArray());
    if (jars.length == 0){ return new Coordinator.State.OutOfDate(); }
    var oldest= java.util.Arrays.stream(jars).min().getAsLong();
    var newest= Fs.walk(project, s -> s.filter(Files::isRegularFile).filter(p -> project.relativize(p).toString().startsWith("_")).mapToLong(Fs::lastModified).max().orElse(0));
    if (newest > oldest){ return new Coordinator.State.OutOfDate(); }
    var manifest= read.apply(project).orElseGet(() -> new MainsInfo(Coordinator.cachedMains(project)));
    return new Coordinator.State.Compiled(manifest, List.of("base"));
  }
}

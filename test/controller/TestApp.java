package controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import tools.Fs;
import utils.Bug;
import utils.Join;
import utils.Push;

/// An application of a test, holding the launcher of jpackage, the runtime of the tests and a main on the class path of the tests.
/// The JVM takes an open-file event only when the main bundle of the process declares document types, and macOS finds the main bundle from the path of the executable:
/// a script running java has the folder of java as its main bundle, so the executable is the launcher of jpackage, as in a deployed manager.
/// The signature of that launcher does not match the Info.plist written here, and macOS refuses to start such an application: the application is signed ad hoc, as jpackage does.
public final class TestApp{
  public static Path create(Path app, String main, String... javaOptions){
    var name= app.getFileName().toString().replaceFirst("\\.app$", "");
    var contents= app.resolve("Contents");
    Fs.writeUtf8(contents.resolve("Info.plist"), """
<?xml version="1.0" encoding="UTF-8"?>
<plist version="1.0"><dict>
<key>CFBundleExecutable</key><string>%s</string>
<key>CFBundleIdentifier</key><string>fearless-test-%s</string>
<key>CFBundlePackageType</key><string>APPL</string>
<key>CFBundleDocumentTypes</key><array><dict><key>CFBundleTypeRole</key><string>Viewer</string><key>LSHandlerRank</key><string>None</string>
<key>LSItemContentTypes</key><array><string>public.item</string></array></dict></array>
</dict></plist>
""".formatted(name, app.getParent().getFileName()));
    var launcher= contents.resolve("MacOS").resolve(name);
    Fs.ensureDir(launcher.getParent());
    var jrt= FileSystems.getFileSystem(URI.create("jrt:/"));
    Fs.ofV(() -> Files.copy(jrt.getPath("/modules/jdk.jpackage/jdk/jpackage/internal/resources/jpackageapplauncher"), launcher));
    assertTrue(launcher.toFile().setExecutable(true));
    Fs.ofV(() -> Files.createSymbolicLink(contents.resolve("runtime"), Path.of(System.getProperty("java.home")).getParent().getParent()));
    var classPath= Stream.of(System.getProperty("java.class.path").split(File.pathSeparator)).map(p -> "app.classpath="+p).toList();
    var options= Stream.of(javaOptions).map(o -> "java-options="+o).toList();
    var lines= Push.of(List.of(Push.of("[Application]", classPath), List.of("app.mainclass="+main, "", "[JavaOptions]"), options));
    Fs.writeUtf8(contents.resolve("app").resolve(name+".cfg"), Join.of(lines, "", "\n", "\n"));
    ended(run("codesign", "--force", "--deep", "--sign", "-", app.toString()));
    return app;
  }
  public static Process open(String... args){ return run(Push.of("open", List.of(args)).toArray(String[]::new)); }
  private static Process run(String... cmd){ return Fs.of(new ProcessBuilder(cmd).redirectErrorStream(true)::start); }
  public static void ended(Process p){
    try{ assertTrue(p.waitFor(1, TimeUnit.MINUTES)); }
    catch (InterruptedException e){ throw Bug.of(e); }
    assertEquals(0, p.exitValue(), new String(Fs.of(() -> p.getInputStream().readAllBytes())));
  }
  public static void until(Path file){
    var end= System.nanoTime() + Duration.ofMinutes(1).toNanos();
    while (!Fs.exists(file) && System.nanoTime() < end){
      try{ Thread.sleep(20); }
      catch (InterruptedException e){ throw Bug.of(e); }
    }
    assertTrue(Fs.exists(file), file.toString());
  }
}

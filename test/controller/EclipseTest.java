package controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tools.Fs;

final class EclipseTest{
  private static Path eclipseAt(Path folder){
    Fs.writeUtf8(folder.resolve(".eclipseproduct"),"name=Eclipse Platform\n");
    Fs.writeUtf8(folder.resolve("eclipse.exe"),"");
    Fs.writeUtf8(folder.resolve("plugins").resolve("a.jar"),"");
    return folder;
  }
  @Test void anEclipseIsFoundFromItsFolderAFileInItAFileInAFolderOfItOrTheFolderHoldingOnlyIt(@TempDir Path dir){
    var eclipse= eclipseAt(dir.resolve("unzipped").resolve("eclipse"));
    assertEquals(Optional.of(eclipse),Eclipse.find(eclipse));
    assertEquals(Optional.of(eclipse),Eclipse.find(eclipse.resolve("eclipse.exe")));
    assertEquals(Optional.of(eclipse),Eclipse.find(eclipse.resolve("plugins").resolve("a.jar")));
    assertEquals(Optional.of(eclipse),Eclipse.find(eclipse.getParent()));
  }
  @Test void nothingElseLeadsToAnEclipse(@TempDir Path dir){
    var eclipse= eclipseAt(dir.resolve("unzipped").resolve("eclipse"));
    Fs.writeUtf8(dir.resolve("unzipped").resolve("readme.txt"),"");
    assertEquals(Optional.empty(),Eclipse.find(eclipse.getParent()));
    assertEquals(Optional.empty(),Eclipse.find(dir));
    Fs.writeUtf8(eclipse.resolve("plugins").resolve("deep").resolve("b.jar"),"");
    assertEquals(Optional.empty(),Eclipse.find(eclipse.resolve("plugins").resolve("deep").resolve("b.jar")));
  }
  @Test void connectingPutsThePluginAndManagerInfoInDropinsReplacingAnOlderPlugin(@TempDir Path dir){
    var plugin= dir.resolve("app").resolve("eclipsePlugin");
    Fs.writeUtf8(plugin.resolve("new.jar"),"new");
    var eclipse= eclipseAt(dir.resolve("eclipse"));
    var fearless= eclipse.resolve("dropins").resolve("fearless");
    Fs.writeUtf8(fearless.resolve("plugins").resolve("old.jar"),"old");
    Eclipse.connect(eclipse,plugin,dir.resolve("program"),dir.resolve("program").resolve("launcher"),dir.resolve("manager"),dir.resolve("baseCache"));
    assertEquals(List.of(fearless.resolve("plugins").resolve("new.jar")),Fs.of(() -> { try (var s= Files.list(fearless.resolve("plugins"))){ return s.toList(); } }));
    assertEquals("new",Fs.readUtf8(fearless.resolve("plugins").resolve("new.jar")));
    var at= dir.toString().replace("\\","\\\\");
    var sep= dir.getFileSystem().getSeparator().replace("\\","\\\\");
    assertEquals("""
      {
        "program": "Str:%1$s%2$sprogram",
        "launcher": "Str:%1$s%2$sprogram%2$slauncher",
        "manager": "Str:%1$s%2$smanager",
        "baseCache": "Str:%1$s%2$sbaseCache"
      }
      """.stripIndent().formatted(at,sep),Fs.readUtf8(fearless.resolve("manager.info")));
    assertEquals(List.of("manager.info","plugins"),Fs.of(() -> { try (var s= Files.list(fearless)){ return s.map(p -> p.getFileName().toString()).sorted().toList(); } }));
  }
}

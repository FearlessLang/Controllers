package controller;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import fileSupport.Info;
import fileSupport.Info.Obj;
import fileSupport.Info.Obj.Field;
import fileSupport.Info.Str;
import tools.Fs;

/// The Eclipse folder a chosen file or folder leads to, and connecting it: the plugin
/// (fearlessPluginProject) in its dropins folder, with manager.info next to the plugins folder.
public final class Eclipse{
  private Eclipse(){}
  static Optional<Path> find(Path chosen){
    var parent= Optional.ofNullable(chosen.getParent());
    var only= Files.isDirectory(chosen) ? Optional.of(Names.list(chosen)).filter(l->l.size() == 1).map(List::getFirst) : Optional.<Path>empty();
    return Stream.of(Optional.of(chosen),parent,parent.map(Path::getParent),only).flatMap(Optional::stream)
      .filter(d->Files.isRegularFile(d.resolve(".eclipseproduct"))).findFirst();
  }
  public static void connect(Path eclipse, Path plugin, Path program, Path launcher, Path managerDir, Path baseCache){
    var fearless= eclipse.resolve("dropins").resolve("fearless");
    var plugins= fearless.resolve("plugins");
    var names= Names.list(plugin).stream().map(Path::getFileName).toList();
    names.forEach(n->replace(plugins.resolve(n),Fs.of(()->Files.readAllBytes(plugin.resolve(n)))));
    Names.list(plugins).stream().filter(p->!names.contains(p.getFileName())).forEach(Fs::rmTree);
    var info= List.of(field("program",program),field("launcher",launcher),field("manager",managerDir),field("baseCache",baseCache));
    replace(fearless.resolve("manager.info"),Info.print(new Obj(info,Info.noSpan)).getBytes(UTF_8));
  }
  private static Field field(String key, Path value){ return new Field(key,Info.noSpan,new Str(TaggedText.of(value.toString()),Info.noSpan)); }
  private static void replace(Path file, byte[] content){
    var tmp= file.resolveSibling(file.getFileName()+".tmp");
    Fs.ensureDir(file.getParent());
    Fs.ofV(()->{ Files.write(tmp,content); Files.move(tmp,file,ATOMIC_MOVE); });
  }
}

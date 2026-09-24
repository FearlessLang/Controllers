package benchmarks;

import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import coordinator.CapabilityEnvironment;
import coordinator.Coordinator;
import coordinator.OutputOracle;
import core.E.Literal;
import core.OtherPackages;
import naiveBackend.BackendTools;
import realSourceOracle.SourceOracleWithAutoload;
import resources.ResolveResource;
import tools.Fs;
import tools.JavacTool;
import tools.SourceOracle;
import utils.Bug;

/// A runtime is one StandardLibrary checkout (its `base` and `rt`): the benchmark project is compiled
/// once against each, under Controllers/.out/benchmarks/<name>, and a loaded runtime runs the mains of
/// that compiled project in this JVM, in a class loader of its own.
public final class Runtimes{
  public static final String stlibsKey= "fearless.benchmarks.stlibs";
  static final Path out= ResolveResource.controllerSrc.getParent().resolve(".out","benchmarks");
  static final Path project= ResolveResource.controllerSrc.getParent().resolve("benchmarks","fearless");
  private Runtimes(){}
  public static List<String> buildAll(){
    var spec= System.getProperty(stlibsKey, "wc="+ResolveResource.stLibPath.getParent());
    var names= new ArrayList<String>();
    for (var entry: spec.split(";")){
      int eq= entry.indexOf('=');
      var name= entry.substring(0, eq);
      build(name, Path.of(entry.substring(eq + 1)));
      names.add(name);
    }
    return names;
  }
  static void build(String name, Path stlib){
    System.setProperty(JavacTool.appDirKey, ResolveResource.stLibPath.getParent().resolve("fearlessArtefact","fearless","app").toString());
    var dir= out.resolve(name);
    Fs.rmTree(dir);
    var basePath= stlib.resolve("base");
    var rtPath= stlib.resolve("rt");
    var baseCache= buildBase(dir, basePath, rtPath);
    var proj= dir.resolve("project");
    Fs.copyTree(project, proj);
    var c= new Coordinator(){
      @Override public Path modsPath(){ return ResolveResource.coordinatorJars; }
      @Override public Optional<Path> baseCachePath(){ return Optional.of(baseCache); }
      @Override public BackendTools backendTools(String pkgName, SourceOracle oracle, OtherPackages other, List<Literal> core, CapabilityEnvironment capabilities){
        return BackendTools.of(pkgName, oracle, other, core, proj.resolve(Coordinator.outDir), baseCachePath(), rtPath, capabilities);
      }
    };
    c.compile(proj, c.sourceOracle(basePath));
  }
  private static Path buildBase(Path dir, Path basePath, Path rtPath){
    var scratch= dir.resolve("_baseScratch");
    Fs.ensureDir(scratch);
    var baseCache= dir.resolve("baseCache");
    var c= new Coordinator(){
      @Override public Path modsPath(){ return ResolveResource.coordinatorJars; }
      @Override public BackendTools backendTools(String pkgName, SourceOracle oracle, OtherPackages other, List<Literal> core, CapabilityEnvironment capabilities){
        return BackendTools.of(pkgName, oracle, other, core, scratch, baseCachePath(), scratch.resolve("_discardedTest","_discardedTest.fear"), rtPath, capabilities);
      }
    };
    OutputOracle o= ()->scratch;
    var other= OtherPackages.empty();
    var oracle= c.sourceOracle(basePath);
    var rich= SourceOracleWithAutoload.ofBase(oracle);
    var core= c.frontend("base", rich.sources(oracle.allFiles()), rich.oracle(), other, Map.of());
    c.backend("base", core, rich.oracle(), other, new CapabilityEnvironment(rich.autoloadedAssets()));
    o.commitPkgApi("base", core, -1);
    Fs.copyFresh(scratch.resolve("base.json"), baseCache.resolve("base.json"));
    Fs.copyFresh(scratch.resolve("gen_java").resolve("base.jar"), baseCache.resolve("base.jar"));
    Fs.rmTree(scratch);
    return baseCache;
  }
  public static Loaded load(String name){
    var dir= out.resolve(name);
    var jars= List.of(dir.resolve("project", Coordinator.outDir, "gen_java", "bench.jar"), dir.resolve("baseCache","base.jar"));
    var urls= jars.stream().map(p->Fs.of(()->p.toUri().toURL())).toArray(URL[]::new);
    return new Loaded(new URLClassLoader(urls, ClassLoader.getPlatformClassLoader()));
  }
  interface Reflect<T>{ T get() throws ReflectiveOperationException; }
  static <T> T reflect(Reflect<T> r){
    try{ return r.get(); }
    catch(InvocationTargetException e){ throw sneaky(e.getCause()); }
    catch(ReflectiveOperationException e){ throw Bug.of(e.toString()); }
  }
  @SuppressWarnings("unchecked") static <E extends Throwable> RuntimeException sneaky(Throwable t) throws E{ throw (E)t; }
  public static final class Loaded{
    final ClassLoader cl;
    final Object system;
    Loaded(ClassLoader cl){
      this.cl= cl;
      this.system= reflect(()->cl.loadClass("_base._System$2o$0").getConstructor().newInstance());
    }
    public void run(String main){
      reflect(()->{
        var c= cl.loadClass("_bench."+main+"$"+tag(main)+"$0");
        var instance= c.getField("instance").get(null);
        return c.getMethod("imm$main$1", Object.class).invoke(instance, system);
      });
    }
  }
  static String tag(String s){
    var bits= new StringBuilder("1");
    for (char c: s.toCharArray()){
      if ('A' <= c && c <= 'Z'){ bits.append('1'); }
      if ('a' <= c && c <= 'z'){ bits.append('0'); }
    }
    return new BigInteger(bits.toString(), 2).toString(36);
  }
  public static void main(String[] args){
    var names= buildAll();
    if (args.length == 0){ return; }
    var loaded= load(names.getFirst());
    for (var main: args){
      long t0= System.nanoTime();
      loaded.run(main);
      System.out.println(main+" "+(System.nanoTime() - t0) / 1_000_000+" ms");
    }
    if (names.isEmpty()){ throw Bug.unreachable(); }
  }
}

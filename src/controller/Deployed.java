package controller;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import coordinator.CapabilityEnvironment;
import coordinator.Coordinator;
import core.E.Literal;
import core.OtherPackages;
import naiveBackend.BackendTools;
import tools.ChildJvm;
import tools.JavacTool;
import tools.SourceOracle;
import userMessages.Violation;

/// The compiler and the mains of a project, as the deployed manager runs them: the
/// compiler in a child JVM (ChildMain), each main in a child JVM, and the mains read in process.
public final class Deployed implements Manager.Tools{
  public static Path stdLib(String name){ return appDir().resolve("stdLib").resolve(name); }
  private static Path appDir(){ return JavacTool.reqAppDir(Violation::mustUseLauncher); }
  static Coordinator coordinator(Path project){
    return new Coordinator(){
      @Override public Optional<Path> baseCachePath(){ return Optional.of(stdLib("baseCache")); }
      @Override public BackendTools backendTools(String pkgName, SourceOracle oracle, OtherPackages other, List<Literal> core, CapabilityEnvironment capabilities){
        return BackendTools.of(pkgName,oracle,other,core,project.resolve(Coordinator.outDir),baseCachePath(),stdLib("rt"),capabilities);
      }
    };
  }
  @Override public ChildJvm compile(Path folder, Consumer<String> out){
    return ChildJvm.start(List.of(
      "-Djava.awt.headless=true",
      "-D"+JavacTool.appDirKey+"="+appDir(),
      "-D"+JavacTool.launcherKey+"="+JavacTool.consoleKey,
      "-D"+JavacTool.versionIdKey+"="+JavacTool.reqVersionId(Violation::mustUseLauncher),
      "--enable-native-access=Commons,Coordinator",
      "-p", appDir().resolve(JavacTool.deployedModsDirName).toString(),
      "-m", "Controller/controller.ChildMain",
      folder.toString()),out);
  }
  @Override public ChildJvm run(Path folder, String main, Consumer<String> out){
    return Coordinator.startMain(folder,stdLib("base"),main,coordinator(folder).sharedClasspath(),out);
  }
  @Override public Optional<Map<String,String>> mains(Path folder){
    var c= coordinator(folder);
    return c.mains(folder,c.sourceOracle(stdLib("base")));
  }
}

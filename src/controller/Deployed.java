package controller;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import coordinator.CapabilityEnvironment;
import coordinator.Coordinator;
import coordinator.MainsInfo;
import core.E.Literal;
import core.OtherPackages;
import fileAssociations.Icon;
import fileAssociations.WindowIcon;
import naiveBackend.BackendTools;
import tools.ChildJvm;
import tools.Fs;
import tools.JavacTool;
import tools.PathClassLoader;
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
        return BackendTools.of(pkgName, oracle, other, core, project.resolve(Coordinator.outDir), baseCachePath(), stdLib("rt"), capabilities);
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
      PathClassLoader.argOf(folder)), out);
  }
  @Override public ChildJvm run(Path folder, String main, Optional<Path> input, Registry.Grants grants, List<String> attenuators, Consumer<String> out){
    return Coordinator.startMain(folder, stdLib("base"), main, input, grants.reads(), grants.edits(), attenuators, Association.desk(folder, main), coordinator(folder).sharedClasspath(), out);
  }
  @Override public Path stdLibBase(){ return stdLib("base"); }
  @Override public Path baseCache(){ return stdLib("baseCache"); }
  @Override public void associate(List<Icon> captured, List<Project.Claimant> preloads, Function<String,String> claimedBy){
    if (Fs.isMac()){ return; }
    var launcher= Association.launcher();
    var windows= preloads.stream().map(Deployed::window).toList();
    Association.reconcile(launcher, Stream.concat(Stream.of(new Icon(".fearless", launcher, Association.iconFile())), captured.stream()).toList(), windows, claimedBy);
  }
  private static WindowIcon window(Project.Claimant c){
    return new WindowIcon(Association.deskId(c.folder(), c.main())+"-"+c.claim().icon(), c.main().substring(c.main().lastIndexOf('.')+1), c.folder(), c.icon(".png"));
  }
  @Override public void connect(Path eclipse, Path managerDir){
    Eclipse.connect(eclipse, appDir().resolve("eclipsePlugin"), Main.programDir(), Association.launcher(), managerDir, stdLib("baseCache"));
  }
  @Override public Optional<MainsInfo> mains(Path folder){
    return coordinator(folder).mains(folder);
  }
}

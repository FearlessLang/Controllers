package controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tempDirs.RealTempDir;

import coordinator.CapabilityEnvironment;
import coordinator.Coordinator;
import core.E.Literal;
import core.OtherPackages;
import fileAssociations.Icon;
import mainCoordinator.Main.Refusal;
import naiveBackend.BackendTools;
import resources.ResolveResource;
import tools.ChildJvm;
import tools.Fs;
import tools.SourceOracle;
import utils.Bug;
import utils.Err;
import utils.OneOr;

final class RealManagerTest{
  @RealTempDir static Path shared;
  public static void main(String[] args) throws IOException{
    var msgDir= Path.of(args[0]);
    var name= String.format(Locale.ROOT, "%020d-%s", System.currentTimeMillis(), UUID.randomUUID());
    Fs.writeUtf8(msgDir.resolve(name+".tmp"), OneOr.of("one message", Main.messages(args[1]).stream()));
    Fs.move(msgDir.resolve(name+".tmp"), msgDir.resolve(name+".msg"));
  }
  static Path baseCache(){ return shared.resolve("baseCache"); }
  @BeforeAll static void buildBase(){
    var scratch= shared.resolve("baseScratch");
    Fs.ensureDir(scratch);
    coordinator(scratch, Optional.empty()).buildBaseCache(ResolveResource.stLibPath, scratch, baseCache());
  }
  static Coordinator coordinator(Path out, Optional<Path> base){
    return new Coordinator(){
      @Override public Path modsPath(){ return ResolveResource.coordinatorJars; }
      @Override public Optional<Path> baseCachePath(){ return base; }
      @Override public Path stdLibBase(){ return ResolveResource.stLibPath; }
      @Override public BackendTools backendTools(String pkgName, SourceOracle oracle, OtherPackages other, List<Literal> core, CapabilityEnvironment capabilities){
        return BackendTools.of(pkgName, oracle, other, core, out, baseCachePath(), false, ResolveResource.stLibRTPath, capabilities);
      }
    };
  }
  static Coordinator coordinator(Path project){ return coordinator(project.resolve(Coordinator.outDir), Optional.of(baseCache())); }
  record Real(Path launcher) implements Manager.Tools{
    @Override public ChildJvm compile(Path folder, String alias, Consumer<String> out){ throw Bug.unreachable(); }
    @Override public ChildJvm run(Path folder, String main, Optional<Path> input, Registry.Grants grants, List<String> attenuators, Consumer<String> out){
      return Coordinator.startMain(folder, ResolveResource.stLibPath, main, input, grants.mounts(), grants.locks(), attenuators, List.of("-DfearlessManager.launcher="+launcher), coordinator(folder).sharedClasspath(), out);
    }
    @Override public Coordinator.State check(Path folder){ return coordinator(folder).check(folder); }
    @Override public Path stdLibBase(){ return ResolveResource.stLibPath; }
    @Override public Path baseCache(){ return RealManagerTest.baseCache(); }
    @Override public Optional<Refusal> associate(List<Icon> captured, List<String> taken, List<Project.Claimant> preloads){ return Optional.empty(); }
    @Override public void connect(Path eclipse, Path managerDir){ throw Bug.unreachable(); }
  }
  private final List<String> exits= Collections.synchronizedList(new ArrayList<>());
  private final List<Throwable> failures= Collections.synchronizedList(new ArrayList<>());
  private final ManagerTest.View view= new ManagerTest.View();
  private static Path launcher(Path dir, Path msgDir){
    var java= Path.of(System.getProperty("java.home"), "bin", "java");
    var cp= System.getProperty("java.class.path");
    var res= dir.resolve(Fs.isWindows() ? "launcher.cmd" : "launcher");
    Fs.writeUtf8(res, Fs.isWindows()
      ? "@\""+java+"\" -cp \""+cp+"\" controller.RealManagerTest \""+msgDir+"\" %*\n"
      : "#!/bin/sh\nexec '"+java+"' -cp '"+cp+"' controller.RealManagerTest '"+msgDir+"' \"$@\"\n");
    assertTrue(res.toFile().setExecutable(true));
    return res;
  }
  private Manager manager(Path dir){
    var managerDir= dir.resolve("manager");
    var msgDir= managerDir.resolve("messages");
    Fs.ensureDir(msgDir);
    return new Manager(managerDir, new Real(launcher(dir, msgDir)), view, new Manager.Exit(){
      @Override public void quit(){ exits.add("quit"); }
      @Override public void reset(){ exits.add("reset"); }
      @Override public void fail(Throwable problem){ failures.add(problem); }
    });
  }
  private static void message(Manager m, String... lines){
    m.message(String.join("\n", lines));
    m.settle();
  }
  private static void until(Manager m, Duration limit, BooleanSupplier done){
    var end= System.nanoTime() + limit.toNanos();
    while (!done.getAsBoolean() && System.nanoTime()<end){
      m.drain();
      m.settle();
      try{ Thread.sleep(20); }
      catch (InterruptedException e){ throw Bug.of(e); }
    }
  }
  private static Path compiled(Manager m, Path dir, String name){
    var project= dir.resolve(name);
    Fs.copyFresh(ResolveResource.integrationTests.resolve("testRealManager"), project);
    message(m, "Register", TaggedText.of(project.toString()));
    message(m, "Kind", name, "code");
    coordinator(project).compile(project, ResolveResource.stLibPath, name, cs -> cs.stream().findFirst()).ifPresent(e -> { throw e; });
    Coordinator.finish(project);
    return project;
  }
  private static boolean idle(Manager m){ return m.state().projects().stream().noneMatch(Project::busy); }
  private void endsTheManagerAndNeverReturns(Path dir, String main, String exit){
    var m= manager(dir);
    var project= compiled(m, dir, "realmgr");
    message(m, "SelectMains", "realmgr", main);
    message(m, "Run", "realmgr");
    until(m, Duration.ofMinutes(1), () -> !exits.isEmpty());
    until(m, Duration.ofSeconds(10), () -> idle(m));
    m.stop();
    assertEquals(List.of(), failures);
    assertEquals(List.of(exit), exits);
    assertFalse(view.out(project).contains(exit+" returned"), view.out(project));
  }
  @Test void aMainCallingQuitNeverReturns(@RealTempDir Path dir){ endsTheManagerAndNeverReturns(dir, "realmgr.Quit", "quit"); }
  @Test void aMainCallingResetNeverReturns(@RealTempDir Path dir){ endsTheManagerAndNeverReturns(dir, "realmgr.Reset", "reset"); }
  @Test void aRegisterOfTheRootOfADriveAfterAnUnpairedSurrogateIsADeterministicError(@RealTempDir Path dir){
    var m= manager(dir);
    var project= compiled(m, dir, "realmgr");
    message(m, "SelectMains", "realmgr", "realmgr.RegisterTests");
    message(m, "Run", "realmgr");
    until(m, Duration.ofSeconds(30), () -> idle(m));
    m.stop();
    assertEquals(List.of(), failures);
    assertEquals(List.of(), view.refused);
    Err.strCmp("--- running realmgr.RegisterTests ---\n--- realmgr.RegisterTests exited with 0 after [###]s ---\n", view.out(project));
  }
}

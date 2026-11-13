package controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import tempDirs.RealTempDir;
import tools.Fs;
import tools.JavacTool;

/// Association is a thin adapter over Commons' fileAssociations; these pin the
/// adapter's identity/family rules.
final class AssociationTest{
  private static final String linuxLauncher= "/home/me/fearlessManaged0_001/bin/fearlessManaged0_001";
  private static final Path winLauncher= Path.of("C:\\fearlessManaged0_001\\fearlessManaged0_001.exe");
  private static final String macLauncher= "/Applications/fearlessManaged0_001.app/Contents/MacOS/fearlessManaged0_001";
  private static final String identity= "fearlessManaged0_001";
  @Test void theLauncherNamesTheProgramAndTheManagerIsNotThePortable(){
    assertEquals(identity, mainCoordinator.Main.identity(Path.of(linuxLauncher)));
    assertEquals(identity, mainCoordinator.Main.identity(Path.of("fearlessManaged0_001.exe")));
    assertEquals(identity, mainCoordinator.Main.identity(Path.of(macLauncher)));
    assertNotEquals(mainCoordinator.Main.identity(Path.of(linuxLauncher)), mainCoordinator.Main.identity(Path.of("/home/me/fearlessBin0_001/bin/fearlessBin0_001")));
  }
  @Test void theTwoLaunchersOfOneFearlessAreTwoIdentitiesWithOneManagerFolder(){
    assertEquals(JavacTool.managerFolderOf(Path.of(linuxLauncher)), JavacTool.managerFolderOf(Path.of(linuxLauncher+"w")));
    assertEquals(identity+"w", mainCoordinator.Main.identity(Path.of(linuxLauncher+"w")));
    assertEquals(Path.of("/Applications/fearless0_001"), JavacTool.managerFolderOf(Path.of(macLauncher)).get());
    assertEquals(JavacTool.managerFolderOf(Path.of(macLauncher)), JavacTool.managerFolderOf(Path.of(macLauncher+"w")));
    assertEquals(identity+"w", mainCoordinator.Main.identity(Path.of("fearlessManaged0_001w.exe")));
    assertEquals(JavacTool.managerFolderOf(Path.of("fearlessManaged0_001/fearlessManaged0_001.exe")), JavacTool.managerFolderOf(Path.of("fearlessManaged0_001/fearlessManaged0_001w.exe")));
    assertTrue(JavacTool.managerFolderOf(Path.of("fearlessManaged0_001/fearlessManaged0_001.exe")).isPresent());
  }
  @Test void everyLauncherOfTheFamilyNamesItInItsOwnFile(){
    assertTrue(mainCoordinator.Main.belongsToFamily.test("fearlessManaged0_001"));
    assertTrue(mainCoordinator.Main.belongsToFamily.test("FearlessApp_001"));
    assertFalse(mainCoordinator.Main.belongsToFamily.test("SomeOtherEditor"));
  }
  @Test void aReplacedManagerLosesItsManagerFolderAndTheOtherLauncherOfTheSameFolderKeepsIt(@RealTempDir Path dir){
    var old= dir.resolve("a").resolve(identity).resolve("bin").resolve(identity);
    var moved= dir.resolve("b").resolve(identity).resolve("bin").resolve(identity);
    var oldFolder= dir.resolve("a").resolve("fearless0_001");
    var folder= dir.resolve("b").resolve("fearless0_001");
    Fs.writeUtf8(oldFolder.resolve("projects.info"), "");
    Fs.writeUtf8(folder.resolve("projects.info"), "");
    mainCoordinator.Main.replaced(moved.resolveSibling(identity+"w"), moved);
    mainCoordinator.Main.replaced(moved, dir.resolve("a").resolve("fearlessBin0_001").resolve("bin").resolve("fearlessBin0_001"));
    mainCoordinator.Main.replaced(moved, dir.resolve("c").resolve(identity).resolve("bin").resolve(identity));
    assertTrue(Files.exists(oldFolder.resolve("projects.info")));
    mainCoordinator.Main.replaced(moved, old);
    assertFalse(Files.exists(oldFolder));
    assertEquals(List.of("fearless0_001"), Names.list(dir.resolve("b")).stream().map(p -> p.getFileName().toString()).toList());
    assertEquals(List.of(), Names.list(dir.resolve("a")));
  }
  public static void main(String[] args) throws IOException{
    try (var ch= FileChannel.open(Path.of(args[0]), StandardOpenOption.CREATE, StandardOpenOption.WRITE)){
      ch.lock();
      System.out.print("locked\n");
      System.out.flush();
      while (System.in.read() >= 0){}
    }
  }
  private static Process locking(Path lock){
    var java= Path.of(System.getProperty("java.home"), "bin", "java");
    var p= Fs.of(new ProcessBuilder(java.toString(), "-cp", System.getProperty("java.class.path"), "controller.AssociationTest", lock.toString()).redirectErrorStream(true)::start);
    assertEquals("locked", Fs.of(() -> new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8)).readLine()));
    return p;
  }
  private static void quits(Process manager, List<Path> sent, Path command){
    sent.add(command);
    Fs.ofV(() -> manager.getOutputStream().close());
  }
  private static Path replacedFolder(Path dir, Consumer<Path> quit){
    var old= dir.resolve("a").resolve(identity).resolve("bin").resolve(identity);
    var oldFolder= dir.resolve("a").resolve("fearless0_001");
    Fs.writeUtf8(oldFolder.resolve("messages").resolve("m.msg"), "Show");
    var before= mainCoordinator.Main.quit;
    mainCoordinator.Main.quit = quit;
    try{ mainCoordinator.Main.replaced(dir.resolve("b").resolve(identity).resolve("bin").resolve(identity), old); }
    finally{ mainCoordinator.Main.quit = before; }
    return oldFolder;
  }
  @Test void aReplacedRunningManagerIsSentQuitAndItsManagerFolderIsDeletedOnceItHasQuit(@RealTempDir Path dir){
    Fs.ensureDir(dir.resolve("a").resolve("fearless0_001"));
    var manager= locking(dir.resolve("a").resolve("fearless0_001").resolve("instance.lock"));
    var sent= new ArrayList<Path>();
    var oldFolder= replacedFolder(dir, c -> quits(manager, sent, c));
    assertEquals(List.of(dir.resolve("a").resolve(identity).resolve("bin").resolve(identity)), sent);
    assertEquals(0, manager.onExit().join().exitValue());
    assertFalse(Files.exists(oldFolder));
    assertEquals(List.of(), Names.list(dir.resolve("a")).stream().filter(p -> !p.getFileName().toString().equals(identity)).toList());
  }
  @Test void aReplacedManagerThatDoesNotQuitLosesTheFilesOfItsManagerFolderAfterTheWait(@RealTempDir Path dir){
    Fs.ensureDir(dir.resolve("a").resolve("fearless0_001"));
    Fs.writeUtf8(dir.resolve("a").resolve("fearless0_001").resolve("projects.info"), "{}");
    var manager= locking(dir.resolve("a").resolve("fearless0_001").resolve("instance.lock"));
    var wait= mainCoordinator.Main.quitWaitMillis;
    mainCoordinator.Main.quitWaitMillis = 500;
    try{ replacedFolder(dir, _ -> {}); }
    finally{ mainCoordinator.Main.quitWaitMillis = wait; manager.destroy(); }
    assertFalse(Files.exists(dir.resolve("a").resolve("fearless0_001").resolve("projects.info")));
  }
  @Test void aManagerFolderWhoseInstanceLockIsOpenIsDeleted(@RealTempDir Path dir){
    var lock= dir.resolve("a").resolve("fearless0_001").resolve("instance.lock");
    Fs.writeUtf8(lock, "");
    try (var _= FileChannel.open(lock, StandardOpenOption.WRITE)){
      var oldFolder= replacedFolder(dir, c -> { throw new AssertionError(c); });
      assertFalse(Files.exists(oldFolder));
    }
    catch (IOException e){ throw new UncheckedIOException(e); }
  }
  @Test void theLauncherIsTheOneJpackageNames(){
    Errs.err("Error: Fearless has been started without using its launcher.[###]", Association::launcher);
    System.setProperty("jpackage.app-path", winLauncher.toString());
    try{ assertEquals(winLauncher, Association.launcher()); }
    finally{ System.clearProperty("jpackage.app-path"); }
  }
}
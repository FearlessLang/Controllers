package controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

import tempDirs.RealTempDir;
import tools.Fs;
import tools.JavacTool;
import utils.Range;

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
  public static void main(String[] args) throws IOException, InterruptedException{
    var lock= Path.of(args[0]);
    var stuck= args[1].equals("stuck");
    try (var ch= FileChannel.open(lock, StandardOpenOption.CREATE, StandardOpenOption.WRITE)){
      ch.lock();
      System.out.print("locked\n");
      System.out.flush();
      var quit= arrived(lock.resolveSibling("messages"));
      System.out.print(quit.replace('\n', ' ').replaceFirst("[0-9]+$", "N")+"\n");
      System.out.flush();
      try (var s= new Socket(InetAddress.getLoopbackAddress(), Integer.parseInt(quit.substring(quit.lastIndexOf(':') + 1))); var out= s.getOutputStream()){
        out.write((ProcessHandle.current().pid()+"\n").getBytes(StandardCharsets.UTF_8));
        for (int i : Range.of(0, stuck ? Integer.MAX_VALUE : 3)){
          out.write(("Ending the mains: "+(stuck ? 0 : i)+" of 3 ended.\n").getBytes(StandardCharsets.UTF_8));
          out.flush();
          Thread.sleep(stuck ? 2000 : 100);
        }
      }
    }
  }
  private static String arrived(Path messages) throws IOException, InterruptedException{
    while (true){
      try (var s= Files.list(messages)){
        var msg= s.filter(p -> p.toString().endsWith(".msg")).findFirst();
        if (msg.isPresent()){ return Files.readString(msg.get()); }
      }
      Thread.sleep(50);
    }
  }
  private static BufferedReader locking(Path folder, String mode){
    Fs.ensureDir(folder.resolve("messages"));
    var java= Path.of(System.getProperty("java.home"), "bin", "java");
    var p= Fs.of(new ProcessBuilder(java.toString(), "-cp", System.getProperty("java.class.path"), "controller.AssociationTest", folder.resolve("instance.lock").toString(), mode).redirectErrorStream(true)::start);
    var out= new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8));
    assertEquals("locked", Fs.of(out::readLine));
    return out;
  }
  private static void replaced(Path dir){
    mainCoordinator.Main.replaced(dir.resolve("b").resolve(identity).resolve("bin").resolve(identity), dir.resolve("a").resolve(identity).resolve("bin").resolve(identity));
  }
  @Test void aReplacedRunningManagerIsSentQuitWithAPortAndItsManagerFolderIsDeletedOnceItHasQuit(@RealTempDir Path dir){
    var oldFolder= dir.resolve("a").resolve("fearless0_001");
    var manager= locking(oldFolder, "ends");
    var start= System.nanoTime();
    replaced(dir);
    assertTrue(Duration.ofNanos(System.nanoTime() - start).toSeconds() < 10);
    assertEquals("Quit Port:N", Fs.of(manager::readLine));
    assertFalse(Files.exists(oldFolder));
    assertEquals(List.of(), Names.list(dir.resolve("a")));
  }
  @Test void aReplacedRunningManagerSendingTheSameProgressLineFor10SecondsIsEndedAndItsManagerFolderIsDeleted(@RealTempDir Path dir){
    var oldFolder= dir.resolve("a").resolve("fearless0_001");
    var manager= locking(oldFolder, "stuck");
    var start= System.nanoTime();
    replaced(dir);
    assertTrue(Duration.ofNanos(System.nanoTime() - start).toSeconds() >= 10);
    assertEquals("Quit Port:N", Fs.of(manager::readLine));
    assertFalse(Files.exists(oldFolder));
    assertEquals(List.of(), Names.list(dir.resolve("a")));
  }
  @Test void aManagerFolderWhoseInstanceLockIsOpenIsDeleted(@RealTempDir Path dir){
    var oldFolder= dir.resolve("a").resolve("fearless0_001");
    Fs.writeUtf8(oldFolder.resolve("messages").resolve("m.msg"), "Show");
    var lock= oldFolder.resolve("instance.lock");
    Fs.writeUtf8(lock, "");
    try (var _= FileChannel.open(lock, StandardOpenOption.WRITE)){
      replaced(dir);
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
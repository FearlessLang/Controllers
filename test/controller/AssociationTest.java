package controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import tools.JavacTool;

/// Association is a thin adapter over Commons' fileAssociations; these pin the
/// adapter's identity/family rules.
final class AssociationTest{
  private static final String linuxLauncher= "/home/me/fearlessManaged0_001/bin/fearlessManaged0_001";
  private static final Path winLauncher= Path.of("C:\\fearlessManaged0_001\\fearlessManaged0_001.exe");
  private static final String identity= "fearlessManaged0_001";
  @Test void theLauncherNamesTheProgramAndTheManagerIsNotThePortable(){
    assertEquals(identity, Association.identity(Path.of(linuxLauncher)));
    assertEquals(identity, Association.identity(Path.of("fearlessManaged0_001.exe")));
    assertNotEquals(Association.identity(Path.of(linuxLauncher)), Association.identity(Path.of("/home/me/fearlessBin0_001/bin/fearlessBin0_001")));
  }
  @Test void theTwoLaunchersOfOneFearlessAreOneIdentity(){
    assertEquals(JavacTool.managerFolderOf(Path.of(linuxLauncher)), JavacTool.managerFolderOf(Path.of(linuxLauncher+"w")));
    assertEquals(Association.identity(Path.of(linuxLauncher)), Association.identity(Path.of(linuxLauncher+"w")));
    assertEquals(Association.identity(Path.of("fearlessManaged0_001.exe")), Association.identity(Path.of("fearlessManaged0_001w.exe")));
  }
  @Test void everyLauncherOfTheFamilyNamesItInItsOwnFile(){
    assertTrue(Association.belongsToFamily.test("fearlessManaged0_001"));
    assertTrue(Association.belongsToFamily.test("FearlessApp_001"));
    assertFalse(Association.belongsToFamily.test("SomeOtherEditor"));
  }
  @Test void theLauncherIsTheOneJpackageNames(){
    Errs.err("Error: Fearless has been started without using its launcher.[###]", Association::launcher);
    System.setProperty("jpackage.app-path", winLauncher.toString());
    try{ assertEquals(winLauncher, Association.launcher()); }
    finally{ System.clearProperty("jpackage.app-path"); }
  }
}
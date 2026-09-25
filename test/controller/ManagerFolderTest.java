package controller;

import static controller.Errs.err;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tools.Fs;

final class ManagerFolderTest{
  private static Path folder(Path dir, String name){
    var res= dir.resolve(name);
    Fs.ensureDir(res);
    return res;
  }
  @Test void aStartedFolderIsTheProjectItself(@TempDir Path dir){
    var project= folder(dir,"myProject");
    assertEquals(project,Manager.projectFolder(project.toString(),folder(dir,"manager")));
  }
  @Test void aStartLeavesAnAbsolutePathResolvedInItsOwnWorkingFolder(){
    var here= Path.of("").toAbsolutePath();
    assertEquals("Str:"+here.resolve("myProject").resolve("hello.fearless"),Main.message(Path.of("myProject","sub","..","hello.fearless").toString()));
    assertEquals("Str:"+here,Main.message("."));
    assertEquals("",Main.message());
  }
  @Test void aStartedFileIsTheFolderAround(@TempDir Path dir){
    var project= folder(dir,"myProject");
    var file= project.resolve("hello.fearless");
    Fs.writeUtf8(file,"anything");
    assertEquals(project,Manager.projectFolder(file.toString(),folder(dir,"manager")));
  }
  @Test void aPathNamingNothingIsRefused(@TempDir Path dir){
    err("""
      Nothing exists at the given path.
      [###]myProject[###]
      Start Fearless on an existing project folder, or on a file inside one.
      """,()->Manager.projectFolder(dir.resolve("myProject").toString(),folder(dir,"manager")));
  }
  @Test void theManagerFolderIsNotAProject(@TempDir Path dir){
    var managerDir= folder(dir,"manager");
    err("""
      Fearless cannot keep track of this folder as a project.
      [###]
      The manager folder holds what Fearless remembers about your projects: it is
      never part of a project, and no project is inside it.
      """,()->Manager.projectFolder(managerDir.toString(),managerDir));
  }
  @Test void aFileInTheManagerFolderIsNotAProjectEither(@TempDir Path dir){
    var managerDir= folder(dir,"manager");
    var file= managerDir.resolve("example.fearless");
    Fs.writeUtf8(file,"anything");
    err("Fearless cannot keep track of this folder as a project.[###]",()->Manager.projectFolder(file.toString(),managerDir));
  }
  @Test void aFolderUnderAnyPathIsAProject(@TempDir Path dir){
    var project= folder(folder(dir,"caf\u00e9 \ud83d\ude00"),"myProject");
    assertEquals(project,Manager.projectFolder(project.toString(),folder(dir,"manager")));
  }
  @Test void aFolderInsideTheManagerFolderIsNotAProjectEither(@TempDir Path dir){
    var managerDir= folder(dir,"manager");
    err("Fearless cannot keep track of this folder as a project.[###]",()->Manager.projectFolder(folder(managerDir,"messages").toString(),managerDir));
  }
  @Test void aFolderHoldingTheManagerFolderIsNotAProjectEither(@TempDir Path dir){
    err("Fearless cannot keep track of this folder as a project.[###]",()->Manager.projectFolder(dir.toString(),folder(dir,"manager")));
  }
  @Test void theRootOfADriveIsNotAProject(@TempDir Path dir){
    err("""
      Fearless cannot keep track of the root of a drive or of the file system as a
      project.
      [###]""",()->Manager.projectFolder(dir.getRoot().toString(),folder(dir,"manager")));
  }
}

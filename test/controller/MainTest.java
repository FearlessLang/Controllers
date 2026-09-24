package controller;

import static controller.Errs.err;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tools.Fs;

final class MainTest{
  private static Path folder(Path dir, String name){
    var res= dir.resolve(name);
    Fs.ensureDir(res);
    return res;
  }
  @Test void aStartedFolderIsTheProjectItself(@TempDir Path dir){
    var project= folder(dir,"myProject");
    assertEquals(project,Main.projectFolder(project.toString(),folder(dir,"manager")));
  }
  @Test void aStartedFileIsTheFolderAround(@TempDir Path dir){
    var project= folder(dir,"myProject");
    var file= project.resolve("hello.fearless");
    Fs.writeUtf8(file,"anything");
    assertEquals(project,Main.projectFolder(file.toString(),folder(dir,"manager")));
  }
  @Test void aPathNamingNothingIsRefused(@TempDir Path dir){
    err("""
      Nothing exists at the given path.
      [###]myProject[###]
      Start Fearless on an existing project folder, or on a file inside one.
      """,()->Main.projectFolder(dir.resolve("myProject").toString(),folder(dir,"manager")));
  }
  @Test void theManagerFolderIsNotAProject(@TempDir Path dir){
    var managerDir= folder(dir,"manager");
    err("""
      Fearless cannot keep track of this folder as a project.
      [###]
      The manager folder holds what Fearless remembers about your projects: it is
      never a project itself.
      """,()->Main.projectFolder(managerDir.toString(),managerDir));
  }
  @Test void aFileInTheManagerFolderIsNotAProjectEither(@TempDir Path dir){
    var managerDir= folder(dir,"manager");
    var file= managerDir.resolve("example.fearless");
    Fs.writeUtf8(file,"anything");
    err("Fearless cannot keep track of this folder as a project.[###]",()->Main.projectFolder(file.toString(),managerDir));
  }
  @Test void aFolderInsideTheManagerFolderIsNotAProjectEither(@TempDir Path dir){
    var managerDir= folder(dir,"manager");
    err("Fearless cannot keep track of this folder as a project.[###]",()->Main.projectFolder(folder(managerDir,"messages").toString(),managerDir));
  }
}

package controller;

import static controller.Errs.same;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
  private static void refused(String expected, Runnable body){ same(expected, assertThrows(Refused.class, body::run).getMessage()); }
  @Test void aStartedFolderIsTheProjectItself(@TempDir Path dir){
    var project= folder(dir, "myProject");
    assertEquals(project, Manager.projectFolder(project, folder(dir, "manager")));
  }
  @Test void aStartLeavesTheMessageOfItsArguments(){
    var here= Path.of("").toAbsolutePath();
    assertEquals("Register\nStr:"+here.resolve("myProject").resolve("hello.fearless"), Main.message(Path.of("myProject", "sub", "..", "hello.fearless").toString()));
    assertEquals("OpenWithOrShortcut\nStr:"+here.resolve("notes.htm"), Main.message("notes.htm"));
    assertEquals("Register\nStr:"+here, Main.message("."));
    assertEquals("Show", Main.message());
    assertEquals("Quit", Main.message("FearlessCommandQuit"));
    assertEquals("Show\nhello_world", Main.message("FearlessCommandShow", "hello_world"));
    assertEquals("Run\nhello_world\nhello.Hello1", Main.message("FearlessCommandRun", "hello_world", "hello.Hello1"));
    assertEquals("Run\nhello_world\nhello.Hello1 hello.Hello3\nPort:51234", Main.message("FearlessCommandRun", "hello_world", "hello.Hello1", "hello.Hello3", "Port:51234"));
    assertEquals("Run\nhello_world\n\nPort:51234", Main.message("FearlessCommandRun", "hello_world", "Port:51234"));
    assertEquals("Run\nhello_world", Main.message("FearlessCommandRun", "hello_world"));
    assertEquals("Compile\nhello_world\nPort:51234", Main.message("FearlessCommandCompile", "hello_world", "Port:51234"));
    assertEquals("Link\nhello_world\nnotes write Notes Diary", Main.message("FearlessCommandLink", "hello_world", "notes", "write", "Notes", "Diary"));
    assertEquals("Register\nBase16:2F646174612F68656C6C6F", Main.message("FearlessCommandRegister", "Base16:2F646174612F68656C6C6F"));
    assertEquals("Frobnicate\na", Main.message("FearlessCommandFrobnicate", "a"));
  }
  @Test void aStartWithTwoArgumentsIsAnInvalidCallNamingBoth(){
    same("""
      InvalidCall
      Fearless was started with 2 arguments, but without a first argument starting with
      "FearlessCommand" it takes at most one: the file to open.

      The arguments are:
        myProject
        other""", Main.message("myProject", "other"));
  }
  @Test void theMessagesOfARunThatIsNotTheManagerAreAnInvalidCall(){
    same("InvalidCall\nFearless was started with the argument \"FearlessCommandFatalError\", but [###]", Main.message("FearlessCommandFatalError", "x"));
    same("InvalidCall\nFearless was started with the argument \"FearlessCommandInvalidCall\", but [###]", Main.message("FearlessCommandInvalidCall"));
    same("InvalidCall\nFearless was started with an argument holding a newline[###]", Main.message("FearlessCommandShow", "a\nb"));
  }
  @Test void aStartedFileIsTheFolderAround(@TempDir Path dir){
    var project= folder(dir, "myProject");
    var file= project.resolve("hello.fearless");
    Fs.writeUtf8(file, "anything");
    assertEquals(project, Manager.projectFolder(file, folder(dir, "manager")));
  }
  @Test void aPathNamingNothingIsRefused(@TempDir Path dir){
    refused("""
      The manager was asked to register
      [###]myProject
      but nothing exists there: register an existing folder, or a file inside one.""", () -> Manager.projectFolder(dir.resolve("myProject"), folder(dir, "manager")));
  }
  @Test void theManagerFolderIsNotAProject(@TempDir Path dir){
    var managerDir= folder(dir, "manager");
    refused("""
      Fearless cannot keep track of this folder as a project.

      The folder is:
      [###]manager
      The manager folder of this Fearless is:
      [###]manager
      The manager folder holds what Fearless remembers about your projects: it is
      never part of a project, and no project is inside it.
      """, () -> Manager.projectFolder(managerDir, managerDir));
  }
  @Test void aFileInTheManagerFolderIsNotAProjectEither(@TempDir Path dir){
    var managerDir= folder(dir, "manager");
    var file= managerDir.resolve("example.fearless");
    Fs.writeUtf8(file, "anything");
    refused("Fearless cannot keep track of this folder as a project.[###]", () -> Manager.projectFolder(file, managerDir));
  }
  @Test void aFolderUnderAnyPathIsAProject(@TempDir Path dir){
    var project= folder(folder(dir, "caf\u00e9 \ud83d\ude00"), "myProject");
    assertEquals(project, Manager.projectFolder(project, folder(dir, "manager")));
  }
  @Test void aFolderInsideTheManagerFolderIsNotAProjectEither(@TempDir Path dir){
    var managerDir= folder(dir, "manager");
    refused("Fearless cannot keep track of this folder as a project.[###]", () -> Manager.projectFolder(folder(managerDir, "messages"), managerDir));
  }
  @Test void aFolderHoldingTheManagerFolderIsNotAProjectEither(@TempDir Path dir){
    refused("Fearless cannot keep track of this folder as a project.[###]", () -> Manager.projectFolder(dir, folder(dir, "manager")));
  }
  @Test void theRootOfADriveIsNotAProject(@TempDir Path dir){
    refused("""
      Fearless cannot keep track of the root of a drive or of the file system as a
      project.

      The folder is:
      [###]
      Put the project in a folder inside it, and make that folder the project
      folder.
      """, () -> Manager.projectFolder(dir.getRoot(), folder(dir, "manager")));
  }
}

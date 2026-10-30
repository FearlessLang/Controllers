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
    assertEquals("Compile\nhello_world\nPort:51234", Main.message("FearlessCommandCompile", "hello_world", "Port:51234"));
    assertEquals("Mount\nhello_world\nhello.Diary notes", Main.message("FearlessCommandMount", "hello_world", "hello.Diary", "notes"));
    assertEquals("Mount\nhello_world\nhello.Diary notes\nPort:51234", Main.message("FearlessCommandMount", "hello_world", "hello.Diary", "notes", "Port:51234"));
    assertEquals("Show\nPort:51234", Main.message("FearlessCommandShow", "Port:51234"));
    assertEquals("Register\nBase16:2F646174612F68656C6C6F", Main.message("FearlessCommandRegister", "Base16:2F646174612F68656C6C6F"));
  }
  @Test void badArgumentsAreAnInvalidCall(){
    assertEquals("InvalidCall\nRefused request \"build\".\n\"build\" is not a request of the manager: the requests are \"Show\", \"Quit\", \"Reset\", \"Register\", \"ConnectEclipse\", \"EditMetadata\", \"OpenWithOrShortcut\", \"Check\", \"Terminate\", \"DeleteCache\", \"Forget\", \"Run\", \"Compile\", \"Kind\", \"Mount\", \"Attenuate\".", Main.message("FearlessCommandbuild", "hello"));
    assertEquals("InvalidCall\nRefused request \"Forget\" with 2 arguments.\nRequest \"Forget\" takes 1 argument.\nThe arguments are:\n  \"hello\"\n  \"now\"", Main.message("FearlessCommandForget", "hello", "now"));
    same("InvalidCall\nRefused request \"Run\" with 0 arguments.[###]", Main.message("FearlessCommandRun"));
    same("InvalidCall\nRefused request \"Run\" with 1 argument.[###]", Main.message("FearlessCommandRun", "hello"));
    same("InvalidCall\nRefused request \"OpenWithOrShortcut\" with 2 arguments.[###]", Main.message("FearlessCommandOpenWithOrShortcut", "Str:/a.foo", "hello"));
    assertEquals("InvalidCall\nRefused request \"Run\" on project \"hello\".\nThe request names no main.", Main.message("FearlessCommandRun", "hello", "Port:51234"));
    assertEquals("InvalidCall\nRefused request \"Kind\" on project \"hello\".\n\"library\" is not a kind: the kinds are \"idle\", \"code\", \"dataRead\" and \"dataEdit\".", Main.message("FearlessCommandKind", "hello", "library"));
    assertEquals("InvalidCall\nRefused request \"Mount\" on project \"hello\".\n\"hello.Pub data sometimes\" is not a mount: a mount is the qualified name of a DataRead type, then the name of a data project, none to remove the mount of that DataRead type.", Main.message("FearlessCommandMount", "hello", "hello.Pub", "data", "sometimes"));
    assertEquals("InvalidCall\nRefused request \"Mount\" on project \"hello\".\n\"\" is not a mount: a mount is the qualified name of a DataRead type, then the name of a data project, none to remove the mount of that DataRead type.", Main.message("FearlessCommandMount", "hello", ""));
    assertEquals("InvalidCall\nRefused request \"Attenuate\" on project \"hello\".\nThe request names no main: an attenuation is the name of a main, then the attenuators applied to it in order, none to remove them all.", Main.message("FearlessCommandAttenuate", "hello", ""));
    assertEquals("InvalidCall\nThe manager was given the port \"Port:0\", but a port is \"Port:\" then a number from 1 to 65535.", Main.message("FearlessCommandCompile", "hello", "Port:0"));
    assertEquals("InvalidCall\nThe manager was given the port \"Port:65536\", but a port is \"Port:\" then a number from 1 to 65535.", Main.message("FearlessCommandKind", "hello", "code", "Port:65536"));
    same("InvalidCall\nRefused request \"Register\"[###]The path of the request is empty.", Main.message("FearlessCommandRegister", "Str:"));
    same("InvalidCall\nRefused request \"Register\"[###]The path of the request is not a tagged text: [###]", Main.message("FearlessCommandRegister", "hello"));
    same("InvalidCall\n[###]root of a drive[###]", Main.message("FearlessCommandRegister", TaggedText.ofPath(Path.of("").toAbsolutePath().getRoot())));
  }
  @Test void aBlankArgumentIsABrokenPath(){
    same("InvalidCall\nFearless was started to open [###], but that is not a path of a file this system accepts.", Main.message(Main.fileAsArg(new String[]{""})));
    same("InvalidCall\nFearless was started to open [###], but that is not a path of a file this system accepts.", Main.message(Main.fileAsArg(new String[]{" "})));
  }
  @Test void aRelativePathOfAFearlessCommandNamesThePathFromWhereTheCommandStarted(){
    var here= Path.of("").toAbsolutePath();
    assertEquals("Register\nStr:"+here.resolve("myProject"), Main.message("FearlessCommandRegister", "Str:myProject"));
    assertEquals("OpenWithOrShortcut\nStr:"+here.resolve("notes.htm"), Main.message("FearlessCommandOpenWithOrShortcut", "Str:notes.htm"));
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
    same("InvalidCall\nRefused request \"FatalError\".\n\"FatalError\" is not a request of the manager: [###]", Main.message("FearlessCommandFatalError", "x"));
    same("InvalidCall\nRefused request \"InvalidCall\".\n\"InvalidCall\" is not a request of the manager: [###]", Main.message("FearlessCommandInvalidCall"));
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
Refused request "Register" on path
[###]myProject
Nothing exists at that path.""", () -> Manager.projectFolder(dir.resolve("myProject"), folder(dir, "manager")));
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
  @Test void aLinkToTheManagerFolderIsNotAProjectEither(@TempDir Path dir){
    var managerDir= folder(dir, "manager");
    var link= Disk.link(dir.resolve("link"), managerDir);
    refused("Fearless cannot keep track of this folder as a project.[###]", () -> Manager.projectFolder(link, managerDir));
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

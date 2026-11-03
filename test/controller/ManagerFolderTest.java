package controller;

import static controller.Errs.same;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import managerMessages.Report;
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
  private static final String hello= Fs.isWindows() ? "Base16:43003A005C0064006100740061005C00680065006C006C006F00" : "Base16:2F646174612F68656C6C6F";
  private static String invalid(String arg, String why){ return "InvalidCall\nFearless was started with the argument\n  \""+arg+"\"\nand that argument is not a call: "+why; }
  @Test void aStartLeavesTheMessageOfItsArguments(){
    var here= Path.of("").toAbsolutePath();
    assertEquals("Register\n"+TaggedText.ofPath(here.resolve("myProject").resolve("hello.fearless")), Main.message(Path.of("myProject", "sub", "..", "hello.fearless").toString()));
    assertEquals("OpenWithOrShortcut\n"+TaggedText.ofPath(here.resolve("notes.htm")), Main.message("notes.htm"));
    assertEquals("Register\n"+TaggedText.ofPath(here.resolve("Hello.FEARLESS")), Main.message("Hello.FEARLESS"));
    assertEquals("Register\n"+TaggedText.ofPath(here), Main.message("."));
    assertEquals("Show", Main.message());
  }
  @Test void aCallIsOneArgumentAndItsMessageIsItsWordsOnePerLine(){
    assertEquals("Quit", Main.message("FearlessCommandQuit"));
    assertEquals("Show\nhello_world", Main.message("FearlessCommandShow hello_world"));
    assertEquals("SelectMains\nhello_world\nhello.Hello1\nhello.Hello3", Main.message("FearlessCommandSelectMains hello_world hello.Hello1 hello.Hello3"));
    assertEquals("SelectMains\nhello_world", Main.message("FearlessCommandSelectMains hello_world"));
    assertEquals("Run\nhello_world\nPort:51234", Main.message("FearlessCommandRun hello_world Port:51234"));
    assertEquals("Register\n"+hello, Main.message("FearlessCommandRegister "+hello));
    assertEquals("Mount\nhello_world\nhello.Diary\nnotes\nPort:1", Main.message("FearlessCommandMount hello_world hello.Diary notes Port:1"));
    assertEquals("Unmount\nhello_world\nhello.Diary", Main.message("FearlessCommandUnmount hello_world hello.Diary"));
    assertEquals("Attenuate\nhello_world\nhello.Hello1", Main.message("FearlessCommandAttenuate hello_world hello.Hello1"));
    assertEquals("Attenuate\nhello_world\nhello.Hello1\nbase.NoManagerQuit\nhello.NoGui", Main.message("FearlessCommandAttenuate hello_world hello.Hello1 base.NoManagerQuit hello.NoGui"));
    assertEquals("OpenWithOrShortcut\n"+hello+"\nhello_world\nhello.Hello1", Main.message("FearlessCommandOpenWithOrShortcut "+hello+" hello_world hello.Hello1"));
    assertEquals("Kind\nhello_world\ndataEdit\nPort:65535", Main.message("FearlessCommandKind hello_world dataEdit Port:65535"));
    assertEquals("Show\nPort:51234", Main.message("FearlessCommandShow Port:51234"));
  }
  @Test void aBase16PathIsTheUnitsOfThisSystem(){
    var windows= Fs.isWindows();
    var ab= windows ? "Base16:43003A005C0061006200" : "Base16:2F6162";
    assertEquals("Register\n"+ab, Main.message("FearlessCommandRegister "+ab));
    var relative= windows ? "Base16:61006200" : "Base16:6162";
    assertEquals(invalid("FearlessCommandRegister "+relative, "\""+relative+"\" is not "+Report.callBase16+"."), Main.message("FearlessCommandRegister "+relative));
    var nul= windows ? "Base16:2F000000" : "Base16:2F00";
    assertEquals(invalid("FearlessCommandRegister "+nul, "\""+nul+"\" is not "+Report.callBase16+"."), Main.message("FearlessCommandRegister "+nul));
    assertEquals(invalid("FearlessCommandRegister Base16:", "\"Base16:\" is not "+Report.callBase16+"."), Main.message("FearlessCommandRegister Base16:"));
    assertEquals(invalid("FearlessCommandRegister Base16:2f", "\"Base16:2f\" is not "+Report.callBase16+"."), Main.message("FearlessCommandRegister Base16:2f"));
    var root= TaggedText.base16(Path.of("").toAbsolutePath().getRoot());
    assertEquals(invalid("FearlessCommandRegister "+root, "\""+root+"\" is not "+Report.callBase16+"."), Main.message("FearlessCommandRegister "+root));
  }
  @Test void aBase16PathOfAnOddNumberOfBytesIsRefusedOnlyOnWindows(){
    var odd= "Base16:2F646174612F68656C6C6F";
    if (!Fs.isWindows()){ assertEquals("Register\n"+odd, Main.message("FearlessCommandRegister "+odd)); return; }
    assertEquals(invalid("FearlessCommandRegister "+odd, "\""+odd+"\" is not "+Report.callBase16+"."), Main.message("FearlessCommandRegister "+odd));
  }
  @Test void aBase16PathThatIsNotUnicodeStaysBase16(){
    Assumptions.assumeFalse(Fs.isMac());
    var notUnicode= Fs.isWindows() ? "Base16:43003A005C00610000D8" : "Base16:2F636166E9";
    assertEquals("Register\n"+notUnicode, Main.message("FearlessCommandRegister "+notUnicode));
  }
  @Test void aWindowsPathWithARootButNoDriveIsNotAbsolute(){
    Assumptions.assumeTrue(Fs.isWindows());
    var noDrive= "Base16:2F0064006100740061002F00680065006C006C006F00";
    assertEquals(invalid("FearlessCommandRegister "+noDrive, "\""+noDrive+"\" is not "+Report.callBase16+"."), Main.message("FearlessCommandRegister "+noDrive));
  }
  @Test void anArgumentThatIsNotACallIsAnInvalidCall(){
    same(invalid("FearlessCommandbuild hello", "\"FearlessCommandbuild\" is not a command; the commands are \"FearlessCommandAttenuate\", [###], \"FearlessCommandUnmount\"."), Main.message("FearlessCommandbuild hello"));
    assertEquals(invalid("FearlessCommandForget hello now", "the calls of that command are\n  FearlessCommandForget projectName\neach one followed or not by a port: \"Port:\" then a number from 1 to 65535."), Main.message("FearlessCommandForget hello now"));
    assertEquals(invalid("FearlessCommandForget hello now Port:42", "the calls of that command are\n  FearlessCommandForget projectName\neach one followed or not by a port: \"Port:\" then a number from 1 to 65535."), Main.message("FearlessCommandForget hello now Port:42"));
    assertEquals(invalid("FearlessCommandShow a b", "the calls of that command are\n  FearlessCommandShow\n  FearlessCommandShow projectName\neach one followed or not by a port: \"Port:\" then a number from 1 to 65535."), Main.message("FearlessCommandShow a b"));
    same(invalid("FearlessCommandRun", "the calls of that command are\n  FearlessCommandRun projectName\n[###]"), Main.message("FearlessCommandRun"));
    same(invalid("FearlessCommandRun hello hello.Hello", "the calls of that command are\n  FearlessCommandRun projectName\n[###]"), Main.message("FearlessCommandRun hello hello.Hello"));
    same(invalid("FearlessCommandAttenuate hello", "the calls of that command are\n  FearlessCommandAttenuate projectName typeName typeName1..typeNamen\n[###]"), Main.message("FearlessCommandAttenuate hello"));
    assertEquals(invalid("FearlessCommandKind hello library", "\"library\" is not "+Report.callKind+"."), Main.message("FearlessCommandKind hello library"));
    assertEquals(invalid("FearlessCommandShow Hello", "\"Hello\" is not "+Report.callProjectName+"."), Main.message("FearlessCommandShow Hello"));
    assertEquals(invalid("FearlessCommandShow con", "\"con\" is not "+Report.callProjectName+"."), Main.message("FearlessCommandShow con"));
    assertEquals(invalid("FearlessCommandMount hello Pub data", "\"Pub\" is not "+Report.callTypeName+"."), Main.message("FearlessCommandMount hello Pub data"));
    assertEquals(invalid("FearlessCommandMount hello prn.Pub data", "\"prn.Pub\" is not "+Report.callTypeName+"."), Main.message("FearlessCommandMount hello prn.Pub data"));
    assertEquals(invalid("FearlessCommandCompile hello Port:0", "\"Port:0\" is not "+Report.callPort+"."), Main.message("FearlessCommandCompile hello Port:0"));
    assertEquals(invalid("FearlessCommandKind hello code Port:65536", "\"Port:65536\" is not "+Report.callPort+"."), Main.message("FearlessCommandKind hello code Port:65536"));
    assertEquals(invalid("FearlessCommandSelectMains hello hello.A hello.B hello.A", "the mains of \"FearlessCommandSelectMains\" are all different, and \"hello.A\" is named twice."), Main.message("FearlessCommandSelectMains hello hello.A hello.B hello.A"));
    assertEquals(invalid("FearlessCommandShow  hello", "the calls of that command are\n  FearlessCommandShow\n  FearlessCommandShow projectName\neach one followed or not by a port: \"Port:\" then a number from 1 to 65535."), Main.message("FearlessCommandShow  hello"));
    assertEquals(invalid("FearlessCommandForget hello ", "the calls of that command are\n  FearlessCommandForget projectName\neach one followed or not by a port: \"Port:\" then a number from 1 to 65535."), Main.message("FearlessCommandForget hello "));
  }
  @Test void aBlankArgumentIsABrokenPath(){
    same("InvalidCall\nFearless was started to open [###],\nbut that is not a path of a file this system accepts.", Main.message(Main.fileAsArg(new String[]{""})));
    same("InvalidCall\nFearless was started to open [###],\nbut that is not a path of a file this system accepts.", Main.message(Main.fileAsArg(new String[]{" "})));
  }
  @Test void aStartWithTwoArgumentsIsAnInvalidCallNamingBoth(){
    same("""
InvalidCall
Fearless was started with 2 arguments, but Fearless takes at most one: a call
starting with "FearlessCommand", or the file to open.

The arguments are:
  FearlessCommandRun
  hello""", Main.message("FearlessCommandRun", "hello"));
  }
  @Test void theMessagesOfARunThatIsNotTheManagerAreAnInvalidCall(){
    same(invalid("FearlessCommandFatalError x", "\"FearlessCommandFatalError\" is not a command[###]"), Main.message("FearlessCommandFatalError x"));
    same(invalid("FearlessCommandInvalidCall", "\"FearlessCommandInvalidCall\" is not a command[###]"), Main.message("FearlessCommandInvalidCall"));
    same("InvalidCall\nFearless was started with the argument\n  \"FearlessCommandShow a\" [###] \"b\"\nand that argument is not a call: [###]", Main.message("FearlessCommandShow a\nb"));
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
    refused("[###]link\nThis path is a symbolic link, and Fearless does not follow symbolic links.", () -> Manager.projectFolder(link, managerDir));
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

package controller;

import static controller.Errs.same;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tempDirs.RealTempDir;

import managerMessages.Report;
import tools.Fs;
import tools.JavacTool;
import utils.OneOr;

final class ManagerFolderTest{
  @AfterEach void checkedByHandOnWindows(){ Disk.checkedByHandOnWindows(); }
  private static Path folder(Path dir, String name){
    var res= dir.resolve(name);
    Fs.ensureDir(res);
    return res;
  }
  private static Path projectFolder(Path path, List<Path> registered, Path managerDir){
    var folder= Manager.projectFolder(path);
    return Manager.chosen(folder, Manager.away(folder, registered, managerDir));
  }
  private static void refused(String expected, Runnable body){ same(expected, assertThrows(Refused.class, body::run).getMessage()); }
  @Test void aStartedFolderIsTheProjectItself(@RealTempDir Path dir){
    var project= folder(dir, "myProject");
    assertEquals(project, projectFolder(project, List.of(), folder(dir, "manager")));
  }
  private static final String hello= Fs.isWindows() ? "Base16:43003A005C0064006100740061005C00680065006C006C006F00" : "Base16:2F646174612F68656C6C6F";
  private static String message(String... args){ return OneOr.of("one message", Main.messages(args).stream()); }
  private static String invalid(String arg, String why){ return "InvalidCall\nFearless was started with the argument\n  \""+arg+"\"\nand that argument is not a call: "+why; }
  @Test void aStartLeavesTheMessageOfItsArguments(){
    var here= Path.of("").toAbsolutePath();
    assertEquals(List.of("Register\n"+TaggedText.ofPath(here.resolve("myProject").resolve("hello.fearless")), "Show"), Main.messages(Path.of("myProject", "sub", "..", "hello.fearless").toString()));
    assertEquals(List.of("OpenWithOrShortcut\n"+TaggedText.ofPath(here.resolve("notes.htm"))), Main.messages("notes.htm"));
    assertEquals(List.of("Register\n"+TaggedText.ofPath(here.resolve("Hello.FEARLESS")), "Show"), Main.messages("Hello.FEARLESS"));
    assertEquals(List.of("Register\n"+TaggedText.ofPath(here), "Show"), Main.messages("."));
    assertEquals("Show", message());
  }
  @Test void aDroppedFolderOrFearlessFileIsRegisteredAndAnyOtherFileIsOpened(@RealTempDir Path dir){
    assertTrue(Main.isProject(dir));
    assertTrue(Main.isProject(dir.resolve("Hello.FEARLESS")));
    assertFalse(Main.isProject(dir.resolve("notes.htm")));
  }
  @Test void aCallIsOneArgumentAndItsMessageIsItsWordsOnePerLine(){
    assertEquals("Quit", message("FearlessCommandQuit"));
    assertEquals("Show\nhello_world", message("FearlessCommandShow hello_world"));
    assertEquals("SelectMains\nhello_world\nhello.Hello1\nhello.Hello3", message("FearlessCommandSelectMains hello_world hello.Hello1 hello.Hello3"));
    assertEquals("SelectMains\nhello_world", message("FearlessCommandSelectMains hello_world"));
    assertEquals("Run\nhello_world\nPort:51234", message("FearlessCommandRun hello_world Port:51234"));
    assertEquals("Register\n"+hello, message("FearlessCommandRegister "+hello));
    assertEquals("Mount\nhello_world\nhello.Diary\nnotes\nPort:1", message("FearlessCommandMount hello_world hello.Diary notes Port:1"));
    assertEquals("Unmount\nhello_world\nhello.Diary", message("FearlessCommandUnmount hello_world hello.Diary"));
    assertEquals("Attenuate\nhello_world\nhello.Hello1", message("FearlessCommandAttenuate hello_world hello.Hello1"));
    assertEquals("Attenuate\nhello_world\nhello.Hello1\nbase.NoManagerQuit\nhello.NoGui", message("FearlessCommandAttenuate hello_world hello.Hello1 base.NoManagerQuit hello.NoGui"));
    assertEquals("OpenWithOrShortcut\n"+hello+"\nhello_world\nhello.Hello1", message("FearlessCommandOpenWithOrShortcut "+hello+" hello_world hello.Hello1"));
    assertEquals("Kind\nhello_world\ndataEdit\nPort:65535", message("FearlessCommandKind hello_world dataEdit Port:65535"));
    assertEquals("Show\nPort:51234", message("FearlessCommandShow Port:51234"));
  }
  @Test void aBase16PathIsTheUnitsOfThisSystem(){
    var windows= Fs.isWindows();
    var ab= windows ? "Base16:43003A005C0061006200" : "Base16:2F6162";
    assertEquals("Register\n"+ab, message("FearlessCommandRegister "+ab));
    var notAbsolute= windows ? List.of("Base16:61006200", "Base16:2F0064006100740061002F00680065006C006C006F00") : List.of("Base16:6162");
    for (var relative : notAbsolute){ assertEquals(invalid("FearlessCommandRegister "+relative, "\""+relative+"\" is not "+Report.callBase16+"."), message("FearlessCommandRegister "+relative)); }
    var nul= windows ? "Base16:2F000000" : "Base16:2F00";
    assertEquals(invalid("FearlessCommandRegister "+nul, "\""+nul+"\" is not "+Report.callBase16+"."), message("FearlessCommandRegister "+nul));
    assertEquals(invalid("FearlessCommandRegister Base16:", "\"Base16:\" is not "+Report.callBase16+"."), message("FearlessCommandRegister Base16:"));
    assertEquals(invalid("FearlessCommandRegister Base16:2f", "\"Base16:2f\" is not "+Report.callBase16+"."), message("FearlessCommandRegister Base16:2f"));
    var root= TaggedText.base16(Path.of("").toAbsolutePath().getRoot());
    assertEquals(invalid("FearlessCommandRegister "+root, "\""+root+"\" is not "+Report.callBase16+"."), message("FearlessCommandRegister "+root));
  }
  @Test void aBase16PathNamingTheRootOfADriveWithADotDotIsNotACall(){
    var root= TaggedText.base16(Path.of("").toAbsolutePath().getRoot().resolve(".."));
    assertEquals(invalid("FearlessCommandRegister "+root, "\""+root+"\" is not "+Report.callBase16+"."), message("FearlessCommandRegister "+root));
  }
  @Test void aBase16PathOfAnOddNumberOfBytesIsRefusedOnlyOnWindows(){
    var odd= "Base16:2F646174612F68656C6C6F";
    if (!Fs.isWindows()){ assertEquals("Register\n"+odd, message("FearlessCommandRegister "+odd)); return; }
    assertEquals(invalid("FearlessCommandRegister "+odd, "\""+odd+"\" is not "+Report.callBase16+"."), message("FearlessCommandRegister "+odd));
  }
  @Test void aBase16PathThatIsNotUnicodeStaysBase16(){
    var notUnicode= Fs.isWindows() ? "Base16:43003A005C00610000D8" : "Base16:2F636166E9";
    assertEquals("Register\n"+notUnicode, message("FearlessCommandRegister "+notUnicode));
  }
  @Test void theArgumentOpenedIsABadArgumentOutsideMacOS(@RealTempDir Path dir) throws IOException{
    if (Fs.isMac()){ openedByAnEvent(dir); return; }
    same(invalid("FearlessCommandOpened", "\"FearlessCommandOpened\" is not a command; the commands are \"FearlessCommandAttenuate\", [###], \"FearlessCommandUnmount\"."), message("FearlessCommandOpened"));
  }
  private static void openedByAnEvent(Path dir) throws IOException{
    var app= TestApp.create(dir.resolve("fearlessManaged0_999.app"), "controller.Main", "-ea", "--enable-native-access=ALL-UNNAMED",
      "-D"+JavacTool.appDirKey+"=$APPDIR", "-D"+JavacTool.launcherKey+"=console", "-D"+JavacTool.versionIdKey+"=0_999");
    var messages= folder(folder(dir, "fearless0_999"), "messages");
    var notes= dir.resolve("notes.htm");
    var data= dir.resolve("data.txt");
    Fs.writeUtf8(notes, "");
    Fs.writeUtf8(data, "");
    try (var lock= FileChannel.open(messages.resolveSibling("instance.lock"), CREATE, WRITE); var _= lock.lock()){
      TestApp.ended(TestApp.open("-W", "-n", "-a", app.toString(), notes.toString(), data.toString(), "--args", "FearlessCommandOpened"));
    }
    var left= Fs.of(() -> { try (var s= Files.list(messages)){ return s.map(Fs::readUtf8).sorted().toList(); }});
    assertEquals(List.of("OpenWithOrShortcut\n"+TaggedText.ofPath(data), "OpenWithOrShortcut\n"+TaggedText.ofPath(notes)), left);
  }
  @Test void anArgumentThatIsNotACallIsAnInvalidCall(){
    same(invalid("FearlessCommandbuild hello", "\"FearlessCommandbuild\" is not a command; the commands are \"FearlessCommandAttenuate\", [###], \"FearlessCommandUnmount\"."), message("FearlessCommandbuild hello"));
    assertEquals(invalid("FearlessCommandForget hello now", "the calls of that command are\n  FearlessCommandForget projectName\neach one followed or not by a port: \"Port:\" then a number from 1 to 65535."), message("FearlessCommandForget hello now"));
    assertEquals(invalid("FearlessCommandForget hello now Port:42", "the calls of that command are\n  FearlessCommandForget projectName\neach one followed or not by a port: \"Port:\" then a number from 1 to 65535.\nPort:42"), message("FearlessCommandForget hello now Port:42"));
    assertEquals(invalid("FearlessCommandShow a b", "the calls of that command are\n  FearlessCommandShow\n  FearlessCommandShow projectName\neach one followed or not by a port: \"Port:\" then a number from 1 to 65535."), message("FearlessCommandShow a b"));
    same(invalid("FearlessCommandRun", "the calls of that command are\n  FearlessCommandRun projectName\n[###]"), message("FearlessCommandRun"));
    same(invalid("FearlessCommandRun hello hello.Hello", "the calls of that command are\n  FearlessCommandRun projectName\n[###]"), message("FearlessCommandRun hello hello.Hello"));
    same(invalid("FearlessCommandAttenuate hello", "the calls of that command are\n  FearlessCommandAttenuate projectName typeName typeName1..typeNamen\n[###]"), message("FearlessCommandAttenuate hello"));
    assertEquals(invalid("FearlessCommandKind hello library", "\"library\" is not "+Report.callKind+"."), message("FearlessCommandKind hello library"));
    assertEquals(invalid("FearlessCommandShow Hello", "\"Hello\" is not "+Report.callProjectName+"."), message("FearlessCommandShow Hello"));
    assertEquals(invalid("FearlessCommandShow con", "\"con\" is not "+Report.callProjectName+"."), message("FearlessCommandShow con"));
    assertEquals(invalid("FearlessCommandMount hello Pub data", "\"Pub\" is not "+Report.callTypeName+"."), message("FearlessCommandMount hello Pub data"));
    assertEquals(invalid("FearlessCommandMount hello prn.Pub data", "\"prn.Pub\" is not "+Report.callTypeName+"."), message("FearlessCommandMount hello prn.Pub data"));
    assertEquals(invalid("FearlessCommandCompile hello Port:0", "\"Port:0\" is not "+Report.callPort+"."), message("FearlessCommandCompile hello Port:0"));
    assertEquals(invalid("FearlessCommandKind hello code Port:65536", "\"Port:65536\" is not "+Report.callPort+"."), message("FearlessCommandKind hello code Port:65536"));
    assertEquals(invalid("FearlessCommandSelectMains hello hello.A hello.B hello.A", "the mains of \"FearlessCommandSelectMains\" are all different, and \"hello.A\" is named twice."), message("FearlessCommandSelectMains hello hello.A hello.B hello.A"));
    assertEquals(invalid("FearlessCommandShow  hello", "the calls of that command are\n  FearlessCommandShow\n  FearlessCommandShow projectName\neach one followed or not by a port: \"Port:\" then a number from 1 to 65535."), message("FearlessCommandShow  hello"));
    assertEquals(invalid("FearlessCommandForget hello ", "the calls of that command are\n  FearlessCommandForget projectName\neach one followed or not by a port: \"Port:\" then a number from 1 to 65535."), message("FearlessCommandForget hello "));
  }
  @Test void aBlankArgumentIsABrokenPath(){
    same("InvalidCall\nFearless was started to open [###],\nbut that is not a path of a file this system accepts.", message(Main.fileAsArg(new String[]{""})));
    same("InvalidCall\nFearless was started to open [###],\nbut that is not a path of a file this system accepts.", message(Main.fileAsArg(new String[]{" "})));
  }
  @Test void aStartWithTwoArgumentsIsAnInvalidCallNamingBoth(){
    same("""
InvalidCall
Fearless was started with 2 arguments, but Fearless takes at most one: a call
starting with "FearlessCommand", or the file to open.

The arguments are:
  FearlessCommandRun
  hello""", message("FearlessCommandRun", "hello"));
  }
  @Test void theMessagesOfARunThatIsNotTheManagerAreAnInvalidCall(){
    same(invalid("FearlessCommandFatalError x", "\"FearlessCommandFatalError\" is not a command[###]"), message("FearlessCommandFatalError x"));
    same(invalid("FearlessCommandInvalidCall", "\"FearlessCommandInvalidCall\" is not a command[###]"), message("FearlessCommandInvalidCall"));
    same("InvalidCall\nFearless was started with the argument\n  \"FearlessCommandShow a\" [###] \"b\"\nand that argument is not a call: [###]", message("FearlessCommandShow a\nb"));
  }
  @Test void aStartedFileIsTheFolderAround(@RealTempDir Path dir){
    var project= folder(dir, "myProject");
    var file= project.resolve("hello.fearless");
    Fs.writeUtf8(file, "anything");
    assertEquals(project, projectFolder(file, List.of(), folder(dir, "manager")));
  }
  @Test void aPathNamingNothingIsRefused(@RealTempDir Path dir){
    refused("""
Refused request "Register" on path
[###]myProject
Nothing exists at that path.""", () -> projectFolder(dir.resolve("myProject"), List.of(), folder(dir, "manager")));
  }
  @Test void theManagerFolderIsNotAProject(@RealTempDir Path dir){
    var managerDir= folder(dir, "manager");
    refused("""
Fearless cannot keep track of this folder as a project.

The folder is:
[###]manager
The manager folder of this Fearless is:
[###]manager
The manager folder holds what Fearless remembers about your projects: it is
never part of a project, and no project is inside it.
""", () -> projectFolder(managerDir, List.of(), managerDir));
  }
  @Test void aFileInTheManagerFolderIsNotAProjectEither(@RealTempDir Path dir){
    var managerDir= folder(dir, "manager");
    var file= managerDir.resolve("example.fearless");
    Fs.writeUtf8(file, "anything");
    refused("Fearless cannot keep track of this folder as a project.[###]", () -> projectFolder(file, List.of(), managerDir));
  }
  @Test void aFolderUnderAnyPathIsAProject(@RealTempDir Path dir){
    var project= folder(folder(dir, "caf\u00e9 \ud83d\ude00"), "myProject");
    assertEquals(project, projectFolder(project, List.of(), folder(dir, "manager")));
  }
  @Test void aFolderInsideTheManagerFolderIsNotAProjectEither(@RealTempDir Path dir){
    var managerDir= folder(dir, "manager");
    refused("Fearless cannot keep track of this folder as a project.[###]", () -> projectFolder(folder(managerDir, "messages"), List.of(), managerDir));
  }
  @Test void aFolderHoldingTheManagerFolderIsNotAProjectEither(@RealTempDir Path dir){
    refused("Fearless cannot keep track of this folder as a project.[###]", () -> projectFolder(dir, List.of(), folder(dir, "manager")));
  }
  @Test void aLinkToTheManagerFolderIsNotAProjectEither(@RealTempDir Path dir){
    var managerDir= folder(dir, "manager");
    var link= Disk.link(dir.resolve("link"), managerDir);
    refused("Refused request \"Register\" on path\n[###]link\nThe path goes through the symbolic link/junction/mount point\n[###]link\nand Fearless does not follow symbolic links/junctions/mount points.", () -> projectFolder(link, List.of(), managerDir));
  }
  @Test void aPathThroughALinkIsNotAProject(@RealTempDir Path dir){
    folder(folder(dir, "real"), "inner");
    var inner= Disk.link(dir.resolve("link"), dir.resolve("real")).resolve("inner");
    refused("Refused request \"Register\" on path\n[###]inner\nThe path goes through the symbolic link/junction/mount point\n[###]link\nand Fearless does not follow symbolic links/junctions/mount points.", () -> projectFolder(inner, List.of(), folder(dir, "manager")));
  }
  @Test void aPathInAnotherLetterCaseIsNotAProject(@RealTempDir Path dir){
    folder(dir, "hello");
    var upper= dir.resolve("HELLO");
    var expected= !Fs.isLinux() ? "Refused request \"Register\" on path\n[###]HELLO\nThe folder\n[###]\nlists no folder named exactly \"HELLO\": write each name of the path as its folder lists it, in the same letter case and the same Unicode form." : "Refused request \"Register\" on path\n[###]HELLO\nNothing exists at that path.";
    refused(expected, () -> projectFolder(upper, List.of(), folder(dir, "manager")));
    if (!Fs.isWindows()){ return; }
    var lower= Path.of(dir.toString().substring(0, 1).toLowerCase(Locale.ROOT)+dir.toString().substring(1));
    refused("Refused request \"Register\" on path\n[###]\nThe letter of the drive is lowercase: write the drive with its uppercase letter, as in \"C:\\\".", () -> projectFolder(lower, List.of(), folder(dir, "manager")));
  }
  @Test void aFolderInsideOrAroundARegisteredFolderIsNotAProjectAndARegisteredFolderIsItself(@RealTempDir Path dir){
    var manager= folder(dir, "manager");
    var hello= folder(folder(dir, "around"), "hello");
    refused("Refused request \"Register\" on path\n[###]inner\nA registered project has the folder\n[###]hello\nOne of the two folders is inside the other[###]", () -> projectFolder(folder(hello, "inner"), List.of(hello), manager));
    refused("Refused request \"Register\" on path\n[###]around\nA registered project has the folder\n[###]hello\nOne of the two folders is inside the other[###]", () -> projectFolder(dir.resolve("around"), List.of(hello), manager));
    assertEquals(hello, projectFolder(hello, List.of(hello), manager));
    assertEquals(List.of("inner"), Names.list(hello).stream().map(p -> p.getFileName().toString()).toList());
  }
  @Test void theRootOfADriveIsNotAProject(@RealTempDir Path dir){
    refused("Refused request \"Register\" on path\n[###]\nThe path goes through the symbolic link/junction/mount point\n[###]\nand Fearless does not follow symbolic links/junctions/mount points.", () -> projectFolder(dir.getRoot(), List.of(), folder(dir, "manager")));
  }
  @Test void theTopFolderOfADriveIsNotAProjectAndAFolderInsideItIs(@RealTempDir Path dir) throws IOException{
    var top= dir;
    while (top.getParent() != null && Files.getFileStore(top).equals(Files.getFileStore(top.getParent()))){ top = top.getParent(); }
    var drive= top;
    var manager= folder(dir, "manager");
    refused("Refused request \"Register\" on path\n[###]\nThe path goes through the symbolic link/junction/mount point\n[###]\nand Fearless does not follow symbolic links/junctions/mount points.", () -> projectFolder(drive, List.of(), manager));
    var hello= folder(dir, "hello");
    assertEquals(hello, projectFolder(hello, List.of(), manager));
  }
}

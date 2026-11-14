package suggest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import tempDirs.RealTempDir;

import controller.Names;
import tools.Fs;
import utils.Err;

final class ShapesTest{
  static{ Err.setUp(AssertionFailedError.class, Assertions::assertEquals, Assertions::assertTrue); }
  private static final boolean windows= System.getProperty("os.name").startsWith("Windows");
  private static void refused(String expected, String path, boolean file, List<String> siblings){ Err.strCmp(expected, Shapes.problem(path, file, siblings).orElseThrow()); }
  private static void refused(String expected, String path, boolean file){ refused(expected, path, file, List.of()); }
  private static void accepted(String path, boolean file){ assertEquals(Optional.empty(), Shapes.problem(path, file, List.of())); }
  private static Path folder(Path dir, String name){
    var res= dir.resolve(name);
    Fs.ensureDir(res);
    return res;
  }
  @Test void theFixedSegmentTurnsATextIntoAValidSegment(){
    assertEquals("helloworld", Shapes.fixedSegment("helloWorld"));
    assertEquals("map_a_to_pkc", Shapes.fixedSegment("map_a_to_pkc"));
    assertEquals("a_b", Shapes.fixedSegment("a - b"));
    assertEquals("p2fast", Shapes.fixedSegment("2fast"));
    assertEquals("pcon", Shapes.fixedSegment("con"));
    assertEquals("pcom1", Shapes.fixedSegment("com1"));
    assertEquals("com", Shapes.fixedSegment("Com"));
    assertEquals("_elvin", Shapes.fixedSegment("\u212Aelvin"));
    assertEquals("_x", Shapes.fixedSegment("\u0130x"));
    assertEquals("_rger", Shapes.fixedSegment("\u00C4rger"));
    assertEquals("pa"+"a".repeat(191), Shapes.fixedSegment("a".repeat(192)));
  }
  @Test void theNameIsTheFirst64CharactersOfTheFixedSegmentOfTheFolderName(@RealTempDir Path dir){
    assertEquals("ab".repeat(32), Shapes.infer(folder(dir, "Ab".repeat(50)), Set.of()));
    assertEquals("p9"+"a".repeat(62), Shapes.infer(folder(dir, "9"+"a".repeat(100)), Set.of()));
    assertEquals("ab".repeat(32)+"2", Shapes.infer(folder(dir, "ab".repeat(50)), Set.of("ab".repeat(32))));
  }
  @Test void aTakenNameGetsTheFirstNumberGivingAFreeValidSegment(@RealTempDir Path dir){
    assertEquals("hello2", Shapes.infer(folder(dir, "hello"), Set.of("hello")));
    assertEquals("hello3", Shapes.infer(folder(dir, "hello"), Set.of("hello", "hello2")));
    assertEquals("com10", Shapes.infer(folder(dir, "com"), Set.of("com")));
    assertEquals("lpt", Shapes.infer(folder(dir, "lpt"), Set.of()));
  }
  @Test void aFolderOfTheNameDoesNotTakeTheName(@RealTempDir Path dir){
    var project= folder(dir, "hello");
    folder(project, "hello");
    assertEquals("hello", Shapes.infer(project, Set.of()));
  }
  @Test void theFirstMarkerNamesTheProjectUnlessItsNameIsTakenOrNotAValidSegment(@RealTempDir Path dir){
    var project= folder(dir, "someProject");
    Fs.writeUtf8(project.resolve("start.fearless"), "\n");
    assertEquals("start", Shapes.infer(project, Set.of()));
    assertEquals("someproject", Shapes.infer(project, Set.of("start")));
    var bad= folder(dir, "bad");
    Fs.writeUtf8(bad.resolve("My Game.fearless"), "\n");
    assertEquals("bad", Shapes.infer(bad, Set.of()));
  }
  @Test void markersAreTheFearlessFilesAtTheTopThatAreNotHidden(@RealTempDir Path dir){
    var project= folder(dir, "p");
    Fs.writeUtf8(project.resolve(".old.fearless"), "");
    Fs.writeUtf8(project.resolve("zeta.fearless"), "");
    Fs.writeUtf8(project.resolve("alpha.fearless"), "");
    Fs.ensureDir(project.resolve("folder.fearless"));
    Fs.writeUtf8(project.resolve("lib").resolve("deep.fearless"), "");
    assertEquals(List.of(project.resolve("alpha.fearless"), project.resolve("zeta.fearless")), Shapes.markers(project));
    assertEquals("alpha", Shapes.marker(project));
    assertEquals(List.of(true, false, false), List.of("hello.fearless", ".hello.fearless", "hello.fear").stream().map(Shapes::isMarker).toList());
  }
  @Test void aRenameBreakingAVisibleNameIsRefusedNamingTheRule(){
    refused("\"Main.fear\" starts with \"M\": a Fearless name starts with a lowercase letter or \"_\".", "lib/Main.fear", true);
    refused("\"a b\" contains \" \": a Fearless name contains only lowercase letters, digits and \"_\".", "lib/a b", false);
    refused("\"a.b\" contains \".\": a Fearless name contains only lowercase letters, digits and \"_\".", "a.b", false);
    refused("\"con.txt\" is named like the device \"con\" of Windows: Windows cannot hold it.", "lib/con.txt", true);
    refused("\"readme\" has no extension: a file name is a name, a dot and an extension.", "readme", true);
    refused("\"a.b.c\" has the extension \"b.c\": an extension with a dot is one of css.map, d.ts, d.ts.map, js.map, min.css, min.js, tar.bz2, tar.gz, tar.lz, tar.lz4, tar.xz, tar.zst.", "a.b.c", true);
    refused("\"a.TXT\" has the extension \"TXT\": an extension is 1 to 16 lowercase letters and digits.", "a.TXT", true);
    refused("\""+"a".repeat(192)+"\" is a name longer than 191 characters: a Fearless name is at most 191 characters long.", "a".repeat(192), false);
    refused("A name is empty: the path has \"/\" at its start or end, or twice in a row.", "lib//a.txt", true);
    accepted("lib/_pkg/main.fear", true);
    accepted("web/a.tar.gz", true);
    accepted("lib", false);
  }
  @Test void aPathLongerThan200CharactersOr250ForAHiddenEntryIsRefused(){
    var x= "x".repeat(150)+"/"+"y".repeat(45);
    accepted(x+".txt", true);
    refused("\""+x+".text\" is longer than 200 characters: a path inside a project folder is at most 200 characters long, or 250 for a hidden file or folder.", x+".text", true);
    accepted(".h/"+"y".repeat(247), true);
    refused("\".h/"+"y".repeat(248)+"\" is longer than 250 characters: a path inside a project folder is at most 200 characters long, or 250 for a hidden file or folder.", ".h/"+"y".repeat(248), true);
  }
  @Test void theLengthOfAPathIsInBytesOfUtf8OrInUnitsOfUtf16OnWindows(){
    var name= "."+"\u00E9".repeat(128);
    var expected= windows
      ? "\""+name+"\" is longer than 255 bytes in UTF-8: a hidden name is at most 255 bytes long."
      : "\""+name+"\" is longer than 250 characters: a path inside a project folder is at most 200 characters long, or 250 for a hidden file or folder.";
    refused(expected, name, true);
  }
  @Test void aHiddenNameFollowsTheRulesOfHiddenNames(){
    accepted(".git/HEAD", true);
    accepted(".config/My Notes.TXT", true);
    refused("\"x.\" ends with \".\": a hidden name does not end with a dot or a space.", ".git/x.", true);
    refused("\".x \" ends with \" \": a hidden name does not end with a dot or a space.", ".x ", false);
    refused("\"a:b\" contains \":\": a hidden name contains none of < > : \" / \\ | ? *", ".git/a:b", true);
    refused("\".a\u0001\" contains the control character U+0001: a hidden name contains no control character.", ".a\u0001", true);
    refused("\".a\uD800\" contains U+FFFD or half of a surrogate pair: a hidden name is text the file system can store.", ".a\uD800", true);
    refused("\"Con .txt\" is named like the device \"con\" of Windows: Windows cannot hold it.", ".git/Con .txt", true);
    refused("\"com\u00B9.x\" is named like the device \"com\u00B9\" of Windows: Windows cannot hold it.", ".git/com\u00B9.x", true);
  }
  @Test void aNameClashingWithASiblingIsRefused(){
    refused("\".Foo\" and \".foo\" differ only in letter case or in Unicode normalization: a folder cannot contain both.", ".Foo", false, List.of(".foo"));
    refused("\".caf\u00E9\" and \".cafe\u0301\" differ only in letter case or in Unicode normalization: a folder cannot contain both.", ".caf\u00E9", true, List.of(".cafe\u0301"));
    refused("\"x.zip\" and \"x\" are a name and that name followed by \".zip\": a folder cannot contain both.", "lib/x.zip", true, List.of("x"));
    refused("\"x\" and \"x.zip\" are a name and that name followed by \".zip\": a folder cannot contain both.", "lib/x", false, List.of("x.zip"));
    assertEquals(Optional.empty(), Shapes.problem(".x", false, List.of(".x.zip")));
    assertEquals(Optional.empty(), Shapes.problem("lib/b.txt", true, List.of("a.txt")));
  }
  @Test void theNewPackageAndNewTypePathsFollowThePackageRules(){
    Err.strCmp("\"lib/_a/_b/x.fear\" is inside two package folders, \"_a\" and \"_b\": a package folder cannot contain another package folder.", Shapes.fearless("lib/_a/_b/x.fear", true).orElseThrow());
    Err.strCmp("\"lib/x.fear\" is outside every package folder: a .fear file goes in a package folder, a folder whose name starts with \"_\".", Shapes.fearless("lib/x.fear", true).orElseThrow());
    Err.strCmp("\"_base\" is a reserved package name: \"base\" and \"rank\" belong to the standard library.", Shapes.fearless("_base", false).orElseThrow());
    Err.strCmp("\"_9x\" is not a valid package folder: after \"_\" a package name starts with a lowercase letter.", Shapes.fearless("_9x", false).orElseThrow());
    Err.strCmp("\"Point.fear\" starts with \"P\": a Fearless name starts with a lowercase letter or \"_\".", Shapes.fearless("_pkg/Point.fear", true).orElseThrow());
    assertEquals(Optional.empty(), Shapes.fearless("lib/_pkg/point.fear", true));
  }
  @Test void aMarkerNameIsValidExactlyWhenTheManagerTakesItsStemAsAProjectName(){
    for (var n : List.of("my_game", "_x9", "myGame", "2fast", "a__b", "con", "lpt", "a".repeat(191), "a".repeat(192), "a-b", "\u00E9")){
      assertEquals(Names.isName(n), Shapes.validSegment(n), n);
      assertEquals(Names.isName(n), Shapes.problem(n+Shapes.ext, true, List.of()).isEmpty(), n);
    }
  }
}

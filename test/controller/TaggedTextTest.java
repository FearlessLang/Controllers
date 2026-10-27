package controller;

import static controller.Errs.err;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import fileSupport.Info;
import tools.Fs;
import tools.PathBytes;
import userMessages.UserError;

final class TaggedTextTest{
  private static String read(String text){ return TaggedText.read(text, UserError::new); }
  private static void roundTrip(String text, String tagged){
    assertEquals(tagged, TaggedText.of(text));
    assertEquals(text, read(tagged));
  }
  @Test void aTextInTheCharacterSetIsStr(){
    roundTrip("C:\\data\\hello", "Str:C:\\data\\hello");
    roundTrip("a \"b\"\n", "Str:a \"b\"\n");
    roundTrip("", "Str:");
  }
  @Test void aTextOnOneLineHoldingANewlineIsNotStr(){
    assertEquals("Str:C:\\data", TaggedText.line("C:\\data"));
    assertEquals("UStr:(\"a\" | \"b\").u", TaggedText.line("a\nb"));
    assertEquals("a\nb", read(TaggedText.line("a\nb")));
  }
  @Test void aUnicodeTextIsUStrAsUStrEscapePrintsIt(){
    roundTrip("\u00e9", "UStr:\"\".u\"00E9\"");
    roundTrip("C:/data/caf\u00e9/hello", "UStr:\"C:/data/caf\".u\"00E9\"+(\"/hello\".u)");
    roundTrip("a\ud83d\ude00b\u00e9c", "UStr:\"a\".u\"1F600\"+(\"b\".u\"00E9\")+(\"c\".u)");
    roundTrip("e\u0301\u00e9\t\ud83d\ude00", "UStr:\"e\".u\"0301 00E9 0009 1F600\"");
    roundTrip("\u00e9\na\"b", "UStr:\"\".u\"00E9\"+((\"\" | `a\"b`).u)");
    roundTrip("C:\\data\u00e9 \ud83d\ude00\\x", "UStr:\"C:\\data\".u\"00E9\"+(\" \".u\"1F600\")+(\"\\x\".u)");
  }
  private static Path readPath(String tagged){ return TaggedText.readPath(tagged, UserError::new); }
  private static void pathRoundTrip(Path p, String tagged){
    assertEquals(tagged, TaggedText.ofPath(p));
    assertEquals(p, readPath(tagged));
  }
  @Test void aPathThatIsNotUnicodeIsBase16OfTheUtf16LittleEndianUnitsOfWindows(){
    Assumptions.assumeTrue(Fs.isWindows());
    pathRoundTrip(Path.of("C:/a\ud800"), "Base16:43003A005C00610000D8");
  }
  @Test void aPathIsStrOrUStrWhenItsBytesAreUtf8AndBase16Otherwise(){
    Assumptions.assumeTrue(Fs.isLinux());
    pathRoundTrip(Path.of("/tmp/data"), "Str:/tmp/data");
    pathRoundTrip(Path.of("/tmp/caf\u00e9"), "UStr:\"/tmp/caf\".u\"00E9\"");
    pathRoundTrip(Path.of("/a\nb"), "UStr:(\"/a\" | \"b\").u");
    pathRoundTrip(PathBytes.path(new byte[]{'/', 'c', 'a', 'f', (byte)0xE9}), "Base16:2F636166E9");
  }
  @Test void aPathReadsBackThroughItsUnitsWhateverTheTag(){
    Assumptions.assumeTrue(Fs.isLinux());
    assertEquals(Path.of("/caf\u00e9"), readPath("Base16:2F636166C3A9"));
    assertEquals(Path.of("/caf\u00e9"), readPath("UStr:\"/caf\".u\"E9\""));
    assertEquals(PathBytes.path(new byte[]{(byte)0xFF}), readPath("Base16:FF"));
  }
  @Test void anyFormReadsBackAsTheSameText(){
    assertEquals("abc", read("UStr:\"abc\".u"));
    assertEquals("abc", read("UStr:\"a\" + `b` + (\"c\")"));
    assertEquals("\u00e9x", read("UStr:\"\".u \"E9\"+\"x\".u"));
    assertEquals(Path.of("ab"), readPath(Fs.isWindows() ? "Base16:61006200" : "Base16:6162"));
  }
  @Test void aTaggedTextIsAStringWithItsQuotesEscaped(){
    var s= new Info.Str(TaggedText.of("C:/caf\u00e9"), Info.noSpan);
    assertEquals("\"UStr:\\\"C:/caf\\\".u\\\"00E9\\\"\"\n", Info.print(s));
    assertEquals("C:/caf\u00e9", read(((Info.Str)Info.parse(Info.print(s), URI.create("test:projects.info"))).value()));
  }
  @Test void aTextWithoutATagIsRejected(){
    err("\"C:/data\" is malformed: it starts with \"Str:\", \"UStr:\" or \"Base16:\", then the text written that way.", () -> read("C:/data"));
  }
  @Test void aMalformedUStrIsRejected(){
    err("\"UStr:\"\".u\"e9\"\" is malformed: code points, each as 1 to 6 uppercase hex digits, separated by single spaces, was expected at offset 8 after \"UStr:\".", () -> read("UStr:\"\".u\"e9\""));
    err("[###]Unicode scalars (no D800 to DFFF, nothing above 10FFFF) was expected[###]", () -> read("UStr:\"\".u\"D800\""));
    err("[###]the end of the text was expected at offset 3[###]", () -> read("UStr:\"a\".size"));
    err("[###]the closing \" was expected[###]", () -> read("UStr:\"a"));
  }
  @Test void aCharacterOutsideTheSetAfterStrOrUStrIsRejected(){
    err("\"Str:/tmp/caf\" [U+00E9] is malformed: after \"Str:\" every character is in the Fearless character set, but [U+00E9] is not.", () -> read("Str:/tmp/caf\u00e9"));
    err("[###] is malformed: after \"UStr:\" every character is in the Fearless character set, but [U+00E9] is not.", () -> read("UStr:\"caf\u00e9\".u"));
  }
  @Test void parenthesesInUStrNestAtMost100Deep(){
    assertEquals("a", read("UStr:"+"(".repeat(100)+"\"a\""+")".repeat(100)));
    err("A \"UStr:\" text is malformed: its parentheses nest at most 100 deep, and the one at offset 100 after \"UStr:\" is deeper.", () -> read("UStr:"+"(".repeat(100_000)));
  }
  @Test void aMalformedBase16IsRejected(){
    err("\"Base16:4\" is malformed: after \"Base16:\" a path is the bytes naming it, each as 2 uppercase hex digits.", () -> readPath("Base16:4"));
    err("[###]each as 2 uppercase hex digits.", () -> readPath("Base16:4a00"));
    if (Fs.isWindows()){ err("\"Base16:430000\" is malformed: a Windows name is 16 bit units, so its bytes are an even number.", () -> readPath("Base16:430000")); return; }
    assertThrows(InvalidPathException.class, () -> readPath("Base16:2F00"));
  }
}

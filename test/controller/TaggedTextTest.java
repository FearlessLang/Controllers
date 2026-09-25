package controller;

import static controller.Errs.err;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import tools.Fs;

final class TaggedTextTest{
  private static String read(String text){ return TaggedText.read(text,Messages::infoError); }
  private static void roundTrip(String text, String tagged){
    assertEquals(tagged,TaggedText.of(text));
    assertEquals(text,read(tagged));
  }
  @Test void aTextInTheCharacterSetIsStr(){
    roundTrip("C:\\data\\hello","Str:C:\\data\\hello");
    roundTrip("a \"b\"\n","Str:a \"b\"\n");
    roundTrip("","Str:");
  }
  @Test void aUnicodeTextIsUStrAsUStrEscapePrintsIt(){
    roundTrip("\u00e9","UStr:\"\".u\"00E9\"");
    roundTrip("C:/data/caf\u00e9/hello","UStr:\"C:/data/caf\".u\"00E9\"+(\"/hello\".u)");
    roundTrip("a\ud83d\ude00b\u00e9c","UStr:\"a\".u\"1F600\"+(\"b\".u\"00E9\")+(\"c\".u)");
    roundTrip("e\u0301\u00e9\t\ud83d\ude00","UStr:\"e\".u\"0301 00E9 0009 1F600\"");
    roundTrip("\u00e9\na\"b","UStr:\"\".u\"00E9\"+((\"\" | `a\"b`).u)");
    roundTrip("C:\\data\u00e9 \ud83d\ude00\\x","UStr:\"C:\\data\".u\"00E9\"+(\" \".u\"1F600\")+(\"\\x\".u)");
  }
  @Test void aPathThatIsNotUnicodeIsBase16OfTheUtf16LittleEndianUnitsOfWindows(){
    Assumptions.assumeTrue(Fs.isWindows());
    roundTrip("C:/a\ud800","Base16:43003A002F00610000D8");
  }
  @Test void anyFormReadsBackAsTheSameText(){
    assertEquals("abc",read("UStr:\"abc\".u"));
    assertEquals("abc",read("UStr:\"a\" + `b` + (\"c\")"));
    assertEquals("\u00e9x",read("UStr:\"\".u \"E9\"+\"x\".u"));
    Assumptions.assumeTrue(Fs.isWindows());
    assertEquals("ab",read("Base16:61006200"));
  }
  @Test void aTextWithoutATagIsRejected(){
    err("\"C:/data\" is malformed: it starts with \"Str:\", \"UStr:\" or \"Base16:\", then the text written that way.",()->read("C:/data"));
  }
  @Test void aMalformedUStrIsRejected(){
    err("\"UStr:\"\".u\"e9\"\" is malformed: code points, each as 1 to 6 uppercase hex digits, separated by single spaces, was expected at offset 8 after \"UStr:\".",()->read("UStr:\"\".u\"e9\""));
    err("[###]Unicode scalars (no D800 to DFFF, nothing above 10FFFF) was expected[###]",()->read("UStr:\"\".u\"D800\""));
    err("[###]the end of the text was expected at offset 3[###]",()->read("UStr:\"a\".size"));
    err("[###]the closing \" was expected[###]",()->read("UStr:\"a"));
  }
  @Test void aMalformedBase16IsRejected(){
    err("\"Base16:4\" is malformed: after \"Base16:\" a path is the bytes naming it, each as 2 uppercase hex digits.",()->read("Base16:4"));
    err("[###]each as 2 uppercase hex digits.",()->read("Base16:4a00"));
    Assumptions.assumeTrue(Fs.isWindows());
    err("\"Base16:430000\" is malformed: a Windows name is 16 bit units, so its bytes are an even number.",()->read("Base16:430000"));
  }
}

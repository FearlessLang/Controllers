package controller;

import static controller.Errs.err;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.Test;

final class InfoTest{
  private static final URI uri= URI.create("test:projects.info");
  private static Info parse(String text){ return Info.parse(text,uri); }
  @Test void anEmptyObjectParsesToNoFields(){
    assertEquals(List.of(),((Info.Obj)parse("{}")).fields());
  }
  @Test void aStringParsesToItsValue(){
    assertEquals("hello",((Info.Str)parse("\"hello\"")).value());
  }
  @Test void escapesRoundTrip(){
    var s= (Info.Str)parse("\"a\\\"b\\\\c\\nd\"");
    assertEquals("a\"b\\c\nd",s.value());
    assertEquals("\"a\\\"b\\\\c\\nd\"",Info.print(s).strip());
  }
  @Test void nestedListsAndObjectsParse(){
    var obj= (Info.Obj)parse("{\"a\":[\"x\",\"y\"],\"b\":{}}");
    assertEquals(2,obj.fields().size());
    assertEquals(List.of("x","y"),((Info.Lst)obj.field("a").orElseThrow().value()).items().stream().map(i->((Info.Str)i).value()).toList());
    assertTrue(((Info.Obj)obj.field("b").orElseThrow().value()).fields().isEmpty());
  }
  @Test void lineCommentsAreSkipped(){
    var obj= (Info.Obj)parse("{\n  \"a\": \"x\" // a comment\n}");
    assertEquals("x",((Info.Str)obj.field("a").orElseThrow().value()).value());
  }
  @Test void wholeWhitespaceAndCommentsRoundTripToTheSameStructure(){
    var a= parse("{\"a\":\"1\",\"b\":[\"2\",\"3\"]}");
    var b= parse("{\n  \"a\" : \"1\" ,\n  \"b\" : [ \"2\" , \"3\" ]\n}\n// trailing comment is not even reached\n");
    assertEquals(Info.print(a),Info.print(b));
  }
  @Test void anUnclosedStringPointsAtWhereItStarted(){
    err("[###]never closed with a matching \".[###]",()->parse("{\"a\": \"never closed"));
  }
  @Test void aMissingColonIsReported(){
    err("[###]Expected ':' after the key \"a\".[###]",()->parse("{\"a\" \"x\"}"));
  }
  @Test void aDuplicateKeyIsReported(){
    err("[###]Duplicate key \"a\": this object already has this key.[###]",()->parse("{\"a\":\"1\",\"a\":\"2\"}"));
  }
  @Test void aRawNewlineInAStringIsRejected(){
    err("[###]cannot contain a raw newline; write \\n instead.[###]",()->parse("\"a\nb\""));
  }
  @Test void anUnsafeCharacterIsRejected(){
    err("[###]outside the safe character set of Fearless[###]",()->parse("\"a\tb\""));
  }
  private static void roundTrip(String value, String printed){
    var s= new Info.Str(value,Info.noSpan);
    assertEquals(printed,Info.print(s));
    assertEquals(value,((Info.Str)parse(printed)).value());
  }
  @Test void aCharacterOutsideTheSetRoundTripsAsCodePoints(){
    roundTrip("C:/data/caf\u00e9/hello","\"C:/data/caf\\u(00E9)/hello\"\n");
  }
  @Test void aSupplementaryCharacterIsOneCodePoint(){
    roundTrip("a\ud83d\ude00b","\"a\\u(1F600)b\"\n");
  }
  @Test void aRunOfCharactersOutsideTheSetIsOneEscape(){
    roundTrip("e\u0301\u00e9\t\ud83d\ude00\"\\\n\u00e9","\"e\\u(0301 00E9 0009 1F600)\\\"\\\\\\n\\u(00E9)\"\n");
  }
  @Test void codePointsMayHaveLeadingZeros(){
    assertEquals("\u00e9\u0301",((Info.Str)parse("\"\\u(00E9 000301)\"")).value());
  }
  @Test void lowercaseCodePointsAreRejected(){
    err("""
      [###]The escape "\\u(e9)" is malformed: inside \\u(...) write one or more code points, each as 1 to 6 uppercase hex digits, separated by single spaces, like \\u(00E9 0301).[###]""",()->parse("\"\\u(e9)\""));
  }
  @Test void aCodePointOfSevenDigitsIsRejected(){
    err("[###]The escape \"\\u(00000E9)\" is malformed[###]",()->parse("\"\\u(00000E9)\""));
  }
  @Test void anEmptyEscapeIsRejected(){
    err("[###]The escape \"\\u()\" is malformed[###]",()->parse("\"\\u()\""));
  }
  @Test void twoSpacesAreRejected(){
    err("[###]The escape \"\\u(E9  301)\" is malformed[###]",()->parse("\"\\u(E9  301)\""));
  }
  @Test void aSurrogateIsRejected(){
    err("""
      [###]The escape "\\u(E9 D83D)" holds D83D, which is not a Unicode scalar: code points from D800 to DFFF (surrogates) and above 10FFFF are not characters.[###]""",()->parse("\"\\u(E9 D83D)\""));
  }
  @Test void aCodePointAboveTheLastIsRejected(){
    err("[###]holds 110000, which is not a Unicode scalar[###]",()->parse("\"\\u(110000)\""));
  }
  @Test void aMissingOpenParenIsRejected(){
    err("[###]The escape \\u needs its code points in parentheses, like \\u(00E9 0301).[###]",()->parse("\"\\uE9\""));
  }
  @Test void aMissingCloseParenIsRejected(){
    err("[###]The escape \"\\u(E9\" is never closed with a matching ).[###]",()->parse("\"\\u(E9\""));
  }
  @Test void trailingJunkIsRejected(){
    err("[###]Unexpected extra text after the end of the value[###]",()->parse("\"a\" \"b\""));
  }
  @Test void anUnclosedObjectIsReported(){
    err("[###]never closed with a matching }.[###]",()->parse("{\"a\":\"1\""));
  }
  @Test void anUnclosedListIsReported(){
    err("[###]never closed with a matching ].[###]",()->parse("[\"a\""));
  }
}
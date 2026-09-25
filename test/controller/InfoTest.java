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
    err("[###]A string cannot hold a raw newline: \"a\" | \"b\" is \"a\", a newline, then \"b\".[###]",()->parse("\"a\nb\""));
  }
  @Test void anUnsafeCharacterIsRejected(){
    err("[###]outside the safe character set of Fearless[###]",()->parse("\"a\tb\""));
  }
  private static void roundTrip(String value, String printed){
    var s= new Info.Str(value,Info.noSpan);
    assertEquals(printed+"\n",Info.print(s));
    assertEquals(value,((Info.Str)parse(printed)).value());
  }
  @Test void aStringIsWrittenAsFearlessWritesAStr(){
    roundTrip("","\"\"");
    roundTrip("abc","\"abc\"");
    roundTrip("C:\\data\\hello","\"C:\\data\\hello\"");
    roundTrip("a\"b","`a\"b`");
    roundTrip("a\"b`c","`a\"b`+\"`c\"");
    roundTrip("a\nb","\"a\" | \"b\"");
    roundTrip("a\n\nb","\"a\"| | \"b\"");
    roundTrip("a\n","\"a\"|");
    roundTrip("\n","\"\"|");
  }
  @Test void aStringOutsideTheSetIsWrittenAsFearlessWritesAUStr(){
    roundTrip("\u00e9","\"\".u\"00E9\"");
    roundTrip("C:/data/caf\u00e9/hello","\"C:/data/caf\".u\"00E9\"+(\"/hello\".u)");
    roundTrip("a\ud83d\ude00b\u00e9c","\"a\".u\"1F600\"+(\"b\".u\"00E9\")+(\"c\".u)");
    roundTrip("e\u0301\u00e9\t\ud83d\ude00","\"e\".u\"0301 00E9 0009 1F600\"");
    roundTrip("\u00e9\na\"b","\"\".u\"00E9\"+((\"\" | `a\"b`).u)");
  }
  @Test void anyFearlessStringExpressionOfThatShapeIsRead(){
    assertEquals("ab\"c\nd\"",((Info.Str)parse("(\"a\" + `b`) ^ \"c\" | \"d\" ^")).value());
    assertEquals("\u00e9\u0301x",((Info.Str)parse("\"\".u(\"E9 000301\") + \"x\".u")).value());
    assertEquals("\u00e9x",((Info.Str)parse("\"\".u \"E9\"+\"x\".u")).value());
    assertEquals("",((Info.Str)parse("\"\".u\"\"")).value());
  }
  @Test void lowercaseCodePointsAreRejected(){
    err("""
      [###]The code points "e9" of .u"..." are malformed: write one or more code points, each as 1 to 6 uppercase hex digits, separated by single spaces, like .u"00E9 0301".[###]""",()->parse("\"\".u\"e9\""));
  }
  @Test void aCodePointOfSevenDigitsIsRejected(){
    err("[###]The code points \"00000E9\" of .u\"...\" are malformed[###]",()->parse("\"\".u\"00000E9\""));
  }
  @Test void twoSpacesAreRejected(){
    err("[###]The code points \"E9  301\" of .u\"...\" are malformed[###]",()->parse("\"\".u\"E9  301\""));
  }
  @Test void aSurrogateIsRejected(){
    err("""
      [###]The code points "E9 D83D" of .u"..." hold D83D, which is not a Unicode scalar: code points from D800 to DFFF (surrogates) and above 10FFFF are not characters.[###]""",()->parse("\"\".u\"E9 D83D\""));
  }
  @Test void aCodePointAboveTheLastIsRejected(){
    err("[###]hold 110000, which is not a Unicode scalar[###]",()->parse("\"\".u\"110000\""));
  }
  @Test void aMethodOtherThanUIsRejected(){
    err("[###]After a string, only .u and .u\"...\" are allowed[###]",()->parse("\"a\".size"));
  }
  @Test void aMissingCloseParenIsRejected(){
    err("[###]Expected ')' here, to close the parenthesis.[###]",()->parse("\"\".u(\"E9\""));
  }
  @Test void aKeyIsOneQuotedLiteral(){
    err("[###]Expected a quoted key \"...\" here.[###]",()->parse("{`a`: \"1\"}"));
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
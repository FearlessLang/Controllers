package suggest;

import static suggest.DocsTest.same;

import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

final class TokensTest{
  static String tokens(String text){ return Tokens.tokens(text).stream().filter(t->t.kind() != Tokens.Kind.Ws).map(t->t.kind()+"|"+t.text()).collect(Collectors.joining(" ")); }
  @Test void aSquareOpensGenericsRightAfterANameOrAnOperatorOnly(){
    same("UppercaseId|Foo OSquareArg|[ UppercaseId|X CSquare|]",tokens("Foo[X]"));
    same("UppercaseId|Foo BadOSquare|[ UppercaseId|X CSquare|]",tokens("Foo [X]"));
    same("DotName|.m OSquareArg|[ RCap|mut CSquare|]",tokens(".m[mut]"));
  }
  @Test void theKindsFrontendRejectsAreTokensToo(){
    same("BadUppercaseId|con.Foo",tokens("con.Foo"));
    same("UppercaseId|cons.Foo",tokens("cons.Foo"));
    same("BadSStrQuote|'a'",tokens("'a'"));
    same("LowercaseId|x BadUnopenedBlockCommentClose|*/",tokens("x */"));
    same("BadUStrUnclosed|\"ab",tokens("\"ab"));
    same("BadUnclosedBlockComment|/* a",tokens("/* a"));
  }
  @Test void longestMatchTiesToTheEarlierKind(){
    same("ReadImm|read/imm UppercaseId|X",tokens("read/imm X"));
    same("SignedInt|+1 UnSignedFloat|2.5 Op|+ SignedFloat|-1.0e3soft",tokens("+1 2.5 + -1.0e3soft"));
    same("CCurlyId|}x LineComment|// y",tokens("}x // y"));
  }
  @Test void groupsNestAndAnUnclosedOneRunsToTheEnd(){
    var root= Tokens.group(Tokens.tokens("a.b(c, {d -> e[F] ) g"));
    same("[LowercaseId|a, DotName|.b, ORound, LowercaseId|g]",root.items.stream().map(TokensTest::show).toList().toString());
    var round= (Tokens.Group)root.items.get(2);
    same("[LowercaseId|c, Comma|,, OCurly]",round.items.stream().map(TokensTest::show).toList().toString());
    var unclosed= (Tokens.Group)Tokens.group(Tokens.tokens("({ x")).items.get(0);
    same("[OCurly]",unclosed.items.stream().map(TokensTest::show).toList().toString());
    same("2147483647",""+unclosed.end);
  }
  static String show(Tokens.Item it){ return it instanceof Tokens.Tok t ? t.kind()+"|"+t.text() : ((Tokens.Group)it).open.toString(); }
}

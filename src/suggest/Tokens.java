package suggest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/// The token kinds of fearlessParser.TokenKind that the chain parser needs, matched the same
/// way: longest match, ties to the earlier kind. Whitespace, comments and unclosed strings or
/// comments are dropped, and so is any character no kind accepts, so a buffer being typed always
/// tokenizes. Brackets nest the tokens into groups; an unclosed group runs to the end of the
/// buffer, a stray closer is ignored, and }id closes a curly group.
final class Tokens{
  enum Kind{
    Ws("\\s+",true), LineComment("//[^\\n]*",true), BlockComment("/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*/",true),
    UnclosedBlockComment("(?s)/\\*(?!.*?\\*/).*",true),
    Arrow("->"), ORound("\\("), CRound("\\)"), OCurly("\\{"), CCurlyId("\\}[A-Za-z0-9_]+'*"), CCurly("\\}"),
    OSquare("\\["), CSquare("\\]"), Underscore("_"), Comma(","), SemiColon(";"), ColonColon("::"), Colon(":"),
    Eq("="), SQuote("'"), ReadImm("read/imm"), RCap("readH|mutH|imm|iso|read|mut"),
    SignedFloat("[+-][0-9](?:[0-9_]*[0-9])?\\.[0-9](?:[0-9_]*[0-9])?(?:[eE][+-]?[0-9](?:[0-9_]*[0-9])?)?(?:soft)?"),
    UnsignedFloat("[0-9](?:[0-9_]*[0-9])?\\.[0-9](?:[0-9_]*[0-9])?(?:[eE][+-]?[0-9](?:[0-9_]*[0-9])?)?(?:soft)?"),
    SignedInt("[+-][0-9](?:[0-9_]*[0-9])?"), UnsignedInt("[0-9](?:[0-9_]*[0-9])?"),
    UnclosedUStr("\"[^\"\\n]*(?=\\n|\\z)",true), UnclosedSStr("`[^`\\n]*(?=\\n|\\z)",true),
    UStr("\"[^\"\\n]*\""), SStr("`[^`\\n]*`"),
    DotName("\\._*[a-z][A-Za-z0-9_]*'*"),
    UppercaseId("(?:[a-z][a-z0-9_]*\\.)?_*[A-Z][A-Za-z0-9_]*'*"),
    LowercaseId("_*[a-z][A-Za-z0-9_]*'*"),
    Op("(?:(?!/\\*|\\*/|//)[\\\\/#*\\-+%<>=!&^~?|])+");
    final Pattern pattern;
    final boolean hidden;
    Kind(String regex){ this(regex,false); }
    Kind(String regex, boolean hidden){ this.pattern= Pattern.compile(regex); this.hidden= hidden; }
  }
  sealed interface Item permits Tok, Group{ int start(); }
  record Tok(Kind kind, String text, int start, int end) implements Item{}
  static final class Group implements Item{
    final Kind open;
    final int start;
    final Group parent;
    final ArrayList<Item> items= new ArrayList<>();
    int end= Integer.MAX_VALUE;
    Group(Kind open, int start, Group parent){ this.open= open; this.start= start; this.parent= parent; }
    @Override public int start(){ return start; }
  }
  static Group group(String text){
    var matchers= Arrays.stream(Kind.values()).map(k->k.pattern.matcher(text)).toList();
    var root= new Group(null, 0, null);
    var cur= root;
    for (int i= 0; i < text.length();){
      var best= Kind.Ws;
      var end= i;
      for (var k : Kind.values()){
        var m= matchers.get(k.ordinal()).region(i, text.length());
        if (m.lookingAt() && m.end() > end){ best= k; end= m.end(); }
      }
      if (end == i){ i+= 1; continue; }
      var tok= new Tok(best, text.substring(i, end), i, end);
      i= end;
      if (best.hidden){ continue; }
      switch (best){
        case OCurly, ORound, OSquare -> { var g= new Group(best, tok.start, cur); cur.items.add(g); cur= g; }
        case CCurly, CCurlyId, CRound, CSquare -> cur= close(cur, tok);
        default -> cur.items.add(tok);
      }
    }
    return root;
  }
  private static Group close(Group cur, Tok closer){
    var open= switch (closer.kind){ case CRound -> Kind.ORound; case CSquare -> Kind.OSquare; default -> Kind.OCurly; };
    var g= cur;
    while (g.parent != null && g.open != open){ g= g.parent; }
    if (g.parent == null){ return cur; }
    for (var h= cur; h != g; h= h.parent){ h.end= closer.start; }
    g.end= closer.end;
    return g.parent;
  }
  static Group innermost(Group g, int pos){
    for (var it : g.items){
      if (it instanceof Group c && c.start < pos && pos < c.end){ return innermost(c, pos); }
    }
    return g;
  }
  static boolean is(Item it, Kind... kinds){ return it instanceof Tok t && List.of(kinds).contains(t.kind); }
  static boolean isGroup(Item it, Kind open){ return it instanceof Group g && g.open == open; }
}

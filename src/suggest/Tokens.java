package suggest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/// The tokens of fearlessParser.TokenKind: the same kinds, in the same order, with the same
/// regular expressions, matched the same way: longest match, ties to the earlier kind.
/// Whitespace, comments, the kinds Frontend rejects (Bad...) and a character no kind matches are
/// dropped. Brackets nest the tokens into groups as fearlessParser.Parse does: (..), [..] opened by
/// OSquareArg, {..} closed by } or by }id. The text is still being typed: an unclosed group runs to
/// the end of the text, a closer closes the innermost group it closes and the groups inside it,
/// and a closer closing no group is dropped.
final class Tokens{
  enum Kind{
    Ws("\\s+"), LineComment("//[^\\n]*"), BlockComment("/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*/"),
    BadUnclosedBlockComment("(?s)/\\*(?!.*?\\*/).*"), BadUnopenedBlockCommentClose("\\*/"),
    Arrow("->"), ORound("\\("), CRound("\\)"), OCurly("\\{"), CCurlyId("\\}[A-Za-z0-9_]+'*"), CCurly("\\}"),
    OSquareArg("(?<=[A-Za-z0-9_'`\\x22\\x5C/#\\x2A\\x2D\\x2B%<>=!&\\x5E~\\x3F\\x7C])\\["), BadOSquare("\\["), CSquare("\\]"),
    Underscore("_"), Comma(","), SemiColon(";"), ColonColon("::"), Colon(":"), Eq("="), SQuote("'"),
    ReadImm("read/imm"), RCap("readH|mutH|imm|iso|read|mut"),
    SignedFloat("[+-](?:[0-9](?:[0-9_]*[0-9])?)\\.(?:[0-9](?:[0-9_]*[0-9])?)(?:[eE][+-]?[0-9](?:[0-9_]*[0-9])?)?(?:soft)?"),
    UnSignedFloat("[0-9](?:[0-9_]*[0-9])?\\x2E(?:[0-9](?:[0-9_]*[0-9])?)(?:[eE][\\x2B\\x2D]?[0-9](?:[0-9_]*[0-9])?)?(?:soft)?"),
    SignedInt("[+-][0-9](?:[0-9_]*[0-9])?"), UnsignedInt("[0-9](?:[0-9_]*[0-9])?"),
    BadUStrUnclosed("\\x22[^\\x22\\x0A]*(?=\\x0A|\\z)"), BadSStrUnclosed("`[^`\\x0A]*(?=\\x0A|\\z)"),
    UStr("\\x22[^\\x22\\x0A]*\\x22"), SStr("`[^`\\x0A]*`"),
    DotName("\\._*[a-z][A-Za-z0-9_]*'*"),
    UppercaseId("(?:(?!(?:con|prn|aux|nul)(?![a-z0-9_])|(?:com|lpt)[1-9](?![a-z0-9_]))[a-z][a-z0-9_]*\\x2E)?_*[A-Z][A-Za-z0-9_]*'*"),
    BadUppercaseId("(?:[a-z][a-z0-9_]*\\x2E)?_*[A-Z][A-Za-z0-9_]*'*"),
    LowercaseId("_*[a-z][A-Za-z0-9_]*'*"),
    BadSStrQuote("'[^'\\x0A]*'"),
    Op("(?:(?!/\\x2A|\\x2A/|//)[\\x5C/#\\x2A\\x2D\\x2B%<>=!&\\x5E~\\x3F\\x7C])+");
    final Pattern pattern;
    Kind(String regex){ this.pattern= Pattern.compile(regex); }
    boolean hidden(){ return ordinal() < 3 || name().startsWith("Bad"); }
  }
  static final Kind[] typeName= {Kind.UppercaseId, Kind.SignedFloat, Kind.UnSignedFloat, Kind.SignedInt, Kind.UnsignedInt, Kind.SStr, Kind.UStr};
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
  /// every token of the text, the dropped ones included
  static List<Tok> tokens(String text){
    var matchers= Arrays.stream(Kind.values()).map(k->k.pattern.matcher(text).useTransparentBounds(true)).toList();
    var res= new ArrayList<Tok>();
    for (int i= 0; i < text.length();){
      var best= Kind.Ws;
      var end= i;
      for (var k : Kind.values()){
        var m= matchers.get(k.ordinal()).region(i, text.length());
        if (m.lookingAt() && m.end() > end){ best= k; end= m.end(); }
      }
      if (end == i){ i+= 1; continue; }
      res.add(new Tok(best, text.substring(i, end), i, end));
      i= end;
    }
    return res;
  }
  static Group group(List<Tok> tokens){
    var root= new Group(null, 0, null);
    var cur= root;
    for (var t : tokens){
      if (t.kind.hidden()){ continue; }
      switch (t.kind){
        case OCurly, ORound, OSquareArg -> { var g= new Group(t.kind, t.start, cur); cur.items.add(g); cur= g; }
        case CCurly, CCurlyId, CRound, CSquare -> cur= close(cur, t);
        default -> cur.items.add(t);
      }
    }
    return root;
  }
  private static Group close(Group cur, Tok closer){
    var open= switch (closer.kind){ case CRound -> Kind.ORound; case CSquare -> Kind.OSquareArg; default -> Kind.OCurly; };
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
  static String text(Item it){ return ((Tok)it).text; }
}

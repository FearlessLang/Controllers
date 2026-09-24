package suggest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import suggest.Api.Ty;
import suggest.Tokens.Group;
import suggest.Tokens.Item;
import suggest.Tokens.Kind;
import suggest.Tokens.Tok;

/// The grammar of fearlessParser.Parser that typing needs: an atom then posts, left to right,
/// a post without parenthesis taking one atom; the methods of a literal; the header of a
/// declaration. Nothing is checked: a missing atom and what the grammar rejects are unknown, and
/// a call's arity is what was typed.
final class Chain{
  sealed interface E permits Atom, Call, Sugar, Known{}
  /// nothing (unknown), a name, ::, a group, or [RC] T, T[..], T{..}, T[..]{..} for a type name T
  record Atom(List<Item> items) implements E{}
  /// targs are the items of the [..] after the name, when there is one
  record Call(E recv, String name, List<E> args, List<Item> targs) implements E{ int arity(){ return args.size(); } }
  /// recv.m x = thunk rest, that is recv.m(thunk, {x, k -> k rest}); x is "_" for a pattern
  record Sugar(Call call, String x, List<Item> rest) implements E{}
  /// a receiver of known type: what the rest of a sugar is applied to
  record Known(Ty t) implements E{}
  static final Atom unknown= new Atom(List.of());
  private final List<Item> items;
  private int i;
  private Chain(List<Item> items){ this.items= items; }
  static E parse(List<Item> items){
    var c= new Chain(items);
    return c.posts(c.atom());
  }
  static E parse(E recv, List<Item> posts){ return new Chain(posts).posts(recv); }
  private E posts(E e){
    while (i < items.size()){ e= post(e); }
    return e;
  }
  private boolean peek(Kind... kinds){ return i < items.size() && Tokens.is(items.get(i), kinds); }
  private boolean peekGroup(Kind open){ return i < items.size() && Tokens.isGroup(items.get(i), open); }
  private E atom(){
    var rc= peek(Kind.RCap);
    if (rc){ i+= 1; }
    if (i >= items.size() || rc && !peek(Tokens.typeName)){ return unknown; }
    var res= new ArrayList<Item>(List.of(items.get(i++)));
    if (!Tokens.is(res.get(0), Tokens.typeName)){ return new Atom(res); }
    if (peekGroup(Kind.OSquareArg)){ res.add(items.get(i++)); }
    if (peekGroup(Kind.OCurly)){ res.add(items.get(i++)); }
    return new Atom(List.copyOf(res));
  }
  private E post(E recv){
    var it= items.get(i++);
    if (Tokens.is(it, Kind.SignedInt, Kind.SignedFloat)){
      var t= (Tok)it;
      var num= new Tok(t.kind() == Kind.SignedInt ? Kind.UnsignedInt : Kind.UnSignedFloat, t.text().substring(1), t.start()+1, t.end());
      return new Call(recv, t.text().substring(0,1), List.of(new Atom(List.of(num))), List.of());
    }
    if (!Tokens.is(it, Kind.DotName, Kind.Op)){ return unknown; }
    var name= Tokens.text(it);
    var targs= peekGroup(Kind.OSquareArg) ? List.copyOf(((Group)items.get(i++)).items) : List.<Item>of();
    if (peekGroup(Kind.ORound)){ return new Call(recv, name, split(((Group)items.get(i++)).items, Kind.Comma).stream().map(Chain::parse).toList(), targs); }
    var sugar= (peek(Kind.LowercaseId) || peekGroup(Kind.OCurly)) && i+1 < items.size() && Tokens.is(items.get(i+1), Kind.Eq);
    if (sugar){
      var x= peek(Kind.LowercaseId) ? Tokens.text(items.get(i)) : "_";
      i+= 2;
      var thunk= atom();
      var rest= List.copyOf(items.subList(i, items.size()));
      i= items.size();
      return new Sugar(new Call(recv, name, List.of(thunk, unknown), targs), x, rest);
    }
    if (i >= items.size() || peek(Kind.DotName, Kind.Op, Kind.Colon)){ return new Call(recv, name, List.of(), targs); }
    return new Call(recv, name, List.of(atom()), targs);
  }
  static List<List<Item>> split(List<Item> items, Kind sep){
    var res= new ArrayList<List<Item>>();
    var cur= new ArrayList<Item>();
    for (var it : items){
      if (!Tokens.is(it, sep)){ cur.add(it); continue; }
      res.add(cur);
      cur= new ArrayList<>();
    }
    res.add(cur);
    return items.isEmpty() ? List.of() : res;
  }
  /// a parameter: its name, "_" for a pattern or a type alone, and its type when written
  record Param(String x, List<Item> type){}
  /// a method of a literal: [RC] [name] [Xs] params [: result] -> body, where params are (..) or
  /// written without parenthesis, and then a colon types the last parameter, not the result;
  /// a lambda has no name; start and end are the offsets of its text
  record Meth(Optional<String> name, List<String> xs, List<Param> params, List<Item> result, List<Item> body, int start, int end){}
  static List<Meth> methodsOf(Group g){
    var res= new ArrayList<Meth>();
    var cur= new ArrayList<Item>();
    var start= g.start;
    for (int j= selfName(g).isPresent() ? 2 : 0; j <= g.items.size(); j+= 1){
      var last= j == g.items.size();
      if (!last && !Tokens.is(g.items.get(j), Kind.SemiColon)){ cur.add(g.items.get(j)); continue; }
      if (!cur.isEmpty()){ res.add(meth(cur, start, last ? g.end : g.items.get(j).start())); }
      cur= new ArrayList<>();
      if (!last){ start= ((Tok)g.items.get(j)).end(); }
    }
    return res;
  }
  private static Meth meth(List<Item> cur, int start, int end){
    var arrow= cur.stream().filter(it->Tokens.is(it, Kind.Arrow)).findFirst().map(cur::indexOf);
    var rc= Tokens.is(cur.get(0), Kind.RCap) ? 1 : 0;
    var sig= arrow.isPresent() || rc < cur.size() && Tokens.is(cur.get(rc), Kind.DotName, Kind.Op);
    var head= !sig ? List.<Item>of() : cur.subList(0, arrow.orElse(cur.size()));
    var body= !sig ? cur : cur.subList(arrow.map(a->a+1).orElse(cur.size()), cur.size());
    var k= head.isEmpty() ? 0 : rc;
    var name= k < head.size() && Tokens.is(head.get(k), Kind.DotName, Kind.Op) ? Optional.of(Tokens.text(head.get(k++))) : Optional.<String>empty();
    var xs= k < head.size() && Tokens.isGroup(head.get(k), Kind.OSquareArg) ? generics(head.get(k++)) : List.<String>of();
    var round= k < head.size() && Tokens.isGroup(head.get(k), Kind.ORound);
    var colon= round ? k+1 : k;
    var typed= colon < head.size() && Tokens.is(head.get(colon), Kind.Colon);
    var ps= round ? ((Group)head.get(k)).items : typed ? List.<Item>of() : head.subList(k, head.size());
    var params= new ArrayList<>(split(ps, Kind.Comma).stream().map(Chain::param).toList());
    if (hasImplicit(body)){ params.add(new Param("::", List.of())); }
    var result= typed ? head.subList(colon+1, head.size()) : List.<Item>of();
    return new Meth(name, xs, List.copyOf(params), List.copyOf(result), List.copyOf(body), start, end);
  }
  /// the names of the generics declared in a [..] group
  private static List<String> generics(Item square){
    return split(((Group)square).items, Kind.Comma).stream().filter(a->!a.isEmpty() && Tokens.is(a.get(0), Kind.UppercaseId)).map(a->Tokens.text(a.get(0))).toList();
  }
  private static Param param(List<Item> p){
    var named= !p.isEmpty() && (Tokens.is(p.get(0), Kind.LowercaseId, Kind.Underscore) || Tokens.isGroup(p.get(0), Kind.OCurly));
    if (!named){ return new Param("_", p); }
    var type= p.size() > 1 && Tokens.is(p.get(1), Kind.Colon) ? p.subList(2, p.size()) : List.<Item>of();
    return new Param(Tokens.is(p.get(0), Kind.LowercaseId) ? Tokens.text(p.get(0)) : "_", type);
  }
  private static boolean hasImplicit(List<Item> items){
    return items.stream().anyMatch(it->it instanceof Group g ? g.open != Kind.OCurly && hasImplicit(g.items) : Tokens.is(it, Kind.ColonColon));
  }
  static Optional<String> selfName(Group g){
    var self= g.items.size() >= 2 && Tokens.is(g.items.get(0), Kind.SQuote) && Tokens.is(g.items.get(1), Kind.LowercaseId);
    return self ? Optional.of(Tokens.text(g.items.get(1))) : Optional.empty();
  }
  /// Name[Xs]: C1, .., Cn {..}: the name, the generics and the supertypes of a declaration body
  record Header(String name, List<String> xs, List<List<Item>> supers){}
  static Optional<Header> header(Group g){
    var items= g.parent.items;
    int j= items.indexOf(g)-1;
    for (; j >= 0 && !Tokens.is(items.get(j), Kind.Colon); j-= 1){
      if (!Tokens.is(items.get(j), Kind.UppercaseId, Kind.Comma) && !Tokens.isGroup(items.get(j), Kind.OSquareArg)){ return Optional.empty(); }
    }
    var supers= split(items.subList(j+1, items.indexOf(g)), Kind.Comma);
    var square= j >= 1 && Tokens.isGroup(items.get(j-1), Kind.OSquareArg);
    var n= square ? j-2 : j-1;
    if (n < 0 || !Tokens.is(items.get(n), Kind.UppercaseId)){ return Optional.empty(); }
    var xs= square ? generics(items.get(j-1)) : List.<String>of();
    return Optional.of(new Header(Tokens.text(items.get(n)), xs, supers));
  }
}

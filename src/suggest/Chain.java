package suggest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import suggest.Api.Ty;
import suggest.Tokens.Group;
import suggest.Tokens.Item;
import suggest.Tokens.Kind;
import suggest.Tokens.Tok;

/// The chain grammar of fearlessParser.Parser, left to right with single atom arguments and
/// nothing checked: a missing atom is unknown, an unknown post ends the chain, a call's arity
/// is what was typed. Also the shape of a literal: its methods split at ';', each with its head
/// and body; the header of a declaration in the enclosing group; the name after 'self.
final class Chain{
  sealed interface E permits Atom, Call, Sugar, Known{}
  /// nothing (unknown), a token, a group, or a typed literal: T, T[..], T{..}, T[..]{..}
  record Atom(List<Item> items) implements E{}
  /// targs are the items of the [..] after the name, when there is one
  record Call(E recv, String name, List<E> args, List<Item> targs) implements E{ int arity(){ return args.size(); } }
  /// recv.m x = thunk rest: the call has the thunk and an unknown second argument; rest is the
  /// chain the continuation's second parameter receives; x is "_" for a pattern
  record Sugar(Call call, String x, List<Item> rest) implements E{}
  /// a receiver of known type: what the rest of a sugar chain is applied to
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
    if (i >= items.size()){ return unknown; }
    var it= items.get(i++);
    if (Tokens.is(it, Kind.RCap)){ return atom(); }
    var res= new ArrayList<Item>(List.of(it));
    if (Tokens.is(it, Kind.UppercaseId)){
      if (peekGroup(Kind.OSquare)){ res.add(items.get(i++)); }
      if (peekGroup(Kind.OCurly)){ res.add(items.get(i++)); }
    }
    return new Atom(List.copyOf(res));
  }
  private E post(E recv){
    var it= items.get(i++);
    if (Tokens.is(it, Kind.SignedInt, Kind.SignedFloat)){
      var t= (Tok)it;
      var num= new Tok(t.kind() == Kind.SignedInt ? Kind.UnsignedInt : Kind.UnsignedFloat, t.text().substring(1), t.start()+1, t.end());
      return new Call(recv, t.text().substring(0,1), List.of(new Atom(List.of(num))), List.of());
    }
    if (!Tokens.is(it, Kind.DotName, Kind.Op)){ return new Atom(List.of(it)); }
    var name= ((Tok)it).text();
    var targs= peekGroup(Kind.OSquare) ? List.copyOf(((Group)items.get(i++)).items) : List.<Item>of();
    if (peekGroup(Kind.ORound)){
      var g= (Group)items.get(i++);
      return new Call(recv, name, split(g.items, Kind.Comma).stream().map(Chain::parse).toList(), targs);
    }
    var eq= i+1 < items.size() && binder(items.get(i)) && Tokens.is(items.get(i+1), Kind.Eq);
    if (eq){
      var x= items.get(i) instanceof Tok b ? b.text() : "_";
      i+= 2;
      var thunk= atom();
      var rest= List.copyOf(items.subList(i, items.size()));
      i= items.size();
      return new Sugar(new Call(recv, name, List.of(thunk, unknown), targs), x, rest);
    }
    if (i >= items.size() || peek(Kind.DotName, Kind.Op, Kind.Colon)){ return new Call(recv, name, List.of(), targs); }
    return new Call(recv, name, List.of(atom()), targs);
  }
  private static boolean binder(Item it){ return Tokens.is(it, Kind.LowercaseId, Kind.Underscore) || Tokens.isGroup(it, Kind.OCurly); }
  static List<List<Item>> split(List<Item> items, Kind sep){
    var res= new ArrayList<List<Item>>();
    var cur= new ArrayList<Item>();
    for (var it : items){
      if (!Tokens.is(it, sep)){ cur.add(it); continue; }
      res.add(cur);
      cur= new ArrayList<>();
    }
    res.add(cur);
    return res.stream().filter(l->!l.isEmpty()).toList();
  }
  /// a method of a literal: the tokens before its arrow and after; a lambda has no head
  record Meth(List<Item> head, List<Item> body, int start, int end){
    Optional<String> name(){
      var k= !head.isEmpty() && Tokens.is(head.get(0), Kind.RCap) ? 1 : 0;
      return k < head.size() && Tokens.is(head.get(k), Kind.DotName, Kind.Op) ? Optional.of(((Tok)head.get(k)).text()) : Optional.empty();
    }
    private int afterName(){ return (!head.isEmpty() && Tokens.is(head.get(0), Kind.RCap) ? 1 : 0)+(name().isPresent() ? 1 : 0); }
    /// the generics declared by the method
    List<String> xs(){
      var k= afterName();
      if (k >= head.size() || !Tokens.isGroup(head.get(k), Kind.OSquare)){ return List.of(); }
      return split(((Group)head.get(k)).items, Kind.Comma).stream().filter(a->Tokens.is(a.get(0), Kind.UppercaseId)).map(a->((Tok)a.get(0)).text()).toList();
    }
    /// the parameter names in order, "_" for a pattern, then "::" when the body uses it
    List<String> params(){
      var k= afterName();
      if (k < head.size() && Tokens.isGroup(head.get(k), Kind.OSquare)){ k+= 1; }
      var res= new ArrayList<String>();
      var round= k < head.size() && Tokens.isGroup(head.get(k), Kind.ORound);
      if (round){ split(((Group)head.get(k)).items, Kind.Comma).forEach(a->res.add(paramName(a.get(0)))); }
      for (; !round && k < head.size() && !Tokens.is(head.get(k), Kind.Colon); k+= 1){
        var it= head.get(k);
        if (!Tokens.is(it, Kind.Comma)){ res.add(paramName(it)); }
      }
      if (hasImplicit(body)){ res.add("::"); }
      return res;
    }
    private static String paramName(Item it){ return Tokens.is(it, Kind.LowercaseId, Kind.Underscore) ? ((Tok)it).text() : "_"; }
    Map<String,List<Item>> typedParams(){
      var res= new HashMap<String,List<Item>>();
      for (var it : head){
        if (!Tokens.isGroup(it, Kind.ORound)){ continue; }
        for (var a : split(((Group)it).items, Kind.Comma)){
          if (a.size() > 2 && Tokens.is(a.get(0), Kind.LowercaseId) && Tokens.is(a.get(1), Kind.Colon)){ res.put(((Tok)a.get(0)).text(), a.subList(2, a.size())); }
        }
      }
      return res;
    }
  }
  static boolean hasImplicit(List<Item> items){
    return items.stream().anyMatch(it->it instanceof Group g ? g.open != Kind.OCurly && hasImplicit(g.items) : Tokens.is(it, Kind.ColonColon));
  }
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
    for (int k= 0; k < cur.size(); k+= 1){
      if (Tokens.is(cur.get(k), Kind.Arrow)){ return new Meth(List.copyOf(cur.subList(0, k)), List.copyOf(cur.subList(k+1, cur.size())), start, end); }
    }
    var sig= Tokens.is(cur.get(0), Kind.DotName, Kind.Op, Kind.RCap);
    return sig ? new Meth(List.copyOf(cur), List.of(), start, end) : new Meth(List.of(), List.copyOf(cur), start, end);
  }
  static Optional<String> selfName(Group g){
    var self= g.items.size() >= 2 && Tokens.is(g.items.get(0), Kind.SQuote) && Tokens.is(g.items.get(1), Kind.LowercaseId);
    return self ? Optional.of(((Tok)g.items.get(1)).text()) : Optional.empty();
  }
  /// Name[Xs]: Super1, Super2 {..}: the name, the generics and the supertypes of a declaration body
  record Header(String name, List<String> xs, List<List<Item>> supers){}
  static Optional<Header> declHeader(Group g){
    var items= g.parent.items;
    var supers= new ArrayList<List<Item>>();
    var cur= new ArrayList<Item>();
    int j= items.indexOf(g)-1;
    for (; j >= 0 && !Tokens.is(items.get(j), Kind.Colon); j-= 1){
      var it= items.get(j);
      if (Tokens.is(it, Kind.Comma)){ supers.add(0, cur); cur= new ArrayList<>(); continue; }
      if (!Tokens.is(it, Kind.RCap, Kind.UppercaseId) && !Tokens.isGroup(it, Kind.OSquare)){ return Optional.empty(); }
      cur.add(0, it);
    }
    if (!cur.isEmpty()){ supers.add(0, cur); }
    if (j < 0){ return Optional.empty(); }
    j-= 1;
    var xs= new ArrayList<String>();
    if (j >= 0 && Tokens.isGroup(items.get(j), Kind.OSquare)){
      for (var a : split(((Group)items.get(j)).items, Kind.Comma)){
        if (Tokens.is(a.get(0), Kind.UppercaseId)){ xs.add(((Tok)a.get(0)).text()); }
      }
      j-= 1;
    }
    if (j < 0 || !Tokens.is(items.get(j), Kind.UppercaseId)){ return Optional.empty(); }
    return Optional.of(new Header(((Tok)items.get(j)).text(), xs, supers));
  }
}

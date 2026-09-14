package suggest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import suggest.Api.Method;
import suggest.Api.Ty;
import suggest.Chain.Atom;
import suggest.Chain.Call;
import suggest.Chain.E;
import suggest.Chain.Known;
import suggest.Chain.Meth;
import suggest.Chain.Sugar;
import suggest.Tokens.Group;
import suggest.Tokens.Item;
import suggest.Tokens.Kind;
import suggest.Tokens.Tok;

/// Types the expression before the cursor from the compiled information and lists the methods
/// of that type, dot methods and operators alike. A parameter name has exactly one binder, found
/// in the enclosing curly groups innermost first: an earlier x = e in the same body, the method's
/// own parameters (typed, or from the method it implements), the literal's self name, and this
/// for the top level declaration. A literal is typed from the parameter type of the call it is an
/// argument of, and the types of its bodies flow back into the generics of that call, which also
/// meet the declared return type of the method the call ends. Whatever cannot be typed is
/// unknown, and unknown has no methods. A lowercase name right before the dot that names a
/// compiled package, after base. or while typing base.Uni, also lists the types of that package,
/// the private ones for this package only.
public final class Resolver{
  public record Row(String name, List<Ty> ts, Ty ret){
    public String insert(){ return name.startsWith(".") ? name : " "+name+" "; }
    public String display(){
      var ps= ts.isEmpty() ? "" : ts.stream().map(Ty::show).collect(Collectors.joining(", ", "(", ")"));
      return name+ps+": "+ret.show();
    }
  }
  /// the rows for the cursor, the receiver type they are the methods of, the offset their
  /// text replaces from: the dot or the typed prefix, and the types of the package before the dot
  public record Suggestions(int from, Ty receiver, List<Row> rows, List<Ty> types){}
  private record Cont(Ty x, Ty k){}
  private record Bound(Method m, HashMap<String,Ty> sub){}
  private static final Kind[] separators= {Kind.SemiColon, Kind.Comma, Kind.Arrow, Kind.Colon, Kind.SQuote};
  private static final Pattern use= Pattern.compile("use\\s+([a-z0-9_]+\\.[A-Za-z0-9_]+)\\s+as\\s+([A-Za-z0-9_]+)\\s*;");
  private static final Comparator<Method> order= Comparator.comparing((Method m)->!m.name().startsWith(".")).thenComparing(Method::name).thenComparing(Method::arity);
  private static final Comparator<Ty> byName= Comparator.comparing(Ty::name).thenComparing(t->t.args().size());
  private final Api api;
  private final String pkg;
  private final Map<String,String> aliases;
  private final String text;
  private final Group root;
  /// what is being computed, a literal being refined or a sugar name being bound: asked again
  /// meanwhile, it is unknown
  private final HashSet<Object> busy= new HashSet<>();
  public Resolver(Api api, String pkg, Map<String,String> aliases, String text){
    this.api= api;
    this.pkg= pkg;
    this.aliases= aliases;
    this.text= text;
    this.root= Tokens.group(text);
  }
  /// the use directives of a package head file, alias to full name
  public static Map<String,String> aliases(String head){
    var res= new HashMap<String,String>();
    var m= use.matcher(head);
    while (m.find()){ res.put(m.group(2), m.group(1)); }
    return res;
  }
  /// nothing while a name is being typed; after a dot or a typed method name the methods matching
  /// it; anywhere else the methods of the expression before the cursor, inserted with their dot
  public Suggestions suggest(int pos){
    var items= new ArrayList<>(before(Tokens.innermost(root, pos), pos));
    var from= pos;
    var prefix= "";
    var touching= !items.isEmpty() && items.getLast() instanceof Tok t && pos <= t.end();
    var typing= touching && Tokens.is(items.getLast(), Kind.DotName, Kind.Op);
    var qualifying= touching && Tokens.is(items.getLast(), Kind.UppercaseId) && ((Tok)items.getLast()).text().indexOf('.') >= 0;
    if (typing){ from= items.removeLast().start(); prefix= text.substring(from, pos); }
    else if (qualifying){ from= items.getLast().start()+((Tok)items.getLast()).text().indexOf('.'); prefix= text.substring(from, pos); }
    else if (pos > 0 && text.charAt(pos-1) == '.'){ from= pos-1; prefix= "."; }
    var types= types(items, from, prefix);
    var seg= touching && !typing ? List.<Item>of() : segment(items);
    if (seg.isEmpty()){ return new Suggestions(from, Ty.unknown, List.of(), types); }
    var t= typeOf(Chain.parse(seg), Map.of());
    var ms= api.methods(t);
    if (ms.isEmpty() && t.name().startsWith(pkg+".")){ ms= supersMethods(t.name().substring(pkg.length()+1)); }
    var p= prefix;
    var rows= ms.stream().filter(m->p.equals(".") || m.name().startsWith(p)).sorted(order).map(m->new Row(m.name(), m.ts(), m.ret())).toList();
    return new Suggestions(from, t, rows, types);
  }
  /// the types of the package named right before the dot at from, alone or as the head of the
  /// qualified name being typed, whose simple name starts with what follows the dot
  private List<Ty> types(List<Item> items, int from, String prefix){
    if (!prefix.startsWith(".") || items.isEmpty() || !(items.getLast() instanceof Tok t)){ return List.of(); }
    var lower= t.kind() == Kind.LowercaseId && t.end() == from;
    var upper= t.kind() == Kind.UppercaseId && t.start() < from && from < t.end();
    if (!lower && !upper){ return List.of(); }
    var name= t.text().substring(0, from-t.start());
    var p= name+"."+prefix.substring(1);
    return api.types(name).stream().filter(ty->ty.name().startsWith(p) && (name.equals(pkg) || !ty.name().startsWith(name+"._"))).sorted(byName).toList();
  }
  private static List<Item> before(Group g, int pos){ return g.items.stream().filter(it->it.start() < pos).toList(); }
  /// the expression segment: what follows the last separator
  private static List<Item> segment(List<Item> items){
    var start= 0;
    for (int j= 0; j < items.size(); j+= 1){ if (Tokens.is(items.get(j), separators)){ start= j+1; } }
    return items.subList(start, items.size());
  }
  private static List<Item> segment(Group g, int pos){ return segment(before(g, pos)); }
  Ty typeOf(E e, Map<String,Ty> scope){
    return switch (e){
      case Known k -> k.t();
      case Atom a -> typeOfAtom(a, scope);
      case Call c -> callSub(c, scope, -1, Optional.empty()).map(b->b.m.ret().subst(b.sub).erase(b.m.bs())).orElse(Ty.unknown);
      case Sugar s -> continuation(s.call(), scope).map(k->typeOf(Chain.parse(new Known(k.k), s.rest()), scope)).orElse(Ty.unknown);
    };
  }
  /// the signature of a call and the substitution binding the receiver's generics, the explicit
  /// type arguments, whatever unifying the arguments binds, but for the argument at skip, then
  /// what the result type expected of the call binds
  private Optional<Bound> callSub(Call c, Map<String,Ty> scope, int skip, Optional<Ty> result){
    var recv= typeOf(c.recv(), scope);
    var e= api.entry(recv);
    var m= e.flatMap(t->t.method(c.name(), c.arity()));
    if (m.isEmpty()){ return Optional.empty(); }
    var sub= new HashMap<>(e.get().bind(recv));
    var targs= Chain.split(c.targs(), Kind.Comma).stream().filter(a->a.size() > 1 || !Tokens.is(a.get(0), Kind.RCap)).toList();
    if (!targs.isEmpty() && targs.size() == m.get().bs().size()){
      var generics= genericsInScope(targs.get(0).get(0).start());
      for (int j= 0; j < targs.size(); j+= 1){ sub.put(m.get().bs().get(j), parseType(targs.get(j), generics)); }
    }
    var open= new ArrayList<>(m.get().bs());
    e.get().bs().stream().filter(b->sub.get(b).equals(Ty.unknown)).forEach(open::add);
    for (int j= 0; j < c.arity(); j+= 1){
      if (j == skip){ continue; }
      var at= typeOfArg(c.args().get(j), m.get().ts().get(j).subst(sub), scope);
      unify(m.get().ts().get(j), at, sub, open);
    }
    result.ifPresent(r->unify(m.get().ret(), r, sub, open));
    return Optional.of(new Bound(m.get(), sub));
  }
  /// an open generic meets a bound type and is bound; a class meets the same class and their
  /// arguments meet, or a subclass and its supertypes meet the class in turn
  private void unify(Ty pt, Ty at, HashMap<String,Ty> sub, List<String> open){
    if (pt.isX()){
      if (open.contains(pt.name()) && !bound(sub.getOrDefault(pt.name(), Ty.unknown), open) && bound(at, open)){ sub.put(pt.name(), at); }
      return;
    }
    if (!at.isC()){ return; }
    if (!pt.name().equals(at.name())){
      var e= api.entry(at);
      if (e.isEmpty()){ return; }
      var s= e.get().bind(at);
      e.get().supers().forEach(sup->unify(pt, sup.subst(s), sub, open));
      return;
    }
    if (pt.args().size() != at.args().size()){ return; }
    for (int j= 0; j < pt.args().size(); j+= 1){ unify(pt.args().get(j), at.args().get(j), sub, open); }
  }
  private static boolean bound(Ty t, List<String> open){ return !t.equals(Ty.unknown) && !(t.isX() && open.contains(t.name())); }
  /// recv.m x = thunk: the continuation is the second parameter; its unique abstract two
  /// parameter method binds x to the first parameter and the rest of the chain to the second
  private Optional<Cont> continuation(Call c, Map<String,Ty> scope){
    var b= callSub(c, scope, -1, Optional.empty());
    if (b.isEmpty()){ return Optional.empty(); }
    var c2= b.get().m.ts().get(1).subst(b.get().sub);
    var e= api.entry(c2);
    var m= e.flatMap(t->t.lambda(2, Set.of()));
    if (m.isEmpty()){ return Optional.empty(); }
    var s2= e.get().bind(c2);
    return Optional.of(new Cont(m.get().ts().get(0).subst(s2), m.get().ts().get(1).subst(s2)));
  }
  private Ty typeOfArg(E a, Ty expected, Map<String,Ty> scope){
    var lambda= a instanceof Atom at && at.items().size() == 1 && Tokens.isGroup(at.items().get(0), Kind.OCurly);
    return lambda ? lambdaType((Group)((Atom)a).items().get(0), expected, scope) : typeOf(a, scope);
  }
  /// the expected type, with its generics still open bound by the types of the bodies of the
  /// literal's methods meeting the result types of the methods they implement
  private Ty lambdaType(Group g, Ty expected, Map<String,Ty> scope){
    var e= api.entry(expected);
    if (e.isEmpty()){ return Ty.unknown; }
    var sub= e.get().bind(expected);
    var open= e.get().bs().stream().filter(b->!sub.get(b).isC()).toList();
    var found= new HashMap<String,Ty>();
    for (var meth : Chain.methodsOf(g)){
      var params= meth.params();
      var m= implemented(e.get(), g, meth);
      if (m.isEmpty() || meth.body().isEmpty()){ continue; }
      var bodyScope= new HashMap<>(scope);
      for (int k= 0; k < params.size(); k+= 1){ bodyScope.put(params.get(k), m.get().ts().get(k).subst(sub)); }
      unify(m.get().ret(), typeOf(Chain.parse(meth.body()), bodyScope), found, open);
    }
    return new Ty(expected.name(), e.get().bs().stream().map(b->found.getOrDefault(b, sub.get(b))).toList());
  }
  /// the method of the type a method of a literal implements: by name, or the abstract one of
  /// its arity not implemented by name in the same literal
  private static Optional<Method> implemented(Api.Type t, Group g, Meth meth){
    var n= meth.params().size();
    if (meth.name().isPresent()){ return t.method(meth.name().get(), n); }
    return t.lambda(n, Chain.methodsOf(g).stream().flatMap(m->m.name().stream()).collect(Collectors.toSet()));
  }
  private Ty typeOfAtom(Atom a, Map<String,Ty> scope){
    if (a.items().isEmpty()){ return Ty.unknown; }
    if (a.items().get(0) instanceof Group g){ return g.open == Kind.ORound ? typeOf(Chain.parse(g.items), scope) : Ty.unknown; }
    var t= (Tok)a.items().get(0);
    return switch (t.kind()){
      case UppercaseId -> parseType(a.items(), genericsInScope(t.start()));
      case UnsignedInt, SignedInt, SignedFloat, UnsignedFloat, UStr, SStr -> tname(t.text(), 0);
      case LowercaseId, ColonColon -> scope.containsKey(t.text()) ? scope.get(t.text()) : binder(t.text(), t.start());
      default -> Ty.unknown;
    };
  }
  /// the type a name written in the source stands for: a literal's, an alias's, or one of this
  /// package; a bare name declared at one arity only gets unknown arguments
  private Ty tname(String s, int arity){
    var c= s.charAt(0);
    if (c == '"' || c == '`'){ return new Ty("base.Str", List.of()); }
    if (Character.isDigit(c) || c == '+' || c == '-'){ return new Ty(s.indexOf('.') >= 0 ? "base.Float" : Character.isDigit(c) ? "base.Nat" : "base.Int", List.of()); }
    var full= s.indexOf('.') >= 0 ? s : aliases.getOrDefault(s, pkg+"."+s);
    return new Ty(full, Collections.nCopies(arity == 0 ? api.onlyArity(full) : arity, Ty.unknown));
  }
  /// [RC] T [Args] or a generic in scope
  private Ty parseType(List<Item> items, Map<String,Ty> generics){
    var ts= items.stream().filter(it->!Tokens.is(it, Kind.RCap, Kind.ReadImm)).toList();
    if (ts.isEmpty() || !Tokens.is(ts.get(0), Kind.UppercaseId)){ return Ty.unknown; }
    var name= ((Tok)ts.get(0)).text();
    if (generics.containsKey(name)){ return generics.get(name); }
    if (ts.size() < 2 || !Tokens.isGroup(ts.get(1), Kind.OSquare)){ return tname(name, 0); }
    var args= Chain.split(((Group)ts.get(1)).items, Kind.Comma).stream().map(x->parseType(x, generics)).toList();
    return new Ty(tname(name, args.size()).name(), args);
  }
  /// the generics of the enclosing declarations and of the enclosing methods
  private Map<String,Ty> genericsInScope(int pos){
    var res= new HashMap<String,Ty>();
    for (var g= Tokens.innermost(root, pos); g.parent != null; g= g.parent){
      if (g.open != Kind.OCurly){ continue; }
      Chain.declHeader(g).ifPresent(h->h.xs().forEach(x->res.put(x, new Ty(x, List.of()))));
      Chain.methodsOf(g).stream().filter(m->m.start() <= pos && pos <= m.end()).forEach(m->m.xs().forEach(x->res.put(x, new Ty(x, List.of()))));
    }
    return res;
  }
  private Ty binder(String x, int pos){
    for (var g= Tokens.innermost(root, pos); g.parent != null; g= g.parent){
      if (g.open != Kind.OCurly){ continue; }
      var t= binderIn(g, x, pos);
      if (t.isPresent()){ return t.get(); }
    }
    return Ty.unknown;
  }
  private Optional<Ty> binderIn(Group g, String x, int pos){
    var mine= Chain.methodsOf(g).stream().filter(m->m.start() <= pos && pos <= m.end()).toList();
    if (mine.isEmpty()){ return Optional.empty(); }
    var m= mine.get(0);
    var self= Chain.selfName(g).map(x::equals).orElseGet(()->x.equals("this") && g.parent.parent == null && Chain.declHeader(g).isPresent());
    if (self){ return Optional.of(literalType(g).orElse(Ty.unknown)); }
    var body= m.body();
    for (int j= 0; j+2 < body.size() && body.get(j).start() < pos; j+= 1){
      var eq= Tokens.is(body.get(j), Kind.LowercaseId) && ((Tok)body.get(j)).text().equals(x) && Tokens.is(body.get(j+1), Kind.Eq);
      if (eq){ return Optional.of(sugarBinder(Chain.parse(body.subList(0, j+3)), x)); }
    }
    var params= m.params();
    if (!params.contains(x)){ return Optional.empty(); }
    var typed= m.typedParams();
    if (typed.containsKey(x)){ return Optional.of(parseType(typed.get(x), genericsInScope(typed.get(x).get(0).start()))); }
    var lt= literalType(g);
    var e= lt.flatMap(api::entry);
    var fromSupers= e.isEmpty() || m.name().isEmpty() && Chain.declHeader(g).isPresent();
    if (fromSupers){ return Optional.of(paramFromSupers(g, m, x)); }
    return Optional.of(implemented(e.get(), g, m).map(l->l.ts().get(params.indexOf(x)).subst(e.get().bind(lt.get()))).orElse(Ty.unknown));
  }
  /// the type x is bound to by the sugar binding it, the last of a chain of sugars
  private Ty sugarBinder(E e, String x){
    if (!busy.add(x)){ return Ty.unknown; }
    var res= Ty.unknown;
    for (; e instanceof Sugar s;){
      var k= continuation(s.call(), Map.of());
      if (k.isEmpty()){ break; }
      if (s.x().equals(x)){ res= k.get().x(); break; }
      e= Chain.parse(new Known(k.get().k()), s.rest());
    }
    busy.remove(x);
    return res;
  }
  /// a declaration the last compile does not know: the parameter comes from the supertypes in its header
  private Ty paramFromSupers(Group g, Meth meth, String x){
    var h= Chain.declHeader(g);
    if (h.isEmpty()){ return Ty.unknown; }
    for (var sup : h.get().supers()){
      var st= parseType(sup, genericsInScope(g.start+1));
      var e= api.entry(st);
      var m= e.flatMap(t->implemented(t, g, meth));
      if (m.isPresent()){ return m.get().ts().get(meth.params().indexOf(x)).subst(e.get().bind(st)); }
    }
    return Ty.unknown;
  }
  /// the nominal type of the object a curly group builds; a literal typed by what expects it has
  /// its open generics bound by its bodies, unless that is what is being computed
  private Optional<Ty> literalType(Group g){
    var h= Chain.declHeader(g);
    if (h.isPresent()){ return Optional.of(new Ty(tname(h.get().name(), h.get().xs().size()).name(), h.get().xs().stream().map(x->new Ty(x, List.of())).toList())); }
    var items= g.parent.items;
    var i= items.indexOf(g);
    var generics= genericsInScope(g.start);
    if (i > 0 && Tokens.is(items.get(i-1), Kind.UppercaseId)){ return Optional.of(parseType(items.subList(i-1, i), generics)); }
    if (i > 1 && Tokens.is(items.get(i-2), Kind.UppercaseId) && Tokens.isGroup(items.get(i-1), Kind.OSquare)){ return Optional.of(parseType(items.subList(i-2, i), generics)); }
    var expected= expectedType(g);
    if (expected.isEmpty() || !busy.add(g)){ return expected; }
    var res= lambdaType(g, expected.get(), Map.of());
    busy.remove(g);
    return Optional.of(res);
  }
  /// the expected type of a curly group: the parameter type of the call it is an argument of,
  /// or the result type of the method it is the whole body of, declared or implemented
  private Optional<Ty> expectedType(Group g){
    var p= g.parent;
    var seg= segment(p, g.start);
    if (seg.isEmpty() && p.open == Kind.ORound){
      var call= new ArrayList<>(segment(p.parent, p.start));
      call.add(p);
      return argType(Chain.parse(call), g, Map.of(), declaredResult(p));
    }
    if (seg.size() >= 2 && Tokens.is(seg.getLast(), Kind.DotName, Kind.Op, Kind.Eq)){
      var call= new ArrayList<>(seg);
      call.add(g);
      return argType(Chain.parse(call), g, Map.of(), declaredResult(g));
    }
    if (!seg.isEmpty() || p.open != Kind.OCurly){ return Optional.empty(); }
    for (var m : Chain.methodsOf(p)){
      if (m.body().isEmpty() || m.body().get(0) != g){ continue; }
      var declared= declaredType(m);
      if (declared.isPresent()){ return declared; }
      var lt= literalType(p);
      var e= lt.flatMap(api::entry);
      return e.flatMap(t->implemented(t, p, m)).map(x->x.ret().subst(e.get().bind(lt.get())).erase(x.bs()));
    }
    return Optional.empty();
  }
  /// the return type declared for a method
  private Optional<Ty> declaredType(Meth m){
    var colon= IntStream.range(0, m.head().size()).filter(k->Tokens.is(m.head().get(k), Kind.Colon)).max();
    return colon.isEmpty() ? Optional.empty() : Optional.of(parseType(m.head().subList(colon.getAsInt()+1, m.head().size()), genericsInScope(m.head().get(0).start())));
  }
  /// the declared return type of the method whose body ends with the item, when it has one
  private Optional<Ty> declaredResult(Item last){
    var p= Tokens.innermost(root, last.start());
    if (p.open != Kind.OCurly){ return Optional.empty(); }
    return Chain.methodsOf(p).stream().filter(m->!m.body().isEmpty() && m.body().getLast() == last).findFirst().flatMap(this::declaredType);
  }
  /// the parameter type for the group in the call: as an argument of the call, or as the thunk
  /// of a sugar, or as an argument of the chain the sugar's continuation receives
  private Optional<Ty> argType(E call, Group g, Map<String,Ty> scope, Optional<Ty> result){
    if (call instanceof Sugar s){
      if (isAtom(s.call().args().get(0), g)){ return callSub(s.call(), scope, 0, Optional.empty()).map(b->b.m.ts().get(0).subst(b.sub)); }
      return continuation(s.call(), scope).flatMap(k->argType(Chain.parse(new Known(k.k), s.rest()), g, scope, result));
    }
    if (!(call instanceof Call c)){ return Optional.empty(); }
    var idx= IntStream.range(0, c.arity()).filter(j->isAtom(c.args().get(j), g)).findFirst();
    if (idx.isEmpty()){ return Optional.empty(); }
    return callSub(c, scope, idx.getAsInt(), result).map(b->b.m.ts().get(idx.getAsInt()).subst(b.sub));
  }
  private static boolean isAtom(E e, Group g){ return e instanceof Atom a && !a.items().isEmpty() && a.items().get(0) == g; }
  /// a declaration of this package the last compile does not know: the methods of its supertypes
  private List<Method> supersMethods(String name){
    var g= findDecl(root, name);
    if (g.isEmpty()){ return List.of(); }
    var seen= new HashSet<String>();
    var res= new ArrayList<Method>();
    for (var sup : Chain.declHeader(g.get()).get().supers()){
      for (var m : api.methods(parseType(sup, genericsInScope(g.get().start+1)))){ if (seen.add(m.name()+"/"+m.arity())){ res.add(m); } }
    }
    return res;
  }
  private static Optional<Group> findDecl(Group g, String name){
    for (var it : g.items){
      if (!(it instanceof Group c)){ continue; }
      var own= c.open == Kind.OCurly && Chain.declHeader(c).filter(h->h.name().equals(name)).isPresent();
      if (own){ return Optional.of(c); }
      var inner= findDecl(c, name);
      if (inner.isPresent()){ return inner; }
    }
    return Optional.empty();
  }
}

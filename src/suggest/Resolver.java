package suggest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import suggest.Api.Method;
import suggest.Api.Ty;
import suggest.Api.Type;
import suggest.Chain.Atom;
import suggest.Chain.Call;
import suggest.Chain.E;
import suggest.Chain.Header;
import suggest.Chain.Known;
import suggest.Chain.Meth;
import suggest.Chain.Sugar;
import suggest.Tokens.Group;
import suggest.Tokens.Item;
import suggest.Tokens.Kind;
import suggest.Tokens.Tok;

/// Types the expression before the cursor from the compiled information and lists the methods
/// of that type, dot methods and operators alike. The names in scope come from the literals
/// around the cursor, outermost first: a literal's self name, or this for a top level
/// declaration; the generics of a declaration and of the method holding the cursor; that
/// method's parameters, typed where written, else from the method the literal implements; the
/// names bound by an earlier x = e of the same expression. A declaration's literal has the type it
/// declares, a typed literal the type written, any other literal the type the call it is an
/// argument of (or the method it is the whole body of) expects, with the generics still open
/// bound by the types of its bodies. A call binds the generics of its method by meeting each
/// argument with its parameter type, left to right, then its result type with the type expected
/// of the call. Whatever cannot be typed is unknown, and unknown has no methods. A type name
/// without [..] has no generics. A declaration of this file the last compile does not know has the
/// methods of its supertypes. A lowercase name right before the dot that names a compiled
/// package, a qualified type name being typed, or the package of a use directive, also lists the
/// public types of that package.
public record Resolver(Api api, String pkg, Map<String,String> aliases, String text, List<Tok> tokens, Group root){
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
  private record Bound(Method m, HashMap<String,Ty> sub){}
  private static final Kind[] separators= {Kind.SemiColon, Kind.Comma, Kind.Arrow, Kind.Colon, Kind.SQuote};
  private static final Set<Kind> quoted= Set.of(Kind.LineComment, Kind.BlockComment, Kind.BadUnclosedBlockComment, Kind.UStr, Kind.SStr, Kind.BadUStrUnclosed, Kind.BadSStrUnclosed);
  private static final Set<Kind> unclosed= Set.of(Kind.LineComment, Kind.BadUnclosedBlockComment, Kind.BadUStrUnclosed, Kind.BadSStrUnclosed);
  private static final Comparator<Method> order= Comparator.comparing((Method m)->!m.name().startsWith(".")).thenComparing(Method::name).thenComparing(Method::arity);
  private static final Comparator<Ty> byName= Comparator.comparing(Ty::name).thenComparing(t->t.args().size());
  public static Resolver of(Api api, String pkg, Map<String,String> aliases, String text){
    var tokens= Tokens.tokens(text);
    return new Resolver(api, pkg, aliases, text, tokens, Tokens.group(tokens));
  }
  /// the use directives of a package head file, alias to full name
  public static Map<String,String> aliases(String head){
    var items= Tokens.group(Tokens.tokens(head)).items;
    var res= new HashMap<String,String>();
    for (int j= 0; j+4 < items.size(); j+= 1){
      var use= word(items.get(j), "use") && Tokens.is(items.get(j+1), Kind.UppercaseId) && word(items.get(j+2), "as") && Tokens.is(items.get(j+3), Kind.UppercaseId) && Tokens.is(items.get(j+4), Kind.SemiColon);
      if (use){ res.put(Tokens.text(items.get(j+3)), Tokens.text(items.get(j+1))); }
    }
    return res;
  }
  private static boolean word(Item it, String w){ return Tokens.is(it, Kind.LowercaseId) && Tokens.text(it).equals(w); }
  /// nothing inside a comment or a string, or while a name is being typed; after a dot or a typed
  /// method name the methods matching it; anywhere else the methods of the expression before the
  /// cursor, inserted with their dot
  public Suggestions suggest(int pos){
    var inside= tokens.stream().anyMatch(t->quoted.contains(t.kind()) && t.start() < pos && (pos < t.end() || pos == t.end() && unclosed.contains(t.kind())));
    if (inside){ return new Suggestions(pos, Ty.unknown, List.of(), List.of()); }
    var g= Tokens.innermost(root, pos);
    var items= new ArrayList<>(g.items.stream().filter(it->it.start() < pos).toList());
    var from= pos;
    var prefix= "";
    var touching= !items.isEmpty() && items.getLast() instanceof Tok t && pos <= t.end();
    var typing= touching && Tokens.is(items.getLast(), Kind.DotName, Kind.Op);
    var qualifying= touching && Tokens.is(items.getLast(), Kind.UppercaseId) && Tokens.text(items.getLast()).indexOf('.') >= 0;
    if (typing){ from= items.removeLast().start(); prefix= text.substring(from, pos); }
    else if (qualifying){ from= items.getLast().start()+Tokens.text(items.getLast()).indexOf('.'); prefix= text.substring(from, pos); }
    else if (pos > 0 && text.charAt(pos-1) == '.'){ from= pos-1; prefix= "."; }
    var types= types(items, from, prefix);
    var seg= touching && !typing ? List.<Item>of() : segment(items);
    if (seg.isEmpty()){ return new Suggestions(from, Ty.unknown, List.of(), types); }
    var t= typeOf(Chain.parse(seg), scope(g, pos));
    var p= prefix;
    var rows= methods(t).stream().filter(m->p.equals(".") || m.name().startsWith(p)).sorted(order).map(m->new Row(m.name(), m.ts(), m.ret())).toList();
    return new Suggestions(from, t, rows, types);
  }
  /// the public types of the package named right before the dot at from, alone or as the head of
  /// the qualified name being typed, whose simple name starts with what follows the dot
  private List<Ty> types(List<Item> items, int from, String prefix){
    if (!prefix.startsWith(".") || items.isEmpty() || !(items.getLast() instanceof Tok t)){ return List.of(); }
    var lower= t.kind() == Kind.LowercaseId && t.end() == from;
    var upper= t.kind() == Kind.UppercaseId && t.start() < from && from < t.end();
    if (!lower && !upper){ return List.of(); }
    var p= t.text().substring(0, from-t.start())+"."+prefix.substring(1);
    return api.types(t.text().substring(0, from-t.start())).stream().filter(ty->ty.name().startsWith(p)).sorted(byName).toList();
  }
  /// the expression: what follows the last separator
  private static List<Item> segment(List<Item> items){
    var start= IntStream.range(0, items.size()).filter(j->Tokens.is(items.get(j), separators)).max().orElse(-1)+1;
    return items.subList(start, items.size());
  }
  private static List<Item> segment(Group g, int pos){ return segment(g.items.stream().filter(it->it.start() < pos).toList()); }
  /// the names in scope at pos, in g and the groups around it
  private HashMap<String,Ty> scope(Group g, int pos){
    var res= g.parent == null ? new HashMap<String,Ty>() : scope(g.parent, pos);
    if (g.open == Kind.OCurly){ enter(g, pos, res); }
    return res;
  }
  private void enter(Group g, int pos, HashMap<String,Ty> scope){
    var t= literal(g, scope);
    Chain.selfName(g).or(()->g.parent == root ? Optional.of("this") : Optional.empty()).ifPresent(x->scope.put(x, t));
    var m= Chain.methodsOf(g).stream().filter(x->x.start() <= pos && pos <= x.end()).findFirst();
    if (m.isEmpty()){ return; }
    m.get().xs().forEach(x->scope.put(x, new Ty(x, List.of())));
    var e= entry(t);
    params(m.get(), e.flatMap(x->implemented(x, g, m.get())), e.map(x->x.bind(t)).orElse(Map.of()), scope);
  }
  /// binds the parameters of a method of a literal: to their written type, else to the type of
  /// the parameter of the method it implements
  private void params(Meth m, Optional<Method> implemented, Map<String,Ty> sub, HashMap<String,Ty> scope){
    var ps= m.params();
    IntStream.range(0, ps.size()).forEach(k->scope.put(ps.get(k).x(), !ps.get(k).type().isEmpty() ? parseType(ps.get(k).type(), scope) : implemented.map(i->i.ts().get(k).subst(sub)).orElse(Ty.unknown)));
  }
  /// the type of the object a curly group builds; it binds the generics of a declaration
  private Ty literal(Group g, HashMap<String,Ty> scope){
    var h= Chain.header(g);
    if (h.isPresent()){
      h.get().xs().forEach(x->scope.put(x, new Ty(x, List.of())));
      return new Ty(tname(h.get().name()), h.get().xs().stream().map(x->new Ty(x, List.of())).toList());
    }
    var items= g.parent.items;
    var i= items.indexOf(g);
    var k= i >= 1 && Tokens.isGroup(items.get(i-1), Kind.OSquareArg) ? 2 : 1;
    if (i >= k && Tokens.is(items.get(i-k), Tokens.typeName)){ return parseType(items.subList(i-k, i), scope); }
    return expected(g, scope).map(x->lambdaType(g, x, scope)).orElse(Ty.unknown);
  }
  /// the parameter type of the call a curly group is an argument of, or the result type of the
  /// method it is the whole body of, declared or implemented
  private Optional<Ty> expected(Group g, HashMap<String,Ty> scope){
    var p= g.parent;
    var seg= segment(p, g.start);
    if (seg.isEmpty() && p.open == Kind.ORound){
      var call= new ArrayList<>(segment(p.parent, p.start));
      call.add(p);
      return argType(Chain.parse(call), g, scope, result(p.parent, p, scope));
    }
    if (!seg.isEmpty() && Tokens.is(seg.getLast(), Kind.DotName, Kind.Op, Kind.Eq)){
      var call= new ArrayList<>(seg);
      call.add(g);
      return argType(Chain.parse(call), g, scope, result(p, g, scope));
    }
    return seg.isEmpty() ? result(p, g, scope) : Optional.empty();
  }
  /// the result type of the method of g whose body ends with the item, declared or implemented
  private Optional<Ty> result(Group g, Item last, HashMap<String,Ty> scope){
    var m= Chain.methodsOf(g).stream().filter(x->g.open == Kind.OCurly && !x.body().isEmpty() && x.body().getLast() == last).findFirst();
    if (m.isEmpty()){ return Optional.empty(); }
    if (!m.get().result().isEmpty()){ return Optional.of(parseType(m.get().result(), scope)); }
    var t= literal(g, scope);
    var e= entry(t);
    return e.flatMap(x->implemented(x, g, m.get())).map(x->x.ret().subst(e.get().bind(t)).erase(x.bs()));
  }
  /// the parameter type for the group in the call: as an argument of the call, as the thunk of a
  /// sugar, or in the rest of a sugar, which also binds the sugar's name
  private Optional<Ty> argType(E call, Group g, HashMap<String,Ty> scope, Optional<Ty> result){
    if (call instanceof Sugar s){
      if (isAtom(s.call().args().get(0), g)){ return callSub(s.call(), scope, 0, Optional.empty()).map(b->b.m.ts().get(0).subst(b.sub)); }
      var k= continuation(s.call(), scope);
      k.ifPresent(x->scope.put(s.x(), x.ts().get(0)));
      return k.flatMap(x->argType(Chain.parse(new Known(x.ts().get(1)), s.rest()), g, scope, result));
    }
    if (!(call instanceof Call c)){ return Optional.empty(); }
    var idx= IntStream.range(0, c.arity()).filter(j->isAtom(c.args().get(j), g)).findFirst();
    if (idx.isEmpty()){ return Optional.empty(); }
    return callSub(c, scope, idx.getAsInt(), result).map(b->b.m.ts().get(idx.getAsInt()).subst(b.sub));
  }
  private static boolean isAtom(E e, Group g){ return e instanceof Atom a && !a.items().isEmpty() && a.items().get(0) == g; }
  Ty typeOf(E e, Map<String,Ty> scope){
    return switch (e){
      case Known k -> k.t();
      case Atom a -> typeOfAtom(a, scope);
      case Call c -> callSub(c, scope, -1, Optional.empty()).map(b->b.m.ret().subst(b.sub).erase(b.m.bs())).orElse(Ty.unknown);
      case Sugar s -> continuation(s.call(), scope).map(k->typeOf(Chain.parse(new Known(k.ts().get(1)), s.rest()), with(scope, s.x(), k.ts().get(0)))).orElse(Ty.unknown);
    };
  }
  private static Map<String,Ty> with(Map<String,Ty> scope, String x, Ty t){
    var res= new HashMap<>(scope);
    res.put(x, t);
    return res;
  }
  private Ty typeOfAtom(Atom a, Map<String,Ty> scope){
    if (a.items().isEmpty()){ return Ty.unknown; }
    if (a.items().get(0) instanceof Group g){ return g.open == Kind.ORound ? typeOf(Chain.parse(g.items), scope) : Ty.unknown; }
    if (Tokens.is(a.items().get(0), Tokens.typeName)){ return parseType(a.items(), scope); }
    return scope.getOrDefault(Tokens.text(a.items().get(0)), Ty.unknown);
  }
  /// the signature of a call and the substitution binding the receiver's generics, the explicit
  /// type arguments, whatever meeting the arguments binds, but for the argument at skip, then
  /// what meeting the result type expected of the call binds
  private Optional<Bound> callSub(Call c, Map<String,Ty> scope, int skip, Optional<Ty> result){
    var recv= typeOf(c.recv(), scope);
    var e= entry(recv);
    var m= e.flatMap(t->t.method(c.name(), c.arity()));
    if (m.isEmpty()){ return Optional.empty(); }
    var sub= new HashMap<>(e.get().bind(recv));
    var targs= Chain.split(c.targs(), Kind.Comma).stream().filter(a->!(a.size() == 1 && Tokens.is(a.get(0), Kind.RCap))).toList();
    if (targs.size() == m.get().bs().size()){ IntStream.range(0, targs.size()).forEach(j->sub.put(m.get().bs().get(j), parseType(targs.get(j), scope))); }
    var open= new ArrayList<>(m.get().bs());
    e.get().bs().stream().filter(b->sub.get(b).equals(Ty.unknown)).forEach(open::add);
    for (int j= 0; j < c.arity(); j+= 1){
      if (j != skip){ unify(m.get().ts().get(j), typeOfArg(c.args().get(j), m.get().ts().get(j).subst(sub), scope), sub, open); }
    }
    result.ifPresent(r->unify(m.get().ret(), r, sub, open));
    return Optional.of(new Bound(m.get(), sub));
  }
  /// an open generic meets a bound type and is bound; a class meets a type whose supertypes, the
  /// type included and each visited once, hold that class: their arguments meet
  private void unify(Ty pt, Ty at, HashMap<String,Ty> sub, List<String> open){
    if (pt.isX()){
      if (open.contains(pt.name()) && !bound(sub.getOrDefault(pt.name(), Ty.unknown), open) && bound(at, open)){ sub.put(pt.name(), at); }
      return;
    }
    if (!at.isC()){ return; }
    var supers= new LinkedHashMap<String,Ty>();
    supers(at, supers);
    supers.values().stream().filter(s->s.name().equals(pt.name()) && s.args().size() == pt.args().size())
      .forEach(s->IntStream.range(0, pt.args().size()).forEach(j->unify(pt.args().get(j), s.args().get(j), sub, open)));
  }
  private void supers(Ty t, LinkedHashMap<String,Ty> seen){
    if (seen.putIfAbsent(t.name()+"/"+t.args().size(), t) != null){ return; }
    entry(t).ifPresent(e->e.supers().forEach(s->supers(s.subst(e.bind(t)), seen)));
  }
  private static boolean bound(Ty t, List<String> open){ return !t.equals(Ty.unknown) && !(t.isX() && open.contains(t.name())); }
  /// recv.m x = thunk: the continuation is the second parameter; its only abstract method of two
  /// parameters binds x to the first parameter and the rest of the chain to the second
  private Optional<Method> continuation(Call c, Map<String,Ty> scope){
    return callSub(c, scope, -1, Optional.empty()).map(b->b.m.ts().get(1).subst(b.sub)).flatMap(this::lambda2);
  }
  private Optional<Method> lambda2(Ty t){ return entry(t).flatMap(e->e.lambda(2, Set.of()).map(m->m.subst(e.bind(t)))); }
  private Ty typeOfArg(E a, Ty expected, Map<String,Ty> scope){
    return a instanceof Atom(var items) && items.size() == 1 && items.get(0) instanceof Group g && g.open == Kind.OCurly ? lambdaType(g, expected, scope) : typeOf(a, scope);
  }
  /// the expected type, with its generics still open bound by the types of the bodies of the
  /// literal's methods meeting the result types of the methods they implement
  private Ty lambdaType(Group g, Ty expected, Map<String,Ty> scope){
    var e= entry(expected);
    if (e.isEmpty()){ return Ty.unknown; }
    var sub= e.get().bind(expected);
    var open= e.get().bs().stream().filter(b->!sub.get(b).isC()).toList();
    var found= new HashMap<String,Ty>();
    for (var meth : Chain.methodsOf(g)){
      var m= implemented(e.get(), g, meth);
      if (m.isEmpty() || meth.body().isEmpty()){ continue; }
      var body= new HashMap<>(scope);
      params(meth, m, sub, body);
      unify(m.get().ret(), typeOf(Chain.parse(meth.body()), body), found, open);
    }
    return new Ty(expected.name(), e.get().bs().stream().map(b->found.getOrDefault(b, sub.get(b))).toList());
  }
  /// the method of the type a method of a literal implements: by name, or the abstract one of
  /// its arity not implemented by name in the same literal
  private static Optional<Method> implemented(Type t, Group g, Meth meth){
    var n= meth.params().size();
    if (meth.name().isPresent()){ return t.method(meth.name().get(), n); }
    return t.lambda(n, Chain.methodsOf(g).stream().flatMap(m->m.name().stream()).collect(Collectors.toSet()));
  }
  private Optional<Type> entry(Ty t){ return entry(t, new HashSet<>()); }
  /// the compiled type, or a declaration of this file the last compile does not know, with the
  /// methods of its supertypes; seen are the declarations already visited, which add nothing
  private Optional<Type> entry(Ty t, HashSet<String> seen){
    var res= api.entry(t);
    if (res.isPresent() || !t.name().startsWith(pkg+".") || !seen.add(t.name())){ return res; }
    return declaration(root, t.name().substring(pkg.length()+1)).filter(h->h.xs().size() == t.args().size()).map(h->declared(h, seen));
  }
  private Type declared(Header h, HashSet<String> seen){
    var xs= new HashMap<String,Ty>();
    h.xs().forEach(x->xs.put(x, new Ty(x, List.of())));
    var supers= h.supers().stream().map(c->parseType(c, xs)).toList();
    return new Type(pkg+"."+h.name(), h.xs(), supers, supers.stream().flatMap(c->methods(c, seen).stream()).toList());
  }
  private static Optional<Header> declaration(Group g, String name){
    return g.items.stream().filter(it->it instanceof Group).map(Group.class::cast)
      .flatMap(c->(c.open == Kind.OCurly ? Chain.header(c) : Optional.<Header>empty()).filter(h->h.name().equals(name)).or(()->declaration(c, name)).stream()).findFirst();
  }
  /// one method per name and arity, with the type's arguments substituted
  private List<Method> methods(Ty t){ return methods(t, new HashSet<>()); }
  private List<Method> methods(Ty t, HashSet<String> seen){
    var e= entry(t, seen);
    if (e.isEmpty()){ return List.of(); }
    var sub= e.get().bind(t);
    var names= new HashSet<String>();
    return e.get().ms().stream().filter(m->names.add(m.name()+"/"+m.arity())).map(m->m.subst(sub)).toList();
  }
  /// the qualified name a type name written in the source stands for: a literal's, an alias's, or
  /// one of this package
  private String tname(String s){
    var c= s.charAt(0);
    if (c == '"' || c == '`'){ return "base.Str"; }
    if (Character.isDigit(c) || c == '+' || c == '-'){ return s.indexOf('.') >= 0 ? "base.Float" : Character.isDigit(c) ? "base.Nat" : "base.Int"; }
    return s.indexOf('.') >= 0 ? s : aliases.getOrDefault(s, pkg+"."+s);
  }
  /// [RC] C[Ts], read/imm X, [RC] X, a generic being a name in scope
  private Ty parseType(List<Item> items, Map<String,Ty> scope){
    var ts= items.stream().filter(it->!Tokens.is(it, Kind.RCap, Kind.ReadImm)).toList();
    if (ts.isEmpty() || !Tokens.is(ts.get(0), Tokens.typeName)){ return Ty.unknown; }
    var name= Tokens.text(ts.get(0));
    if (scope.containsKey(name)){ return scope.get(name); }
    var args= ts.size() > 1 && Tokens.isGroup(ts.get(1), Kind.OSquareArg) ? Chain.split(((Group)ts.get(1)).items, Kind.Comma).stream().map(x->parseType(x, scope)).toList() : List.<Ty>of();
    return new Ty(tname(name), args);
  }
}

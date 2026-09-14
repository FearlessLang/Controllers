package suggest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/// The compiled information of the packages in scope, read from the api json the compiler
/// writes for every package (apiJson.ApiJson in Coordinator): each type with its generics,
/// its supertypes and every method it has, inherited ones included, keyed by name and arity.
/// Reference capabilities are dropped: a type is nominal, a class C[T1..Tn], a type variable X
/// or unknown. A method name and arity repeats once per receiver capability; the first is used.
public final class Api{
  public record Ty(String name, List<Ty> args){
    public static final Ty unknown= new Ty("?", List.of());
    public boolean isC(){ return name.indexOf('.') >= 0; }
    public boolean isX(){ return !isC() && !name.equals("?"); }
    Ty subst(Map<String,Ty> sub){
      if (isX()){ return sub.getOrDefault(name, this); }
      return new Ty(name, args.stream().map(a->a.subst(sub)).toList());
    }
    Ty erase(List<String> bs){
      if (isX()){ return bs.contains(name) ? unknown : this; }
      return new Ty(name, args.stream().map(a->a.erase(bs)).toList());
    }
    /// the type with simple names
    public String show(){
      var simple= name.substring(name.indexOf('.')+1);
      return args.isEmpty() ? simple : simple+"["+args.stream().map(Ty::show).collect(Collectors.joining(","))+"]";
    }
    @Override public String toString(){
      return args.isEmpty() ? name : name+"["+args.stream().map(Ty::toString).collect(Collectors.joining(","))+"]";
    }
  }
  public record Method(String name, List<String> bs, List<Ty> ts, Ty ret, boolean abs){
    public int arity(){ return ts.size(); }
    Method subst(Map<String,Ty> sub){ return new Method(name, bs, ts.stream().map(t->t.subst(sub)).toList(), ret.subst(sub), abs); }
  }
  public record Type(String name, List<String> bs, List<Ty> supers, List<Method> ms){
    Optional<Method> method(String name, int arity){ return ms.stream().filter(m->m.name.equals(name) && m.arity() == arity).findFirst(); }
    /// the abstract method of that arity an unnamed method implements: the only one but for the taken names
    Optional<Method> lambda(int arity, Set<String> taken){
      var names= ms.stream().filter(m->m.abs && m.arity() == arity && !taken.contains(m.name)).map(Method::name).distinct().toList();
      return names.size() == 1 ? method(names.get(0), arity) : Optional.empty();
    }
    Map<String,Ty> bind(Ty t){
      assert t.args.size() == bs.size();
      var res= new HashMap<String,Ty>();
      for (int j= 0; j < bs.size(); j+= 1){ res.put(bs.get(j), t.args.get(j)); }
      return res;
    }
  }
  private final Map<String,List<Type>> types= new HashMap<>();
  public Api(List<Type> all){ all.forEach(t->types.computeIfAbsent(t.name, n->new ArrayList<>()).add(t)); }
  public Optional<Type> entry(Ty t){
    var res= types.getOrDefault(t.name, List.of()).stream().filter(x->x.bs.size() == t.args.size()).toList();
    assert res.size() <= 1;
    return res.stream().findFirst();
  }
  /// one Ty per declaration of the package, with its generics as type variables
  public List<Ty> types(String pkg){
    return types.values().stream().flatMap(List::stream).filter(t->t.name.startsWith(pkg+".")).map(t->new Ty(t.name, t.bs.stream().map(b->new Ty(b, List.of())).toList())).toList();
  }
  /// the arity of a name declared at exactly one arity, else 0
  int onlyArity(String name){
    var l= types.getOrDefault(name, List.of());
    return l.size() == 1 ? l.get(0).bs.size() : 0;
  }
  /// one method per name and arity, with the receiver's type arguments substituted
  public List<Method> methods(Ty t){
    var e= entry(t);
    if (e.isEmpty()){ return List.of(); }
    var sub= e.get().bind(t);
    var seen= new HashSet<String>();
    return e.get().ms.stream().filter(m->seen.add(m.name+"/"+m.arity())).map(m->m.subst(sub)).toList();
  }
  public static List<Type> parse(String json){ return new Json(json).arr().stream().map(t->type(arr(t))).toList(); }
  private static Type type(List<Object> a){
    var bs= arr(a.get(2)).stream().map(b->str(arr(b).get(0))).toList();
    var supers= arr(a.get(3)).stream().map(Api::superTy).toList();
    var ms= arr(a.get(4)).stream().map(m->method(arr(m))).toList();
    return new Type(str(a.get(0)), bs, supers, ms);
  }
  private static Ty superTy(Object o){
    var a= arr(o);
    return new Ty(str(a.get(0)), a.subList(1, a.size()).stream().map(Api::ty).toList());
  }
  private static Method method(List<Object> a){
    var bs= arr(a.get(2)).stream().map(b->str(arr(b).get(0))).toList();
    var ts= arr(a.get(3)).stream().map(Api::ty).toList();
    return new Method(str(a.get(0)), bs, ts, ty(a.get(4)), str(a.get(7)).equals("abs"));
  }
  private static Ty ty(Object o){
    var a= arr(o);
    if (str(a.get(0)).equals("x")){ return new Ty(str(a.get(a.size()-1)), List.of()); }
    return new Ty(str(a.get(2)), a.subList(3, a.size()).stream().map(Api::ty).toList());
  }
  @SuppressWarnings("unchecked") private static List<Object> arr(Object o){ return (List<Object>)o; }
  private static String str(Object o){ return (String)o; }
  /// Reads what ApiJson writes: nested arrays of strings, without escapes or whitespace.
  private static final class Json{
    private final String s;
    private int i;
    Json(String s){ this.s= s; }
    List<Object> arr(){
      var res= new ArrayList<Object>();
      for (i+= 1; s.charAt(i) != ']'; i+= s.charAt(i) == ',' ? 1 : 0){ res.add(s.charAt(i) == '"' ? str() : arr()); }
      i+= 1;
      return res;
    }
    String str(){
      var j= s.indexOf('"', i+1);
      var res= s.substring(i+1, j);
      i= j+1;
      return res;
    }
  }
}

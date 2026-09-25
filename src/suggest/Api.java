package suggest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/// The compiled information of the packages in scope, read from the api json the compiler
/// writes for every package (apiJson.ApiJson in Coordinator): each type with its generics,
/// its supertypes and every method it has, inherited ones included. Reference capabilities are
/// dropped: a type is a class C[T1..Tn] (a qualified name), a type variable X, or unknown.
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
  }
  public record Method(String name, List<String> bs, List<Ty> ts, Ty ret, boolean abs){
    public int arity(){ return ts.size(); }
    Method subst(Map<String,Ty> sub){ return new Method(name, bs, ts.stream().map(t->t.subst(sub)).toList(), ret.subst(sub), abs); }
  }
  public record Type(String name, List<String> bs, List<Ty> supers, List<Method> ms){
    Optional<Method> method(String name, int arity){ return ms.stream().filter(m->m.name.equals(name) && m.arity() == arity).reduce((a,b)->{ throw new IllegalArgumentException("Type "+this.name+" has two methods "+a.name+" of "+arity+" parameters"); }); }
    /// the abstract method of that arity a method without name implements: the only one but for the taken names
    Optional<Method> lambda(int arity, Set<String> taken){
      var names= ms.stream().filter(m->m.abs && m.arity() == arity && !taken.contains(m.name)).map(Method::name).distinct().toList();
      return names.size() == 1 ? method(names.get(0), arity) : Optional.empty();
    }
    Map<String,Ty> bind(Ty t){ return IntStream.range(0, bs.size()).boxed().collect(Collectors.toMap(bs::get, t.args::get)); }
  }
  private final Map<String,Type> types;
  public Api(List<Type> all){ types= all.stream().collect(Collectors.toMap(t->t.name+"/"+t.bs.size(), Function.identity())); }
  Optional<Type> entry(Ty t){ return Optional.ofNullable(types.get(t.name+"/"+t.args.size())); }
  /// the public types of the package, with their generics as type variables
  List<Ty> types(String pkg){
    return types.values().stream().filter(t->t.name.startsWith(pkg+".") && !t.name.startsWith(pkg+"._")).map(t->new Ty(t.name, t.bs.stream().map(b->new Ty(b, List.of())).toList())).toList();
  }
  /// [name, rc, [[X, rcs..]..], [[C, ts..]..], methods, self] with a method
  /// [name, rc, [[X, rcs..]..], [ts..], ret, origin, origin arity, abs|concrete] and a type
  /// [x, [rc,] X] or [c, rc, C, ts..]; a malformed text is an error
  public static List<Type> parse(String json){
    var j= new Json(json);
    var all= j.arr();
    j.check(j.i == json.length());
    return all.stream().map(t->type(arr(t))).toList();
  }
  private static Type type(List<Object> a){
    var supers= arr(a.get(3)).stream().map(o->{ var c= arr(o); return new Ty(str(c.get(0)), c.subList(1, c.size()).stream().map(Api::ty).toList()); }).toList();
    return new Type(str(a.get(0)), bs(a.get(2)), supers, arr(a.get(4)).stream().map(m->method(arr(m))).toList());
  }
  private static List<String> bs(Object o){ return arr(o).stream().map(b->str(arr(b).get(0))).toList(); }
  private static Method method(List<Object> a){
    return new Method(str(a.get(0)), bs(a.get(2)), arr(a.get(3)).stream().map(Api::ty).toList(), ty(a.get(4)), str(a.get(7)).equals("abs"));
  }
  private static Ty ty(Object o){
    var a= arr(o);
    if (str(a.get(0)).equals("x")){ return new Ty(str(a.getLast()), List.of()); }
    return new Ty(str(a.get(2)), a.subList(3, a.size()).stream().map(Api::ty).toList());
  }
  /// the packages the map directives of the project give the package names written in pkg, from
  /// the _map.json the compiler writes, {"target":{"in":"out",...},...}; a malformed text is an error
  public static Map<String,String> packages(String json, String pkg){
    var j= new Json(json);
    var all= j.obj(()->j.obj(j::str));
    j.check(j.i == json.length());
    return all.getOrDefault(pkg, Map.of());
  }
  @SuppressWarnings("unchecked") private static List<Object> arr(Object o){ return (List<Object>)o; }
  private static String str(Object o){ return (String)o; }
  /// nested arrays of strings, without escapes or whitespace, as ApiJson writes them, and objects
  /// as the _map.json of the compiler has them: a newline after a comma and after the closing brace
  private static final class Json{
    private final String s;
    private int i;
    Json(String s){ this.s= s; }
    void check(boolean ok){ if (!ok){ throw new IllegalArgumentException("Malformed compiled json at offset "+i); } }
    <T> Map<String,T> obj(Supplier<T> value){
      check(s.startsWith("{", i));
      i+= 1;
      var res= new LinkedHashMap<String,T>();
      while (!s.startsWith("}", i)){
        if (!res.isEmpty()){ check(s.startsWith(",\n", i)); i+= 2; }
        var k= str();
        check(s.startsWith(":", i));
        i+= 1;
        check(res.put(k, value.get()) == null);
      }
      i+= s.startsWith("}\n", i) ? 2 : 1;
      return res;
    }
    List<Object> arr(){
      check(s.startsWith("[", i));
      var res= new ArrayList<Object>();
      i+= 1;
      if (s.startsWith("]", i)){ i+= 1; return res; }
      while (true){
        res.add(s.startsWith("\"", i) ? str() : arr());
        if (s.startsWith("]", i)){ i+= 1; return res; }
        check(s.startsWith(",", i));
        i+= 1;
      }
    }
    String str(){
      var j= s.indexOf('"', i+1);
      check(j > 0);
      var res= s.substring(i+1, j);
      i= j+1;
      return res;
    }
  }
}

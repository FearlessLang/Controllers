package suggest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static suggest.DocsTest.same;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import suggest.Api.Ty;
import suggest.Resolver.Row;
import suggest.Resolver.Suggestions;

final class ResolverTest{
  static String x(String n){ return "[\"x\",\""+n+"\"]"; }
  static String c(String n, String... args){ return "[\"c\",\"imm\",\""+n+"\""+Stream.of(args).map(a->","+a).collect(Collectors.joining())+"]"; }
  static String sup(String n, String... args){ return "[\""+n+"\""+Stream.of(args).map(a->","+a).collect(Collectors.joining())+"]"; }
  static String list(String... xs){ return Stream.of(xs).collect(Collectors.joining(",","[","]")); }
  static String bs(String... xs){ return list(Stream.of(xs).map(b->"[\""+b+"\",\"imm\"]").toArray(String[]::new)); }
  static String method(String name, String bs, String ts, String ret, String kind){ return "[\""+name+"\",\"imm\","+bs+","+ts+","+ret+",\"o\",\"0\",\""+kind+"\"]"; }
  static String m(String name, String bs, String ts, String ret){ return method(name,bs,ts,ret,"concrete"); }
  static String abs(String name, String bs, String ts, String ret){ return method(name,bs,ts,ret,"abs"); }
  static String type(String name, String bs, String cs, String... ms){ return "[\""+name+"\",\"imm\","+bs+","+cs+","+list(ms)+",\"this\"]"; }
  static final String api= list(
    type("base.Void",bs(),"[]"),
    type("base.Bool",bs(),"[]",m("&&",bs(),list(c("base.Bool")),c("base.Bool"))),
    type("base.Str",bs(),"[]",m(".size",bs(),list(),c("base.Nat")),m("+",bs(),list(c("base.Str")),c("base.Str"))),
    type("base.Nat",bs(),"[]",m("+",bs(),list(c("base.Nat")),c("base.Nat")),m(">",bs(),list(c("base.Nat")),c("base.Bool")),m(".str",bs(),list(),c("base.Str")),m("<=>",bs(),list(c("base.Nat"),c("base.Bool")),c("base.Bool"))),
    type("base.Int",bs(),"[]",m(".abs",bs(),list(),c("base.Nat"))),
    type("base.Float",bs(),"[]",m(".round",bs(),list(),c("base.Int"))),
    type("base.F",bs("A","R"),"[]",abs("#",bs(),list(x("A")),x("R"))),
    type("base.F",bs("A","B","R"),"[]",abs("#",bs(),list(x("A"),x("B")),x("R"))),
    type("base.MF",bs("R"),"[]",abs("#",bs(),list(),x("R"))),
    type("base.List",bs("E"),"[]",m(".get",bs(),list(c("base.Nat")),x("E")),m(".flow",bs(),list(),c("base.Flow",x("E"))),m(".size",bs(),list(),c("base.Nat"))),
    type("base.Flow",bs("E"),"[]",
      m(".map",bs("R"),list(c("base.F",x("E"),x("R"))),c("base.Flow",x("R"))),
      m(".filter",bs(),list(c("base.F",x("E"),c("base.Bool"))),c("base.Flow",x("E"))),
      m(".fold",bs("R"),list(c("base.MF",x("R")),c("base.F",x("R"),x("E"),x("R"))),x("R")),
      m(".list",bs(),list(),c("base.List",x("E"))),
      m(".first",bs(),list(),c("base.Opt",x("E")))),
    type("base.Opt",bs("E"),"[]",m(".match",bs("R"),list(c("base.OptMatch",x("E"),x("R"))),x("R")),m(".get",bs(),list(),x("E"))),
    type("base.OptMatch",bs("E","R"),"[]",abs(".some",bs(),list(x("E")),x("R")),abs(".empty",bs(),list(),x("R"))),
    type("base.Opts",bs(),"[]",m("#",bs("T"),list(x("T")),c("base.Opt",x("T")))),
    type("base.Block",bs(),"[]",m("#",bs("R"),list(),c("base.Block",x("R")))),
    type("base.Block",bs("R"),"[]",m(".let",bs("X"),list(c("base.MF",x("X")),c("base.Continuation",x("X"),x("R"))),x("R")),m(".return",bs(),list(c("base.MF",x("R"))),x("R"))),
    type("base.Continuation",bs("T","R"),"[]",abs("#",bs(),list(x("T"),c("base.Block",x("R"))),x("R"))),
    type("base.OrderHash",bs("T"),"[]",abs(".cmp",bs(),list(x("T"),x("T")),c("base.Bool")),abs(".hash",bs(),list(),c("base.Nat")),m(".assertEq",bs(),list(x("T")),c("base.Void"))),
    type("base._Secret",bs(),"[]"),
    type("test._Hidden",bs(),"[]"),
    type("test.Cat",bs(),"[]",m(".name",bs(),list(),c("base.Str")),m(".weight",bs(),list(),c("base.Nat"))),
    type("test.Person",bs(),list(sup("base.OrderHash",c("test.Person"))),
      m(".name",bs(),list(),c("base.Str")),m(".age",bs(),list(),c("base.Nat")),m(".cats",bs(),list(),c("base.List",c("test.Cat"))),
      m(".cmp",bs(),list(c("test.Person"),c("test.Person")),c("base.Bool")),m(".hash",bs(),list(),c("base.Nat")),m(".assertEq",bs(),list(c("test.Person")),c("base.Void"))),
    type("test.Persons",bs(),"[]",m("#",bs(),list(c("base.Nat"),c("base.Str"),c("base.List",c("test.Cat"))),c("test.Person"))),
    type("test.Cats",bs(),"[]",m("#",bs(),list(c("base.Str"),c("base.Nat")),c("test.Cat"))));
  static final Map<String,String> aliases= Stream.of("Str","Nat","List","Flow","Opt","Opts","OptMatch","F","Block","OrderHash","Bool").collect(Collectors.toMap(n->n,n->"base."+n));
  static final String file= """
    Cats: { #(name: Str, weight: Nat): Cat -> Cat: { .name: Str -> name; .weight: Nat -> weight; } }
    Persons: { #(age: Nat, name: Str, cats: List[Cat]): Person -> Person: OrderHash[Person] { 'self
      read .name: Str -> name;
      read .age: Nat -> age;
      read .cats: List[Cat] -> cats;
      .cmp p1, p2 -> p1.age > (p2.age);
      .hash: Nat -> age;
      } }
    Names: { #(ps: List[Person]): List[Str] ->\s""";
  static final String person= ".age .assertEq .cats .cmp .hash .name";
  static final String nat= ".str + <=> >";
  static final String opt= ".get .match";
  /// the suggestions at the | of the text
  static Suggestions at(String text){
    var pos= text.indexOf('|');
    return Resolver.of(new Api(Api.parse(api)),"test",aliases,text.substring(0,pos)+text.substring(pos+1)).suggest(pos);
  }
  static String names(String text){ return at(text).rows().stream().map(Row::name).collect(Collectors.joining(" ")); }
  static String types(String text){ return at(text).types().stream().map(Ty::show).collect(Collectors.joining(" ")); }
  static String probe(String snippet){ return names(file+snippet); }
  static final String baseTypes= "Block Block[R] Bool Continuation[T,R] F[A,R] F[A,B,R] Float Flow[E] Int List[E] MF[R] Nat Opt[E] OptMatch[E,R] Opts OrderHash[T] Str Void";
  @Test void aParameterTypedInTheHeadRootsTheChain(){ same(".flow .get .size",probe("ps.|")); }
  @Test void aColonAfterParametersWithoutParenthesisTypesTheLastOne(){
    same(nat,names("A: { .foo a, b: Nat -> b.| }"));
    same("",names("A: { .foo a, b: Nat -> a.| }"));
    same(nat,names("A: { .foo(a: Nat): Str -> a.| }"));
  }
  @Test void aTypeNameRootsTheChain(){
    same(person,probe("Persons#(1, \"a\", ps).|"));
    same("#",probe("Cats.|"));
    same("#",probe("test.Cats.|"));
    same(opt,probe("Opts#[Nat] 3 .|"));
  }
  @Test void aTypeNameWithoutGenericsHasNone(){
    same("",probe("Opt.|"));
    same(opt,probe("Opt[Nat].|"));
  }
  @Test void literalsHaveTheirBaseTypes(){
    same(nat,probe("\"abc\".size.|"));
    same(".size +",probe("`abc`.|"));
    same(nat,probe("12.|"));
    same(".abs",probe("-12.|"));
    same(".round",probe("1.5.|"));
  }
  @Test void argumentsComeInRoundGroupsOrJuxtaposedAndASignedNumberAfterANameIsOne(){
    same(person,probe("ps.get(0).|"));
    same(person,probe("ps.get 0 .|"));
    same(person,probe("(ps.get 0).|"));
    same(nat,probe("ps.size + 1 .|"));
    same(nat,probe("12 +1 .|"));
    same("",probe("ps.size +1 .|"));
  }
  @Test void aTokenThatIsNotAPostMakesTheExpressionUnknown(){
    same("",probe("ps ps.|"));
    same("",probe("mut ps.|"));
  }
  @Test void lambdaParametersComeFromTheExpectedType(){
    same(person,probe("ps.flow.map{x -> x.|"));
    same(person,probe("ps.flow.map{::.|"));
    same(person,probe("ps.flow.filter{::.age > 3}.filter{ x -> x.name.size > 3 }.map{::.|"));
    same(".name .weight",probe("ps.flow.map{x -> x.cats.flow.filter{c -> c.|"));
  }
  @Test void theBodyTypeFlowsBackIntoTheCallGeneric(){
    same(".size +",probe("ps.flow.map{::.name}.list.get(0).|"));
    same(".filter .first .fold .list .map",probe("ps.flow.map{::.name}.map{::.size}.|"));
    same(".size +",probe("ps.flow.map{p -> p.cats.flow.map{::.name}.list}.list.get(0).get(1).|"));
    same(".map(F[Person,R]): Flow[R]",at(file+"ps.flow.|").rows().get(4).display());
    same(".fold(MF[R], F[R,Person,R]): R",at(file+"ps.flow.|").rows().get(2).display());
  }
  @Test void foldBindsTheAccumulatorFromTheThunkBeforeTypingTheLambda(){
    same(nat,probe("ps.flow.fold({0}, {acc, p -> acc.|"));
    same(person,probe("ps.flow.fold({0}, {acc, p -> p.|"));
    same(nat,probe("ps.flow.fold({0}, {acc, p -> acc + p.age}).|"));
  }
  @Test void eqSugarBindsTheNameAndTheRestRunsOnTheContinuationBlock(){
    same(person,probe("Block#.let x = {ps.get 0} .return{ x.|"));
    same(".let .return",probe("Block#.let x = {ps.get 0} .|"));
    same(".let .return",probe("Block#.let x = {ps.get 0} .let y = {x.age} .|"));
    same(nat,probe("Block#.let x = {ps.get 0} .let y = {x.age} .return{ y.|"));
    same(person,probe("Block#.let x = {ps.get 0} .return{ ps.flow.map{ p -> x.|"));
    same(nat,probe("Block#.let x = {ps.get 0} .return{ x.age }.|"));
  }
  @Test void anUnderscoreBeforeTheEqualIsNotASugar(){ same("",probe("Block#.let _ = {ps.get 0} .|")); }
  @Test void aNamedMethodOfTheExpectedTypeBindsItsParameters(){
    same(".size +",probe("ps.flow.map{::.name}.first.match{ .some s -> s.|"));
  }
  @Test void theWholeBodyOfAMethodIsTypedByTheResultOfTheMethod(){
    same(".size +",names("A: { .foo: OptMatch[Str,Nat] -> { .some s -> s.| } }"));
    same(person,names("A: { .foo: F[Person,Nat] -> { p -> p.| } }"));
    same("",names("A: { .foo: F[Person,Nat] -> { p -> p.age }.bar{ q -> q.| } }"));
  }
  @Test void thisIsTheTopLevelDeclarationAndTheSelfNameItsLiteral(){
    same(person,names("Person: OrderHash[Person] { 'self .name: Str -> \"a\"; .foo -> self.| }"));
    same(person,names("Person: OrderHash[Person] { .foo -> this.| }"));
    same("#",names(file.replace("-> name;","-> this.|;")));
    same(person,names(file.replace("-> age;","-> self.|;")));
  }
  @Test void untypedParametersComeFromTheMethodImplemented(){
    same(person,names("Person: OrderHash[Person] { .cmp p1, p2 -> p1.| }"));
    same(person,names("Person: OrderHash[Person] { .cmp(p1, p2) -> p2.| }"));
  }
  @Test void aDeclarationTheLastCompileDoesNotKnowUsesItsSupertypes(){
    same(".assertEq .cmp .hash",names("NewThing: OrderHash[NewThing] { .cmp a, b -> a.| }"));
    same(".assertEq .cmp .hash",names("NewThing: OrderHash[NewThing] { .foo -> this.| }"));
    same(".flow .get .size",names("NewThing[T]: OrderHash[NewThing[T]] { .foo(l: List[T]) -> l.| }"));
    same("",names("NewThing[T]: OrderHash[NewThing[T]] { .foo(l: List[T]) -> l.get(0).| }"));
    same(".assertEq .cmp .hash",names("A: { .foo(n: NewThing) -> n.| }\nNewThing: OrderHash[NewThing] { }"));
  }
  @Test void aDeclarationTheLastCompileDoesNotKnowHasTheMethodOfItsFirstSupertypeHavingIt(){
    same(opt,names("NewThing: Opt[Nat], Opt[Str] { .foo -> this.| }"));
    same(nat,names("NewThing: Opt[Nat], Opt[Str] { .foo -> this.get.| }"));
    same(".size +",names("NewThing: Opt[Str], Opt[Nat] { .foo -> this.get.| }"));
  }
  @Test void cyclicSupertypesTheLastCompileDoesNotKnowAddNoMethods(){
    same(".assertEq .cmp .hash",names("A: B, OrderHash[A] { .foo(a: A) -> a.| }\nB: A { }"));
    same(".assertEq .cmp .hash",names("A: B, OrderHash[A] { .foo(b: B) -> b.| }\nB: A { }"));
    same("",names("A: B { .foo(a: A) -> a.| }\nB: A { }"));
    same(nat,names("A: B { .foo(f: F[A,Nat]) -> Block#.let x = {f#(this)} .return{ x.| } }\nB: A { }"));
    same("",names("A: B { .foo(o: Opt[A]) -> o.match{ .some s -> s.| } }\nB: A { }"));
  }
  @Test void nothingWhileANameIsBeingTyped(){
    same("",probe("Perso|"));
    same("",probe("ps|"));
    same("",probe("ps.flow.map{x -> x|"));
  }
  @Test void nothingInsideACommentOrAString(){
    same("",probe("// ps.|"));
    same("",probe("/* ps.|"));
    same("",probe("\"ps.|"));
    same("",probe("`ps.|"));
    same(".size +",probe("\"ps\".|"));
  }
  @Test void aTypedMethodNameFiltersAndIsReplacedFromItsDot(){
    var s= at(file+"ps.flow.f|");
    same(".filter .first .fold",s.rows().stream().map(Row::name).collect(Collectors.joining(" ")));
    assertEquals(file.length()+7,s.from());
    same(".map",names(file+"ps.flow.ma|p"));
    same("+",probe("\"a\".size +|"));
  }
  @Test void theDotIsReplacedAndOperatorsAreInsertedWithSpaces(){
    var s= at(file+"\"a\".size.|");
    assertEquals(file.length()+8,s.from());
    assertEquals(" + ",s.rows().get(1).insert());
    same(".str",s.rows().get(0).insert());
    same("+(Nat): Nat",s.rows().get(1).display());
  }
  @Test void afterAnExpressionTheMethodsAreInsertedAtTheCursor(){
    var s= at(file+"ps.flow |");
    same(".filter .first .fold .list .map",s.rows().stream().map(Row::name).collect(Collectors.joining(" ")));
    assertEquals(file.length()+8,s.from());
  }
  @Test void unknownHasNoMethods(){
    same("",probe("foo.|"));
    same("",probe("ps.nope.|"));
    same("",probe("ps.get.|"));
    same("",probe("{ x -> x }.|"));
    same("",probe("|"));
  }
  @Test void aNameUsedInsideItsOwnThunkIsUnknown(){
    same("",probe("Block#.let x = {x.|"));
    same("",probe("Block#.let x = {ps.get 0} .let y = {y.age.|"));
    same(person,probe("Block#.let x = {ps.get 0} .let y = {x.|"));
  }
  @Test void commentsStringsAndCapabilitiesAreSkipped(){
    same(".flow .get .size",probe("// ps.flow\n ps.|"));
    same(".flow .get .size",probe("ps /* .flow */ .|"));
    same(person,names("Person: OrderHash[Person] { .foo(p: read/imm Person) -> p.| }"));
    same(person,probe("mut Persons#(1, \"a\", ps).|"));
  }
  @Test void theHeadFileGivesTheAliases(){
    assertEquals(Map.of("List","base.List","S","base.Str"),Resolver.aliases("use base.List as List;\n// use base.Nat as N;\nuse base.Str as S;\nA: {}"));
  }
  @Test void aPackageNameBeforeTheDotSuggestsItsPublicTypes(){
    same(baseTypes,types(file+"base.|"));
    same("Cat Cats Person Persons",types(file+"test.|"));
    same("",probe("base.|"));
    same("",types(file+"ps.|"));
    same("",types(file+"Persons.|"));
    same("",types(file+"base |"));
  }
  @Test void aQualifiedNameBeingTypedFiltersTheTypesAndIsReplacedFromItsDot(){
    var s= at(file+"base.O|");
    same("Opt[E] OptMatch[E,R] Opts OrderHash[T]",s.types().stream().map(Ty::show).collect(Collectors.joining(" ")));
    assertEquals(file.length()+4,s.from());
    same("",s.rows().stream().map(Row::name).collect(Collectors.joining(" ")));
    same("Opt[E] OptMatch[E,R] Opts",types(file+"base.Op|tMatch"));
    same("",types(file+"base.o|"));
    same("",types(file+"nope.O|"));
  }
  @Test void aUseDirectiveSuggestsTheTypesOfThePackage(){
    var s= at("use base.Li|");
    same("List[E]",s.types().stream().map(Ty::show).collect(Collectors.joining(" ")));
    assertEquals(8,s.from());
    same(baseTypes,types("use base.|"));
    same("",names("use base.|"));
  }
  @Test void aParameterNamedLikeAPackageGetsItsMethodsAndTheTypesTogether(){
    same(nat,names("A: { .foo(base: Nat) -> base.| }"));
    same(baseTypes,types("A: { .foo(base: Nat) -> base.| }"));
    same(".str",names("A: { .foo(base: Nat) -> base.s| }"));
    same("",types("A: { .foo(base: Nat) -> base.s| }"));
    same("",names("A: { .foo(base: Nat) -> base.O| }"));
    same("Opt[E] OptMatch[E,R] Opts OrderHash[T]",types("A: { .foo(base: Nat) -> base.O| }"));
  }
}

package suggest;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
  static final Map<String,String> aliases= Map.of("Str","base.Str","Nat","base.Nat","List","base.List","Flow","base.Flow","Opt","base.Opt","Block","base.Block","OrderHash","base.OrderHash","Bool","base.Bool");
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
  /// the suggestions at the | of the text
  static Suggestions at(String text){
    var pos= text.indexOf('|');
    return new Resolver(new Api(Api.parse(api)),"test",aliases,text.substring(0,pos)+text.substring(pos+1)).suggest(pos);
  }
  static String names(String text){ return at(text).rows().stream().map(Row::name).collect(Collectors.joining(" ")); }
  static String types(String text){ return at(text).types().stream().map(Ty::show).collect(Collectors.joining(" ")); }
  static String probe(String snippet){ return names(file+snippet); }
  static final String baseTypes= "Block Block[R] Bool Continuation[T,R] F[A,R] F[A,B,R] Float Flow[E] Int List[E] MF[R] Nat Opt[E] OptMatch[E,R] OrderHash[T] Str Void";
  @Test void aParameterTypedInTheHeadRootsTheChain(){ assertEquals(".flow .get .size",probe("ps.|")); }
  @Test void aTypeNameRootsTheChain(){
    assertEquals(person,probe("Persons#(1, \"a\", ps).|"));
    assertEquals("#",probe("Cats.|"));
    assertEquals("#",probe("test.Cats.|"));
  }
  @Test void literalsHaveTheirBaseTypes(){
    assertEquals(nat,probe("`abc`.size.|"));
    assertEquals(".size +",probe("\"abc\".|"));
    assertEquals(nat,probe("12.|"));
    assertEquals(".abs",probe("-12.|"));
    assertEquals(".round",probe("1.5.|"));
  }
  @Test void argumentsComeInRoundGroupsOrJuxtaposedAndASignedNumberAfterANameIsOne(){
    assertEquals(person,probe("ps.get(0).|"));
    assertEquals(person,probe("ps.get 0 .|"));
    assertEquals(person,probe("(ps.get 0).|"));
    assertEquals(nat,probe("ps.size + 1 .|"));
    assertEquals(nat,probe("12 +1 .|"));
    assertEquals("",probe("ps.size +1 .|"));
  }
  @Test void lambdaParametersComeFromTheExpectedType(){
    assertEquals(person,probe("ps.flow.map{x -> x.|"));
    assertEquals(person,probe("ps.flow.map{::.|"));
    assertEquals(person,probe("ps.flow.filter{::.age > 3}.filter{ x -> x.name.size > 3 }.map{::.|"));
    assertEquals(".name .weight",probe("ps.flow.map{x -> x.cats.flow.filter{c -> c.|"));
  }
  @Test void theBodyTypeFlowsBackIntoTheCallGeneric(){
    assertEquals(".size +",probe("ps.flow.map{::.name}.list.get(0).|"));
    assertEquals(".filter .first .fold .list .map",probe("ps.flow.map{::.name}.map{::.size}.|"));
    assertEquals(".size +",probe("ps.flow.map{p -> p.cats.flow.map{::.name}.list}.list.get(0).get(1).|"));
    assertEquals(".map(F[Person,R]): Flow[R]",at(file+"ps.flow.|").rows().get(4).display());
    assertEquals(".fold(MF[R], F[R,Person,R]): R",at(file+"ps.flow.|").rows().get(2).display());
  }
  @Test void foldBindsTheAccumulatorFromTheThunkBeforeTypingTheLambda(){
    assertEquals(nat,probe("ps.flow.fold({0}, {acc, p -> acc.|"));
    assertEquals(person,probe("ps.flow.fold({0}, {acc, p -> p.|"));
    assertEquals(nat,probe("ps.flow.fold({0}, {acc, p -> acc + p.age}).|"));
  }
  @Test void eqSugarBindsTheNameAndTheRestRunsOnTheContinuationBlock(){
    assertEquals(person,probe("Block#.let x = {ps.get 0} .return{ x.|"));
    assertEquals(".let .return",probe("Block#.let x = {ps.get 0} .|"));
    assertEquals(".let .return",probe("Block#.let x = {ps.get 0} .let y = {x.age} .|"));
    assertEquals(nat,probe("Block#.let x = {ps.get 0} .let y = {x.age} .return{ y.|"));
    assertEquals(person,probe("Block#.let x = {ps.get 0} .return{ ps.flow.map{ p -> x.|"));
  }
  @Test void aNamedMethodOfTheExpectedTypeBindsItsParameters(){
    assertEquals(".size +",probe("ps.flow.map{::.name}.first.match{ .some s -> s.|"));
  }
  @Test void thisIsTheTopLevelDeclarationAndTheSelfNameItsLiteral(){
    assertEquals(person,names("Person: OrderHash[Person] { 'self .name: Str -> \"a\"; .foo -> self.| }"));
    assertEquals(person,names("Person: OrderHash[Person] { .foo -> this.| }"));
    assertEquals("#",names(file.replace("-> name;","-> this.|;")));
    assertEquals(person,names(file.replace("-> age;","-> self.|;")));
  }
  @Test void untypedParametersComeFromTheMethodImplemented(){
    assertEquals(person,names("Person: OrderHash[Person] { .cmp p1, p2 -> p1.| }"));
    assertEquals(person,names("Person: OrderHash[Person] { .cmp(p1, p2) -> p2.| }"));
  }
  @Test void aDeclarationTheLastCompileDoesNotKnowUsesItsSupertypes(){
    assertEquals(".assertEq .cmp .hash",names("NewThing: OrderHash[NewThing] { .cmp a, b -> a.| }"));
    assertEquals(".assertEq .cmp .hash",names("NewThing: OrderHash[NewThing] { .foo -> this.| }"));
    assertEquals(".flow .get .size",names("NewThing[T]: OrderHash[NewThing[T]] { .foo(l: List[T]) -> l.| }"));
    assertEquals("",names("NewThing[T]: OrderHash[NewThing[T]] { .foo(l: List[T]) -> l.get(0).| }"));
  }
  @Test void nothingWhileANameIsBeingTyped(){
    assertEquals("",probe("Perso|"));
    assertEquals("",probe("ps|"));
    assertEquals("",probe("ps.flow.map{x -> x|"));
  }
  @Test void aTypedMethodNameFiltersAndIsReplacedFromItsDot(){
    var s= at(file+"ps.flow.f|");
    assertEquals(".filter .first .fold",s.rows().stream().map(Row::name).collect(Collectors.joining(" ")));
    assertEquals(file.length()+7,s.from());
    assertEquals(".map",names(file+"ps.flow.ma|p"));
    assertEquals("+",probe("\"a\".size +|"));
  }
  @Test void theDotIsReplacedAndOperatorsAreInsertedWithSpaces(){
    var s= at(file+"\"a\".size.|");
    assertEquals(file.length()+8,s.from());
    assertEquals(" + ",s.rows().get(1).insert());
    assertEquals(".str",s.rows().get(0).insert());
    assertEquals("+(Nat): Nat",s.rows().get(1).display());
  }
  @Test void afterAnExpressionTheMethodsAreInsertedAtTheCursor(){
    var s= at(file+"ps.flow |");
    assertEquals(".filter .first .fold .list .map",s.rows().stream().map(Row::name).collect(Collectors.joining(" ")));
    assertEquals(file.length()+8,s.from());
  }
  @Test void unknownHasNoMethods(){
    assertEquals("",probe("foo.|"));
    assertEquals("",probe("ps.nope.|"));
    assertEquals("",probe("ps.get.|"));
    assertEquals("",probe("{ x -> x }.|"));
    assertEquals("",probe("|"));
  }
  @Test void aNameUsedInsideItsOwnThunkIsUnknown(){
    assertEquals("",probe("Block#.let x = {x.|"));
    assertEquals("",probe("Block#.let x = {ps.get 0} .let y = {y.age.|"));
    assertEquals(person,probe("Block#.let x = {ps.get 0} .let y = {x.|"));
  }
  @Test void commentsStringsAndCapabilitiesAreSkipped(){
    assertEquals(".flow .get .size",probe("// ps.flow\n ps.|"));
    assertEquals(".flow .get .size",probe("ps /* .flow */ .|"));
    assertEquals(".flow .get .size",probe("mut ps.|"));
    assertEquals(person,names("Person: OrderHash[Person] { .foo(p: read/imm Person) -> p.| }"));
  }
  @Test void theHeadFileGivesTheAliases(){
    assertEquals(Map.of("List","base.List","S","base.Str"),Resolver.aliases("use base.List as List;\nuse base.Str as S;\nA: {}"));
  }
  @Test void aPackageNameBeforeTheDotSuggestsItsTypesPrivateOnesInTheirOwnPackageOnly(){
    assertEquals(baseTypes,types(file+"base.|"));
    assertEquals("Cat Cats Person Persons _Hidden",types(file+"test.|"));
    assertEquals("",probe("base.|"));
    assertEquals("",types(file+"ps.|"));
    assertEquals("",types(file+"Persons.|"));
    assertEquals("",types(file+"base |"));
  }
  @Test void aQualifiedNameBeingTypedFiltersTheTypesAndIsReplacedFromItsDot(){
    var s= at(file+"base.O|");
    assertEquals("Opt[E] OptMatch[E,R] OrderHash[T]",s.types().stream().map(Ty::show).collect(Collectors.joining(" ")));
    assertEquals(file.length()+4,s.from());
    assertEquals("",s.rows().stream().map(Row::name).collect(Collectors.joining(" ")));
    assertEquals("Opt[E] OptMatch[E,R]",types(file+"base.Op|tMatch"));
    assertEquals("",types(file+"base.o|"));
    assertEquals("",types(file+"nope.O|"));
  }
  @Test void aUseDirectiveSuggestsTheTypesOfThePackage(){
    var s= at("use base.Li|");
    assertEquals("List[E]",s.types().stream().map(Ty::show).collect(Collectors.joining(" ")));
    assertEquals(8,s.from());
    assertEquals(baseTypes,types("use base.|"));
    assertEquals("",names("use base.|"));
  }
  @Test void aParameterNamedLikeAPackageGetsItsMethodsAndTheTypesTogether(){
    assertEquals(nat,names("A: { .foo(base: Nat) -> base.| }"));
    assertEquals(baseTypes,types("A: { .foo(base: Nat) -> base.| }"));
    assertEquals(".str",names("A: { .foo(base: Nat) -> base.s| }"));
    assertEquals("",types("A: { .foo(base: Nat) -> base.s| }"));
    assertEquals("",names("A: { .foo(base: Nat) -> base.O| }"));
    assertEquals("Opt[E] OptMatch[E,R] OrderHash[T]",types("A: { .foo(base: Nat) -> base.O| }"));
  }
}

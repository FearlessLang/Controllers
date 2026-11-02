package suggest;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static suggest.DocsTest.same;

import java.util.List;

import org.junit.jupiter.api.Test;

import suggest.Api.Type;

final class ApiTest{
  static String json(String types){ return "{\"inputsSnapshot\":\"\",\n\"manifest\":\"\",\n\"types\":"+types+"}\n"; }
  static List<Type> types(String types){ return Api.parse(json(types)).types(); }
  static String err(String json){ return assertThrows(IllegalArgumentException.class, () -> Api.parse(json)).getMessage(); }
  @Test void aTypeHasItsGenericsSupertypesAndMethods(){
    var t= types(ResolverTest.api).stream().filter(x -> x.name().equals("test.Person")).toList();
    same("[Type[name=test.Person, bs=[], supers=[Ty[name=base.OrderHash, args=[Ty[name=test.Person, args=[]]]]], ms=[[###]]]]", t.toString());
    same("[]", types("[]").toString());
  }
  @Test void aBackslashEscapesTheNextCharacter(){
    same("[Type[name=a.B, bs=[], supers=[Ty[name=base.OpenWith, args=[Ty[name=a.IconsB, args=[]], Ty[name=\"b\\\\\", args=[]]]]], ms=[]]]",
      types("[[\"a.B\",\"imm\",[],[[\"base.OpenWith\",[\"c\",\"imm\",\"a.IconsB\"],[\"c\",\"imm\",\"\\\"b\\\\\\\\\\\"\"]]],[],\"this\"]]").toString());
  }
  @Test void theSnapshotAndTheManifestAreDecoded(){
    var c= Api.parse("{\"inputsSnapshot\":\"fear:/_a/_rank_app.fear\\n---\\nbase 7\\n---\\n\",\n\"manifest\":\"{\\n  \\\"mains\\\": {}\\n}\",\n\"types\":[]}\n");
    same("fear:/_a/_rank_app.fear\n---\nbase 7\n---\n", c.inputsSnapshot());
    same("{\n  \"mains\": {}\n}", c.manifest());
  }
  @Test void aMalformedTextIsAnError(){
    same("Malformed compiled json at offset 0", err(""));
    same("Malformed compiled json at offset 0", err("[]"));
    same("Malformed compiled json at offset 46", err(json("[x]")));
    same("Malformed compiled json at offset 49", err(json("[\"a\"\"b\"]")));
    same("Malformed compiled json at offset 47", err(json("[]]")));
    same("Malformed compiled json at offset 46", err(json("[\"a]")));
    same("Malformed compiled json at offset 47", err(json("[]").stripTrailing()));
    same("Malformed compiled json at offset 49", err(json("[]")+" "));
  }
  @Test void twoTypesOfTheSameNameAndArityAreAnError(){
    var t= "[\"a.B\",\"imm\",[],[],[],\"this\"]";
    assertThrows(IllegalStateException.class, () -> new Api(types("["+t+","+t+"]")));
  }
  @Test void aCompiledTypeWithTwoMethodsOfTheSameNameAndArityIsAnError(){
    var m= ResolverTest.m(".a", ResolverTest.bs(), ResolverTest.list(), ResolverTest.c("a.B"));
    var t= types(ResolverTest.list(ResolverTest.type("a.B", ResolverTest.bs(), "[]", m, m))).getFirst();
    same("Type a.B has two methods .a of 0 parameters", assertThrows(IllegalArgumentException.class, () -> t.method(".a", 0)).getMessage());
  }
}

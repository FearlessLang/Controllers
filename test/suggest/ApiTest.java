package suggest;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static suggest.DocsTest.same;

import org.junit.jupiter.api.Test;

final class ApiTest{
  static String err(String json){ return assertThrows(IllegalArgumentException.class,()->Api.parse(json)).getMessage(); }
  @Test void aTypeHasItsGenericsSupertypesAndMethods(){
    var t= Api.parse(ResolverTest.api).stream().filter(x->x.name().equals("test.Person")).toList();
    same("[Type[name=test.Person, bs=[], supers=[Ty[name=base.OrderHash, args=[Ty[name=test.Person, args=[]]]]], ms=[[###]]]]",t.toString());
    same("[]",Api.parse("[]").toString());
  }
  @Test void aMalformedTextIsAnError(){
    same("Malformed api json at offset 0",err(""));
    same("Malformed api json at offset 1",err("[x]"));
    same("Malformed api json at offset 4",err("[\"a\"\"b\"]"));
    same("Malformed api json at offset 2",err("[]]"));
    same("Malformed api json at offset 4",err("[\"a\""));
    same("Malformed api json at offset 1",err("[\"a]"));
  }
  @Test void twoTypesOfTheSameNameAndArityAreAnError(){
    var t= "[\"a.B\",\"imm\",[],[],[],\"this\"]";
    assertThrows(IllegalStateException.class,()->new Api(Api.parse("["+t+","+t+"]")));
  }
}

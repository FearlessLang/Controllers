package suggest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import utils.Err;

final class DocsTest{
  static{ Err.setUp(AssertionFailedError.class,Assertions::assertEquals,Assertions::assertTrue); }
  static void same(String expected, String actual){ Err.strCmp(expected,actual); }
  static final String txt= """
    package base

    Nat : Sealed, ToStr
    \tread !=(read Nat):Bool
        from: DataType[Nat,Nat]!=
    \t+(Nat):Nat
        ``this + x`` returns the checked sum.
        example:
            .check{(2 + 3) .assertEq 5}
    \t.aluAnd(Nat):Nat
    \t.str:Str

    NatMatch[R:*]
    \t.some(Nat):R
    \tmut .map[R:*](mut MF[T,R]):mut Action[R]

    Block : Sealed
    \t#[R:**]:mut Block[R]

    Block[R:*] : Sealed
      The block of a result R.

      Blocks chain.
    \tmut .return(mut MF[R]):R

        The result.
    \t.done:R

    Pair[A:imm,read,B:*]
    \t.a:A

    Gui
      Text is drawn with fonts bundled in the standard library (Noto Sans, one Noto font per major script,
      Noto Sans Symbols/Math): a character outside those fonts is drawn as a box [everywhere.
      example:
        .check{Gui.run{}}
    \tmut .run(mut Consumer[mut Frame]):Void
        ``this.run f`` runs the gui.
    """;
  static final Docs docs= new Docs(txt);
  static String method(String type, int arity, String name, int n){ return docs.method(type,arity,name,n).orElseThrow(); }
  @Test void aMethodLineComesWithItsDocumentation(){
    same("+(Nat):Nat\n``this + x`` returns the checked sum.\nexample:\n.check{(2 + 3) .assertEq 5}",method("Nat",0,"+",1));
    same("read !=(read Nat):Bool\nfrom: DataType[Nat,Nat]!=",method("Nat",0,"!=",1));
    same(".str:Str",method("Nat",0,".str",0));
    same(".some(Nat):R",method("NatMatch",1,".some",1));
  }
  @Test void aTypeLineComesWithItsOwnDocumentation(){
    same("Nat : Sealed, ToStr",docs.type("Nat",0).orElseThrow());
    same("NatMatch[R:*]",docs.type("NatMatch",1).orElseThrow());
  }
  @Test void theArityTellsTheTypesOfOneName(){
    same("Block : Sealed",docs.type("Block",0).orElseThrow());
    same("Block[R:*] : Sealed\nThe block of a result R.\n\nBlocks chain.",docs.type("Block",1).orElseThrow());
    same("mut .return(mut MF[R]):R\n\nThe result.",method("Block",1,".return",1));
    same(".done:R",method("Block",1,".done",0));
    same(".a:A",method("Pair",2,".a",0));
  }
  @Test void theTabTellsTheDocumentationOfTheTypeFromTheMethodsWhateverItContains(){
    same("Gui\nText is drawn with fonts bundled in the standard library (Noto Sans, one Noto font per major script,\nNoto Sans Symbols/Math): a character outside those fonts is drawn as a box [everywhere.\nexample:\n.check{Gui.run{}}",docs.type("Gui",0).orElseThrow());
    same("mut .run(mut Consumer[mut Frame]):Void\n``this.run f`` runs the gui.",method("Gui",0,".run",1));
  }
  @Test void anAnonymousLiteralHasNoEntry(){
    assertEquals(Optional.empty(),docs.type("_AANat",0));
    assertEquals(Optional.empty(),docs.method("_AANat",0,".str",0));
  }
  @Test void aMissingPublicTypeOrMethodIsAnError(){
    same("No type Nope with 0 generics in the documentation",assertThrows(IllegalArgumentException.class,()->docs.type("Nope",0)).getMessage());
    same("No type Nat with 1 generics in the documentation",assertThrows(IllegalArgumentException.class,()->docs.type("Nat",1)).getMessage());
    same("No method .some/1 of Nat in the documentation",assertThrows(IllegalArgumentException.class,()->docs.method("Nat",0,".some",1)).getMessage());
    same("No method .some/1 of Gui in the documentation",assertThrows(IllegalArgumentException.class,()->docs.method("Gui",0,".some",1)).getMessage());
  }
  @Test void theSignatureIsTheNameAndTheArityWhateverTheTypes(){
    same(".map/1",Docs.signature("\tmut .map[R:*](mut MF[T,R]):mut Action[R]"));
    same(".assertEq/2",Docs.signature("\tread .assertEq(read A1,read LazyInfo):Void"));
    same("!/0",Docs.signature("\t![R:**]:R"));
    same("<=>/2",Docs.signature("\tread <=>(read Nat,mut Ordering[Nat]):Bool"));
    same(".f/1",Docs.signature("\t.f[X:imm,read](X):X"));
  }
}

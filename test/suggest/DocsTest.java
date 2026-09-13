package suggest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;

final class DocsTest{
  static final String txt= """
    package base

    Nat : Sealed, ToStr
      read !=(read Nat):Bool
        from: DataType[Nat,Nat]!=
      +(Nat):Nat
        ``this + x`` returns the checked sum.
        example:
            .check{(2 + 3) .assertEq 5}
      .aluAnd(Nat):Nat
      .str:Str

    NatMatch[R:*]
      .some(Nat):R
      mut .map[R:*](mut MF[T,R]):mut Action[R]

    Gui
      Text is drawn with fonts bundled in the standard library (Noto Sans, one Noto font per major script,
      Noto Sans Symbols/Math): a character outside those fonts is drawn as a box [everywhere.
      mut .run(mut Consumer[mut Frame]):Void
        ``this.run f`` runs the gui.
    """;
  @Test void aMethodLineComesWithItsDocumentation(){
    assertEquals(Optional.of("+(Nat):Nat\n``this + x`` returns the checked sum.\nexample:\n.check{(2 + 3) .assertEq 5}"),new Docs(txt,"Nat").of("+",1));
    assertEquals(Optional.of("read !=(read Nat):Bool\nfrom: DataType[Nat,Nat]!="),new Docs(txt,"Nat").of("!=",1));
    assertEquals(Optional.of(".str:Str"),new Docs(txt,"Nat").of(".str",0));
    assertEquals(Optional.of(".some(Nat):R"),new Docs(txt,"NatMatch").of(".some",1));
  }
  @Test void theTypeDocumentationAtTheSameIndentIsNoMethodWhateverItContains(){
    assertEquals(Optional.of("mut .run(mut Consumer[mut Frame]):Void\n``this.run f`` runs the gui."),new Docs(txt,"Gui").of(".run",1));
    assertEquals(Optional.empty(),new Docs(txt,"Gui").of(".some",1));
  }
  @Test void whatIsNotThereIsEmpty(){
    assertEquals(Optional.empty(),new Docs(txt,"Nat").of(".nope",0));
    assertEquals(Optional.empty(),new Docs(txt,"Nat").of(".some",1));
    assertEquals(Optional.empty(),new Docs(txt,"Na").of(".str",0));
    assertEquals(Optional.empty(),new Docs(txt,"Nope").of(".str",0));
  }
  @Test void theSignatureIsTheNameAndTheArityWhateverTheTypes(){
    assertEquals(".map/1",Docs.signature("  mut .map[R:*](mut MF[T,R]):mut Action[R]"));
    assertEquals(".assertEq/2",Docs.signature("  read .assertEq(read A1,read LazyInfo):Void"));
    assertEquals("!/0",Docs.signature("  ![R:**]:R"));
    assertEquals("<=>/2",Docs.signature("  read <=>(read Nat,mut Ordering[Nat]):Bool"));
  }
}

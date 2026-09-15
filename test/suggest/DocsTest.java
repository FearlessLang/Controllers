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
  static final Docs docs= new Docs(txt);
  @Test void aMethodLineComesWithItsDocumentation(){
    assertEquals(Optional.of("+(Nat):Nat\n``this + x`` returns the checked sum.\nexample:\n.check{(2 + 3) .assertEq 5}"),docs.method("Nat","+",1));
    assertEquals(Optional.of("read !=(read Nat):Bool\nfrom: DataType[Nat,Nat]!="),docs.method("Nat","!=",1));
    assertEquals(Optional.of(".str:Str"),docs.method("Nat",".str",0));
    assertEquals(Optional.of(".some(Nat):R"),docs.method("NatMatch",".some",1));
  }
  @Test void aTypeLineComesWithItsOwnDocumentation(){
    assertEquals(Optional.of("Gui\nText is drawn with fonts bundled in the standard library (Noto Sans, one Noto font per major script,\nNoto Sans Symbols/Math): a character outside those fonts is drawn as a box [everywhere."),docs.type("Gui"));
    assertEquals(Optional.of("Nat : Sealed, ToStr"),docs.type("Nat"));
    assertEquals(Optional.of("NatMatch[R:*]"),docs.type("NatMatch"));
  }
  @Test void theTypeDocumentationAtTheSameIndentIsNoMethodWhateverItContains(){
    assertEquals(Optional.of("mut .run(mut Consumer[mut Frame]):Void\n``this.run f`` runs the gui."),docs.method("Gui",".run",1));
    assertEquals(Optional.empty(),docs.method("Gui",".some",1));
  }
  @Test void whatIsNotThereIsEmpty(){
    assertEquals(Optional.empty(),docs.method("Nat",".nope",0));
    assertEquals(Optional.empty(),docs.method("Nat",".some",1));
    assertEquals(Optional.empty(),docs.method("Na",".str",0));
    assertEquals(Optional.empty(),docs.method("Nope",".str",0));
    assertEquals(Optional.empty(),docs.type("Nope"));
  }
  @Test void onlyAMethodNameMakesALineAMethod(){
    assertEquals(true,Docs.isMethod("  read .assertEq(read A1,read LazyInfo):Void"));
    assertEquals(true,Docs.isMethod("  <=>(Nat):Bool"));
    assertEquals(false,Docs.isMethod("  example:"));
    assertEquals(false,Docs.isMethod("  - a bullet of the documentation"));
    assertEquals(false,Docs.isMethod("    .str:Str"));
  }
  @Test void theSignatureIsTheNameAndTheArityWhateverTheTypes(){
    assertEquals(".map/1",Docs.signature("  mut .map[R:*](mut MF[T,R]):mut Action[R]"));
    assertEquals(".assertEq/2",Docs.signature("  read .assertEq(read A1,read LazyInfo):Void"));
    assertEquals("!/0",Docs.signature("  ![R:**]:R"));
    assertEquals("<=>/2",Docs.signature("  read <=>(read Nat,mut Ordering[Nat]):Bool"));
  }
}

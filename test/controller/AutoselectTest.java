package controller;

import static controller.Errs.err;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import coordinator.MainsInfo;
import coordinator.MainsInfo.Claim;
import coordinator.MainsInfo.Main;

final class AutoselectTest{
  private static Claim claim(String icon, String ext){ return new Claim(icon,"_hello/icons/x.png","","",ext); }
  private static MainsInfo info(String main, List<Claim> shortcuts, List<Claim> openWiths){ return new MainsInfo(Map.of(main,new Main("_hello/bar.fear",shortcuts,openWiths))); }
  private static MainsInfo shortcut(String main, String icon, String ext){ return info(main,List.of(claim(icon,ext)),List.of()); }
  private static MainsInfo others(String... exts){ return info("other.Other",Stream.of(exts).map(e->claim("other.IconsO",e)).toList(),List.of()); }
  private static int start(String alias, String main, String icon){ return Math.floorMod((alias+"::"+main+"::"+icon).hashCode(),1000); }
  private static String fapp(int n){ return "fapp%03d".formatted(Math.floorMod(n,1000)); }
  private static List<String> exts(MainsInfo i){
    return i.mains().values().stream().flatMap(m->Stream.concat(m.shortcuts().stream(),m.openWiths().stream())).map(Claim::extension).toList();
  }
  private static List<String> filled(MainsInfo info, MainsInfo previous, String alias, MainsInfo... others){
    return exts(Project.filled(info,previous,alias,Stream.of(others)));
  }
  @Test void theStartIsTheHashOfAliasMainAndIcon(){
    var n= Math.floorMod("hello::hello.Bar::hello.IconsBar".hashCode(),1000);
    assertEquals(List.of(fapp(n)),filled(shortcut("hello.Bar","hello.IconsBar",""),Project.noClaims,"hello"));
    assertEquals(List.of("ffile%03d".formatted(n)),filled(info("hello.Bar",List.of(),List.of(claim("hello.IconsBar",""))),Project.noClaims,"hello"));
  }
  @Test void anExplicitExtensionIsKept(){
    assertEquals(List.of("bar"),filled(shortcut("hello.Bar","hello.IconsBar","bar"),Project.noClaims,"hello",others("bar")));
  }
  @Test void aTakenNumberMovesToTheNext(){
    var n= start("hello","hello.Bar","hello.IconsBar");
    assertEquals(List.of(fapp(n+2)),filled(shortcut("hello.Bar","hello.IconsBar",""),Project.noClaims,"hello",others(fapp(n),fapp(n+1))));
  }
  @Test void theNumbersWrapFrom999To000(){
    var alias= IntStream.range(0,100000).mapToObj(i->"p"+i).filter(a->start(a,"hello.Bar","hello.IconsBar") == 999).findFirst().orElseThrow();
    assertEquals(List.of("fapp000"),filled(shortcut("hello.Bar","hello.IconsBar",""),Project.noClaims,alias,others("fapp999")));
  }
  @Test void theAutoselectedExtensionOfThePreviousClaimIsKeptEvenWhenTaken(){
    var n= start("hello","hello.Bar","hello.IconsBar");
    var previous= shortcut("hello.Bar","hello.IconsBar",fapp(n+5));
    assertEquals(List.of(fapp(n+5)),filled(shortcut("hello.Bar","hello.IconsBar",""),previous,"hello",others(fapp(n),fapp(n+5))));
  }
  @Test void onlyAnAutoselectedExtensionOfTheSameMainKindAndIconIsKept(){
    var n= start("hello","hello.Bar","hello.IconsBar");
    var fresh= shortcut("hello.Bar","hello.IconsBar","");
    assertEquals(List.of(fapp(n)),filled(fresh,shortcut("hello.Bar","hello.IconsBaz",fapp(n+5)),"hello"));
    assertEquals(List.of(fapp(n)),filled(fresh,shortcut("hello.Baz","hello.IconsBar",fapp(n+5)),"hello"));
    assertEquals(List.of(fapp(n)),filled(fresh,shortcut("hello.Bar","hello.IconsBar","bar"),"hello"));
    assertEquals(List.of(fapp(n)),filled(fresh,info("hello.Bar",List.of(),List.of(claim("hello.IconsBar","ffile%03d".formatted(n+5)))),"hello"));
  }
  @Test void aKeptExtensionIsNotOneTheProjectClaimsExplicitly(){
    var n= start("hello","hello.Bar","hello.IconsBar");
    var previous= info("hello.Bar",List.of(claim("hello.IconsBar","fapp007"),claim("hello.IconsBar",fapp(n+5))),List.of());
    var fresh= info("hello.Bar",List.of(claim("hello.IconsBar","fapp007"),claim("hello.IconsBar","")),List.of());
    assertEquals(List.of("fapp007",fapp(n+5)),filled(fresh,previous,"hello"));
  }
  @Test void twoShortcutsOfOneMainGetDifferentNumbers(){
    var got= filled(info("hello.Bar",List.of(claim("hello.IconsOne",""),claim("hello.IconsTwo","")),List.of()),Project.noClaims,"hello");
    assertEquals(fapp(start("hello","hello.Bar","hello.IconsOne")),got.getFirst());
    assertNotEquals(got.get(0),got.get(1));
  }
  @Test void aNumberIsNotTakenFromAnotherClaimOfTheSameProject(){
    var n= start("hello","hello.Bar","hello.IconsBar");
    var both= info("hello.Bar",List.of(claim("hello.IconsBar",""),claim("hello.IconsFoo",fapp(n))),List.of());
    assertEquals(List.of(fapp(n+1),fapp(n)),filled(both,Project.noClaims,"hello"));
  }
  @Test void noFreeExtensionLeft(){
    var all= others(IntStream.range(0,1000).mapToObj(AutoselectTest::fapp).toArray(String[]::new));
    err("""
      No free extension is left for "base.Shortcut[hello.IconsBar]" of main "hello.Bar": all the 1000 extensions "fapp000" to "fapp999" are used by the projects of this manager. Give this Shortcut an explicit extension, for example "base.Shortcut[hello.IconsBar,\\"bar\\"]".
      """,()->Project.filled(shortcut("hello.Bar","hello.IconsBar",""),Project.noClaims,"hello",Stream.of(all)));
    var openWiths= info("other.Other",List.of(),IntStream.range(0,1000).mapToObj(i->claim("other.IconsO","ffile%03d".formatted(i))).toList());
    err("""
      No free extension is left for "base.OpenWith[base.IconsConflict]" of main "hello.Foo_1": all the 1000 extensions "ffile000" to "ffile999" are used by the projects of this manager. Give this OpenWith an explicit extension, for example "base.OpenWith[base.IconsConflict,\\"foo1\\"]".
      """,()->Project.filled(info("hello.Foo_1",List.of(),List.of(claim("base.IconsConflict",""))),Project.noClaims,"hello",Stream.of(openWiths)));
    err("""
      No free extension is left for "base.Shortcut[hello.IconsBar]" of main "hello.ImageViewerApplication": all the 1000 extensions "fapp000" to "fapp999" are used by the projects of this manager. Give this Shortcut an explicit extension, for example "base.Shortcut[hello.IconsBar,\\"imageviewerappli\\"]".
      """,()->Project.filled(shortcut("hello.ImageViewerApplication","hello.IconsBar",""),Project.noClaims,"hello",Stream.of(all)));
    err("""
      No free extension is left for "base.Shortcut[hello.IconsBar]" of main "hello.Fearless": all the 1000 extensions "fapp000" to "fapp999" are used by the projects of this manager. Give this Shortcut an explicit extension, for example "base.Shortcut[hello.IconsBar,\\"ext\\"]".
      """,()->Project.filled(shortcut("hello.Fearless","hello.IconsBar",""),Project.noClaims,"hello",Stream.of(all)));
  }
}

package controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import core.TName;
import tools.Fs;

final class NamesTest{
  private static Path folder(Path dir, String name){
    var res= dir.resolve(name);
    Fs.ensureDir(res);
    return res;
  }
  @Test void aNameIsOneFearlessAcceptsForItsOwnFile(){
    assertTrue(Names.isName("my_game"));
    assertTrue(Names.isName("_x9"));
    assertFalse(Names.isName("myGame"));
    assertFalse(Names.isName("2fast"));
    assertTrue(Names.isName("a__b"));
    assertFalse(Names.isName("con"));
    assertFalse(Names.isName(""));
    assertFalse(Names.isName("a/b"));
    assertFalse(Names.isName("a.b"));
  }
  @Test void aFolderNameIsTurnedIntoANameFearlessAccepts(@TempDir Path dir){
    var project= folder(dir,"someProject");
    assertEquals("helloworld",Names.free(project,"helloWorld",Set.of()));
    assertEquals("map_a_to_pkc",Names.free(project,"map_a_to_pkc",Set.of()));
    assertEquals("a_b",Names.free(project,"a - b",Set.of()));
    assertEquals("p2fast",Names.free(project,"2fast",Set.of()));
    assertEquals("pcon",Names.free(project,"con",Set.of()));
  }
  @Test void aTakenNameGetsANumber(@TempDir Path dir){
    var project= folder(dir,"someProject");
    assertEquals("hello2",Names.free(project,"hello",Set.of("hello")));
    assertEquals("hello3",Names.free(project,"hello",Set.of("hello","hello2")));
  }
  @Test void aFreeNameIsKeptAndGetsAMarkerFile(@TempDir Path dir){
    var project= folder(dir,"someproject");
    assertEquals("someproject",Names.makeUnique(project,Set.of("other")));
    assertEquals("someproject",Names.compactName(project));
    assertTrue(Files.isRegularFile(project.resolve("someproject.fearless")));
  }
  @Test void makeUniqueIsIdempotentOnAnAlreadyMarkedFolder(@TempDir Path dir){
    var project= folder(dir,"someproject");
    Names.makeUnique(project,Set.of("other"));
    assertEquals("someproject",Names.makeUnique(project,Set.of("other")));
    assertEquals("someproject",Names.compactName(project));
  }
  @Test void anUnsafeFolderNameIsMadeAcceptableEvenWithNoCollision(@TempDir Path dir){
    var project= folder(dir,"someProject");
    assertEquals("someproject",Names.makeUnique(project,Set.of("other")));
    assertTrue(Files.isRegularFile(project.resolve("someproject.fearless")));
  }
  @Test void aTakenNameGetsANumberAndANewFearlessFile(@TempDir Path dir){
    var project= folder(dir,"someProject");
    assertEquals("someproject2",Names.makeUnique(project,Set.of("someproject")));
    assertEquals("someproject2",Names.compactName(project));
    assertFalse(Fs.readUtf8(project.resolve("someproject2.fearless")).isEmpty());
  }
  @Test void theChosenNameIsTheFolderNameMadeAcceptableAndFree(@TempDir Path dir){
    var project= folder(dir,"helloWorld");
    Fs.writeUtf8(project.resolve("start.fearless"),"");
    assertEquals("helloworld2",Names.makeUnique(project,Set.of("start","helloworld")));
    assertEquals("helloworld2",Names.compactName(project));
  }
  @Test void anExistingFearlessFileIsRenamedNotDuplicated(@TempDir Path dir){
    var project= folder(dir,"someProject");
    Fs.writeUtf8(project.resolve("start.fearless"),"kept\n");
    assertEquals("someproject",Names.makeUnique(project,Set.of("start")));
    assertFalse(Files.exists(project.resolve("start.fearless")));
    assertEquals("kept\n",Fs.readUtf8(project.resolve("someproject.fearless")));
  }
  @Test void anEmptyFearlessFileIsGivenTheMarkerText(@TempDir Path dir){
    var project= folder(dir,"someproject");
    Fs.writeUtf8(project.resolve("someproject.fearless"),"");
    assertEquals("someproject",Names.makeUnique(project,Set.of("other")));
    assertEquals(mainCoordinator.MakeDemo.markerContent,Fs.readUtf8(project.resolve("someproject.fearless")));
  }
  @Test void anEmptyFearlessFileIsRenamedAndGivenTheMarkerText(@TempDir Path dir){
    var project= folder(dir,"someProject");
    Fs.writeUtf8(project.resolve("start.fearless"),"");
    assertEquals("someproject",Names.makeUnique(project,Set.of("start")));
    assertEquals(mainCoordinator.MakeDemo.markerContent,Fs.readUtf8(project.resolve("someproject.fearless")));
  }
  @Test void markerProblemIsEmptyWhenTheMarkerMatchesTheAlias(@TempDir Path dir){
    var project= folder(dir,"someProject");
    Fs.writeUtf8(project.resolve("my_game.fearless"),"");
    assertEquals(Optional.empty(),Names.markerProblem(project,"my_game"));
  }
  @Test void markerProblemReportsAMissingMarker(@TempDir Path dir){
    var problem= Names.markerProblem(folder(dir,"someProject"),"my_game");
    assertTrue(problem.orElseThrow().contains("my_game.fearless"));
  }
  @Test void markerProblemReportsMoreThanOneMarker(@TempDir Path dir){
    var project= folder(dir,"someProject");
    Fs.writeUtf8(project.resolve("my_game.fearless"),"");
    Fs.writeUtf8(project.resolve("other.fearless"),"");
    assertTrue(Names.markerProblem(project,"my_game").orElseThrow().contains("More than one"));
  }
  @Test void everyChosenNameIsAValidFreeNameWithAValidPackageName(@TempDir Path dir){
    var taken= new HashSet<String>();
    var names= List.of("com","lpt","con","aux","a".repeat(250),"---","1abc","_","base","rank","caf\u00e9","\u65e5\u672c","\u00c4rger","Com");
    for (var i= 0; i < 12; i++){
      for (var n: names){
        var chosen= Names.makeUnique(folder(dir.resolve(i+"_"+names.indexOf(n)),n),Set.copyOf(taken));
        assertTrue(Names.isName(chosen),chosen);
        assertTrue(taken.add(chosen),chosen);
        assertTrue(TName.isPkgName(Names.pkgName(chosen)),chosen);
      }
    }
  }
  @Test void aMarkerDifferingOnlyInCaseIsNotTakenAsTheChosenName(@TempDir Path dir){
    var project= folder(dir,"hello");
    Fs.writeUtf8(project.resolve("Hello.fearless"),"kept\n");
    assertEquals("hello2",Names.makeUnique(project,Set.of()));
    assertEquals("kept\n",Fs.readUtf8(project.resolve("hello2.fearless")));
  }
  @Test void pkgNameIsTheAliasMadeAcceptable(){
    assertEquals("eclipseexample",Names.pkgName("eclipseexample"));
    assertEquals("foo",Names.pkgName("_foo"));
    assertEquals("app_base",Names.pkgName("base"));
    assertEquals("app_rank",Names.pkgName("rank"));
    assertEquals("app_",Names.pkgName("_"));
    assertTrue(core.TName.isPkgName(Names.pkgName("_")));
  }
}
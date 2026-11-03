package controller;

import static controller.Errs.same;
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

import tools.Fs;

final class NamesTest{
  private static Path folder(Path dir, String name){
    var res= dir.resolve(name);
    Fs.ensureDir(res);
    return res;
  }
  private static String named(Path folder, Set<String> taken){
    var res= Names.infer(folder, taken);
    Names.nameAs(folder, res);
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
    var project= folder(dir, "someProject");
    assertEquals("helloworld", Names.free(project, "helloWorld", Set.of()));
    assertEquals("map_a_to_pkc", Names.free(project, "map_a_to_pkc", Set.of()));
    assertEquals("a_b", Names.free(project, "a - b", Set.of()));
    assertEquals("p2fast", Names.free(project, "2fast", Set.of()));
    assertEquals("pcon", Names.free(project, "con", Set.of()));
  }
  @Test void aTakenNameGetsANumber(@TempDir Path dir){
    var project= folder(dir, "someProject");
    assertEquals("hello2", Names.free(project, "hello", Set.of("hello")));
    assertEquals("hello3", Names.free(project, "hello", Set.of("hello", "hello2")));
  }
  @Test void aFileOfTheNameTakesTheNameAndAFolderOfTheNameDoesNot(@TempDir Path dir){
    var project= folder(dir, "someProject");
    folder(project, "hello");
    assertEquals("hello", Names.free(project, "hello", Set.of()));
    Fs.writeUtf8(project.resolve("readme"), "hi\n");
    assertEquals("readme2", Names.free(project, "readme", Set.of()));
  }
  @Test void aFreeNameIsKeptAndGetsAMarkerFile(@TempDir Path dir){
    var project= folder(dir, "someproject");
    assertEquals("someproject", named(project, Set.of("other")));
    assertEquals(Optional.empty(), Names.markerProblem(project, "someproject"));
    assertTrue(Files.isRegularFile(project.resolve("someproject.fearless")));
  }
  @Test void makeUniqueIsIdempotentOnAnAlreadyMarkedFolder(@TempDir Path dir){
    var project= folder(dir, "someproject");
    named(project, Set.of("other"));
    assertEquals("someproject", named(project, Set.of("other")));
    assertEquals(Optional.empty(), Names.markerProblem(project, "someproject"));
  }
  @Test void anUnsafeFolderNameIsMadeAcceptableEvenWithNoCollision(@TempDir Path dir){
    var project= folder(dir, "someProject");
    assertEquals("someproject", named(project, Set.of("other")));
    assertTrue(Files.isRegularFile(project.resolve("someproject.fearless")));
  }
  @Test void aTakenNameGetsANumberAndANewFearlessFile(@TempDir Path dir){
    var project= folder(dir, "someProject");
    assertEquals("someproject2", named(project, Set.of("someproject")));
    assertEquals(Optional.empty(), Names.markerProblem(project, "someproject2"));
    assertFalse(Fs.readUtf8(project.resolve("someproject2.fearless")).isEmpty());
  }
  @Test void theChosenNameIsTheFolderNameMadeAcceptableAndFree(@TempDir Path dir){
    var project= folder(dir, "helloWorld");
    Fs.writeUtf8(project.resolve("start.fearless"), "");
    assertEquals("helloworld2", named(project, Set.of("start", "helloworld")));
    assertEquals(Optional.empty(), Names.markerProblem(project, "helloworld2"));
  }
  @Test void anExistingFearlessFileIsRenamedNotDuplicated(@TempDir Path dir){
    var project= folder(dir, "someProject");
    Fs.writeUtf8(project.resolve("start.fearless"), "kept\n");
    assertEquals("someproject", named(project, Set.of("start")));
    assertFalse(Files.exists(project.resolve("start.fearless")));
    assertEquals("kept\n", Fs.readUtf8(project.resolve("someproject.fearless")));
  }
  @Test void anEmptyFearlessFileIsGivenTheMarkerText(@TempDir Path dir){
    var project= folder(dir, "someproject");
    Fs.writeUtf8(project.resolve("someproject.fearless"), "");
    assertEquals("someproject", named(project, Set.of("other")));
    assertEquals(mainCoordinator.MakeDemo.markerContent, Fs.readUtf8(project.resolve("someproject.fearless")));
  }
  @Test void anEmptyFearlessFileIsRenamedAndGivenTheMarkerText(@TempDir Path dir){
    var project= folder(dir, "someProject");
    Fs.writeUtf8(project.resolve("start.fearless"), "");
    assertEquals("someproject", named(project, Set.of("start")));
    assertEquals(mainCoordinator.MakeDemo.markerContent, Fs.readUtf8(project.resolve("someproject.fearless")));
  }
  @Test void markerProblemIsEmptyWhenTheMarkerMatchesTheAlias(@TempDir Path dir){
    var project= folder(dir, "someProject");
    Fs.writeUtf8(project.resolve("my_game.fearless"), "");
    assertEquals(Optional.empty(), Names.markerProblem(project, "my_game"));
  }
  @Test void aMissingMarkerIsNoMarkerProblem(@TempDir Path dir){
    assertEquals(Optional.empty(), Names.markerProblem(folder(dir, "someProject"), "my_game"));
  }
  @Test void markerProblemReportsMoreThanOneMarker(@TempDir Path dir){
    var project= folder(dir, "someProject");
    Fs.writeUtf8(project.resolve("my_game.fearless"), "");
    Fs.writeUtf8(project.resolve("other.fearless"), "");
    same("More than one .fearless marker file was found in\n[###]someProject\nA project folder holds exactly one, and its name is the project name: keep only \"my_game.fearless\".", Names.markerProblem(project, "my_game").orElseThrow());
  }
  @Test void everyChosenNameIsAValidFreeName(@TempDir Path dir){
    var taken= new HashSet<String>();
    var names= List.of("com", "lpt", "con", "aux", "a".repeat(250), "---", "1abc", "_", "base", "rank", "caf\u00e9", "\u65e5\u672c", "\u00c4rger", "Com");
    for (var i= 0; i < 12; i += 1){
      for (var n : names){
        var chosen= named(folder(dir.resolve(i+"_"+names.indexOf(n)), n), Set.copyOf(taken));
        assertTrue(Names.isName(chosen), chosen);
        assertTrue(taken.add(chosen), chosen);
      }
    }
  }
  @Test void aMarkerDifferingOnlyInCaseIsRenamed(@TempDir Path dir){
    var project= folder(dir, "hello");
    Fs.writeUtf8(project.resolve("Hello.fearless"), "kept\n");
    assertEquals("hello", named(project, Set.of()));
    assertEquals(List.of(project.resolve("hello.fearless")), Names.list(project));
    assertEquals("kept\n", Fs.readUtf8(project.resolve("hello.fearless")));
    assertEquals(Optional.empty(), Names.markerProblem(project, "hello"));
  }
  @Test void theNameIsTheFirstMarkerInFileNameOrderAndTheOtherMarkersStay(@TempDir Path dir){
    var project= folder(dir, "someProject");
    Fs.writeUtf8(project.resolve("zeta.fearless"), "z\n");
    Fs.writeUtf8(project.resolve("alpha.fearless"), "a\n");
    assertEquals("alpha", Names.marker(project));
    assertEquals("alpha", named(project, Set.of()));
    assertEquals("alpha", named(project, Set.of()));
    assertEquals(List.of(project.resolve("alpha.fearless"), project.resolve("zeta.fearless")), Names.list(project));
  }
  @Test void aTakenFirstMarkerIsRenamedAndTheOtherMarkersStay(@TempDir Path dir){
    var project= folder(dir, "someProject");
    Fs.writeUtf8(project.resolve("zeta.fearless"), "z\n");
    Fs.writeUtf8(project.resolve("alpha.fearless"), "a\n");
    assertEquals("someproject", named(project, Set.of("alpha")));
    assertEquals(List.of(project.resolve("someproject.fearless"), project.resolve("zeta.fearless")), Names.list(project));
    assertEquals("a\n", Fs.readUtf8(project.resolve("someproject.fearless")));
    assertEquals("someproject", named(project, Set.of("alpha")));
  }
  @Test void inferringANameChangesNoFile(@TempDir Path dir){
    var project= folder(dir, "someProject");
    Fs.writeUtf8(project.resolve("start.fearless"), "");
    assertEquals("someproject", Names.infer(project, Set.of("start")));
    assertEquals(List.of(project.resolve("start.fearless")), Names.list(project));
  }
  @Test void aFolderNamedLikeTheMarkerTakesTheNameAndAFolderNamedLikeTheNameDoesNot(@TempDir Path dir){
    var proj= dir.resolve("proj");
    Fs.ensureDir(proj.resolve("proj.fearless"));
    Fs.ensureDir(proj.resolve("proj2"));
    Fs.writeUtf8(proj.resolve("Proj.fearless"), "");
    var name= Names.infer(proj, java.util.Set.of());
    assertEquals("proj2", name);
    Names.nameAs(proj, name);
    assertTrue(Files.isRegularFile(proj.resolve("proj2.fearless")));
  }
}

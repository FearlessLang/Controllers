package controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import controller.Registry.Entry;
import controller.Registry.Kind;
import tools.Fs;

final class EclipseTest{
  static Project project(String alias, Path path, Kind kind, Optional<Map<String,String>> mains, String job, int runs, String lastRun){
    var facts= new Facts(0,0,-1,List.of(),true,true,Optional.empty(),List.of(),Optional.empty());
    return new Project(new Entry(alias,path,kind,List.of(),Map.of(),Map.of(),-1,-1),facts,mains,Optional.empty(),job,Instant.EPOCH,runs,lastRun,-1);
  }
  private static Project project(String alias, Path path){ return project(alias,path,Kind.code,Optional.empty(),"",0,""); }
  private static final String sourceError= """
In file: fear:/_pkb/_rank_app200.fear

002| B:{.text:Str->a.C.text;}
   |               ^^^^^^^^^^

While inspecting a type name
Package "nonexistentpkg" does not exist.
Visible packages: "base".
Error 7 WellFormedness
""";
  @Test void everyKnownProjectIsListedAsItsAliasThenItsPathAndReportsSitByAlias(@TempDir Path dir){
    var eclipse= new Eclipse(dir.resolve("eclipse"));
    var one= dir.resolve("one");
    var two= dir.resolve("with space");
    eclipse.publish(Eclipse.listing(List.of(project("one",one),project("two",two))));
    assertEquals("one "+one+"\ntwo "+two+"\n",Fs.readUtf8(dir.resolve("eclipse").resolve("projects.txt")));
    assertEquals(dir.resolve("eclipse").resolve("two").resolve("console.txt"),eclipse.console("two"));
  }
  @Test void theStateIsInfoWithTheKindTheCacheTheJobTheRunsTheLastExitThenEveryMainWithItsFile(){
    var mains= Map.of("hello.Hello","_hello/_rank_app.fear");
    assertEquals("""
      {
        "kind": "code",
        "needsCompiling": "false",
        "busy": "hello.Hello",
        "running": "hello.Hello",
        "runs": "3",
        "lastRun": "hello.Hello",
        "exit": "-1",
        "mains": {
          "hello.Hello": "_hello/_rank_app.fear"
        }
      }
      """.stripIndent(),Eclipse.stateText(project("a",Path.of("a"),Kind.code,Optional.of(mains),"hello.Hello",3,"hello.Hello")));
  }
  @Test void compilingIsAJobButNotARunningMain(){
    var state= Eclipse.stateText(project("a",Path.of("a"),Kind.code,Optional.empty(),Project.compiling,0,""));
    assertEquals(true,state.contains("\"busy\": \"compiling\",\n  \"running\": \"\","));
  }
  @Test void aDataProjectWithNoKnownMainHasEmptyStrings(){
    assertEquals("""
      {
        "kind": "data:readOnly",
        "needsCompiling": "false",
        "busy": "",
        "running": "",
        "runs": "0",
        "lastRun": "",
        "exit": "-1",
        "mains": {}
      }
      """.stripIndent(),Eclipse.stateText(project("a",Path.of("a"),Kind.dataReadOnly,Optional.empty(),"",0,"")));
  }
  @Test void theStateOfAProjectIsWrittenInItsReportsFolder(@TempDir Path dir){
    var eclipse= new Eclipse(dir.resolve("eclipse"));
    var p= project("one",dir.resolve("one"));
    eclipse.state(p);
    assertEquals(Eclipse.stateText(p),Fs.readUtf8(dir.resolve("eclipse").resolve("one").resolve("state.txt")));
  }
  @Test void aSourceErrorBecomesItsPathItsLineThenTheWholeMessage(@TempDir Path project){
    Eclipse.problems(project,sourceError);
    assertEquals("_pkb/_rank_app200.fear\n002\n"+sourceError,Fs.readUtf8(project.resolve("problems.txt")));
  }
  @Test void aCompileThatSucceedsClearsTheProblem(@TempDir Path project){
    Eclipse.problems(project,sourceError);
    Eclipse.problems(project,"");
    assertEquals("",Fs.readUtf8(project.resolve("problems.txt")));
  }
  @Test void anErrorWithNoSourcePositionMarksNothing(@TempDir Path project){
    Eclipse.problems(project,"The fearless project folder contains no *.fear files\n");
    assertEquals("",Fs.readUtf8(project.resolve("problems.txt")));
  }
  @Test void aPositionQuotedInsideALongerErrorIsStillFound(@TempDir Path project){
    var wrapped= "Broken reference in a doc comment.\n\n"+sourceError;
    Eclipse.problems(project,wrapped);
    assertEquals("_pkb/_rank_app200.fear\n002\n"+wrapped,Fs.readUtf8(project.resolve("problems.txt")));
  }
}

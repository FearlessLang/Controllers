package controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
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
  static Project project(String alias, Kind kind, Optional<Map<String,String>> mains, String job, int runs, String lastRun, String failure){
    var facts= new Facts(0,0,-1,List.of(),true,true,Optional.empty(),List.of(),Optional.empty());
    return new Project(new Entry(alias,Path.of("/p").resolve(alias),kind,List.of(),Map.of(),Map.of(),-1,-1),facts,mains,Optional.empty(),job,Instant.EPOCH,runs,lastRun,-1,failure);
  }
  private static final String sourceError= """
In file: fear:/_pkb/_rank_app200.fear

002| B:{.text:Str->a.C.text;}
   |               ^^^^^^^^^^

While inspecting a type name
Package "nonexistentpkg" does not exist.
Visible packages: "base".
Error 7 WellFormedness
""";
  private static String problem(String failure){
    var o= (Info.Obj)Info.parse(Eclipse.state(List.of(project("a",Kind.code,Optional.empty(),"",0,"",failure))),Path.of("state.info").toUri());
    var p= (Info.Obj)((Info.Obj)o.field("a").orElseThrow().value()).field("problem").orElseThrow().value();
    return String.join("|",p.fields().stream().map(f->f.key()+"="+((Info.Str)f.value()).value()).toList());
  }
  @Test void theStateHoldsEveryProjectByNameWithItsFolderKindRunsMainsAndProblem(){
    var hello= project("hello",Kind.code,Optional.of(Map.of("hello.Hello","_hello/_rank_app.fear")),"hello.Hello",3,"hello.Hello","");
    var data= project("data",Kind.dataReadOnly,Optional.empty(),"",0,"","");
    assertEquals("""
      {
        "hello": {
          "folder": "%s",
          "kind": "code",
          "running": "hello.Hello",
          "runs": "3",
          "lastRun": "hello.Hello",
          "exit": "-1",
          "mains": {
            "hello.Hello": "_hello/_rank_app.fear"
          },
          "problem": {}
        },
        "data": {
          "folder": "%s",
          "kind": "data:readOnly",
          "running": "",
          "runs": "0",
          "lastRun": "",
          "exit": "-1",
          "mains": {},
          "problem": {}
        }
      }
      """.stripIndent().formatted(hello.folder().toString().replace("\\","\\\\"),data.folder().toString().replace("\\","\\\\")),Eclipse.state(List.of(hello,data)));
  }
  @Test void compilingIsNotARunningMain(){
    assertEquals(true,Eclipse.state(List.of(project("a",Kind.code,Optional.empty(),Project.compiling,0,"",""))).contains("\"running\": \"\","));
  }
  @Test void aFailedCompileWithASourcePositionIsTheProblemAtThatFileAndLine(){
    assertEquals("file=_pkb/_rank_app200.fear|line=002|message="+sourceError,problem(sourceError));
  }
  @Test void aPositionQuotedInsideALongerErrorIsStillFound(){
    var wrapped= "Broken reference in a doc comment.\n\n"+sourceError;
    assertEquals("file=_pkb/_rank_app200.fear|line=002|message="+wrapped,problem(wrapped));
  }
  @Test void aFailedCompileWithNoSourcePositionIsNoProblem(){
    assertEquals("",problem("The fearless project folder contains no *.fear files\n"));
  }
  @Test void theStateIsReplacedWhole(@TempDir Path dir){
    var eclipse= new Eclipse(dir);
    eclipse.publish("{}\n");
    eclipse.publish("{\n}\n");
    assertEquals("{\n}\n",Fs.readUtf8(dir.resolve("state.info")));
    assertEquals(List.of(dir.resolve("state.info")),Fs.of(()->{ try(var s= Files.list(dir)){ return s.toList(); } }));
  }
}

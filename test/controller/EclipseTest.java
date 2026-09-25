package controller;

import static controller.Errs.same;
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
    return String.join("|",p.fields().stream().map(f->f.key()+"="+message(f)).toList());
  }
  private static String message(Info.Obj.Field f){
    var s= ((Info.Str)f.value()).value();
    return f.key().equals("message") ? TaggedText.read(s,Messages::infoError) : s;
  }
  @Test void theStateHoldsEveryProjectByNameWithItsFolderKindRunsMainsAndProblem(){
    var hello= project("hello",Kind.code,Optional.of(Map.of("hello.Hello","_hello/_rank_app.fear")),"hello.Hello",3,"hello.Hello","");
    var data= project("data",Kind.dataReadOnly,Optional.empty(),"",0,"","");
    assertEquals("""
      {
        "hello": {
          "folder": "Str:%s",
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
          "folder": "Str:%s",
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
  private static Path eclipseAt(Path folder){
    Fs.writeUtf8(folder.resolve(".eclipseproduct"),"name=Eclipse Platform\n");
    Fs.writeUtf8(folder.resolve("eclipse.exe"),"");
    return folder;
  }
  @Test void anEclipseIsFoundFromItsProgramItsFolderOrTheFolderItWasUnzippedInto(@TempDir Path dir){
    var eclipse= eclipseAt(dir.resolve("eclipse-java-2025-12-R-win32-x86_64").resolve("eclipse"));
    var unzipped= eclipse.getParent();
    assertEquals(List.of(eclipse),Eclipse.installs(eclipse));
    assertEquals(List.of(eclipse),Eclipse.installs(unzipped));
    assertEquals(List.of(eclipse),Eclipse.installs(dir));
    var mac= eclipseAt(dir.resolve("Eclipse.app").resolve("Contents").resolve("Eclipse"));
    assertEquals(List.of(mac),Eclipse.installs(dir.resolve("Eclipse.app")));
    assertEquals(List.of(mac,eclipse),Eclipse.installs(dir));
    Fs.ensureDir(dir.resolve("none").resolve("inner"));
    assertEquals(List.of(),Eclipse.installs(dir.resolve("none")));
  }
  @Test void connectingNamesWhatIsWrongWithTheChoice(@TempDir Path dir){
    var eclipse= new Eclipse(dir.resolve("eclipse"));
    Fs.ensureDir(dir.resolve("empty"));
    same("""
      Eclipse is not connected: no Eclipse installation, a folder holding the file ".eclipseproduct", is in
        [###]empty
      or in its folders "eclipse" or "Contents/Eclipse", or in those of a folder of it.

      Select the Eclipse program, the folder holding it, or the folder Eclipse was unzipped into.""",eclipse.connect(dir.resolve("empty"),dir));
    eclipseAt(dir.resolve("two").resolve("a").resolve("eclipse"));
    eclipseAt(dir.resolve("two").resolve("b").resolve("eclipse"));
    same("""
      Eclipse is not connected: more than one Eclipse installation is in
        [###]two
      They are:
        [###]a[###]eclipse
        [###]b[###]eclipse

      Select the Eclipse program, or the folder holding it, of the one to connect.""",eclipse.connect(dir.resolve("two"),dir));
  }
  @Test void theStateIsReplacedWhole(@TempDir Path dir){
    var eclipse= new Eclipse(dir);
    eclipse.publish("{}\n");
    eclipse.publish("{\n}\n");
    assertEquals("{\n}\n",Fs.readUtf8(dir.resolve("state.info")));
    assertEquals(List.of(dir.resolve("state.info")),Fs.of(()->{ try(var s= Files.list(dir)){ return s.toList(); } }));
  }
}

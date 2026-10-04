package controller;

import static controller.Errs.err;
import static controller.Errs.same;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import controller.Registry.Entry;
import controller.Registry.Kind;
import fileSupport.Info;
import tools.Fs;

final class RegistryTest{
  private static final URI uri= URI.create("test:projects.info");
  private static final String root= Path.of("").toAbsolutePath().getRoot().toString().replace('\\','/');
  private static List<Entry> parse(String text){ return Registry.fromInfo(text,Info.parse(text,uri),Registry::real,Path.of(root+"manager")); }
  private static Registry registry(Path dir){ return new Registry(folder(dir,"manager")); }
  private static Path folder(Path dir, String name){
    var res= dir.resolve(name);
    Fs.ensureDir(res);
    return res;
  }
  private static String unix(Path p){ return p.toAbsolutePath().normalize().toString().replace('\\','/'); }

  //-- the schema
  @Test void anEmptyFileIsNoProjects(){ assertEquals(List.of(),parse("{}")); }
  @Test void aMinimalProjectHasAPathAndAKindWithNoMainsReadsOrEdits(){
    var e= parse("{\"someproject\":{\"path\":\"Str:"+root+"abs/someproject\",\"kind\":\"idle\"}}").getFirst();
    assertEquals(new Entry("someproject",Path.of(root+"abs/someproject"),Kind.idle,List.of(),Map.of(),Map.of(),-1,-1),e);
  }
  @Test void aFullCodeProjectParsesEveryField(){
    var e= parse("""
      {"someproject":{
        "path":"Str:%sabs/someproject",
        "kind":"code",
        "mains":["some.Main1","some.Main2"],
        "reads":{"publicfiles":["Data1"]},
        "edits":{"publicdata":["Data2","Data3"]}
      }}""".formatted(root)).getFirst();
    assertEquals(new Entry("someproject",Path.of(root+"abs/someproject"),Kind.code,
      List.of("some.Main1","some.Main2"),Map.of("publicfiles",List.of("Data1")),Map.of("publicdata",List.of("Data2","Data3")),-1,-1),e);
  }
  @Test void allFourKindsParse(){
    assertEquals(Kind.idle,parse("{\"a\":{\"path\":\"Str:"+root+"a\",\"kind\":\"idle\"}}").getFirst().kind());
    assertEquals(Kind.code,parse("{\"a\":{\"path\":\"Str:"+root+"a\",\"kind\":\"code\"}}").getFirst().kind());
    assertEquals(Kind.dataReadOnly,parse("{\"a\":{\"path\":\"Str:"+root+"a\",\"kind\":\"data:readOnly\"}}").getFirst().kind());
    assertEquals(Kind.dataReadWrite,parse("{\"a\":{\"path\":\"Str:"+root+"a\",\"kind\":\"data:readWrite\"}}").getFirst().kind());
  }
  @Test void theWholeFileMustBeAnObject(){
    err("[###]The whole file must be an object {...} mapping each project name to the metadata of that project.[###]",()->parse("\"not an object\""));
  }
  @Test void aProjectNameMustBeShapedLikeAFolderName(){
    err("[###]\"Not Valid\" is not a valid project name[###]",()->parse("{\"Not Valid\":{\"path\":\"Str:"+root+"a\"}}"));
  }
  @Test void anUnknownAttributeIsRejected(){
    err("[###]Unknown project attribute \"bogus\": the attributes of a project are \"path\", \"kind\", \"mains\", \"reads\", \"edits\".[###]",()->parse("{\"a\":{\"path\":\"Str:"+root+"a\",\"bogus\":\"1\"}}"));
  }
  @Test void aMissingPathIsRejected(){
    err("[###]Project \"a\" is missing its \"path\"[###]",()->parse("{\"a\":{\"kind\":\"idle\"}}"));
  }
  @Test void aRelativePathIsRejected(){
    err("[###]\"path\" must be an absolute path[###]",()->parse("{\"a\":{\"path\":\"Str:relative/path\"}}"));
  }
  @Test void aMissingKindIsRejected(){
    err("[###]Project \"a\" is missing its \"kind\": one of \"idle\", \"code\", \"data:readOnly\" or \"data:readWrite\".[###]",()->parse("{\"a\":{\"path\":\"Str:"+root+"a\"}}"));
  }
  @Test void anUnknownKindIsRejectedWithTheFourValidOptionsListed(){
    err("[###]\"kind\" must be one of \"idle\", \"code\", \"data:readOnly\" or \"data:readWrite\", not \"nonsense\".[###]",()->parse("{\"a\":{\"path\":\"Str:"+root+"a\",\"kind\":\"nonsense\"}}"));
  }
  @Test void repeatedMainsAreRejected(){
    err("[###]\"a.Main1\" is repeated in \"mains\".[###]",()->parse("{\"a\":{\"path\":\"Str:"+root+"a\",\"mains\":[\"a.Main1\",\"a.Main1\"]}}"));
  }
  @Test void aMainMustBePackageThenType(){
    err("[###]is not a Fearless main name[###]",()->parse("{\"a\":{\"path\":\"Str:"+root+"a\",\"mains\":[\"Hello1\"]}}"));
    err("[###]is not a Fearless main name[###]",()->parse("{\"a\":{\"path\":\"Str:"+root+"a\",\"mains\":[\"Hello.Hello1\"]}}"));
    err("[###]is not a Fearless main name[###]",()->parse("{\"a\":{\"path\":\"Str:"+root+"a\",\"mains\":[\"hello.hello1\"]}}"));
  }
  @Test void mainsAreThePackageQualifiedNamesTheCompilerReports(){
    assertEquals(List.of("hello.Hello1","hello.Hello3"),parse("{\"a\":{\"path\":\"Str:"+root+"a\",\"kind\":\"code\",\"mains\":[\"hello.Hello1\",\"hello.Hello3\"]}}").getFirst().mains());
  }
  @Test void aReadsTargetTakesTypeNamesNotPackageQualifiedNames(){
    err("[###]is not a Fearless type name[###]",()->parse("{\"a\":{\"path\":\"Str:"+root+"a\",\"reads\":{\"b\":[\"hello.Data1\"]}}}"));
  }
  @Test void identicalOrNestedPathsAreRejected(){
    err("[###]\"b\" has the same path as \"a\", or one is inside the other[###]",()->parse("{\"a\":{\"path\":\"Str:"+root+"same\",\"kind\":\"idle\"},\"b\":{\"path\":\"Str:"+root+"same\",\"kind\":\"idle\"}}"));
    err("[###]\"b\" has the same path as \"a\", or one is inside the other[###]",()->parse("{\"a\":{\"path\":\"Str:"+root+"parent\",\"kind\":\"idle\"},\"b\":{\"path\":\"Str:"+root+"parent/child\",\"kind\":\"idle\"}}"));
  }
  @Test void aLinkNamesTheFolderItLinksTo(@TempDir Path dir){
    Assumptions.assumeFalse(Fs.isWindows());
    var real= Registry.real(folder(dir,"real"));
    var link= Fs.of(()->Files.createSymbolicLink(dir.resolve("link"),real));
    err("[###]\"b\" has the same path as \"a\", or one is inside the other[###]",()->parse("{\"a\":{\"path\":\"Str:"+unix(real)+"\",\"kind\":\"idle\"},\"b\":{\"path\":\"Str:"+unix(link)+"\",\"kind\":\"idle\"}}"));
    assertEquals(real,parse("{\"a\":{\"path\":\"Str:"+unix(link)+"\",\"kind\":\"idle\"}}").getFirst().path());
  }
  @Test void toInfoThenFromInfoRoundTripsAnEntry(){
    var e= new Entry("someproject",Path.of(root+"abs/someproject"),Kind.code,List.of("some.Main1"),Map.of("publicfiles",List.of("Data1")),Map.of(),999,999);
    var back= parse(Info.print(Registry.toInfo(List.of(e)))).getFirst();
    assertEquals(new Entry("someproject",Path.of(root+"abs/someproject"),Kind.code,List.of("some.Main1"),Map.of("publicfiles",List.of("Data1")),Map.of(),-1,-1),back);
  }

  //-- persistence
  @Test void noFileYetIsNoRegisteredFolder(@TempDir Path dir){ assertEquals(List.of(),registry(dir).all()); }
  @Test void addedFolderIsIdleWithNoTimesAndNoLinks(@TempDir Path dir){
    var project= folder(dir,"someproject");
    registry(dir).add("someproject",project,Kind.idle);
    assertEquals(List.of(new Entry("someproject",project.toAbsolutePath().normalize(),Kind.idle,List.of(),Map.of(),Map.of(),-1,-1)),registry(dir).all());
  }
  @Test void addingARegisteredFolderOrNameAgainIsABug(@TempDir Path dir){
    var project= folder(dir,"someproject");
    var r= registry(dir);
    r.add("someproject",project,Kind.idle);
    assertThrows(AssertionError.class,()->r.add("again",project.resolve("..").resolve("someproject"),Kind.idle));
    assertThrows(AssertionError.class,()->r.add("someproject",folder(dir,"other"),Kind.idle));
    assertEquals(1,registry(dir).all().size());
  }
  @Test void aForgottenFolderForgetsItsTimesToo(@TempDir Path dir){
    var project= folder(dir,"someproject");
    var r= registry(dir);
    r.add("someproject",project,Kind.idle);
    r.update(project,e->e.withTimes(111,e.run()));
    r.remove(project);
    r.add("someproject",project,Kind.idle);
    assertEquals(-1,r.all().getFirst().compiled());
  }
  @Test void compileRunTimesAndMainsSurviveAReRead(@TempDir Path dir){
    var project= folder(dir,"someproject");
    var r= registry(dir);
    r.add("someproject",project,Kind.idle);
    r.update(project,e->e.withTimes(111,e.run()));
    r.update(project,e->e.withTimes(e.compiled(),222));
    r.update(project,e->e.withMains(List.of("hello.Hello1")));
    var reread= registry(dir).all().getFirst();
    assertEquals(111,reread.compiled());
    assertEquals(222,reread.run());
    assertEquals(List.of("hello.Hello1"),reread.mains());
  }
  @Test void forgettingAFolderRemovesOnlyThatOne(@TempDir Path dir){
    var kept= folder(dir,"kept");
    var gone= folder(dir,"gone");
    var r= registry(dir);
    r.add("kept",kept,Kind.idle);
    r.add("gone",gone,Kind.idle);
    r.remove(gone);
    assertEquals(List.of(kept.toAbsolutePath().normalize()),r.all().stream().map(Entry::path).toList());
    assertEquals(Optional.empty(),r.of(gone));
  }
  @Test void aFolderInsideOrAroundARegisteredOneIsFound(@TempDir Path dir){
    var project= folder(dir,"someproject");
    var r= registry(dir);
    r.add("someproject",project,Kind.idle);
    assertEquals(project,r.overlapping(project.resolve("inside")).orElseThrow());
    assertEquals(project,r.overlapping(dir).orElseThrow());
    assertEquals(Optional.empty(),r.overlapping(folder(dir,"otherproject")));
    assertEquals(Optional.empty(),r.overlapping(project));
  }
  @Test void aSiblingNamedLikeAPrefixIsNotNested(@TempDir Path dir){
    var r= registry(dir);
    r.add("some",folder(dir,"some"),Kind.idle);
    assertEquals(Optional.empty(),r.overlapping(folder(dir,"someproject")));
  }
  @Test void awkwardFolderNamesSurviveAReRead(@TempDir Path dir){
    var project= folder(dir,"a name with spaces");
    registry(dir).add("spacey",project,Kind.idle);
    assertEquals(project.toAbsolutePath().normalize(),registry(dir).all().getFirst().path());
  }
  @Test void aFolderNamedWithABackslashOrANewlineSurvivesAReReadWithItsTimes(@TempDir Path dir){
    Assumptions.assumeFalse(Fs.isWindows());
    var slash= folder(dir,"a\\b");
    var line= folder(dir,"c\nd");
    var r= registry(dir);
    r.add("slash",slash,Kind.idle);
    r.add("line",line,Kind.idle);
    r.update(slash,e->e.withTimes(111,222));
    r.update(line,e->e.withTimes(333,444));
    assertEquals(r.all(),registry(dir).all());
  }
  private static void activity(Path dir, String text, String expected){
    Fs.writeUtf8(dir.resolve("manager").resolve("activity.txt"),text);
    err("In "+dir.resolve("manager").resolve("activity.txt")+", line 2:\n"+expected,()->registry(dir));
  }
  @Test void eachMalformedLineOfActivityTxtIsAFatalErrorNamingTheLine(@TempDir Path dir){
    var r= registry(dir);
    r.add("someproject",folder(dir,"someproject"),Kind.idle);
    var project= r.all().getFirst().path();
    var line= "1 2 "+TaggedText.line(project.toString())+"\n";
    var shape= " is malformed: a line is the time of the last compile, a space, the time of the last run, a space, then the project folder as a tagged text; a time is -1 for never, else the milliseconds since 1970, in at most 18 digits.";
    activity(dir,line+"1 2\n","\"1 2\""+shape);
    activity(dir,line+"1 x Str:/a\n","\"1 x Str:/a\""+shape);
    activity(dir,line+"1 1234567890123456789 Str:/a\n","\"1 1234567890123456789 Str:/a\""+shape);
    activity(dir,line+"\n","\"\""+shape);
    activity(dir,line+"1 2 /a\n","\"/a\" is malformed: it starts with \"Str:\", \"UStr:\" or \"Base16:\", then the text written that way.");
    activity(dir,line+"1 2 "+TaggedText.of("/a\u0000b")+"\n","\"/a\" [Null 0x00] \"b\" is not a path this system accepts: [###].");
    activity(dir,line+line,"The project folder\n"+project+"\nis also on line 1: a project folder is on one line only.");
    Fs.writeUtf8(dir.resolve("manager").resolve("activity.txt"),line);
    assertEquals(List.of(1L,2L),List.of(registry(dir).all().getFirst().compiled(),registry(dir).all().getFirst().run()));
  }
  @Test void changingTheLinksKeepsTheirOrderInTheFile(@TempDir Path dir){
    var code= folder(dir,"code");
    var r= registry(dir);
    r.commit(Registry.text(r.all()),"{\"code\": {\"path\": \"Str:"+unix(code)+"\", \"kind\": \"code\", \"reads\": {\"zeta\": [\"Z\"], \"alpha\": [\"A\"], \"mid\": [\"M\"], \"beta\": [\"B\"]}}}");
    r.update(code,e->e.withLinks(e.reads(),Map.of("zeta",List.of("W"))));
    assertEquals("""
      {
        "code": {
          "path": "Str:%s",
          "kind": "code",
          "reads": {
            "zeta": ["Z"],
            "alpha": ["A"],
            "mid": ["M"],
            "beta": ["B"]
          },
          "edits": {
            "zeta": ["W"]
          }
        }
      }
      """.formatted(unix(code)),Fs.readUtf8(folder(dir,"manager").resolve("projects.info")));
  }
  @Test void aCorruptedFileRefusesToLoadWithARichError(@TempDir Path dir){
    Fs.writeUtf8(folder(dir,"manager").resolve("projects.info"),"not info at all");
    err("[###]Expected a string \"...\", a list [...] or an object {...} here.[###]",()->registry(dir).all());
  }
  @Test void theAllowedSystemExtensionsSurviveAReReadSortedAndAMalformedListRefusesToLoad(@TempDir Path dir){
    registry(dir).extensions(List.of("pdf","htm"));
    assertEquals(List.of("htm","pdf"),registry(dir).extensions());
    var file= folder(dir,"manager").resolve("extensions.info");
    Fs.writeUtf8(file,"[\"htm\", \"fapp001\"]");
    err("[###]\"fapp001\" in \"extensions.info\" is not a system extension: 1 to 16 lowercase letters or digits, other than \"fearless\", \"fapp000\" to \"fapp999\" and \"ffile000\" to \"ffile999\".[###]",()->registry(dir));
    Fs.writeUtf8(file,"[\"htm\", \"htm\"]");
    err("[###]\"htm\" is repeated in \"extensions.info\".[###]",()->registry(dir));
    Fs.writeUtf8(file,"{}");
    err("[###]\"extensions.info\" must be a list [...] of strings.[###]",()->registry(dir));
  }
  @Test void commitWritesAValidatedFileAndRejectsLeavingDiskUnchanged(@TempDir Path dir){
    var project= folder(dir,"someproject");
    var r= registry(dir);
    r.commit(Registry.text(r.all()),"{\n  \"someproject\": {\"path\": \"Str:"+unix(project)+"\", \"kind\": \"code\"}\n}\n");
    assertEquals(Kind.code,r.all().getFirst().kind());
    var before= Registry.text(r.all());
    assertThrows(Registry.Refused.class,()->r.commit(Registry.text(r.all()),"{\n  \"someproject\": {\"path\": \"Str:"+unix(project)+"\", \"kind\": \"nonsense\"}\n}\n"));
    assertEquals(before,Registry.text(registry(dir).all()));
  }
  private static String inManager(Path folder, Path manager){
    return "[###]\"path\": \"Str:"+unix(folder)+"\"[###]^^^^^^\n\nWhile inspecting the file\nFearless cannot keep track of this folder as a project.\n\nThe folder is:\n  "+folder+"\nThe manager folder of this Fearless is:\n  "+manager+"\n[###]";
  }
  private static void placedAt(Registry r, Path folder, String expected){
    var text= "{\n  \"x\": {\"path\": \"Str:"+unix(folder)+"\", \"kind\": \"code\"}\n}\n";
    same(expected,assertThrows(Registry.Refused.class,()->r.commit(Registry.text(r.all()),text)).getMessage());
    assertEquals(List.of(),r.all());
  }
  @Test void aCommitPlacingAProjectAtARootOrInOrAroundTheManagerFolderIsRefusedAtThePath(@TempDir Path dir){
    var r= registry(dir);
    var manager= Registry.real(dir.resolve("manager"));
    placedAt(r,Path.of(root),"[###]\"path\": \"Str:"+root+"\"[###]^^^^^^^\n\nWhile inspecting the file\nFearless cannot keep track of the root of a drive or of the file system as a\nproject.\n[###]");
    placedAt(r,manager,inManager(manager,manager));
    placedAt(r,folder(manager,"inside"),inManager(manager.resolve("inside"),manager));
    placedAt(r,Registry.real(dir),inManager(Registry.real(dir),manager));
  }
  @Test void aProjectsInfoPlacingAProjectInTheManagerFolderIsAFatalErrorAtThePath(@TempDir Path dir){
    var manager= folder(dir,"manager");
    Fs.writeUtf8(manager.resolve("projects.info"),"{\n  \"x\": {\"path\": \"Str:"+unix(manager)+"\", \"kind\": \"code\"}\n}\n");
    err(inManager(Registry.real(manager),Registry.real(manager)),()->registry(dir));
  }
  @Test void aTypeNameIsInReadsOrInEditsNotInBoth(){
    err("""
      [###]"edits":{"pub":["Data2","Data1"]}[###]
      [###]"Data1" is in both "reads"."pub" and "edits"."pub": a type name in "edits" also reads, so it is not repeated in "reads"; a type name in "reads" only reads.[###]""",
      ()->parse("{\"a\":{\"path\":\"Str:"+root+"a\",\"kind\":\"code\",\"reads\":{\"pub\":[\"Data1\"]},\"edits\":{\"pub\":[\"Data2\",\"Data1\"]}}}"));
  }
  @Test void aLinkNamesAType(){
    err("[###]\"reads\".\"pub\" names no type: a link names the one or more type names the code uses for \"pub\".[###]",()->parse("{\"a\":{\"path\":\"Str:"+root+"a\",\"kind\":\"code\",\"reads\":{\"pub\":[]}}}"));
  }
  @Test void readsAndEditsSurviveAReRead(@TempDir Path dir){
    var code= folder(dir,"mycode");
    var pub= folder(dir,"pub");
    var r= registry(dir);
    r.add("mycode",code,Kind.idle);
    r.add("pub",pub,Kind.idle);
    r.update(code,e->e.withKind(Kind.code));
    r.update(pub,e->e.withKind(Kind.dataReadWrite));
    r.update(code,e->e.withLinks(Map.of("pub",List.of("Data2","Data3")),Map.of("pub",List.of("Data1"))));
    var reread= registry(dir).of(code).orElseThrow();
    assertEquals(Map.of("pub",List.of("Data2","Data3")),reread.reads());
    assertEquals(Map.of("pub",List.of("Data1")),reread.edits());
  }

  //-- link checks
  private static Entry link(String alias, Path path, Kind kind, Map<String,List<String>> reads, Map<String,List<String>> edits){
    return new Entry(alias,path,kind,List.of(),reads,edits,-1,-1);
  }
  private static Path readme(Path dir, String name){
    var res= dir.resolve(name);
    Fs.writeUtf8(res.resolve("readme"),"hi\n");
    return res;
  }
  private Optional<String> linkProblem(Path dir, Entry e, Entry... others){
    var r= registry(dir);
    for (var o: others){ r.add(o.alias(),o.path(),Kind.idle); r.update(o.path(),_->o); }
    r.add(e.alias(),e.path(),Kind.idle);
    r.update(e.path(),_->e);
    return r.linkProblem(e,_->Optional.empty());
  }
  @Test void aNonCodeEntryNeverHasALinkProblem(@TempDir Path dir){
    var e= link("a",readme(dir,"a"),Kind.idle,Map.of("missing",List.of("X")),Map.of());
    assertEquals(Optional.empty(),linkProblem(dir,e));
  }
  @Test void readingARegisteredReadOnlyAndEditingAReadWriteAreFine(@TempDir Path dir){
    var pub= link("pub",readme(dir,"pub"),Kind.dataReadWrite,Map.of(),Map.of());
    var code= link("code",readme(dir,"code"),Kind.code,Map.of("pub",List.of("Data1")),Map.of("pub",List.of("Data2")));
    assertEquals(Optional.empty(),linkProblem(dir,code,pub));
  }
  @Test void readingFromAMissingAliasIsADeadLink(@TempDir Path dir){
    var code= link("code",readme(dir,"code"),Kind.code,Map.of("nosuchproject",List.of("Data1")),Map.of());
    assertTrue(linkProblem(dir,code).orElseThrow().contains("no project called \"nosuchproject\" is registered"));
  }
  @Test void editingAReadOnlyDataProjectIsRejected(@TempDir Path dir){
    var pub= link("pub",readme(dir,"pub"),Kind.dataReadOnly,Map.of(),Map.of());
    var code= link("code",readme(dir,"code"),Kind.code,Map.of(),Map.of("pub",List.of("Data1")));
    assertTrue(linkProblem(dir,code,pub).orElseThrow().contains("accepts only \"data:readWrite\""));
  }
  @Test void readingFromACodeProjectIsRejected(@TempDir Path dir){
    var other= link("other",readme(dir,"other"),Kind.code,Map.of(),Map.of());
    var code= link("code",readme(dir,"code"),Kind.code,Map.of("other",List.of("Data1")),Map.of());
    assertTrue(linkProblem(dir,code,other).orElseThrow().contains("accepts only \"data:readOnly\" or \"data:readWrite\""));
  }
  @Test void readingFromAnInvalidDataProjectIsADeadLink(@TempDir Path dir){
    var pub= link("pub",readme(dir,"pub"),Kind.dataReadOnly,Map.of(),Map.of());
    var code= link("code",readme(dir,"code"),Kind.code,Map.of("pub",List.of("Data1")),Map.of());
    linkProblem(dir,code,pub);
    assertEquals(Optional.of("\"reads\" refers to \"pub\", which is itself invalid:\nbroken"),registry(dir).linkProblem(code,p->Optional.of("broken").filter(_->p.equals(pub.path()))));
  }
  @Test void anEditThatWouldWriteAnInvalidFileIsRefusedAndChangesNothing(@TempDir Path dir){
    var project= folder(dir,"someproject");
    var r= registry(dir);
    r.add("someproject",project,Kind.idle);
    same("[###]\"hello\" in \"mains\" is not a Fearless main name[###]",assertThrows(Registry.Refused.class,()->r.update(project,e->e.withMains(List.of("hello")))).getMessage());
    assertEquals(List.of(),r.all().getFirst().mains());
  }
}
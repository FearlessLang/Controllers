package controller;

import static controller.Errs.err;
import static controller.Errs.same;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import controller.Registry.Content;
import controller.Registry.Entry;
import controller.Registry.Kind;
import tools.Fs;
import userMessages.UserError;

final class RegistryTest{
  private static final Path file= Path.of("projects.info").toAbsolutePath();
  private static final String root= Path.of("").toAbsolutePath().getRoot().toString().replace('\\', '/');
  private static Content content(String text){ return Registry.parse(text, file, Registry::real, Path.of(root+"manager")); }
  private static List<Entry> parse(String projects){ return content("{\"projects\":"+projects+",\"systemExtensions\":[]}").projects(); }
  private static Registry registry(Path dir){ return new Registry(folder(dir, "manager"), _ -> {}); }
  private static Path folder(Path dir, String name){
    var res= dir.resolve(name);
    Fs.ensureDir(res);
    return res;
  }
  private static String unix(Path p){ return p.toAbsolutePath().normalize().toString().replace('\\', '/'); }

  //-- the schema
  @Test void anEmptyFileIsNoProjects(){ assertEquals(List.of(), parse("{}")); }
  @Test void aMinimalProjectHasAPathAndAKindWithNoMainsReadsOrEdits(){
    var e= parse("{\"someproject\":{\"path\":\"Str:"+root+"abs/someproject\",\"kind\":\"idle\"}}").getFirst();
    assertEquals(new Entry("someproject", Path.of(root+"abs/someproject"), Kind.idle, List.of(), Map.of(), Map.of()), e);
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
    assertEquals(new Entry("someproject", Path.of(root+"abs/someproject"), Kind.code,
      List.of("some.Main1", "some.Main2"), Map.of("publicfiles", List.of("Data1")), Map.of("publicdata", List.of("Data2", "Data3"))), e);
  }
  @Test void allFourKindsParse(){
    assertEquals(Kind.idle, parse("{\"a\":{\"path\":\"Str:"+root+"a\",\"kind\":\"idle\"}}").getFirst().kind());
    assertEquals(Kind.code, parse("{\"a\":{\"path\":\"Str:"+root+"a\",\"kind\":\"code\"}}").getFirst().kind());
    assertEquals(Kind.dataReadOnly, parse("{\"a\":{\"path\":\"Str:"+root+"a\",\"kind\":\"data:readOnly\"}}").getFirst().kind());
    assertEquals(Kind.dataReadWrite, parse("{\"a\":{\"path\":\"Str:"+root+"a\",\"kind\":\"data:readWrite\"}}").getFirst().kind());
  }
  @Test void theWholeFileMustBeAnObjectWithProjectsAndSystemExtensions(){
    err("[###]The whole file must be an object {...} with \"projects\" and \"systemExtensions\".[###]", () -> content("\"not an object\""));
    err("[###]\"projects\" must be an object {...} mapping each project name to the metadata of that project.[###]", () -> parse("\"not an object\""));
    err("[###]The key \"systemExtensions\" is missing: the keys are \"projects\" and \"systemExtensions\".[###]", () -> content("{\"projects\":{}}"));
    err("[###]Unknown key \"other\": the keys are \"projects\" and \"systemExtensions\".[###]", () -> content("{\"projects\":{},\"systemExtensions\":[],\"other\":[]}"));
  }
  @Test void aProjectNameMustBeShapedLikeAFolderName(){
    err("[###]\"Not Valid\" is not a valid project name[###]", () -> parse("{\"Not Valid\":{\"path\":\"Str:"+root+"a\"}}"));
  }
  @Test void anUnknownAttributeIsRejected(){
    err("[###]Unknown project attribute \"bogus\": the attributes of a project are \"path\", \"kind\", \"mains\", \"reads\", \"edits\".[###]", () -> parse("{\"a\":{\"path\":\"Str:"+root+"a\",\"bogus\":\"1\"}}"));
  }
  @Test void aMissingPathIsRejected(){
    err("[###]Project \"a\" is missing its \"path\"[###]", () -> parse("{\"a\":{\"kind\":\"idle\"}}"));
  }
  @Test void aRelativePathIsRejected(){
    err("[###]\"path\" must be an absolute path[###]", () -> parse("{\"a\":{\"path\":\"Str:relative/path\"}}"));
  }
  @Test void aMissingKindIsRejected(){
    err("[###]Project \"a\" is missing its \"kind\": one of \"idle\", \"code\", \"data:readOnly\" or \"data:readWrite\".[###]", () -> parse("{\"a\":{\"path\":\"Str:"+root+"a\"}}"));
  }
  @Test void anUnknownKindIsRejectedWithTheFourValidOptionsListed(){
    err("[###]\"kind\" must be one of \"idle\", \"code\", \"data:readOnly\" or \"data:readWrite\", not \"nonsense\".[###]", () -> parse("{\"a\":{\"path\":\"Str:"+root+"a\",\"kind\":\"nonsense\"}}"));
  }
  @Test void repeatedMainsAreRejected(){
    err("[###]\"a.Main1\" is repeated in \"mains\".[###]", () -> parse("{\"a\":{\"path\":\"Str:"+root+"a\",\"mains\":[\"a.Main1\",\"a.Main1\"]}}"));
  }
  @Test void aMainMustBePackageThenType(){
    err("[###]is not a Fearless main name[###]", () -> parse("{\"a\":{\"path\":\"Str:"+root+"a\",\"mains\":[\"Hello1\"]}}"));
    err("[###]is not a Fearless main name[###]", () -> parse("{\"a\":{\"path\":\"Str:"+root+"a\",\"mains\":[\"Hello.Hello1\"]}}"));
    err("[###]is not a Fearless main name[###]", () -> parse("{\"a\":{\"path\":\"Str:"+root+"a\",\"mains\":[\"hello.hello1\"]}}"));
  }
  @Test void mainsAreThePackageQualifiedNamesTheCompilerReports(){
    assertEquals(List.of("hello.Hello1", "hello.Hello3"), parse("{\"a\":{\"path\":\"Str:"+root+"a\",\"kind\":\"code\",\"mains\":[\"hello.Hello1\",\"hello.Hello3\"]}}").getFirst().mains());
  }
  @Test void aReadsTargetTakesTypeNamesNotPackageQualifiedNames(){
    err("[###]is not a Fearless type name[###]", () -> parse("{\"a\":{\"path\":\"Str:"+root+"a\",\"reads\":{\"b\":[\"hello.Data1\"]}}}"));
  }
  @Test void identicalOrNestedPathsAreRejected(){
    err("[###]\"b\" has the same path as \"a\", or one is inside the other[###]", () -> parse("{\"a\":{\"path\":\"Str:"+root+"same\",\"kind\":\"idle\"},\"b\":{\"path\":\"Str:"+root+"same\",\"kind\":\"idle\"}}"));
    err("[###]\"b\" has the same path as \"a\", or one is inside the other[###]", () -> parse("{\"a\":{\"path\":\"Str:"+root+"parent\",\"kind\":\"idle\"},\"b\":{\"path\":\"Str:"+root+"parent/child\",\"kind\":\"idle\"}}"));
  }
  @Test void aLinkNamesTheFolderItLinksTo(@TempDir Path dir){
    Assumptions.assumeFalse(Fs.isWindows());
    var real= Registry.real(folder(dir, "real"));
    var link= Fs.of(() -> Files.createSymbolicLink(dir.resolve("link"), real));
    err("[###]\"b\" has the same path as \"a\", or one is inside the other[###]", () -> parse("{\"a\":{\"path\":\"Str:"+unix(real)+"\",\"kind\":\"idle\"},\"b\":{\"path\":\"Str:"+unix(link)+"\",\"kind\":\"idle\"}}"));
    assertEquals(real, parse("{\"a\":{\"path\":\"Str:"+unix(link)+"\",\"kind\":\"idle\"}}").getFirst().path());
  }
  @Test void printingThenParsingRoundTripsTheContent(){
    var c= new Content(List.of(new Entry("someproject", Path.of(root+"abs/someproject"), Kind.code, List.of("some.Main1"), Map.of("publicfiles", List.of("Data1")), Map.of())), List.of("htm"));
    assertEquals(c, content(Registry.text(c)));
  }

  //-- persistence
  @Test void noFileYetIsNoRegisteredFolder(@TempDir Path dir){ assertEquals(List.of(), registry(dir).all()); }
  @Test void addedFolderIsIdleWithNoLinks(@TempDir Path dir){
    var project= folder(dir, "someproject");
    registry(dir).add("someproject", project);
    assertEquals(List.of(new Entry("someproject", project.toAbsolutePath().normalize(), Kind.idle, List.of(), Map.of(), Map.of())), registry(dir).all());
  }
  @Test void addingARegisteredFolderOrNameAgainIsABug(@TempDir Path dir){
    var project= folder(dir, "someproject");
    var r= registry(dir);
    r.add("someproject", project);
    assertThrows(AssertionError.class, () -> r.add("again", project.resolve("..").resolve("someproject")));
    assertThrows(AssertionError.class, () -> r.add("someproject", folder(dir, "other")));
    assertEquals(1, registry(dir).all().size());
  }
  @Test void mainsSurviveAReRead(@TempDir Path dir){
    var project= folder(dir, "someproject");
    var r= registry(dir);
    r.add("someproject", project);
    r.update(project, e -> e.withMains(List.of("hello.Hello1")));
    assertEquals(List.of("hello.Hello1"), registry(dir).all().getFirst().mains());
  }
  @Test void forgettingAFolderRemovesOnlyThatOne(@TempDir Path dir){
    var kept= folder(dir, "kept");
    var gone= folder(dir, "gone");
    var r= registry(dir);
    r.add("kept", kept);
    r.add("gone", gone);
    r.remove(gone);
    assertEquals(List.of(kept.toAbsolutePath().normalize()), r.all().stream().map(Entry::path).toList());
    assertEquals(Optional.empty(), r.of(gone));
  }
  @Test void aFolderInsideOrAroundARegisteredOneIsFound(@TempDir Path dir){
    var project= folder(dir, "someproject");
    var r= registry(dir);
    r.add("someproject", project);
    assertEquals(project, r.overlapping(project.resolve("inside")).orElseThrow());
    assertEquals(project, r.overlapping(dir).orElseThrow());
    assertEquals(Optional.empty(), r.overlapping(folder(dir, "otherproject")));
    assertEquals(Optional.empty(), r.overlapping(project));
  }
  @Test void aSiblingNamedLikeAPrefixIsNotNested(@TempDir Path dir){
    var r= registry(dir);
    r.add("some", folder(dir, "some"));
    assertEquals(Optional.empty(), r.overlapping(folder(dir, "someproject")));
  }
  @Test void awkwardFolderNamesSurviveAReRead(@TempDir Path dir){
    var project= folder(dir, "a name with spaces");
    registry(dir).add("spacey", project);
    assertEquals(project.toAbsolutePath().normalize(), registry(dir).all().getFirst().path());
  }
  @Test void aFolderNamedWithABackslashOrANewlineSurvivesAReRead(@TempDir Path dir){
    Assumptions.assumeFalse(Fs.isWindows());
    var r= registry(dir);
    r.add("slash", folder(dir, "a\\b"));
    r.add("line", folder(dir, "c\nd"));
    assertEquals(r.all(), registry(dir).all());
  }
  @Test void changingTheLinksKeepsTheirOrderInTheFile(@TempDir Path dir){
    var code= folder(dir, "code");
    var r= registry(dir);
    r.replace(content("{\"projects\": {\"code\": {\"path\": \"Str:"+unix(code)+"\", \"kind\": \"code\", \"reads\": {\"zeta\": [\"Z\"], \"alpha\": [\"A\"], \"mid\": [\"M\"], \"beta\": [\"B\"]}}}, \"systemExtensions\": []}"));
    r.update(code, e -> e.withLinks(e.reads(), Map.of("zeta", List.of("W"))));
    assertEquals("""
      {
        "projects": {
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
        },
        "systemExtensions": []
      }
      """.formatted(unix(code)), Fs.readUtf8(folder(dir, "manager").resolve("projects.info")));
  }
  @Test void aCorruptedFileRefusesToLoadWithARichError(@TempDir Path dir){
    Fs.writeUtf8(folder(dir, "manager").resolve("projects.info"), "not info at all");
    err("[###]Expected a string \"...\", a list [...] or an object {...} here.[###]", () -> registry(dir).all());
  }
  @Test void aMissingFileIsWrittenEmpty(@TempDir Path dir){
    registry(dir);
    assertEquals("""
      {
        "projects": {},
        "systemExtensions": []
      }
      """, Fs.readUtf8(dir.resolve("manager").resolve("projects.info")));
  }
  @Test void theSystemExtensionsSurviveAReReadAndAMalformedListRefusesToLoad(@TempDir Path dir){
    var r= registry(dir);
    r.replace(new Content(List.of(), List.of("pdf", "htm")));
    assertEquals(List.of("pdf", "htm"), registry(dir).extensions());
    err("[###]\"fapp001\" in \"systemExtensions\" is not a system extension: 1 to 16 lowercase letters or digits, other than \"fearless\", \"fapp000\" to \"fapp999\" and \"ffile000\" to \"ffile999\".[###]", () -> content("{\"projects\":{},\"systemExtensions\":[\"htm\", \"fapp001\"]}"));
    err("[###]\"htm\" is repeated in \"systemExtensions\".[###]", () -> content("{\"projects\":{},\"systemExtensions\":[\"htm\", \"htm\"]}"));
    err("[###]\"systemExtensions\" must be a list [...] of strings.[###]", () -> content("{\"projects\":{},\"systemExtensions\":{}}"));
  }
  private static String inManager(Path folder, Path manager){
    return "[###]\"path\": \"Str:"+unix(folder)+"\"[###]^^^^^^\n\nWhile inspecting the file\nFearless cannot keep track of this folder as a project.\n\nThe folder is:\n  "+folder+"\nThe manager folder of this Fearless is:\n  "+manager+"\n[###]";
  }
  private static void placedAt(Path manager, Path folder, String expected){
    var text= "{\"projects\": {\n  \"x\": {\"path\": \"Str:"+unix(folder)+"\", \"kind\": \"code\"}\n}, \"systemExtensions\": []}\n";
    err(expected, () -> Registry.parse(text, file, Registry::real, manager));
  }
  @Test void anEditPlacingAProjectAtARootOrInOrAroundTheManagerFolderIsRefusedAtThePath(@TempDir Path dir){
    registry(dir);
    var manager= Registry.real(dir.resolve("manager"));
    placedAt(manager, Path.of(root), "[###]\"path\": \"Str:"+root+"\"[###]^^^^^^^\n\nWhile inspecting the file\nFearless cannot keep track of the root of a drive or of the file system as a\nproject.\n[###]");
    placedAt(manager, manager, inManager(manager, manager));
    placedAt(manager, folder(manager, "inside"), inManager(manager.resolve("inside"), manager));
    placedAt(manager, Registry.real(dir), inManager(Registry.real(dir), manager));
  }
  @Test void aProjectsInfoPlacingAProjectInTheManagerFolderIsAFatalErrorAtThePath(@TempDir Path dir){
    var manager= folder(dir, "manager");
    Fs.writeUtf8(manager.resolve("projects.info"), "{\"projects\": {\n  \"x\": {\"path\": \"Str:"+unix(manager)+"\", \"kind\": \"code\"}\n}, \"systemExtensions\": []}\n");
    err(inManager(Registry.real(manager), Registry.real(manager)), () -> registry(dir));
  }
  @Test void aTypeNameIsInReadsOrInEditsNotInBoth(){
    err("""
      [###]"edits":{"pub":["Data2","Data1"]}[###]
      [###]"Data1" is in both "reads"."pub" and "edits"."pub": a type name is in one link only, since the code reaches one data project through it; a type name in "edits" also reads.[###]""",
      () -> parse("{\"a\":{\"path\":\"Str:"+root+"a\",\"kind\":\"code\",\"reads\":{\"pub\":[\"Data1\"]},\"edits\":{\"pub\":[\"Data2\",\"Data1\"]}}}"));
  }
  @Test void aTypeNameNamesOneDataProject(){
    err("""
      [###]"reads":{"pub":["Data1"],"notes":["Data1"]}[###]
      [###]"Data1" is in both "reads"."pub" and "reads"."notes": a type name is in one link only[###]""",
      () -> parse("{\"a\":{\"path\":\"Str:"+root+"a\",\"kind\":\"code\",\"reads\":{\"pub\":[\"Data1\"],\"notes\":[\"Data1\"]}}}"));
    err("[###]\"Data1\" is in both \"reads\".\"pub\" and \"edits\".\"notes\"[###]",
      () -> parse("{\"a\":{\"path\":\"Str:"+root+"a\",\"kind\":\"code\",\"reads\":{\"pub\":[\"Data1\"]},\"edits\":{\"notes\":[\"Data1\"]}}}"));
  }
  @Test void aCodeProjectIsGrantedTheFoldersOfTheDataProjectsItsLinksAccept(){
    var pub= link("pub", Path.of(root+"pub"), Kind.dataReadOnly, Map.of(), Map.of());
    var notes= link("notes", Path.of(root+"notes"), Kind.dataReadWrite, Map.of(), Map.of());
    var code= link("code", Path.of(root+"code"), Kind.code, Map.of("pub", List.of("Pub"), "notes", List.of("Old"), "gone", List.of("Gone")), Map.of("notes", List.of("Notes", "Diary"), "pub", List.of("Pub2")));
    var grants= Registry.grants(new Content(List.of(pub, notes, code), List.of()), code);
    assertEquals(Map.of("Pub", pub.path(), "Old", notes.path()), grants.reads());
    assertEquals(Map.of("Notes", notes.path(), "Diary", notes.path()), grants.edits());
  }
  @Test void aLinkNamesAType(){
    err("[###]\"reads\".\"pub\" names no type: a link names the one or more type names the code uses for \"pub\".[###]", () -> parse("{\"a\":{\"path\":\"Str:"+root+"a\",\"kind\":\"code\",\"reads\":{\"pub\":[]}}}"));
  }
  @Test void readsAndEditsSurviveAReRead(@TempDir Path dir){
    var code= folder(dir, "mycode");
    var pub= folder(dir, "pub");
    var r= registry(dir);
    r.add("mycode", code);
    r.add("pub", pub);
    r.update(code, e -> e.withKind(Kind.code));
    r.update(pub, e -> e.withKind(Kind.dataReadWrite));
    r.update(code, e -> e.withLinks(Map.of("pub", List.of("Data2", "Data3")), Map.of("pub", List.of("Data1"))));
    var reread= registry(dir).of(code).orElseThrow();
    assertEquals(Map.of("pub", List.of("Data2", "Data3")), reread.reads());
    assertEquals(Map.of("pub", List.of("Data1")), reread.edits());
  }

  //-- link checks
  private static Entry link(String alias, Path path, Kind kind, Map<String,List<String>> reads, Map<String,List<String>> edits){
    return new Entry(alias, path, kind, List.of(), reads, edits);
  }
  private static Path readme(Path dir, String name){
    var res= dir.resolve(name);
    Fs.writeUtf8(res.resolve("readme"), "hi\n");
    return res;
  }
  private Optional<String> linkProblem(Path dir, Entry e, Entry... others){
    var r= registry(dir);
    for (var o : others){ r.add(o.alias(), o.path()); r.update(o.path(), _ -> o); }
    r.add(e.alias(), e.path());
    r.update(e.path(), _ -> e);
    return r.linkProblem(e, _ -> Optional.empty());
  }
  @Test void aNonCodeEntryNeverHasALinkProblem(@TempDir Path dir){
    var e= link("a", readme(dir, "a"), Kind.idle, Map.of("missing", List.of("X")), Map.of());
    assertEquals(Optional.empty(), linkProblem(dir, e));
  }
  @Test void readingARegisteredReadOnlyAndEditingAReadWriteAreFine(@TempDir Path dir){
    var pub= link("pub", readme(dir, "pub"), Kind.dataReadWrite, Map.of(), Map.of());
    var code= link("code", readme(dir, "code"), Kind.code, Map.of("pub", List.of("Data1")), Map.of("pub", List.of("Data2")));
    assertEquals(Optional.empty(), linkProblem(dir, code, pub));
  }
  @Test void readingFromAMissingAliasIsADeadLink(@TempDir Path dir){
    var code= link("code", readme(dir, "code"), Kind.code, Map.of("nosuchproject", List.of("Data1")), Map.of());
    assertTrue(linkProblem(dir, code).orElseThrow().contains("no project called \"nosuchproject\" is registered"));
  }
  @Test void editingAReadOnlyDataProjectIsRejected(@TempDir Path dir){
    var pub= link("pub", readme(dir, "pub"), Kind.dataReadOnly, Map.of(), Map.of());
    var code= link("code", readme(dir, "code"), Kind.code, Map.of(), Map.of("pub", List.of("Data1")));
    assertTrue(linkProblem(dir, code, pub).orElseThrow().contains("accepts only \"data:readWrite\""));
  }
  @Test void readingFromACodeProjectIsRejected(@TempDir Path dir){
    var other= link("other", readme(dir, "other"), Kind.code, Map.of(), Map.of());
    var code= link("code", readme(dir, "code"), Kind.code, Map.of("other", List.of("Data1")), Map.of());
    assertTrue(linkProblem(dir, code, other).orElseThrow().contains("accepts only \"data:readOnly\" or \"data:readWrite\""));
  }
  @Test void readingFromAnInvalidDataProjectIsADeadLink(@TempDir Path dir){
    var pub= link("pub", readme(dir, "pub"), Kind.dataReadOnly, Map.of(), Map.of());
    var code= link("code", readme(dir, "code"), Kind.code, Map.of("pub", List.of("Data1")), Map.of());
    linkProblem(dir, code, pub);
    assertEquals(Optional.of("\"reads\" refers to \"pub\", which is itself invalid:\nbroken"), registry(dir).linkProblem(code, p -> Optional.of("broken").filter(_ -> p.equals(pub.path()))));
  }
  @Test void anEditThatWouldWriteAnInvalidFileIsRefusedAndChangesNothing(@TempDir Path dir){
    var project= folder(dir, "someproject");
    var r= registry(dir);
    r.add("someproject", project);
    same("[###]\"hello\" in \"mains\" is not a Fearless main name[###]", assertThrows(Refused.class, () -> r.update(project, e -> e.withMains(List.of("hello")))).getMessage());
    assertEquals(List.of(), r.all().getFirst().mains());
  }
}
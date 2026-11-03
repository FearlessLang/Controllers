package controller;

import static controller.Errs.err;
import static controller.Errs.same;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
  @Test void anEmptyFileIsNoProjects(){ assertEquals(List.of(), parse("[]")); }
  @Test void aMinimalProjectHasAPathAndAKindWithNoMainsReadsOrEdits(){
    var e= parse("[{\"path\":\"Str:\\\""+root+"abs/someproject\\\"\",\"kind\":\"idle\"}]").getFirst();
    assertEquals(new Entry("", Path.of(root+"abs/someproject"), Kind.idle, List.of(), Map.of(), Map.of()), e);
  }
  @Test void aFullCodeProjectParsesEveryField(){
    var e= parse("""
[{
  "path":"Str:\\"%sabs/someproject\\"",
  "kind":"code",
  "mains":["some.Main1","some.Main2"],
  "mounts":{"some.Pub":"publicfiles","some.Diary":"publicdata"},
  "attenuators":{"some.Main1":["some.NoGui","base.NoManager"]}
}]""".formatted(root)).getFirst();
    assertEquals(new Entry("", Path.of(root+"abs/someproject"), Kind.code,
      List.of("some.Main1", "some.Main2"), Map.of("some.Pub", "publicfiles", "some.Diary", "publicdata"), Map.of("some.Main1", List.of("some.NoGui", "base.NoManager"))), e);
  }
  @Test void allFourKindsParse(){
    assertEquals(Kind.idle, parse("[{\"path\":\"Str:\\\""+root+"a\\\"\",\"kind\":\"idle\"}]").getFirst().kind());
    assertEquals(Kind.code, parse("[{\"path\":\"Str:\\\""+root+"a\\\"\",\"kind\":\"code\"}]").getFirst().kind());
    assertEquals(Kind.dataRead, parse("[{\"path\":\"Str:\\\""+root+"a\\\"\",\"kind\":\"dataRead\"}]").getFirst().kind());
    assertEquals(Kind.dataEdit, parse("[{\"path\":\"Str:\\\""+root+"a\\\"\",\"kind\":\"dataEdit\"}]").getFirst().kind());
  }
  @Test void theWholeFileMustBeAnObjectWithProjectsAndSystemExtensions(){
    err("[###]The whole file must be an object {...} with \"projects\" and \"systemExtensions\".[###]", () -> content("\"not an object\""));
    err("[###]\"projects\" must be a list [...] of the registered projects, each an object {...} with the metadata of that project.[###]", () -> parse("{\"a\":{\"path\":\"Str:\\\""+root+"a\\\"\",\"kind\":\"idle\"}}"));
    err("[###]The key \"systemExtensions\" is missing: the keys are \"projects\" and \"systemExtensions\".[###]", () -> content("{\"projects\":[]}"));
    err("[###]Unknown key \"other\": the keys are \"projects\" and \"systemExtensions\".[###]", () -> content("{\"projects\":[],\"systemExtensions\":[],\"other\":[]}"));
  }
  @Test void everyProjectIsAnObject(){
    err("""
In file: [###]projects.info

001| {"projects":[{"path":"Str:\\"%sa\\"","kind":"idle"}, "b"],"systemExtensions":[]}
   |                                     [###]^^^

While inspecting the file
Every entry in "projects" must be an object {...} with the metadata of a project.[###]""".formatted(root), () -> parse("[{\"path\":\"Str:\\\""+root+"a\\\"\",\"kind\":\"idle\"}, \"b\"]"));
  }
  @Test void anUnknownAttributeIsRejected(){
    err("[###]Unknown project attribute \"bogus\": the attributes of a project are \"path\", \"kind\", \"mains\", \"mounts\", \"attenuators\".[###]", () -> parse("[{\"path\":\"Str:\\\""+root+"a\\\"\",\"bogus\":\"1\"}]"));
  }
  @Test void aMissingPathIsRejected(){
    err("[###]Project 2 of \"projects\" is missing its \"path\": the absolute path of the project folder.[###]", () -> parse("[{\"path\":\"Str:\\\""+root+"a\\\"\",\"kind\":\"idle\"},{\"kind\":\"idle\"}]"));
  }
  @Test void aRelativePathIsRejected(){
    err("[###]\"path\" must be an absolute path[###]", () -> parse("[{\"path\":\"Str:\\\"relative/path\\\"\"}]"));
  }
  @Test void aMissingKindIsRejected(){
    err("[###]Project 1 of \"projects\" is missing its \"kind\": one of \"idle\", \"code\", \"dataRead\" or \"dataEdit\".[###]", () -> parse("[{\"path\":\"Str:\\\""+root+"a\\\"\"}]"));
  }
  @Test void anUnknownKindIsRejectedWithTheFourValidOptionsListed(){
    err("[###]\"kind\" must be one of \"idle\", \"code\", \"dataRead\" or \"dataEdit\", not \"nonsense\".[###]", () -> parse("[{\"path\":\"Str:\\\""+root+"a\\\"\",\"kind\":\"nonsense\"}]"));
  }
  @Test void repeatedMainsAreRejected(){
    err("[###]\"a.Main1\" is repeated in \"mains\".[###]", () -> parse("[{\"path\":\"Str:\\\""+root+"a\\\"\",\"mains\":[\"a.Main1\",\"a.Main1\"]}]"));
  }
  @Test void aMainMustBePackageThenType(){
    err("[###]is not a Fearless main name[###]", () -> parse("[{\"path\":\"Str:\\\""+root+"a\\\"\",\"mains\":[\"Hello1\"]}]"));
    err("[###]is not a Fearless main name[###]", () -> parse("[{\"path\":\"Str:\\\""+root+"a\\\"\",\"mains\":[\"Hello.Hello1\"]}]"));
    err("[###]is not a Fearless main name[###]", () -> parse("[{\"path\":\"Str:\\\""+root+"a\\\"\",\"mains\":[\"hello.hello1\"]}]"));
  }
  @Test void mainsAreThePackageQualifiedNamesTheCompilerReports(){
    assertEquals(List.of("hello.Hello1", "hello.Hello3"), parse("[{\"path\":\"Str:\\\""+root+"a\\\"\",\"kind\":\"code\",\"mains\":[\"hello.Hello1\",\"hello.Hello3\"]}]").getFirst().mains());
  }
  @Test void mountsMapEachQualifiedDataReadTypeToAProjectName(){
    var a= "[{\"path\":\"Str:\\\""+root+"a\\\"\",\"kind\":\"code\",\"mounts\":";
    err("[###]\"mounts\" must be an object {...} mapping the name of a DataRead type to the name of a data project.[###]", () -> parse(a+"[\"hello.Pub\"]}]"));
    err("[###]\"Pub\" in \"mounts\" is not a Fearless DataRead type name: a package name, a dot, then a type name, like \"hello.Diary\".[###]", () -> parse(a+"{\"Pub\":\"pub\"}}]"));
    err("[###]\"mounts\".\"hello.Pub\" must be a string \"...\".[###]", () -> parse(a+"{\"hello.Pub\":[\"pub\"]}}]"));
    err("[###]\"Not Valid\" in \"mounts\".\"hello.Pub\" is not a valid project name.[###]", () -> parse(a+"{\"hello.Pub\":\"Not Valid\"}}]"));
  }
  @Test void attenuatorsMapEachMainToTheAttenuatorNamesAppliedToIt(){
    var a= "[{\"path\":\"Str:\\\""+root+"a\\\"\",\"kind\":\"code\",\"attenuators\":";
    err("[###]\"attenuators\" must be an object {...} mapping a main name to a list of attenuator names.[###]", () -> parse(a+"[\"hello.NoGui\"]}]"));
    err("[###]\"Hello1\" in \"attenuators\" is not a Fearless main name: a package name, a dot, then a type name, like \"hello.Hello1\".[###]", () -> parse(a+"{\"Hello1\":[\"hello.NoGui\"]}}]"));
    err("[###]\"NoGui\" in \"attenuators\".\"hello.Hello1\" is not a Fearless attenuator name: a package name, a dot, then a type name, like \"hello.NoGui\".[###]", () -> parse(a+"{\"hello.Hello1\":[\"NoGui\"]}}]"));
    err("[###]\"attenuators\".\"hello.Hello1\" must be a list [...] of strings.[###]", () -> parse(a+"{\"hello.Hello1\":\"hello.NoGui\"}}]"));
    err("[###]\"attenuators\".\"hello.Hello1\" names no attenuator: a main with no attenuator is left out of \"attenuators\".[###]", () -> parse(a+"{\"hello.Hello1\":[]}}]"));
    assertEquals(Map.of("hello.Old", List.of("other.NoGui", "hello.NoGui", "other.NoGui")), parse(a+"{\"hello.Old\":[\"other.NoGui\",\"hello.NoGui\",\"other.NoGui\"]}}]").getFirst().attenuators());
  }
  @Test void identicalOrNestedPathsAreRejected(){
    err("[###]Project 2 of \"projects\" has the same path as project 1, or one is inside the other; every file belongs to exactly one project.[###]", () -> parse("[{\"path\":\"Str:\\\""+root+"same\\\"\",\"kind\":\"idle\"},{\"path\":\"Str:\\\""+root+"same\\\"\",\"kind\":\"idle\"}]"));
    err("[###]Project 2 of \"projects\" has the same path as project 1, or one is inside the other; every file belongs to exactly one project.[###]", () -> parse("[{\"path\":\"Str:\\\""+root+"parent\\\"\",\"kind\":\"idle\"},{\"path\":\"Str:\\\""+root+"parent/child\\\"\",\"kind\":\"idle\"}]"));
  }
  @Test void aLinkNamesTheFolderItLinksTo(@TempDir Path dir){
    var real= Registry.real(folder(dir, "real"));
    var link= Disk.link(dir.resolve("link"), real);
    err("[###]Project 2 of \"projects\" has the same path as project 1, or one is inside the other; every file belongs to exactly one project.[###]", () -> parse("[{\"path\":\"Str:\\\""+unix(real)+"\\\"\",\"kind\":\"idle\"},{\"path\":\"Str:\\\""+unix(link)+"\\\"\",\"kind\":\"idle\"}]"));
    assertEquals(real, parse("[{\"path\":\"Str:\\\""+unix(link)+"\\\"\",\"kind\":\"idle\"}]").getFirst().path());
  }
  @Test void printingThenParsingRoundTripsTheContent(){
    var c= new Content(List.of(new Entry("", Path.of(root+"abs/someproject"), Kind.code, List.of("some.Main1"), Map.of("some.Pub", "publicfiles"), Map.of("some.Main1", List.of("some.NoGui")))), List.of("htm"));
    assertEquals(c, content(Registry.text(c)));
  }

  //-- persistence
  @Test void noFileYetIsNoRegisteredFolder(@TempDir Path dir){ assertEquals(List.of(), registry(dir).all()); }
  @Test void addedFolderIsIdleWithNoMounts(@TempDir Path dir){
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
  @Test void aFolderNamedWithABackslashANewlineOrNotUnicodeSurvivesAReRead(@TempDir Path dir){
    var r= registry(dir);
    added(r, "slash", folder(dir, Fs.isWindows() ? "aéb" : "a\\b"));
    added(r, "line", folder(dir, Fs.isWindows() ? "c\ud800d" : "c\nd"));
    assertEquals(r.all(), registry(dir).all());
  }
  /// registers the folder as the manager does: its marker is written first
  private static void added(Registry r, String alias, Path folder){ Names.nameAs(folder, alias); r.add(alias, folder); }
  private static Map<String,String> remounted(Map<String,String> mounts){
    var res= new LinkedHashMap<>(mounts);
    res.put("code.Alpha", "other");
    res.put("code.Beta", "beta");
    return res;
  }
  @Test void changingTheMountsKeepsTheirOrderInTheFile(@TempDir Path dir){
    var code= folder(dir, "code");
    var r= registry(dir);
    r.save(content("{\"projects\": [{\"path\": \"Str:\\\""+unix(code)+"\\\"\", \"kind\": \"code\", \"mounts\": {\"code.Zeta\": \"zeta\", \"code.Alpha\": \"alpha\", \"code.Mid\": \"mid\"}}], \"systemExtensions\": []}"));
    r.update(code, e -> e.withMounts(remounted(e.mounts())));
    assertEquals("""
{
  "projects": [
    {
      "path": "Str:\\"%s\\"",
      "kind": "code",
      "mounts": {
        "code.Zeta": "zeta",
        "code.Alpha": "other",
        "code.Mid": "mid",
        "code.Beta": "beta"
      }
    }
  ],
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
  "projects": [],
  "systemExtensions": []
}
""", Fs.readUtf8(dir.resolve("manager").resolve("projects.info")));
  }
  @Test void theSystemExtensionsSurviveAReReadAndAMalformedListRefusesToLoad(@TempDir Path dir){
    var r= registry(dir);
    r.save(new Content(List.of(), List.of("pdf", "htm")));
    assertEquals(List.of("pdf", "htm"), registry(dir).extensions());
    err("[###]\"fapp001\" in \"systemExtensions\" is not a system extension: 1 to 16 lowercase letters or digits, other than \"fearless\", \"fapp000\" to \"fapp999\" and \"ffile000\" to \"ffile999\".[###]", () -> content("{\"projects\":[],\"systemExtensions\":[\"htm\", \"fapp001\"]}"));
    err("[###]\"htm\" is repeated in \"systemExtensions\".[###]", () -> content("{\"projects\":[],\"systemExtensions\":[\"htm\", \"htm\"]}"));
    err("[###]\"systemExtensions\" must be a list [...] of strings.[###]", () -> content("{\"projects\":[],\"systemExtensions\":{}}"));
  }
  private static String inManager(Path folder, Path manager){
    return "[###]\"path\": \"Str:\\\""+unix(folder)+"\\\"\"[###]^^^^^^\n\nWhile inspecting the file\nFearless cannot keep track of this folder as a project.\n\nThe folder is:\n  "+folder+"\nThe manager folder of this Fearless is:\n  "+manager+"\n[###]";
  }
  private static void placedAt(Path manager, Path folder, String expected){
    var text= "{\"projects\": [\n  {\"path\": \"Str:\\\""+unix(folder)+"\\\"\", \"kind\": \"code\"}\n], \"systemExtensions\": []}\n";
    err(expected, () -> Registry.parse(text, file, Registry::real, manager));
  }
  @Test void anEditPlacingAProjectAtARootOrInOrAroundTheManagerFolderIsRefusedAtThePath(@TempDir Path dir){
    registry(dir);
    var manager= Registry.real(dir.resolve("manager"));
    placedAt(manager, Path.of(root), "[###]\"path\": \"Str:\\\""+root+"\\\"\"[###]^^^^^^^\n\nWhile inspecting the file\nFearless cannot keep track of the root of a drive or of the file system as a\nproject.\n[###]");
    placedAt(manager, manager, inManager(manager, manager));
    placedAt(manager, folder(manager, "inside"), inManager(manager.resolve("inside"), manager));
    placedAt(manager, Registry.real(dir), inManager(Registry.real(dir), manager));
  }
  @Test void aProjectsInfoPlacingAProjectInTheManagerFolderIsAFatalErrorAtThePath(@TempDir Path dir){
    var manager= folder(dir, "manager");
    Fs.writeUtf8(manager.resolve("projects.info"), "{\"projects\": [\n  {\"path\": \"Str:\\\""+unix(manager)+"\\\"\", \"kind\": \"code\"}\n], \"systemExtensions\": []}\n");
    err(inManager(Registry.real(manager), Registry.real(manager)), () -> registry(dir));
  }
  private static Entry entry(String alias, Kind kind, Map<String,String> mounts){ return new Entry(alias, Path.of(root+alias), kind, List.of(), mounts, Map.of()); }
  @Test void onlyAMountOfADataEditProjectOnADataEditTypeIsGrantedForEditing(){
    var pub= entry("pub", Kind.dataRead, Map.of());
    var notes= entry("notes", Kind.dataEdit, Map.of());
    var other= entry("other", Kind.code, Map.of());
    var code= entry("code", Kind.code, Map.of("code.Pub", "pub", "code.Old", "notes", "code.Diary", "notes", "code.Ledger", "pub", "code.Gone", "gone", "code.Other", "other"));
    var grants= Registry.grants(new Content(List.of(pub, notes, other, code), List.of()), code, List.of("code.Diary", "code.Ledger"));
    assertEquals(Map.of("code.Pub", pub.path(), "code.Old", notes.path()), grants.reads());
    assertEquals(Map.of("code.Diary", notes.path()), grants.edits());
  }
  @Test void mountsSurviveAReRead(@TempDir Path dir){
    var code= folder(dir, "mycode");
    var r= registry(dir);
    r.add("mycode", code);
    r.update(code, e -> e.withKind(Kind.code).withMounts(Map.of("mycode.Pub", "pub", "mycode.Diary", "notes")));
    assertEquals(Map.of("mycode.Pub", "pub", "mycode.Diary", "notes"), registry(dir).of(code).orElseThrow().mounts());
  }

  //-- names
  @Test void projectsInfoIsTheListOfTheProjectsInRegistrationOrderWithNoNames(@TempDir Path dir){
    var r= registry(dir);
    var zeta= folder(dir, "zeta");
    var alpha= folder(dir, "alpha");
    r.add("zeta", zeta);
    r.add("alpha", alpha);
    r.update(alpha, e -> e.withKind(Kind.dataRead));
    assertEquals("""
{
  "projects": [
    {
      "path": "Str:\\"%s\\"",
      "kind": "idle"
    },
    {
      "path": "Str:\\"%s\\"",
      "kind": "dataRead"
    }
  ],
  "systemExtensions": []
}
""".formatted(unix(zeta), unix(alpha)), Fs.readUtf8(dir.resolve("manager").resolve("projects.info")));
    assertEquals(List.of("zeta", "alpha"), registry(dir).all().stream().map(Entry::alias).toList());
  }
  private static String listed(Path... folders){
    return Stream.of(folders).map(f -> "{\"path\": \"Str:\\\""+unix(f)+"\\\"\", \"kind\": \"idle\"}").collect(Collectors.joining(", ", "{\"projects\": [", "], \"systemExtensions\": []}"));
  }
  @Test void aFolderWithTheMarkerOfAnEarlierFolderGetsItsMarkerRenamedAtTheLookup(@TempDir Path dir){
    var first= folder(dir, "first");
    var second= folder(dir, "second");
    Fs.writeUtf8(first.resolve("same.fearless"), "1\n");
    Fs.writeUtf8(second.resolve("same.fearless"), "2\n");
    Fs.writeUtf8(folder(dir, "manager").resolve("projects.info"), listed(second, first));
    var inferred= Registry.inferred(content(listed(second, first)));
    assertEquals(List.of("same", "first"), inferred.projects().stream().map(Entry::alias).toList());
    assertEquals(List.of(first.resolve("same.fearless")), Names.list(first));
    var r= registry(dir);
    assertEquals(List.of("same", "first"), r.all().stream().map(Entry::alias).toList());
    assertEquals(List.of(first.resolve("first.fearless")), Names.list(first));
    assertEquals("1\n", Fs.readUtf8(first.resolve("first.fearless")));
    assertEquals(List.of(second.resolve("same.fearless")), Names.list(second));
  }
  @Test void aFolderWithSeveralMarkersKeepsItsNameAcrossLookups(@TempDir Path dir){
    var several= folder(dir, "several");
    Fs.writeUtf8(several.resolve("beta.fearless"), "");
    Fs.writeUtf8(several.resolve("alpha.fearless"), "");
    Fs.writeUtf8(folder(dir, "manager").resolve("projects.info"), listed(several));
    var r= registry(dir);
    r.lookup();
    r.nameCheck(several);
    assertEquals(List.of("alpha"), r.all().stream().map(Entry::alias).toList());
    assertEquals(List.of(several.resolve("alpha.fearless"), several.resolve("beta.fearless")), Names.list(several));
  }
  @Test void aNameCheckFindsAMarkerRenamedByHandAndAMissingMarker(@TempDir Path dir){
    var hello= folder(dir, "hello");
    var r= registry(dir);
    added(r, "hello", hello);
    Fs.ofV(() -> Files.move(hello.resolve("hello.fearless"), hello.resolve("hi.fearless")));
    assertEquals("hello", r.of(hello).orElseThrow().alias());
    r.nameCheck(hello);
    assertEquals("hi", r.of(hello).orElseThrow().alias());
    assertEquals(Optional.empty(), r.lookedUp("hello"));
    Fs.ofV(() -> Files.delete(hello.resolve("hi.fearless")));
    assertEquals(hello, r.lookedUp("hello").orElseThrow().path());
    assertEquals(List.of(hello.resolve("hello.fearless")), Names.list(hello));
  }
  @Test void aFolderTheRegistryCanNotReachGetsNoNameAndNoFile(@TempDir Path dir){
    var gone= dir.resolve("gone");
    var hello= folder(dir, "hello");
    Fs.writeUtf8(folder(dir, "manager").resolve("projects.info"), listed(gone, hello));
    assertEquals(List.of("", "hello"), registry(dir).all().stream().map(Entry::alias).toList());
    assertFalse(Files.exists(gone));
  }
}

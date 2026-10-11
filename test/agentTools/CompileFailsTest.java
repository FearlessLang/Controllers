package agentTools;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.util.List;

import tools.Fs;

final class CompileFailsTest extends ManagerTest{
  final Action appsShown= action("appsShown",on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",on("ubuntu_gnome",()->waitUntilTime(val(18000))));
  final Action codeShown= action("codeShown",on("ubuntu_gnome",()->waitUntilTime(val(21000))));
  final Action compile= action("compile",on("ubuntu_gnome",()->click(val(535),val(112))));
  final Action compileFailed= action("compileFailed",on("ubuntu_gnome",()->waitUntilTime(val(36000))));
  final Action compileFixed= action("compileFixed",on("ubuntu_gnome",()->click(val(535),val(112))));
  final Action fixedCompiled= action("fixedCompiled",on("ubuntu_gnome",()->waitUntilTime(val(52000))));
  final Action managerQuit= action("managerQuit",on("ubuntu_gnome",()->waitUntilTime(val(57000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var broken= filesIOFolder.resolve("broken");
    var source= broken.resolve("_broken").resolve("_rank_app.fear");
    Fs.rmTree(broken);
    Fs.writeUtf8(broken.resolve("broken.fearless"),"\n");
    Fs.writeUtf8(source,"use base.Main as Main;\n\nHello:Main{s->Nope.nope}\n");
    launchScript("first",broken.resolve("broken.fearless"));
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    becomeCode.go();
    codeShown.go();
    compile.go();
    compileFailed.go();
    stabilize();
    checkContent(List.of("broken",".fearless_out","compile_error.txt"),"""
      In file: fear:/_broken/_rank_app.fear

      003| Hello:Main{s->Nope.nope}
         |               ^^^^^

      While inspecting a type name
      Type "Nope" is not declared in package "broken" and is not made visible via "use".
      In scope: "Hello", "Main".
      Error 7 WellFormedness
      """);
    Fs.writeUtf8(source,"use base.Main as Main;\n\nHello:Main{s->base.Debug#(\"fixed\")}\n");
    stabilize();
    compileFixed.go();
    fixedCompiled.go();
    stabilize();
    assertFalse(Files.exists(broken.resolve(".fearless_out").resolve("compile_error.txt")));
    checkContent(List.of("broken",".fearless_out","mains.info"),"[###]\"broken.Hello\"[###]");
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
  }
}

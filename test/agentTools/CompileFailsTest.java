package agentTools;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;

import tools.Fs;

final class CompileFailsTest extends ManagerTest{
  final Action becomeCode= action("becomeCode",
    on("ubuntu_gnome",() -> click(val(990),val(113))),
    on("xubuntu_xfce",() -> click(val(996),val(93))));
  final Action openBroken= action("openBroken",
    on("ubuntu_gnome",() -> waitDoubleClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",() -> waitDoubleClick(val(null),val(null),val(null))));
  final Action openBrokenMarker= action("openBrokenMarker",
    on("ubuntu_gnome",() -> waitDoubleClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",() -> waitDoubleClick(val(null),val(null),val(null))));
  final Action projectShown= action("projectShown",
    on("ubuntu_gnome",() -> waitAfter(val(null))),
    on("xubuntu_xfce",() -> waitAfter(val(null))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",() -> waitAfter(val(17500))),
    on("xubuntu_xfce",() -> waitAfter(val(21500))));
  final Action codeShown= action("codeShown",
    on("ubuntu_gnome",() -> waitAfter(val(4500))),
    on("xubuntu_xfce",() -> waitAfter(val(6000))));
  final Action compile= action("compile",
    on("ubuntu_gnome",() -> click(val(543),val(113))),
    on("xubuntu_xfce",() -> click(val(502),val(93))));
  final Action compileFailed= action("compileFailed",
    on("ubuntu_gnome",() -> waitAfter(val(21000))),
    on("xubuntu_xfce",() -> waitAfter(val(26000))));
  final Action compileFixed= action("compileFixed",
    on("ubuntu_gnome",() -> click(val(543),val(113))),
    on("xubuntu_xfce",() -> click(val(502),val(93))));
  final Action fixedCompiled= action("fixedCompiled",
    on("ubuntu_gnome",() -> waitAfter(val(22500))),
    on("xubuntu_xfce",() -> waitAfter(val(28000))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",() -> waitAfter(val(7000))),
    on("xubuntu_xfce",() -> waitAfter(val(9000))));
  @Override void walk() throws Throwable{
    var broken= files.resolve("broken");
    var source= broken.resolve("_broken").resolve("_rank_app.fear");
    Fs.writeUtf8(broken.resolve("broken.fearless"),"\n");
    Fs.writeUtf8(source,"use base.Main as Main;\n\nHello:Main{s->Nope.nope}\n");
    var bare= look();
    start();
    managerShown.go();
    toFiles();
    openBroken.go();
    openBrokenMarker.go();
    closeFiles();
    projectShown.go();
    becomeCode.go();
    codeShown.go();
    compile.go();
    compileFailed.go();
    stabilize();
    checkContent(files.resolve("broken/.fearless_out/compile_error.txt"),"""
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
    checkContent(files.resolve("broken/.fearless_out/mains.info"),"[###]\"broken.Hello\"[###]");
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    same(bare,inWindow(100,200,200,400));
  }
}

package agentTools;

import tools.Fs;

final class RecompileAfterEditTest extends ManagerTest{
  final Action becomeCode= action("becomeCode",
    on("ubuntu_gnome", () -> waitClick(val(100), val(971), val(112))),
    on("xubuntu_xfce", () -> waitClick(val(null), val(976), val(93))));
  static final String first= "use base.Main as Main;\n\nFirst:Main{s->base.Debug#(\"first\")}\n";
  final Action openDraft= action("openDraft",
    on("ubuntu_gnome", () -> waitDoubleClick(val(100), val(798), val(380))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action openDraftMarker= action("openDraftMarker",
    on("ubuntu_gnome", () -> waitDoubleClick(val(100), val(910), val(376))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action projectShown= action("projectShown",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("xubuntu_xfce", () -> waitAfter(val(null))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("xubuntu_xfce", () -> waitAfter(val(21500))));
  final Action codeShown= action("codeShown",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("xubuntu_xfce", () -> waitAfter(val(6000))));
  final Action compile= action("compile",
    on("ubuntu_gnome", () -> waitClick(val(100), val(539), val(112))),
    on("xubuntu_xfce", () -> waitClick(val(null), val(482), val(93))));
  final Action firstCompiled= action("firstCompiled",
    on("ubuntu_gnome", () -> waitAfter(val(2000))),//the compile of draft runs in its own JVM
    on("xubuntu_xfce", () -> waitAfter(val(26000))));
  final Action editNoticed= action("editNoticed",
    on("ubuntu_gnome", () -> waitAfter(val(3500))),//the manager checks the selected project every three seconds
    on("xubuntu_xfce", () -> waitAfter(val(14000))));
  final Action compileEdited= action("compileEdited",
    on("ubuntu_gnome", () -> waitClick(val(100), val(539), val(112))),
    on("xubuntu_xfce", () -> waitClick(val(null), val(482), val(93))));
  final Action editedCompiled= action("editedCompiled",
    on("ubuntu_gnome", () -> waitAfter(val(2000))),//the compile of draft runs in its own JVM
    on("xubuntu_xfce", () -> waitAfter(val(26000))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome", () -> waitAfter(val(100))),
    on("xubuntu_xfce", () -> waitAfter(val(9000))));
  @Override void walk() throws Throwable{
    var draft= files.resolve("draft");
    var source= draft.resolve("_draft").resolve("_rank_app.fear");
    Fs.writeUtf8(draft.resolve("draft.fearless"), "\n");
    Fs.writeUtf8(source, first);
    var bare= look();
    start();
    managerShown.go();
    toFiles();
    openDraft.go();
    openDraftMarker.go();
    projectShown.go();
    becomeCode.go();
    codeShown.go();
    compile.go();
    firstCompiled.go();
    stabilize();
    checkContent(files.resolve("draft/.fearless_out/mains.info"), "{\n  \"draft.First\": [###]\n}\n");
    var button= inWindow(509, 102, 30, 20);
    var runnable= look();
    Fs.writeUtf8(source, first+"Second:Main{s->base.Debug#(\"second\")}\n");
    stabilize();
    editNoticed.go();
    changed(runnable, button);
    var outdated= look();
    compileEdited.go();
    editedCompiled.go();
    changed(outdated, button);
    stabilize();
    checkContent(files.resolve("draft/.fearless_out/mains.info"), "{\n  \"draft.First\": [###],\n  \"draft.Second\": [###]\n}\n");
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    closeFiles();
    same(bare, button);
  }
}

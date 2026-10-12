package agentTools;

import java.awt.Rectangle;
import java.util.List;

import tools.Fs;

final class RecompileAfterEditTest extends ManagerTest{
  final Action becomeCode= header("becomeCode",971);
  static final String first= "use base.Main as Main;\n\nFirst:Main{s->base.Debug#(\"first\")}\n";
  final Action appsShown= action("appsShown",on("ubuntu_gnome",()->waitUntilTime(val(3500))));
  final Action terminalShown= action("terminalShown",on("ubuntu_gnome",()->waitUntilTime(val(8500))));
  final Action managerShown= action("managerShown",on("ubuntu_gnome",()->waitUntilTime(val(26000))));
  final Action codeShown= action("codeShown",on("ubuntu_gnome",()->waitUntilTime(val(30500))));
  final Action compile= action("compile",on("ubuntu_gnome",()->click(val(524),val(113))));
  final Action firstCompiled= action("firstCompiled",on("ubuntu_gnome",()->waitUntilTime(val(51500))));
  final Action editNoticed= action("editNoticed",on("ubuntu_gnome",()->waitUntilTime(val(62500))));
  final Action compileEdited= action("compileEdited",on("ubuntu_gnome",()->click(val(524),val(113))));
  final Action editedCompiled= action("editedCompiled",on("ubuntu_gnome",()->waitUntilTime(val(83500))));
  final Action managerQuit= action("managerQuit",on("ubuntu_gnome",()->waitUntilTime(val(90500))));
  @Override void walk() throws Throwable{
    noManagerData();
    var draft= filesIOFolder.resolve("draft");
    var source= draft.resolve("_draft").resolve("_rank_app.fear");
    Fs.rmTree(draft);
    Fs.writeUtf8(draft.resolve("draft.fearless"),"\n");
    Fs.writeUtf8(source,first);
    launchScript("first",draft.resolve("draft.fearless"));
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    becomeCode.go();
    codeShown.go();
    compile.go();
    firstCompiled.go();
    stabilize();
    checkContent(List.of("draft",".fearless_out","mains.info"),"{\n  \"draft.First\": [###]\n}\n");
    var button= new Rectangle(509,102,30,20);
    var runnable= look();
    Fs.writeUtf8(source,first+"Second:Main{s->base.Debug#(\"second\")}\n");
    stabilize();
    editNoticed.go();
    changed(runnable,button);
    var outdated= look();
    compileEdited.go();
    editedCompiled.go();
    changed(outdated,button);
    stabilize();
    checkContent(List.of("draft",".fearless_out","mains.info"),"{\n  \"draft.First\": [###],\n  \"draft.Second\": [###]\n}\n");
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
  }
}

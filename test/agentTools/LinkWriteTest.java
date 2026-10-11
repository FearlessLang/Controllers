package agentTools;

import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.List;

import tools.Fs;

final class LinkWriteTest extends ManagerTest{
  final Action appsShown= action("appsShown",on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",on("ubuntu_gnome",()->waitUntilTime(val(18000))));
  final Action dataSaved= action("dataSaved",on("ubuntu_gnome",()->waitUntilTime(val(21000))));
  final Action appsShownTeller= action("appsShownTeller",on("ubuntu_gnome",()->waitUntilTime(val(22500))));
  final Action terminalShownTeller= action("terminalShownTeller",on("ubuntu_gnome",()->waitUntilTime(val(26000))));
  final Action tellerShown= action("tellerShown",on("ubuntu_gnome",()->waitUntilTime(val(34000))));
  final Action codeSaved= action("codeSaved",on("ubuntu_gnome",()->waitUntilTime(val(37000))));
  final Action openLinks= action("openLinks",on("ubuntu_gnome",()->click(val(450),val(379))));
  final Action linksShown= action("linksShown",on("ubuntu_gnome",()->waitUntilTime(val(39000))));
  final Action writeField= action("writeField",on("ubuntu_gnome",()->click(val(760),val(421))));
  final Action editsSaved= action("editsSaved",on("ubuntu_gnome",()->waitUntilTime(val(44000))));
  final Action readField= action("readField",on("ubuntu_gnome",()->click(val(560),val(421))));
  final Action noteShown= action("noteShown",on("ubuntu_gnome",()->waitUntilTime(val(49000))));
  final Action readFieldAgain= action("readFieldAgain",on("ubuntu_gnome",()->click(val(560),val(421))));
  final Action readEmptied= action("readEmptied",on("ubuntu_gnome",()->waitUntilTime(val(53000))));
  final Action writeFieldAgain= action("writeFieldAgain",on("ubuntu_gnome",()->click(val(760),val(421))));
  final Action writeEmptied= action("writeEmptied",on("ubuntu_gnome",()->waitUntilTime(val(57000))));
  final Action managerQuit= action("managerQuit",on("ubuntu_gnome",()->waitUntilTime(val(62000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var ledger= filesIOFolder.resolve("ledger");
    var vault= ledger.resolve("vault");
    var teller= ledger.resolve("teller");
    Fs.rmTree(ledger);
    Fs.writeUtf8(vault.resolve("vault.fearless"),"\n");
    Fs.writeUtf8(vault.resolve("_vault").resolve("_rank_app.fear"),"Coin:{}\n");
    Fs.writeUtf8(teller.resolve("teller.fearless"),"\n");
    Fs.writeUtf8(teller.resolve("_teller").resolve("_rank_app.fear"),"use base.Main as Main;\n\nHello:Main{s->base.Debug#(\"teller\")}\n");
    launchScript("first",vault.resolve("vault.fearless"));
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    becomeEditableData.go();
    dataSaved.go();
    launchScript("second",teller.resolve("teller.fearless"));
    runInTerminal(appsShownTeller,terminalShownTeller);
    tellerShown.go();
    stabilize();
    checkContent(List.of("second.exit"),"0\n");
    becomeCode.go();
    codeSaved.go();
    stabilize();
    var unlinked= registry(vault,teller,"");
    checkContent(info,unlinked);
    openLinks.go();
    linksShown.go();
    writeField.go();
    type("Coin\n");
    editsSaved.go();
    stabilize();
    var edits= registry(vault,teller,"""
      ,
            "edits": {
              "vault": ["Coin"]
            }""");
    checkContent(info,edits);
    var before= look();
    readField.go();
    type("Coin\n");
    noteShown.go();
    changed(before,new Rectangle(1720,1000,400,160));
    stabilize();
    checkContent(info,edits);
    closeDialog.go();
    readFieldAgain.go();
    empty();
    readEmptied.go();
    stabilize();
    checkContent(info,edits);
    writeFieldAgain.go();
    empty();
    writeEmptied.go();
    stabilize();
    checkContent(info,unlinked);
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
  }
  void empty(){
    keys(KeyEvent.VK_CONTROL,KeyEvent.VK_A);
    keys(KeyEvent.VK_BACK_SPACE);
    keys(KeyEvent.VK_ENTER);
  }
  static String registry(Path vault, Path teller, String links){
    return """
      {
        "projects": {
          "vault": {
            "path": "Str:%s",
            "kind": "data:readWrite"
          },
          "teller": {
            "path": "Str:%s",
            "kind": "code"%s
          }
        },
        "systemExtensions": []
      }
      """.formatted(slashed(vault),slashed(teller),links);
  }
}

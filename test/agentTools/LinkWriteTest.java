package agentTools;

import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.List;

import tools.Fs;

final class LinkWriteTest extends ManagerTest{
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(18000))));
  final Action focusTiles= action("focusTiles",
    on("ubuntu_gnome",()->click(val(200),val(1500))),
    on("windows",()->click(val(200),val(400))));
  final Action becomeEditableData= action("becomeEditableData",
    on("ubuntu_gnome",()->click(val(272),val(140))),
    on("windows",()->click(val(202),val(94))));
  final Action dataSaved= action("dataSaved",
    on("ubuntu_gnome",()->waitUntilTime(val(21500))));
  final Action appsShownTeller= action("appsShownTeller",
    on("ubuntu_gnome",()->waitUntilTime(val(23000))));
  final Action terminalShownTeller= action("terminalShownTeller",
    on("ubuntu_gnome",()->waitUntilTime(val(26500))));
  final Action tellerShown= action("tellerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(37500))));
  final Action becomeCode= action("becomeCode",
    on("ubuntu_gnome",()->click(val(400),val(140))),
    on("windows",()->click(val(332),val(94))));
  final Action codeSaved= action("codeSaved",
    on("ubuntu_gnome",()->waitUntilTime(val(40000))));
  final Action openLinks= action("openLinks",
    on("ubuntu_gnome",()->click(val(130),val(191))),
    on("windows",()->click(val(62),val(144))));
  final Action linksShown= action("linksShown",
    on("ubuntu_gnome",()->waitUntilTime(val(42000))));
  final Action writeField= action("writeField",
    on("ubuntu_gnome",()->click(val(442),val(233))),
    on("windows",()->click(val(327),val(183))));
  final Action editsSaved= action("editsSaved",
    on("ubuntu_gnome",()->waitUntilTime(val(45500))));
  final Action readField= action("readField",
    on("ubuntu_gnome",()->click(val(240),val(233))),
    on("windows",()->click(val(158),val(183))));
  final Action noteShown= action("noteShown",
    on("ubuntu_gnome",()->waitUntilTime(val(49000))));
  final Action ok= action("ok",
    on("ubuntu_gnome",()->click(val(1952),val(1183))),
    on("windows",()->click(val(639),val(414))));
  final Action readFieldAgain= action("readFieldAgain",
    on("ubuntu_gnome",()->click(val(240),val(233))),
    on("windows",()->click(val(158),val(183))));
  final Action readEmptied= action("readEmptied",
    on("ubuntu_gnome",()->waitUntilTime(val(52500))));
  final Action writeFieldAgain= action("writeFieldAgain",
    on("ubuntu_gnome",()->click(val(442),val(233))),
    on("windows",()->click(val(327),val(183))));
  final Action writeEmptied= action("writeEmptied",
    on("ubuntu_gnome",()->waitUntilTime(val(55500))));
  final Action appsShownToEnd= action("appsShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(57000))));
  final Action terminalShownToEnd= action("terminalShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(60500))));
  final Action managerEnded= action("managerEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(70500))));
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
    launchScript("first",vault);
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    focusTiles.go();
    keys(KeyEvent.VK_F8);
    keys(KeyEvent.VK_HOME);
    becomeEditableData.go();
    dataSaved.go();
    stabilize();
    checkContent(info,"""
      {
        "vault": {
          "path": "Str:%s",
          "kind": "data:readWrite"
        }
      }
      """.formatted(slashed(vault)));
    launchScript("second",teller);
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
    readField.go();
    type("Coin\n");
    noteShown.go();
    stabilize();
    checkContent(notes,"""
      In file: %s

      013|       "vault": ["Coin"]
         |                 ^^^^^^

      While inspecting the file
      "Coin" is in both "reads"."vault" and "edits"."vault": a type name in "edits" also reads, so it is not repeated in "reads"; a type name in "reads" only reads.
      """.formatted(filesIOFolder.resolve(data).resolve("projects.info")));
    checkContent(info,edits);
    ok.go();
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
    endScript();
    runInTerminal(appsShownToEnd,terminalShownToEnd);
    managerEnded.go();
    stabilize();
    checkContent(List.of("first.exit"),"137\n");
  }
  void empty(){
    keys(KeyEvent.VK_CONTROL,KeyEvent.VK_A);
    keys(KeyEvent.VK_BACK_SPACE);
    keys(KeyEvent.VK_ENTER);
  }
  static String registry(Path vault, Path teller, String links){
    return """
      {
        "vault": {
          "path": "Str:%s",
          "kind": "data:readWrite"
        },
        "teller": {
          "path": "Str:%s",
          "kind": "code"%s
        }
      }
      """.formatted(slashed(vault),slashed(teller),links);
  }
}

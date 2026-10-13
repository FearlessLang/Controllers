package agentTools;

import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.nio.file.Path;

import tools.Fs;

final class LinkWriteTest extends ManagerTest{
  final Action becomeEditableData= action("becomeEditableData",
    on("ubuntu_gnome",()->click(val(842),val(113))),
    on("xubuntu_xfce",()->click(val(828),val(93))));
  final Action becomeCode= action("becomeCode",
    on("ubuntu_gnome",()->click(val(974),val(113))),
    on("xubuntu_xfce",()->click(val(980),val(93))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitAfter(val(17500))),
    on("xubuntu_xfce",()->waitAfter(val(21500))));
  final Action dataSaved= action("dataSaved",
    on("ubuntu_gnome",()->waitAfter(val(4500))),
    on("xubuntu_xfce",()->waitAfter(val(6000))));
  final Action tellerShown= action("tellerShown",
    on("ubuntu_gnome",()->waitAfter(val(11000))),
    on("xubuntu_xfce",()->waitAfter(val(13500))));
  final Action codeSaved= action("codeSaved",
    on("ubuntu_gnome",()->waitAfter(val(4500))),
    on("xubuntu_xfce",()->waitAfter(val(5500))));
  final Action openLinks= action("openLinks",
    on("ubuntu_gnome",()->click(val(450),val(356))),
    on("xubuntu_xfce",()->click(val(383),val(336))));
  final Action linksShown= action("linksShown",
    on("ubuntu_gnome",()->waitAfter(val(3500))),
    on("xubuntu_xfce",()->waitAfter(val(4500))));
  final Action writeField= action("writeField",
    on("ubuntu_gnome",()->click(val(768),val(398))),
    on("xubuntu_xfce",()->click(val(701),val(378))));
  final Action editsSaved= action("editsSaved",
    on("ubuntu_gnome",()->waitAfter(val(7000))),
    on("xubuntu_xfce",()->waitAfter(val(9000))));
  final Action readField= action("readField",
    on("ubuntu_gnome",()->click(val(560),val(398))),
    on("xubuntu_xfce",()->click(val(493),val(378))));
  final Action noteShown= action("noteShown",
    on("ubuntu_gnome",()->waitAfter(val(7000))),
    on("xubuntu_xfce",()->waitAfter(val(8500))));
  final Action readFieldAgain= action("readFieldAgain",
    on("ubuntu_gnome",()->click(val(560),val(398))),
    on("xubuntu_xfce",()->click(val(493),val(378))));
  final Action readEmptied= action("readEmptied",
    on("ubuntu_gnome",()->waitAfter(val(5500))),
    on("xubuntu_xfce",()->waitAfter(val(7000))));
  final Action writeFieldAgain= action("writeFieldAgain",
    on("ubuntu_gnome",()->click(val(768),val(398))),
    on("xubuntu_xfce",()->click(val(701),val(378))));
  final Action writeEmptied= action("writeEmptied",
    on("ubuntu_gnome",()->waitAfter(val(5500))),
    on("xubuntu_xfce",()->waitAfter(val(7000))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",()->waitAfter(val(7000))),
    on("xubuntu_xfce",()->waitAfter(val(8500))));
  final Action openLedger= action("openLedger",
    on("ubuntu_gnome",()->waitDoubleClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",()->waitDoubleClick(val(null),val(null),val(null))));
  final Action openVault= action("openVault",
    on("ubuntu_gnome",()->waitDoubleClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",()->waitDoubleClick(val(null),val(null),val(null))));
  final Action openVaultMarker= action("openVaultMarker",
    on("ubuntu_gnome",()->waitDoubleClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",()->waitDoubleClick(val(null),val(null),val(null))));
  final Action vaultListed= action("vaultListed",
    on("ubuntu_gnome",()->waitAfter(val(null))),
    on("xubuntu_xfce",()->waitAfter(val(null))));
  final Action openLedgerAgain= action("openLedgerAgain",
    on("ubuntu_gnome",()->waitDoubleClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",()->waitDoubleClick(val(null),val(null),val(null))));
  final Action openTeller= action("openTeller",
    on("ubuntu_gnome",()->waitDoubleClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",()->waitDoubleClick(val(null),val(null),val(null))));
  final Action openTellerMarker= action("openTellerMarker",
    on("ubuntu_gnome",()->waitDoubleClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",()->waitDoubleClick(val(null),val(null),val(null))));
  final Action leaveWriteField= action("leaveWriteField",
    on("ubuntu_gnome",()->waitClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",()->waitClick(val(null),val(null),val(null))));
  final Action leaveReadField= action("leaveReadField",
    on("ubuntu_gnome",()->waitClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",()->waitClick(val(null),val(null),val(null))));
  final Action okButton= action("okButton",
    on("ubuntu_gnome",()->waitClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",()->waitClick(val(null),val(null),val(null))));
  final Action selectRead= action("selectRead",
    on("ubuntu_gnome",()->waitDrag(val(null),val(null),val(null),val(null),val(null))),
    on("xubuntu_xfce",()->waitDrag(val(null),val(null),val(null),val(null),val(null))));
  final Action leaveReadFieldAgain= action("leaveReadFieldAgain",
    on("ubuntu_gnome",()->waitClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",()->waitClick(val(null),val(null),val(null))));
  final Action selectWrite= action("selectWrite",
    on("ubuntu_gnome",()->waitDrag(val(null),val(null),val(null),val(null),val(null))),
    on("xubuntu_xfce",()->waitDrag(val(null),val(null),val(null),val(null),val(null))));
  final Action leaveWriteFieldAgain= action("leaveWriteFieldAgain",
    on("ubuntu_gnome",()->waitClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",()->waitClick(val(null),val(null),val(null))));
  @Override void walk() throws Throwable{
    var ledger= files.resolve("ledger");
    var vault= ledger.resolve("vault");
    var teller= ledger.resolve("teller");
    Fs.writeUtf8(vault.resolve("vault.fearless"),"\n");
    Fs.writeUtf8(vault.resolve("_vault").resolve("_rank_app.fear"),"Coin:{}\n");
    Fs.writeUtf8(teller.resolve("teller.fearless"),"\n");
    Fs.writeUtf8(teller.resolve("_teller").resolve("_rank_app.fear"),"use base.Main as Main;\n\nHello:Main{s->base.Debug#(\"teller\")}\n");
    start();
    managerShown.go();
    toFiles();
    openLedger.go();
    openVault.go();
    openVaultMarker.go();
    closeFiles();
    vaultListed.go();
    becomeEditableData.go();
    dataSaved.go();
    toFiles();
    openLedgerAgain.go();
    openTeller.go();
    openTellerMarker.go();
    closeFiles();
    tellerShown.go();
    becomeCode.go();
    codeSaved.go();
    stabilize();
    var unlinked= registry(vault,teller,"");
    checkContent(info,unlinked);
    openLinks.go();
    linksShown.go();
    writeField.go();
    type("Coin");
    leaveWriteField.go();
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
    type("Coin");
    leaveReadField.go();
    noteShown.go();
    changed(before,new Rectangle(760,460,400,160));
    stabilize();
    checkContent(info,edits);
    okButton.go();
    readFieldAgain.go();
    selectRead.go();
    keys(KeyEvent.VK_BACK_SPACE);
    leaveReadFieldAgain.go();
    readEmptied.go();
    stabilize();
    checkContent(info,edits);
    writeFieldAgain.go();
    selectWrite.go();
    keys(KeyEvent.VK_BACK_SPACE);
    leaveWriteFieldAgain.go();
    writeEmptied.go();
    stabilize();
    checkContent(info,unlinked);
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
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

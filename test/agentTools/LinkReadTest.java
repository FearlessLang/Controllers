package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Rectangle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import tools.Fs;

final class LinkReadTest extends ManagerTest{
  final Action becomeData= header("becomeData",714);
  final Action becomeCode= header("becomeCode",986);
  final Action backToIdle= header("backToIdle",726);
  final Action appsShown= action("appsShown",on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",on("ubuntu_gnome",()->waitUntilTime(val(18000))));
  final Action dataSaved= action("dataSaved",on("ubuntu_gnome",()->waitUntilTime(val(21000))));
  final Action appsShownReader= action("appsShownReader",on("ubuntu_gnome",()->waitUntilTime(val(22500))));
  final Action terminalShownReader= action("terminalShownReader",on("ubuntu_gnome",()->waitUntilTime(val(26000))));
  final Action readerShown= action("readerShown",on("ubuntu_gnome",()->waitUntilTime(val(34000))));
  final Action codeSaved= action("codeSaved",on("ubuntu_gnome",()->waitUntilTime(val(37000))));
  final Action openLinks= action("openLinks",on("ubuntu_gnome",()->click(val(450),val(356))));
  final Action linksShown= action("linksShown",on("ubuntu_gnome",()->waitUntilTime(val(39000))));
  final Action readField= action("readField",on("ubuntu_gnome",()->click(val(560),val(398))));
  final Action readsSaved= action("readsSaved",on("ubuntu_gnome",()->waitUntilTime(val(44000))));
  final Action storeTile= action("storeTile",on("ubuntu_gnome",()->click(val(261),val(130))));
  final Action storeShown= action("storeShown",on("ubuntu_gnome",()->waitUntilTime(val(46500))));
  final Action idleSaved= action("idleSaved",on("ubuntu_gnome",()->waitUntilTime(val(49500))));
  final Action readerTile= action("readerTile",on("ubuntu_gnome",()->click(val(133),val(130))));
  final Action readerShownAgain= action("readerShownAgain",on("ubuntu_gnome",()->waitUntilTime(val(52000))));
  final Action check= action("check",on("ubuntu_gnome",()->click(val(539),val(113))));
  final Action checked= action("checked",on("ubuntu_gnome",()->waitUntilTime(val(55000))));
  final Action managerQuit= action("managerQuit",on("ubuntu_gnome",()->waitUntilTime(val(61000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var links= filesIOFolder.resolve("links");
    var store= links.resolve("store");
    var reader= links.resolve("reader");
    Fs.rmTree(links);
    Fs.writeUtf8(store.resolve("store.fearless"),"\n");
    Fs.writeUtf8(store.resolve("_store").resolve("_rank_app.fear"),"Stock:{}\n");
    Fs.writeUtf8(reader.resolve("reader.fearless"),"\n");
    Fs.writeUtf8(reader.resolve("_reader").resolve("_rank_app.fear"),"use base.Main as Main;\n\nHello:Main{s->base.Debug#(\"reader\")}\n");
    launchScript("first",store.resolve("store.fearless"));
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    becomeData.go();
    dataSaved.go();
    launchScript("second",reader.resolve("reader.fearless"));
    runInTerminal(appsShownReader,terminalShownReader);
    readerShown.go();
    stabilize();
    checkContent(List.of("second.exit"),"0\n");
    becomeCode.go();
    codeSaved.go();
    stabilize();
    checkContent(info,registry(store,reader,"data:readOnly",""));
    openLinks.go();
    linksShown.go();
    readField.go();
    type("Stock\n");
    readsSaved.go();
    stabilize();
    var reads= """
      ,
            "reads": {
              "store": ["Stock"]
            }""";
    checkContent(info,registry(store,reader,"data:readOnly",reads));
    storeTile.go();
    storeShown.go();
    backToIdle.go();
    idleSaved.go();
    stabilize();
    checkContent(info,registry(store,reader,"idle",reads));
    readerTile.go();
    readerShownAgain.go();
    var before= look();
    check.go();
    checked.go();
    changed(before,new Rectangle(410,168,1000,80));
    stabilize();
    assertEquals(List.of(".fearless_out/check_error.txt"),Fs.walk(reader.resolve(".fearless_out"),st->st.filter(Files::isRegularFile).map(p->slashed(reader.relativize(p))).toList()));
    checkContent(List.of("links","reader",".fearless_out","check_error.txt"),"\"reads\" refers to \"store\", but the kind of \"store\" is \"idle\"; \"reads\" accepts only \"data:readOnly\" or \"data:readWrite\".");
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
  }
  static String registry(Path store, Path reader, String storeKind, String links){
    return """
      {
        "projects": {
          "store": {
            "path": "Str:%s",
            "kind": "%s"
          },
          "reader": {
            "path": "Str:%s",
            "kind": "code"%s
          }
        },
        "systemExtensions": []
      }
      """.formatted(slashed(store),storeKind,slashed(reader),links);
  }
}

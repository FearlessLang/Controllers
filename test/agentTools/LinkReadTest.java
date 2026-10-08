package agentTools;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import tools.Fs;

final class LinkReadTest extends ManagerTest{
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(18000))));
  final Action focusTiles= action("focusTiles",
    on("ubuntu_gnome",()->click(val(200),val(1500))),
    on("windows",()->click(val(200),val(400))));
  final Action becomeData= action("becomeData",
    on("ubuntu_gnome",()->click(val(141),val(140))),
    on("windows",()->click(val(74),val(94))));
  final Action dataSaved= action("dataSaved",
    on("ubuntu_gnome",()->waitUntilTime(val(21500))));
  final Action appsShownReader= action("appsShownReader",
    on("ubuntu_gnome",()->waitUntilTime(val(23000))));
  final Action terminalShownReader= action("terminalShownReader",
    on("ubuntu_gnome",()->waitUntilTime(val(26500))));
  final Action readerShown= action("readerShown",
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
  final Action readField= action("readField",
    on("ubuntu_gnome",()->click(val(240),val(233))),
    on("windows",()->click(val(158),val(183))));
  final Action readsSaved= action("readsSaved",
    on("ubuntu_gnome",()->waitUntilTime(val(45500))));
  final Action appsShownStore= action("appsShownStore",
    on("ubuntu_gnome",()->waitUntilTime(val(47000))));
  final Action terminalShownStore= action("terminalShownStore",
    on("ubuntu_gnome",()->waitUntilTime(val(50500))));
  final Action storeShown= action("storeShown",
    on("ubuntu_gnome",()->waitUntilTime(val(61500))));
  final Action backToIdle= action("backToIdle",
    on("ubuntu_gnome",()->click(val(138),val(140))),
    on("windows",()->click(val(70),val(94))));
  final Action idleSaved= action("idleSaved",
    on("ubuntu_gnome",()->waitUntilTime(val(64000))));
  final Action appsShownReaderAgain= action("appsShownReaderAgain",
    on("ubuntu_gnome",()->waitUntilTime(val(65500))));
  final Action terminalShownReaderAgain= action("terminalShownReaderAgain",
    on("ubuntu_gnome",()->waitUntilTime(val(69000))));
  final Action readerShownAgain= action("readerShownAgain",
    on("ubuntu_gnome",()->waitUntilTime(val(80000))));
  final Action compile= action("compile",
    on("ubuntu_gnome",()->click(val(164),val(111))),
    on("windows",()->click(val(102),val(67))));
  final Action compileRefused= action("compileRefused",
    on("ubuntu_gnome",()->waitUntilTime(val(86000))));
  final Action appsShownToEnd= action("appsShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(87500))));
  final Action terminalShownToEnd= action("terminalShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(91000))));
  final Action managerEnded= action("managerEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(101000))));
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
    launchScript("first",store);
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    focusTiles.go();
    keys(KeyEvent.VK_F8);
    keys(KeyEvent.VK_HOME);
    becomeData.go();
    dataSaved.go();
    stabilize();
    checkContent(info,"""
      {
        "store": {
          "path": "Str:%s",
          "kind": "data:readOnly"
        }
      }
      """.formatted(slashed(store)));
    handOver("second",reader,appsShownReader,terminalShownReader,readerShown);
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
    handOver("third",store,appsShownStore,terminalShownStore,storeShown);
    backToIdle.go();
    idleSaved.go();
    stabilize();
    checkContent(info,registry(store,reader,"idle",reads));
    handOver("fourth",reader,appsShownReaderAgain,terminalShownReaderAgain,readerShownAgain);
    compile.go();
    compileRefused.go();
    stabilize();
    checkContent(List.of(data,"eclipse","reader","console.txt"),"""
      "reads" refers to "store", but the kind of "store" is "idle"; "reads" accepts only "data:readOnly" or "data:readWrite".
      """);
    assertFalse(Files.exists(reader.resolve(".fearless_out")));
    endScript();
    runInTerminal(appsShownToEnd,terminalShownToEnd);
    managerEnded.go();
    stabilize();
    checkContent(List.of("first.exit"),"137\n");
  }
  void handOver(String exit, Path folder, Action appsShown, Action terminalShown, Action shown) throws Throwable{
    launchScript(exit,folder);
    runInTerminal(appsShown,terminalShown);
    shown.go();
    stabilize();
    checkContent(List.of(exit+".exit"),"0\n");
  }
  static String registry(Path store, Path reader, String storeKind, String links){
    return """
      {
        "store": {
          "path": "Str:%s",
          "kind": "%s"
        },
        "reader": {
          "path": "Str:%s",
          "kind": "code"%s
        }
      }
      """.formatted(slashed(store),storeKind,slashed(reader),links);
  }
}

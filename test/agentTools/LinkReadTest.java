package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import tools.Fs;

final class LinkReadTest extends ManagerTest{
  final Action becomeData= action("becomeData",
    on("ubuntu_gnome", () -> click(val(714), val(113))),
    on("xubuntu_xfce", () -> click(val(679), val(93))));
  final Action becomeCode= action("becomeCode",
    on("ubuntu_gnome", () -> click(val(986), val(113))),
    on("xubuntu_xfce", () -> click(val(992), val(93))));
  final Action backToIdle= action("backToIdle",
    on("ubuntu_gnome", () -> click(val(726), val(113))),
    on("xubuntu_xfce", () -> click(val(687), val(93))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome", () -> waitAfter(val(17500))),
    on("xubuntu_xfce", () -> waitAfter(val(21500))));
  final Action dataSaved= action("dataSaved",
    on("ubuntu_gnome", () -> waitAfter(val(4500))),
    on("xubuntu_xfce", () -> waitAfter(val(6000))));
  final Action readerShown= action("readerShown",
    on("ubuntu_gnome", () -> waitAfter(val(11000))),
    on("xubuntu_xfce", () -> waitAfter(val(13500))));
  final Action codeSaved= action("codeSaved",
    on("ubuntu_gnome", () -> waitAfter(val(4500))),
    on("xubuntu_xfce", () -> waitAfter(val(5500))));
  final Action openLinks= action("openLinks",
    on("ubuntu_gnome", () -> click(val(450), val(356))),
    on("xubuntu_xfce", () -> click(val(383), val(336))));
  final Action linksShown= action("linksShown",
    on("ubuntu_gnome", () -> waitAfter(val(3500))),
    on("xubuntu_xfce", () -> waitAfter(val(4500))));
  final Action readField= action("readField",
    on("ubuntu_gnome", () -> click(val(560), val(398))),
    on("xubuntu_xfce", () -> click(val(493), val(378))));
  final Action readsSaved= action("readsSaved",
    on("ubuntu_gnome", () -> waitAfter(val(7000))),
    on("xubuntu_xfce", () -> waitAfter(val(9000))));
  final Action storeTile= action("storeTile",
    on("ubuntu_gnome", () -> click(val(261), val(130))),
    on("xubuntu_xfce", () -> click(val(194), val(110))));
  final Action storeShown= action("storeShown",
    on("ubuntu_gnome", () -> waitAfter(val(4000))),
    on("xubuntu_xfce", () -> waitAfter(val(5000))));
  final Action idleSaved= action("idleSaved",
    on("ubuntu_gnome", () -> waitAfter(val(4500))),
    on("xubuntu_xfce", () -> waitAfter(val(5500))));
  final Action readerTile= action("readerTile",
    on("ubuntu_gnome", () -> click(val(133), val(130))),
    on("xubuntu_xfce", () -> click(val(66), val(110))));
  final Action readerShownAgain= action("readerShownAgain",
    on("ubuntu_gnome", () -> waitAfter(val(4000))),
    on("xubuntu_xfce", () -> waitAfter(val(5000))));
  final Action check= action("check",
    on("ubuntu_gnome", () -> click(val(539), val(113))),
    on("xubuntu_xfce", () -> click(val(477), val(93))));
  final Action checked= action("checked",
    on("ubuntu_gnome", () -> waitAfter(val(4500))),
    on("xubuntu_xfce", () -> waitAfter(val(5500))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome", () -> waitAfter(val(8500))),
    on("xubuntu_xfce", () -> waitAfter(val(10500))));
  final Action openLinksFolder= action("openLinksFolder",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action openStore= action("openStore",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action openStoreMarker= action("openStoreMarker",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action storeListed= action("storeListed",
    on("ubuntu_gnome", () -> waitAfter(val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(null))));
  final Action openLinksFolderAgain= action("openLinksFolderAgain",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action openReader= action("openReader",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action openReaderMarker= action("openReaderMarker",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action leaveReadField= action("leaveReadField",
    on("ubuntu_gnome", () -> waitClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitClick(val(null), val(null), val(null))));
  @Override void walk() throws Throwable{
    var links= files.resolve("links");
    var store= links.resolve("store");
    var reader= links.resolve("reader");
    Fs.writeUtf8(store.resolve("store.fearless"), "\n");
    Fs.writeUtf8(store.resolve("_store").resolve("_rank_app.fear"), "Stock:{}\n");
    Fs.writeUtf8(reader.resolve("reader.fearless"), "\n");
    Fs.writeUtf8(reader.resolve("_reader").resolve("_rank_app.fear"), "use base.Main as Main;\n\nHello:Main{s->base.Debug#(\"reader\")}\n");
    start();
    managerShown.go();
    toFiles();
    openLinksFolder.go();
    openStore.go();
    openStoreMarker.go();
    closeFiles();
    storeListed.go();
    becomeData.go();
    dataSaved.go();
    toFiles();
    openLinksFolderAgain.go();
    openReader.go();
    openReaderMarker.go();
    closeFiles();
    readerShown.go();
    becomeCode.go();
    codeSaved.go();
    stabilize();
    checkContent(info, registry(store, reader, "data:readOnly", ""));
    openLinks.go();
    linksShown.go();
    readField.go();
    type("Stock");
    leaveReadField.go();
    readsSaved.go();
    stabilize();
    var reads= """
      ,
            "reads": {
              "store": ["Stock"]
            }""";
    checkContent(info, registry(store, reader, "data:readOnly", reads));
    storeTile.go();
    storeShown.go();
    backToIdle.go();
    idleSaved.go();
    stabilize();
    checkContent(info, registry(store, reader, "idle", reads));
    readerTile.go();
    readerShownAgain.go();
    var before= look();
    check.go();
    checked.go();
    changed(before, inWindow(410, 168, 1000, 80));
    stabilize();
    assertEquals(List.of(".fearless_out/check_error.txt"), Fs.walk(reader.resolve(".fearless_out"), st -> st.filter(Files::isRegularFile).map(p -> slashed(reader.relativize(p))).toList()));
    checkContent(files.resolve("links/reader/.fearless_out/check_error.txt"), "\"reads\" refers to \"store\", but the kind of \"store\" is \"idle\"; \"reads\" accepts only \"data:readOnly\" or \"data:readWrite\".");
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
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
      """.formatted(slashed(store), storeKind, slashed(reader), links);
  }
}

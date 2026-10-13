package agentTools;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.util.List;

import tools.Fs;

final class FolderGoneTest extends ManagerTest{
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitAfter(val(3500))),
    on("xubuntu_xfce",()->waitAfter(val(4500))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitAfter(val(5000))),
    on("xubuntu_xfce",()->waitAfter(val(6500))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitAfter(val(17500))),
    on("xubuntu_xfce",()->waitAfter(val(21500))));
  final Action goneNoticed= action("goneNoticed",
    on("ubuntu_gnome",()->waitAfter(val(8500))),
    on("xubuntu_xfce",()->waitAfter(val(11000))));
  final Action check= action("check",
    on("ubuntu_gnome",()->click(val(524),val(113))),
    on("xubuntu_xfce",()->click(val(474),val(93))));
  final Action checked= action("checked",
    on("ubuntu_gnome",()->waitAfter(val(4500))),
    on("xubuntu_xfce",()->waitAfter(val(5500))));
  final Action tileBack= action("tileBack",
    on("ubuntu_gnome",()->waitAfter(val(8500))),
    on("xubuntu_xfce",()->waitAfter(val(10500))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",()->waitAfter(val(7000))),
    on("xubuntu_xfce",()->waitAfter(val(9000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var gone= filesIOFolder.resolve("gone");
    var moved= filesIOFolder.resolve("gone2");
    Fs.rmTree(gone);
    Fs.rmTree(moved);
    Fs.writeUtf8(gone.resolve("gone.fearless"),"\n");
    Fs.writeUtf8(gone.resolve("_gone").resolve("_rank_app.fear"),"use base.Main as Main;\n\nHello:Main{s->base.Debug#(\"gone\")}\n");
    launchScript("first",gone.resolve("gone.fearless"));
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    var tile= inWindow(69,95,128,85);
    var output= inWindow(410,168,1000,80);
    var valid= look();
    Files.move(gone,moved);
    stabilize();
    goneNoticed.go();
    changed(valid,tile);
    check.go();
    checked.go();
    changed(valid,output);
    stabilize();
    checkContent(info,"""
      {
        "projects": {
          "gone": {
            "path": "Str:%s",
            "kind": "idle"
          }
        },
        "systemExtensions": []
      }
      """.formatted(slashed(gone)));
    assertFalse(Files.exists(gone));
    Files.move(moved,gone);
    stabilize();
    tileBack.go();
    same(valid,tile);
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
  }
}

package agentTools;

import java.awt.event.KeyEvent;
import java.util.List;

final class EditMetadataAddProjectTest extends ManagerTest{
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(3500))),
    on("xubuntu_xfce",()->waitUntilTime(val(4500))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(8500))),
    on("xubuntu_xfce",()->waitUntilTime(val(11000))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitUntilTime(val(26000))),
    on("xubuntu_xfce",()->waitUntilTime(val(32500))));
  final Action editorShown= action("editorShown",
    on("ubuntu_gnome",()->waitUntilTime(val(31500))),
    on("xubuntu_xfce",()->waitUntilTime(val(39500))));
  final Action focusText= action("focusText",
    on("ubuntu_gnome",()->click(val(940),val(660))),
    on("xubuntu_xfce",()->click(val(907),val(657))));
  final Action commit= action("commit",
    on("ubuntu_gnome",()->click(val(1278),val(830))),
    on("xubuntu_xfce",()->click(val(1245),val(827))));
  final Action bothSaved= action("bothSaved",
    on("ubuntu_gnome",()->waitUntilTime(val(87500))),
    on("xubuntu_xfce",()->waitUntilTime(val(109500))));
  final Action secondTile= action("secondTile",
    on("ubuntu_gnome",()->click(val(261),val(130))),
    on("xubuntu_xfce",()->click(val(194),val(110))));
  final Action secondShown= action("secondShown",
    on("ubuntu_gnome",()->waitUntilTime(val(91000))),
    on("xubuntu_xfce",()->waitUntilTime(val(114000))));
  final Action check= action("check",
    on("ubuntu_gnome",()->click(val(523),val(113))),
    on("xubuntu_xfce",()->click(val(474),val(93))));
  final Action checked= action("checked",
    on("ubuntu_gnome",()->waitUntilTime(val(95000))),
    on("xubuntu_xfce",()->waitUntilTime(val(119000))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",()->waitUntilTime(val(102000))),
    on("xubuntu_xfce",()->waitUntilTime(val(127500))));
  @Override void walk() throws Throwable{
    noManagerData();
    var project= project("helloWorld");
    var other= project("helloStackTraces");
    var both= """
      {"projects": {"hello_world": {"path": "Str:%s", "kind": "idle"}, "start": {"path": "Str:%s", "kind": "idle"}}, "systemExtensions": []}
      """.formatted(slashed(project),slashed(other));
    launchScript("first",project.resolve("hello_world.fearless"));
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    managerMenu.go();
    editMetadata.go();
    editorShown.go();
    focusText.go();
    keys(KeyEvent.VK_CONTROL,KeyEvent.VK_A);
    type(both.strip());
    commit.go();
    bothSaved.go();
    stabilize();
    checkContent(info,"""
      {
        "projects": {
          "hello_world": {
            "path": "Str:%s",
            "kind": "idle"
          },
          "start": {
            "path": "Str:%s",
            "kind": "idle"
          }
        },
        "systemExtensions": []
      }
      """.formatted(slashed(project),slashed(other)));
    secondTile.go();
    secondShown.go();
    var output= inWindow(410,168,1000,80);
    var before= look();
    check.go();
    checked.go();
    changed(before,output);
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
  }
}

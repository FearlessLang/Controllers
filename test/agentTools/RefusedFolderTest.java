package agentTools;

import java.awt.Rectangle;

import tools.Fs;

final class RefusedFolderTest extends ManagerTest{
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",() -> waitAfter(val(null))),
    on("xubuntu_xfce",() -> waitAfter(val(null))));
  final Action openManagerFolder= action("openManagerFolder",
    on("ubuntu_gnome",() -> waitDoubleClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",() -> waitDoubleClick(val(null),val(null),val(null))));
  final Action openInside= action("openInside",
    on("ubuntu_gnome",() -> waitDoubleClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",() -> waitDoubleClick(val(null),val(null),val(null))));
  final Action noteShownAgain= action("noteShownAgain",
    on("ubuntu_gnome",() -> waitAfter(val(10500))),
    on("xubuntu_xfce",() -> waitAfter(val(13500))));
  final Action okButton= action("okButton",
    on("ubuntu_gnome",() -> waitClick(val(null),val(null),val(null))),
    on("xubuntu_xfce",() -> waitClick(val(null),val(null),val(null))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",() -> waitAfter(val(8500))),
    on("xubuntu_xfce",() -> waitAfter(val(10500))));
  @Override void walk() throws Throwable{
    var folder= filesIOFolder.resolve(data);
    var center= new Rectangle(760,460,400,160);
    var bare= look();
    start();
    managerShown.go();
    Fs.writeUtf8(folder.resolve("inside.fearless"),"\n");
    stabilize();
    var before= look();
    toPilotio();
    openManagerFolder.go();
    openInside.go();
    closeFiles();
    noteShownAgain.go();
    changed(before,center);
    stabilize();
    checkContent(info,"{\n  \"projects\": {},\n  \"systemExtensions\": []\n}\n");
    okButton.go();
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    same(bare,center);
  }
}

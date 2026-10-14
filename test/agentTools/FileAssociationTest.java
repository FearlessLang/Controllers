package agentTools;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;

final class FileAssociationTest extends ManagerTest{
  final Action iconArea= action("iconArea",
    on("ubuntu_gnome", () -> region(val(880), val(340), val(64), val(64))),
    on("xubuntu_xfce", () -> region(val(985), val(396), val(60), val(60))));
  final Action trayArea= action("trayArea",
    on("ubuntu_gnome", () -> region(val(null), val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> region(val(null), val(null), val(null), val(null))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome", () -> waitAfter(val(17500))),
    on("xubuntu_xfce", () -> waitAfter(val(21500))));
  final Action openHelloWorld= action("openHelloWorld",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action leaveHelloWorld= action("leaveHelloWorld",
    on("ubuntu_gnome", () -> waitClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitClick(val(null), val(null), val(null))));
  final Action openHelloWorldAgain= action("openHelloWorldAgain",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action associatedShown= action("associatedShown",
    on("ubuntu_gnome", () -> waitAfter(val(3500))),
    on("xubuntu_xfce", () -> waitAfter(val(4500))));
  final Action managerMenuBehind= action("managerMenuBehind",
    on("ubuntu_gnome", () -> waitClick(val(0), val(99), val(79))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(38), val(62))));
  final Action resetAsked= action("resetAsked",
    on("ubuntu_gnome", () -> waitAfter(val(5000))),
    on("xubuntu_xfce", () -> waitAfter(val(6000))));
  final Action cancelReset= action("cancelReset",
    on("ubuntu_gnome", () -> waitClick(val(0), val(1026), val(596))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(993), val(593))));
  final Action resetCancelled= action("resetCancelled",
    on("ubuntu_gnome", () -> waitAfter(val(3500))),
    on("xubuntu_xfce", () -> waitAfter(val(4500))));
  final Action resetAskedAgain= action("resetAskedAgain",
    on("ubuntu_gnome", () -> waitAfter(val(4000))),
    on("xubuntu_xfce", () -> waitAfter(val(5000))));
  final Action confirmReset= action("confirmReset",
    on("ubuntu_gnome", () -> waitClick(val(0), val(958), val(596))),
    on("xubuntu_xfce", () -> waitClick(val(0), val(925), val(593))));
  final Action managerReset= action("managerReset",
    on("ubuntu_gnome", () -> waitAfter(val(4500))),
    on("xubuntu_xfce", () -> waitAfter(val(5500))));
  final Action leaveHelloWorldAgain= action("leaveHelloWorldAgain",
    on("ubuntu_gnome", () -> waitClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitClick(val(null), val(null), val(null))));
  final Action openHelloWorldThird= action("openHelloWorldThird",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action genericShown= action("genericShown",
    on("ubuntu_gnome", () -> waitAfter(val(7000))),
    on("xubuntu_xfce", () -> waitAfter(val(9000))));
  final Action managerShownAgain= action("managerShownAgain",
    on("ubuntu_gnome", () -> waitAfter(val(18000))),
    on("xubuntu_xfce", () -> waitAfter(val(22500))));
  final Action openHelloWorldFourth= action("openHelloWorldFourth",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action associatedAgain= action("associatedAgain",
    on("ubuntu_gnome", () -> waitAfter(val(17000))),
    on("xubuntu_xfce", () -> waitAfter(val(21000))));
  final Action openHelloWorldMarker= action("openHelloWorldMarker",
    on("ubuntu_gnome", () -> waitDoubleClick(val(null), val(null), val(null))),
    on("xubuntu_xfce", () -> waitDoubleClick(val(null), val(null), val(null))));
  final Action projectShown= action("projectShown",
    on("ubuntu_gnome", () -> waitAfter(val(null))),
    on("xubuntu_xfce", () -> waitAfter(val(null))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome", () -> waitAfter(val(7000))),
    on("xubuntu_xfce", () -> waitAfter(val(9000))));
  @Override void walk() throws Throwable{
    project("helloWorld");
    iconArea.go();
    var icon= region;
    trayArea.go();
    var tray= region;
    var menuBar= inWindow(67, 70, 200, 20);
    var tile= inWindow(69, 95, 128, 85);
    var bare= look();
    start();
    managerShown.go();
    toFiles();
    openHelloWorld.go();
    leaveHelloWorld.go();
    openHelloWorldAgain.go();
    associatedShown.go();
    var associated= look();
    managerMenuBehind.go();
    resetManager.go();
    resetAsked.go();
    cancelReset.go();
    resetCancelled.go();
    changed(bare, menuBar);
    changed(bare, tray);
    stabilize();
    assertTrue(Files.exists(info));
    managerMenu.go();
    resetManager.go();
    resetAskedAgain.go();
    confirmReset.go();
    managerReset.go();
    same(bare, menuBar);
    same(bare, tray);
    stabilize();
    assertFalse(Files.exists(filesIOFolder.resolve(data)));
    leaveHelloWorldAgain.go();
    openHelloWorldThird.go();
    genericShown.go();
    changed(associated, icon);
    closeFiles();
    start();
    managerShownAgain.go();
    var empty= look();
    toFiles();
    openHelloWorldFourth.go();
    associatedAgain.go();
    same(associated, icon);
    openHelloWorldMarker.go();
    closeFiles();
    projectShown.go();
    changed(empty, tile);
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    same(bare, menuBar);
    same(bare, tray);
  }
}

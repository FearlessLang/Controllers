package agentTools;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.awt.Rectangle;
import java.nio.file.Files;
import java.util.List;

import tools.Fs;

final class SecondMarkerTest extends ManagerTest{
  final Action appsShown= action("appsShown",on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action managerShown= action("managerShown",on("ubuntu_gnome",()->waitUntilTime(val(18000))));
  final Action secondNoticed= action("secondNoticed",on("ubuntu_gnome",()->waitUntilTime(val(25000))));
  final Action check= action("check",on("ubuntu_gnome",()->click(val(527),val(113))));
  final Action checked= action("checked",on("ubuntu_gnome",()->waitUntilTime(val(28000))));
  final Action secondGone= action("secondGone",on("ubuntu_gnome",()->waitUntilTime(val(35000))));
  final Action checkAgain= action("checkAgain",on("ubuntu_gnome",()->click(val(527),val(113))));
  final Action checkedAgain= action("checkedAgain",on("ubuntu_gnome",()->waitUntilTime(val(37000))));
  final Action managerQuit= action("managerQuit",on("ubuntu_gnome",()->waitUntilTime(val(42000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var twice= filesIOFolder.resolve("twice");
    var second= twice.resolve("other.fearless");
    Fs.rmTree(twice);
    Fs.writeUtf8(twice.resolve("twice.fearless"),"\n");
    Fs.writeUtf8(twice.resolve("_twice").resolve("_rank_app.fear"),"use base.Main as Main;\n\nHello:Main{s->base.Debug#(\"twice\")}\n");
    launchScript("first",twice.resolve("twice.fearless"));
    runInTerminal(appsShown,terminalShown);
    managerShown.go();
    var tile= new Rectangle(69,95,128,85);
    var output= new Rectangle(410,168,1000,80);
    var valid= look();
    Fs.writeUtf8(second,"\n");
    stabilize();
    secondNoticed.go();
    changed(valid,tile);
    check.go();
    checked.go();
    changed(valid,output);
    stabilize();
    var error= List.of("twice",".fearless_out","check_error.txt");
    checkContent(error,"More than one .fearless marker file was found in[###]keep only \"twice.fearless\".");
    var invalid= look();
    Files.delete(second);
    stabilize();
    secondGone.go();
    same(valid,tile);
    checkAgain.go();
    checkedAgain.go();
    changed(invalid,output);
    stabilize();
    assertFalse(Files.exists(filesIOFolder.resolve(String.join("/",error))));
    managerMenu.go();
    quitManager.go();
    managerQuit.go();
    stabilize();
    checkContent(List.of("first.exit"),"0\n");
  }
}

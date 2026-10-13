package agentTools;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.util.List;

import tools.Fs;

final class SecondMarkerTest extends ManagerTest{
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitAfter(val(3500))),
    on("xubuntu_xfce",()->waitAfter(val(4500))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitAfter(val(5000))),
    on("xubuntu_xfce",()->waitAfter(val(6500))));
  final Action managerShown= action("managerShown",
    on("ubuntu_gnome",()->waitAfter(val(17500))),
    on("xubuntu_xfce",()->waitAfter(val(21500))));
  final Action secondNoticed= action("secondNoticed",
    on("ubuntu_gnome",()->waitAfter(val(10000))),
    on("xubuntu_xfce",()->waitAfter(val(12500))));
  final Action check= action("check",
    on("ubuntu_gnome",()->click(val(527),val(113))),
    on("xubuntu_xfce",()->click(val(478),val(93))));
  final Action checked= action("checked",
    on("ubuntu_gnome",()->waitAfter(val(4500))),
    on("xubuntu_xfce",()->waitAfter(val(6000))));
  final Action secondGone= action("secondGone",
    on("ubuntu_gnome",()->waitAfter(val(10000))),
    on("xubuntu_xfce",()->waitAfter(val(12500))));
  final Action checkAgain= action("checkAgain",
    on("ubuntu_gnome",()->click(val(527),val(113))),
    on("xubuntu_xfce",()->click(val(478),val(93))));
  final Action checkedAgain= action("checkedAgain",
    on("ubuntu_gnome",()->waitAfter(val(3500))),
    on("xubuntu_xfce",()->waitAfter(val(4000))));
  final Action managerQuit= action("managerQuit",
    on("ubuntu_gnome",()->waitAfter(val(7000))),
    on("xubuntu_xfce",()->waitAfter(val(9000))));
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
    var tile= inWindow(69,95,128,85);
    var output= inWindow(410,168,1000,80);
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

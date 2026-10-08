package agentTools;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import tools.Fs;

final class RefusedFolderTest extends ManagerTest{
  final Action appsShown= action("appsShown",
    on("ubuntu_gnome",()->waitUntilTime(val(2000))));
  final Action terminalShown= action("terminalShown",
    on("ubuntu_gnome",()->waitUntilTime(val(5500))));
  final Action noteShown= action("noteShown",
    on("ubuntu_gnome",()->waitUntilTime(val(19000))));
  final Action ok= action("ok",
    on("ubuntu_gnome",()->click(val(1952),val(1146))),
    on("windows",()->click(val(639),val(378))));
  final Action appsShownAgain= action("appsShownAgain",
    on("ubuntu_gnome",()->waitUntilTime(val(21500))));
  final Action terminalShownAgain= action("terminalShownAgain",
    on("ubuntu_gnome",()->waitUntilTime(val(25000))));
  final Action noteShownAgain= action("noteShownAgain",
    on("ubuntu_gnome",()->waitUntilTime(val(37500))));
  final Action okAgain= action("okAgain",
    on("ubuntu_gnome",()->click(val(1952),val(1194))),
    on("windows",()->click(val(639),val(425))));
  final Action appsShownRoot= action("appsShownRoot",
    on("ubuntu_gnome",()->waitUntilTime(val(40000))));
  final Action terminalShownRoot= action("terminalShownRoot",
    on("ubuntu_gnome",()->waitUntilTime(val(43500))));
  final Action rootNoteShown= action("rootNoteShown",
    on("ubuntu_gnome",()->waitUntilTime(val(56000))));
  final Action rootOk= action("rootOk",
    on("ubuntu_gnome",()->click(val(1952),val(1184))),
    on("windows",()->click(val(639),val(414))));
  final Action appsShownToEnd= action("appsShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(58500))));
  final Action terminalShownToEnd= action("terminalShownToEnd",
    on("ubuntu_gnome",()->waitUntilTime(val(62000))));
  final Action managerEnded= action("managerEnded",
    on("ubuntu_gnome",()->waitUntilTime(val(72000))));
  @Override void walk() throws Throwable{
    noManagerData();
    var gone= filesIOFolder.resolve("gone");
    var folder= filesIOFolder.resolve(data);
    Fs.rmTree(gone);
    var missing= "The manager was asked to register\n"+gone+"\nbut nothing exists there: register an existing folder, or a file inside one.\n";
    launchScript("first",gone);
    runInTerminal(appsShown,terminalShown);
    noteShown.go();
    stabilize();
    checkContent(notes,missing);
    assertFalse(Files.exists(folder.resolve("projects.info")));
    assertFalse(Files.exists(gone));
    ok.go();
    var manager= missing+"""
      Fearless cannot keep track of this folder as a project.

      The folder is:
        %s
      The manager folder of this Fearless is:
        %s
      The manager folder holds what Fearless remembers about your projects: it is
      never part of a project, and no project is inside it.
      """.formatted(folder,folder);
    refused("second",folder,appsShownAgain,terminalShownAgain,noteShownAgain,manager);
    okAgain.go();
    var root= Path.of("/").toAbsolutePath();
    refused("third",root,appsShownRoot,terminalShownRoot,rootNoteShown,manager+"""
      Fearless cannot keep track of the root of a drive or of the file system as a
      project.

      The folder is:
        %s
      Put the project in a folder inside it, and make that folder the project
      folder.
      """.formatted(root));
    rootOk.go();
    endScript();
    runInTerminal(appsShownToEnd,terminalShownToEnd);
    managerEnded.go();
    stabilize();
    checkContent(List.of("first.exit"),"137\n");
  }
  void refused(String exit, Path folder, Action appsShown, Action terminalShown, Action shown, String said) throws Throwable{
    launchScript(exit,folder);
    runInTerminal(appsShown,terminalShown);
    shown.go();
    stabilize();
    checkContent(List.of(exit+".exit"),"0\n");
    checkContent(notes,said);
    assertFalse(Files.exists(filesIOFolder.resolve(data).resolve("projects.info")));
  }
}

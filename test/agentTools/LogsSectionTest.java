package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.opentest4j.AssertionFailedError;

import tools.Fs;
import utils.Err;
import utils.OneOr;

/// The log a program writes while the manager runs it shows in the Logs section of its project, where Copy puts its text on the clipboard and Delete, once confirmed, removes it.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, nothing is registered for .fearless, the working folder of the test holds no .out, and the folder diary beside the manager holds only its marker and a program that writes dear diary into its log Diary.
/// Action 1: run the launcher on diary: the manager window opens showing diary, an idle project.
/// Action 2: click the empty space below the tiles, and move the divider between tiles and panel as far left as it goes with the keyboard.
/// Action 3: press Become code: the manager remembers diary as a code project.
/// Action 4: press Compile: the manager knows diary.Write as the one main of diary.
/// Action 5: open the Logs section: it opens with an empty list.
/// Action 6: press Run: the program exits with 0 leaving one closed log of Diary holding dear diary, and the list shows it.
/// Action 7: click the log in the list and press Copy: the clipboard holds exactly the text of the log.
/// Action 8: press Delete and answer Yes: the log is gone, and the list is empty again.
/// Action 9: end the manager.
final class LogsSectionTest extends ManagerTest{
  static{ Err.setUp(AssertionFailedError.class,Assertions::assertEquals,Assertions::assertTrue); }
  static final Path diary= data.resolveSibling("diary");
  static final Path logs= diary.resolve(".out").resolve("logs").resolve("diary");
  final At managerShown= new At("managerShown",linux(3000));
  final Click focusTiles= new Click("focusTiles",linux(200,1500));
  final Click becomeCode= new Click("becomeCode",linux(400,140));
  final Click compile= new Click("compile",linux(164,111));
  final At compileShown= new At("compileShown",linux(1000));
  final Click openLogs= new Click("openLogs",linux(128,253));
  final Area list= new Area("list",linux(80,240,3750,170));
  final Click run= new Click("run",linux(164,111));
  final Click firstLog= new Click("firstLog",linux(250,278));
  final Click copy= new Click("copy",linux(3739,251));
  final Click delete= new Click("delete",linux(3796,251));
  final At confirmShown= new At("confirmShown",linux(1500));
  final Click yes= new Click("yes",linux(1933,1150));
  @Override protected void walk() throws Exception{
    clean();
    Fs.writeUtf8(diary.resolve("diary.fearless"),"\n");
    Fs.writeUtf8(diary.resolve("_diary").resolve("_rank_app.fear"),"""
      use base.Main as Main;
      use base.FileLog as FileLog;

      Diary: FileLog{"diary"}
      Write: Main{sys -> Diary.log("dear diary")}
      """);
    launch(diary.toString());
    managerShown.go();
    focusTiles.go();
    pilot.chord(KeyEvent.VK_F8);
    pilot.chord(KeyEvent.VK_HOME);
    becomeCode.go();
    until(()->Fs.readUtf8(info).contains("\"code\""));
    var noMains= Fs.readUtf8(state);
    compile.go();
    until(()->!noMains.equals(Fs.readUtf8(state)));
    assert Fs.readUtf8(state).contains("\"diary.Write\": \"_diary/_rank_app.fear\"");
    compileShown.go();
    openLogs.go();
    look();
    var at= list.aim();
    var empty= pixels(at);
    run.go();
    until(()->Fs.readUtf8(state).contains("\"exit\": \"0\""));
    look();
    until(()->!Arrays.equals(empty,pixels(at)));
    var log= Fs.walk(logs,s->OneOr.of("one log",s.filter(Files::isRegularFile)));
    Err.strCmp("Diary$[###].log",log.getFileName().toString());
    Err.strCmp("[###] dear diary\n",Fs.readUtf8(log));
    firstLog.go();
    copy.go();
    assertEquals(Fs.readUtf8(log),Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor));
    delete.go();
    confirmShown.go();
    yes.go();
    until(()->!Files.exists(log));
    look();
    until(()->Arrays.equals(empty,pixels(at)));
    stopManagers();
  }
  @Override @AfterEach void clean() throws Exception{
    super.clean();
    Fs.rmTree(diary);
    Fs.rmTree(Path.of(".out"));
  }
}

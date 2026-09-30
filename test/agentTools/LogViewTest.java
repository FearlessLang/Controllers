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

/// The Logs section lists the logs of two runs newest first, and View shows the whole text of the log chosen in the list, in a dialog that goes away with OK.
///
/// Prerequisite: the desk shows its background with no window over it, and the manager DeployManagedFearless.java builds is deployed.
///
/// Setup: no manager runs, the manager has no data folder, nothing is registered for .fearless, the working folder of the test holds no .out, and the folder journal beside the manager holds only its marker and a program that writes dear diary then good night into its log Diary.
/// Action 1: run the launcher on journal: the manager window opens showing journal, an idle project.
/// Action 2: click the empty space below the tiles, and move the divider between tiles and panel as far left as it goes with the keyboard.
/// Action 3: press Become code: the manager remembers journal as a code project.
/// Action 4: press Compile: the manager knows journal.Write as the one main of journal.
/// Action 5: open the Logs section: it opens with an empty list.
/// Action 6: press Run: the program exits with 0 leaving one closed log of Diary holding its two lines, and the list shows it.
/// Action 7: press Run again: the program exits with 0 leaving a second closed log, and the list changes to show both.
/// Action 8: click the second log in the list and press View: a dialog opens.
/// Action 9: click in its text and copy all of it: the clipboard holds exactly the text of the older log.
/// Action 10: press OK: the dialog goes away.
/// Action 11: click the first log in the list and press View: a dialog opens.
/// Action 12: click in its text and copy all of it: the clipboard holds exactly the text of the newer log.
/// Action 13: press OK: the dialog goes away.
/// Action 14: end the manager.
final class LogViewTest extends ManagerTest{
  static{ Err.setUp(AssertionFailedError.class,Assertions::assertEquals,Assertions::assertTrue); }
  static final Path journal= data.resolveSibling("journal");
  static final Path logs= journal.resolve(".out").resolve("logs").resolve("journal");
  final At managerShown= new At("managerShown",linux(3000));
  final Click focusTiles= new Click("focusTiles",linux(200,1500));
  final Click becomeCode= new Click("becomeCode",linux(400,140));
  final Click compile= new Click("compile",linux(164,111));
  final At compileShown= new At("compileShown",linux(1000));
  final Click openLogs= new Click("openLogs",linux(128,253));
  final Area list= new Area("list",linux(80,240,3750,170));
  final Area dialog= new Area("dialog",linux(1600,865,700,25));
  final Click run= new Click("run",linux(164,111));
  final Click runAgain= new Click("runAgain",linux(164,111));
  final Click olderLog= new Click("olderLog",linux(250,296));
  final Click view= new Click("view",linux(3688,251));
  final Click focusText= new Click("focusText",linux(1958,1100));
  final Click ok= new Click("ok",linux(1958,1340));
  final Click newerLog= new Click("newerLog",linux(250,278));
  final Click viewNewer= new Click("viewNewer",linux(3688,251));
  final Click focusNewerText= new Click("focusNewerText",linux(1958,1100));
  final Click okNewer= new Click("okNewer",linux(1958,1340));
  @Override protected void walk() throws Exception{
    clean();
    Fs.writeUtf8(journal.resolve("journal.fearless"),"\n");
    Fs.writeUtf8(journal.resolve("_journal").resolve("_rank_app.fear"),"""
      use base.Main as Main;
      use base.Block as Block;
      use base.FileLog as FileLog;

      Diary: FileLog{"diary"}
      Write: Main{sys -> Block#.do{Diary.log("dear diary")}.do{Diary.log("good night")}.done}
      """);
    launch(journal.toString());
    managerShown.go();
    focusTiles.go();
    pilot.chord(KeyEvent.VK_F8);
    pilot.chord(KeyEvent.VK_HOME);
    becomeCode.go();
    until(()->Fs.readUtf8(info).contains("\"code\""));
    compile.go();
    until(()->Fs.readUtf8(state).contains("\"journal.Write\": \"_journal/_rank_app.fear\""));
    compileShown.go();
    openLogs.go();
    look();
    var at= list.aim();
    var empty= pixels(at);
    var where= dialog.aim();
    var behind= pixels(where);
    run.go();
    until(()->Fs.readUtf8(state).contains("\"exit\": \"0\""));
    look();
    until(()->!Arrays.equals(empty,pixels(at)));
    var one= pixels(at);
    runAgain.go();
    until(()->Fs.readUtf8(state).contains("\"runs\": \"2\"") && Fs.readUtf8(state).contains("\"exit\": \"0\""));
    look();
    until(()->!Arrays.equals(one,pixels(at)));
    var both= Fs.walk(logs,s->s.filter(Files::isRegularFile).sorted().toList());
    assertEquals(2,both.size());
    for (var log: both){
      Err.strCmp("Diary$[###].log",log.getFileName().toString());
      Err.strCmp("[###] dear diary\n[###] good night\n",Fs.readUtf8(log));
    }
    olderLog.go();
    view.go();
    until(()->!Arrays.equals(behind,pixels(where)));
    focusText.go();
    assertEquals(Fs.readUtf8(both.getFirst()),copied());
    ok.go();
    until(()->Arrays.equals(behind,pixels(where)));
    newerLog.go();
    viewNewer.go();
    until(()->!Arrays.equals(behind,pixels(where)));
    focusNewerText.go();
    assertEquals(Fs.readUtf8(both.getLast()),copied());
    okNewer.go();
    until(()->Arrays.equals(behind,pixels(where)));
    stopManagers();
  }
  private Object copied() throws Exception{
    pilot.chord(KeyEvent.VK_CONTROL,KeyEvent.VK_A);
    pilot.chord(KeyEvent.VK_CONTROL,KeyEvent.VK_C);
    return Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
  }
  @Override @AfterEach void clean() throws Exception{
    super.clean();
    Fs.rmTree(journal);
    Fs.rmTree(Path.of(".out"));
  }
}

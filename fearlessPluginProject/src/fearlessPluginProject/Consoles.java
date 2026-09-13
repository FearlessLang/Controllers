package fearlessPluginProject;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.MessageConsole;

/// One Eclipse console per project, named after it, holding what the manager's
/// Output area for that project holds: compile output, run output, problems; and
/// one for the manager's own notes. The type tells them apart: Links applies to
/// the project consoles only.
public final class Consoles{
  public static final String projectType= "fearlessPluginProject.project";
  public static final String notesType= "fearlessPluginProject.notes";
  private Consoles(){}
  public static void print(String name, String type, String text, boolean fresh){
    var manager= ConsolePlugin.getDefault().getConsoleManager();
    MessageConsole console= null;
    for (var c : manager.getConsoles()){ if (c.getName().equals(name)){ console= (MessageConsole)c; } }
    if (console == null){ console= new MessageConsole(name, type, null, true); manager.addConsoles(new IConsole[]{console}); }
    if (fresh){ console.clearConsole(); }
    try(var stream= console.newMessageStream()){ stream.print(text); }
    catch(IOException e){ throw new UncheckedIOException(e); }
    manager.showConsoleView(console);
  }
}

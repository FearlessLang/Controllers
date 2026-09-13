package fearlessPluginProject;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.ui.PlatformUI;

import fearlessPluginProject.ManagerLink.State;

/// The question an idle project gets when it is used, an edit of it saved or a run asked:
/// the manager compiles and runs code projects and checks data projects, and leaves an idle
/// one alone. The kind chosen is sent to the manager and returned; leaving it idle returns nothing.
public final class Idle{
  private static final List<String> kinds= List.of("code", "data:readOnly", "data:readWrite");
  private Idle(){}
  public static boolean is(ManagerLink link, String alias){ return link.state(alias).map(State::kind).orElse("").equals("idle"); }
  public static Optional<String> ask(ManagerLink link, String alias, Path folder){
    var shell= PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
    var message= alias+" is an idle project: the manager compiles and runs code projects and checks data projects, and leaves an idle one alone.";
    var choice= MessageDialog.open(MessageDialog.QUESTION, shell, "Fearless", message, SWT.NONE, "Become code", "Become data", "Become editable data", "Leave idle");
    if (choice < 0 || choice >= kinds.size()){ return Optional.empty(); }
    link.send("kind", folder, kinds.get(choice));
    return Optional.of(kinds.get(choice));
  }
}

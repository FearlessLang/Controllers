package fearlessPluginProject;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.console.TextConsoleViewer;
import org.eclipse.ui.part.ViewPart;

public final class Output extends ViewPart{
  static final String id= "fearlessPluginProject.output";
  private static final MessageConsole console= new MessageConsole("Fearless", null);
  static{ console.addPatternMatchListener(new Links()); }
  static volatile String alias= "";
  @Override public void createPartControl(Composite parent){ new TextConsoleViewer(parent, console).setEditable(false); }
  @Override public void setFocus(){}
  static MessageConsoleStream start(String alias){
    Output.alias = alias;
    console.clearConsole();
    Display.getDefault().asyncExec(Output::show);
    return console.newMessageStream();
  }
  private static void show(){
    var page= PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
    if (!page.getPerspective().getId().equals(FearlessPerspective.id)){ return; }
    try{ page.showView(id, null, IWorkbenchPage.VIEW_VISIBLE); }
    catch (PartInitException e){ throw new IllegalStateException(e); }
  }
}

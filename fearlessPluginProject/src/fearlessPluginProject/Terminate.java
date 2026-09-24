package fearlessPluginProject;

import java.util.stream.Stream;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

/// The Terminate button beside the Run button of the Fearless perspective: ends every Fearless
/// program the manager is running, as the Terminate button of a program's console ends that
/// one. Enabled while one runs: a Process tells when it starts and ends by a debug event, and the
/// toolbar shows a change of enablement only when asked to.
public final class Terminate extends AbstractHandler implements IDebugEventSetListener{
  public Terminate(){
    DebugPlugin.getDefault().addDebugEventListener(this);
    handleDebugEvents(null);
  }
  @Override public Object execute(ExecutionEvent event){
    live().forEach(Process::terminate);
    return null;
  }
  private static Stream<Process> live(){
    return Stream.of(DebugPlugin.getDefault().getLaunchManager().getProcesses()).filter(p->p instanceof Process && p.canTerminate()).map(p->(Process)p);
  }
  @Override public void handleDebugEvents(DebugEvent[] events){
    var on= live().findAny().isPresent();
    Display.getDefault().asyncExec(()->enable(on));
  }
  private void enable(boolean on){
    setBaseEnabled(on);
    PlatformUI.getWorkbench().getService(IEventBroker.class).post(UIEvents.REQUEST_ENABLEMENT_UPDATE_TOPIC, UIEvents.ALL_ELEMENT_ID);
  }
  @Override public void dispose(){ DebugPlugin.getDefault().removeDebugEventListener(this); }
}

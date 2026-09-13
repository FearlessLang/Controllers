package fearlessPluginProject;

import java.util.stream.Stream;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchesListener2;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.swt.widgets.Display;

/// The Terminate button beside the Run button of the Fearless perspective: ends every Fearless
/// program the manager is running, as the Terminate button of a program's console ends that
/// one. Enabled while one runs: the launch manager tells when a Process starts and ends.
public final class Terminate extends AbstractHandler implements ILaunchesListener2{
  public Terminate(){
    DebugPlugin.getDefault().getLaunchManager().addLaunchListener(this);
    update();
  }
  @Override public Object execute(ExecutionEvent event){
    live().forEach(Process::terminate);
    return null;
  }
  private static Stream<Process> live(){
    return Stream.of(DebugPlugin.getDefault().getLaunchManager().getLaunches()).flatMap(l->Stream.of(l.getProcesses()))
      .filter(p->p instanceof Process && p.canTerminate()).map(p->(Process)p);
  }
  private void update(){
    var on= live().findAny().isPresent();
    Display.getDefault().asyncExec(()->setBaseEnabled(on));
  }
  @Override public void launchesAdded(ILaunch[] launches){ update(); }
  @Override public void launchesRemoved(ILaunch[] launches){ update(); }
  @Override public void launchesChanged(ILaunch[] launches){ update(); }
  @Override public void launchesTerminated(ILaunch[] launches){ update(); }
  @Override public void dispose(){ DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(this); }
}

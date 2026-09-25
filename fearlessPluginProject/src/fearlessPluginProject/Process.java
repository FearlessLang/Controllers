package fearlessPluginProject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.IFlushableStreamMonitor;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.core.model.IStreamsProxy;

/// A main the manager is running, shown as Eclipse shows a process it started itself: in
/// the Debug view, and in the Console view with its output and its Terminate button. The
/// watcher starts it when the manager's state counts a new run, appends the output the manager
/// writes meanwhile, and ends it when the state names no running main; Terminate asks the
/// manager to end the job.
public final class Process extends PlatformObject implements IProcess{
  static final String type= "fearless";
  static final String aliasAttr= "fearlessPluginProject.alias";
  private final ILaunch launch;
  private final String alias;
  private final String label;
  private final Map<String,String> attributes= new HashMap<>();
  private final Stream out= new Stream();
  private int exit;
  private boolean terminated;
  private Process(ILaunch launch, String alias, String label){
    this.launch= launch;
    this.alias= alias;
    this.label= label;
  }
  static Process start(String alias, String main){
    var launch= new Launch(null, ILaunchManager.RUN_MODE, null);
    var res= new Process(launch, alias, alias+" - "+main);
    res.setAttribute(ATTR_PROCESS_TYPE, type);
    res.setAttribute(aliasAttr, alias);
    launch.addProcess(res);
    DebugPlugin.getDefault().getLaunchManager().addLaunch(launch);
    res.fire(DebugEvent.CREATE);
    return res;
  }
  void append(String text){ out.append(text); }
  void ended(int exit){
    this.exit= exit;
    terminated= true;
    fire(DebugEvent.TERMINATE);
  }
  private void fire(int kind){ DebugPlugin.getDefault().fireDebugEventSet(new DebugEvent[]{new DebugEvent(this, kind)}); }
  @Override public boolean canTerminate(){ return !terminated; }
  @Override public boolean isTerminated(){ return terminated; }
  @Override public void terminate(){ ManagerLink.send("terminate", alias); }
  @Override public String getLabel(){ return label; }
  @Override public ILaunch getLaunch(){ return launch; }
  @Override public IStreamsProxy getStreamsProxy(){ return out; }
  @Override public void setAttribute(String key, String value){ attributes.put(key, value); }
  @Override public String getAttribute(String key){ return attributes.get(key); }
  @Override public int getExitValue() throws DebugException{
    if (!terminated){ throw new DebugException(Status.error(label+" has not ended yet")); }
    return exit;
  }
  /// The output stream of the process, which is also its own streams proxy; a Fearless
  /// program run by the manager has no error stream and takes no input from the console.
  private static final class Stream implements IStreamsProxy, IFlushableStreamMonitor{
    private final ListenerList<IStreamListener> listeners= new ListenerList<>();
    private final StringBuilder buffer= new StringBuilder();
    private boolean buffered= true;
    synchronized void append(String text){
      if (buffered){ buffer.append(text); }
      listeners.forEach(l->l.streamAppended(text, this));
    }
    @Override public IStreamMonitor getErrorStreamMonitor(){ return null; }
    @Override public IStreamMonitor getOutputStreamMonitor(){ return this; }
    @Override public void write(String input) throws IOException{ throw new IOException("A Fearless program run by the manager takes no input from this console."); }
    @Override public void addListener(IStreamListener listener){ listeners.add(listener); }
    @Override public void removeListener(IStreamListener listener){ listeners.remove(listener); }
    @Override public synchronized String getContents(){ return buffer.toString(); }
    @Override public synchronized void flushContents(){ buffer.setLength(0); }
    @Override public synchronized void setBuffered(boolean buffer){ buffered= buffer; }
    @Override public synchronized boolean isBuffered(){ return buffered; }
  }
}

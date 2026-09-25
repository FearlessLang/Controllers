package fearlessPluginProject;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;

/// Launching a Fearless configuration (its attributes: the project name, and the main
/// to run or "" for the mains selected in the manager) is one run message to the manager.
/// The run then shows up as the Process the watcher creates once the manager reports it,
/// so the launch Eclipse made here is dropped at once. Nothing is built before it: the
/// manager compiles a stale project itself before running it.
public final class Launcher extends LaunchConfigurationDelegate{
  public static final String type= "fearlessPluginProject.launch";
  public static final String projectAttr= "project";
  public static final String mainAttr= "main";
  @Override public boolean buildForLaunch(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor){ return false; }
  @Override public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException{
    ManagerLink.send("run", configuration.getAttribute(projectAttr, ""), configuration.getAttribute(mainAttr, ""));
    DebugPlugin.getDefault().getLaunchManager().removeLaunch(launch);
  }
}

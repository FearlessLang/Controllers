package fearlessPluginProject;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;

/// Launching a Fearless configuration (its attributes: the project name, and the main
/// to run or "" for the mains selected in the manager) does what the main button of the
/// manager does: a Compile, and when there is nothing to compile a Run of the main, else of
/// the selected mains, else of the only main. Eclipse follows the job it asked for, and
/// starts no other until that job ends. The launch Eclipse made here is dropped at once.
public final class Launcher extends LaunchConfigurationDelegate{
  public static final String type= "fearlessPluginProject.launch";
  public static final String projectAttr= "project";
  public static final String mainAttr= "main";
  @Override public boolean buildForLaunch(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor){ return false; }
  @Override public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException{
    DebugPlugin.getDefault().getLaunchManager().removeLaunch(launch);
    if (Follow.followed().isPresent()){ return; }
    var alias= configuration.getAttribute(projectAttr, "");
    var main= configuration.getAttribute(mainAttr, "");
    Follow.compile(alias, ()->Follow.run(alias, mains(alias, main)));
  }
  private static String[] mains(String alias, String main){
    if (!main.isEmpty()){ return new String[]{main}; }
    var p= ManagerLink.project(alias);
    var all= ManagerLink.mains(p.folder()).keySet();
    return (all.size() == 1 ? all.stream() : p.mains().stream()).toArray(String[]::new);
  }
}

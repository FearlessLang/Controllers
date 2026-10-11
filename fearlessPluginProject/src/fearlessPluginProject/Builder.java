package fearlessPluginProject;

import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.IProgressMonitor;

/// Project > Build asks the manager to compile a code project with valid content, following the compile, and to
/// check any other project; Project > Clean asks it to delete the compiled cache. An automatic
/// build asks nothing: saving a file compiles through Save.
public final class Builder extends IncrementalProjectBuilder{
  public static final String id= "fearlessPluginProject.builder";
  @Override protected IProject[] build(int kind, Map<String,String> args, IProgressMonitor monitor){
    if (kind == AUTO_BUILD){ return null; }
    var alias= getProject().getName();
    if (MainButton.of(alias, ManagerLink.projects()).action() == MainButton.Action.check){ ManagerLink.send("Check", alias); return null; }
    if (Follow.followed().isEmpty()){ Follow.compile(alias); }
    return null;
  }
  @Override protected void clean(IProgressMonitor monitor){ ManagerLink.send("Clean", getProject().getName()); }
}

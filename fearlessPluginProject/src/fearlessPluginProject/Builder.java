package fearlessPluginProject;

import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.IProgressMonitor;

/// Project > Clean asks the manager to delete the compilation cache. A build asks nothing:
/// saving a file compiles through Save.
public final class Builder extends IncrementalProjectBuilder{
  public static final String id= "fearlessPluginProject.builder";
  @Override protected IProject[] build(int kind, Map<String,String> args, IProgressMonitor monitor){ return null; }
  @Override protected void clean(IProgressMonitor monitor){ ManagerLink.send("DeleteCache", FearlessWatcher.alias(getProject())); }
}

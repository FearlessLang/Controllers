package fearlessPluginProject;

import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/// Project > Build asks the manager to compile a code project (to check a data project);
/// Project > Clean asks it to clear the compiled cache. An automatic build asks only when
/// a file of the project itself changed: what the manager writes under the project folder
/// (its cache and its logs) comes back as a delta too, and must not start another compile.
public final class Builder extends IncrementalProjectBuilder{
  public static final String id= "fearlessPluginProject.builder";
  private static final List<String> written= List.of(".fearless_out", ".out");
  @Override protected IProject[] build(int kind, Map<String,String> args, IProgressMonitor monitor) throws CoreException{
    var delta= getDelta(getProject());
    if (kind == AUTO_BUILD && (delta == null || !authoredChange(delta))){ return null; }
    send("compile");
    return null;
  }
  @Override protected void clean(IProgressMonitor monitor){ send("clean"); }
  private void send(String verb){
    var link= ManagerLink.find().orElseThrow();
    link.send(verb, link.projects().get(getProject().getName()));
  }
  private static boolean authoredChange(IResourceDelta delta) throws CoreException{
    var found= new boolean[1];
    delta.accept(d->visit(d, found));
    return found[0];
  }
  private static boolean visit(IResourceDelta d, boolean[] found){
    var path= d.getProjectRelativePath();
    if (path.segmentCount() > 0 && !path.segment(0).equals(FearlessWatcher.srcName)){ return false; }
    if (path.segmentCount() > 1 && written.contains(path.segment(1))){ return false; }
    found[0]|= d.getResource().getType() == IResource.FILE;
    return !found[0];
  }
}

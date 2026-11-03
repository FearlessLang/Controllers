package fearlessPluginProject;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.FrameworkUtil;


/// Started with the workbench, and every two seconds, mirrors the registered projects of the
/// manager into the workspace and reflects their files: the error of _error.txt into the
/// Problems view, and the unit test log the running main writes into the JUnit view.
/// A mirrored project lives in the workspace's own folder and reaches the real project folder
/// only through a linked folder, so Eclipse never writes into that folder. The linked folder
/// is refreshed every tick: the workspace learns of edits made on disk by anything but Eclipse
/// only through a refresh.
/// Mirrored projects carry the Fearless nature: only those are ever deleted (from the
/// workspace, never from disk) once the manager forgets them. A workspace project named like a registered
/// project that is not its mirror is an error shown once; any other error stops the job and is shown.
public final class FearlessWatcher extends Job implements IStartup{
  static final String srcName= "src";
  private static final String problemType= "fearlessPluginProject.problem";
  private static final Pattern at= Pattern.compile("(?m)^In file: fear:/(\\S+)\\n(?:.*\\n)*?(\\d+)\\| .*\\n *\\| [ ^~-]*\\^");
  private static final Pattern named= Pattern.compile("fear:/([^\\s\"]+)");
  private static volatile boolean stopped;
  private final Map<String,String> reports= new HashMap<>();
  private final Map<String,String> marks= new HashMap<>();
  private final Set<String> clashes= new HashSet<>();
  public FearlessWatcher(){
    super("Fearless connect");
    setSystem(true);
    setRule(ResourcesPlugin.getWorkspace().getRoot());
  }
  @Override public void earlyStartup(){ schedule(); }
  @Override protected IStatus run(IProgressMonitor monitor){
    if (stopped){ return Status.CANCEL_STATUS; }
    try{ tick(monitor); }
    catch (CoreException|RuntimeException|ExceptionInInitializerError e){
      stopped = true;
      StatusManager.getManager().handle(Status.error("Fearless stopped following the manager.", e), StatusManager.SHOW|StatusManager.LOG);
      return Status.CANCEL_STATUS;
    }
    if (!monitor.isCanceled()){ schedule(2000); }
    return Status.OK_STATUS;
  }
  private void tick(IProgressMonitor monitor) throws CoreException{
    var projects= ManagerLink.projects();
    var root= ResourcesPlugin.getWorkspace().getRoot();
    for (var p : root.getProjects()){
      if (Nature.marks(p) && !projects.containsKey(p.getName())){ p.delete(false, true, monitor); }
    }
    marks.keySet().retainAll(projects.keySet());
    for (var e : projects.entrySet()){
      var project= root.getProject(e.getKey());
      if (mirror(project, e.getValue(), monitor)){ reflect(project, e.getValue()); }
    }
    MainButton.look(projects);
  }
  /// False for a workspace project that is not the mirror of the registered project of that name, or that the user closed.
  private boolean mirror(IProject project, ManagerLink.Project p, IProgressMonitor monitor) throws CoreException{
    if (!project.exists()){
      marks.remove(project.getName());
      project.create(monitor);
      project.open(monitor);
      var description= project.getDescription();
      description.setNatureIds(new String[]{Nature.id});
      project.setDescription(description, monitor);
    }
    if (!project.isOpen()){ return false; }
    if (!Nature.marks(project)){ clash(project.getName()); return false; }
    clashes.remove(project.getName());
    var src= project.getFolder(srcName);
    //TO DECIDE AND TEST: a Windows folder whose name holds an unpaired surrogate is linked by its
    //exact path, but Eclipse saves the link in .project as UTF-8, where that unit becomes '?':
    //after Eclipse restarts, the mirror names a folder that does not exist.
    var location= new Path(p.folder().toString());
    if (!location.equals(src.getLocation())){ src.createLink(location, IResource.REPLACE|IResource.ALLOW_MISSING_LOCAL, monitor); }
    src.refreshLocal(IResource.DEPTH_INFINITE, monitor);
    return true;
  }
  private void clash(String name){
    if (!clashes.add(name)){ return; }
    StatusManager.getManager().handle(Status.error("The workspace has a project named \""+name+"\" that is not the Fearless project of that name: rename it or delete it."), StatusManager.SHOW|StatusManager.LOG);
  }
  private void reflect(IProject project, ManagerLink.Project p) throws CoreException{
    var alias= project.getName();
    var problem= ManagerLink.error(p.folder()).orElse("");
    var m= at.matcher(problem);
    var lined= m.find();
    var f= lined ? m : named.matcher(problem);
    var found= lined || f.find();
    var on= found ? project.getFolder(srcName).getFile(new Path(f.group(1))) : project;
    var target= found && on.exists() ? on : project;
    var key= problem+" on "+target.getFullPath();
    if (!key.equals(marks.put(alias, key))){
      project.deleteMarkers(problemType, true, IResource.DEPTH_INFINITE);
      mark(target, problem, lined && target != project ? Integer.parseInt(m.group(2)) : 0);
    }
    var report= UnitTests.report(alias, p.folder());
    if (report.isEmpty() || report.get().equals(reports.put(alias, report.get()))){ return; }
    var copy= Platform.getStateLocation(FrameworkUtil.getBundle(getClass())).append("report.xml").toFile().toPath();
    try{ Files.writeString(copy, report.get()); }
    catch (IOException e){ throw new UncheckedIOException(e); }
    JUnitCore.importTestRunSession(copy.toFile());
  }
  private static void mark(IResource on, String problem, int line) throws CoreException{
    if (problem.isEmpty()){ return; }
    var marker= on.createMarker(problemType);
    if (line > 0){ marker.setAttribute(IMarker.LINE_NUMBER, line); }
    marker.setAttribute(IMarker.MESSAGE, problem);
    marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
  }
}

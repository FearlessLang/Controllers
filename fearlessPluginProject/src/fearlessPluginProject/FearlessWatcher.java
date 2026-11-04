package fearlessPluginProject;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

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
  static final String mirrorPrefix= "fearless_";
  static IProject mirror(String alias){ return ResourcesPlugin.getWorkspace().getRoot().getProject(mirrorPrefix+alias); }
  static String alias(IProject p){ return p.getName().startsWith(mirrorPrefix) ? p.getName().substring(mirrorPrefix.length()) : ""; }
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
  @Override public void earlyStartup(){ FearlessPerspective.hideRunAndDebug(); schedule(); }
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
    var workspace= real(root.getLocation().toFile().toPath());
    var around= Stream.concat(Stream.of(ManagerLink.manager), projects.values().stream().map(ManagerLink.Project::folder)).filter(f -> f.startsWith(workspace) || workspace.startsWith(f)).findFirst();
    if (around.isPresent()){ shown("The Eclipse workspace \""+workspace+"\" and the folder \""+around.get()+"\" are inside one another: Fearless mirrors no project until the workspace is outside every Fearless project folder and the manager folder."); return; }
    for (var p : root.getProjects()){
      if (Nature.marks(p) && !projects.containsKey(alias(p))){ p.delete(false, true, monitor); }
    }
    marks.keySet().retainAll(projects.keySet());
    for (var e : projects.entrySet()){
      var project= mirror(e.getKey());
      if (mirror(project, e.getValue(), monitor)){ reflect(project, e.getValue()); }
    }
    MainButton.look(projects);
  }
  /// False for a workspace project that is not the mirror of the registered project of that name.
  private boolean mirror(IProject project, ManagerLink.Project p, IProgressMonitor monitor) throws CoreException{
    if (!project.exists()){
      marks.remove(alias(project));
      project.create(monitor);
      project.open(monitor);
      var description= project.getDescription();
      description.setNatureIds(new String[]{Nature.id});
      project.setDescription(description, monitor);
    }
    var closedOther= !project.isOpen() && !List.of(project.getWorkspace().loadProjectDescription(project.getLocation().append(".project")).getNatureIds()).contains(Nature.id);
    if (closedOther){ clash(project); return false; }
    project.open(monitor);
    if (!Nature.marks(project)){ clash(project); return false; }
    clashes.remove(clashText(project));
    var src= project.getFolder(srcName);
    //TO DECIDE AND TEST: a Windows folder whose name holds an unpaired surrogate is linked by its
    //exact path, but Eclipse saves the link in .project as UTF-8, where that unit becomes '?':
    //after Eclipse restarts, the mirror names a folder that does not exist.
    var location= new Path(p.folder().toString());
    if (!location.equals(src.getLocation())){ src.createLink(location, IResource.REPLACE|IResource.ALLOW_MISSING_LOCAL, monitor); }
    src.refreshLocal(IResource.DEPTH_INFINITE, monitor);
    return true;
  }
  private void clash(IProject project){ shown(clashText(project)); }
  private static String clashText(IProject project){ return "The workspace has a project named \""+project.getName()+"\" that is not the mirror of the Fearless project \""+alias(project)+"\": rename it or delete it."; }
  private void shown(String error){
    if (!clashes.add(error)){ return; }
    StatusManager.getManager().handle(Status.error(error), StatusManager.SHOW|StatusManager.LOG);
  }
  private static java.nio.file.Path real(java.nio.file.Path p){
    try{ return p.toRealPath(); }
    catch (IOException e){ throw new UncheckedIOException(e); }
  }
  private void reflect(IProject project, ManagerLink.Project p) throws CoreException{
    var alias= alias(project);
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
    var report= UnitTests.report(alias, p.folder(), Follow.lastRun(alias));
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

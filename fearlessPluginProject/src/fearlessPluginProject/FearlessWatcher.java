package fearlessPluginProject;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

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
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.FrameworkUtil;

import fearlessPluginProject.ManagerLink.Project;

/// Started with the workbench, and every two seconds, mirrors the manager's registered projects
/// into the workspace and reflects their files: the problem into the Problems view and the report
/// into the JUnit view. Its Output job, every fifth of a second, shows the consoles: the console
/// into a Console view console named after the project, and every run the manager started into
/// a Process that ends once the manager reports no main running.
/// A mirrored project lives in the workspace's own folder and reaches the real project folder
/// only through a linked folder, so Eclipse never writes into that folder. The linked folder
/// is refreshed every tick: the workspace learns of edits made on disk by anything but Eclipse
/// only through a refresh, and an edited source then gets its automatic build.
/// Mirrored projects carry the Fearless nature: only those are ever deleted (from the
/// workspace, never from disk) once the manager forgets them. An error stops both jobs and
/// is shown.
public final class FearlessWatcher extends Job implements IStartup{
  static final String srcName= "src";
  static final String consolePrefix= "Fearless ";
  static final String consoleType= "fearlessPluginProject.project";
  private static final String problemType= "fearlessPluginProject.problem";
  private static volatile boolean stopped;
  private final Map<String,String> reports= new HashMap<>();
  private final Map<String,String> marks= new HashMap<>();
  public FearlessWatcher(){
    super("Fearless connect");
    setSystem(true);
    setRule(ResourcesPlugin.getWorkspace().getRoot());
  }
  @Override public void earlyStartup(){
    schedule();
    new Output().schedule();
  }
  @Override protected IStatus run(IProgressMonitor monitor){ return again(this, 2000, ()->tick(monitor), monitor); }
  interface Tick{ void run() throws CoreException; }
  static IStatus again(Job job, long every, Tick tick, IProgressMonitor monitor){
    if (stopped){ return Status.CANCEL_STATUS; }
    try{ tick.run(); }
    catch(CoreException|RuntimeException|ExceptionInInitializerError e){
      stopped= true;
      StatusManager.getManager().handle(Status.error("Fearless stopped following the manager.", e), StatusManager.SHOW|StatusManager.LOG);
      return Status.CANCEL_STATUS;
    }
    if (!monitor.isCanceled()){ job.schedule(every); }
    return Status.OK_STATUS;
  }
  private void tick(IProgressMonitor monitor) throws CoreException{
    var projects= ManagerLink.projects();
    var root= ResourcesPlugin.getWorkspace().getRoot();
    for (var p : root.getProjects()){
      if (Nature.marks(p) && !projects.containsKey(p.getName())){ p.delete(false, true, monitor); }
    }
    marks.keySet().retainAll(projects.keySet());
    for (var e : projects.entrySet()){ reflect(mirror(root.getProject(e.getKey()), e.getValue(), monitor), e.getValue()); }
  }
  private static IProject mirror(IProject project, Project p, IProgressMonitor monitor) throws CoreException{
    if (!project.exists()){
      project.create(monitor);
      project.open(monitor);
      var description= project.getDescription();
      description.setNatureIds(new String[]{Nature.id});
      project.setDescription(description, monitor);
    }
    project.open(monitor);
    if (!Nature.marks(project)){ throw new IllegalStateException("The workspace has a project named \""+project.getName()+"\" that is not the Fearless project of that name: rename it or delete it."); }
    var src= project.getFolder(srcName);
    //TO DECIDE AND TEST: a Windows folder whose name holds an unpaired surrogate is linked by its
    //exact path, but Eclipse saves the link in .project as UTF-8, where that unit becomes '?':
    //after Eclipse restarts, the mirror names a folder that does not exist.
    var location= new Path(p.folder().toString());
    if (!location.equals(src.getLocation())){ src.createLink(location, IResource.REPLACE, monitor); }
    src.refreshLocal(IResource.DEPTH_INFINITE, monitor);
    return project;
  }
  private void reflect(IProject project, Project p) throws CoreException{
    var alias= project.getName();
    var on= on(project, p.problem());
    var key= p.problem()+" on "+on.getFullPath();
    if (!key.equals(marks.put(alias, key))){ mark(project, on, p.problem()); }
    var report= ManagerLink.eclipse().resolve(alias).resolve("report.xml");
    if (!Files.exists(report)){ return; }
    var xml= ManagerLink.read(report);
    if (xml.equals(reports.put(alias, xml))){ return; }
    var copy= Platform.getStateLocation(FrameworkUtil.getBundle(getClass())).append("report.xml").toFile().toPath();
    try{ Files.writeString(copy, xml); }
    catch(IOException e){ throw new UncheckedIOException(e); }
    JUnitCore.importTestRunSession(copy.toFile());
  }
  private static IResource on(IProject project, Map<String,String> problem){
    if (problem.isEmpty()){ return project; }
    var file= project.getFolder(srcName).getFile(new Path(problem.get("file")));
    return file.exists() ? file : project;
  }
  private static void mark(IProject project, IResource on, Map<String,String> problem) throws CoreException{
    project.deleteMarkers(problemType, true, IResource.DEPTH_INFINITE);
    if (problem.isEmpty()){ return; }
    var marker= on.createMarker(problemType);
    if (on != project){ marker.setAttribute(IMarker.LINE_NUMBER, Integer.parseInt(problem.get("line"))); }
    marker.setAttribute(IMarker.MESSAGE, problem.get("message"));
    marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
  }
  /// The consoles and the Processes of the runs; it holds no rule, so it never waits for a build.
  private static final class Output extends Job{
    private final Map<String,String> shown= new HashMap<>();
    private final Map<String,String> stamps= new HashMap<>();
    private final Map<String,MessageConsole> consoles= new HashMap<>();
    private final Map<String,Project> seen= new HashMap<>();
    private final Map<String,Process> live= new HashMap<>();
    Output(){
      super("Fearless output");
      setSystem(true);
    }
    @Override protected IStatus run(IProgressMonitor monitor){ return again(this, 200, this::tick, monitor); }
    private void tick(){
      var projects= ManagerLink.projects();
      tail("Fearless", "fearlessPluginProject.notes", ManagerLink.eclipse().resolve("console.txt"));
      live.keySet().stream().filter(a->!projects.containsKey(a)).toList().forEach(a->live.remove(a).ended(-1));
      seen.keySet().retainAll(projects.keySet());
      projects.forEach(this::reflect);
    }
    private void reflect(String alias, Project p){
      var before= seen.put(alias, p);
      if (before != null && before.runs() < p.runs() && !live.containsKey(alias)){ live.put(alias, Process.start(alias, p.lastRun())); }
      tail(alias, consoleType, ManagerLink.eclipse().resolve(alias).resolve("console.txt"));
      if (p.running().isEmpty() && live.containsKey(alias)){ live.remove(alias).ended(p.exit()); }
    }
    /// The console shows what the file holds: what was added goes to the live Process of the
    /// project if any, else to its console; a file that is no longer an extension of what was
    /// shown is shown again from the start. A file of the same size and time as last read is not read again.
    private void tail(String alias, String type, java.nio.file.Path file){
      String stamp;
      try{ stamp= Files.size(file)+" "+Files.getLastModifiedTime(file); }
      catch(IOException e){ throw new UncheckedIOException(e); }
      if (stamp.equals(stamps.put(alias, stamp))){ return; }
      var text= ManagerLink.lines(file);
      var before= shown.getOrDefault(alias, "");
      if (text.equals(before)){ return; }
      shown.put(alias, text);
      var fresh= !text.startsWith(before);
      var added= fresh ? text : text.substring(before.length());
      var process= live.get(alias);
      if (process != null && !fresh){ process.append(added); return; }
      var console= consoles.computeIfAbsent(alias, a->console(a, type));
      if (fresh){ console.clearConsole(); }
      try(var stream= console.newMessageStream()){ stream.print(added); }
      catch(IOException e){ throw new UncheckedIOException(e); }
      ConsolePlugin.getDefault().getConsoleManager().showConsoleView(console);
    }
    private static MessageConsole console(String alias, String type){
      var res= new MessageConsole(type.equals(consoleType) ? consolePrefix+alias : alias, type, null, true);
      ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[]{res});
      return res;
    }
  }
}

package fearlessPluginProject;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import fearlessPluginProject.ManagerLink.State;

/// Every two seconds, mirrors the manager's registered projects into the workspace and
/// reflects their reports: problems into the Problems view, test reports into the JUnit
/// view, output into the Console view, and every run the manager started into a Process
/// that ends once the manager reports no main running (a run over within one period
/// still gets its Process, ended in the same tick).
/// A mirrored project lives in the workspace's own folder and reaches the real project
/// folder only through a linked folder, so Eclipse never writes into that folder.
/// Mirrored projects carry the Fearless nature: only those are ever deleted (from the
/// workspace, never from disk) once the manager forgets them.
public final class FearlessWatcher extends Job{
  private static final long periodMs= 2000;
  static final String srcName= "src";
  static final String consolePrefix= "Fearless ";
  private final Map<String,String> lastProblems= new HashMap<>();
  private final Map<String,String> lastJUnit= new HashMap<>();
  private final Map<String,Integer> shownConsole= new HashMap<>();
  private final Map<String,Process> live= new HashMap<>();
  private final Map<String,Integer> seenRuns= new HashMap<>();
  public FearlessWatcher(){
    super("Fearless connect");
    setSystem(true);
    setRule(ResourcesPlugin.getWorkspace().getRoot());
  }
  @Override protected IStatus run(IProgressMonitor monitor){
    if (monitor.isCanceled()){ return Status.CANCEL_STATUS; }
    var link= ManagerLink.find();
    try{ if (link.isPresent()){ tick(link.get(), monitor); } }
    catch(CoreException e){ return e.getStatus(); }
    schedule(periodMs);
    return Status.OK_STATUS;
  }
  private void tick(ManagerLink link, IProgressMonitor monitor) throws CoreException{
    var projects= link.projects();
    tail("Fearless", Consoles.notesType, link.console());
    var root= ResourcesPlugin.getWorkspace().getRoot();
    for (var p : root.getProjects()){
      if (Nature.marks(p) && !projects.containsKey(p.getName())){ p.delete(false, true, monitor); }
    }
    for (var e : projects.entrySet()){
      var project= root.getProject(e.getKey());
      mirror(project, e.getValue(), monitor);
      if (Nature.marks(project)){ reflect(link, project, e.getValue(), monitor); }
    }
  }
  private static void mirror(IProject project, java.nio.file.Path folder, IProgressMonitor monitor) throws CoreException{
    if (!project.exists()){
      project.create(monitor);
      project.open(monitor);
      var description= project.getDescription();
      description.setNatureIds(new String[]{Nature.id});
      project.setDescription(description, monitor);
    }
    if (!Nature.marks(project)){ return; }
    var src= project.getFolder(srcName);
    var location= new Path(folder.toString());
    if (src.exists() && location.equals(src.getLocation())){ return; }
    if (src.exists()){ src.delete(IResource.NONE, monitor); }
    src.createLink(location, IResource.NONE, monitor);
  }
  //What the file holds past what was shown: to the live process of that console if any,
  //else to the console itself, cleared first when the manager cleared the file.
  private void tail(String name, String type, java.nio.file.Path file){
    var text= ManagerLink.read(file);
    var shown= shownConsole.getOrDefault(name, 0);
    if (text.length() < shown){ shown= 0; }
    shownConsole.put(name, text.length());
    if (text.length() == shown){ return; }
    var process= live.get(name);
    if (process != null){ process.append(text.substring(shown)); return; }
    Consoles.print(name, type, text.substring(shown), shown == 0);
  }
  private void reflect(ManagerLink link, IProject project, java.nio.file.Path folder, IProgressMonitor monitor) throws CoreException{
    var alias= project.getName();
    var name= consolePrefix+alias;
    var state= link.state(alias);
    var runs= state.map(State::runs).orElse(0);
    if (runs != seenRuns.getOrDefault(alias, 0) && !live.containsKey(name)){ live.put(name, Process.start(link, alias, folder, state.get().lastRun())); }
    seenRuns.put(alias, runs);
    var problems= ManagerLink.read(link.reports(alias).resolve("problems.txt"));
    if (!problems.equals(lastProblems.get(alias))){
      lastProblems.put(alias, problems);
      project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
      ProblemMarkers.apply(project.getFolder(srcName), problems);
    }
    tail(name, Consoles.projectType, link.reports(alias).resolve("console.txt"));
    if (state.map(State::running).orElse("").isEmpty() && live.containsKey(name)){ live.remove(name).ended(state.get().exit()); }
    var report= link.reports(alias).resolve("report.xml");
    var xml= ManagerLink.read(report);
    if (xml.isBlank() || xml.equals(lastJUnit.get(alias))){ return; }
    lastJUnit.put(alias, xml);
    JUnitImport.doImport(report);
  }
}

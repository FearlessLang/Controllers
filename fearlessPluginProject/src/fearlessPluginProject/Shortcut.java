package fearlessPluginProject;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

import fearlessPluginProject.ManagerLink.State;

/// Run As > Fearless Program, the Run button and Ctrl+F11 on a mirrored project or
/// anything in it: the launch configuration for the selection is found or made, then
/// launched. The mains offered are those the selected file declares, or all the known
/// ones when it declares none: one is run, several are asked about, and none known yet
/// (the project is not compiled) leaves the choice to the manager, which compiles first.
public final class Shortcut implements ILaunchShortcut{
  @Override public void launch(ISelection selection, String mode){
    launch(Adapters.adapt(((IStructuredSelection)selection).getFirstElement(), IResource.class), mode);
  }
  @Override public void launch(IEditorPart editor, String mode){ launch(Adapters.adapt(editor.getEditorInput(), IResource.class), mode); }
  private static void launch(IResource resource, String mode){
    var link= ManagerLink.find().orElseThrow();
    var alias= resource.getProject().getName();
    var mains= link.state(alias).map(State::mains).orElse(Map.of());
    var inFile= resource instanceof IFile f ? mains.entrySet().stream().filter(e->e.getValue().equals(fileOf(f))).map(Map.Entry::getKey).toList() : List.<String>of();
    var offered= inFile.isEmpty() ? List.copyOf(mains.keySet()) : inFile;
    var main= offered.size() > 1 ? choose(offered) : Optional.of(offered.isEmpty() ? "" : offered.getFirst());
    if (main.isEmpty()){ return; }
    var project= resource.getProject();
    IResource mapped= main.get().isEmpty() ? project : project.getFolder(FearlessWatcher.srcName).getFile(new org.eclipse.core.runtime.Path(mains.get(main.get())));
    try{ DebugUITools.launch(configuration(alias, link.projects().get(alias), main.get(), mapped), mode); }
    catch(CoreException e){ throw new IllegalStateException(e); }
  }
  static String fileOf(IFile f){ return f.getProjectRelativePath().removeFirstSegments(1).toString(); }
  private static Optional<String> choose(List<String> mains){
    var dialog= new ElementListSelectionDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), new LabelProvider());
    dialog.setTitle("Fearless");
    dialog.setMessage("Which main runs?");
    dialog.setElements(mains.toArray());
    return dialog.open() == Window.OK ? Optional.of((String)dialog.getFirstResult()) : Optional.empty();
  }
  /// The configuration is mapped to the file declaring its main (to the project when none
  /// is known yet): the Run button on a resource relaunches the configurations mapped
  /// inside it, and reaches the shortcut only when there is none.
  private static ILaunchConfiguration configuration(String alias, Path folder, String main, IResource mapped) throws CoreException{
    var manager= DebugPlugin.getDefault().getLaunchManager();
    var type= manager.getLaunchConfigurationType(Launcher.type);
    for (var c : manager.getLaunchConfigurations(type)){
      var same= c.getAttribute(Launcher.folderAttr, "").equals(folder.toString()) && c.getAttribute(Launcher.mainAttr, "").equals(main);
      if (same){ return c; }
    }
    var fresh= type.newInstance(null, manager.generateLaunchConfigurationName(main.isEmpty() ? alias : alias+" "+main));
    fresh.setAttribute(Launcher.folderAttr, folder.toString());
    fresh.setAttribute(Launcher.mainAttr, main);
    fresh.setMappedResources(new IResource[]{mapped});
    return fresh.doSave();
  }
}

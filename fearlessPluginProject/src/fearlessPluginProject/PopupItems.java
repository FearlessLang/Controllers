package fearlessPluginProject;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CompoundContributionItem;

/// The Fearless entries of the right-click menu on a mirrored project or anything in it,
/// the same the manager's panel offers: for a code project, Compile while the compiled
/// cache is not up to date, otherwise Run for the mains declared in the selected file (all
/// the mains of the project when the selection is not a file declaring one); Terminate
/// while a main of the project runs; then a submenu switching the kind and forgetting the
/// project. The manager is asked for the state when the menu opens; every entry is one
/// message to it.
public final class PopupItems extends CompoundContributionItem{
  @Override protected IContributionItem[] getContributionItems(){
    var link= ManagerLink.find();
    var resource= selectedResource();
    if (link.isEmpty() || resource == null){ return new IContributionItem[0]; }
    var alias= resource.getProject().getName();
    var folder= link.get().projects().get(alias);
    if (folder == null){ return new IContributionItem[0]; }
    var state= link.get().state(alias, folder);
    if (state.isEmpty()){ return new IContributionItem[]{disabled("The Fearless manager is not answering")}; }
    var res= new ArrayList<IContributionItem>();
    if (!state.get().running().isEmpty()){ res.add(action("Terminate Fearless "+state.get().running(), ()->link.get().send("terminate", folder))); }
    else if (state.get().kind().equals("code")){ res.addAll(runItems(link.get(), resource, alias, folder, state.get())); }
    var project= new MenuManager("Fearless project "+alias);
    kinds(state.get().kind()).forEach((text,kind)->project.add(action(text, ()->link.get().send("kind", folder, kind))));
    project.add(new Separator());
    project.add(action("Forget project", ()->link.get().send("forget", folder)));
    res.add(project);
    return res.toArray(IContributionItem[]::new);
  }
  private static List<IContributionItem> runItems(ManagerLink link, IResource resource, String alias, Path folder, ManagerLink.State state){
    if (state.needsCompiling()){ return List.of(action("Compile Fearless project "+alias, ()->link.send("run", folder))); }
    var inFile= resource instanceof IFile f ? state.mains().entrySet().stream().filter(e->e.getValue().equals(fileOf(f))).map(Map.Entry::getKey).toList() : List.<String>of();
    var offered= inFile.isEmpty() ? List.copyOf(state.mains().keySet()) : inFile;
    if (offered.size() == 1){ return List.of(action("Run Fearless "+offered.getFirst(), ()->link.send("run", folder, offered.getFirst()))); }
    var menu= new MenuManager("Run Fearless");
    offered.forEach(m->menu.add(action(m, ()->link.send("run", folder, m))));
    return List.of(menu);
  }
  private static Map<String,String> kinds(String current){
    var res= new LinkedHashMap<String,String>();
    if (!current.equals("idle")){ res.put("Back to idle", "idle"); return res; }
    res.put("Become data", "data:readOnly");
    res.put("Become editable data", "data:readWrite");
    res.put("Become code", "code");
    return res;
  }
  private static String fileOf(IFile f){ return f.getProjectRelativePath().removeFirstSegments(1).toString(); }
  private static IContributionItem action(String text, Runnable run){
    return new ActionContributionItem(new Action(text){ @Override public void run(){ run.run(); } });
  }
  private static IContributionItem disabled(String text){
    var res= new Action(text){};
    res.setEnabled(false);
    return new ActionContributionItem(res);
  }
  /// The structured selection of the active part, even when empty; only a part with no
  /// structured selection (an editor) falls back to the file it edits.
  private static IResource selectedResource(){
    var window= PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    if (window == null || window.getActivePage() == null){ return null; }
    var selection= window.getSelectionService().getSelection();
    if (selection instanceof IStructuredSelection s){ return Adapters.adapt(s.getFirstElement(), IResource.class); }
    var editor= window.getActivePage().getActiveEditor();
    return editor == null ? null : Adapters.adapt(editor.getEditorInput(), IResource.class);
  }
}

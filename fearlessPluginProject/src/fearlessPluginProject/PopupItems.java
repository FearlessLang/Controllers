package fearlessPluginProject;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CompoundContributionItem;

/// The Fearless submenu of the right-click menu on a mirrored project or anything in it,
/// holding what Eclipse has no place of its own for: the kind switches the manager's panel offers, the
/// manager's own view of the project, and forgetting it. Every entry
/// is one message to the manager.
public final class PopupItems extends CompoundContributionItem{
  @Override protected IContributionItem[] getContributionItems(){
    var selection= (IStructuredSelection)PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
    var alias= Adapters.adapt(selection.getFirstElement(), IResource.class).getProject().getName();
    var p= ManagerLink.projects().get(alias);
    var res= new ArrayList<IContributionItem>();
    if (p == null){ return new IContributionItem[0]; }
    var kinds= List.of(List.of("Back to idle", "idle"), List.of("Become dataRead", "dataRead"), List.of("Become dataEdit", "dataEdit"), List.of("Become code", "code"));
    kinds.stream().filter(k -> !k.get(1).equals(p.kind())).forEach(k -> res.add(action(k.get(0), () -> ManagerLink.send("Kind", alias, k.get(1)))));
    res.add(new Separator());
    res.add(action("Show in manager", () -> ManagerLink.send("Show", alias)));
    res.add(action("Forget project", () -> ManagerLink.send("Forget", alias)));
    return res.toArray(IContributionItem[]::new);
  }
  private static IContributionItem action(String text, Runnable run){
    return new ActionContributionItem(new Action(text){ @Override public void run(){ run.run(); } });
  }
}

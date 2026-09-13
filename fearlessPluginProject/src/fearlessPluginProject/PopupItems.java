package fearlessPluginProject;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

import fearlessPluginProject.ManagerLink.State;

/// The Fearless submenu of the right-click menu on a mirrored project or anything in it,
/// holding what Eclipse has no place of its own for: the documentation of the project
/// and of the standard library, the kind switches the manager's panel offers, the
/// manager's own view of the project, and forgetting it. Every entry but Documentation
/// is one message to the manager.
public final class PopupItems extends CompoundContributionItem{
  @Override protected IContributionItem[] getContributionItems(){
    var link= ManagerLink.find().orElseThrow();
    var selection= (IStructuredSelection)PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
    var alias= Adapters.adapt(selection.getFirstElement(), IResource.class).getProject().getName();
    var folder= link.projects().get(alias);
    var res= new ArrayList<IContributionItem>();
    res.add(action("Documentation", ()->docs(link, folder)));
    res.add(new Separator());
    kinds(link.state(alias).map(State::kind).orElse("")).forEach((text,kind)->res.add(action(text, ()->link.send("kind", folder, kind))));
    res.add(new Separator());
    res.add(action("Show in manager", ()->link.send("select", folder)));
    res.add(action("Forget project", ()->link.send("forget", folder)));
    return res.toArray(IContributionItem[]::new);
  }
  private static Map<String,String> kinds(String current){
    var res= new LinkedHashMap<String,String>();
    if (!current.equals("idle")){ res.put("Back to idle", "idle"); return res; }
    res.put("Become data", "data:readOnly");
    res.put("Become editable data", "data:readWrite");
    res.put("Become code", "code");
    return res;
  }
  private static IContributionItem action(String text, Runnable run){
    return new ActionContributionItem(new Action(text){ @Override public void run(){ run.run(); } });
  }
  /// The html page of every package of the project, then the one of the standard library,
  /// each in a browser tab of its own that opening again reuses.
  private static void docs(ManagerLink link, Path folder){
    var genJava= folder.resolve(".fearless_out").resolve("gen_java");
    try(var pages= Files.isDirectory(genJava) ? Files.walk(genJava) : Stream.<Path>of()){
      Stream.concat(pages.filter(p->p.toString().endsWith(".html")), Stream.of(link.baseDocs)).forEach(PopupItems::open);
    }
    catch(IOException e){ throw new UncheckedIOException(e); }
  }
  private static void open(Path page){
    var support= PlatformUI.getWorkbench().getBrowserSupport();
    try{ support.createBrowser(IWorkbenchBrowserSupport.AS_EDITOR, page.toString(), page.getFileName().toString(), null).openURL(page.toUri().toURL()); }
    catch(PartInitException|MalformedURLException e){ throw new IllegalStateException(e); }
  }
}

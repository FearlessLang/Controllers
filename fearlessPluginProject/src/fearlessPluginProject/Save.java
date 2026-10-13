package fearlessPluginProject;

import java.time.Duration;
import java.util.stream.Stream;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

public final class Save extends AbstractHandler{
  @Override public Object execute(ExecutionEvent event){
    var editor= HandlerUtil.getActiveEditor(event);
    var project= Adapters.adapt(editor.getEditorInput(), IResource.class).getProject();
    var alias= project.getName();
    if (!ManagerLink.project(alias).kind().equals("code")){ editor.doSave(new NullProgressMonitor()); return null; }
    if (Follow.compilingFor(alias, Duration.ofSeconds(15))){ return null; }
    ManagerLink.send("Terminate", alias);
    Stream.of(editor.getSite().getPage().getDirtyEditors()).filter(e -> project.equals(projectOf(e))).forEach(e -> e.doSave(new NullProgressMonitor()));
    Follow.compile(alias);
    return null;
  }
  private static Object projectOf(IEditorPart editor){
    var resource= Adapters.adapt(editor.getEditorInput(), IResource.class);
    return resource == null ? null : resource.getProject();
  }
}

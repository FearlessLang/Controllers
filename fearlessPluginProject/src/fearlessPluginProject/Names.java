package fearlessPluginProject;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import suggest.Shapes;

final class Names{
  static Optional<String> outside(IPath container){
    var inside= container.segmentCount() > 1 && container.segment(1).equals(FearlessWatcher.srcName);
    return inside ? Optional.empty() : Optional.of("\""+container+"\" is outside the folder of the project: its files and folders go inside \"src\".");
  }
  static Optional<String> inside(IPath container, String name){
    var root= ResourcesPlugin.getWorkspace().getRoot();
    if (outside(container).isPresent() || !Nature.marks(root.getProject(container.segment(0)))){ return Optional.empty(); }
    var rel= container.removeFirstSegments(2).toString();
    return Optional.of(rel.isEmpty() ? name : rel+"/"+name);
  }
  static Optional<String> problem(IPath container, String name, boolean file, IResource except){
    var path= inside(container, name);
    if (path.isEmpty()){ return Optional.empty(); }
    var at= ResourcesPlugin.getWorkspace().getRoot().findMember(container);
    try{
      var siblings= at instanceof IContainer c ? Stream.of(c.members(IContainer.INCLUDE_HIDDEN)).filter(m -> !m.equals(except)).map(IResource::getName).toList() : List.<String>of();
      return Shapes.problem(path.get(), file, siblings);
    }
    catch (CoreException e){ throw new IllegalStateException(e); }
  }
}

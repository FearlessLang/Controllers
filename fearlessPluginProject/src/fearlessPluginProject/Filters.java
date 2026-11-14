package fearlessPluginProject;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import suggest.Shapes;

/// The manager's projects are seen only in the Fearless explorer: Hide takes them out of
/// the other explorers, Only keeps nothing else in the Fearless one.
public final class Filters{
  private static boolean project(Object element){ return Adapters.adapt(element, IResource.class) instanceof IProject; }
  private static boolean mirrored(Object element){ return Adapters.adapt(element, IResource.class) instanceof IProject p && Nature.marks(p); }
  private static boolean marker(Object element){
    return Adapters.adapt(element, IResource.class) instanceof IFile f && Shapes.isMarker(f.getName()) && f.getParent().getProjectRelativePath().toString().equals(FearlessWatcher.srcName) && Nature.marks(f.getProject());
  }
  public static final class Hide extends ViewerFilter{
    @Override public boolean select(Viewer viewer, Object parent, Object element){ return !mirrored(element); }
  }
  public static final class Only extends ViewerFilter{
    @Override public boolean select(Viewer viewer, Object parent, Object element){ return (!project(element) || mirrored(element)) && !marker(element); }
  }
}

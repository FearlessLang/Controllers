package fearlessPluginProject;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

/// The manager's projects are seen only in the Fearless explorer: Hide takes them out of
/// the other explorers, Only keeps nothing else in the Fearless one.
public final class Filters{
  private Filters(){}
  private static boolean project(Object element){ return Adapters.adapt(element, IResource.class) instanceof IProject; }
  private static boolean mirrored(Object element){ return Adapters.adapt(element, IResource.class) instanceof IProject p && Nature.marks(p); }
  public static final class Hide extends ViewerFilter{
    @Override public boolean select(Viewer viewer, Object parent, Object element){ return !mirrored(element); }
  }
  public static final class Only extends ViewerFilter{
    @Override public boolean select(Viewer viewer, Object parent, Object element){ return !project(element) || mirrored(element); }
  }
}

package fearlessPluginProject;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

/// The nature marking a mirrored project: what the explorer filters, the launch shortcut
/// and the Fearless popup look for. Configuring it installs Builder.
public final class Nature implements IProjectNature{
  public static final String id= "fearlessPluginProject.nature";
  private IProject project;
  @Override public void configure() throws CoreException{
    var description= project.getDescription();
    var command= description.newCommand();
    command.setBuilderName(Builder.id);
    description.setBuildSpec(new ICommand[]{command});
    project.setDescription(description, null);
  }
  @Override public void deconfigure(){}
  @Override public IProject getProject(){ return project; }
  @Override public void setProject(IProject project){ this.project= project; }
  public static boolean marks(IProject project){
    try{ return project.isAccessible() && project.hasNature(id); }
    catch(CoreException e){ throw new IllegalStateException(e); }
  }
}

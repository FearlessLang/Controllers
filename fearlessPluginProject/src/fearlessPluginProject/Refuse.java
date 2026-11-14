package fearlessPluginProject;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.MoveParticipant;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;

import suggest.Shapes;

public final class Refuse{
  public static final class Rename extends RenameParticipant{
    private IResource resource;
    @Override protected boolean initialize(Object element){ resource = (IResource)element; return true; }
    @Override public String getName(){ return "Fearless file names"; }
    @Override public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context){ return status(resource, resource.getParent(), getArguments().getNewName()); }
    @Override public Change createChange(IProgressMonitor pm){ return null; }
  }
  public static final class Move extends MoveParticipant{
    private IResource resource;
    @Override protected boolean initialize(Object element){ resource = (IResource)element; return getArguments().getDestination() instanceof IContainer; }
    @Override public String getName(){ return "Fearless file names"; }
    @Override public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context){ return status(resource, (IContainer)getArguments().getDestination(), resource.getName()); }
    @Override public Change createChange(IProgressMonitor pm){ return null; }
  }
  private static RefactoringStatus status(IResource r, IContainer to, String name){
    var path= Names.inside(to.getFullPath(), name);
    var problem= Names.problem(to.getFullPath(), name, r instanceof IFile, r).or(() -> path.flatMap(p -> below(r, p)));
    return problem.map(RefactoringStatus::createFatalErrorStatus).orElseGet(RefactoringStatus::new);
  }
  private static Optional<String> below(IResource r, String path){
    if (!(r instanceof IContainer c)){ return Optional.empty(); }
    try{ return Stream.of(c.members(IContainer.INCLUDE_HIDDEN)).flatMap(m -> moved(m, path+"/"+m.getName()).stream()).findFirst(); }
    catch (CoreException e){ throw new IllegalStateException(e); }
  }
  private static Optional<String> moved(IResource r, String path){ return Shapes.problem(path, r instanceof IFile, List.of()).or(() -> below(r, path)); }
}

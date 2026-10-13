package fearlessPluginProject;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

public final class NewFile extends BasicNewResourceWizard{
  private WizardNewFileCreationPage page;
  @Override public void addPages(){
    setWindowTitle("New File");
    page = new WizardNewFileCreationPage("file", getSelection()){
      @Override protected boolean validatePage(){
        if (!super.validatePage()){ return false; }
        Names.problem(getContainerFullPath(), getFileName(), true).ifPresent(p->setMessage(p, IMessageProvider.WARNING));
        return true;
      }
    };
    page.setTitle("File");
    page.setDescription("A file of a Fearless project.");
    addPage(page);
  }
  @Override public boolean performFinish(){
    var file= page.createNewFile();
    if (file == null){ return false; }
    selectAndReveal(file);
    try{ IDE.openEditor(getWorkbench().getActiveWorkbenchWindow().getActivePage(), file); }
    catch(PartInitException e){ throw new IllegalStateException(e); }
    return true;
  }
}

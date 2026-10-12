package fearlessPluginProject;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

public final class NewFolder extends BasicNewResourceWizard{
  private Page page;
  @Override public void addPages(){
    setWindowTitle("New Folder");
    var on= Adapters.adapt(getSelection().getFirstElement(), IResource.class);
    page= new Page(on instanceof IContainer c ? c : on.getParent());
    addPage(page);
  }
  @Override public boolean performFinish(){
    var folder= page.parent.getFolder(new Path(page.name.getText()));
    try{ folder.create(false, true, null); }
    catch(CoreException e){ throw new IllegalStateException(e); }
    selectAndReveal(folder);
    return true;
  }
  private static final class Page extends WizardPage{
    final IContainer parent;
    Text name;
    Page(IContainer parent){
      super("folder");
      this.parent= parent;
      setTitle("Folder");
      setDescription("A folder in "+parent.getFullPath()+".");
    }
    @Override public void createControl(Composite parentControl){
      var root= new Composite(parentControl, SWT.NONE);
      root.setLayout(new GridLayout(2, false));
      new Label(root, SWT.NONE).setText("Folder name:");
      name= new Text(root, SWT.BORDER);
      var wide= new GridData(SWT.FILL, SWT.CENTER, true, false);
      wide.widthHint= 480;
      name.setLayoutData(wide);
      name.addModifyListener(e->validate());
      setPageComplete(false);
      setControl(root);
    }
    private void validate(){
      var n= name.getText();
      var error= n.isEmpty() ? "Write the name of the folder." : n.contains("/") ? "A folder name holds no \"/\"." : parent.findMember(n) != null ? "\""+n+"\" already exists." : null;
      setErrorMessage(error);
      setMessage(error != null ? null : Names.problem(parent.getFullPath(), n, false).orElse(null), IMessageProvider.WARNING);
      setPageComplete(error == null);
    }
  }
}

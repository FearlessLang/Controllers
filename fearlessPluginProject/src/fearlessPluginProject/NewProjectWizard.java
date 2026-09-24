package fearlessPluginProject;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

/// File > New > Fearless Project: a folder, made if missing and registered with the
/// manager by a select message; the manager fills an empty folder with a hello world.
public final class NewProjectWizard extends Wizard implements INewWizard{
  private final Page page= new Page();
  @Override public void init(IWorkbench workbench, IStructuredSelection selection){ setWindowTitle("New Fearless Project"); }
  @Override public void addPages(){ addPage(page); }
  @Override public boolean performFinish(){
    var folder= Path.of(page.folder.getText().strip());
    try{ Files.createDirectories(folder); }
    catch(IOException e){ throw new UncheckedIOException(e); }
    ManagerLink.send("select", folder);
    return true;
  }
  private static final class Page extends WizardPage{
    private Text folder;
    Page(){
      super("folder");
      setTitle("Fearless Project");
      setDescription("The folder of the project: an empty or missing one gets a hello world, one holding Fearless code is registered as it is.");
    }
    @Override public void createControl(Composite parent){
      var root= new Composite(parent, SWT.NONE);
      root.setLayout(new GridLayout(3, false));
      new Label(root, SWT.NONE).setText("Folder:");
      folder= new Text(root, SWT.BORDER);
      folder.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
      folder.addModifyListener(e->setPageComplete(!folder.getText().isBlank()));
      var browse= new Button(root, SWT.PUSH);
      browse.setText("Browse...");
      browse.addListener(SWT.Selection, e->browse());
      setPageComplete(false);
      setControl(root);
    }
    private void browse(){
      var chosen= new DirectoryDialog(getShell()).open();
      if (chosen != null){ folder.setText(chosen); }
    }
  }
}

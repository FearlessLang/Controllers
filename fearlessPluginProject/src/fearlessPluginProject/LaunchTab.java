package fearlessPluginProject;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/// The Run Configurations page of a Fearless configuration: the project folder and the
/// main, or no main for the mains selected in the manager.
public final class LaunchTab extends AbstractLaunchConfigurationTabGroup{
  @Override public void createTabs(ILaunchConfigurationDialog dialog, String mode){ setTabs(new ILaunchConfigurationTab[]{new Main(), new CommonTab()}); }
  private static final class Main extends AbstractLaunchConfigurationTab{
    private Text folder;
    private Text main;
    @Override public void createControl(Composite parent){
      var root= new Composite(parent, SWT.NONE);
      root.setLayout(new GridLayout(2, false));
      folder= field(root, "Project folder:");
      main= field(root, "Main (none: the mains selected in the manager):");
      setControl(root);
    }
    private Text field(Composite root, String label){
      new Label(root, SWT.NONE).setText(label);
      var res= new Text(root, SWT.BORDER);
      res.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
      res.addModifyListener(e->updateLaunchConfigurationDialog());
      return res;
    }
    @Override public void setDefaults(ILaunchConfigurationWorkingCopy configuration){}
    @Override public void initializeFrom(ILaunchConfiguration configuration){
      try{
        folder.setText(configuration.getAttribute(Launcher.folderAttr, ""));
        main.setText(configuration.getAttribute(Launcher.mainAttr, ""));
      }
      catch(CoreException e){ throw new IllegalStateException(e); }
    }
    @Override public void performApply(ILaunchConfigurationWorkingCopy configuration){
      configuration.setAttribute(Launcher.folderAttr, folder.getText());
      configuration.setAttribute(Launcher.mainAttr, main.getText());
    }
    @Override public String getName(){ return "Fearless"; }
  }
}

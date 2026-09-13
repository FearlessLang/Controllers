package fearlessPluginProject;

import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.console.IConsoleConstants;

/// Registered on org.eclipse.ui.perspectives: the Fearless explorer (the manager's
/// projects and nothing else) on the left, the Problems, Console and JUnit views below
/// the editors, the Run button and menu in the toolbar (Terminate joins it, see
/// plugin.xml), and the Fearless project wizard under File > New.
public final class FearlessPerspective implements IPerspectiveFactory{
  public static final String explorer= "fearlessPluginProject.explorer";
  private static final String junitView= "org.eclipse.jdt.junit.ResultView";
  @Override public void createInitialLayout(IPageLayout layout){
    layout.addActionSet(IDebugUIConstants.LAUNCH_ACTION_SET);
    var editorArea= layout.getEditorArea();
    IFolderLayout left= layout.createFolder("fearlessPluginProject.left", IPageLayout.LEFT, 0.25f, editorArea);
    left.addView(explorer);
    IFolderLayout bottom= layout.createFolder("fearlessPluginProject.bottom", IPageLayout.BOTTOM, 0.7f, editorArea);
    bottom.addView(IPageLayout.ID_PROBLEM_VIEW);
    bottom.addView(IConsoleConstants.ID_CONSOLE_VIEW);
    bottom.addView(junitView);
    layout.addShowViewShortcut(explorer);
    layout.addShowViewShortcut(IPageLayout.ID_PROBLEM_VIEW);
    layout.addShowViewShortcut(IConsoleConstants.ID_CONSOLE_VIEW);
    layout.addShowViewShortcut(junitView);
    layout.addNewWizardShortcut("fearlessPluginProject.newProject");
  }
}

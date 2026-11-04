package fearlessPluginProject;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PerspectiveAdapter;
import org.eclipse.ui.PlatformUI;

/// Registered on org.eclipse.ui.perspectives: the Fearless explorer (the manager's
/// projects and nothing else) on the left, the Problems, Output and JUnit views below
/// the editors, the main button in the toolbar and none of the toolbar buttons of this
/// Eclipse that do nothing for Fearless or repeat another (see plugin.xml), and the
/// Fearless project wizard under File > New.
public final class FearlessPerspective implements IPerspectiveFactory{
  public static final String id= "fearlessPluginProject.perspective";
  public static final String explorer= "fearlessPluginProject.explorer";
  private static final String junitView= "org.eclipse.jdt.junit.ResultView";
  private static final List<String> hidden= List.of("org.eclipse.debug.ui.launchActionSet", "org.eclipse.ui.externaltools.ExternalToolsSet");
  static void hideRunAndDebug(){
    var workbench= PlatformUI.getWorkbench();
    workbench.getDisplay().asyncExec(() -> {
      Stream.of(workbench.getWorkbenchWindows()).forEach(FearlessPerspective::watch);
      workbench.addWindowListener(new IWindowListener(){
        @Override public void windowOpened(IWorkbenchWindow w){ watch(w); }
        @Override public void windowActivated(IWorkbenchWindow w){}
        @Override public void windowDeactivated(IWorkbenchWindow w){}
        @Override public void windowClosed(IWorkbenchWindow w){}
      });
    });
  }
  private static void watch(IWorkbenchWindow w){
    w.addPerspectiveListener(new PerspectiveAdapter(){
      @Override public void perspectiveActivated(IWorkbenchPage page, IPerspectiveDescriptor p){ hide(page, p); }
    });
    Optional.ofNullable(w.getActivePage()).ifPresent(page -> hide(page, page.getPerspective()));
  }
  private static void hide(IWorkbenchPage page, IPerspectiveDescriptor p){
    if (p != null && p.getId().equals(id)){ hidden.forEach(page::hideActionSet); }
  }
  @Override public void createInitialLayout(IPageLayout layout){
    var editorArea= layout.getEditorArea();
    IFolderLayout left= layout.createFolder("fearlessPluginProject.left", IPageLayout.LEFT, 0.25f, editorArea);
    left.addView(explorer);
    IFolderLayout bottom= layout.createFolder("fearlessPluginProject.bottom", IPageLayout.BOTTOM, 0.7f, editorArea);
    bottom.addView(IPageLayout.ID_PROBLEM_VIEW);
    bottom.addView(Output.id);
    bottom.addView(junitView);
    layout.addShowViewShortcut(explorer);
    layout.addShowViewShortcut(IPageLayout.ID_PROBLEM_VIEW);
    layout.addShowViewShortcut(Output.id);
    layout.addShowViewShortcut(junitView);
    layout.addNewWizardShortcut("fearlessPluginProject.newProject");
    layout.addNewWizardShortcut("fearlessPluginProject.newFile");
  }
}

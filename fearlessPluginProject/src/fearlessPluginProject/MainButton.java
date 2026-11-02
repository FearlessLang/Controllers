package fearlessPluginProject;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.jface.resource.ResourceLocator;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISources;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.dialogs.ListSelectionDialog;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;

import suggest.Cached;

public final class MainButton extends AbstractHandler implements IElementUpdater{
  static final String id= "fearlessPluginProject.main";
  enum Action{ check, compile, run }
  record State(Action action, List<String> mains, List<String> selected){}
  private static volatile Map<String,State> found= Map.of();
  private final Set<IWorkbenchWindow> watched= new HashSet<>();
  static void look(Map<String,ManagerLink.Project> projects){
    var next= projects.keySet().stream().collect(Collectors.toMap(a -> a, a -> of(a, projects)));
    if (next.equals(found)){ return; }
    found = Map.copyOf(next);
    refresh();
  }
  static void refresh(){
    Display.getDefault().asyncExec(() -> {
      var workbench= PlatformUI.getWorkbench();
      workbench.getService(ICommandService.class).refreshElements(id, null);
      workbench.getService(IEventBroker.class).post(UIEvents.REQUEST_ENABLEMENT_UPDATE_TOPIC, UIEvents.ALL_ELEMENT_ID);
    });
  }
  static State of(String alias, Map<String,ManagerLink.Project> projects){
    var p= projects.get(alias);
    var action= action(alias, p);
    if (action != Action.run){ return new State(action, List.of(), List.of()); }
    var all= ManagerLink.mains(p.folder());
    return new State(action, all, all.stream().filter(p.mains()::contains).toList());
  }
  @Override public Object execute(ExecutionEvent event){
    var alias= target(HandlerUtil.getActiveWorkbenchWindow(event)).orElseThrow();
    if (Follow.followed().filter(f -> !f.compile && f.alias.equals(alias)).isPresent()){ ManagerLink.send("Terminate", alias); return null; }
    var s= of(alias, ManagerLink.projects());
    switch (s.action()){
      case check -> ManagerLink.send("Check", alias);
      case compile -> Follow.compile(alias);
      case run -> (s.mains().size() == 1 ? Optional.of(s.mains()) : chosen(s, HandlerUtil.getActiveShell(event))).ifPresent(ms -> Follow.run(alias, ms));
    }
    return null;
  }
  /// The mains checked in the dialog; none when the dialog is cancelled.
  private static Optional<List<String>> chosen(State s, Shell shell){
    var dialog= ListSelectionDialog.of(s.mains()).contentProvider(ArrayContentProvider.getInstance()).labelProvider(new LabelProvider()).title("Fearless").message("Mains to run:").create(shell);
    dialog.setInitialElementSelections(s.selected());
    if (dialog.open() != Window.OK){ return Optional.empty(); }
    return Optional.of(Stream.of(dialog.getResult()).map(m -> (String)m).toList());
  }
  @Override public void setEnabled(Object context){
    var window= ((IEvaluationContext)context).getVariable(ISources.ACTIVE_WORKBENCH_WINDOW_NAME);
    setBaseEnabled(window instanceof IWorkbenchWindow w && target(w).filter(MainButton::pressable).isPresent());
  }
  private static boolean pressable(String alias){
    var followed= Follow.followed();
    var s= found.get(alias);
    if (followed.isPresent() && followed.get().alias.equals(alias)){ return !followed.get().compile; }
    if (followed.isPresent()){ return s.action() == Action.check; }
    return s.action() != Action.run || !s.mains().isEmpty();
  }
  @Override public void updateElement(UIElement element, @SuppressWarnings("rawtypes") Map parameters){
    var window= element.getServiceLocator().getService(IWorkbenchWindow.class);
    if (watched.add(window)){ window.getSelectionService().addPostSelectionListener((part, selection) -> refresh()); }
    var alias= target(window);
    var terminate= alias.isPresent() && Follow.followed().filter(f -> !f.compile && f.alias.equals(alias.get())).isPresent();
    var s= alias.map(found::get).orElse(new State(Action.run, List.of(), List.of()));
    var icon= terminate ? List.of("org.eclipse.debug.ui", "icons/full/elcl16/terminate_co.png") : switch (s.action()){
      case check -> List.of("org.eclipse.ui.ide", "icons/full/etool16/problem_category.png");
      case compile -> List.of("org.eclipse.ui.ide", "icons/full/etool16/build_exec.png");
      case run -> List.of("org.eclipse.debug.ui", s.mains().size() > 1 ? "icons/full/etool16/run_exc.png" : "icons/full/obj16/lrun_obj.png");
    };
    element.setIcon(ResourceLocator.imageDescriptorFromBundle(icon.get(0), icon.get(1)).orElseThrow());
    var verb= terminate ? "Terminate" : switch (s.action()){ case check -> "Check"; case compile -> "Compile"; case run -> s.mains().size() > 1 ? "Run V" : "Run"; };
    element.setTooltip(alias.map(a -> verb+" "+a).orElse(verb));
  }
  private static Optional<String> target(IWorkbenchWindow window){
    var page= Optional.ofNullable(window.getActivePage());
    var part= page.map(p -> p.getActivePart());
    Object on= part.filter(p -> p instanceof IEditorPart).map(p -> (Object)((IEditorPart)p).getEditorInput())
      .orElse(page.map(p -> p.getSelection()).filter(s -> s instanceof IStructuredSelection).map(s -> ((IStructuredSelection)s).getFirstElement()).orElse(null));
    return Optional.ofNullable(Adapters.adapt(on, IResource.class)).map(r -> r.getProject().getName()).filter(found::containsKey);
  }
  private static Action action(String alias, ManagerLink.Project p){
    if (!p.kind().equals("code") || !marked(p.folder(), alias)){ return Action.check; }
    return switch (Cached.state(p.folder(), ManagerLink.baseCache)){ case invalid -> Action.check; case outOfDate -> Action.compile; case compiled -> Action.run; };
  }
  private static boolean marked(Path folder, String alias){
    if (!Files.isDirectory(folder)){ return false; }
    try (var s= Files.list(folder)){
      return s.filter(f -> f.getFileName().toString().endsWith(".fearless") && Files.isRegularFile(f)).map(f -> f.getFileName().toString()).toList().equals(List.of(alias+".fearless"));
    }
    catch (IOException e){ throw new UncheckedIOException(e); }
  }
}

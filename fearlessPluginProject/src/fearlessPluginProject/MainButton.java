package fearlessPluginProject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipInputStream;

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
    var action= action(alias, p, projects);
    if (action != Action.run){ return new State(action, List.of(), List.of()); }
    var all= List.copyOf(ManagerLink.mains(p.folder()).keySet());
    return new State(action, all, all.stream().filter(p.mains()::contains).toList());
  }
  @Override public Object execute(ExecutionEvent event){
    var alias= target(HandlerUtil.getActiveWorkbenchWindow(event)).orElseThrow();
    if (Follow.followed().isPresent()){ ManagerLink.send("Terminate", alias); return null; }
    var s= of(alias, ManagerLink.projects());
    switch (s.action()){
      case check -> ManagerLink.send("Check", alias);
      case compile -> Follow.compile(alias);
      case run -> run(alias, s.mains().size() == 1 ? s.mains() : chosen(s, HandlerUtil.getActiveShell(event)));
    }
    return null;
  }
  private static void run(String alias, List<String> mains){ if (!mains.isEmpty()){ Follow.run(alias, mains.toArray(String[]::new)); } }
  private static List<String> chosen(State s, Shell shell){
    var dialog= ListSelectionDialog.of(s.mains()).contentProvider(ArrayContentProvider.getInstance()).labelProvider(new LabelProvider()).title("Fearless").message("Mains to run:").create(shell);
    dialog.setInitialElementSelections(s.selected());
    if (dialog.open() != Window.OK){ return List.of(); }
    return Stream.of(dialog.getResult()).map(m -> (String)m).toList();
  }
  @Override public void setEnabled(Object context){
    var window= ((IEvaluationContext)context).getVariable(ISources.ACTIVE_WORKBENCH_WINDOW_NAME);
    setBaseEnabled(window instanceof IWorkbenchWindow w && target(w).filter(MainButton::pressable).isPresent());
  }
  private static boolean pressable(String alias){
    var followed= Follow.followed();
    if (followed.isPresent()){ return followed.get().alias.equals(alias); }
    var s= found.get(alias);
    return s.action() != Action.run || !s.mains().isEmpty();
  }
  @Override public void updateElement(UIElement element, @SuppressWarnings("rawtypes") Map parameters){
    var window= element.getServiceLocator().getService(IWorkbenchWindow.class);
    if (watched.add(window)){ window.getSelectionService().addPostSelectionListener((part, selection) -> refresh()); }
    var alias= target(window);
    var terminate= alias.isPresent() && Follow.followed().filter(f -> f.alias.equals(alias.get())).isPresent();
    var s= alias.map(found::get).orElse(new State(Action.run, List.of(), List.of()));
    var icon= terminate ? List.of("org.eclipse.debug.ui", "icons/full/elcl16/terminate_co.png") : switch (s.action()){
      case check -> List.of("org.eclipse.ui.ide", "icons/full/etool16/problem_category.png");
      case compile -> List.of("org.eclipse.ui.ide", "icons/full/etool16/build_exec.png");
      case run -> List.of("org.eclipse.debug.ui", "icons/full/obj16/lrun_obj.png");
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
  private static Action action(String alias, ManagerLink.Project p, Map<String,ManagerLink.Project> projects){
    var linked= p.reads().stream().allMatch(a -> projects.containsKey(a) && projects.get(a).kind().startsWith("data:"))
      && p.edits().stream().allMatch(a -> projects.containsKey(a) && projects.get(a).kind().equals("data:readWrite"));
    if (!p.kind().equals("code") || !linked || !marked(p.folder(), alias)){ return Action.check; }
    try{ return outOfDate(p.folder()) ? Action.compile : Action.run; }
    catch (Invalid e){ return Action.check; }
  }
  private static boolean marked(Path folder, String alias){
    if (!Files.isDirectory(folder)){ return false; }
    try (var s= Files.list(folder)){
      return s.filter(f -> f.getFileName().toString().endsWith(".fearless") && Files.isRegularFile(f)).map(f -> f.getFileName().toString()).toList().equals(List.of(alias+".fearless"));
    }
    catch (IOException e){ throw new UncheckedIOException(e); }
  }
  private static final class Invalid extends RuntimeException{ private static final long serialVersionUID= 1L; }
  private record Ref(List<String> segments, long stamp){
    String fearPath(){ return "fear:/"+String.join("/", segments); }
    String name(){ return segments.getLast(); }
  }
  private static final List<String> ranks= List.of(
    "_rank_base", "_rank_core", "_rank_driver", "_rank_worker", "_rank_framework", "_rank_accumulator", "_rank_tool", "_rank_app");
  private static boolean outOfDate(Path folder){
    var refs= refs(folder);
    if (refs.stream().noneMatch(r -> r.name().endsWith(".fear"))){ throw new Invalid(); }
    var pkgs= new LinkedHashMap<String,List<Ref>>();
    for (var r : refs){
      var pkg= pkg(r);
      if (pkg.isEmpty() && r.name().endsWith(".fear")){ throw new Invalid(); }
      pkg.ifPresent(n -> pkgs.computeIfAbsent(n, k -> new ArrayList<>()).add(r));
    }
    var rank= new HashMap<String,Integer>();
    pkgs.forEach((pkg, rs) -> rank.put(pkg, rank(rs)));
    var out= ManagerLink.out(folder);
    if (!Files.isDirectory(out)){ return true; }
    var map= stamp(out.resolve("_map.json"));
    return pkgs.keySet().stream().anyMatch(pkg -> !built(out, pkg, pkgs.get(pkg), rank, map));
  }
  private static boolean built(Path out, String pkg, List<Ref> files, Map<String,Integer> rank, long map){
    var lower= rank.entrySet().stream().filter(e -> e.getValue() < rank.get(pkg)).map(Map.Entry::getKey).toList();
    var below= lower.stream().mapToLong(n -> stamp(out.resolve(n+".json")));
    var own= files.stream().mapToLong(Ref::stamp);
    var maxIn= LongStream.concat(LongStream.concat(own, below), LongStream.of(map)).max().getAsLong();
    var built= out.resolve(pkg+".built");
    var list= files.stream().map(Ref::fearPath).sorted().collect(Collectors.joining("\n"))
      +Stream.concat(lower.stream(), Stream.of("base")).sorted().map(n -> "\n"+n).collect(Collectors.joining("", "\n---", ""));
    return stamp(built) >= maxIn && ManagerLink.read(built).equals(list) && Files.exists(out.resolve(pkg+"_manifest.info"));
  }
  private static Optional<String> pkg(Ref r){
    var candidates= r.segments().stream().filter(s -> s.startsWith("_") && !s.contains(".")).toList();
    if (candidates.size() > 1){ throw new Invalid(); }
    return candidates.stream().findAny().map(s -> s.substring(1));
  }
  private static int rank(List<Ref> files){
    var reserved= files.stream().filter(r -> r.name().startsWith("_rank_")).toList();
    if (reserved.size() != 1){ throw new Invalid(); }
    var name= reserved.getFirst().name();
    if (!name.endsWith(".fear")){ throw new Invalid(); }
    var stem= name.substring(0, name.indexOf('.'));
    for (int i= 0; i < ranks.size(); i += 1){
      var pref= ranks.get(i);
      if (stem.equals(pref)){ return (i+1)*1000+999; }
      if (!stem.startsWith(pref)){ continue; }
      var digits= stem.substring(pref.length());
      if (!digits.matches("[0-9]{3}")){ throw new Invalid(); }
      return (i+1)*1000+Integer.parseInt(digits);
    }
    throw new Invalid();
  }
  private static List<Ref> refs(Path folder){
    var res= new ArrayList<Ref>();
    try (var all= Files.walk(folder)){
      for (var p : all.filter(p -> !Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS)).toList()){
        var segments= StreamSupport.stream(folder.relativize(p).spliterator(), false).map(Path::toString).toList();
        if (invisible(segments) || generatedTests(p)){ continue; }
        if (!Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS)){ throw new Invalid(); }
        dated(stamp(p));
        if (!segments.getLast().endsWith(".zip")){ res.add(new Ref(segments, stamp(p))); continue; }
        try (var in= Files.newInputStream(p)){ zip(unzipped(segments), in, stamp(p), res); }
      }
    }
    catch (IOException|UncheckedIOException e){ throw new Invalid(); }
    res.removeIf(r -> invisible(r.segments()));
    res.sort(Comparator.comparing(Ref::fearPath));
    return res;
  }
  private static boolean generatedTests(Path p){
    var shape= p.getParent().getFileName().toString().equals("auto_tests") && p.getFileName().toString().endsWith("_test.fear");
    return shape && Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS) && ManagerLink.read(p).startsWith("//Autogenerated from the doc examples of package ");
  }
  private static void zip(List<String> at, InputStream in, long stamp, ArrayList<Ref> res) throws IOException{
    var zip= new ZipInputStream(in, StandardCharsets.UTF_8);
    for (var e= zip.getNextEntry(); e != null; e = zip.getNextEntry()){
      if (e.isDirectory()){ continue; }
      var segments= Stream.concat(at.stream(), Stream.of(e.getName().split("/"))).toList();
      if (!invisible(segments)){ dated(e.getTime()); }
      if (e.getName().endsWith(".zip")){ zip(unzipped(segments), new ByteArrayInputStream(zip.readAllBytes()), stamp, res); continue; }
      res.add(new Ref(segments, stamp));
    }
  }
  private static void dated(long t){ if (t <= 0 || t > System.currentTimeMillis()+5_000){ throw new Invalid(); } }
  private static List<String> unzipped(List<String> segments){
    var last= segments.getLast();
    return Stream.concat(segments.stream().limit(segments.size()-1), Stream.of(last.substring(0, last.length()-4))).toList();
  }
  private static boolean invisible(List<String> segments){ return segments.stream().anyMatch(s -> s.startsWith(".")); }
  private static long stamp(Path file){
    try{ return Files.getLastModifiedTime(file, LinkOption.NOFOLLOW_LINKS).toMillis(); }
    catch (NoSuchFileException e){ return -1; }
    catch (IOException e){ throw new UncheckedIOException(e); }
  }
}

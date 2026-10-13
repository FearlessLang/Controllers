package fearlessPluginProject;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.ide.IDE;

public final class NewSource extends AbstractHandler{
  @Override public Object execute(ExecutionEvent event){
    var window= HandlerUtil.getActiveWorkbenchWindow(event);
    var part= window.getActivePage().getActivePart();
    Object on= part instanceof IEditorPart e ? e.getEditorInput() : window.getSelectionService().getSelection() instanceof IStructuredSelection s ? s.getFirstElement() : null;
    var resource= Optional.ofNullable(Adapters.adapt(on, IResource.class)).filter(r -> Nature.marks(r.getProject()));
    var project= resource.map(IResource::getProject).orElseThrow();
    var type= "type".equals(event.getParameter("fearlessPluginProject.what"));
    new Dialog(window.getShell(), window.getActivePage(), project, type, resource.map(NewSource::start).orElse("")).open();
    return null;
  }
  private static String start(IResource r){
    var folder= r instanceof IContainer c ? c : r.getParent();
    var rel= folder.getProjectRelativePath();
    return rel.segmentCount() == 0 || !rel.segment(0).equals(FearlessWatcher.srcName) || rel.segmentCount() == 1 ? "" : rel.removeFirstSegments(1)+"/";
  }
  private static final List<String> ranks= List.of("base","core","driver","worker","framework","accumulator","tool","app");
  private static final String uses= Stream.of("Main","UnitTests","Test","Void","Bool","Nat","Int","Float","Str","List","Lists","Opt","Opts","Block","Error","Debug")
    .map(t -> "use base."+t+" as "+t+";\n").collect(Collectors.joining());
  private static String tests(String name, String base){ return """
    %s: %sUnitTests{::
      .test Addition: %sTest{:: .check{(2 + 3).assertEq 5}}
      .test Multiplication: %sTest{:: .check{(4 * 5).assertEq 20}}
      .test LeftToRight: %sTest{:: .check{(2 + 3 * 4).assertEq 20}}
      }
    """.formatted(name, base, base, base, base); }
  private static final class Dialog extends TitleAreaDialog{
    private final IWorkbenchPage page;
    private final IProject project;
    private final boolean type;
    private final String start;
    private Text path;
    private Button rank;
    private Combo rankName;
    private Button example;
    private Label creates;
    Dialog(Shell shell, IWorkbenchPage page, IProject project, boolean type, String start){
      super(shell);
      this.page= page;
      this.project= project;
      this.type= type;
      this.start= start;
    }
    @Override protected Control createDialogArea(Composite parent){
      var area= (Composite)super.createDialogArea(parent);
      setTitle(type ? "New Type" : "New Package");
      var root= new Composite(area, SWT.NONE);
      root.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
      root.setLayout(new GridLayout(2, false));
      new Label(root, SWT.NONE).setText("Path in "+project.getName()+":");
      path = new Text(root, SWT.BORDER);
      path.setText(start);
      var wide= new GridData(SWT.FILL, SWT.CENTER, true, false);
      wide.widthHint = 480;
      path.setLayoutData(wide);
      path.addModifyListener(e -> validate());
      if (type){
        rank = check(root, "Rank file: the file of the package holding its use lines and its mains", 1);
        rankName = new Combo(root, SWT.READ_ONLY);
        ranks.forEach(rankName::add);
        rankName.select(ranks.indexOf("app"));
        rankName.addListener(SWT.Selection, e -> validate());
        example = check(root, "Example tests: a test suite checking some arithmetic, to run instead of a bare main", 2);
      }
      creates = new Label(root, SWT.WRAP);
      creates.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
      return area;
    }
    private Button check(Composite root, String text, int span){
      var b= new Button(root, SWT.CHECK);
      b.setText(text);
      b.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, span, 1));
      b.addListener(SWT.Selection, e -> validate());
      return b;
    }
    @Override protected void createButtonsForButtonBar(Composite parent){
      super.createButtonsForButtonBar(parent);
      validate();
    }
    private String target(){
      var p= path.getText().strip();
      if (!type){ return p.endsWith("/") ? p.substring(0, p.length()-1) : p; }
      var folder= p.contains("/") ? p.substring(0, p.lastIndexOf('/')+1) : "";
      if (rank.getSelection()){ return folder+"_rank_"+rankName.getText()+".fear"; }
      var name= p.substring(folder.length());
      return name.isEmpty() || name.contains(".") ? p : p+".fear";
    }
    private Optional<String> problem(String t){
      if (path.getText().isBlank()){ return Optional.of(type ? "Write the path of the type, like \"_pkg/point\"." : "Write the path of the package, like \"_pkg\" or \"lib/_pkg\"."); }
      var p= Names.problem(t, type);
      if (p.isPresent()){ return p; }
      var last= t.substring(t.lastIndexOf('/')+1);
      if (!type && !last.startsWith("_")){ return Optional.of("\""+last+"\" is not a package folder: the name of a package folder starts with \"_\", like \"_"+last+"\"."); }
      if (type && !last.endsWith(".fear")){ return Optional.of("\""+last+"\" is not a Fearless file: a type goes in a .fear file."); }
      if (src().findMember(t) != null){ return Optional.of("\""+t+"\" already exists."); }
      if (type && rank.getSelection()){ return otherRank(t); }
      return Optional.empty();
    }
    private Optional<String> otherRank(String t){
      var folder= src().findMember(t.substring(0, t.lastIndexOf('/')+1));
      if (!(folder instanceof IContainer c)){ return Optional.empty(); }
      try{ return Stream.of(c.members()).map(IResource::getName).filter(n -> n.startsWith("_rank_")).findFirst().map(n -> "The package already has the rank file \""+n+"\": a package has exactly one."); }
      catch (CoreException e){ throw new IllegalStateException(e); }
    }
    private IFolder src(){ return project.getFolder(FearlessWatcher.srcName); }
    private void validate(){
      if (type){ rankName.setEnabled(rank.getSelection()); }
      var t= target();
      var problem= problem(t);
      setErrorMessage(problem.orElse(null));
      setMessage(type ? "A .fear file in a package folder. The path is inside the project folder, like \"lib/_pkg/point\"." : "A package folder, made with the folders holding it. The path is inside the project folder, like \"lib/_pkg\".");
      creates.setText(problem.isPresent() ? "" : "Creates "+t);
      getButton(IDialogConstants.OK_ID).setEnabled(problem.isEmpty());
    }
    @Override protected void okPressed(){
      var t= target();
      try{
        var segments= new Path(t).segments();
        IContainer at= src();
        for (int i= 0; i < (type ? segments.length-1 : segments.length); i++){
          var f= at.getFolder(new Path(segments[i]));
          if (!f.exists()){ f.create(true, true, null); }
          at = f;
        }
        if (type){ open(create(at.getFile(new Path(segments[segments.length-1])))); }
      }
      catch (CoreException e){ throw new IllegalStateException(e); }
      super.okPressed();
    }
    private IFile create(IFile file) throws CoreException{
      var stem= file.getName().substring(0, file.getName().length()-".fear".length());
      var isRank= rank.getSelection();
      var name= Stream.of(stem.split("_")).filter(s -> !s.isEmpty()).map(s -> Character.toUpperCase(s.charAt(0))+s.substring(1)).collect(Collectors.joining());
      var body= isRank ? uses : name+": {}\n";
      var test= !example.getSelection() ? "" : "\n"+tests(isRank ? "ArithmeticTests" : name+"Tests", isRank ? "" : "base.");
      file.create(new ByteArrayInputStream((body+test).getBytes(StandardCharsets.UTF_8)), true, null);
      return file;
    }
    private void open(IFile file){
      try{ IDE.openEditor(page, file); }
      catch (PartInitException e){ throw new IllegalStateException(e); }
    }
  }
}

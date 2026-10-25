package fearlessPluginProject;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.PaletteData;
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
/// manager by a Register message. A demo is written into a new or empty folder first,
/// and the project becomes code by a Kind message once the manager lists it.
public final class NewProjectWizard extends Wizard implements INewWizard{
  private final Page page= new Page();
  @Override public void init(IWorkbench workbench, IStructuredSelection selection){ setWindowTitle("New Fearless Project"); }
  @Override public void addPages(){ addPage(page); }
  @Override public boolean performFinish(){
    var demo= page.demo;
    Path folder;
    try{
      folder= Files.createDirectories(page.folder()).toRealPath();
      if (!demo.isEmpty()){ write(folder, demo); }
    }
    catch (IOException e){ throw new UncheckedIOException(e); }
    ManagerLink.send("Register", ManagerLink.pathArg(folder));
    if (!demo.isEmpty()){ becomeCode(folder); }
    return true;
  }
  private static void becomeCode(Path folder){
    Job.create("Fearless project becomes code", (IProgressMonitor monitor) -> {
      for (int i= 0; i < 120; i += 1){
        var alias= ManagerLink.projects().entrySet().stream().filter(e -> e.getValue().folder().equals(folder)).map(Map.Entry::getKey).findFirst();
        if (alias.isPresent()){ ManagerLink.send("Kind", alias.get(), "code"); return Status.OK_STATUS; }
        try{ Thread.sleep(250); }
        catch (InterruptedException e){ Thread.currentThread().interrupt(); return Status.CANCEL_STATUS; }
      }
      return Status.error("The manager did not register "+folder+" within 30 seconds: make it code from the manager.");
    }).schedule();
  }
  private static final List<String> demos= List.of("", "Hello world", "Simple GUI", "Shortcut and OpenWith");
  private static void write(Path folder, String demo) throws IOException{
    switch (demo){
      case "Hello world" -> Files.writeString(Files.createDirectories(folder.resolve("_hello")).resolve("_rank_app.fear"), hello);
      case "Simple GUI" -> Files.writeString(Files.createDirectories(folder.resolve("_gui")).resolve("_rank_app.fear"), gui);
      default -> {
        var icons= Files.createDirectories(folder.resolve("_open").resolve("icons"));
        Files.writeString(folder.resolve("_open").resolve("_rank_app.fear"), open);
        png(icons.resolve("app.png"), true);
        png(icons.resolve("file.png"), false);
      }
    }
  }
  private static void png(Path file, boolean app){
    var data= new ImageData(64, 64, 24, new PaletteData(0xFF0000, 0xFF00, 0xFF));
    for (int y= 0; y < 64; y += 1){ for (int x= 0; x < 64; x += 1){ data.setPixel(x, y, app ? appPixel(x, y) : filePixel(x, y)); } }
    var loader= new ImageLoader();
    loader.data = new ImageData[]{data};
    loader.save(file.toString(), SWT.IMAGE_PNG);
  }
  private static int appPixel(int x, int y){ return x >= 22 && x <= 46 && Math.abs(y-32) <= (46-x)/2 ? 0xFFFFFF : 0x2F6FD0; }
  private static int filePixel(int x, int y){
    var border= x < 3 || x > 60 || y < 3 || y > 60;
    var line= x > 10 && x < 54 && y > 12 && y < 54 && y % 8 == 0;
    return border ? 0xB08A00 : line ? 0x8A8A8A : 0xFFE680;
  }
  private static final String hello= """
use base.Main as Main;

Hello: Main { sys -> sys.out.println("Hello World!") }
""";
  private static final String gui= """
use base.Main as Main;
use base.Consumer as Consumer;
use base.Frame as Frame;
use base.Color as Color;
use base.Debug as Debug;

Window: Main { sys -> sys.gui.name "Hello Fearless" .run HelloFrame }
HelloFrame: Consumer[mut Frame]{::
  .content{::
    .background (Color.ghostWhite)
    .label{:: .text "Hello from a Fearless window" }
    .button{::
      .text "Say hi"
      .background (Color.rubberDuck)
      .action{Debug#"Hi from the button"}
      }
    }
  }
""";
  private static final String open= """
use base.Main as Main;
use base.Shortcut as Shortcut;
use base.OpenWith as OpenWith;
use base.Consumer as Consumer;
use base.Frame as Frame;
use base.Color as Color;
use base.Block as Block;
use base.Void as Void;
use base.Var as Var;
use base.Pane as Pane;
use base.Slot as Slot;
use base.Slots as Slots;
use base.WidthNat as WidthNat;
use base.HeightNat as HeightNat;
use base.InputCursor as InputCursor;
use base.BadStrUnitRecover as BadStrUnitRecover;

ByShortcut: Main, Shortcut[IconsApp] { sys -> sys.gui.name "Shortcut" .run ShortcutFrame }
ByOpenWith: Main, OpenWith[IconsFile] { sys -> sys.gui.name "OpenWith" .icon IconsFile .run{f -> Block#
  .let[mut Slot[mut Pane]] lines= {Slots#[mut Pane]}
  .var[mut InputCursor] at= {sys.inputCursor}
  .return{f
    .title "drop more files here"
    .virtualPixelSpace(WidthNat#600, HeightNat#400)
    .modelFps(10, {:: .action{ShowFiles#(at, lines!)}})
    .content{::
      .background (Color.rubberDuck)
      .vertical .chunk 1000 .heightGap 0
      .save lines
      }
    }
  }}
ShortcutFrame: Consumer[mut Frame]{::
  .content{::
    .background (Color.ghostWhite)
    .label{:: .text "Started from the shortcut file by_shortcut.fapp... in the project folder." }
    }
  }
ShowFiles: {#(at: mut Var[mut InputCursor], lines: mut Pane): Void -> at.get#.match{
  .empty -> Void;
  .some file -> Block#
    .do{(file.readStrUtf8 NoCr).split("" | "").flow.for{l -> Block#(lines.label{:: .text l .topInset 0 .bottomInset 0}, Void)}}
    .do{at.set(at.get.next)}
    .return{this#(at, lines)}
  }}
NoCr: BadStrUnitRecover{reason, byteOffset, byteLength, rejectedValue -> (rejectedValue == 13).if{.then -> ""; .else -> "?"}}
""";
  private static final class Page extends WizardPage{
    private Text folder;
    private String demo= "";
    Page(){
      super("folder");
      setTitle("Fearless Project");
      setDescription("The folder of the project: a missing one is made, and the folder is registered as it is.");
    }
    Path folder(){ return Path.of(folder.getText().strip()).toAbsolutePath(); }
    @Override public void createControl(Composite parent){
      var root= new Composite(parent, SWT.NONE);
      root.setLayout(new GridLayout(3, false));
      new Label(root, SWT.NONE).setText("Folder:");
      folder = new Text(root, SWT.BORDER);
      folder.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
      folder.addModifyListener(e -> validate());
      var browse= new Button(root, SWT.PUSH);
      browse.setText("Browse...");
      browse.addListener(SWT.Selection, e -> browse());
      var label= new Label(root, SWT.NONE);
      label.setText("Content:");
      label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, demos.size()));
      demos.forEach(d -> radio(root, d));
      setPageComplete(false);
      setControl(root);
    }
    private void radio(Composite root, String d){
      var b= new Button(root, SWT.RADIO);
      b.setText(d.isEmpty() ? "The folder as it is" : "Demo: "+d+" (the project becomes code)");
      b.setSelection(d.isEmpty());
      b.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
      b.addListener(SWT.Selection, e -> { if (b.getSelection()){ demo = d; validate(); } });
    }
    private void validate(){
      var problem= problem();
      setErrorMessage(problem.orElse(null));
      setPageComplete(problem.isEmpty());
    }
    private Optional<String> problem(){
      if (folder.getText().isBlank()){ return Optional.of("Choose the folder of the project."); }
      var f= folder();
      if (demo.isEmpty() || !Files.exists(f)){ return Optional.empty(); }
      try (var entries= Files.list(f)){ return entries.findAny().map(e -> "The folder "+f+" is not empty: a demo goes in a new or empty folder."); }
      catch (IOException e){ return Optional.of("The folder "+f+" can not be read: "+e.getMessage()); }
    }
    private void browse(){
      var chosen= new DirectoryDialog(getShell()).open();
      if (chosen != null){ folder.setText(chosen); }
    }
  }
}

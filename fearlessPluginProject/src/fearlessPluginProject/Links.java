package fearlessPluginProject;

import java.util.function.IntSupplier;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.IHyperlink;
import org.eclipse.ui.console.IPatternMatchListener;
import org.eclipse.ui.console.PatternMatchEvent;
import org.eclipse.ui.console.TextConsole;
import org.eclipse.ui.ide.IDE;

/// The links of the console of Output: in a stack frame
/// "error line: N in file F" and in a compile error header "In file: fear:/F", F is a link
/// opening that file at line N. A compile error carries its N on the numbered source line
/// "N| " below the header that the line of ^ under it marks; the console fills in pieces, so that line is looked up when the
/// link is used. F is relative to the project folder, so it is found in the src folder of
/// the mirrored project whose job the console shows.
final class Links implements IPatternMatchListener{
  private static final Pattern at= Pattern.compile("error line: (\\d+) in file (\\S+)|In file: fear:/(\\S+)");
  @Override public String getPattern(){ return at.pattern(); }
  @Override public int getCompilerFlags(){ return 0; }
  @Override public String getLineQualifier(){ return null; }
  private static final Pattern marked= Pattern.compile("(?m)^(\\d+)\\| .*\\n *\\| [ ^~-]*\\^");
  private TextConsole console;
  @Override public void connect(TextConsole console){ this.console = console; }
  @Override public void disconnect(){ console = null; }
  @Override public void matchFound(PatternMatchEvent event){
    try{ link(event.getOffset(), console.getDocument().get(event.getOffset(), event.getLength())); }
    catch(BadLocationException e){ throw new IllegalStateException(e); }
  }
  private void link(int offset, String text) throws BadLocationException{
    var m= at.matcher(text);
    m.matches();
    var frame= m.group(1) != null;
    var file= m.group(frame ? 2 : 3);
    var f= ResourcesPlugin.getWorkspace().getRoot().getProject(Output.alias).getFolder(FearlessWatcher.srcName).getFile(new Path(file));
    if (!f.exists()){ return; }
    IntSupplier line= frame ? ()->Integer.parseInt(m.group(1)) : ()->markedLineAfter(console.getDocument(), offset);
    console.addHyperlink(new Link(f, line), offset+m.start(frame ? 2 : 3), file.length());
  }
  private static int markedLineAfter(IDocument doc, int offset){
    var m= marked.matcher(doc.get());
    if (!m.find(offset)){ throw new IllegalStateException("No marked source line follows the compile error at offset "+offset); }
    return Integer.parseInt(m.group(1));
  }
  /// A marker carries the line to the editor and is gone as soon as the editor is open.
  private record Link(IFile file, IntSupplier line) implements IHyperlink{
    @Override public void linkActivated(){
      try{
        var marker= file.createMarker(IMarker.TEXT);
        marker.setAttribute(IMarker.LINE_NUMBER, line.getAsInt());
        IDE.openEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(), marker, true);
        marker.delete();
      }
      catch(CoreException e){ throw new IllegalStateException(e); }
    }
    @Override public void linkEntered(){}
    @Override public void linkExited(){}
  }
}

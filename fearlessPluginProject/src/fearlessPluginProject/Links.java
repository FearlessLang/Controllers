package fearlessPluginProject;

import java.util.function.IntSupplier;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.ui.console.IConsole;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.IHyperlink;
import org.eclipse.ui.console.IPatternMatchListenerDelegate;
import org.eclipse.ui.console.PatternMatchEvent;
import org.eclipse.ui.console.TextConsole;
import org.eclipse.ui.ide.IDE;

/// Registered on org.eclipse.ui.console.consolePatternMatchListeners for the project
/// consoles (Consoles.projectType) and the consoles of a Process: in a stack frame
/// "error line: N in file F" and in a compile error header "In file: fear:/F", F is a link
/// opening that file at line N. A compile error carries its N on the numbered source line
/// "N| " below the header; the console fills in pieces, so that line is looked up when the
/// link is used. F is relative to the project folder, so it is found in the src folder of
/// the mirrored project the console is named after, or whose Process the console shows.
public final class Links implements IPatternMatchListenerDelegate{
  private static final Pattern at= Pattern.compile("error line: (\\d+) in file (\\S+)|In file: fear:/(\\S+)");
  private static final Pattern numbered= Pattern.compile("(\\d+)\\| .*");
  private TextConsole console;
  @Override public void connect(TextConsole console){ this.console= console; }
  @Override public void disconnect(){ console= null; }
  @Override public void matchFound(PatternMatchEvent event){
    try{ link(event.getOffset(), console.getDocument().get(event.getOffset(), event.getLength())); }
    catch(BadLocationException e){ throw new IllegalStateException(e); }
  }
  private void link(int offset, String text) throws BadLocationException{
    var m= at.matcher(text);
    m.matches();
    var frame= m.group(1) != null;
    var file= m.group(frame ? 2 : 3);
    var alias= console instanceof IConsole c ? c.getProcess().getAttribute(Process.aliasAttr) : console.getName().substring(FearlessWatcher.consolePrefix.length());
    var f= ResourcesPlugin.getWorkspace().getRoot().getProject(alias).getFolder(FearlessWatcher.srcName).getFile(new Path(file));
    if (!f.exists()){ return; }
    IntSupplier line= frame ? ()->Integer.parseInt(m.group(1)) : ()->numberedLineAfter(console.getDocument(), offset);
    console.addHyperlink(new Link(f, line), offset+m.start(frame ? 2 : 3), file.length());
  }
  private static int numberedLineAfter(IDocument doc, int offset){
    try{
      for (int i= doc.getLineOfOffset(offset)+1; i < doc.getNumberOfLines(); i++){
        var info= doc.getLineInformation(i);
        var m= numbered.matcher(doc.get(info.getOffset(), info.getLength()));
        if (m.matches()){ return Integer.parseInt(m.group(1)); }
      }
    }
    catch(BadLocationException e){ throw new IllegalStateException(e); }
    throw new IllegalStateException("No numbered source line follows the compile error at offset "+offset);
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

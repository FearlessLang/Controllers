package fearlessPluginProject;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessorExtension;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;

import suggest.Api;
import suggest.Api.Type;
import suggest.Docs;
import suggest.Resolver;
import suggest.Resolver.Row;

/// Registered on org.eclipse.ui.genericeditor.contentAssistProcessors for Fearless sources: after
/// a dot, and on ctrl+space, the methods of the type of the expression before the cursor as the
/// last compile describes them (suggest.Resolver), each with its documentation from the text
/// rendering next to the api json. The edited file, src/_pkg/name.fear of a mirrored project,
/// names the project and the package (a file of no mirrored project, or outside a package
/// folder, gets nothing); the compiled information is the api json of every package
/// of the project and of the standard library, read again when its file changes; the aliases are
/// the use directives of the package head file, taken from the editor when that is the file
/// being edited. The generic editor computes proposals off the UI thread, so the file comes from
/// the document's file buffer.
public final class Assist implements IContentAssistProcessorExtension{
  private record Loaded(long stamp, List<Type> types){}
  private static final Map<Path,Loaded> loaded= new HashMap<>();
  @Override public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset){
    var link= ManagerLink.find().orElseThrow();
    var file= ResourcesPlugin.getWorkspace().getRoot().getFile(FileBuffers.getTextFileBufferManager().getTextFileBuffer(viewer.getDocument()).getLocation());
    var folder= link.projects().get(file.getProject().getName());
    if (folder == null || file.getProjectRelativePath().segmentCount() < 3){ return new ICompletionProposal[0]; }
    var pkgDir= file.getProjectRelativePath().segment(1);
    var out= folder.resolve(".fearless_out");
    var text= viewer.getDocument().get();
    var head= file.getName().startsWith("_rank_") ? text : head(folder.resolve(pkgDir)).map(ManagerLink::read).orElse("");
    var base= link.baseDocs.resolveSibling("base.json");
    var types= Stream.concat(Stream.of(base), jsons(out)).flatMap(p->types(p).stream()).toList();
    var s= new Resolver(new Api(types), pkgDir.substring(1), Resolver.aliases(head), text).suggest(offset);
    if (s.rows().isEmpty()){ return new ICompletionProposal[0]; }
    var receiver= s.receiver().name();
    var pkg= receiver.substring(0, receiver.indexOf('.'));
    var txt= pkg.equals("base") ? link.baseDocs.resolveSibling("base.txt") : out.resolve("gen_java").resolve(pkg+".txt");
    var docs= new Docs(ManagerLink.read(txt), receiver.substring(pkg.length()+1));
    return s.rows().stream().map(r->proposal(r, s.from(), offset, docs)).toArray(ICompletionProposal[]::new);
  }
  private static ICompletionProposal proposal(Row r, int from, int offset, Docs docs){
    return new CompletionProposal(r.insert(), from, offset-from, r.insert().length(), null, r.display(), null, docs.of(r.name(), r.ts().size()).orElse(null));
  }
  private static Optional<Path> head(Path pkgDir){
    try(var files= Files.list(pkgDir)){ return files.filter(p->p.getFileName().toString().startsWith("_rank_")).findFirst(); }
    catch(IOException e){ throw new UncheckedIOException(e); }
  }
  private static Stream<Path> jsons(Path out){
    if (!Files.isDirectory(out)){ return Stream.of(); }
    try(var files= Files.list(out)){ return files.filter(p->p.getFileName().toString().endsWith(".json") && !p.getFileName().toString().startsWith("_")).toList().stream(); }
    catch(IOException e){ throw new UncheckedIOException(e); }
  }
  private static synchronized List<Type> types(Path json){
    var stamp= json.toFile().lastModified();
    return loaded.compute(json, (p,old)->old != null && old.stamp == stamp ? old : new Loaded(stamp, Api.parse(ManagerLink.read(json)))).types;
  }
  @Override public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset){ return null; }
  @Override public boolean isCompletionProposalAutoActivation(char c, ITextViewer viewer, int offset){ return c == '.'; }
  @Override public boolean isContextInformationAutoActivation(char c, ITextViewer viewer, int offset){ return false; }
  @Override public String getErrorMessage(){ return null; }
  @Override public IContextInformationValidator getContextInformationValidator(){ return null; }
}

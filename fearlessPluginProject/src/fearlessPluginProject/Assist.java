package fearlessPluginProject;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import suggest.Api.Ty;
import suggest.Api.Type;
import suggest.Docs;
import suggest.Resolver;
import suggest.Resolver.Row;

/// Registered on org.eclipse.ui.genericeditor.contentAssistProcessors for Fearless sources: after
/// a dot, and on ctrl+space, the methods of the type of the expression before the cursor as the
/// last compile describes them (suggest.Resolver), and the types of the package named before the
/// dot, in a use directive or in code; a type and a method alike come with their documentation
/// from the text rendering next to the api json. The edited file, src/_pkg/name.fear of a
/// mirrored project, names the project and the package (a file of no mirrored project, or outside
/// a package folder, gets nothing); the compiled information is the api json of every package
/// of the project and of the standard library, read again when its file changes; the aliases are
/// the use directives of the head file of the package, its only _rank_ file (none when it has
/// none or several), taken from the editor when that is the file being edited. The generic
/// editor computes proposals off the UI thread, so the file comes from the document's file buffer.
public final class Assist implements IContentAssistProcessorExtension{
  private record Loaded(long stamp, List<Type> types){}
  private record Text(long stamp, Docs docs){}
  private static final Map<Path,Loaded> loaded= new HashMap<>();
  private static final Map<Path,Text> texts= new HashMap<>();
  @Override public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset){
    var file= ResourcesPlugin.getWorkspace().getRoot().getFile(FileBuffers.getTextFileBufferManager().getTextFileBuffer(viewer.getDocument()).getLocation());
    var project= ManagerLink.projects().get(file.getProject().getName());
    if (project == null || file.getProjectRelativePath().segmentCount() < 3){ return new ICompletionProposal[0]; }
    var pkgDir= file.getProjectRelativePath().segment(1);
    var out= project.folder().resolve(".fearless_out");
    var text= viewer.getDocument().get();
    var head= file.getName().startsWith("_rank_") ? text : head(project.folder().resolve(pkgDir));
    var base= ManagerLink.baseCache.resolve("base.json");
    var types= Stream.concat(Stream.of(base), jsons(out)).flatMap(p->types(p).stream()).toList();
    var s= Resolver.of(new Api(types), pkgDir.substring(1), Resolver.aliases(head), text).suggest(offset);
    var typeProposals= s.types().stream().map(t->typeProposal(t, s.from(), offset, docs(txt(out, pkg(t.name())))));
    if (s.rows().isEmpty()){ return typeProposals.toArray(ICompletionProposal[]::new); }
    var docs= docs(txt(out, pkg(s.receiver().name())));
    return Stream.concat(s.rows().stream().map(r->proposal(r, s.from(), offset, docs, s.receiver())), typeProposals).toArray(ICompletionProposal[]::new);
  }
  private static ICompletionProposal proposal(Row r, int from, int offset, Docs docs, Ty receiver){
    return new CompletionProposal(r.insert(), from, offset-from, r.insert().length(), null, r.display(), null, docs.method(simple(receiver.name()), receiver.args().size(), r.name(), r.ts().size()).orElse(null));
  }
  private static ICompletionProposal typeProposal(Ty t, int from, int offset, Docs docs){
    var name= simple(t.name());
    return new CompletionProposal("."+name, from, offset-from, name.length()+1, null, t.show(), null, docs.type(name, t.args().size()).orElse(null));
  }
  private static String pkg(String name){ return name.substring(0, name.indexOf('.')); }
  private static String simple(String name){ return name.substring(name.indexOf('.')+1); }
  private static Path txt(Path out, String pkg){
    return pkg.equals("base") ? ManagerLink.baseCache.resolve("base.txt") : out.resolve("gen_java").resolve(pkg+".txt");
  }
  private static String head(Path pkgDir){
    try(var files= Files.list(pkgDir)){
      var heads= files.filter(p->p.getFileName().toString().startsWith("_rank_")).toList();
      return heads.size() == 1 ? ManagerLink.read(heads.get(0)) : "";
    }
    catch(IOException e){ throw new UncheckedIOException(e); }
  }
  private static Stream<Path> jsons(Path out){
    if (!Files.isDirectory(out)){ return Stream.of(); }
    try(var files= Files.list(out)){ return files.filter(p->p.getFileName().toString().endsWith(".json") && !p.getFileName().toString().startsWith("_")).sorted().toList().stream(); }
    catch(IOException e){ throw new UncheckedIOException(e); }
  }
  private static synchronized List<Type> types(Path json){
    var stamp= json.toFile().lastModified();
    return loaded.compute(json, (p,old)->old != null && old.stamp == stamp ? old : new Loaded(stamp, Api.parse(ManagerLink.read(json)))).types;
  }
  private static synchronized Docs docs(Path txt){
    var stamp= txt.toFile().lastModified();
    return texts.compute(txt, (p,old)->old != null && old.stamp == stamp ? old : new Text(stamp, new Docs(ManagerLink.read(txt)))).docs;
  }
  @Override public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset){ return null; }
  @Override public boolean isCompletionProposalAutoActivation(char c, ITextViewer viewer, int offset){ return c == '.'; }
  @Override public boolean isContextInformationAutoActivation(char c, ITextViewer viewer, int offset){ return false; }
  @Override public String getErrorMessage(){ return null; }
  @Override public IContextInformationValidator getContextInformationValidator(){ return null; }
}

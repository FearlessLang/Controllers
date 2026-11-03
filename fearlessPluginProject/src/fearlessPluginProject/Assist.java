package fearlessPluginProject;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ContextInformation;
import org.eclipse.jface.text.contentassist.ContextInformationValidator;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessorExtension;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import suggest.Api;
import suggest.Api.Ty;
import suggest.Api.Type;
import suggest.Cached;
import suggest.Docs;
import suggest.Resolver;
import suggest.Resolver.Row;

/// Registered on org.eclipse.ui.genericeditor.contentAssistProcessors for Fearless sources: after
/// a dot, and on ctrl+space, the methods of the type of the expression before the cursor as the
/// last compile describes them (suggest.Resolver), and the types of the package named before the
/// dot, in a use directive or in code; a type and a method alike come with their documentation
/// from the text rendering next to the api json, and the methods of a type the last compile does
/// not know, declared in the edited file, with none. The edited file, under the folder _pkg of a
/// mirrored project, names the project and the package (a file of no mirrored project, or outside
/// a package folder, gets nothing); the compiled information is the api json of every package
/// of the project and of the standard library, read again when its file changes, and the map
/// directives of the rank files of the project (suggest.Cached); the aliases are
/// the use directives of the head file of the package, its only _rank_ file (none when it has
/// none or several), taken from the editor when that is the file being edited. The generic
/// editor computes proposals off the UI thread, so the file comes from the document's file buffer.
/// The context information, on ctrl+shift+space and after ( and , is the type expected at the
/// cursor; after Name: the proposals are that type and the types implementing it, as supertypes.
public final class Assist implements IContentAssistProcessorExtension{
  private record Loaded(long stamp, List<Type> types){}
  private record Text(long stamp, Docs docs){}
  private record At(String alias, Path out, Api api, Resolver r, boolean unfinished, List<Path> read){}
  /// a malformed <pkg>.json, a missing <pkg>.txt, or a type or a method missing from the documentation
  private static final class Broken extends RuntimeException{
    private static final long serialVersionUID= 1L;
    Broken(Path file, Exception e){ super("The compilation cache of this project is broken.\nFile: "+file+"\n"+(e instanceof NoSuchFileException ? "The file is missing." : e.getMessage())); }
  }
  private static final Map<Path,Loaded> loaded= new HashMap<>();
  private static final Map<Path,Text> texts= new HashMap<>();
  /// the resolver of the edited text; null for a file of no mirrored project or outside a package folder
  private static At _at(ITextViewer viewer){
    var file= file(viewer);
    var project= ManagerLink.projects().get(FearlessWatcher.alias(file.getProject()));
    var segments= List.of(file.getProjectRelativePath().removeLastSegments(1).segments());
    var at= IntStream.range(1, segments.size()).filter(i -> segments.get(i).startsWith("_") && !segments.get(i).contains(".")).boxed().toList();
    if (project == null || at.size() != 1){ return null; }
    var pkg= segments.get(at.getFirst()).substring(1);
    var out= ManagerLink.out(project.folder());
    var unfinished= Files.exists(out.resolve("_unfinished.txt"));
    var text= viewer.getDocument().get();
    var head= file.getName().startsWith("_rank_") ? text : Cached.rankText(project.folder(), pkg);
    var read= ManagerLink.jsons(out).toList();
    var types= Stream.concat(Stream.of(ManagerLink.baseCache.resolve("base.json")), read.stream()).flatMap(p -> types(p).stream()).toList();
    var api= new Api(types);
    var r= Resolver.of(api, pkg, Cached.packages(project.folder(), pkg), head, text);
    return new At(FearlessWatcher.alias(file.getProject()), out, api, r, unfinished, read);
  }
  @Override public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset){
    try{
      var a= _at(viewer);
      if (a == null){ return new ICompletionProposal[0]; }
      try{ return proposals(a, offset, true); }
      catch (Broken e){
        if (!forgiven(a)){ return new ICompletionProposal[]{ clean(a.alias(), e.getMessage()) }; }
        return proposals(a, offset, false);
      }
    }
    catch (Broken e){ return new ICompletionProposal[]{ clean(FearlessWatcher.alias(file(viewer).getProject()), e.getMessage()) }; }
  }
  private static IFile file(ITextViewer viewer){ return ResourcesPlugin.getWorkspace().getRoot().getFile(FileBuffers.getTextFileBufferManager().getTextFileBuffer(viewer.getDocument()).getLocation()); }
  /// _unfinished.txt was there before the files were read, or is there now, or a <pkg>.json that was read is gone
  private static boolean forgiven(At a){ return a.unfinished() || Files.exists(a.out().resolve("_unfinished.txt")) || a.read().stream().anyMatch(p -> !Files.exists(p)); }
  private static ICompletionProposal[] proposals(At a, int offset, boolean documented){
    var s= a.r().suggest(offset);
    var typeProposals= s.types().stream().map(t -> typeProposal(t, s.from(), offset, doc(a, documented, t.name(), d -> d.type(simple(t.name()), t.args().size()))));
    var others= Stream.concat(typeProposals, s.supers().stream().map(t -> superProposal(t, s.from(), offset, a.r().text(), a.api().entry(t).map(e -> doc(a, documented, t.name(), d -> d.type(simple(t.name()), t.args().size()))).orElse(null))));
    if (s.rows().isEmpty()){ return others.toArray(ICompletionProposal[]::new); }
    return Stream.concat(s.rows().stream().map(r -> proposal(r, s.from(), offset, a.api().entry(r.owner()).map(t -> doc(a, documented, t.name(), d -> d.method(simple(r.owner().name()), r.owner().args().size(), r.rc(), r.name(), r.ts().size()))).orElse(null))), others).toArray(ICompletionProposal[]::new);
  }
  private static String doc(At a, boolean documented, String name, Function<Docs,Optional<String>> f){
    if (!documented){ return null; }
    var txt= txt(a.out(), pkg(name));
    try{ return f.apply(docs(txt)).orElse(null); }
    catch (IllegalArgumentException e){ throw new Broken(txt, e); }
    catch (UncheckedIOException e){ throw new Broken(txt, e.getCause()); }
  }
  private static ICompletionProposal clean(String alias, String error){
    return new ICompletionProposal(){
      @Override public void apply(IDocument document){ ManagerLink.send("DeleteCache", alias); }
      @Override public Point getSelection(IDocument document){ return null; }
      @Override public String getAdditionalProposalInfo(){ return error; }
      @Override public String getDisplayString(){ return "Clean"; }
      @Override public Image getImage(){ return null; }
      @Override public IContextInformation getContextInformation(){ return null; }
    };
  }
  private static ICompletionProposal proposal(Row r, int from, int offset, String doc){
    return new CompletionProposal(r.insert(), from, offset-from, r.insert().length(), null, r.display(), null, doc);
  }
  private static ICompletionProposal typeProposal(Ty t, int from, int offset, String doc){
    var name= simple(t.name());
    return new CompletionProposal("."+name, from, offset-from, name.length()+1, null, t.show(), null, doc);
  }
  private static ICompletionProposal superProposal(Ty t, int from, int offset, String text, String doc){
    var insert= (from > 0 && text.charAt(from-1) == ':' ? " " : "")+t.show();
    return new CompletionProposal(insert, from, offset-from, insert.length(), null, t.show(), null, doc);
  }
  private static String pkg(String name){ return name.substring(0, name.indexOf('.')); }
  private static String simple(String name){ return name.substring(name.indexOf('.')+1); }
  private static Path txt(Path out, String pkg){
    return pkg.equals("base") ? ManagerLink.baseCache.resolve("base.txt") : out.resolve(pkg+".txt");
  }
  private static synchronized List<Type> types(Path json){
    var stamp= json.toFile().lastModified();
    try{ return loaded.compute(json, (p,old) -> old != null && old.stamp == stamp ? old : new Loaded(stamp, Api.parse(ManagerLink.read(json)).types())).types; }
    catch (IllegalArgumentException e){ throw new Broken(json, e); }
    catch (UncheckedIOException e){
      if (!(e.getCause() instanceof NoSuchFileException)){ throw e; }
      return List.of();
    }
  }
  private static synchronized Docs docs(Path txt){
    var stamp= txt.toFile().lastModified();
    return texts.compute(txt, (p,old) -> old != null && old.stamp == stamp ? old : new Text(stamp, new Docs(ManagerLink.read(txt)))).docs;
  }
  @Override public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset){
    var a= _at(viewer);
    var t= a == null ? Ty.unknown : a.r().expected(offset);
    if (t.equals(Ty.unknown)){ return new IContextInformation[0]; }
    return new IContextInformation[]{ new ContextInformation(t.show(), "expected: "+t.show()) };
  }
  @Override public boolean isCompletionProposalAutoActivation(char c, ITextViewer viewer, int offset){ return c == '.'; }
  @Override public boolean isContextInformationAutoActivation(char c, ITextViewer viewer, int offset){ return c == '(' || c == ','; }
  @Override public String getErrorMessage(){ return null; }
  @Override public IContextInformationValidator getContextInformationValidator(){ return new ContextInformationValidator(this); }
}

package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;

import fileAssociations.FileAssociations;
import resources.ResolveResource;
import tools.Fs;
import tools.JavacTool;
import utils.OneOr;
import utils.Push;

/// A test of the manager DeployManagedFearless.java builds. Its setup, repeated when the test ends: no manager runs, the manager has no data folder, helloWorld and testGui1 were never compiled, and nothing is registered for .fearless.
abstract class ManagerTest extends PilotTest{
  static final Path app= ResolveResource.managedFolderOut.resolve("fearlessManaged"+ResolveResource.versionId);
  static final Path launcher= Fs.isWindows() ? app.resolve(app.getFileName()+".exe") : app.resolve("bin").resolve(app.getFileName().toString());
  static final Path project= ResolveResource.integrationTests.resolve("helloWorld");
  static final Path other= ResolveResource.integrationTests.resolve("helloStackTraces");
  static final Path gui= ResolveResource.integrationTests.resolve("testGui1");
  static final Path data= app.resolveSibling(JavacTool.dataDirNameFor(ResolveResource.versionId));
  static final Path info= data.resolve("projects.info");
  static final Path state= data.resolve("eclipse").resolve("state.info");
  static final Path notes= data.resolve("eclipse").resolve("console.txt");
  static final Path share= Path.of(System.getProperty("user.home"),".local","share");
  static final String killed= Fs.isWindows() ? "1" : "143";
  static String slashed(Path p){ return p.toString().replace('\\','/'); }
  static String escaped(Path p){ return p.toString().replace("\\","\\\\"); }
  static boolean elevated(){ return Fs.isWindows() && Fs.of(()->new ProcessBuilder("net","session").redirectOutput(Redirect.DISCARD).redirectError(Redirect.DISCARD).start().onExit().join().exitValue())==0; }
  static List<String> registered(){
    if (Fs.isWindows()){ return Fs.of(()->new ProcessBuilder("reg","query","HKCU\\Software\\Classes\\.fearless").redirectOutput(Redirect.DISCARD).redirectError(Redirect.DISCARD).start().onExit().join().exitValue())==0 ? List.of(".fearless") : List.of(); }
    return Stream.of(share.resolve("applications"),share.resolve("mime").resolve("packages")).flatMap(d->Fs.walk(d,s->s.filter(p->p.getFileName().toString().contains("earless")).map(Path::toString).toList()).stream()).toList();
  }
  Process launch(String... args) throws Exception{
    var before= pilot.shot();
    var res= new ProcessBuilder(Push.of(launcher.toString(),List.of(args))).redirectOutput(Redirect.DISCARD).redirectError(Redirect.DISCARD).start();
    until(()->!same(before,pilot.shot()));
    return res;
  }
  BufferedImage look(){
    var s= Toolkit.getDefaultToolkit().getScreenSize();
    pilot.glide(s.width-1,s.height/2,Pilot.none,s.width-1,s.height/2,Pilot.none);
    return pilot.shot();
  }
  int[] pixels(int[] at){ return pilot.shot().getRGB(at[0],at[1],at[2],at[3],null,0,at[2]); }
  static boolean same(BufferedImage a, BufferedImage b){
    var pa= a.getRGB(0,0,a.getWidth(),a.getHeight(),null,0,a.getWidth());
    var pb= b.getRGB(0,0,b.getWidth(),b.getHeight(),null,0,b.getWidth());
    assert pa.length==pb.length;
    var diff= IntStream.range(0,pa.length).filter(i->Math.abs((pa[i]&0xff)-(pb[i]&0xff))+Math.abs((pa[i]>>8&0xff)-(pb[i]>>8&0xff))+Math.abs((pa[i]>>16&0xff)-(pb[i]>>16&0xff))>30).count();
    return diff*20<pa.length;
  }
  @AfterEach void clean() throws Exception{
    stopManagers();
    Fs.rmTree(data);
    Fs.rmTree(project.resolve(".fearless_out"));
    Fs.rmTree(gui.resolve(".fearless_out"));
    if (Fs.isWindows()){ FileAssociations.eradicateAll(s->s.contains("earless"),RuntimeException::new); return; }
    for (var dir: List.of(share.resolve("applications"),share.resolve("mime").resolve("packages"))){
      Fs.walkV(dir,s->s.filter(p->p.getFileName().toString().contains("earless")).toList().forEach(p->Fs.ofV(()->Files.delete(p))));
    }
    assertEquals(0,new ProcessBuilder("update-mime-database",share.resolve("mime").toString()).start().waitFor());
    assertEquals(0,new ProcessBuilder("update-desktop-database",share.resolve("applications").toString()).start().waitFor());
  }
  static void stopManagers(){ ProcessHandle.allProcesses().filter(p->p.info().command().filter(launcher.toString()::equals).isPresent()).forEach(ManagerTest::kill); }
  private static void kill(ProcessHandle p){
    var all= Stream.concat(p.descendants(),Stream.of(p)).toList();
    all.forEach(ProcessHandle::destroyForcibly);
    all.forEach(h->h.onExit().join());
  }
  static ProcessHandle program(Process manager){ return OneOr.of("program",manager.descendants().filter(p->p.info().command().filter(c->Path.of(c).getFileName().toString().startsWith("java")).isPresent())); }
}

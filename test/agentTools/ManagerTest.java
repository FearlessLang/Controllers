package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;

import resources.ResolveResource;
import tools.Fs;
import tools.JavacTool;
import utils.Bug;
import utils.Push;

/// A test of the manager DeployManagedFearless.java builds. Its setup, repeated when the test ends: no manager runs, the manager has no data folder, helloWorld was never compiled, and nothing is registered for .fearless.
abstract class ManagerTest extends PilotTest{
  static final Path app= ResolveResource.managedFolderOut.resolve("fearlessManaged"+ResolveResource.versionId);
  static final Path launcher= Fs.isWindows() ? app.resolve(app.getFileName()+".exe") : app.resolve("bin").resolve(app.getFileName().toString());
  static final Path project= ResolveResource.integrationTests.resolve("helloWorld");
  static final Path data= app.resolveSibling(JavacTool.dataDirNameFor(ResolveResource.versionId));
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
    if (Fs.isWindows()){ throw Bug.todo(); }
    var share= Path.of(System.getProperty("user.home"),".local","share");
    for (var dir: List.of(share.resolve("applications"),share.resolve("mime").resolve("packages"))){
      Fs.walkV(dir,s->s.filter(p->p.getFileName().toString().contains("earless")).toList().forEach(p->Fs.ofV(()->Files.delete(p))));
    }
    assertEquals(0,new ProcessBuilder("update-mime-database",share.resolve("mime").toString()).start().waitFor());
    assertEquals(0,new ProcessBuilder("update-desktop-database",share.resolve("applications").toString()).start().waitFor());
  }
  static void stopManagers(){ ProcessHandle.allProcesses().filter(p->p.info().command().filter(launcher.toString()::equals).isPresent()).forEach(ManagerTest::kill); }
  private static void kill(ProcessHandle p){ p.destroyForcibly(); p.onExit().join(); }
}

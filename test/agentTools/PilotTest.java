package agentTools;

import java.awt.image.BufferedImage;
import java.io.UncheckedIOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import tools.Fs;
import utils.OneOr;

public abstract class PilotTest{
  public record On(String desk, long... values){}
  protected static On linux(long... values){ return new On("linux",values); }
  protected static On windows(long... values){ return new On("windows",values); }
  private static final Optional<Path> channel= Optional.ofNullable(System.getProperty("pilot")).map(Path::of);
  private static final String os= System.getProperty("os.name").toLowerCase(Locale.ROOT);
  protected final Pilot pilot= new Pilot();
  private final ArrayList<Aim> aims= new ArrayList<>();
  private long last;
  final At shellRestarted= new At("shellRestarted",linux(0),windows(6000));
  protected abstract void walk() throws Exception;
  protected void forgetWindowPlaces(){
    if (!Fs.isWindows()){ return; }
    var shell= "HKCU\\Software\\Classes\\Local Settings\\Software\\Microsoft\\Windows\\Shell\\";
    List.of(List.of("taskkill","/f","/im","explorer.exe"),List.of("reg","delete",shell+"Bags","/f"),List.of("reg","delete",shell+"BagMRU","/f")).forEach(PilotTest::quietly);
    Fs.ofV(()->new ProcessBuilder("explorer.exe").start());
    last= System.currentTimeMillis();
    shellRestarted.go();
  }
  private static void quietly(List<String> command){ Fs.ofV(()->new ProcessBuilder(command).redirectOutput(Redirect.DISCARD).redirectError(Redirect.DISCARD).start().onExit().join()); }
  @Test void walks() throws Exception{
    var unrecorded= channel.isEmpty() && aims.stream().anyMatch(a->a.recorded().isEmpty());
    if (unrecorded){ Assumptions.abort("This desk has no recording yet: walk the test here in agent mode and write down where each aim lands."); }
    channel.ifPresent(Fs::cleanDir);
    last= System.currentTimeMillis();
    walk();
  }
  protected void until(BooleanSupplier done){
    var end= System.currentTimeMillis()+60_000;
    while (!holds(done)){ assert System.currentTimeMillis()<end; Pilot.pause(100); }
    last= System.currentTimeMillis();
  }
  private static boolean holds(BooleanSupplier done){
    try{ return done.getAsBoolean(); }
    catch(UncheckedIOException e){ if (e.getCause() instanceof NoSuchFileException){ return false; } throw e; }
  }
  public abstract class Aim{
    private final String name;
    private final int arity;
    private final List<On> ons;
    Aim(String name, int arity, On... ons){
      this.name= name;
      this.arity= arity;
      this.ons= List.of(ons);
      assert this.ons.stream().allMatch(o->o.values().length==arity);
      aims.add(this);
    }
    private Optional<On> recorded(){ return OneOr.opt(name,ons.stream().filter(o->os.contains(o.desk()))); }
    long[] take(){ return channel.map(this::ask).orElseGet(()->recorded().orElseThrow().values()); }
    int[] aim(){ return Arrays.stream(take()).mapToInt(Math::toIntExact).toArray(); }
    private long[] ask(Path channel){
      var file= channel.resolve(name);
      Fs.ofV(()->Files.write(file,new byte[0],StandardOpenOption.CREATE));
      for (var v= answer(file); ; v= answer(file)){
        if (v.isPresent()){ Fs.ofV(()->Files.delete(file)); assert v.get().length==arity; return v.get(); }
        Fs.walkV(channel,s->s.filter(p->p.toString().endsWith(".do")).toList().forEach(this::serve));
        Pilot.pause(100);
      }
    }
    private void serve(Path request){
      var v= answer(request);
      if (v.isEmpty()){ return; }
      switch(request.getFileName().toString()){
        case "shot.do" -> Fs.ofV(()->ImageIO.write(pilot.shot(),"png",request.resolveSibling("shot.png").toFile()));
        case "hover.do" -> pilot.glide((int)v.get()[0],(int)v.get()[1],Pilot.none,(int)v.get()[0],(int)v.get()[1],Pilot.none);
        default -> throw new AssertionError(request);
      }
      Fs.ofV(()->Files.delete(request));
    }
  }
  private static Optional<long[]> answer(Path file){
    var s= Fs.readUtf8(file).strip();
    if (!s.endsWith(";")){ return Optional.empty(); }
    var body= s.substring(0,s.length()-1).strip();
    return Optional.of(body.isEmpty() ? new long[0] : Stream.of(body.split("\\s+")).mapToLong(Long::parseLong).toArray());
  }
  public final class At extends Aim{
    public At(String name, On... ons){ super(name,1,ons); }
    public void go(){
      var wait= take()[0]+(channel.isEmpty() ? last : 0)-System.currentTimeMillis();
      if (wait>0){ Pilot.pause((int)wait); }
      last= System.currentTimeMillis();
    }
  }
  public final class Click extends Aim{
    public Click(String name, On... ons){ super(name,2,ons); }
    public void go(){ var v= aim(); pilot.click(v[0],v[1]); }
  }
  public final class DoubleClick extends Aim{
    public DoubleClick(String name, On... ons){ super(name,2,ons); }
    public void go(){ var v= aim(); pilot.doubleClick(v[0],v[1]); }
  }
  public final class Drag extends Aim{
    public Drag(String name, On... ons){ super(name,4,ons); }
    public void go(){ var v= aim(); pilot.drag(v[0],v[1],v[2],v[3]); }
  }
  public final class Area extends Aim{
    public Area(String name, On... ons){ super(name,4,ons); }
    public BufferedImage shot(){ var v= aim(); return pilot.shot().getSubimage(v[0],v[1],v[2],v[3]); }
  }
}

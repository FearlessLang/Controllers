package gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.Color;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import controller.Facts;
import controller.Project;
import controller.Registry.Entry;
import controller.Registry.Kind;
import coordinator.MainsInfo;
import tools.Fs;
import utils.Box;
import utils.OneOr;
import utils.ThrowingConsumer;

/// A Project rendered into a Panel, its controls used on the EDT, and the requests they make read back as messages: verb, name, argument.
final class PanelTest{
  private final List<String> asked= new ArrayList<>();
  private final Panel panel= onEdt(()->new Panel((v,n,a)->asked.add(v+"\n"+n+"\n"+a),asked::add));
  private static Project project(String alias, Kind kind, Optional<List<String>> known, List<String> chosen, Map<String,List<String>> reads, boolean upToDate, String job, long run){
    var entry= new Entry(alias,Path.of(alias).toAbsolutePath(),kind,chosen,reads,Map.of(),-1,run);
    var facts= new Facts(1,1,0,List.of("hello"),true,upToDate,Optional.empty(),List.of(),Optional.empty());
    var mains= known.<Map<String,String>>map(ms->Collections.unmodifiableMap(ms.stream().collect(Collectors.toMap(m->m,_->"_hello/_rank_app.fear",(x,_)->x,LinkedHashMap::new))));
    return new Project(entry,facts,mains,Project.noClaims,Optional.empty(),job,Instant.EPOCH,0,"",-1,"");
  }
  private static Project code(List<String> known, List<String> chosen){ return project("hello",Kind.code,Optional.of(known),chosen,Map.of(),true,"",-1); }
  private static final List<String> abc= List.of("hello.A","hello.B","hello.C");
  private void render(Project p, Project... others){ onEdt(()->{ panel.render(p,Stream.concat(Stream.of(p),Stream.of(others)).toList()); return null; }); }
  private <T extends Component> T named(String name){
    @SuppressWarnings("unchecked") var res= (T)onEdt(()->OneOr.of(name,all(panel.root).filter(c->name.equals(c.getName()))));
    return res;
  }
  private static Stream<Component> all(Component c){
    if (!(c instanceof Container k)){ return Stream.of(c); }
    return Stream.concat(Stream.of(c),Stream.of(k.getComponents()).flatMap(PanelTest::all));
  }
  private void click(String name){ AbstractButton b= named(name); onEdt(()->{ b.doClick(); return null; }); }
  private static void loseFocus(JTextField f){
    onEdt(()->{
      for (var l: f.getFocusListeners()){ l.focusLost(new FocusEvent(f,FocusEvent.FOCUS_LOST)); }
      return null;
    });
  }
  private void asked(String... requests){ assertEquals(List.of(requests),asked); }
  @Test void theMainButtonMakesTheRequestItsTextNames(){
    render(project("hello",Kind.code,Optional.empty(),List.of(),Map.of(),false,"",-1));
    click("action");
    render(code(abc,List.of("hello.B")));
    click("action");
    render(project("hello",Kind.code,Optional.of(abc),List.of(),Map.of(),true,"hello.B",-1));
    click("action");
    render(project("hello",Kind.dataReadOnly,Optional.empty(),List.of(),Map.of(),true,"",-1));
    click("action");
    asked("compile\nhello\n","run\nhello\n","terminate\nhello\n","check\nhello\n");
  }
  @Test void theMainBoxesAskForWhatTheyShowWhenClicked(){
    render(code(abc,List.of()));
    click("hello.A");
    click("hello.B");
    click("hello.A");
    asked("mains\nhello\nhello.A","mains\nhello\nhello.A hello.B","mains\nhello\nhello.B");
  }
  @Test void allAndNoneAskForEveryMainAndForNone(){
    render(code(abc,List.of("hello.B")));
    click("all");
    click("none");
    asked("mains\nhello\nhello.A hello.B hello.C","mains\nhello\n");
  }
  @Test void allAndNoneAreHiddenWhenThereIsNoChoice(){
    render(code(List.of("hello.A"),List.of("hello.B")));
    assertFalse(this.<Component>named("all").getParent().isVisible());
    render(project("hello",Kind.code,Optional.empty(),List.of("hello.B"),Map.of(),false,"",-1));
    assertFalse(this.<Component>named("none").getParent().isVisible());
  }
  @Test void theKindButtonsAskForTheirKind(){
    render(project("hello",Kind.idle,Optional.empty(),List.of(),Map.of(),true,"",-1));
    click("data:readWrite");
    render(code(abc,List.of()));
    click("idle");
    asked("kind\nhello\ndata:readWrite","kind\nhello\nidle");
  }
  private static Project notes(){ return project("notes",Kind.dataReadWrite,Optional.empty(),List.of(),Map.of(),true,"",-1); }
  @Test void aLinkFieldLosingFocusAsksForWhatItShowsWhenChanged(){
    render(project("hello",Kind.code,Optional.of(abc),List.of(),Map.of("notes",List.of("Notes")),true,"",-1),notes());
    loseFocus(named("notes read"));
    JTextField write= named("notes write");
    onEdt(()->{ write.setText("Diary Log"); return null; });
    loseFocus(write);
    asked("link\nhello\nnotes write Diary Log");
  }
  @Test void aRedrawNeitherSendsALinkNorLosesWhatIsTyped(){
    render(project("hello",Kind.code,Optional.of(abc),List.of(),Map.of(),true,"",-1),notes());
    JTextField typing= named("notes read");
    onEdt(()->{ typing.setText("No"); return null; });
    render(project("hello",Kind.code,Optional.of(abc),List.of(),Map.of(),true,"",1000),notes());
    loseFocus(typing);
    asked();
    JTextField shown= named("notes read");
    assertNotSame(typing,shown);
    assertEquals("No",onEdt(shown::getText));
    loseFocus(shown);
    asked("link\nhello\nnotes read No");
  }
  @Test void aLinkToAProjectNoLongerDataStaysShownAndCanBeEmptied(){
    var reader= project("hello",Kind.code,Optional.of(abc),List.of(),Map.of("notes",List.of("Notes")),true,"",-1);
    render(reader,project("notes",Kind.idle,Optional.empty(),List.of(),Map.of(),true,"",-1));
    assertEquals("Notes",onEdt(this.<JTextField>named("notes read")::getText));
    render(reader);
    JTextField shown= named("notes read");
    onEdt(()->{ shown.setText(""); return null; });
    loseFocus(shown);
    asked("link\nhello\nnotes read ");
  }
  @Test void theMainsShrinkBackOnceUnknownAgain(){
    var unknown= project("hello",Kind.code,Optional.empty(),List.of(),Map.of(),false,"",-1);
    render(unknown);
    Component scroll= this.<Component>named("mains").getParent().getParent();
    var height= onEdt(()->scroll.getPreferredSize().height);
    render(code(abc,List.of()));
    render(unknown);
    assertEquals(height,onEdt(()->scroll.getPreferredSize().height));
  }
  @Test void aFailedCompileShowsTheProjectInvalid(){
    var p= project("hello",Kind.code,Optional.empty(),List.of(),Map.of(),false,"",-1);
    var failed= new Project(p.entry(),p.facts(),p.mains(),p.claims(),p.linkProblem(),"",Instant.EPOCH,0,"",-1,"boom\n");
    assertEquals(Optional.of("boom\n"),failed.problem());
    render(failed);
    assertEquals(1L,onEdt(()->all(panel.root).filter(c->c instanceof JLabel l && l.getText().equals("<invalid: see Error report>")).count()));
  }
  private static MainsInfo.Claim claim(String icon, String ext){ return new MainsInfo.Claim(icon,"icons/x.png","","",ext); }
  private static Project claiming(Path dir, String alias, List<MainsInfo.Claim> shortcuts, List<MainsInfo.Claim> openWiths){
    var p= project(alias,Kind.code,Optional.of(List.of(alias+".Main")),List.of(),Map.of(),true,"",-1);
    var entry= new Entry(alias,dir.resolve(alias),Kind.code,List.of(),Map.of(),Map.of(),-1,-1);
    var claims= new MainsInfo(Map.of(alias+".Main",new MainsInfo.Main("_"+alias+"/main.fear",shortcuts,openWiths)));
    return new Project(entry,p.facts(),p.mains(),claims,Optional.empty(),"",Instant.EPOCH,0,"",-1,"");
  }
  private List<String> claimTexts(){
    Component claims= named("claims");
    return onEdt(()->all(claims).filter(c->c instanceof JLabel).map(c->Objects.requireNonNullElse(((JLabel)c).getText(),"<icon>")).toList());
  }
  private static void png(Path file, Color color){
    var img= new BufferedImage(64,64,BufferedImage.TYPE_INT_RGB);
    var g= img.createGraphics();
    g.setColor(color);
    g.fillRect(0,0,64,64);
    Fs.ensureDir(file.getParent());
    Fs.ofV(()->ImageIO.write(img,"png",file.toFile()));
  }
  private List<Integer> shown(String text){
    Component claims= named("claims");
    var icon= (ImageIcon)onEdt(()->OneOr.of(text,all(claims).filter(c->c instanceof JLabel l && text.equals(l.getText())).map(c->((JLabel)c).getIcon())));
    var shown= new BufferedImage(20,20,BufferedImage.TYPE_INT_RGB);
    shown.createGraphics().drawImage(icon.getImage(),0,0,null);
    return List.of(icon.getIconWidth(),icon.getIconHeight(),shown.getRGB(10,10) & 0xFFFFFF);
  }
  @Test void eachMainShowsTheExtensionsItOpensItsConflictsAndItsShortcuts(@TempDir Path dir){
    png(dir.resolve("hello").resolve(Facts.outDir).resolve("icons").resolve("hello.IconsFoo.png"),Color.red);
    var hello= claiming(dir,"hello",List.of(claim("hello.IconsApp","app")),List.of(claim("hello.IconsFoo","foo"),claim("hello.IconsFoo","bar")));
    var others= List.of("d","b","c").stream().map(a->claiming(dir,a,List.of(),List.of(claim(a+".IconsBar","bar")))).toArray(Project[]::new);
    render(hello,others);
    assertEquals(List.of("hello.Main","Extensions this main opens:",".app",".foo","Conflicting extensions:",".bar","also claimed by","b::b.Main","and 2 more","Shortcuts (double click to run):","<icon>"),claimTexts());
    assertEquals(List.of(20,20,0xFF0000),shown(".foo"));
    JLabel shortcut= named("shortcut hello.Main app");
    onEdt(()->{
      for (var l: shortcut.getMouseListeners()){ l.mouseClicked(new MouseEvent(shortcut,MouseEvent.MOUSE_CLICKED,0,0,1,1,1,false)); }
      for (var l: shortcut.getMouseListeners()){ l.mouseClicked(new MouseEvent(shortcut,MouseEvent.MOUSE_CLICKED,0,0,1,1,2,false)); }
      return null;
    });
    asked("run\nhello\nhello.Main");
    render(hello,others[0]);
    assertEquals(List.of("hello.Main","Extensions this main opens:",".app",".foo","Conflicting extensions:",".bar","also claimed by","d::d.Main","Shortcuts (double click to run):","<icon>"),claimTexts());
    render(hello);
    assertEquals(List.of("hello.Main","Extensions this main opens:",".app",".bar",".foo","Shortcuts (double click to run):","<icon>"),claimTexts());
    assertTrue(onEdt(this.<Component>named("claims")::isVisible));
    render(code(abc,List.of()));
    assertFalse(onEdt(this.<Component>named("claims")::isVisible));
  }
  @Test void anIconRewrittenOnDiskIsShownAtTheNextRender(@TempDir Path dir){
    var file= dir.resolve("b").resolve(Facts.outDir).resolve("icons").resolve("b.IconsBar.png");
    png(file,Color.red);
    var hello= claiming(dir,"hello",List.of(),List.of(claim("hello.IconsBar","bar")));
    var b= claiming(dir,"b",List.of(),List.of(claim("b.IconsBar","bar")));
    render(hello,b);
    assertEquals(List.of(20,20,0xFF0000),shown("b::b.Main"));
    png(file,Color.blue);
    Fs.ofV(()->Files.setLastModifiedTime(file,FileTime.fromMillis(file.toFile().lastModified()+10_000)));
    render(hello,b);
    assertEquals(List.of(20,20,0x0000FF),shown("b::b.Main"));
  }
  @Test void manyClaimsScrollInsteadOfPushingTheRestOfThePanelAway(@TempDir Path dir){
    var mains= IntStream.range(0,40).boxed().collect(Collectors.toMap(i->"hello.Main"+i,i->new MainsInfo.Main("_hello/main.fear",List.of(),List.of(claim("hello.IconsFoo","e"+i)))));
    var p= claiming(dir,"hello",List.of(),List.of());
    render(new Project(p.entry(),p.facts(),p.mains(),new MainsInfo(mains),Optional.empty(),"",Instant.EPOCH,0,"",-1,""));
    assertEquals(120,claimTexts().size());
    Component claims= named("claims");
    assertTrue(onEdt(()->claims.getPreferredSize().height) <= 240);
  }
  private static <T> T onEdt(Supplier<T> f){
    var out= new Box<T>(null);
    ThrowingConsumer.of(SwingUtilities::invokeAndWait).accept(()->out.set(f.get()));
    return out.get();
  }
}

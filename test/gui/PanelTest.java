package gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.FocusEvent;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.AbstractButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import controller.Facts;
import controller.Project;
import controller.Registry.Entry;
import controller.Registry.Kind;
import utils.Box;
import utils.OneOr;
import utils.ThrowingConsumer;

/// A Project rendered into a Panel, its controls used on the EDT, and the requests they make read back as messages: verb, name, argument.
final class PanelTest{
  private final List<String> asked= new ArrayList<>();
  private final Panel panel= onEdt(()->new Panel((v,n,a)->asked.add(v+"\n"+n+"\n"+a)));
  private static Project project(String alias, Kind kind, Optional<List<String>> known, List<String> chosen, Map<String,List<String>> reads, boolean upToDate, String job, long run){
    var entry= new Entry(alias,Path.of(alias).toAbsolutePath(),kind,chosen,reads,Map.of(),-1,run);
    var facts= new Facts(1,1,0,List.of("hello"),true,upToDate,Optional.empty(),List.of(),Optional.empty());
    var mains= known.<Map<String,String>>map(ms->Collections.unmodifiableMap(ms.stream().collect(Collectors.toMap(m->m,_->"_hello/_rank_app.fear",(x,_)->x,LinkedHashMap::new))));
    return new Project(entry,facts,mains,Optional.empty(),job,Instant.EPOCH,0,"",-1,"");
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
  private static <T> T onEdt(Supplier<T> f){
    var out= new Box<T>(null);
    ThrowingConsumer.of(SwingUtilities::invokeAndWait).accept(()->out.set(f.get()));
    return out.get();
  }
}

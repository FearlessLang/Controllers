package gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Timer;

import controller.Main;
import controller.Manager.State;
import controller.Project;

/// The grid of registered projects: one tile per project, its icon badged with its state.
@SuppressWarnings("serial")
public final class Tiles extends JPanel{
  static final int iconSize= 48;
  final JList<Project> list= new JList<>();
  private final Timer spinner= new Timer(80,_ -> list.repaint());
  private State state= new State(List.of(),Optional.empty(),List.of());
  private Map<Project,Image> images= Map.of();
  public Tiles(){
    super(new BorderLayout());
    list.setName("tiles");
    list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
    list.setVisibleRowCount(-1);
    list.setFixedCellWidth(128);
    list.setFixedCellHeight(88);
    list.setCellRenderer(new Tile());
    list.addMouseListener(new MouseAdapter(){
      @Override public void mouseClicked(MouseEvent e){ clicked(e); }
    });
    add(new JScrollPane(list),BorderLayout.CENTER);
  }
  private void clicked(MouseEvent e){
    var i= list.locationToIndex(e.getPoint());
    if (i < 0 || !list.getCellBounds(i,i).contains(e.getPoint())){ return; }
    var p= list.getModel().getElementAt(i);
    var runs= e.getClickCount() == 2 && p.state() == Project.State.codeCompiled && !p.selectedMains().isEmpty();
    if (!runs){ Main.command("Show",p.alias()); return; }
    Main.command("Run",Stream.concat(Stream.of(p.alias()),p.selectedMains().stream()).toArray(String[]::new));
  }
  static JLabel desktop(JLabel res, Icon icon, String text){
    res.setText(text);
    res.setIcon(icon);
    res.setHorizontalAlignment(JLabel.CENTER);
    res.setHorizontalTextPosition(JLabel.CENTER);
    res.setVerticalTextPosition(JLabel.BOTTOM);
    res.setPreferredSize(new Dimension(128,88));
    return res;
  }
  public void render(State s){
    var moved= !s.selected().equals(state.selected());
    state = s;
    var old= images;
    images = s.projects().stream().collect(Collectors.toMap(Function.identity(),p -> old.containsKey(p) ? old.get(p) : Icons.of(p,iconSize)));
    var rows= s.projects().stream().sorted(Comparator.comparing(Project::alias)).toList();
    list.setListData(rows.toArray(Project[]::new));
    var at= s.selected().map(f -> rows.stream().map(Project::folder).toList().indexOf(f)).orElse(-1);
    if (at >= 0){ list.setSelectedIndex(at); }
    if (at >= 0 && moved){ list.ensureIndexIsVisible(at); }
    var anyBusy= rows.stream().anyMatch(Project::busy);
    if (anyBusy && !spinner.isRunning()){ spinner.start(); }
    if (!anyBusy && spinner.isRunning()){ spinner.stop(); }
  }
  private final class Tile extends DefaultListCellRenderer{
    @Override public Component getListCellRendererComponent(JList<?> l, Object value, int i, boolean selected, boolean focus){
      var p= (Project)value;
      var res= desktop((JLabel)super.getListCellRendererComponent(l,value,i,selected,focus),new Icons.Badge(images.get(p),iconSize,p.state()),p.alias());
      res.setToolTipText(p.folder()+" - "+p.state().text);
      res.setBorder(selected ? BorderFactory.createLineBorder(l.getSelectionBackground().darker(),3) : null);
      return res;
    }
  }
}

package gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Timer;

import controller.Manager.State;
import controller.Project;

/// The grid of registered projects: one tile per project, its icon badged with its state.
@SuppressWarnings("serial")
public final class Tiles extends JPanel{
  public enum Sort{
    Name, Modified, Compiled, Run;
    Comparator<Project> comparator(){ return switch(this){
      case Name -> Comparator.comparing(Project::alias,String.CASE_INSENSITIVE_ORDER);
      case Modified -> Comparator.<Project>comparingLong(p->p.facts().modified()).reversed();
      case Compiled -> Comparator.<Project>comparingLong(p->p.entry().compiled()).reversed();
      case Run -> Comparator.<Project>comparingLong(p->p.entry().run()).reversed();
    };}
  }
  private static final int iconSize= 48;
  final JList<Project> list= new JList<>();
  final JComboBox<Sort> sort= new JComboBox<>(Sort.values());
  private final Timer spinner= new Timer(80,_->list.repaint());
  private State state= new State(List.of(),Optional.empty());
  private Map<Project,Image> images= Map.of();
  public Tiles(Consumer<String> onSelect){
    super(new BorderLayout());
    list.setName("tiles");
    list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
    list.setVisibleRowCount(-1);
    list.setFixedCellWidth(128);
    list.setFixedCellHeight(88);
    list.setCellRenderer(new Tile());
    list.addMouseListener(new MouseAdapter(){
      @Override public void mouseClicked(MouseEvent e){
        var i= list.locationToIndex(e.getPoint());
        if (i >= 0 && list.getCellBounds(i,i).contains(e.getPoint())){ onSelect.accept(list.getModel().getElementAt(i).alias()); }
      }
    });
    sort.setName("sort");
    sort.addActionListener(_->render(state));
    var top= new JPanel(new FlowLayout(FlowLayout.LEFT));
    top.add(new JLabel("Order by"));
    top.add(sort);
    add(top,BorderLayout.NORTH);
    add(new JScrollPane(list),BorderLayout.CENTER);
  }
  public void render(State s){
    var moved= !s.selected().equals(state.selected());
    state= s;
    var old= images;
    images= s.projects().stream().collect(Collectors.toMap(Function.identity(),p->old.containsKey(p) ? old.get(p) : Icons.of(p,iconSize)));
    var rows= s.projects().stream().sorted(((Sort)sort.getSelectedItem()).comparator()).toList();
    list.setListData(rows.toArray(Project[]::new));
    var at= s.selected().map(f->rows.stream().map(Project::folder).toList().indexOf(f)).orElse(-1);
    if (at >= 0){ list.setSelectedIndex(at); }
    if (at >= 0 && moved){ list.ensureIndexIsVisible(at); }
    var anyBusy= rows.stream().anyMatch(Project::busy);
    if (anyBusy && !spinner.isRunning()){ spinner.start(); }
    if (!anyBusy && spinner.isRunning()){ spinner.stop(); }
  }
  private final class Tile extends DefaultListCellRenderer{
    @Override public Component getListCellRendererComponent(JList<?> l, Object value, int i, boolean selected, boolean focus){
      var res= (JLabel)super.getListCellRendererComponent(l,value,i,selected,focus);
      var p= (Project)value;
      res.setText(p.alias());
      res.setIcon(new Icons.Badge(images.get(p),iconSize,p.state()));
      res.setHorizontalAlignment(CENTER);
      res.setHorizontalTextPosition(CENTER);
      res.setVerticalTextPosition(BOTTOM);
      res.setToolTipText(p.folder()+" - "+p.state().text);
      res.setBorder(selected ? BorderFactory.createLineBorder(l.getSelectionBackground().darker(),3) : null);
      return res;
    }
  }
}

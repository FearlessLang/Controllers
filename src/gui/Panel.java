package gui;

import static gui.Window.doubleClicked;
import static gui.Window.mono;
import static gui.Window.named;
import static gui.Window.small;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import controller.Facts;
import controller.Main;
import controller.Project;
import controller.Registry.Kind;
import fileSupport.LogFiles;
import managerMessages.Report;
import tools.Fs;
import tools.OpenPath;
import utils.Push;

/// The right half of the window: everything about one registered project, and the
/// buttons that act on it. One Panel per project, kept while the project stays registered:
/// it holds the Output of the project.
final class Panel{
  private static final int iconSize= 32;
  private static final int claimSize= 20;
  private final Consumer<String> refuse;
  final JPanel root= new JPanel(new BorderLayout(8, 8));
  final JTextArea output= mono(named(new JTextArea(10, 60), "output"));
  private final JPanel kinds= new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
  private final JPanel shortcuts= new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
  private final JPanel attenuatorsBox= new JPanel();
  private final Collapsible attenuators= new Collapsible("Attenuators", half(attenuatorsBox), false);
  private final JPanel claimsBox= new JPanel();
  private final Collapsible claims= new Collapsible("Extensions", half(claimsBox), true);
  private final JPanel dataBox= new JPanel();
  private final Collapsible data= new Collapsible("Data", half(dataBox), false);
  private final JList<LogFiles.Entry> logList= new JList<>();
  private final Collapsible logs= new Collapsible("Logs", half(logList), false, small("Logs", this::openLogs));
  private final JButton openDocs= small("Open docs", this::openDocs);
  private final JButton action= named(small("Compile", this::act), "action");
  private final JButton deleteCache= small("Delete cache", () -> Main.command("DeleteCache", this.project.alias()));
  private final JButton forget= small("Forget", () -> Main.command("Forget", this.project.alias()));
  private final JLabel icon= new JLabel();
  private final JLabel name= new JLabel();
  private Object shown= List.of();
  private Project project;
  Panel(Consumer<String> refuse){
    this.refuse= refuse;
    output.setEditable(false);
    dataBox.setLayout(new BoxLayout(dataBox, BoxLayout.Y_AXIS));
    claimsBox.setLayout(new BoxLayout(claimsBox, BoxLayout.Y_AXIS));
    attenuatorsBox.setLayout(new BoxLayout(attenuatorsBox, BoxLayout.Y_AXIS));
    root.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
    root.setMinimumSize(new Dimension(0, 0));//let the split divider shrink this panel past its natural content width
    var header= new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    header.add(icon);
    name.setFont(name.getFont().deriveFont(Font.BOLD, 18f));
    header.add(name);
    header.add(action);
    header.add(openDocs);
    header.add(kinds);
    header.add(small("Browse", () -> Window.open(refuse, project.folder())));
    header.add(forget);
    header.add(deleteCache);
    var top= new JPanel();
    top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
    Stream.of(header, shortcuts).forEach(top::add);
    var areas= new JPanel();
    areas.setLayout(new BoxLayout(areas, BoxLayout.Y_AXIS));
    Stream.of(new Collapsible("Output", half(output), true, small("Clear", () -> output.setText(""))), attenuators, claims, data, logs).forEach(areas::add);
    areas.add(new JPanel());
    root.add(top, BorderLayout.NORTH);
    root.add(areas, BorderLayout.CENTER);
    root.addComponentListener(new ComponentAdapter(){
      @Override public void componentResized(ComponentEvent e){ root.revalidate(); }
    });
  }
  private JScrollPane half(Component view){
    return new JScrollPane(view){
      @Override public Dimension getPreferredSize(){
        var d= super.getPreferredSize();
        return new Dimension(d.width, Math.max(40, Math.min(d.height, root.getHeight()/2)));
      }
      @Override public Dimension getMaximumSize(){ return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }
    };
  }
  private void openDocs(){ openDocs(refuse, project.folder()); }
  static void openDocs(Consumer<String> refuse, Path folder){
    var gen= folder.resolve(Facts.outDir).resolve("gen_java");
    var docs= Fs.isDirectory(gen) ? Fs.walk(gen, s -> s.filter(p -> p.toString().endsWith(".html")).toList()) : List.<Path>of();
    if (docs.isEmpty()){ refuse.accept(Report.noDocs(gen)); return; }
    docs.forEach(OpenPath::open);
  }
  private void openLogs(){ Window.open(refuse, project.folder().resolve(LogFiles.runDir).resolve("logs")); }
  void append(String text){
    var bar= ((JScrollPane)output.getParent().getParent()).getVerticalScrollBar();
    var following= bar.getValue()+bar.getVisibleAmount() >= bar.getMaximum()-16;
    output.append(text);
    if (following){ output.setCaretPosition(output.getDocument().getLength()); }
    root.revalidate();
  }
  private void act(){
    var alias= project.alias();
    switch (project.action()){
      case check -> Main.command("Check", alias);
      case compile -> Main.command("Compile", alias);
      case terminate -> Main.command("Terminate", alias);
      case run -> run();
    }
  }
  private void run(){
    var mains= project.knownMains();
    if (mains.size() == 1){ Main.command("Run", project.alias(), mains.getFirst()); return; }
    var popup= new JPopupMenu();
    var boxes= mains.stream().map(m -> named(new JCheckBox(m, project.selectedMains().contains(m)), m)).toList();
    var run= named(small("Run", () -> { popup.setVisible(false); runTicked(boxes); }), "runTicked");
    Runnable enable= () -> run.setEnabled(boxes.stream().anyMatch(JCheckBox::isSelected));
    boxes.forEach(b -> b.addActionListener(_ -> enable.run()));
    enable.run();
    var list= new JPanel();
    list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
    boxes.forEach(list::add);
    list.add(run);
    popup.add(list);
    popup.show(action, 0, action.getHeight());
  }
  private void runTicked(List<JCheckBox> boxes){
    var ticked= boxes.stream().filter(JCheckBox::isSelected).map(JCheckBox::getText);
    Main.command("Run", Stream.concat(Stream.of(project.alias()), ticked).toArray(String[]::new));
  }
  void render(Project p, List<Project> all, int height){
    var claimed= Project.claimed(all);
    var stamps= claimed.values().stream().flatMap(List::stream).map(c -> c.icon(".png").toFile().lastModified()).toList();
    var next= List.of(p, all.stream().map(Project::entry).toList(), all.stream().map(Project::mains).toList(), claimed, stamps, height);
    if (next.equals(shown)){ return; }
    shown = next;
    project = p;
    icon.setIcon(new ImageIcon(Icons.of(p, iconSize).getScaledInstance(iconSize, iconSize, Image.SCALE_SMOOTH)));
    name.setText(p.alias());
    var a= p.action();
    var several= a == Project.Action.run && p.knownMains().size() > 1;
    action.setText(switch (a){ case check -> "Check"; case compile -> "Compile"; case run -> "Run"; case terminate -> "Terminate"; });
    action.setIcon(several ? new Arrow() : null);
    action.setHorizontalTextPosition(JButton.LEADING);
    action.setEnabled(a != Project.Action.run || !p.knownMains().isEmpty());
    openDocs.setEnabled(p.mains().isPresent());
    deleteCache.setVisible(p.kind() == Kind.code);
    deleteCache.setEnabled(!p.busy());
    forget.setEnabled(!p.busy());
    fillKinds(p);
    fillShortcuts(p, claimed);
    fillAttenuators(p);
    fillClaims(p, claimed);
    fillData(p, all);
    logs.setVisible(p.kind() == Kind.code);
    var log= logList.getSelectedValue();
    logList.setListData(p.facts().logs().toArray(LogFiles.Entry[]::new));
    logList.setSelectedValue(log, false);
    root.revalidate();
    root.repaint();
  }
  private void fillShortcuts(Project p, Map<String,List<Project.Claimant>> claimed){
    shortcuts.removeAll();
    var mine= mine(p, claimed).filter(Project.Claimant::shortcut).toList();
    shortcuts.setVisible(!mine.isEmpty());
    mine.forEach(c -> shortcuts.add(doubleClicked(named(shortcut(c), "shortcut "+c.main()+" "+c.claim().extension()), () -> Main.command("OpenWithOrShortcut", Main.path(c.shortcutFile())))));
  }
  private static JLabel shortcut(Project.Claimant c){
    return Tiles.desktop(new JLabel(), new ImageIcon(Icons.of(c, Tiles.iconSize)), c.shortcutFile().getFileName().toString());
  }
  private static Stream<Project.Claimant> mine(Project p, Map<String,List<Project.Claimant>> claimed){
    return claimed.values().stream().flatMap(List::stream).filter(c -> c.folder().equals(p.folder()));
  }
  private void fillClaims(Project p, Map<String,List<Project.Claimant>> claimed){
    claimsBox.removeAll();
    var opens= mine(p, claimed).filter(c -> !c.shortcut()).collect(Collectors.groupingBy(Project.Claimant::main, TreeMap::new, Collectors.toList()));
    claims.setVisible(!opens.isEmpty());
    opens.forEach((main,cs) -> fillClaims(main, cs, claimed));
  }
  private void fillClaims(String main, List<Project.Claimant> cs, Map<String,List<Project.Claimant>> claimed){
    claimsBox.add(row(0, new JLabel(main)));
    section("Extensions this main opens:", cs.stream().map(c -> row(16, label("."+c.claim().extension(), c))).toList());
    section("Conflicting extensions:", cs.stream().filter(c -> !others(c, claimed).isEmpty()).map(c -> conflict(c, others(c, claimed))).toList());
  }
  private static List<Project.Claimant> others(Project.Claimant c, Map<String,List<Project.Claimant>> claimed){ return claimed.get(c.claim().extension()).stream().filter(o -> !o.equals(c)).toList(); }
  private void section(String heading, List<JPanel> rows){
    if (rows.isEmpty()){ return; }
    claimsBox.add(row(0, new JLabel(heading)));
    rows.forEach(claimsBox::add);
  }
  private static JPanel row(int indent, Component... cs){
    var res= new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 1));
    res.setBorder(BorderFactory.createEmptyBorder(0, indent, 0, 0));
    Stream.of(cs).forEach(res::add);
    return res;
  }
  private static JLabel label(String text, Project.Claimant c){ return new JLabel(text, new ImageIcon(Icons.of(c, claimSize)), JLabel.LEADING); }
  private static JPanel conflict(Project.Claimant c, List<Project.Claimant> others){
    var res= row(16, new JLabel("."+c.claim().extension()+" is also opened by"));
    others.forEach(o -> res.add(label(o.label(), o)));
    return res;
  }
  private void fillAttenuators(Project p){
    attenuatorsBox.removeAll();
    attenuators.setVisible(p.kind() == Kind.code || !p.entry().attenuators().isEmpty());
    if (!attenuators.isVisible()){ return; }
    Stream.concat(p.knownMains().stream(), p.entry().attenuators().keySet().stream()).distinct().sorted().forEach(m -> fillAttenuators(p, m));
  }
  private void fillAttenuators(Project p, String main){
    var applied= p.entry().attenuators().getOrDefault(main, List.of());
    var known= p.mains().isPresent();
    attenuatorsBox.add(row(0, label(main, !known || p.knownMains().contains(main))));
    IntStream.range(0, applied.size()).forEach(i -> attenuatorsBox.add(attenuator(p, main, applied, i)));
    var choices= p.knownAttenuators().toArray(String[]::new);
    if (choices.length == 0){ return; }
    var combo= new JComboBox<>(choices);
    attenuatorsBox.add(row(16, combo, small("Add", () -> attenuate(p, main, Push.of(applied, (String)combo.getSelectedItem())))));
  }
  private static JPanel attenuator(Project p, String main, List<String> applied, int i){
    var a= applied.get(i);
    var known= p.mains().isEmpty() || p.knownAttenuators().contains(a);
    var up= small("Up", () -> attenuate(p, main, moved(applied, i, i-1)));
    up.setEnabled(i > 0);
    var down= small("Down", () -> attenuate(p, main, moved(applied, i, i+1)));
    down.setEnabled(i < applied.size()-1);
    return row(16, label(a, known), small("Remove", () -> attenuate(p, main, moved(applied, i, -1))), up, down);
  }
  private static JLabel label(String text, boolean known){ return known ? new JLabel(text) : new JLabel(text, new Icons.Badge(null, 16, Project.State.codeInvalid), JLabel.LEADING); }
  private static List<String> moved(List<String> applied, int from, int to){
    var res= new ArrayList<>(applied);
    res.remove(from);
    if (to >= 0){ res.add(to, applied.get(from)); }
    return res;
  }
  private static void attenuate(Project p, String main, List<String> applied){
    Main.command("Attenuate", Stream.concat(Stream.of(p.alias(), main), applied.stream()).toArray(String[]::new));
  }
  private void fillKinds(Project p){
    kinds.removeAll();
    if (p.kind() != Kind.idle){ kinds.add(kindButton(p, "Back to idle", Kind.idle)); }
    if (p.kind() != Kind.dataRead){ kinds.add(kindButton(p, "Become dataRead", Kind.dataRead)); }
    if (p.kind() != Kind.dataEdit){ kinds.add(kindButton(p, "Become dataEdit", Kind.dataEdit)); }
    if (p.kind() != Kind.code){ kinds.add(kindButton(p, "Become code", Kind.code)); }
  }
  private JButton kindButton(Project p, String text, Kind target){
    var res= named(small(text, () -> Main.command("Kind", p.alias(), target.text)), target.text);
    res.setEnabled(!p.busy());
    return res;
  }
  private void fillData(Project p, List<Project> all){
    dataBox.removeAll();
    data.setVisible(p.kind() == Kind.code || p.kind().isData() || !p.entry().mounts().isEmpty());
    if (!data.isVisible()){ return; }
    Stream.concat(p.knownDataRead().stream(), p.entry().mounts().keySet().stream()).distinct().sorted().forEach(t -> fillMount(p, t, all));
    if (!p.kind().isData()){ return; }
    dataBox.add(row(0, new JLabel("Mounted on:")));
    all.forEach(o -> o.entry().mounts().forEach((t, d) -> { if (d.equals(p.alias())){ dataBox.add(row(16, new JLabel(o.alias()+"::"+t))); } }));
    var free= all.stream().filter(o -> o.kind() == Kind.code).flatMap(o -> o.knownDataRead().stream().filter(t -> !o.entry().mounts().containsKey(t) && fits(o, t, p.kind())).map(t -> o.alias()+"::"+t)).toArray(String[]::new);
    if (free.length == 0){ return; }
    var combo= new JComboBox<>(free);
    dataBox.add(row(16, combo, small("Mount", () -> mountFree(((String)combo.getSelectedItem()).split("::"), p.alias()))));
  }
  private void fillMount(Project p, String type, List<Project> all){
    var mounted= p.entry().mounts().get(type);
    var remove= small("Remove", () -> Main.command("Mount", p.alias(), type));
    dataBox.add(mounted == null ? row(0, new JLabel(type), new JLabel("no mount")) : row(0, new JLabel(type), new JLabel("mounted: "+mounted), remove));
    var broken= mounted == null || p.mains().isEmpty() ? Optional.<String>empty() : p.brokenMount(type, all.stream().map(Project::entry).toList());
    broken.ifPresent(why -> dataBox.add(row(16, label(Report.brokenMount(type, mounted, why), false))));
    if (!p.knownDataRead().contains(type)){ return; }
    var choices= all.stream().filter(o -> fits(p, type, o.kind()) && !o.alias().equals(mounted)).map(Project::alias).toArray(String[]::new);
    if (choices.length == 0){ return; }
    var combo= new JComboBox<>(choices);
    dataBox.add(row(16, combo, small("Mount", () -> Main.command("Mount", p.alias(), type, (String)combo.getSelectedItem()))));
  }
  private static boolean fits(Project code, String type, Kind data){ return data == Kind.dataEdit || data == Kind.dataRead && !code.mains().orElseThrow().dataEdit().contains(type); }
  private static void mountFree(String[] codeAndType, String data){ Main.command("Mount", codeAndType[0], codeAndType[1], data); }
  private static final class Arrow implements Icon{
    @Override public int getIconWidth(){ return 10; }
    @Override public int getIconHeight(){ return 6; }
    @Override public void paintIcon(Component c, Graphics g, int x, int y){
      g.setColor(c.isEnabled() ? Color.darkGray : Color.lightGray);
      g.fillPolygon(new int[]{x, x+10, x+5}, new int[]{y, y, y+6}, 3);
    }
  }
}

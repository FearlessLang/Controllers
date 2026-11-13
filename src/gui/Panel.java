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
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.regex.Pattern;
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
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import controller.Deployed;
import controller.Facts;
import controller.Main;
import controller.TaggedText;
import controller.Project;
import controller.Registry.Kind;
import fileSupport.LogFiles;
import managerMessages.Report;
import tools.Fs;
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
  private final JPanel shortcuts= new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
  private final JPanel attenuatorsBox= new JPanel();
  private final Collapsible attenuators= new Collapsible("Attenuators", half(attenuatorsBox), false);
  private final JPanel claimsBox= new JPanel();
  private final Collapsible claims= new Collapsible("Extensions", half(claimsBox), true);
  private final JPanel dataBox= new JPanel();
  private final Collapsible data= new Collapsible("Data", half(dataBox), false);
  static final Color passed= new Color(0x2E7D32);
  static final Color failed= new Color(0xC62828);
  static final Color disabled= Color.GRAY;
  /// a row of the Tests tree: its text, and its mark (none for a line of a failure)
  record TestRow(String text, Optional<Color> mark){ @Override public String toString(){ return text; } }
  final JTree testTree= named(new JTree(new DefaultMutableTreeNode(new TestRow("", Optional.empty()))), "tests");
  private final Collapsible tests= new Collapsible("Tests", half(testTree), true);
  private Object shownTests= List.of();
  private static final Pattern testCase= Pattern.compile("(?s)<testcase classname=\"[^\"]*\" name=\"([^\"]*)\"[^>]*>(.*?)</testcase>");
  private final JList<LogFiles.Entry> logList= new JList<>();
  private final Collapsible logs= new Collapsible("Logs", half(logList), false, small("Logs", this::openLogs));
  private final JButton openDocs= small("Docs", this::openDocs);
  private final JButton kind= named(small("Kind", this::kinds), "kind");
  private final JButton action= named(small("Compile", this::act), "action");
  private final JButton projectMenu= named(small("Project", this::projectMenu), "project");
  private final JPanel selectedMains= new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
  private final JLabel icon= new JLabel();
  private final JLabel name= new JLabel();
  private final JLabel status= named(new JLabel(), "status");
  private Object shown= List.of();
  private Project project;
  private List<Project> all= List.of();
  Panel(Consumer<String> refuse){
    this.refuse= refuse;
    output.setEditable(false);
    testTree.setRootVisible(false);
    testTree.setShowsRootHandles(true);
    testTree.setCellRenderer(new TestMarks());
    output.setDropTarget(null);
    dataBox.setLayout(new BoxLayout(dataBox, BoxLayout.Y_AXIS));
    claimsBox.setLayout(new BoxLayout(claimsBox, BoxLayout.Y_AXIS));
    attenuatorsBox.setLayout(new BoxLayout(attenuatorsBox, BoxLayout.Y_AXIS));
    root.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
    root.setMinimumSize(new Dimension(0, 0));//let the split divider shrink this panel past its natural content width
    var header= new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
    name.setFont(name.getFont().deriveFont(Font.BOLD, 15f));
    header.add(name);
    header.add(action);
    header.add(openDocs);
    header.add(kind);
    Stream.of(openDocs, kind, projectMenu).forEach(b -> { b.setIcon(new Arrow()); b.setHorizontalTextPosition(JButton.LEADING); });
    header.add(projectMenu);
    var top= new JPanel();
    top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
    var statusLine= new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
    statusLine.add(icon);
    statusLine.add(status);
    Stream.of(header, statusLine, selectedMains, shortcuts).forEach(top::add);
    var areas= new JPanel();
    areas.setLayout(new BoxLayout(areas, BoxLayout.Y_AXIS));
    Stream.of(new Collapsible("Output", half(output), true, small("Clear", () -> output.setText(""))), tests, attenuators, claims, data, logs).forEach(areas::add);
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
        var wide= getViewport().getWidth() > 0 && view.getPreferredSize().width > getViewport().getWidth();
        var bar= wide ? getHorizontalScrollBar().getPreferredSize().height : 0;
        var content= view.getPreferredSize().height+getInsets().top+getInsets().bottom;
        return new Dimension(d.width, Math.max(40, Math.min(content+bar, root.getHeight()/2)));
      }
      @Override public Dimension getMaximumSize(){ return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }
    };
  }
  private void openDocs(){
    var menu= new JPopupMenu();
    var f= project.folder();
    project.facts().packages().forEach(pkg -> menu.add(Window.item(pkg, () -> Window.open(refuse, Deployed.coordinator(f).docs(f, pkg)))));
    menu.show(openDocs, 0, openDocs.getHeight());
  }
  private void projectMenu(){
    var menu= new JPopupMenu();
    var alias= project.alias();
    menu.add(Window.item("Browse", () -> Window.open(refuse, project.folder())));
    Stream.of(named(Window.item("Forget", () -> Main.command("Forget "+alias)), "forget"), named(Window.item("Delete cache", () -> Main.command("DeleteCache "+alias)), "deleteCache"))
      .forEach(i -> { i.setEnabled(!project.busy()); menu.add(i); });
    var fix= named(Window.item("Fix", () -> fix(alias)), "fix");
    fix.setEnabled(project.usedBy(all).isEmpty());
    menu.add(fix);
    menu.show(projectMenu, 0, projectMenu.getHeight());
  }
  private void fix(String alias){ if (Window.confirm(root, Report.fixQuestion(alias, project.folder()), "Fix")){ Main.command("Fix "+alias); } }
  /// the state in words, then what is running, the first line of the 'problem', and the broken mounts
  static String status(Project p, List<Project> all){
    var running= p.job() instanceof Project.Job.Main(var main) ? ", running "+main : ", compiling";
    var detail= p.busy() ? running : p.problem().map(pr -> ": "+pr.lines().findFirst().orElse("")).orElse("");
    var broken= p.entry().mounts().keySet().stream().filter(t -> p.brokenMount(t, all).isPresent()).map(t -> "; the mount on "+t+" is broken (see Data)").collect(Collectors.joining());
    return p.state().text+detail+broken;
  }
  private void kinds(){
    var menu= new JPopupMenu();
    var alias= project.alias();
    List.of(Kind.idle, Kind.dataRead, Kind.dataEdit, Kind.code).stream().filter(k -> k != project.kind())
      .forEach(k -> menu.add(named(Window.item(k == Kind.idle ? "Back to idle" : "Become "+k.name(), () -> Main.command("Kind "+alias+" "+k.name())), k.name())));
    menu.show(kind, 0, kind.getHeight());
  }
  /// one node per unit test log, named after the main of the Run that wrote it; the nodes with a failure start expanded
  private void showTests(Project p){
    var root= new DefaultMutableTreeNode(new TestRow("", Optional.empty()));
    p.facts().tests().forEach(s -> root.add(suiteNode(s, p.ran())));
    ((DefaultTreeModel)testTree.getModel()).setRoot(root);
    Collections.list(root.depthFirstEnumeration()).stream().map(n -> (DefaultMutableTreeNode)n).filter(n -> !n.isLeaf() && mark(n) == failed).forEach(n -> testTree.expandPath(new TreePath(n.getPath())));
  }
  private static DefaultMutableTreeNode suiteNode(Facts.Suite s, List<Project.Ran> ran){
    var cases= testCase.matcher(s.xml()).results().map(r -> caseNode(r.group(1), r.group(2))).toList();
    var bad= cases.stream().filter(c -> mark(c) == failed).count();
    var off= cases.stream().filter(c -> mark(c) == disabled).count();
    var main= ran.stream().filter(r -> !r.since().isAfter(s.when())).reduce((_, b) -> b).map(Project.Ran::main).orElse("unit test log of "+s.when());
    var res= new DefaultMutableTreeNode(new TestRow(main+": "+(cases.size()-bad-off)+" passed, "+bad+" failed, "+off+" disabled", Optional.of(bad > 0 ? failed : passed)));
    cases.forEach(res::add);
    return res;
  }
  private static DefaultMutableTreeNode caseNode(String name, String body){
    if (body.contains("<skipped/>")){ return new DefaultMutableTreeNode(new TestRow(xmlText(name), Optional.of(disabled))); }
    if (!body.contains("<failure>")){ return new DefaultMutableTreeNode(new TestRow(xmlText(name), Optional.of(passed))); }
    var res= new DefaultMutableTreeNode(new TestRow(xmlText(name), Optional.of(failed)));
    var failure= body.substring(body.indexOf("<failure>")+"<failure>".length()).replaceFirst("(?s)</failure>.*", "").replaceAll("(?s)<expected>.*?</expected>|<actual>.*?</actual>", "").strip();
    xmlText(failure).lines().forEach(l -> res.add(new DefaultMutableTreeNode(new TestRow(l, Optional.empty()))));
    return res;
  }
  private static Color mark(DefaultMutableTreeNode n){ return ((TestRow)n.getUserObject()).mark().orElse(null); }
  @SuppressWarnings("serial")
  private static final class TestMarks extends DefaultTreeCellRenderer{
    @Override public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean focus){
      super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, focus);
      setIcon(((TestRow)((DefaultMutableTreeNode)value).getUserObject()).mark().map(Dot::new).orElse(null));
      return this;
    }
  }
  private record Dot(Color color) implements Icon{
    @Override public int getIconWidth(){ return 10; }
    @Override public int getIconHeight(){ return 10; }
    @Override public void paintIcon(Component c, Graphics g, int x, int y){ g.setColor(color); g.fillOval(x, y, 10, 10); }
  }
  private static String xmlText(String s){ return s.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&amp;", "&"); }
  private void openLogs(){ Window.open(refuse, project.folder().resolve(LogFiles.runDir).resolve("logs")); }
  void append(String text){
    var bar= ((JScrollPane)output.getParent().getParent()).getVerticalScrollBar();
    var following= bar.getValue()+bar.getVisibleAmount() >= bar.getMaximum()-16;
    output.append(text);
    if (following){ output.setCaretPosition(output.getDocument().getLength()); }
    root.revalidate();
  }
  private void act(){
    if (project.action() == Project.Action.Run){ run(); return; }
    Main.command(project.action()+" "+project.alias());
  }
  private void run(){
    var alias= project.alias();
    var mains= project.knownMains();
    if (mains.size() == 1){ Main.command("SelectMains "+alias+" "+mains.getFirst(), "Run "+alias); return; }
    var popup= new JPopupMenu();
    var ticked= new ArrayList<>(project.entry().mains());
    var entries= Stream.concat(mains.stream(), ticked.stream()).distinct().toList();
    var boxes= entries.stream().map(m -> named(new JCheckBox(m, ticked.contains(m)), m)).toList();
    var run= named(small("Run", () -> { popup.setVisible(false); Main.command("Run "+alias); }), "runTicked");
    Runnable enable= () -> run.setEnabled(!ticked.isEmpty() && mains.containsAll(ticked));
    boxes.forEach(b -> b.addActionListener(_ -> tick(alias, ticked, b, enable)));
    enable.run();
    var list= new JPanel();
    list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
    boxes.forEach(list::add);
    list.add(run);
    popup.add(list);
    popup.show(action, 0, action.getHeight());
  }
  /// Checking or unchecking an entry is a SelectMains naming the checked entries, in the order they were checked.
  private static void tick(String alias, ArrayList<String> ticked, JCheckBox box, Runnable enable){
    ticked.remove(box.getText());
    if (box.isSelected()){ ticked.add(box.getText()); }
    enable.run();
    Main.command(ticked.stream().map(m -> " "+m).collect(Collectors.joining("", "SelectMains "+alias, "")));
  }
  void render(Project p, List<Project> all, int height){
    var claimed= Project.claimed(all);
    var stamps= claimed.values().stream().flatMap(List::stream).map(c -> c.png().toFile().lastModified()).toList();
    var next= List.of(p, all, stamps, height);
    if (next.equals(shown)){ return; }
    shown = next;
    project = p;
    this.all = all;
    icon.setIcon(new ImageIcon(Icons.of(p, iconSize).getScaledInstance(iconSize, iconSize, Image.SCALE_SMOOTH)));
    name.setText(p.alias());
    status.setText(status(p, all));
    var a= p.action();
    var several= a == Project.Action.Run && p.knownMains().size() > 1;
    action.setText(a.name());
    action.setIcon(several ? new Arrow() : null);
    action.setHorizontalTextPosition(JButton.LEADING);
    action.setEnabled(!(p.job() instanceof Project.Job.Compile) && (a != Project.Action.Run || !p.knownMains().isEmpty()));
    openDocs.setEnabled(p.mains().isPresent());
    kind.setText("Kind: "+p.kind().name());
    kind.setEnabled(!p.busy());
    fillSelected(p, several);
    fillShortcuts(p, claimed);
    fillAttenuators(p);
    fillClaims(p, claimed);
    fillData(p, all);
    logs.setVisible(p.kind() == Kind.code);
    tests.setVisible(p.kind() == Kind.code);
    var testKey= List.of(p.facts().tests(), p.ran());
    if (!testKey.equals(shownTests)){ shownTests = testKey; showTests(p); }
    var log= logList.getSelectedValue();
    logList.setListData(p.facts().logs().toArray(LogFiles.Entry[]::new));
    logList.setSelectedValue(log, false);
    root.revalidate();
    root.repaint();
  }
  private void fillSelected(Project p, boolean runV){
    selectedMains.removeAll();
    var mains= p.entry().mains();
    selectedMains.setVisible(!runV && !mains.isEmpty());
    if (!selectedMains.isVisible()){ return; }
    selectedMains.add(new JLabel("Selected mains: "+String.join(", ", mains)));
    selectedMains.add(small("Remove", () -> Main.command("SelectMains "+p.alias())));
  }
  private void fillShortcuts(Project p, Map<String,List<Project.Claimant>> claimed){
    shortcuts.removeAll();
    mine(p, claimed).filter(Project.Claimant::shortcut).forEach(c -> c.namedShortcutFile().filter(f -> Files.isRegularFile(f, LinkOption.NOFOLLOW_LINKS)).ifPresent(f -> shortcuts.add(shortcut(c, f))));
    shortcuts.setVisible(shortcuts.getComponentCount() > 0);
  }
  private static JLabel shortcut(Project.Claimant c, Path file){
    var tile= Tiles.desktop(new JLabel(), new ImageIcon(Icons.of(c, Tiles.iconSize)), file.getFileName().toString());
    return doubleClicked(named(tile, "shortcut "+c.main()+" "+c.claim().extension()), () -> Main.command("OpenWithOrShortcut "+TaggedText.base16(file)));
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
    Main.command(applied.stream().map(a -> " "+a).collect(Collectors.joining("", "Attenuate "+p.alias()+" "+main, "")));
  }
  private void fillData(Project p, List<Project> all){
    dataBox.removeAll();
    data.setVisible(p.kind() == Kind.code || p.kind().isData() || !p.entry().mounts().isEmpty());
    if (!data.isVisible()){ return; }
    Stream.concat(p.knownDataRead().stream(), p.entry().mounts().keySet().stream()).distinct().sorted().forEach(t -> fillMount(p, t, all));
    if (!p.kind().isData()){ return; }
    var mounted= all.stream().flatMap(o -> o.entry().mounts().entrySet().stream().filter(m -> m.getValue().equals(p.alias())).map(m -> o.alias()+"::"+m.getKey())).toList();
    dataBox.add(row(0, new JLabel(mounted.isEmpty() ? "Mounted on nothing" : "Mounted on:")));
    mounted.forEach(m -> dataBox.add(row(16, new JLabel(m))));
    var free= all.stream().filter(o -> o.kind() == Kind.code).flatMap(o -> o.knownDataRead().stream().filter(t -> !o.entry().mounts().containsKey(t) && fits(o, t, p.kind())).map(t -> o.alias()+"::"+t)).toArray(String[]::new);
    if (free.length == 0 || p.problem().isPresent()){ return; }
    var combo= new JComboBox<>(free);
    dataBox.add(row(0, new JLabel("Mount on"), combo, small("Mount", () -> mountFree(((String)combo.getSelectedItem()).split("::"), p.alias()))));
  }
  private void fillMount(Project p, String type, List<Project> all){
    var mounted= p.entry().mounts().get(type);
    var remove= small("Remove", () -> Main.command("Unmount "+p.alias()+" "+type));
    dataBox.add(mounted == null ? row(0, new JLabel(type), new JLabel("no mount")) : row(0, new JLabel(type), new JLabel("mounted: "+mounted), remove));
    var broken= mounted == null ? Optional.<String>empty() : p.brokenMount(type, all);
    broken.ifPresent(why -> dataBox.add(row(16, label(Report.brokenMount(type, mounted, why).replace('\n', ' '), false))));
    if (!p.knownDataRead().contains(type)){ return; }
    var choices= all.stream().filter(o -> fits(p, type, o.kind()) && o.problem().isEmpty() && !o.alias().equals(mounted)).map(Project::alias).toArray(String[]::new);
    if (choices.length == 0){ return; }
    var combo= new JComboBox<>(choices);
    dataBox.add(row(16, combo, small("Mount", () -> Main.command("Mount "+p.alias()+" "+type+" "+combo.getSelectedItem()))));
  }
  private static boolean fits(Project code, String type, Kind data){ return data == Kind.dataEdit || data == Kind.dataRead && !code.mains().orElseThrow().dataEdit().contains(type); }
  private static void mountFree(String[] codeAndType, String data){ Main.command("Mount "+codeAndType[0]+" "+codeAndType[1]+" "+data); }
  private static final class Arrow implements Icon{
    @Override public int getIconWidth(){ return 10; }
    @Override public int getIconHeight(){ return 6; }
    @Override public void paintIcon(Component c, Graphics g, int x, int y){
      g.setColor(c.isEnabled() ? Color.darkGray : Color.lightGray);
      g.fillPolygon(new int[]{x, x+10, x+5}, new int[]{y, y, y+6}, 3);
    }
  }
}

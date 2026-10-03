package gui;

import static gui.Window.mono;
import static gui.Window.named;
import static gui.Window.small;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import controller.Facts;
import controller.Project;
import controller.Registry.Kind;
import fileSupport.LogFiles;
import tools.Fs;
import tools.OpenPath;

/// The right half of the window: everything about one registered project, and the
/// buttons that act on it. One Panel per project, kept while the project stays registered:
/// it holds the Output of the project.
final class Panel{
  interface Requests{ void ask(String verb, String name, String arg); }
  private record Link(JTextField field, String was){ boolean typed(){ return !field.getText().strip().equals(was); } }
  private static final int iconSize= 32;
  private static final int claimSize= 20;
  private final Requests requests;
  private final Consumer<String> refuse;
  final JPanel root= new JPanel(new BorderLayout(8,8));
  final JTextArea output= mono(named(new JTextArea(10,60),"output"));
  private final JScrollPane outputScroll= new JScrollPane(output);
  private final JButton clearOutput= small("Clear output",this::clearOutput);
  private final JTextArea details= mono(named(new JTextArea(9,40),"details"));
  private final JPanel kinds= new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));
  private final JPanel mainsBox= named(new JPanel(),"mains");
  private final JScrollPane mainsScroll= new JScrollPane(mainsBox);
  private final JPanel mainsPanel= new JPanel(new BorderLayout());
  private final JPanel pick= new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));
  private final JPanel claimsBox= new JPanel();
  private final JScrollPane claimsScroll= named(new JScrollPane(claimsBox),"claims");
  private final JPanel linksBox= new JPanel();
  private final Collapsible links= new Collapsible("Links",new JScrollPane(linksBox),false);
  private final Collapsible information= new Collapsible("Information",new JScrollPane(details),true);
  private final JButton openDocs= small("Open docs",this::openDocs);
  private final JButton action= named(new JButton("Compile"),"action");
  private final JLabel icon= new JLabel();
  private final JLabel name= new JLabel();
  private final JList<LogFiles.Entry> logList= new JList<>();
  private final JButton viewLog= small("View",this::viewLog);
  private final JButton copyLog= small("Copy",this::copyLog);
  private final JButton deleteLog= small("Delete",this::deleteLog);
  private Object shown= List.of();
  private HashMap<String,Link> linkFields= new HashMap<>();
  private Project project;
  Panel(Requests requests, Consumer<String> refuse){
    this.requests= requests;
    this.refuse= refuse;
    output.setEditable(false);
    details.setEditable(false);
    mainsBox.setLayout(new BoxLayout(mainsBox,BoxLayout.Y_AXIS));
    linksBox.setLayout(new BoxLayout(linksBox,BoxLayout.Y_AXIS));
    claimsBox.setLayout(new BoxLayout(claimsBox,BoxLayout.Y_AXIS));
    claimsScroll.setBorder(BorderFactory.createEtchedBorder());
    claimsScroll.getVerticalScrollBar().setUnitIncrement(16);
    pick.add(named(small("All",()->requests.ask("mains",project.alias(),String.join(" ",project.knownMains()))),"all"));
    pick.add(named(small("None",()->requests.ask("mains",project.alias(),"")),"none"));
    mainsPanel.add(pick,BorderLayout.NORTH);
    mainsPanel.add(mainsScroll,BorderLayout.CENTER);
    mainsPanel.setBorder(BorderFactory.createEtchedBorder());
    var logScroll= new JScrollPane(logList);
    logScroll.setPreferredSize(new Dimension(0,140));
    logList.addListSelectionListener(_->updateLogButtons());
    outputScroll.setBorder(BorderFactory.createTitledBorder("Output"));
    action.addActionListener(_->act());
    root.setBorder(BorderFactory.createEmptyBorder(4,8,4,8));
    root.setMinimumSize(new Dimension(0,0));//let the split divider shrink this panel past its natural content width
    var header= new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));
    header.add(icon);
    header.add(action);
    name.setFont(name.getFont().deriveFont(Font.BOLD,18f));
    header.add(name);
    header.add(openDocs);
    var top= new JPanel();
    top.setLayout(new BoxLayout(top,BoxLayout.Y_AXIS));
    top.add(header);
    top.add(kinds);
    top.add(mainsPanel);
    top.add(claimsScroll);
    top.add(links);
    top.add(information);
    top.add(new Collapsible("Logs",logScroll,false,viewLog,copyLog,deleteLog));
    root.add(top,BorderLayout.NORTH);
    var bar= new JPanel(new FlowLayout(FlowLayout.RIGHT,0,0));
    bar.add(clearOutput);
    var outputPanel= new JPanel(new BorderLayout());
    outputPanel.add(bar,BorderLayout.NORTH);
    outputPanel.add(outputScroll,BorderLayout.CENTER);
    root.add(outputPanel,BorderLayout.CENTER);
  }
  private void openDocs(){ openDocs(refuse,project.folder()); }
  private void clearOutput(){ requests.ask("clear",project.alias(),""); }
  void append(String text){
    var bar= outputScroll.getVerticalScrollBar();
    var following= bar.getValue()+bar.getVisibleAmount() >= bar.getMaximum()-16;
    output.append(text);
    if (following){ output.setCaretPosition(output.getDocument().getLength()); }
  }
  private void act(){
    information.setOpen(false);
    links.setOpen(false);
    requests.ask(project.action().verb(),project.alias(),"");
  }
  void render(Project p, List<Project> all){
    var claimed= Project.claimed(all);
    var stamps= claimed.values().stream().flatMap(List::stream).map(c->c.icon(".png").toFile().lastModified()).toList();
    var next= List.of(p,all.stream().map(Project::entry).toList(),claimed,stamps);
    if (next.equals(shown)){ return; }
    shown= next;
    project= p;
    icon.setIcon(new ImageIcon(Icons.of(p,iconSize).getScaledInstance(iconSize,iconSize,Image.SCALE_SMOOTH)));
    name.setText(p.alias());
    var a= p.action();
    action.setText(a.text());
    action.setEnabled(a.enabled());
    openDocs.setEnabled(p.mains().isPresent());
    details.setText(p.information());
    details.setCaretPosition(0);
    fillKinds(p);
    fillMains(p);
    fillClaims(p,claimed);
    fillLinks(p,all);
    var log= logList.getSelectedValue();
    logList.setListData(p.facts().logs().toArray(LogFiles.Entry[]::new));
    logList.setSelectedValue(log,false);
    updateLogButtons();
    root.revalidate();
    root.repaint();
  }
  private void updateLogButtons(){
    var has= logList.getSelectedValue() != null;
    viewLog.setEnabled(has);
    copyLog.setEnabled(has);
    deleteLog.setEnabled(has);
  }
  private void viewLog(){
    var sel= logList.getSelectedValue();
    Window.onFiles(refuse,"The log is not shown",()->Window.showText(root,Fs.readUtf8(sel.path()),sel.path().getFileName().toString(),JOptionPane.PLAIN_MESSAGE));
  }
  private void copyLog(){ Window.onFiles(refuse,"The log is not copied",()->copy(Fs.readUtf8(logList.getSelectedValue().path()))); }
  private static void copy(String text){
    var selection= new StringSelection(text);
    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection,selection);
  }
  private void deleteLog(){
    var sel= logList.getSelectedValue();
    if (JOptionPane.showConfirmDialog(root,"Delete "+sel.path().getFileName()+"?","Fearless",JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION){ return; }
    Window.onFiles(refuse,"The log is not deleted",()->Fs.rmTree(sel.path()));
  }
  //For a code project: what it can run. Unknown until compiled, a single main needs
  //no choice, and several mains are picked one by one or with All and None.
  private void fillMains(Project p){
    mainsBox.removeAll();
    mainsScroll.setPreferredSize(null);
    mainsPanel.setVisible(p.kind() == Kind.code);
    var known= p.knownMains();
    pick.setVisible(known.size() > 1);
    if (p.mains().isEmpty()){ mainsBox.add(new JLabel(p.problem().isPresent() ? "<invalid: see Error report>" : "<needs compiling>")); return; }
    if (known.size() == 1){ mainsBox.add(new JLabel(known.getFirst())); return; }
    for(var main: known){
      var box= named(new JCheckBox(main,p.selectedMains().contains(main)),main);
      box.addActionListener(_->requests.ask("mains",p.alias(),ticked()));
      mainsBox.add(box);
    }
    mainsScroll.setPreferredSize(new Dimension(0,Math.min(3,known.size())*26+8));
  }
  private void fillClaims(Project p, Map<String,List<Project.Claimant>> claimed){
    claimsBox.removeAll();
    var mine= claimed.values().stream().flatMap(List::stream).filter(c->c.folder().equals(p.folder()))
      .collect(Collectors.groupingBy(Project.Claimant::main,TreeMap::new,Collectors.toList()));
    claimsScroll.setVisible(!mine.isEmpty());
    mine.forEach((main,cs)->fillClaims(main,cs,claimed));
    claimsScroll.setPreferredSize(new Dimension(0,Math.min(240,claimsBox.getPreferredSize().height+4)));
  }
  private void fillClaims(String main, List<Project.Claimant> cs, Map<String,List<Project.Claimant>> claimed){
    claimsBox.add(row(new JLabel(main)));
    section("Extensions this main opens:",cs.stream().filter(c->others(c,claimed).isEmpty()).map(Panel::extension).toList());
    section("Conflicting extensions:",cs.stream().filter(c->!others(c,claimed).isEmpty()).map(c->conflict(c,others(c,claimed))).toList());
    section("Shortcuts (double click to run):",cs.stream().filter(Project.Claimant::shortcut).map(this::shortcut).toList());
  }
  private static List<Project.Claimant> others(Project.Claimant c, Map<String,List<Project.Claimant>> claimed){ return claimed.get(c.claim().extension()).stream().filter(o->!o.equals(c)).toList(); }
  private void section(String heading, List<? extends Component> items){
    if (items.isEmpty()){ return; }
    claimsBox.add(row(new JLabel(heading)));
    items.forEach(i->claimsBox.add(indented(row(i))));
  }
  private static JPanel row(Component... cs){
    var res= new JPanel(new FlowLayout(FlowLayout.LEFT,6,1));
    Stream.of(cs).forEach(res::add);
    return res;
  }
  private static JPanel indented(JPanel row){
    row.setBorder(BorderFactory.createEmptyBorder(0,16,0,0));
    return row;
  }
  private static JLabel extension(Project.Claimant c){ return new JLabel("."+c.claim().extension(),new ImageIcon(Icons.of(c,claimSize)),JLabel.LEADING); }
  private static JPanel conflict(Project.Claimant c, List<Project.Claimant> others){
    var first= others.getFirst();
    var res= row(extension(c),new JLabel("also claimed by"),new JLabel(first.label(),new ImageIcon(Icons.of(first,claimSize)),JLabel.LEADING));
    if (others.size() > 1){ res.add(new JLabel("and "+(others.size()-1)+" more")); }
    return res;
  }
  private JLabel shortcut(Project.Claimant c){
    var res= named(new JLabel(new ImageIcon(Icons.of(c,iconSize))),"shortcut "+c.main()+" "+c.claim().extension());
    res.addMouseListener(new MouseAdapter(){
      @Override public void mouseClicked(MouseEvent e){ if (e.getClickCount() == 2){ requests.ask("run",c.alias(),c.main()); } }
    });
    return res;
  }
  private String ticked(){ return String.join(" ",Stream.of(mainsBox.getComponents()).map(c->(JCheckBox)c).filter(JCheckBox::isSelected).map(JCheckBox::getText).toList()); }
  private void fillKinds(Project p){
    kinds.removeAll();
    if (p.kind() != Kind.idle){ kinds.add(kindButton(p,"Back to idle",Kind.idle)); return; }
    kinds.add(kindButton(p,"Become data",Kind.dataReadOnly));
    kinds.add(kindButton(p,"Become editable data",Kind.dataReadWrite));
    kinds.add(kindButton(p,"Become code",Kind.code));
  }
  private JButton kindButton(Project p, String text, Kind target){
    var res= named(small(text,()->requests.ask("kind",p.alias(),target.text)),target.text);
    res.setEnabled(!p.busy());
    return res;
  }
  private void fillLinks(Project p, List<Project> all){
    var iAmCode= p.kind() == Kind.code;
    var old= linkFields;
    var owner= KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    linkFields= new HashMap<>();
    linksBox.removeAll();
    links.setVisible(iAmCode || p.kind().isData());
    if (!links.isVisible()){ return; }
    linksBox.add(new JLabel(iAmCode ? "Data projects this code project reads or edits:" : "Code projects that may read or edit this:"));
    if (!iAmCode){ all.stream().filter(o->o.kind() == Kind.code).sorted(Comparator.comparing(Project::alias)).forEach(o->linksBox.add(linkRow(o,p.alias(),p.kind() == Kind.dataReadWrite,o.alias()))); }
    if (iAmCode){
      var linked= Stream.concat(p.entry().reads().keySet().stream(),p.entry().edits().keySet().stream());
      Stream.concat(all.stream().filter(o->o.kind().isData()).map(Project::alias),linked).distinct().sorted()
        .forEach(a->linksBox.add(linkRow(p,a,all.stream().anyMatch(o->o.alias().equals(a) && o.kind() == Kind.dataReadWrite),a)));
    }
    old.values().forEach(l->carry(l,owner));
  }
  private void carry(Link from, Component owner){
    var to= linkFields.get(from.field().getName());
    if (to == null){ return; }
    if (from.typed()){ to.field().setText(from.field().getText()); }
    to.field().setCaretPosition(Math.min(from.field().getCaretPosition(),to.field().getText().length()));
    if (from.field() == owner){ to.field().requestFocusInWindow(); }
  }
  private JPanel linkRow(Project code, String data, boolean writable, String other){
    var row= new JPanel(new FlowLayout(FlowLayout.LEFT,6,0));
    row.add(new JLabel(other));
    row.add(linkField(code,data,other,"read",code.entry().reads()));
    if (writable || code.entry().edits().containsKey(data)){ row.add(linkField(code,data,other,"write",code.entry().edits())); }
    return row;
  }
  private JPanel linkField(Project code, String data, String other, String how, Map<String,List<String>> links){
    var was= String.join(" ",links.getOrDefault(data,List.of()));
    var field= named(new JTextField(was,14),other+" "+how);
    var link= new Link(field,was);
    linkFields.put(field.getName(),link);
    field.addActionListener(_->field.transferFocus());
    field.addFocusListener(new FocusAdapter(){
      @Override public void focusLost(FocusEvent e){ if (linkFields.get(field.getName()) == link && link.typed()){ requests.ask("link",code.alias(),data+" "+how+" "+field.getText()); } }
    });
    var res= new JPanel(new FlowLayout(FlowLayout.LEFT,2,0));
    res.add(new JLabel(how));
    res.add(field);
    return res;
  }
  static void openDocs(Consumer<String> refuse, Path folder){ Window.onFiles(refuse,"The documentation is not opened",()->openAll(refuse,folder.resolve(Facts.outDir).resolve("gen_java"))); }
  private static void openAll(Consumer<String> refuse, Path gen){
    var docs= Fs.walk(gen,s->s.filter(p->p.toString().endsWith(".html")).toList());
    if (docs.isEmpty()){ refuse.accept("The documentation is not opened: no .html file is in\n"+gen); return; }
    docs.forEach(OpenPath::open);
  }
}

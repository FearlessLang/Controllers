package gui;

import static gui.Window.mono;
import static gui.Window.named;
import static gui.Window.small;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

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
import utils.Push;

/// The right half of the window: everything about one registered project, and the
/// buttons that act on it. One Panel per project, kept while the project stays registered:
/// it holds the Output of the project.
final class Panel{
  private static final int iconSize= 32;
  private final Window window;
  final JPanel root= new JPanel(new BorderLayout(8,8));
  final JTextArea output= mono(named(new JTextArea(10,60),"output"));
  private final JScrollPane outputScroll= new JScrollPane(output);
  private final JButton clearOutput= small("Clear output",this::clearOutput);
  private final JTextArea details= mono(named(new JTextArea(9,40),"details"));
  private final JPanel kinds= new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));
  private final JPanel mainsBox= named(new JPanel(),"mains");
  private final JScrollPane mainsScroll= new JScrollPane(mainsBox);
  private final JPanel mainsPanel= new JPanel(new BorderLayout());
  private final JPanel linksBox= new JPanel();
  private final Collapsible links= new Collapsible("Links",new JScrollPane(linksBox),false);
  private final Collapsible information= new Collapsible("Information",new JScrollPane(details),true);
  private final JButton openDocs= small("Open docs",()->openDocs(this.project.folder()));
  private final JButton action= named(new JButton("Compile"),"action");
  private final JLabel icon= new JLabel();
  private final JLabel name= new JLabel();
  private final JList<LogFiles.Entry> logList= new JList<>();
  private final JButton viewLog= small("View",this::viewLog);
  private final JButton copyLog= small("Copy",this::copyLog);
  private final JButton deleteLog= small("Delete",this::deleteLog);
  private Object shown= List.of();
  private Project project;
  Panel(Window window){
    this.window= window;
    output.setEditable(false);
    details.setEditable(false);
    mainsBox.setLayout(new BoxLayout(mainsBox,BoxLayout.Y_AXIS));
    linksBox.setLayout(new BoxLayout(linksBox,BoxLayout.Y_AXIS));
    var pick= new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));
    pick.add(named(small("All",()->window.ask("mains",project.alias(),String.join(" ",project.knownMains()))),"all"));
    pick.add(named(small("None",()->window.ask("mains",project.alias(),"")),"none"));
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
  private void clearOutput(){ window.ask("clear",project.alias(),""); }
  void append(String text){
    var bar= outputScroll.getVerticalScrollBar();
    var following= bar.getValue()+bar.getVisibleAmount() >= bar.getMaximum()-16;
    output.append(text);
    if (following){ output.setCaretPosition(output.getDocument().getLength()); }
  }
  private void act(){
    information.setOpen(false);
    links.setOpen(false);
    window.ask(project.action().verb(),project.alias(),"");
  }
  void render(Project p, List<Project> all){
    var next= List.of(p,all.stream().map(Project::entry).toList());
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
    fillLinks(p,all);
    logList.setListData(p.facts().logs().toArray(LogFiles.Entry[]::new));
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
    Window.showText(root,Fs.readUtf8(sel.path()),sel.path().getFileName().toString(),JOptionPane.PLAIN_MESSAGE);
  }
  private void copyLog(){
    var selection= new StringSelection(Fs.readUtf8(logList.getSelectedValue().path()));
    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection,selection);
  }
  private void deleteLog(){
    var sel= logList.getSelectedValue();
    if (JOptionPane.showConfirmDialog(root,"Delete "+sel.path().getFileName()+"?","Fearless",JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION){ return; }
    Fs.rmTree(sel.path());
  }
  //For a code project: what it can run. Unknown until compiled, a single main needs
  //no choice, and several mains are picked one by one or with All and None.
  private void fillMains(Project p){
    mainsBox.removeAll();
    mainsPanel.setVisible(p.kind() == Kind.code);
    var known= p.knownMains();
    if (p.mains().isEmpty()){ mainsBox.add(new JLabel("<needs compiling>")); return; }
    if (known.size() == 1){ mainsBox.add(new JLabel(known.getFirst())); return; }
    var chosen= p.selectedMains();
    for(var main: known){
      var box= named(new JCheckBox(main,chosen.contains(main)),main);
      box.setEnabled(!p.busy());
      box.addActionListener(_->window.ask("mains",p.alias(),String.join(" ",box.isSelected() ? Push.of(chosen,main) : chosen.stream().filter(m->!m.equals(main)).toList())));
      mainsBox.add(box);
    }
    mainsScroll.setPreferredSize(new Dimension(0,Math.min(3,known.size())*26+8));
  }
  private void fillKinds(Project p){
    kinds.removeAll();
    if (p.kind() != Kind.idle){ kinds.add(kindButton(p,"Back to idle",Kind.idle)); return; }
    kinds.add(kindButton(p,"Become data",Kind.dataReadOnly));
    kinds.add(kindButton(p,"Become editable data",Kind.dataReadWrite));
    kinds.add(kindButton(p,"Become code",Kind.code));
  }
  private JButton kindButton(Project p, String text, Kind target){
    var res= small(text,()->window.ask("kind",p.alias(),target.text));
    res.setEnabled(!p.busy());
    return res;
  }
  private void fillLinks(Project p, List<Project> all){
    var iAmCode= p.kind() == Kind.code;
    linksBox.removeAll();
    links.setVisible(iAmCode || p.kind().isData());
    if (!links.isVisible()){ return; }
    linksBox.add(new JLabel(iAmCode ? "Data projects this code project reads or edits:" : "Code projects that may read or edit this:"));
    all.stream()
      .filter(o->iAmCode ? o.kind().isData() : o.kind() == Kind.code)
      .sorted(Comparator.comparing(Project::alias))
      .forEach(o->linksBox.add(linkRow(iAmCode ? p : o,iAmCode ? o : p,o.alias())));
  }
  private JPanel linkRow(Project code, Project data, String other){
    var row= new JPanel(new FlowLayout(FlowLayout.LEFT,6,0));
    row.add(new JLabel(other));
    row.add(linkField(code,data,"read",code.entry().reads()));
    if (data.kind() == Kind.dataReadWrite || code.entry().edits().containsKey(data.alias())){ row.add(linkField(code,data,"write",code.entry().edits())); }
    return row;
  }
  private JPanel linkField(Project code, Project data, String how, Map<String,List<String>> links){
    var was= String.join(" ",links.getOrDefault(data.alias(),List.of()));
    var field= new JTextField(was,14);
    field.addActionListener(_->field.transferFocus());
    field.addFocusListener(new FocusAdapter(){
      @Override public void focusLost(FocusEvent e){ if (!field.getText().strip().equals(was)){ window.ask("link",code.alias(),data.alias()+" "+how+" "+field.getText()); } }
    });
    var res= new JPanel(new FlowLayout(FlowLayout.LEFT,2,0));
    res.add(new JLabel(how));
    res.add(field);
    return res;
  }
  static void openDocs(Path folder){
    var genJava= folder.resolve(Facts.outDir).resolve("gen_java");
    if (!Files.isDirectory(genJava)){ return; }
    Fs.walk(genJava,s->s.filter(p->p.toString().endsWith(".html")).toList()).forEach(OpenPath::open);
  }
}

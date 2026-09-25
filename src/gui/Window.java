package gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Taskbar;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.TransferHandler;
import javax.swing.WindowConstants;

import controller.Deployed;
import controller.Main;
import controller.Manager;
import controller.Manager.State;
import controller.Project;
import controller.Registry;
import tools.OpenPath;
import userMessages.Violation;

/// The manager window: the tiles of the registered projects on the left, the Panel of
/// the selected one on the right. It shows the State the Manager hands it and turns every
/// click into a request to the Manager. Closing the window only hides it; Quit ends the process.
public final class Window implements Manager.View{
  //A divider dragged to the tiles-only edge would otherwise restore to that same edge
  //on every future selection, silently reopening each panel too narrow to see.
  private static final int minPanelWidth= 300;
  public final JFrame frame= new JFrame("Fearless Manager");
  private final Main main;
  private final JLabel status= new JLabel();
  final Tiles tiles= new Tiles(this::select);
  final JSplitPane split= new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,tiles,new JPanel());
  private final JMenu running= new JMenu("Running");
  private final JMenu project= new JMenu("Project");
  private final Map<Path,Panel> panels= new HashMap<>();
  private final Instant start= Instant.now();
  private final Timer ticker= new Timer(1000,_->tick());
  private State state= new State(List.of(),Optional.empty());
  private Optional<Panel> shown= Optional.empty();
  private boolean surfaced;
  private Window(Main main){
    this.main= main;
    status.setBorder(BorderFactory.createEmptyBorder(4,8,4,8));
    tiles.setBorder(BorderFactory.createTitledBorder("Registered project folders"));
    split.setResizeWeight(0.3);
    split.setDividerLocation(320);
    frame.setJMenuBar(menuBar());
    frame.add(split,BorderLayout.CENTER);
    frame.add(status,BorderLayout.SOUTH);
    frame.getRootPane().setTransferHandler(dropHandler());
    frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    frame.addWindowListener(new WindowAdapter(){
      @Override public void windowClosing(WindowEvent e){ frame.setVisible(false); ticker.stop(); }
      @Override public void windowIconified(WindowEvent e){ ticker.stop(); }
      @Override public void windowDeiconified(WindowEvent e){ ticker.start(); surfaced= true; }
      @Override public void windowActivated(WindowEvent e){ surfaced= true; }
    });
    frame.setIconImage(Icons.app());
    if (Taskbar.isTaskbarSupported() && Taskbar.getTaskbar().isSupported(Taskbar.Feature.ICON_IMAGE)){ Taskbar.getTaskbar().setIconImage(Icons.app()); }
    frame.setLocationByPlatform(true);
    var avail= GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
    frame.setSize(new Dimension(Math.min(760,avail.width),Math.min(900,avail.height)));//the size a restore (un-maximize) falls back to
    frame.setExtendedState(Frame.MAXIMIZED_BOTH);
    tick();
  }
  public static Window create(Main main){ return onEdt(()->new Window(main)); }
  private static <T> T onEdt(Supplier<T> make){
    if (SwingUtilities.isEventDispatchThread()){ return make.get(); }
    var result= new AtomicReference<T>();
    try{ SwingUtilities.invokeAndWait(()->result.set(make.get())); }
    catch(InterruptedException|InvocationTargetException e){ throw Violation.couldNotStartGui(e); }
    return result.get();
  }
  @Override public void show(){
    SwingUtilities.invokeLater(()->{
      surfaced= surfaced && onScreen();
      frame.setVisible(true);
      frame.setExtendedState(frame.getExtendedState() & ~Frame.ICONIFIED);
      frame.toFront();
      frame.requestFocus();
      ticker.start();
      var check= new Timer(2000,_->checkSurfaced());
      check.setRepeats(false);
      check.start();
    });
  }
  @Override public void state(State s){ SwingUtilities.invokeLater(()->render(s)); }
  @Override public void output(Path folder, String text){ SwingUtilities.invokeLater(()->panel(folder).append(text)); }
  @Override public void note(String text){ SwingUtilities.invokeLater(()->JOptionPane.showMessageDialog(frame,text,"Fearless",JOptionPane.PLAIN_MESSAGE)); }
  @Override public void clear(Path folder){ SwingUtilities.invokeLater(()->panel(folder).output.setText("")); }
  private boolean onScreen(){ return frame.isVisible() && (frame.getExtendedState() & Frame.ICONIFIED) == 0; }
  //A desktop that refuses to show a window reports it as iconified and never deiconifies it.
  private void checkSurfaced(){
    if (frame.isVisible() && !surfaced && !onScreen()){ main.fail(Violation.desktopHidesWindow()); }
  }
  public boolean askForget(){
    return onEdt(()->JOptionPane.showConfirmDialog(frame,"""
      Remove Fearless as the program registered to open Fearless projects?

      What your desktop already remembers by hand is left exactly as it is:
      this only removes what Fearless itself registered.""","Fearless",JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION);
  }
  void ask(String verb, String name, String arg){ main.manager().ask(verb,name,arg); }
  private void select(String name){ ask("select",name,""); }
  @Override public boolean visible(){ return ticker.isRunning(); }
  private Panel panel(Path folder){ return panels.computeIfAbsent(folder,_->new Panel(this)); }
  private void render(State s){
    state= s;
    tiles.render(s);
    panels.keySet().removeIf(f->s.of(f).isEmpty());
    s.shown().ifPresent(p->panel(p.folder()).render(p,s.projects()));
    var next= s.shown().map(p->panel(p.folder()));
    if (!next.equals(shown)){ place(next); }
    fillProjectMenu();
    running.removeAll();
    if (s.running().isEmpty()){ running.add(item("<nothing running>",false,()->{})); }
    s.projects().stream().filter(Project::busy).forEach(p->running.add(item(p.alias()+" - "+p.job(),true,()->select(p.alias()))));
  }
  private void place(Optional<Panel> next){
    shown= next;
    var where= split.getWidth() > 0 ? Math.min(split.getDividerLocation(),split.getWidth()-minPanelWidth) : split.getDividerLocation();
    split.setRightComponent(next.map(p->(Component)p.root).orElseGet(JPanel::new));
    split.setDividerLocation(where);
  }
  private JMenuBar menuBar(){
    var res= new JMenuBar();
    var manager= new JMenu("Manager");
    manager.setMnemonic('M');
    manager.add(item("Edit project metadata...",true,this::editMetadata));
    manager.add(item("Show raw project state...",true,()->showText(frame,rawState(),"Raw project state",JOptionPane.PLAIN_MESSAGE)));
    manager.add(item("Connect Eclipse...",true,this::connectEclipse));
    manager.addSeparator();
    manager.add(item("Forget association",true,()->main.forgetAssociation(this)));
    manager.add(item("Quit manager",true,main::quit));
    project.setMnemonic('P');
    running.setMnemonic('R');
    res.add(manager);
    res.add(project);
    res.add(running);
    fillProjectMenu();
    return res;
  }
  static JMenuItem item(String text, boolean enabled, Runnable action){
    var res= new JMenuItem(text);
    res.setEnabled(enabled);
    res.addActionListener(_->action.run());
    return res;
  }
  private void fillProjectMenu(){
    project.removeAll();
    project.add(item("Add folder...",true,this::addFolder));
    project.addSeparator();
    var on= state.shown();
    var f= on.map(Project::folder);
    project.add(item("Clear cache",on.isPresent(),()->ask("clean",on.get().alias(),"")));
    project.add(item("Browse files",on.isPresent(),()->OpenPath.open(f.get())));
    project.add(item("View documentation",on.flatMap(Project::mains).isPresent(),()->Panel.openDocs(f.get())));
    project.add(item("View base documentation",true,()->OpenPath.open(Deployed.stdLib("baseCache").resolve("base.html"))));
    project.add(item("Error report",on.flatMap(Project::problem).isPresent(),()->showText(frame,on.get().problem().get(),"Why this project is invalid",JOptionPane.ERROR_MESSAGE)));
    project.addSeparator();
    project.add(item("Forget project",on.isPresent(),()->ask("forget",on.get().alias(),"")));
  }
  private void addFolder(){
    var chooser= new JFileChooser();
    chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    chooser.setDialogTitle("Add a Fearless project folder");
    if (chooser.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION){ return; }
    main.manager().message(chooser.getSelectedFile().toPath().toString());
  }
  private TransferHandler dropHandler(){
    return new TransferHandler(){
      @Override public boolean canImport(TransferSupport support){
        if (!Drop.hasFiles(support.getTransferable())){ return false; }
        support.setDropAction(COPY);
        return true;
      }
      @Override public boolean importData(TransferSupport support){
        var paths= Drop.paths(support.getTransferable());
        if (paths.isEmpty()){ return false; }
        paths.forEach(p->main.manager().message(p.toString()));
        return true;
      }
    };
  }
  private void connectEclipse(){
    var chooser= new JFileChooser();
    chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    chooser.setDialogTitle("Select Eclipse: its program, its folder, or the folder it was unzipped into");
    if (chooser.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION){ return; }
    main.manager().connect(chooser.getSelectedFile().toPath());
  }
  private void editMetadata(){
    var area= mono(new JTextArea(Registry.text(state.projects().stream().map(Project::entry).toList()),30,100));
    var dialog= new JDialog(frame,"Edit project metadata",true);
    var commit= new JButton("Commit");
    var close= new JButton("Close");
    commit.addActionListener(_->main.manager().commit(area.getText(),()->SwingUtilities.invokeLater(dialog::dispose)));
    close.addActionListener(_->dialog.dispose());
    var buttons= new JPanel(new FlowLayout(FlowLayout.RIGHT));
    buttons.add(commit);
    buttons.add(close);
    dialog.add(new JScrollPane(area),BorderLayout.CENTER);
    dialog.add(buttons,BorderLayout.SOUTH);
    dialog.pack();
    dialog.setLocationRelativeTo(frame);
    dialog.setVisible(true);
  }
  private String rawState(){
    if (state.projects().isEmpty()){ return "<nothing registered>"; }
    return String.join("\n\n",state.projects().stream().map(Project::information).toList());
  }
  private void tick(){
    var up= Duration.between(start,Instant.now()).toSeconds();
    var job= state.shown().filter(Project::busy).map(p->"   -   "+p.job()+"   "+clock(Duration.between(p.since(),Instant.now()).toSeconds()));
    status.setText("Running for "+clock(up)+job.orElse(""));
  }
  static String clock(long seconds){ return "%02d:%02d:%02d".formatted(seconds/3600,(seconds/60)%60,seconds%60); }
  static <T extends JComponent> T named(T c, String name){ c.setName(name); return c; }
  static JTextArea mono(JTextArea area){ area.setFont(new Font(Font.MONOSPACED,Font.PLAIN,13)); return area; }
  static JButton small(String text, Runnable action){
    var res= new JButton(text);
    res.setMargin(new Insets(0,6,0,6));
    res.addActionListener(_->action.run());
    return res;
  }
  static void showText(Component parent, String text, String title, int kind){
    var area= mono(new JTextArea(text,24,90));
    area.setEditable(false);
    JOptionPane.showMessageDialog(parent,new JScrollPane(area),title,kind);
  }
}

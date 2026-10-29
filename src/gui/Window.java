package gui;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Taskbar;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.TransferHandler;
import javax.swing.WindowConstants;

import controller.Deployed;
import controller.Main;
import controller.TaggedText;
import controller.Manager;
import controller.Manager.State;
import controller.Project;
import controller.Registry;
import managerMessages.Report;
import managerMessages.Violation;
import tools.Fs;
import tools.OpenPath;
import userMessages.UserError;
import utils.Bug;

/// The manager window: the tiles of the registered projects on the left, the Panel of
/// the selected one on the right. It shows the State the Manager hands it and turns every
/// click into a run of the manager program with the arguments of a FearlessCommand.
/// Closing the window only hides it; Quit ends the process.
public final class Window implements Manager.View{
  //A divider dragged to the tiles-only edge would otherwise restore to that same edge
  //on every future selection, silently reopening each panel too narrow to see.
  private static final int minPanelWidth= 300;
  public final JFrame frame= new JFrame("Fearless Manager");
  private final Main main;
  private final Path info;
  final Tiles tiles= new Tiles();
  final JSplitPane split= new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
  private final Map<Path,Panel> panels= new HashMap<>();
  private final Map<Path,JDialog> editors= new HashMap<>();
  private final Set<Path> written= ConcurrentHashMap.newKeySet();
  private final StandIn standIn= new StandIn();
  private State state= new State(List.of(), Optional.empty(), List.of());
  private Optional<Panel> shown= Optional.empty();
  private volatile boolean onScreen;
  private boolean surfaced;
  private boolean tray= true;
  private Window(Main main, Path info){
    this.main= main;
    this.info= info;
    split.setDividerLocation(320);
    frame.setJMenuBar(menuBar());
    frame.add(tiles, BorderLayout.CENTER);
    frame.getRootPane().setTransferHandler(dropHandler());
    frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    frame.addWindowListener(new WindowAdapter(){
      @Override public void windowClosing(WindowEvent e){ close(); }
      @Override public void windowIconified(WindowEvent e){ onScreen = false; }
      @Override public void windowDeiconified(WindowEvent e){ onScreen = true; surfaced = true; }
      @Override public void windowActivated(WindowEvent e){ surfaced = true; }
    });
    frame.setIconImage(Icons.app());
    if (Taskbar.isTaskbarSupported() && Taskbar.getTaskbar().isSupported(Taskbar.Feature.ICON_IMAGE)){ Taskbar.getTaskbar().setIconImage(Icons.app()); }
    frame.setLocationByPlatform(true);
    var avail= GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
    frame.setSize(new Dimension(Math.min(760, avail.width), Math.min(900, avail.height)));//the size a restore (un-maximize) falls back to
    frame.setExtendedState(Frame.MAXIMIZED_BOTH);
    Toolkit.getDefaultToolkit().addAWTEventListener(this::leaveField, AWTEvent.MOUSE_EVENT_MASK);
  }
  private void leaveField(AWTEvent e){
    var focus= KeyboardFocusManager.getCurrentKeyboardFocusManager();
    var owner= focus.getFocusOwner();
    var leaves= e.getID() == MouseEvent.MOUSE_PRESSED && owner instanceof JTextField && owner != e.getSource() && SwingUtilities.isDescendingFrom(owner, frame);
    if (leaves){ focus.clearFocusOwner(); }
  }
  public static Window create(Main main, Path managerDir){ return onEdt(() -> new Window(main, managerDir.resolve("projects.info"))); }
  public void tray(boolean present){ tray = present; }
  void fail(Throwable problem){ main.fail(problem); }
  static <T> T onEdt(Supplier<T> make){
    if (SwingUtilities.isEventDispatchThread()){ return make.get(); }
    var result= new AtomicReference<T>();
    try{ SwingUtilities.invokeAndWait(() -> result.set(make.get())); }
    catch (InterruptedException e){ throw Bug.of(e); }
    catch (InvocationTargetException e){
      if (e.getCause() instanceof UserError u){ throw u; }
      if (e.getCause() instanceof VirtualMachineError || e.getCause() instanceof LinkageError){ throw (Error)e.getCause(); }
      throw Violation.couldNotStartGui(e.getCause());
    }
    return result.get();
  }
  private void close(){
    frame.setVisible(false);
    onScreen = false;
    if (!tray){ standIn.show(); }
  }
  @Override public void show(){ SwingUtilities.invokeLater(this::showNow); }
  private void showNow(){
    standIn.hide();
    surfaced = surfaced && onScreen;
    frame.setVisible(true);
    frame.setExtendedState(frame.getExtendedState() & ~Frame.ICONIFIED);
    frame.toFront();
    tiles.list.requestFocus();
    onScreen = true;
    var check= new Timer(2000, _ -> checkSurfaced());
    check.setRepeats(false);
    check.start();
  }
  @Override public void state(State s){ SwingUtilities.invokeLater(() -> render(s)); }
  @Override public void output(Path folder, String text){ SwingUtilities.invokeLater(() -> panel(folder).append(text)); }
  @Override public void clear(Path folder){ SwingUtilities.invokeLater(() -> panel(folder).output.setText("")); }
  @Override public void refuse(String text){ SwingUtilities.invokeLater(() -> { showNow(); say(text); }); }
  @Override public void tell(String text){ refuse(text); }
  private void say(String text){ JOptionPane.showMessageDialog(frame, text, "Fearless", JOptionPane.PLAIN_MESSAGE); }
  @Override public void choose(Path file, List<Project.Claimant> choices){ SwingUtilities.invokeLater(() -> chooser(file, choices)); }
  @Override public void allow(String question, List<String> extensions){
    SwingUtilities.invokeLater(() -> { if (JOptionPane.showConfirmDialog(frame, question, "Fearless", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION){ capture(extensions); } });
  }
  private void capture(List<String> extensions){
    Registry.Content now;
    try{ now= Registry.parse(Fs.readUtf8(info), info, p -> p, info.getParent()); }
    catch (UserError e){ say(e.getMessage()); return; }
    var all= new TreeSet<>(now.extensions());
    all.addAll(extensions);
    commit(Registry.text(new Registry.Content(now.projects(), List.copyOf(all))));
  }
  @Override public void edited(Path file){
    SwingUtilities.invokeLater(() -> { var editor= editors.remove(file); if (editor != null){ editor.dispose(); } });
    if (written.remove(file)){ Fs.ofV(() -> Files.deleteIfExists(file)); }
  }
  @Override public boolean visible(){ return onScreen; }
  private Path commit(String content){
    var file= Fs.of(() -> Files.createTempFile("projects", ".info"));
    file.toFile().deleteOnExit();
    written.add(file);
    Fs.writeUtf8(file, content);
    Main.command("EditMetadata", TaggedText.ofPath(file));
    return file;
  }
  private void chooser(Path file, List<Project.Claimant> choices){
    var list= choices(choices);
    var dialog= new JDialog(frame, "Open with", false);
    Runnable pick= () -> pick(dialog, list, file);
    var run= small("Run", pick);
    doubleClicked(list, pick);
    var buttons= new JPanel(new FlowLayout(FlowLayout.RIGHT));
    buttons.add(run);
    dialog.add(new JScrollPane(list), BorderLayout.CENTER);
    dialog.add(buttons, BorderLayout.SOUTH);
    dialog.getRootPane().setDefaultButton(run);
    dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    dialog.pack();
    dialog.setLocationRelativeTo(frame);
    dialog.setVisible(true);
    list.requestFocusInWindow();
  }
  private static void pick(JDialog dialog, JList<Project.Claimant> list, Path file){
    if (list.isSelectionEmpty()){ return; }
    dialog.dispose();
    Main.command("OpenWithOrShortcut", TaggedText.ofPath(file), list.getSelectedValue().alias(), list.getSelectedValue().main());
  }
  static JList<Project.Claimant> choices(List<Project.Claimant> choices){
    var icons= choices.stream().map(c -> new ImageIcon(Icons.of(c, 32))).toList();
    var res= new JList<>(choices.toArray(Project.Claimant[]::new));
    res.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    res.setSelectedIndex(0);
    res.setCellRenderer(new DefaultListCellRenderer(){
      @Override public Component getListCellRendererComponent(JList<?> l, Object value, int i, boolean selected, boolean focused){
        super.getListCellRendererComponent(l, choices.get(i).label(), i, selected, focused);
        setIcon(icons.get(i));
        return this;
      }
    });
    return res;
  }
  //A desktop that refuses to show a window reports it as iconified and never deiconifies it.
  private void checkSurfaced(){
    var hidden= frame.isVisible() && (frame.getExtendedState() & Frame.ICONIFIED) != 0;
    if (hidden && !surfaced){ main.fail(Violation.desktopHidesWindow()); }
  }
  private Panel panel(Path folder){ return panels.computeIfAbsent(folder, _ -> new Panel(this::say)); }
  private void render(State s){
    state = s;
    tiles.render(s);
    panels.keySet().removeIf(f -> s.of(f).isEmpty());
    s.shown().ifPresent(p -> panel(p.folder()).render(p, s.projects(), split.getHeight()));
    var next= s.shown().map(p -> panel(p.folder()));
    if (!next.equals(shown)){ place(next); }
  }
  private void place(Optional<Panel> next){
    shown = next;
    var content= frame.getContentPane();
    content.removeAll();
    content.add(next.isEmpty() ? tiles : split, BorderLayout.CENTER);
    content.revalidate();
    content.repaint();
    if (next.isEmpty()){ return; }
    var where= split.getWidth() > 0 ? Math.min(split.getDividerLocation(), split.getWidth()-minPanelWidth) : split.getDividerLocation();
    split.setLeftComponent(tiles);
    split.setRightComponent(next.get().root);
    split.setDividerLocation(where);
  }
  private JMenuBar menuBar(){
    var res= new JMenuBar();
    var manager= new JMenu("Manager");
    manager.setMnemonic('M');
    manager.add(item("Add folder...", () -> choose("Add a Fearless project folder", p -> Main.command("Register", TaggedText.ofPath(p)))));
    manager.add(item("Edit project metadata...", this::editMetadata));
    manager.add(item("Connect Eclipse...", () -> choose("Select Eclipse: its folder, a file in it, or the folder it was unzipped into", p -> Main.command("ConnectEclipse", TaggedText.ofPath(p)))));
    manager.add(item("Base documentation", () -> open(this::say, Deployed.stdLib("baseCache").resolve("base.html"))));
    manager.addSeparator();
    manager.add(item("Reset", this::reset));
    manager.add(item("Quit", () -> Main.command("Quit")));
    res.add(manager);
    return res;
  }
  private void reset(){
    var reset= JOptionPane.showOptionDialog(frame, Report.resetQuestion(), "Fearless", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, new Object[]{"Reset", "Cancel"}, "Cancel");
    if (reset == 0){ Main.command("Reset"); }
  }
  static JMenuItem item(String text, Runnable action){
    var res= new JMenuItem(text);
    res.addActionListener(_ -> action.run());
    return res;
  }
  private void choose(String title, Consumer<Path> chosen){
    var chooser= new JFileChooser();
    chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    chooser.setDialogTitle(title);
    if (chooser.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION){ return; }
    chosen.accept(chooser.getSelectedFile().toPath());
  }
  private TransferHandler dropHandler(){
    return new TransferHandler(){
      @Override public boolean canImport(TransferSupport support){
        if (!Drop.hasFiles(support.getTransferable())){ return false; }
        support.setDropAction(COPY);
        return true;
      }
      @Override public boolean importData(TransferSupport support){
        var paths= Drop.paths(support.getTransferable(), Window.this::say);
        paths.forEach(p -> Main.command("Register", TaggedText.ofPath(p)));
        return !paths.isEmpty();
      }
    };
  }
  private void editMetadata(){
    var base= Fs.readUtf8(info);
    var area= mono(new JTextArea(base, 30, 100));
    var dialog= new JDialog(frame, "Edit project metadata", true);
    var commit= new JButton("Commit");
    var close= new JButton("Close");
    commit.addActionListener(_ -> commit(dialog, base, area.getText()));
    close.addActionListener(_ -> dialog.dispose());
    var buttons= new JPanel(new FlowLayout(FlowLayout.RIGHT));
    buttons.add(commit);
    buttons.add(close);
    dialog.add(new JScrollPane(area), BorderLayout.CENTER);
    dialog.add(buttons, BorderLayout.SOUTH);
    dialog.pack();
    dialog.setLocationRelativeTo(frame);
    dialog.setVisible(true);
  }
  private void commit(JDialog dialog, String base, String text){
    if (!Fs.readUtf8(info).equals(base)){ JOptionPane.showMessageDialog(dialog, Report.metadataChanged(), "Fearless", JOptionPane.PLAIN_MESSAGE); return; }
    editors.put(commit(text), dialog);
  }
  static JTextArea mono(JTextArea area){ area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13)); return area; }
  static <T extends JComponent> T named(T c, String name){ c.setName(name); return c; }
  static <T extends JComponent> T doubleClicked(T c, Runnable r){
    c.addMouseListener(new MouseAdapter(){
      @Override public void mouseClicked(MouseEvent e){ if (e.getClickCount() == 2){ r.run(); } }
    });
    return c;
  }
  static JButton small(String text, Runnable action){
    var res= new Small(text);
    res.setMargin(new Insets(0, 6, 0, 6));
    res.addActionListener(_ -> res.press(action));
    return res;
  }
  @SuppressWarnings("serial")
  private static final class Small extends JButton{
    private boolean wanted= true;
    private final Timer rest= new Timer(500, _ -> super.setEnabled(wanted));
    Small(String text){ super(text); rest.setRepeats(false); }
    @Override public void setEnabled(boolean b){
      wanted = b;
      if (!rest.isRunning()){ super.setEnabled(b); }
    }
    void press(Runnable action){
      super.setEnabled(false);
      rest.restart();
      action.run();
    }
  }
  static void open(Consumer<String> refuse, Path path){
    if (!Files.exists(path)){ refuse.accept(Report.nothingToOpen(path)); return; }
    try{ OpenPath.open(path); }
    catch (UncheckedIOException e){ refuse.accept(Report.notOpened(path, e.getCause())); }
  }
}

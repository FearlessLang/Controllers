package gui;

import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JWindow;

import controller.Main;

final class StandIn{
  private static final int side= 32;
  private JWindow window;
  void show(){
    if (window == null){ window= make(); }
    var screen= GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
    window.setLocation(screen.x+screen.width-side,screen.y+screen.height-side);
    window.setVisible(true);
  }
  void hide(){ if (window != null){ window.setVisible(false); } }
  private static JWindow make(){
    var res= new JWindow();
    res.setName("standIn");
    res.setAlwaysOnTop(true);
    res.setSize(side,side);
    var icon= new JLabel(new ImageIcon(Icons.app().getScaledInstance(side,side,Image.SCALE_SMOOTH)));
    var menu= new JPopupMenu();
    menu.add(new Entry("Show manager",()->Main.command("Show")));
    menu.addSeparator();
    menu.add(new Entry("Quit manager",()->Main.command("Quit")));
    icon.addMouseListener(new MouseAdapter(){
      @Override public void mouseClicked(MouseEvent e){ menu.show(icon,e.getX(),e.getY()); }
    });
    res.add(icon);
    return res;
  }
  @SuppressWarnings("serial")
  static final class Entry extends JMenuItem{
    private long shown;
    private boolean pressed;
    Entry(String text, Runnable action){
      super(text);
      addActionListener(_->action.run());
      addHierarchyListener(_->shown= System.currentTimeMillis());
    }
    @Override protected void processMouseEvent(MouseEvent e){
      if (e.getID() == MouseEvent.MOUSE_PRESSED){ pressed= System.currentTimeMillis()-shown >= 500; }
      super.processMouseEvent(e);
      if (e.getID() == MouseEvent.MOUSE_RELEASED){ pressed= false; }
    }
    @Override protected void fireActionPerformed(ActionEvent e){ if (pressed){ super.fireActionPerformed(e); } }
  }
}

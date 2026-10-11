package gui;

import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;

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
    menu.add(Window.item("Show manager",()->Main.command("Show")));
    menu.add(Window.item("Quit manager",()->Main.command("Quit")));
    icon.addMouseListener(new MouseAdapter(){
      @Override public void mousePressed(MouseEvent e){
        if (SwingUtilities.isRightMouseButton(e)){ menu.show(icon,e.getX(),e.getY()); return; }
        if (SwingUtilities.isLeftMouseButton(e)){ Main.command("Show"); }
      }
    });
    res.add(icon);
    return res;
  }
}

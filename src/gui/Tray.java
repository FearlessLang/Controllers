package gui;

import java.awt.AWTException;
import java.awt.Point;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;

import javax.swing.JPopupMenu;
import javax.swing.JWindow;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import controller.Main;
import managerMessages.Violation;
import tools.Fs;

/// The icon is the way back to a closed window: with no tray the window shows its StandIn
/// instead, and a failure while adding the icon, or the desktop later taking the icon away, stops the manager.
/// On Linux the icon is a StatusNotifierItem on the session bus (Sni); elsewhere it is the AWT tray icon.
/// The icon lives for the whole process life and is removed by process death.
public final class Tray{
  private Tray(){}
  public static boolean install(Window window){
    if (Fs.isLinux()){ return Sni.install(()->Main.command("Show"),()->Main.command("Quit"),Icons.app()); }
    if (!SystemTray.isSupported()){ return false; }
    var menu= menu();
    var anchor= new JWindow();
    anchor.setAlwaysOnTop(true);
    menu.addPopupMenuListener(new PopupMenuListener(){
      @Override public void popupMenuWillBecomeVisible(PopupMenuEvent e){}
      @Override public void popupMenuWillBecomeInvisible(PopupMenuEvent e){ anchor.setVisible(false); }
      @Override public void popupMenuCanceled(PopupMenuEvent e){}
    });
    var icon= new TrayIcon(Icons.app(),"Fearless Manager");
    icon.setImageAutoSize(true);
    icon.addMouseListener(new MouseAdapter(){
      @Override public void mouseReleased(MouseEvent e){ open(anchor,menu,e.getPoint()); }
    });
    var tray= SystemTray.getSystemTray();
    try{ tray.add(icon); }
    catch(AWTException e){ throw Violation.couldNotAddTrayIcon(e); }
    PropertyChangeListener gone= _->{ if (!SystemTray.isSupported() || !Arrays.asList(tray.getTrayIcons()).contains(icon)){ window.fail(Violation.trayIconRemoved()); } };
    tray.addPropertyChangeListener("systemTray",gone);
    tray.addPropertyChangeListener("trayIcons",gone);
    return true;
  }
  static JPopupMenu menu(){
    var res= new JPopupMenu();
    res.add(Window.item("Show manager",()->Main.command("Show")));
    res.addSeparator();
    res.add(Window.item("Quit manager",()->Main.command("Quit")));
    return res;
  }
  private static void open(JWindow anchor, JPopupMenu menu, Point at){
    anchor.setBounds(at.x,at.y,1,1);
    anchor.setVisible(true);
    menu.show(anchor,0,0);
  }
}

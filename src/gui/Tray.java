package gui;

import java.awt.AWTException;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.stream.Stream;

import javax.swing.JPopupMenu;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import controller.Main;
import managerMessages.Violation;
import tools.Fs;
import utils.OneOr;

/// The icon is the way back to a closed window: with no tray the window shows its StandIn
/// instead, and a failure while adding the icon, or the desktop later taking the icon away, stops the manager.
/// On Linux the icon is a StatusNotifierItem on the session bus (Sni) when the desktop has a watcher for it; otherwise it is the AWT tray icon.
/// The icon lives for the whole process life and is removed by process death.
public final class Tray{
  public static boolean install(Window window){
    var menu= menu();
    var anchor= new JWindow();
    anchor.setType(java.awt.Window.Type.POPUP);
    anchor.setAlwaysOnTop(true);
    menu.addPopupMenuListener(new PopupMenuListener(){
      @Override public void popupMenuWillBecomeVisible(PopupMenuEvent e){}
      @Override public void popupMenuWillBecomeInvisible(PopupMenuEvent e){ anchor.setVisible(false); }
      @Override public void popupMenuCanceled(PopupMenuEvent e){}
    });
    if (Fs.isLinux() && Sni.install(() -> Main.command("Show"), () -> Main.command("Quit"), at -> SwingUtilities.invokeLater(() -> open(anchor, menu, at)), Icons.app())){ return true; }
    if (!SystemTray.isSupported()){ return false; }
    var icon= new TrayIcon(Icons.app(), "Fearless Manager");
    icon.setImageAutoSize(true);
    icon.addMouseListener(new MouseAdapter(){
      @Override public void mouseReleased(MouseEvent e){ open(anchor, menu, e.getPoint()); }
    });
    var tray= SystemTray.getSystemTray();
    try{ tray.add(icon); }
    catch (AWTException e){ throw Violation.couldNotAddTrayIcon(e); }
    PropertyChangeListener gone= _ -> { if (!SystemTray.isSupported() || !Arrays.asList(tray.getTrayIcons()).contains(icon)){ window.fail(Violation.trayIconRemoved()); } };
    tray.addPropertyChangeListener("systemTray", gone);
    tray.addPropertyChangeListener("trayIcons", gone);
    return true;
  }
  private static JPopupMenu menu(){
    var res= new JPopupMenu();
    res.add(Window.item("Show manager", () -> Main.command("Show")));
    res.addSeparator();
    res.add(Window.item("Quit manager", () -> Main.command("Quit")));
    return res;
  }
  //A desktop takes the pointer input over its panels, also where a window covers them: the menu stays inside the usable area of the screen holding the click.
  private static void open(JWindow anchor, JPopupMenu menu, Point at){
    var screen= OneOr.of("one screen holds the click", Stream.of(GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()).map(GraphicsDevice::getDefaultConfiguration).filter(c -> c.getBounds().contains(at)));
    var b= screen.getBounds();
    var in= Toolkit.getDefaultToolkit().getScreenInsets(screen);
    var size= menu.getPreferredSize();
    var x= Math.clamp(at.x, b.x+in.left, b.x+b.width-in.right-size.width);
    var y= Math.clamp(at.y, b.y+in.top, b.y+b.height-in.bottom-size.height);
    anchor.setBounds(x, y, 1, 1);
    anchor.setVisible(true);
    menu.show(anchor, 0, 0);
  }
}

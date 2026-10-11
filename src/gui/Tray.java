package gui;

import java.awt.AWTException;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.beans.PropertyChangeListener;
import java.util.Arrays;

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
    var show= new MenuItem("Show manager");
    show.addActionListener(_->Main.command("Show"));
    var quit= new MenuItem("Quit manager");
    quit.addActionListener(_->Main.command("Quit"));
    var menu= new PopupMenu();
    menu.add(show);
    menu.addSeparator();
    menu.add(quit);
    var icon= new TrayIcon(Icons.app(),"Fearless Manager",menu);
    icon.setImageAutoSize(true);
    icon.addActionListener(_->Main.command("Show"));
    var tray= SystemTray.getSystemTray();
    try{ tray.add(icon); }
    catch(AWTException e){ throw Violation.couldNotAddTrayIcon(e); }
    PropertyChangeListener gone= _->{ if (!SystemTray.isSupported() || !Arrays.asList(tray.getTrayIcons()).contains(icon)){ window.fail(Violation.trayIconRemoved()); } };
    tray.addPropertyChangeListener("systemTray",gone);
    tray.addPropertyChangeListener("trayIcons",gone);
    return true;
  }
}

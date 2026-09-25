package gui;

import java.awt.AWTException;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.beans.PropertyChangeListener;
import java.util.Arrays;

import controller.Main;
import controller.Messages;
import tools.Fs;

/// The icon is the way back to a closed window, so no tray support (some desktops), a
/// failure while adding the icon, or the desktop later taking the icon away, stops the manager.
/// On Linux the icon is a StatusNotifierItem on the session bus (Sni); elsewhere it is the AWT tray icon.
/// The icon lives for the whole process life and is removed by process death.
public final class Tray{
  private Tray(){}
  public static void install(Window window, Main main){
    if (Fs.isLinux()){ Sni.install(window::show,main::quit,Icons.app()); return; }
    if (!SystemTray.isSupported()){ throw Messages.noSystemTray(); }
    var show= new MenuItem("Show manager");
    show.addActionListener(_->window.show());
    var quit= new MenuItem("Quit manager");
    quit.addActionListener(_->main.quit());
    var menu= new PopupMenu();
    menu.add(show);
    menu.addSeparator();
    menu.add(quit);
    var icon= new TrayIcon(Icons.app(),"Fearless Manager",menu);
    icon.setImageAutoSize(true);
    icon.addActionListener(_->window.show());
    var tray= SystemTray.getSystemTray();
    try{ tray.add(icon); }
    catch(AWTException e){ throw Messages.couldNotAddTrayIcon(e); }
    PropertyChangeListener gone= _->{ if (!SystemTray.isSupported() || !Arrays.asList(tray.getTrayIcons()).contains(icon)){ main.fail(Messages.trayIconRemoved()); } };
    tray.addPropertyChangeListener("systemTray",gone);
    tray.addPropertyChangeListener("trayIcons",gone);
  }
}

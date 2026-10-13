package managerMessages;

import static userMessages.UserError.path;
import static userMessages.Violation.freshCopyThenReport;
import static userMessages.Violation.reported;

import java.nio.file.Path;
import java.util.List;

import userMessages.UserError;
import utils.Join;

/// Violation: what the manager can no longer do: the system does not offer what it needs,
/// or its own manager folder changed under it.
/// The counterpart is Report, for what the user gave the manager.
public final class Violation{
  private static String managerFolderIntro(){ return """
    Fearless uses one process (called the manager process)
    to keep track of other Fearless processes (the user processes).
    The manager folder is the folder used by the manager process to store
    coordination information into files.
    """;
  }
  private static String blockingPrograms(){ return """
    Programs that may use or block this folder include security software
    (antivirus, ransomware protection, endpoint protection), backup tools,
    sync tools, and file preview tools.
    """;
  }
  public static UserError fatalFromRun(String details){ return new UserError("A Fearless process reported a failure the manager can not recover from:\n\n"+details); }
  public static String fearlessTaken(List<String> programs){
    return """
      Another Fearless program now opens Fearless projects:
      %s

      Only one Fearless program at a time opens them, so this manager ends now.
      Starting this manager again makes it the program opening them, with every
      kind of file its projects open.""".formatted(Join.of(programs.stream().map(p -> "  "+p), "", "\n", "", ""));
  }
  public static UserError programFolderNotFound(Path startedFrom, String expectedDirName){
    return new UserError("""
      This copy of Fearless appears to have been moved, renamed, or damaged:
      no folder named "%s" exists above
      %s

      %s""".formatted(expectedDirName, path(startedFrom.toString()), freshCopyThenReport()));
  }
  public static UserError couldNotStartGui(Throwable cause){
    return new UserError("""
      Fearless could not open its window.

      Fearless needs to show a window, but opening the window failed.

      %s""".formatted(reported(cause)), cause);
  }
  public static UserError desktopHidesWindow(){
    return new UserError("""
      The desktop did not show the Fearless manager window.

      Fearless asked the desktop to show its window, and the desktop keeps
      reporting the window as minimized. When this happens every program with
      decorated windows is affected, not only Fearless: on GNOME it means the
      process that decorates windows (mutter-x11-frames) has died.
      Log out and log in again, then start Fearless again.
      """).bare();
  }
  public static UserError noSystemTray(){
    return new UserError("""
      Fearless could not add its icon to the system tray.

      The system tray of this desktop went away while Fearless was adding its
      icon, and the tray icon is how a closed manager window is brought back.
      Restore the system tray of this desktop, then start Fearless again.
      """);
  }
  public static UserError trayIconRemoved(){
    return new UserError("""
      The desktop removed the Fearless icon from the system tray.

      The tray icon is how a closed manager window is brought back, and the
      desktop took it away: its system tray went away or rejected the icon.
      Restore the system tray of this desktop, then start Fearless again.
      """);
  }
  public static UserError trayConnectionLost(Throwable cause){
    return new UserError("""
      The desktop stopped talking to the Fearless icon in the system tray.

      The tray icon is how a closed manager window is brought back, and the
      connection to the system tray of this desktop failed.

      %s
      Restore the system tray of this desktop, then start Fearless again.
      """.formatted(reported(cause)), cause);
  }
  public static UserError couldNotAddTrayIcon(Throwable cause){
    return new UserError("""
      Fearless could not add its icon to the system tray.

      This desktop offers a system tray, and the tray icon is how a closed
      manager window is brought back, but adding the icon failed.

      %s""".formatted(reported(cause)), cause);
  }
  public static UserError vmOrLinkageFailure(Throwable cause){
    return new UserError("""
      Fearless crashed because of a low-level JVM failure.

      Fearless includes its own packaged JVM. The packaged JVM failed, or the
      packaged Fearless code could not be linked correctly.""", cause);
  }
  public static UserError couldNotLoadIcon(Path icon, Throwable cause){
    return new UserError("""
      Fearless could not load its own icon.

      This copy of Fearless should already have this file:
      %s

      %s""".formatted(path(icon.toString()), reported(cause)), cause);
  }
  public static UserError couldNotDecodeIcon(Path icon){
    return new UserError("""
      Fearless could not load its own icon.

      This copy of Fearless should already have this file:
      %s
      The file is there and could be read, but it does not hold an image this
      Java runtime can decode.""".formatted(path(icon.toString())));
  }
  public static UserError couldNotCreateManagerFolder(Path dir, Throwable cause){
    return new UserError("""
      Fearless could not create its manager folder.

      Fearless tried to create this folder:
      %s

      %s
      The manager folder needs write permission.

      %s

      Move or unpack Fearless into a folder with write permission, then start
      Fearless again.""".formatted(path(dir.toString()), managerFolderIntro(), reported(cause)), cause);
  }
  public static UserError couldNotUseInstanceLock(Path lockFile, Throwable cause){
    return new UserError("""
      Fearless could not use the file that marks the manager process.

      %s
      While a manager process runs, it keeps this file reserved, so that any
      new Fearless process can tell that a manager already exists. This
      feature is called file locking. Fearless tried to open and reserve:
      %s

      %s

      Common fixes are to use a manager folder with write permission, or to
      close another program that is using or blocking this folder.
      %s
      """.formatted(managerFolderIntro(), path(lockFile.toString()), reported(cause), blockingPrograms()), cause);
  }
  public static UserError couldNotLeaveStartMessage(Path msgDir, Throwable cause){
    return new UserError("""
      Fearless could not leave its start message.

      %s
      Every starting Fearless process writes one small message file into this
      folder:
      %s
      The manager process (an already running one, or the process that is
      starting right now) then reads and removes those files. The message
      file was written, but renaming it to its final name failed.

      %s""".formatted(managerFolderIntro(), path(msgDir.toString()), reported(cause)), cause);
  }
  public static String willWipe(Path managerDir){
    return """


      Once this dialog is closed, Fearless deletes its manager folder
      %s
      and removes every file association of Fearless.
      %s""".formatted(path(managerDir.toString()), freshCopyThenReport());
  }
  public static UserError notWiped(Path managerDir, Throwable cause){
    return new UserError("""
      Fearless could not delete its manager folder
      %s
      and remove every file association of Fearless.
      %s

      Delete the manager folder.
      %s""".formatted(path(managerDir.toString()), reported(cause), freshCopyThenReport()), cause);
  }
  public static UserError couldNotWatchMessageFolder(Path msgDir, Throwable cause){
    return new UserError("""
      Fearless could not watch its manager folder.

      %s
      Fearless asked the operating system to tell it when files appear in this
      folder:
      %s
      This feature is called file-change notifications, and the request failed.

      %s""".formatted(managerFolderIntro(), path(msgDir.toString()), reported(cause)), cause);
  }
  public static UserError messageFolderNotWatchable(Path msgDir){
    return new UserError("""
      Fearless can no longer watch its manager folder.

      %s
      Fearless was watching this folder:
      %s
      Fearless could watch it when the manager process started, but cannot
      anymore. Watching a folder means asking the operating system to tell
      Fearless when files appear in it (file-change notifications).

      The folder may have been removed, replaced, or disconnected, or the
      operating system may have stopped sending file-change notifications
      for it.""".formatted(managerFolderIntro(), path(msgDir.toString())));
  }
  public static UserError couldNotDrainMessageFolder(Path msgDir, Throwable cause){
    return new UserError("""
      Fearless could not list its manager folder, or could not read or
      remove a message file in it.

      The manager folder is:
      %s
      A file may have been deleted, locked, or changed while Fearless was
      using it, or the folder itself may be blocked.

      %s""".formatted(path(msgDir.toString()), reported(cause)), cause);
  }
  public static UserError couldNotSaveManagerFile(Path file, Throwable cause){
    return new UserError("""
      Fearless could not save the file "%s" of its manager folder.

      %s
      The manager folder is:
      %s
      The change was not recorded, so Fearless will not remember it.

      %s
      %s""".formatted(file.getFileName(), managerFolderIntro(), path(file.getParent().toString()), reported(cause), blockingPrograms()), cause);
  }
}

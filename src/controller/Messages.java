package controller;

import static userMessages.UserError.path;
import static userMessages.Violation.freshCopyThenReport;
import static userMessages.Violation.reported;

import java.nio.file.Path;
import java.util.List;

import userMessages.UserError;
import utils.Join;

/// The texts only the manager raises, of the two kinds of userMessages.UserError: what the
/// user gave the manager that it cannot accept, and what the manager can no longer do.
public final class Messages{
  private Messages(){}
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

  //-- what the user asked the manager, and it cannot accept; a String is a note, shown and not thrown
  public static UserError folderNestedWithRegistered(Path folder, Path registered){
    return new UserError("""
      Fearless cannot keep track of this project folder.

      You started Fearless on:
      %s
      Fearless is already keeping track of:
      %s
      One of the two is inside the other. Fearless keeps track of project folders
      that do not overlap, so that every file belongs to exactly one project.

      Use the folder Fearless already keeps track of, or make Fearless forget that
      folder first, and then start Fearless on this one.
      """.formatted(path(folder.toString()),path(registered.toString())));
  }
  public static UserError projectNamed(Path folder, String wanted, String alias){
    return new UserError("""
      Fearless keeps track of this project folder as "%s", not as "%s".

      You started Fearless on:
      %s
      A project name uses only lowercase letters, digits and underscores,
      starts with a letter or an underscore, and is not the name of another
      project Fearless keeps track of.

      The marker file "%s%s" in that folder holds the name: rename it to change the name.
      """.formatted(alias,wanted,path(folder.toString()),alias,".fearless"));
  }
  public static UserError managerFolderNotAProject(Path given, Path managerDir){
    return new UserError("""
      Fearless cannot keep track of this folder as a project.

      You started Fearless on:
      %s
      The manager folder of this Fearless is:
      %s
      The manager folder holds what Fearless remembers about your projects: it is
      never part of a project, and no project is inside it.
      """.formatted(path(given.toString()),path(managerDir.toString())));
  }
  public static UserError projectFolderIsRoot(Path root){
    return new UserError("""
      Fearless cannot keep track of the root of a drive or of the file system as a
      project.

      You started Fearless on:
      %s
      Put the project in a folder inside it, and start Fearless on that folder.
      """.formatted(path(root.toString())));
  }
  public static UserError projectIconsMany(Path dir, List<Path> found){
    return new UserError("""
      More than one .png file was found for this project's icon.

      Looked in:
      %s

      Found:
      %s

      Keep exactly one .png file there.
      """.formatted(path(dir.toString()),Join.of(found.stream().map(p->"  "+p.getFileName()),"","\n","","")));
  }
  public static UserError projectIconUnreadable(Path icon){
    return new UserError("""
      The icon of this project is not a PNG image Fearless can read.

      File:
      %s

      The icon of a project is the only .png file in its ".config/icon" folder:
      replace that file with a PNG image, or remove it.
      """.formatted(path(icon.toString())));
  }
  public static UserError infoError(String rendered){ return new UserError(rendered); }
  public static String kindReset(String alias){
    return "In projects.info the \"kind\" of \""+alias+"\" was missing or not one of the kinds: \""+alias+"\" is now idle, and its compiled cache is deleted.";
  }
  public static String tooManyLines(String message){
    return "The manager was sent a message of "+message.split("\n",-1).length+" lines, but a message is empty, to show the window, or a path, or a request of two or three lines: a verb, then a project name (a path for \"register\"), then for some verbs a third line:\n"+message;
  }
  public static String unknownVerb(String verb, List<String> verbs){
    return "The manager was asked to \""+verb+"\", but that is not a request it knows: the requests are "+Join.of(verbs.stream().map(v->"\""+v+"\""),"",", ","")+".";
  }
  public static String unknownProject(String verb, String name, List<String> names){
    return "The manager was asked to \""+verb+"\" the project \""+name+"\", but no project is named \""+name+"\"."+Join.of(names.stream().map(n->"\n  "+n),"\nThe projects are:","","","\nNo project is registered.");
  }
  public static String registerNoFolder(){ return "The manager was asked to register a folder, but the message names no folder."; }
  public static String unknownKind(Path folder, String text){
    return "The manager was asked to change the kind of\n"+folder+"\nto \""+text+"\", but the kinds are \"idle\", \"code\", \"data:readOnly\" and \"data:readWrite\".";
  }
  public static String malformedLink(String alias, String link){
    return "The manager was asked to link \""+alias+"\" with \""+link+"\", but a link is a project name, then \"read\" or \"write\", then the type names, none to remove the link.";
  }
  public static String noEclipse(Path dir){
    return "Eclipse is not connected: no Eclipse installation, a folder holding the file \".eclipseproduct\", is in\n  "+dir+"\nor in its folders \"eclipse\" or \"Contents/Eclipse\", or in those of a folder of it.\n\nSelect the Eclipse program, the folder holding it, or the folder Eclipse was unzipped into.";
  }
  public static String severalEclipses(Path dir, List<Path> found){
    return "Eclipse is not connected: more than one Eclipse installation is in\n  "+dir+Join.of(found.stream().map(f->"\n  "+f),"\nThey are:","","")+"\n\nSelect the Eclipse program, or the folder holding it, of the one to connect.";
  }
  public static String eclipseConnected(Path eclipse){
    return """
      Eclipse is now connected:
      %s

      Restart Eclipse: every project this manager knows appears in its Fearless
      perspective. File > New makes a project, Project > Build compiles it, the
      Run button runs it, and the Terminate button of its console stops it.
      """.formatted(eclipse);
  }

  //-- what the manager can no longer do
  public static UserError programFolderNotFound(Path startedFrom, String expectedDirName){
    return new UserError("""
      This copy of Fearless appears to have been moved, renamed, or damaged:
      no folder named "%s" exists above
      %s

      %s""".formatted(expectedDirName,path(startedFrom.toString()),freshCopyThenReport()));
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

      This desktop offers no system tray, and the tray icon is how a closed
      manager window is brought back.
      Enable the system tray of this desktop, then start Fearless again.
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
      packaged Fearless code could not be linked correctly.

      %s""".formatted(freshCopyThenReport()), cause);
  }
  public static UserError couldNotLoadIcon(Path icon, Throwable cause){
    return new UserError("""
      Fearless could not load its own icon.

      This copy of Fearless should already have this file:
      %s

      %s

      %s""".formatted(path(icon.toString()),reported(cause),freshCopyThenReport()), cause);
  }
  public static UserError couldNotDecodeIcon(Path icon){
    return new UserError("""
      Fearless could not load its own icon.

      This copy of Fearless should already have this file:
      %s
      The file is there and could be read, but it does not hold an image this
      Java runtime can decode.

      %s""".formatted(path(icon.toString()),freshCopyThenReport()));
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
      Fearless again.""".formatted(path(dir.toString()),managerFolderIntro(),reported(cause)), cause);
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
      """.formatted(managerFolderIntro(),path(lockFile.toString()),reported(cause),blockingPrograms()), cause);
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

      %s

      A manager process may still be running: do not delete the manager
      folder. If the problem repeats, close programs that may be using or
      blocking the folder.
      %s""".formatted(managerFolderIntro(),path(msgDir.toString()),reported(cause),blockingPrograms()), cause);
  }
  public static UserError couldNotWatchMessageFolder(Path msgDir, Throwable cause){
    return new UserError("""
      Fearless could not watch its manager folder.

      %s
      Fearless asked the operating system to tell it when files appear in this
      folder:
      %s
      This feature is called file-change notifications, and the request failed.

      %s""".formatted(managerFolderIntro(),path(msgDir.toString()),reported(cause)), cause);
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
      for it.""".formatted(managerFolderIntro(),path(msgDir.toString())));
  }
  public static UserError couldNotDrainMessageFolder(Path msgDir, Throwable cause){
    return new UserError("""
      Fearless could not list its manager folder, or could not remove a
      message file from it.

      The manager folder is:
      %s
      A file may have been deleted, locked, or changed while Fearless was
      using it, or the folder itself may be blocked.

      %s""".formatted(path(msgDir.toString()),reported(cause)), cause);
  }
  public static UserError couldNotSaveRegisteredFolders(Path managerDir, Throwable cause){
    return new UserError("""
      Fearless could not save what it remembers about your project folders.

      %s
      The manager folder is:
      %s
      The change was not recorded, so Fearless will not remember it.

      %s
      %s""".formatted(managerFolderIntro(),path(managerDir.toString()),reported(cause),blockingPrograms()), cause);
  }
}

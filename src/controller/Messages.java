package controller;

import static userMessages.UserError.disp;
import static userMessages.UserError.path;
import static userMessages.Violation.freshCopyThenReport;
import static userMessages.Violation.reported;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

import coordinator.MainsInfo;
import tools.Fs;
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
  public static String quoted(List<String> texts){ return Join.of(texts.stream().map(t->"\""+t+"\""),"",", ",""); }
  private static String blockingPrograms(){ return """
    Programs that may use or block this folder include security software
    (antivirus, ransomware protection, endpoint protection), backup tools,
    sync tools, and file preview tools.
    """;
  }

  //-- what the user asked the manager, and it cannot accept; a String is a note, shown and not thrown
  public static String folderNestedWithRegistered(Path folder, Path registered){
    return """
      Fearless cannot keep track of this project folder.

      The manager was asked to register:
      %s
      Fearless is already keeping track of:
      %s
      One of the two is inside the other. Fearless keeps track of project folders
      that do not overlap, so that every file belongs to exactly one project.

      Use the folder Fearless already keeps track of, or make Fearless forget that
      folder first, and then register this one again.
      """.formatted(path(folder.toString()),path(registered.toString()));
  }
  public static String projectNamed(Path folder, String wanted, String alias){
    return """
      Fearless keeps track of this project folder as "%s", not as "%s".

      The manager registered:
      %s
      A project name uses only lowercase letters, digits and underscores,
      starts with a letter or an underscore, is not a name the file system
      reserves ("con", "prn", "aux", "nul", "com1" to "com9", "lpt1" to "lpt9"),
      and is not the name of another project Fearless keeps track of.

      The marker file "%s%s" in that folder holds the name: rename it to change the name.
      """.formatted(alias,wanted,path(folder.toString()),alias,".fearless");
  }
  public static String managerFolderNotAProject(Path folder, Path managerDir){
    return """
      Fearless cannot keep track of this folder as a project.

      The folder is:
      %s
      The manager folder of this Fearless is:
      %s
      The manager folder holds what Fearless remembers about your projects: it is
      never part of a project, and no project is inside it.
      """.formatted(path(folder.toString()),path(managerDir.toString()));
  }
  public static String projectFolderIsRoot(Path root){
    return """
      Fearless cannot keep track of the root of a drive or of the file system as a
      project.

      The folder is:
      %s
      Put the project in a folder inside it, and make that folder the project
      folder.
      """.formatted(path(root.toString()));
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
  public static String kindReset(String alias, Optional<String> cacheKept){
    return "In projects.info the \"kind\" of \""+alias+"\" was missing or not one of the kinds: \""+alias+"\" is now idle, and its compiled cache is "+cacheKept.map(w->"not deleted: "+w).orElse("deleted")+".";
  }
  public static String fileFailure(IOException e){
    return e.getClass().getSimpleName().replaceFirst("Exception$","").replaceAll("(?<=[a-z])(?=[A-Z])"," ").toLowerCase(Locale.ROOT)+": "+e.getMessage();
  }
  public static String registerRefused(Path folder, IOException e){
    return "The manager was asked to register\n"+folder+"\nbut could not read or change it: "+fileFailure(e)+"\nGive Fearless access to the folder, then register it again.";
  }
  public static String tooManyLines(String message){
    return "The manager was sent a message of "+message.split("\n",-1).length+" lines, but a message is empty, to show the window, or a path, or a request of two or three lines: a verb, then a project name (a path for \"register\"), then for some verbs a third line:\n"+message;
  }
  public static String unknownVerb(String verb, List<String> verbs){
    return "The manager was asked to "+disp(verb)+", but that is not a request it knows: the requests are "+quoted(verbs)+".";
  }
  public static String unknownProject(String verb, String name, List<String> names){
    return "The manager was asked to "+disp(verb)+" the project "+disp(name)+", but no project is named "+disp(name)+"."+Join.of(names.stream().map(n->"\n  "+n),"\nThe projects are:","","","\nNo project is registered.");
  }
  public static String unreadableMessage(Path file){ return "The manager refused a message, and removed its file: a message is UTF-8 text the manager can read.\nThe bytes of\n"+file+"\ndo not form valid UTF-8 text."; }
  public static String metadataChanged(){ return "The project metadata is not committed: projects.info changed while it was edited.\nClose the editor, and choose Edit project metadata again to edit what projects.info holds now."; }
  public static String registerNoFolder(){ return "The manager was asked to register a folder, but the message names no folder."; }
  public static String registerNotTagged(String why){ return "The manager was asked to register a folder, but the path in the message is not a tagged text: "+why; }
  public static String registerNotAPath(String given, String why){ return "The manager was asked to register "+disp(given)+", but that is not a path this system accepts: "+why+"."; }
  public static String registerNothing(Path path){ return "The manager was asked to register\n"+path+"\nbut nothing exists there: register an existing folder, or a file inside one."; }
  public static String unknownKind(Path folder, String text){
    return "The manager was asked to change the kind of\n"+folder+"\nto "+disp(text)+", but the kinds are \"idle\", \"code\", \"data:readOnly\" and \"data:readWrite\".";
  }
  public static String malformedLink(String alias, String link){
    return "The manager was asked to link \""+alias+"\" with "+disp(link)+", but a link is a project name, then \"read\" or \"write\", then the type names, none to remove the link.";
  }
  public static String thirdLineRefused(String verb, String name, String third, List<String> takers){
    return "The manager was asked to "+disp(verb)+" "+disp(name)+" with the third line "+disp(third)+", but only the requests "+quoted(takers)+" take a third line.";
  }
  public static String dropRefused(String item, String why){ return "The manager was asked to register "+disp(item)+", dropped on its window, but "+why+"."; }
  public static String dropUnreadable(Exception e){ return "The manager was asked to register what was dropped on its window, but the desktop did not hand it over: "+e.getMessage(); }
  public static UserError noFreeExtension(String main, boolean shortcut, String icon){
    var kind= shortcut ? "Shortcut" : "OpenWith";
    var prefix= shortcut ? "fapp" : "ffile";
    var name= main.substring(main.lastIndexOf('.')+1).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]","");
    var example= name.isEmpty() || name.equals("fearless") ? "ext" : name.substring(0,Math.min(name.length(),Fs.maxExtSeg));
    return new UserError("No free extension is left for \"base."+kind+"["+icon+"]\" of main "+disp(main)+": all the 1000 extensions \""+prefix+"000\" to \""+prefix+"999\" are used by the projects of this manager. Give this "+kind+" an explicit extension, for example \"base."+kind+"["+icon+",\\\""+example+"\\\"]\".");
  }
  public static UserError shortcutNoFileName(String main){
    return new UserError("Main "+disp(main)+" can not have a Shortcut: its name has no matching file name. A main with a Shortcut has a name that can be mapped to a file name, like \"FooBar\" can be mapped to \"foo_bar\".");
  }
  public static UserError shortcutBadFileName(String main, String file){
    return new UserError("Main "+disp(main)+" can not have the shortcut file "+disp(file)+": a file name in a project is at most 200 characters long, and its part before the dot is not \"con\", \"prn\", \"aux\", \"nul\", \"com1\"..\"com9\" or \"lpt1\"..\"lpt9\", reserved on Windows. Give this main another name.");
  }
  public static UserError shortcutExtRefused(String main, String file){
    var why= file.endsWith(".zip") ? "a \".zip\" file of a project is read as a zip archive, and a shortcut file is not a zip archive" : "a \".fear\" file of a project is a source file, and it must be inside a package folder";
    return new UserError("Main "+disp(main)+" can not have the shortcut file "+disp(file)+": "+why+". Give this Shortcut another extension.");
  }
  public static UserError shortcutMasksFile(String main, String file, String name){
    return new UserError("Main "+disp(main)+" can not have the shortcut file "+disp(file)+": the project has the file "+disp(name)+", and a file without extension can not share its name with a file with an extension. Rename the main, or rename the file "+disp(name)+".");
  }
  public static UserError shortcutsCollide(String main1, String main2, String file){
    return new UserError("Mains "+disp(main1)+" and "+disp(main2)+" can not both have the shortcut file "+disp(file)+": a shortcut file is named by the name of its main and the extension of its Shortcut. Rename one of the two mains, or give one of their Shortcuts another extension.");
  }
  public static String iconGone(Path file){ return "The icon file of a claim of this project is gone:\n"+file+"\nThe project claims no extension until it is compiled again."; }
  public static String claimsNotSaved(IOException e){ return "The icons and the extensions of the claims of this project can not be saved in its compiled cache: "+fileFailure(e)+"\nGive Fearless access to the folder, then compile the project again."; }
  public static String claimedBy(List<Project.Claimant> cs){ return Join.of(cs.stream().map(c->disp(c.main())+" of project "+disp(c.alias()))," claimed by "," and ","",""); }
  public static UserError iconRefused(String main, boolean shortcut, MainsInfo.Claim c, String problem){
    return new UserError(icon(main,shortcut,c)+" "+problem+" (from "+disp(from(c))+"): an icon must be a square PNG image with a side from 64 to 1024 pixels.");
  }
  public static UserError iconUnreadable(String main, boolean shortcut, MainsInfo.Claim c, IOException e){
    return new UserError(icon(main,shortcut,c)+" can not be read from "+disp(from(c))+": "+fileFailure(e)+"\nCompile the project again.");
  }
  private static String icon(String main, boolean shortcut, MainsInfo.Claim c){
    var claim= "\"base."+(shortcut ? "Shortcut" : "OpenWith")+"["+c.icon()+(c.extension().isEmpty() ? "" : ",\\\""+c.extension()+"\\\"")+"]\"";
    return "The icon "+disp(c.icon())+" in "+claim+" of main "+disp(main);
  }
  private static String from(MainsInfo.Claim c){ return Join.of(Stream.of(c.diskPath(),c.zipSteps().replace(';','/'),c.zipEntry()).filter(s->!s.isEmpty()),"","/",""); }
  public static UserError tooManyArguments(List<String> args){
    return new UserError("""
      Fearless was started with %d arguments, but it takes at most one.

      The arguments are:
      %s
      Start Fearless with no argument to show its window, or with one argument:
      a project folder, or a file inside one.
      """.formatted(args.size(),Join.of(args.stream().map(UserError::path),"","\n","","")));
  }
  public static String noEclipse(Path dir){
    return "Eclipse is not connected: no Eclipse installation, a folder holding the file \".eclipseproduct\", is in\n  "+dir+"\nor in its folders \"eclipse\" or \"Contents/Eclipse\", or in those of a folder of it.\n\nSelect the Eclipse program, the folder holding it, or the folder Eclipse was unzipped into.";
  }
  public static String severalEclipses(Path dir, List<Path> found){
    return "Eclipse is not connected: more than one Eclipse installation is in\n  "+dir+Join.of(found.stream().map(f->"\n  "+f),"\nThey are:","","")+"\n\nSelect the Eclipse program, or the folder holding it, of the one to connect.";
  }
  public static String eclipseNotConnected(IOException e){ return "Eclipse is not connected: "+fileFailure(e); }
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

      %s""".formatted(path(icon.toString()),reported(cause)), cause);
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

      %s""".formatted(managerFolderIntro(),path(msgDir.toString()),reported(cause)), cause);
  }
  public static String wiped(Path managerDir){
    return """


      Fearless deleted its manager folder
      %s
      and removed every file association of Fearless.
      %s""".formatted(path(managerDir.toString()),freshCopyThenReport());
  }
  public static String notWiped(Path managerDir, Throwable cause){
    return """


      Fearless could not delete its manager folder
      %s
      and remove every file association of Fearless.
      %s

      Delete the manager folder.
      %s""".formatted(path(managerDir.toString()),reported(cause),freshCopyThenReport());
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
      Fearless could not list its manager folder, or could not read or
      remove a message file in it.

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

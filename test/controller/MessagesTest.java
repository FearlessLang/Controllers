package controller;

import static controller.Errs.same;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import managerMessages.Report;
import managerMessages.Violation;

final class MessagesTest{
  @Test void programFolderNotFound(){
    var startedFrom= Path.of("C:\\Users\\ada\\Desktop\\app");
    same("""
This copy of Fearless appears to have been moved, renamed, or damaged:
no folder named "fearlessManaged0_007" exists above
  C:\\Users\\ada\\Desktop\\app

Replace this Fearless folder with a fresh copy.
If this keeps happening, report the problem.
""", Violation.programFolderNotFound(startedFrom, "fearlessManaged0_007").getMessage());
  }
  @Test void couldNotStartGui(){
    var cause= new RuntimeException("No X11 DISPLAY variable was set, but this program performed an operation which requires it.");
    same("""
Fearless could not open its window.

Fearless needs to show a window, but opening the window failed.

Reported reason:
No X11 DISPLAY variable was set, but this program performed an operation which requires it.""", Violation.couldNotStartGui(cause).getMessage());
  }
  @Test void vmOrLinkageFailure(){
    var cause= new LinkageError("loader constraint violation: loader previously initiated loading for coordinator.Coordinator");
    same("""
Fearless crashed because of a low-level JVM failure.

Fearless includes its own packaged JVM. The packaged JVM failed, or the
packaged Fearless code could not be linked correctly.
""", Violation.vmOrLinkageFailure(cause).getMessage());
  }
  @Test void couldNotLoadIcon(){
    var icon= Path.of("C:\\Program Files\\FearlessManaged0_007\\app\\icon.png");
    var cause= new IOException("The process cannot access the file because it is being used by another process");
    same("""
Fearless could not load its own icon.

This copy of Fearless should already have this file:
  C:\\Program Files\\FearlessManaged0_007\\app\\icon.png

Reported reason:
The process cannot access the file because it is being used by another process
""", Violation.couldNotLoadIcon(icon, cause).getMessage());
  }
  @Test void couldNotDecodeIcon(){
    var icon= Path.of("C:\\Users\\ada\\Downloads\\fearlessManaged0_007\\app\\icon.png");
    same("""
Fearless could not load its own icon.

This copy of Fearless should already have this file:
  C:\\Users\\ada\\Downloads\\fearlessManaged0_007\\app\\icon.png
The file is there and could be read, but it does not hold an image this
Java runtime can decode.
""", Violation.couldNotDecodeIcon(icon).getMessage());
  }
  @Test void couldNotCreateManagerFolder(){
    var dir= Path.of("C:\\Users\\ada\\AppData\\Local\\Fearless\\manager");
    var cause= new IOException("Access is denied");
    same("""
Fearless could not create its manager folder.

Fearless tried to create this folder:
  C:\\Users\\ada\\AppData\\Local\\Fearless\\manager

Fearless uses one process (called the manager process)
to keep track of other Fearless processes (the user processes).
The manager folder is the folder used by the manager process to store
coordination information into files.

The manager folder needs write permission.

Reported reason:
Access is denied

Move or unpack Fearless into a folder with write permission, then start
Fearless again.""", Violation.couldNotCreateManagerFolder(dir, cause).getMessage());
  }
  @Test void couldNotUseInstanceLockSanitizesNonAsciiUserName(){
    var lockFile= Path.of("C:\\Users\\caf\u00e9\\AppData\\Local\\Fearless\\manager\\lock");
    var cause= new IOException("The process cannot access the file because it is being used by another process");
    same("""
Fearless could not use the file that marks the manager process.

Fearless uses one process (called the manager process)
to keep track of other Fearless processes (the user processes).
The manager folder is the folder used by the manager process to store
coordination information into files.

While a manager process runs, it keeps this file reserved, so that any
new Fearless process can tell that a manager already exists. This
feature is called file locking. Fearless tried to open and reserve:
  C:\\Users\\caf?\\AppData\\Local\\Fearless\\manager\\lock

Reported reason:
The process cannot access the file because it is being used by another process

Common fixes are to use a manager folder with write permission, or to
close another program that is using or blocking this folder.
Programs that may use or block this folder include security software
(antivirus, ransomware protection, endpoint protection), backup tools,
sync tools, and file preview tools.

""", Violation.couldNotUseInstanceLock(lockFile, cause).getMessage());
  }
  @Test void couldNotLeaveStartMessage(){
    var msgDir= Path.of("C:\\Users\\ada\\AppData\\Local\\Fearless\\manager\\messages");
    var cause= new IOException("Cannot create a file when that file already exists");
    same("""
Fearless could not leave its start message.

Fearless uses one process (called the manager process)
to keep track of other Fearless processes (the user processes).
The manager folder is the folder used by the manager process to store
coordination information into files.

Every starting Fearless process writes one small message file into this
folder:
  C:\\Users\\ada\\AppData\\Local\\Fearless\\manager\\messages
The manager process (an already running one, or the process that is
starting right now) then reads and removes those files. The message
file was written, but renaming it to its final name failed.

Reported reason:
Cannot create a file when that file already exists""", Violation.couldNotLeaveStartMessage(msgDir, cause).getMessage());
  }
  @Test void willWipe(){
    same("""


Once this dialog is closed, Fearless deletes its manager folder
  C:\\Users\\ada\\Downloads\\fearless0_007
and removes every file association of Fearless.
Replace this Fearless folder with a fresh copy.
If this keeps happening, report the problem.
""", Violation.willWipe(Path.of("C:\\Users\\ada\\Downloads\\fearless0_007")));
  }
  @Test void notWiped(){
    same("""
Fearless could not delete its manager folder
  C:\\Users\\ada\\Downloads\\fearless0_007
and remove every file association of Fearless.
Reported reason:
The process cannot access the file because it is being used by another process

Delete the manager folder.
Replace this Fearless folder with a fresh copy.
If this keeps happening, report the problem.
""", Violation.notWiped(Path.of("C:\\Users\\ada\\Downloads\\fearless0_007"), new IOException("The process cannot access the file because it is being used by another process")).getMessage());
  }
  @Test void couldNotWatchMessageFolder(){
    var msgDir= Path.of("C:\\Users\\ada\\AppData\\Local\\Fearless\\manager\\messages");
    var cause= new IOException("The I/O operation has been aborted because of either a thread exit or an application request");
    same("""
Fearless could not watch its manager folder.

Fearless uses one process (called the manager process)
to keep track of other Fearless processes (the user processes).
The manager folder is the folder used by the manager process to store
coordination information into files.

Fearless asked the operating system to tell it when files appear in this
folder:
  C:\\Users\\ada\\AppData\\Local\\Fearless\\manager\\messages
This feature is called file-change notifications, and the request failed.

Reported reason:
The I/O operation has been aborted because of either a thread exit or an application request""", Violation.couldNotWatchMessageFolder(msgDir, cause).getMessage());
  }
  @Test void newFilesNotReported(){
    var msgDir= Path.of("C:\\Users\\ada\\AppData\\Local\\Fearless\\manager\\messages");
    same("""
Fearless gets no file-change notifications for its manager folder.

Fearless uses one process (called the manager process)
to keep track of other Fearless processes (the user processes).
The manager folder is the folder used by the manager process to store
coordination information into files.

Fearless asked the operating system to tell it when files appear in this
folder:
  C:\\Users\\ada\\AppData\\Local\\Fearless\\manager\\messages
Fearless created a file in it, and no notification came within 5 seconds.
Some drives, for example some network drives, send no such notifications.

Put this Fearless folder on a disk of this computer.""", Violation.newFilesNotReported(msgDir).getMessage());
  }
  @Test void messageFolderNotWatchable(){
    var msgDir= Path.of("C:\\Users\\ada\\AppData\\Local\\Fearless\\manager\\messages");
    same("""
Fearless can no longer watch its manager folder.

Fearless uses one process (called the manager process)
to keep track of other Fearless processes (the user processes).
The manager folder is the folder used by the manager process to store
coordination information into files.

Fearless was watching this folder:
  C:\\Users\\ada\\AppData\\Local\\Fearless\\manager\\messages
Fearless could watch it when the manager process started, but cannot
anymore. Watching a folder means asking the operating system to tell
Fearless when files appear in it (file-change notifications).

The folder may have been removed, replaced, or disconnected, or the
operating system may have stopped sending file-change notifications
for it.""", Violation.messageFolderNotWatchable(msgDir).getMessage());
  }
  @Test void couldNotDrainMessageFolder(){
    var msgDir= Path.of("C:\\Users\\ada\\AppData\\Local\\Fearless\\manager");
    var cause= new IOException("The system cannot find the file specified");
    same("""
Fearless could not list its manager folder, or could not read or
remove a message file in it.

The manager folder is:
  C:\\Users\\ada\\AppData\\Local\\Fearless\\manager
A file may have been deleted, locked, or changed while Fearless was
using it, or the folder itself may be blocked.

Reported reason:
The system cannot find the file specified""", Violation.couldNotDrainMessageFolder(msgDir, cause).getMessage());
  }
  @Test void couldNotSaveManagerFile(){
    var file= Path.of("C:\\Users\\ada\\AppData\\Local\\Fearless\\manager").resolve("extensions.info");
    var cause= new IOException("There is not enough space on the disk");
    same("""
Fearless could not save the file "extensions.info" of its manager folder.

Fearless uses one process (called the manager process)
to keep track of other Fearless processes (the user processes).
The manager folder is the folder used by the manager process to store
coordination information into files.

The manager folder is:
  C:\\Users\\ada\\AppData\\Local\\Fearless\\manager
The change was not recorded, so Fearless will not remember it.

Reported reason:
There is not enough space on the disk
Programs that may use or block this folder include security software
(antivirus, ransomware protection, endpoint protection), backup tools,
sync tools, and file preview tools.
""", Violation.couldNotSaveManagerFile(file, cause).getMessage());
  }
  @Test void folderNestedWithRegistered(){
    same("""
Fearless cannot keep track of this project folder.

The manager was asked to register:
  C:\\Users\\ada\\hello\\inner
Fearless is already keeping track of:
  C:\\Users\\ada\\hello
One of the two is inside the other. Fearless keeps track of project folders
that do not overlap, so that every file belongs to exactly one project.

Use the folder Fearless already keeps track of, or make Fearless forget that
folder first, and then register this one again.
""", Report.folderNestedWithRegistered(Path.of("C:\\Users\\ada\\hello\\inner"), Path.of("C:\\Users\\ada\\hello")));
  }
  @Test void managerFolderNotAProject(){
    same("""
Fearless cannot keep track of this folder as a project.

The folder is:
  C:\\Users\\ada\\fearless0_007\\messages
The manager folder of this Fearless is:
  C:\\Users\\ada\\fearless0_007
The manager folder holds what Fearless remembers about your projects: it is
never part of a project, and no project is inside it.
""", Report.managerFolderNotAProject(Path.of("C:\\Users\\ada\\fearless0_007\\messages"), Path.of("C:\\Users\\ada\\fearless0_007")));
  }
  @Test void projectFolderIsRoot(){
    same("""
Fearless cannot keep track of the root of a drive or of the file system as a
project.

The folder is:
  C:\\
Put the project in a folder inside it, and make that folder the project
folder.
""", Report.projectFolderIsRoot(Path.of("C:\\")));
  }
  @Test void trayConnectionLost(){
    same("""
The desktop stopped talking to the Fearless icon in the system tray.

The tray icon is how a closed manager window is brought back, and the
connection to the system tray of this desktop failed.

Reported reason:
the session bus closed the connection
Restore the system tray of this desktop, then start Fearless again.
""", Violation.trayConnectionLost(new IOException("the session bus closed the connection")).getMessage());
  }
}

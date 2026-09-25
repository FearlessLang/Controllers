package controller;

import static controller.Errs.same;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

final class MessagesTest{
  @Test void programFolderNotFound(){
    var startedFrom= Path.of("C:\\Users\\ada\\Desktop\\app");
    same("""
This copy of Fearless appears to have been moved, renamed, or damaged:
no folder named "fearlessManaged0_007" exists above
  C:\\Users\\ada\\Desktop\\app

Replace this Fearless folder with a fresh copy.
If this keeps happening, report the problem.
""", Messages.programFolderNotFound(startedFrom, "fearlessManaged0_007").getMessage());
  }
  @Test void couldNotStartGui(){
    var cause= new RuntimeException("No X11 DISPLAY variable was set, but this program performed an operation which requires it.");
    same("""
Fearless could not open its window.

Fearless needs to show a window, but opening the window failed.

Reported reason:
No X11 DISPLAY variable was set, but this program performed an operation which requires it.""", Messages.couldNotStartGui(cause).getMessage());
  }
  @Test void vmOrLinkageFailure(){
    var cause= new LinkageError("loader constraint violation: loader previously initiated loading for coordinator.Coordinator");
    same("""
Fearless crashed because of a low-level JVM failure.

Fearless includes its own packaged JVM. The packaged JVM failed, or the
packaged Fearless code could not be linked correctly.

Replace this Fearless folder with a fresh copy.
If this keeps happening, report the problem.
""", Messages.vmOrLinkageFailure(cause).getMessage());
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

Replace this Fearless folder with a fresh copy.
If this keeps happening, report the problem.
""", Messages.couldNotLoadIcon(icon, cause).getMessage());
  }
  @Test void couldNotDecodeIcon(){
    var icon= Path.of("C:\\Users\\ada\\Downloads\\fearlessManaged0_007\\app\\icon.png");
    same("""
Fearless could not load its own icon.

This copy of Fearless should already have this file:
  C:\\Users\\ada\\Downloads\\fearlessManaged0_007\\app\\icon.png
The file is there and could be read, but it does not hold an image this
Java runtime can decode.

Replace this Fearless folder with a fresh copy.
If this keeps happening, report the problem.
""", Messages.couldNotDecodeIcon(icon).getMessage());
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
Fearless again.""", Messages.couldNotCreateManagerFolder(dir, cause).getMessage());
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

""", Messages.couldNotUseInstanceLock(lockFile, cause).getMessage());
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
Cannot create a file when that file already exists

A manager process may still be running: do not delete the manager
folder. If the problem repeats, close programs that may be using or
blocking the folder.
Programs that may use or block this folder include security software
(antivirus, ransomware protection, endpoint protection), backup tools,
sync tools, and file preview tools.
""", Messages.couldNotLeaveStartMessage(msgDir, cause).getMessage());
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
The I/O operation has been aborted because of either a thread exit or an application request""", Messages.couldNotWatchMessageFolder(msgDir, cause).getMessage());
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
for it.""", Messages.messageFolderNotWatchable(msgDir).getMessage());
  }
  @Test void couldNotDrainMessageFolder(){
    var msgDir= Path.of("C:\\Users\\ada\\AppData\\Local\\Fearless\\manager");
    var cause= new IOException("The system cannot find the file specified");
    same("""
Fearless could not list its manager folder, or could not remove a
message file from it.

The manager folder is:
  C:\\Users\\ada\\AppData\\Local\\Fearless\\manager
A file may have been deleted, locked, or changed while Fearless was
using it, or the folder itself may be blocked.

Reported reason:
The system cannot find the file specified""", Messages.couldNotDrainMessageFolder(msgDir, cause).getMessage());
  }
  @Test void couldNotSaveRegisteredFolders(){
    var managerDir= Path.of("C:\\Users\\ada\\AppData\\Local\\Fearless\\manager");
    var cause= new IOException("There is not enough space on the disk");
    same("""
Fearless could not save what it remembers about your project folders.

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
""", Messages.couldNotSaveRegisteredFolders(managerDir, cause).getMessage());
  }
}

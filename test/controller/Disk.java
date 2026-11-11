package controller;

import static java.nio.file.attribute.AclEntryPermission.APPEND_DATA;
import static java.nio.file.attribute.AclEntryPermission.DELETE;
import static java.nio.file.attribute.AclEntryPermission.DELETE_CHILD;
import static java.nio.file.attribute.AclEntryPermission.READ_DATA;
import static java.nio.file.attribute.AclEntryPermission.WRITE_DATA;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.UncheckedIOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


import tools.Fs;

final class Disk{
  private static boolean junction;
  static Path link(Path link, Path target){
    try{ return Fs.of(() -> Files.createSymbolicLink(link, target)); }
    catch (UncheckedIOException e){
      var refused= Fs.isWindows() && e.getCause() instanceof FileSystemException;
      if (!refused){ throw e; }
      if (!Files.isDirectory(target)){ return fail("Agent on Windows: this process can not create symbolic links, so this test can not check them. Turn on Developer Mode (Settings, System, For developers), or run the tests with the privilege to create symbolic links."); }
      var p= Fs.of(new ProcessBuilder("cmd", "/c", "mklink", "/J", link.toString(), target.toString()).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD)::start);
      try{ assert p.waitFor() == 0; }
      catch (InterruptedException ie){ throw new AssertionError(ie); }
      junction= true;
      return link;
    }
  }
  static void checkedByHandOnWindows(){
    var made= junction;
    junction= false;
    assert !made: "This test was written on Linux, where a Windows junction can not be made: Disk.link made a junction (dir shows <JUNCTION>) because this process can not make symbolic links; on this Windows desk check by hand that the manager treated the junction as the test expects of a symbolic link; then remove this assert.";
  }
  static void noRead(Path p){
    if (!Fs.isWindows()){ posix(p, "---------"); return; }
    deny(p, Set.of(READ_DATA));
  }
  static void noWrite(Path p){
    if (!Fs.isWindows()){ posix(p, "r-xr-xr-x"); return; }
    deny(p, Set.of(WRITE_DATA, APPEND_DATA, DELETE_CHILD));
    children(p).forEach(c -> deny(c, Set.of(DELETE)));
  }
  static void allow(Path p){
    if (!Fs.isWindows()){ posix(p, "rwxr-xr-x"); return; }
    allowAcl(p);
    if (Files.isDirectory(p)){ children(p).forEach(Disk::allowAcl); }
  }
  private static void posix(Path p, String mode){ Fs.ofV(() -> Files.setPosixFilePermissions(p, PosixFilePermissions.fromString(mode))); }
  private static List<Path> children(Path dir){ return Fs.of(() -> { try (var s= Files.list(dir)){ return s.toList(); } }); }
  private static AclFileAttributeView acl(Path p){ return Files.getFileAttributeView(p, AclFileAttributeView.class); }
  private static void deny(Path p, Set<AclEntryPermission> denied){
    var user= Fs.of(() -> p.getFileSystem().getUserPrincipalLookupService().lookupPrincipalByName(System.getProperty("user.name")));
    var entries= new ArrayList<>(Fs.of(acl(p)::getAcl));
    entries.addFirst(AclEntry.newBuilder().setType(AclEntryType.DENY).setPrincipal(user).setPermissions(denied).build());
    Fs.ofV(() -> acl(p).setAcl(entries));
  }
  private static void allowAcl(Path p){
    var entries= Fs.of(acl(p)::getAcl).stream().filter(e -> e.type() != AclEntryType.DENY).toList();
    Fs.ofV(() -> acl(p).setAcl(entries));
  }
}

package controller;

import static java.nio.file.attribute.AclEntryPermission.APPEND_DATA;
import static java.nio.file.attribute.AclEntryPermission.DELETE;
import static java.nio.file.attribute.AclEntryPermission.DELETE_CHILD;
import static java.nio.file.attribute.AclEntryPermission.READ_DATA;
import static java.nio.file.attribute.AclEntryPermission.WRITE_DATA;

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

import org.junit.jupiter.api.Assumptions;

import tools.Fs;

final class Disk{
  static Path link(Path link, Path target){
    try{ return Fs.of(() -> Files.createSymbolicLink(link, target)); }
    catch (UncheckedIOException e){
      var refused= Fs.isWindows() && e.getCause() instanceof FileSystemException;
      if (!refused){ throw e; }
      return Assumptions.abort("Windows does not give this process the privilege to create symbolic links");
    }
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

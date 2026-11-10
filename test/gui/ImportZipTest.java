package gui;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opentest4j.AssertionFailedError;

import tools.Fs;
import userMessages.UserError;
import utils.Err;

final class ImportZipTest{
  static{ Err.setUp(AssertionFailedError.class, Assertions::assertEquals, Assertions::assertTrue); }
  private static Path zipFile(Path dir, String name, String... entries){
    var bytes= new ByteArrayOutputStream();
    Fs.ofV(() -> { try (var z= new ZipOutputStream(bytes)){ for (var e : entries){ z.putNextEntry(new ZipEntry(e)); z.write(e.getBytes(UTF_8)); z.closeEntry(); } } });
    var res= dir.resolve("downloads").resolve(name);
    Fs.ensureDir(res.getParent());
    Fs.ofV(() -> Fs.write(res, bytes.toByteArray()));
    return res;
  }
  private static List<String> files(Path folder){ return Fs.walk(folder, s -> s.filter(Files::isRegularFile).map(f -> folder.relativize(f).toString().replace('\\', '/')).sorted().toList()); }
  private static Path imported(Path dir, Path zip, Optional<Path> selected){
    var z= ImportZip.zip(zip.toUri().toString());
    var bytes= ImportZip.bytes(z);
    var target= ImportZip.target(z, dir.resolve("fearless0_001"), selected);
    ImportZip.install(z, bytes, ImportZip.entries(z, bytes), target);
    return target;
  }
  @Test void theNameOfTheZipGivesItsKind(){
    var z= ImportZip.zip(" https://example.org/demos/tiny_bank_edu_app.zip ");
    assertEquals(List.of("tiny_bank", "tiny_bank_edu_app", true, true), List.of(z.name(), z.folder(), z.edu(), z.app()));
    var l= ImportZip.zip("https://example.org/text_kit_lib.zip");
    assertEquals(List.of("text_kit", false, false), List.of(l.name(), l.edu(), l.app()));
    Err.strCmp("\"ftp://example.org/a_lib.zip\" is not the URL of a zip: [###]", assertThrows(UserError.class, () -> ImportZip.zip("ftp://example.org/a_lib.zip")).getMessage());
    Err.strCmp("The file \"demo.zip\" is not a zip the manager imports: [###]", assertThrows(UserError.class, () -> ImportZip.zip("https://example.org/demo.zip")).getMessage());
    for (var wrong : Fs.isWindows() ? List.of("file:a_app.zip", "https:///a_app.zip", "https:a_app.zip") : List.of("file://data/a_app.zip", "file:a_app.zip", "https:///a_app.zip", "https:a_app.zip")){
      Err.strCmp("\""+wrong+"\" is not the URL of a zip: [###]", assertThrows(UserError.class, () -> ImportZip.zip(wrong)).getMessage());
    }
  }
  @Test void anAppKeepsTheZipAndCopiesTheMarkerAndTheIconOut(@TempDir Path dir){
    var target= imported(dir, zipFile(dir, "lights_out_app.zip", "lights_out.fearless", ".config/icon/icon.png", "_lights_out/_rank_app.fear"), Optional.empty());
    assertEquals(dir.resolve("fearless_imported_zips").resolve("lights_out_app"), target);
    assertEquals(List.of(".config/icon/icon.png", "lights_out.fearless", "lights_out_app.zip"), files(target));
  }
  @Test void anEduAppIsUnzipped(@TempDir Path dir){
    var target= imported(dir, zipFile(dir, "tiny_bank_edu_app.zip", "tiny_bank.fearless", "_tiny_bank/_rank_app.fear"), Optional.empty());
    assertEquals(List.of("_tiny_bank/_rank_app.fear", "tiny_bank.fearless"), files(target));
  }
  @Test void anEduAppKeepsTheFoldersWithNothingInside(@TempDir Path dir){
    var target= imported(dir, zipFile(dir, "tiny_bank_edu_app.zip", "tiny_bank.fearless", ".git/refs/tags/", ".git/head"), Optional.empty());
    assertEquals(List.of(".git/head", "tiny_bank.fearless"), files(target));
    assertEquals(List.of(), files(target.resolve(".git").resolve("refs").resolve("tags")));
  }
  @Test void aLibGoesIntoTheFolderLibOfTheSelectedProject(@TempDir Path dir){
    var hello= dir.resolve("hello");
    assertEquals(hello.resolve("lib").resolve("text_kit_lib.zip"), imported(dir, zipFile(dir, "text_kit_lib.zip", "_text_kit/_rank_core200.fear"), Optional.of(hello)));
    imported(dir, zipFile(dir, "shapes_lab_edu_lib.zip", "_shapes_lab/_rank_worker100.fear"), Optional.of(hello));
    assertEquals(List.of("lib/shapes_lab_edu_lib/_shapes_lab/_rank_worker100.fear", "lib/text_kit_lib.zip"), files(hello));
  }
  @Test void aFileThatIsNotAZipIsRefused(@TempDir Path dir){
    var file= dir.resolve("bad_app.zip");
    Fs.writeUtf8(file, "not a zip");
    var z= ImportZip.zip(file.toUri().toString());
    Err.strCmp("The zip \"bad_app.zip\" cannot be imported: the file is not a zip", assertThrows(UserError.class, () -> ImportZip.entries(z, ImportZip.bytes(z))).getMessage());
  }
  @Test void anAppWithNoMarkerIsRefused(@TempDir Path dir){
    var z= ImportZip.zip(zipFile(dir, "lights_out_app.zip", "_lights_out/_rank_app.fear").toUri().toString());
    Err.strCmp("The zip \"lights_out_app.zip\" has no \"lights_out.fearless\" at its top level: the zip of an app holds the marker of its project.", assertThrows(UserError.class, () -> ImportZip.entries(z, ImportZip.bytes(z))).getMessage());
  }
}

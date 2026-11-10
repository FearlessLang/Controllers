package gui;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.zip.ZipInputStream;

import managerMessages.Report;
import tools.Fs;
import tools.ReadZip;
import userMessages.UserError;
import utils.Bug;

final class ImportZip{
  static final String store= "https://marcoservetto.github.io/fearless_demos/";
  private static final Pattern named= Pattern.compile("([a-z][a-z0-9_]*?)_(edu_)?(app|lib)\\.zip");
  record Zip(URI uri, String file, String name, boolean edu, boolean app){
    String folder(){ return file.substring(0, file.length()-".zip".length()); }
  }
  static Zip zip(String text){
    URI uri;
    try{ uri= new URI(text.strip()); }
    catch (URISyntaxException _){ throw new UserError(Report.importNotUrl(text)); }
    var https= "https".equals(uri.getScheme()) && uri.getHost() != null;
    if (!https && !file(uri) || uri.getPath() == null){ throw new UserError(Report.importNotUrl(text)); }
    var file= uri.getPath().substring(uri.getPath().lastIndexOf('/')+1);
    var m= named.matcher(file);
    if (!m.matches()){ throw new UserError(Report.importNotNamed(file)); }
    return new Zip(uri, file, m.group(1), m.group(2) != null, m.group(3).equals("app"));
  }
  private static boolean file(URI uri){
    if (!"file".equals(uri.getScheme())){ return false; }
    try{ Path.of(uri); return true; }
    catch (IllegalArgumentException _){ return false; }
  }
  static byte[] bytes(Zip z){
    if (z.uri().getScheme().equals("file")){
      try{ return Files.readAllBytes(Path.of(z.uri())); }
      catch (IOException e){ throw new UserError(Report.importNotRead(Path.of(z.uri()), e)); }
    }
    try{
      try (var client= HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()){
        var answer= client.send(HttpRequest.newBuilder(z.uri()).build(), HttpResponse.BodyHandlers.ofByteArray());
        if (answer.statusCode() != 200){ throw new UserError(Report.importNotDownloaded(z.uri(), "the server answered "+answer.statusCode())); }
        return answer.body();
      }
    }
    catch (IOException e){ throw new UserError(Report.importNotDownloaded(z.uri(), e.getMessage() == null ? "no answer from "+z.uri().getHost() : Report.fileFailure(e))); }
    catch (InterruptedException e){ throw Bug.of(e); }
  }
  static Map<String,byte[]> entries(Zip z, byte[] bytes){
    if (bytes.length < 2 || bytes[0] != 'P' || bytes[1] != 'K'){ throw new UserError(Report.importBadZip(z.file(), "the file is not a zip")); }
    Function<String,RuntimeException> bad= n -> new UserError(Report.importBadZip(z.file(), n));
    Map<String,byte[]> res;
    try{ res= new ReadZip(bad, bad, bad, bad, (_, _) -> {}).readAll(() -> new ZipInputStream(new ByteArrayInputStream(bytes), UTF_8)); }
    catch (UncheckedIOException e){ throw new UserError(Report.importBadZip(z.file(), Report.fileFailure(e.getCause()))); }
    if (res.isEmpty()){ throw new UserError(Report.importNoFiles(z.file())); }
    if (z.app() && !res.containsKey(z.name()+".fearless")){ throw new UserError(Report.importNoMarker(z.file(), z.name())); }
    return res;
  }
  static Path target(Zip z, Path managerDir, Optional<Path> selected){
    if (z.app()){ return managerDir.resolveSibling("fearless_imported_zips").resolve(z.folder()); }
    var lib= selected.orElseThrow().resolve("lib");
    return lib.resolve(z.edu() ? z.folder() : z.file());
  }
  static void install(Zip z, byte[] bytes, Map<String,byte[]> entries, Path target){
    if (Files.exists(target)){ Fs.rmTree(target); }
    if (z.edu()){ entries.forEach((n, b) -> unzip(target.resolve(n), n, b)); return; }
    if (!z.app()){ write(target, bytes); return; }
    write(target.resolve(z.file()), bytes);
    entries.forEach((n, b) -> { if (n.equals(z.name()+".fearless") || icon(n)){ write(target.resolve(n), b); } });
  }
  private static boolean icon(String entry){
    var folder= ".config/icon/";
    return entry.startsWith(folder) && entry.indexOf('/', folder.length()) < 0 && entry.endsWith(".png");
  }
  private static void unzip(Path to, String name, byte[] bytes){
    if (name.endsWith("/")){ Fs.ensureDir(to); return; }
    write(to, bytes);
  }
  private static void write(Path file, byte[] bytes){
    Fs.ensureDir(file.getParent());
    Fs.ofV(() -> Fs.write(file, bytes));
  }
}

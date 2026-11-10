package fearlessPluginProject;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

final class UnitTests{
  private static final Pattern disabled= Pattern.compile("(?m)^PLAN\tDISABLED\t([^\t\\r\\n]*)\t([^\t\\r\\n]*)\t([^\t\\r\\n]*)\t([^\t\\r\\n]*)$");
  static Optional<String> report(String name, Path folder, Optional<Instant> since){
    var logs= folder.resolve(".out").resolve("logs").resolve("_base");
    if (!Files.isDirectory(logs)){ return Optional.empty(); }
    try (var all= Files.list(logs)){
      var found= all.filter(p -> p.getFileName().toString().matches("unit_test_log(\\$.*)?\\.log")).sorted(Comparator.comparing(p -> p.getFileName().toString())).toList();
      var shown= since.map(t -> found.stream().filter(p -> !time(p).isBefore(t)).toList()).orElse(found.isEmpty() ? List.of() : List.of(found.getLast()));
      var suites= shown.stream().flatMap(p -> complete(p).stream().map(log -> suite(shown.size() == 1 ? name : name+" "+clock.format(time(p).atZone(ZoneId.systemDefault())), log))).toList();
      return suites.isEmpty() ? Optional.empty() : Optional.of(document(suites));
    }
    catch (NoSuchFileException e){ return Optional.empty(); }
    catch (IOException e){ throw new UncheckedIOException(e); }
    catch (UncheckedIOException e){
      if (!(e.getCause() instanceof NoSuchFileException)){ throw e; }
      return Optional.empty();
    }
  }
  private static final DateTimeFormatter clock= DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
  private static Instant time(Path log){
    try{ return Files.getLastModifiedTime(log).toInstant(); }
    catch (NoSuchFileException e){ return Instant.MIN; }
    catch (IOException e){ throw new UncheckedIOException(e); }
  }
  private static String suite(String name, String log){
    var body= disabled.matcher(log).replaceAll(UnitTests::skipped).replaceAll("(?m)^PLAN\tRUN\t.*$\\r?\\n?", "");
    return """
<testsuite name="%s" tests="%s" failures="%s" errors="0">
%s</testsuite>
""".formatted(xmlAttr(name), count(body, "<testcase "), count(body, "<failure>"), body);
  }
  private static String document(List<String> suites){ return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<testsuites>\n"+String.join("", suites)+"</testsuites>\n"; }
  /// none for a log that the running main moved away meanwhile
  private static Optional<String> complete(Path log){
    String text;
    try{
      var bytes= Files.readAllBytes(log);
      var end= bytes.length;
      while (end > 0 && bytes[end-1] != '\n'){ end -= 1; }
      text= StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes, 0, end)).toString();
    }
    catch (CharacterCodingException e){ return Optional.empty(); }
    catch (NoSuchFileException e){ return Optional.empty(); }
    catch (IOException e){ throw new UncheckedIOException(e); }
    var done= 0;
    while (done < text.length()){
      var close= text.startsWith("<testcase ", done) ? text.indexOf("</testcase>\n", done) : done;
      if (close < 0){ return Optional.of(text.substring(0, done)); }
      done = text.indexOf('\n', close)+1;
    }
    return Optional.of(text);
  }
  private static int count(String body, String what){ return body.split(what, -1).length-1; }
  private static String skipped(MatchResult mr){
    return "<testcase classname=\""+xmlAttr(mr.group(2))+"\" name=\""+xmlAttr(mr.group(3))
      +"\" file=\""+xmlAttr(mr.group(1))+"\" line=\""+mr.group(4)+"\"><skipped/></testcase>";
  }
  private static String xmlAttr(String s){ return s.replace("&", "&amp;").replace("<", "&lt;").replace("\"", "&quot;"); }
}

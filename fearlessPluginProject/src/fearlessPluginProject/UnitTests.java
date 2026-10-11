package fearlessPluginProject;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

final class UnitTests{
  private UnitTests(){}
  private static final Pattern disabled= Pattern.compile("(?m)^PLAN\\|DISABLED\\|([^|\\r\\n]*)\\|([^|\\r\\n]*)\\|([^|\\r\\n]*)\\|([^|\\r\\n]*)$");
  static Optional<String> report(String name, Path folder){
    var logs= folder.resolve(".out").resolve("logs");
    if (!Files.isDirectory(logs)){ return Optional.empty(); }
    try(var all= Files.walk(logs)){
      var newest= all.filter(p->p.getFileName().toString().startsWith("unit_test_log") && p.toString().endsWith(".log")).max(Comparator.comparing(p->p.getFileName().toString()));
      return newest.map(log->document(name, complete(log)));
    }
    catch(NoSuchFileException e){ return Optional.empty(); }
    catch(IOException e){ throw new UncheckedIOException(e); }
  }
  private static String document(String name, String log){
    var body= disabled.matcher(log).replaceAll(UnitTests::skipped).replaceAll("(?m)^PLAN\\|RUN\\|.*$\\r?\\n?","");
    return """
<?xml version="1.0" encoding="UTF-8"?>
<testsuites>
<testsuite name="%s" tests="%d" failures="%d" errors="0">
%s</testsuite>
</testsuites>
""".formatted(name, count(body,"<testcase "), count(body,"<failure>"), body);
  }
  private static String complete(Path log){
    String text;
    try{
      var bytes= Files.readAllBytes(log);
      var end= bytes.length;
      while(end > 0 && bytes[end-1] != '\n'){ end-= 1; }
      text= StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes, 0, end)).toString();
    }
    catch(CharacterCodingException e){ throw new IllegalStateException("The unit test log "+log+" is not UTF-8 text.", e); }
    catch(IOException e){ throw new UncheckedIOException(e); }
    var done= 0;
    while(done < text.length()){
      var close= text.startsWith("<testcase ", done) ? text.indexOf("</testcase>\n", done) : done;
      if (close < 0){ return text.substring(0, done); }
      done= text.indexOf('\n', close)+1;
    }
    return text;
  }
  private static int count(String body, String what){ return body.split(what,-1).length-1; }
  private static String skipped(MatchResult mr){
    return "<testcase classname=\""+xmlAttr(mr.group(2))+"\" name=\""+xmlAttr(mr.group(3))
      +"\" file=\""+xmlAttr(mr.group(1))+"\" line=\""+mr.group(4)+"\"><skipped/></testcase>";
  }
  private static String xmlAttr(String s){ return s.replace("&","&amp;").replace("<","&lt;").replace("\"","&quot;"); }
}

package fearlessPluginProject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

final class Follow{
  private static volatile Optional<Follow> followed= Optional.empty();
  private static final Map<String,Instant> runs= new ConcurrentHashMap<>();
  private static final String applied= "Applied\n";
  final String alias;
  final boolean compile;
  private Follow(String alias, boolean compile){ this.alias= alias; this.compile= compile; }
  static Optional<Follow> followed(){ return followed; }
  static boolean compiling(String alias){ return followed.filter(j -> j.compile && j.alias.equals(alias)).isPresent(); }
  static void compile(String alias){ follow(alias, true, "Compile", alias); }
  static Optional<Instant> lastRun(String alias){ return Optional.ofNullable(runs.get(alias)); }
  static void run(String alias, List<String> mains){
    runs.put(alias, Instant.now());
    ManagerLink.send("SelectMains", Stream.concat(Stream.of(alias), mains.stream()).toArray(String[]::new));
    follow(alias, false, "Run", alias);
  }
  private static void follow(String alias, boolean compile, String verb, String... args){
    var job= new Follow(alias, compile);
    followed = Optional.of(job);
    MainButton.refresh();
    ServerSocket server;
    try{ server= new ServerSocket(0, 1, InetAddress.getLoopbackAddress()); server.setSoTimeout(60_000); }
    catch (IOException e){ throw new UncheckedIOException(e); }
    Thread.startVirtualThread(() -> job.follow(server));
    ManagerLink.send(verb, Stream.concat(Stream.of(args), Stream.of("Port:"+server.getLocalPort())).toArray(String[]::new));
  }
  private void follow(ServerSocket server){
    try (server; var s= server.accept(); var raw= s.getInputStream(); var in= new InputStreamReader(raw, StandardCharsets.UTF_8)){
      var status= new String(raw.readNBytes(applied.length()), StandardCharsets.UTF_8);
      if (status.equals("Refused\n")){ return; }
      assert status.equals(applied);
      try (var out= Output.start(alias)){
        var buf= new char[8192];
        for (int n; (n = in.read(buf)) != -1 && followed.orElse(null) == this; ){ out.print(new String(buf, 0, n)); }
      }
    }
    catch (SocketTimeoutException e){}
    catch (IOException e){ throw new UncheckedIOException(e); }
    finally{ followed = followed.filter(j -> j != this); MainButton.refresh(); }
  }
}

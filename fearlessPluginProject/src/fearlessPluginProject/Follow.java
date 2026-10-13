package fearlessPluginProject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;

final class Follow{
  private static volatile Optional<Follow> followed= Optional.empty();
  final String alias;
  final boolean compile;
  final Instant since= Instant.now();
  private Follow(String alias, boolean compile){ this.alias= alias; this.compile= compile; }
  static Optional<Follow> followed(){ return followed; }
  static boolean compilingFor(String alias, Duration longer){
    return followed.filter(j -> j.compile && j.alias.equals(alias) && j.since.plus(longer).isBefore(Instant.now())).isPresent();
  }
  static void compile(String alias){ follow(alias, true, "Compile", alias); }
  static void run(String alias, String... mains){ follow(alias, false, "Run", Stream.concat(Stream.of(alias), Stream.of(mains)).toArray(String[]::new)); }
  private static void follow(String alias, boolean compile, String verb, String... args){
    var job= new Follow(alias, compile);
    followed = Optional.of(job);
    MainButton.refresh();
    ServerSocket server;
    try{ server= new ServerSocket(0, 1, InetAddress.getLoopbackAddress()); server.setSoTimeout(60_000); }
    catch(IOException e){ throw new UncheckedIOException(e); }
    Thread.startVirtualThread(() -> job.follow(server));
    ManagerLink.send(verb, Stream.concat(Stream.of(args), Stream.of("Port:"+server.getLocalPort())).toArray(String[]::new));
  }
  private void follow(ServerSocket server){
    try(server; var out= Output.start(alias); var s= server.accept(); var in= new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8)){
      var buf= new char[8192];
      for (int n; (n = in.read(buf)) != -1; ){ out.print(new String(buf, 0, n)); }
    }
    catch(SocketTimeoutException e){}
    catch(IOException e){ throw new UncheckedIOException(e); }
    finally{ followed = followed.filter(j -> j != this); MainButton.refresh(); }
  }
}

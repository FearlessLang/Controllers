package controller;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Pattern;

import managerMessages.Report;
import utils.Bug;

final class Follow{
  private static final Pattern port= Pattern.compile("Port:([1-9][0-9]{0,4})");
  private final LinkedBlockingQueue<Optional<String>> queue= new LinkedBlockingQueue<>();
  private Follow(){}
  static Follow to(String text){
    var m= port.matcher(text);
    var number= m.matches() ? Integer.parseInt(m.group(1)) : 0;
    if (number == 0 || number > 65535){ throw new Refused(Report.badPort(text)); }
    Socket socket;
    try{ socket= new Socket(InetAddress.getLoopbackAddress(),number); }
    catch(IOException e){ throw new Refused(Report.portClosed(number,e)); }
    var res= new Follow();
    Thread.startVirtualThread(()->res.pump(socket));
    return res;
  }
  void send(String text){ queue.add(Optional.of(text)); }
  void end(){ queue.add(Optional.empty()); }
  private void pump(Socket socket){
    try(socket; var out= socket.getOutputStream()){
      for(var next= queue.take(); next.isPresent(); next= queue.take()){ out.write(next.get().getBytes(UTF_8)); out.flush(); }
    }
    catch(IOException e){}
    catch(InterruptedException e){ throw Bug.of(e); }
  }
}

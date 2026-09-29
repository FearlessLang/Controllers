package gui;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.URLDecoder;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import controller.Messages;
import utils.Bug;

/// The manager's icon on a Linux desktop: a StatusNotifierItem served on the session bus.
/// The desktop's watcher (org.kde.StatusNotifierWatcher) reads the item's properties and
/// calls Activate or ContextMenu when the icon is clicked; both show the window.
/// One thread reads the bus for the whole process life; the watcher leaving the bus, or
/// the bus closing, stops the manager.
final class Sni{
  private static final String bus= "org.freedesktop.DBus";
  private static final String watcher= "org.kde.StatusNotifierWatcher";
  private static final String serviceUnknown= "org.freedesktop.DBus.Error.ServiceUnknown";
  static final String itemPath= "/StatusNotifierItem";
  static final String menu= "/MenuBar";
  static final List<List<String>> items= List.of(
    List.of("0","children-display","submenu"),
    List.of("1","label","Show manager"),
    List.of("2","type","separator"),
    List.of("3","label","Quit manager"));
  private final SocketChannel channel;
  private final Runnable activate;
  private final Runnable quit;
  private final BufferedImage icon;
  private int serial;
  record Msg(int type, int flags, int serial, int replySerial, String[] fields, Reader body){
    String path(){ return fields[1]; }
    String member(){ return fields[3]; }
    String error(){ return fields[4]; }
    String sender(){ return fields[7]; }
    String signature(){ return fields[8] == null ? "" : fields[8]; }
  }
  static void install(Runnable activate, Runnable quit, Image image){
    var address= System.getenv("DBUS_SESSION_BUS_ADDRESS");
    var path= Pattern.compile("(^|;)unix:([^;]*,)?path=([^,;]+)").matcher(address == null ? "" : address);
    if (!path.find()){ throw Messages.couldNotAddTrayIcon(new IOException(address == null ? "DBUS_SESSION_BUS_ADDRESS is not set" : "DBUS_SESSION_BUS_ADDRESS \""+address+"\" names no \"unix:path=\" socket")); }
    var icon= new BufferedImage(64,64,BufferedImage.TYPE_INT_ARGB);
    var g= icon.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    g.drawImage(image,0,0,64,64,null);
    g.dispose();
    var sni= new Sni(Path.of(URLDecoder.decode(path.group(3),UTF_8)),activate,quit,icon);
    Thread.startVirtualThread(sni::serve);
  }
  Sni(Path socket, Runnable activate, Runnable quit, BufferedImage icon){
    this.activate= activate;
    this.quit= quit;
    this.icon= icon;
    try{
      channel= SocketChannel.open(StandardProtocolFamily.UNIX);
      channel.connect(UnixDomainSocketAddress.of(socket));
      var uid= Files.getAttribute(Path.of("/proc/self"),"unix:uid").toString();
      write(channel,("\0AUTH EXTERNAL "+HexFormat.of().formatHex(uid.getBytes(UTF_8))+"\r\n").getBytes(UTF_8));
      var answer= line(channel);
      if (!answer.startsWith("OK ")){ throw new IOException("the session bus refused the connection: "+answer); }
      write(channel,"BEGIN\r\n".getBytes(UTF_8));
      await(call(bus,"/org/freedesktop/DBus",bus,"Hello","",new Writer()));
      await(call(bus,"/org/freedesktop/DBus",bus,"AddMatch","s",new Writer().str("type='signal',sender='"+bus+"',member='NameOwnerChanged',arg0='"+watcher+"'")));
      register();
    }
    catch(IOException e){ throw Messages.couldNotAddTrayIcon(e); }
  }
  private void register() throws IOException{
    await(call(watcher,"/StatusNotifierWatcher",watcher,"RegisterStatusNotifierItem","s",new Writer().str(itemPath)));
  }
  void serve(){
    try{ for(;;){ handle(next(channel)); } }
    catch(IOException e){ throw Messages.trayConnectionLost(e); }
  }
  private Msg await(int serial) throws IOException{
    for(;;){
      var m= next(channel);
      var answers= (m.type() == 2 || m.type() == 3) && m.replySerial() == serial;
      if (!answers){ handle(m); continue; }
      if (m.type() == 2){ return m; }
      if (m.error().equals(serviceUnknown)){ throw Messages.noSystemTray(); }
      throw new IOException(m.error()+(m.signature().startsWith("s") ? ": "+m.body().str() : ""));
    }
  }
  private void handle(Msg m) throws IOException{
    if (m.type() == 4){ signal(m); return; }
    if (m.type() != 1){ return; }
    if (!m.path().equals(itemPath) && !m.path().equals(menu)){ error(m,"org.freedesktop.DBus.Error.UnknownObject","Fearless offers no object "+m.path()); return; }
    switch(m.member()+"("+m.signature()+")"){
      case "GetAll(s)" -> reply(m,"a{sv}",all(m.path(),icon));
      case "Get(ss)" -> get(m);
      case "GetLayout(iias)" -> reply(m,"u(ia{sv}av)",layout());
      case "GetGroupProperties(aias)" -> reply(m,"a(ia{sv})",groupProperties());
      case "AboutToShow(i)" -> reply(m,"b",new Writer().u32(0));
      case "Event(isvu)" -> event(m);
      case "Activate(ii)","SecondaryActivate(ii)","ContextMenu(ii)" -> { activate.run(); reply(m,"",new Writer()); }
      case "Scroll(is)" -> reply(m,"",new Writer());
      default -> error(m,"org.freedesktop.DBus.Error.UnknownMethod","Fearless offers no method "+m.member()+"("+m.signature()+")");
    }
  }
  private void event(Msg m) throws IOException{
    var id= m.body().u32();
    var clicked= m.body().str().equals("clicked");
    reply(m,"",new Writer());
    if (clicked && id == 1){ activate.run(); }
    if (clicked && id == 3){ quit.run(); }
  }
  private void signal(Msg m) throws IOException{
    var watched= m.sender().equals(bus) && m.member().equals("NameOwnerChanged") && m.body().str().equals(watcher);
    if (!watched){ return; }
    m.body().str();
    if (m.body().str().isEmpty()){ throw Messages.trayIconRemoved(); }
    register();
  }
  private void get(Msg m) throws IOException{
    m.body().str();
    var name= m.body().str();
    if (!props(m.path()).contains(name)){ error(m,"org.freedesktop.DBus.Error.UnknownProperty","Fearless offers no property "+name); return; }
    reply(m,"v",prop(new Writer(),name,icon));
  }
  static List<String> props(String path){
    return path.equals(menu) ? List.of("Version") : List.of("Category","Id","Title","Status","IconPixmap","ToolTip","ItemIsMenu","Menu");
  }
  static Writer all(String path, BufferedImage icon){ return new Writer().array(8,w->props(path).forEach(p->prop(w.pad(8).str(p),p,icon))); }
  static Writer prop(Writer w, String name, BufferedImage icon){
    return switch(name){
      case "Category" -> w.sig("s").str("ApplicationStatus");
      case "Id" -> w.sig("s").str("fearless-manager");
      case "Title" -> w.sig("s").str("Fearless Manager");
      case "Status" -> w.sig("s").str("Active");
      case "Menu" -> w.sig("o").str(menu);
      case "ItemIsMenu" -> w.sig("b").u32(0);
      case "IconPixmap" -> pixmaps(w.sig("a(iiay)"),icon);
      case "ToolTip" -> pixmaps(w.sig("(sa(iiay)ss)").pad(8).str("")).str("Fearless Manager").str("");
      case "Version" -> w.sig("u").u32(3);
      default -> throw Bug.unreachable();
    };
  }
  static Writer layout(){ return item(new Writer().u32(1).pad(8),items.getFirst()).array(1,Sni::children); }
  private static void children(Writer w){ items.stream().skip(1).forEach(i->item(w.sig("(ia{sv}av)").pad(8),i).u32(0)); }
  static Writer groupProperties(){ return new Writer().array(8,w->items.forEach(i->item(w.pad(8),i))); }
  private static Writer item(Writer w, List<String> item){
    return w.u32(Integer.parseInt(item.get(0))).array(8,e->e.str(item.get(1)).sig("s").str(item.get(2)));
  }
  private static Writer pixmaps(Writer w, BufferedImage... images){ return w.array(8,e->List.of(images).forEach(i->pixmap(e,i))); }
  private static void pixmap(Writer w, BufferedImage i){
    var argb= i.getRGB(0,0,i.getWidth(),i.getHeight(),null,0,i.getWidth());
    w.pad(8).u32(i.getWidth()).u32(i.getHeight()).array(1,e->IntStream.of(argb).forEach(p->e.b(p>>24).b(p>>16).b(p>>8).b(p)));
  }
  private int call(String dest, String path, String iface, String member, String sig, Writer body) throws IOException{
    write(channel,frame(++serial,1,0,dest,path,iface,member,null,sig,body));
    return serial;
  }
  private void reply(Msg m, String sig, Writer body) throws IOException{ answer(m,2,null,sig,body); }
  private void error(Msg m, String name, String text) throws IOException{ answer(m,3,name,"s",new Writer().str(text)); }
  private void answer(Msg m, int type, String error, String sig, Writer body) throws IOException{
    var noReplyExpected= (m.flags()&1) != 0;
    if (!noReplyExpected){ write(channel,frame(++serial,type,m.serial(),m.sender(),null,null,null,error,sig,body)); }
  }
  static byte[] frame(int serial, int type, int reply, String dest, String path, String iface, String member, String error, String sig, Writer body){
    return new Writer().b('l').b(type).b(0).b(1).u32(body.pos).u32(serial)
      .array(8,h->header(h,reply,dest,path,iface,member,error,sig)).pad(8).append(body).bytes();
  }
  private static void header(Writer h, int reply, String dest, String path, String iface, String member, String error, String sig){
    field(h,1,"o",path);
    field(h,2,"s",iface);
    field(h,3,"s",member);
    field(h,4,"s",error);
    field(h,6,"s",dest);
    field(h,8,"g",sig);
    if (reply != 0){ h.pad(8).b(5).sig("u").u32(reply); }
  }
  private static void field(Writer w, int code, String type, String value){
    if (value == null || value.isEmpty()){ return; }
    w.pad(8).b(code).sig(type);
    if (type.equals("g")){ w.sig(value); } else { w.str(value); }
  }
  static Msg next(SocketChannel c) throws IOException{
    var head= read(c,16);
    var r= new Reader(head,4,head[0] == 'B');
    int bodyLen= r.u32(), serial= r.u32(), fieldsLen= r.u32();
    int bodyAt= (16+fieldsLen+7)/8*8;
    var all= Arrays.copyOf(head,bodyAt+bodyLen);
    System.arraycopy(read(c,bodyAt-16+bodyLen),0,all,16,bodyAt-16+bodyLen);
    return parse(all);
  }
  static Msg parse(byte[] all){
    var h= new Reader(all,4,all[0] == 'B');
    h.u32();
    int serial= h.u32();
    int end= 16+h.u32();
    var fields= new String[9];
    int reply= 0;
    while (h.pos < end){
      h.pad(8);
      int code= h.b();
      var sig= h.sig();
      switch(code){
        case 1,2,3,4,6,7 -> fields[code]= h.str();
        case 8 -> fields[8]= h.sig();
        case 5 -> reply= h.u32();
        default -> h.skip(sig,0);
      }
    }
    h.pad(8);
    return new Msg(all[1],all[2],serial,reply,fields,h);
  }
  private static byte[] read(SocketChannel c, int n) throws IOException{
    var b= ByteBuffer.allocate(n);
    while (b.hasRemaining()){ if (c.read(b) < 0){ throw new IOException("the session bus closed the connection"); } }
    return b.array();
  }
  static String line(SocketChannel c) throws IOException{
    var sb= new StringBuilder();
    for (int b= read(c,1)[0]; b != '\n'; b= read(c,1)[0]){ sb.append((char)b); }
    return sb.toString().strip();
  }
  static void write(SocketChannel c, byte[] bytes) throws IOException{
    var b= ByteBuffer.wrap(bytes);
    while (b.hasRemaining()){ c.write(b); }
  }
  static final class Reader{
    final byte[] buf;
    final boolean big;
    int pos;
    Reader(byte[] buf, int pos, boolean big){ this.buf= buf; this.pos= pos; this.big= big; }
    void pad(int a){ pos= (pos+a-1)/a*a; }
    int b(){ return buf[pos++]&0xff; }
    int u32(){ pad(4); int v= 0; for (int i= 0; i<4; i++){ v|= b()<<(big ? 24-8*i : 8*i); } return v; }
    String str(){ int n= u32(); var s= new String(buf,pos,n,UTF_8); pos+= n+1; return s; }
    String sig(){ int n= b(); var s= new String(buf,pos,n,UTF_8); pos+= n+1; return s; }
    int skip(String sig, int i){
      var c= sig.charAt(i);
      pad(align(c));
      if (c == 'a'){ int n= u32(); pad(align(sig.charAt(i+1))); pos+= n; return end(sig,i+1); }
      if (c == '(' || c == '{'){ int j= i+1; while (")}".indexOf(sig.charAt(j)) < 0){ j= skip(sig,j); } return j+1; }
      switch(c){
        case 's','o' -> str();
        case 'g' -> sig();
        case 'v' -> skip(sig(),0);
        case 'y','n','q','b','i','u','h','x','t','d' -> pos+= align(c);
        default -> throw Bug.unreachable();
      }
      return i+1;
    }
    static int end(String sig, int i){
      var c= sig.charAt(i);
      if (c == 'a'){ return end(sig,i+1); }
      if (c != '(' && c != '{'){ return i+1; }
      int j= i+1;
      while (")}".indexOf(sig.charAt(j)) < 0){ j= end(sig,j); }
      return j+1;
    }
    static int align(char c){
      return switch(c){
        case 'y','g','v' -> 1;
        case 'n','q' -> 2;
        case 'x','t','d','(','{' -> 8;
        default -> 4;
      };
    }
  }
  static final class Writer{
    byte[] buf= new byte[64];
    int pos;
    Writer b(int v){ if (pos == buf.length){ buf= Arrays.copyOf(buf,pos*2); } buf[pos++]= (byte)v; return this; }
    Writer pad(int a){ while (pos%a != 0){ b(0); } return this; }
    Writer u32(int v){ pad(4); return b(v).b(v>>8).b(v>>16).b(v>>24); }
    Writer str(String s){ var bytes= s.getBytes(UTF_8); u32(bytes.length); for (var x: bytes){ b(x); } return b(0); }
    Writer sig(String s){ b(s.length()); for (var x: s.getBytes(UTF_8)){ b(x); } return b(0); }
    Writer array(int align, Consumer<Writer> elements){
      int at= u32(0).pos-4;
      pad(align);
      int start= pos;
      elements.accept(this);
      for (int i= 0; i<4; i++){ buf[at+i]= (byte)((pos-start)>>(8*i)); }
      return this;
    }
    Writer append(Writer w){ for (int i= 0; i<w.pos; i++){ b(w.buf[i]); } return this; }
    byte[] bytes(){ return Arrays.copyOf(buf,pos); }
  }
}

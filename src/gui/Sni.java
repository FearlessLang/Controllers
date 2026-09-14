package gui;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Pattern;

import userMessages.Violation;

/// The manager's icon on a Linux desktop: a StatusNotifierItem served on the session bus.
/// The desktop's watcher (org.kde.StatusNotifierWatcher) reads the item's properties and
/// calls Activate or ContextMenu when the icon is clicked; both show the window.
/// One thread reads the bus for the whole process life; the watcher leaving the bus, or
/// the bus closing, stops the manager.
final class Sni{
  private static final String bus= "org.freedesktop.DBus";
  private static final String watcher= "org.kde.StatusNotifierWatcher";
  private static final String serviceUnknown= "org.freedesktop.DBus.Error.ServiceUnknown";
  static final List<String> props= List.of("Category","Id","Title","Status","IconPixmap","ToolTip","ItemIsMenu","Menu");
  private final SocketChannel channel;
  private final Runnable activate;
  private final BufferedImage icon;
  private int serial;
  record Msg(int type, int serial, int replySerial, String[] fields, Reader body){
    String member(){ return fields[3]; }
    String error(){ return fields[4]; }
    String sender(){ return fields[7]; }
    String signature(){ return fields[8] == null ? "" : fields[8]; }
  }
  static void install(Runnable activate, Image image){
    var icon= new BufferedImage(64,64,BufferedImage.TYPE_INT_ARGB);
    var g= icon.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    g.drawImage(image,0,0,64,64,null);
    g.dispose();
    var sni= new Sni(activate,icon);
    Thread.startVirtualThread(sni::serve);
  }
  private Sni(Runnable activate, BufferedImage icon){
    this.activate= activate;
    this.icon= icon;
    var address= System.getenv("DBUS_SESSION_BUS_ADDRESS");
    var path= Pattern.compile("unix:path=([^,;]+)").matcher(address == null ? "" : address);
    if (!path.find()){ throw Violation.couldNotAddTrayIcon(new IOException("DBUS_SESSION_BUS_ADDRESS is "+address)); }
    try{
      channel= SocketChannel.open(StandardProtocolFamily.UNIX);
      channel.connect(UnixDomainSocketAddress.of(path.group(1)));
      var uid= Files.getAttribute(Path.of("/proc/self"),"unix:uid").toString();
      write(("\0AUTH EXTERNAL "+HexFormat.of().formatHex(uid.getBytes(UTF_8))+"\r\n").getBytes(UTF_8));
      var answer= line();
      if (!answer.startsWith("OK ")){ throw new IOException("the session bus refused the connection: "+answer); }
      write("BEGIN\r\n".getBytes(UTF_8));
      await(call(bus,"/org/freedesktop/DBus",bus,"Hello","",new Writer()));
      await(call(bus,"/org/freedesktop/DBus",bus,"AddMatch","s",new Writer().str("type='signal',sender='"+bus+"',member='NameOwnerChanged',arg0='"+watcher+"'")));
      register();
    }
    catch(IOException e){ throw Violation.couldNotAddTrayIcon(e); }
  }
  private void register() throws IOException{
    await(call(watcher,"/StatusNotifierWatcher",watcher,"RegisterStatusNotifierItem","s",new Writer().str("/StatusNotifierItem")));
  }
  private void serve(){
    try{ for(;;){ handle(next()); } }
    catch(IOException e){ throw Violation.trayIconRemoved(); }
  }
  private Msg await(int serial) throws IOException{
    for(;;){
      var m= next();
      if (m.replySerial() != serial){ handle(m); continue; }
      if (m.type() == 2){ return m; }
      if (m.error().equals(serviceUnknown)){ throw Violation.noSystemTray(); }
      throw new IOException(m.error()+(m.signature().startsWith("s") ? ": "+m.body().str() : ""));
    }
  }
  private void handle(Msg m) throws IOException{
    if (m.type() == 4){ if (m.member().equals("NameOwnerChanged")){ ownerChanged(m); } return; }
    if (m.type() != 1){ return; }
    switch(m.member()){
      case "GetAll" -> reply(m,"a{sv}",all(icon));
      case "Get" -> get(m);
      case "Activate","SecondaryActivate","ContextMenu" -> { activate.run(); reply(m,"",new Writer()); }
      case "Scroll" -> reply(m,"",new Writer());
      default -> error(m,"org.freedesktop.DBus.Error.UnknownMethod","Fearless offers no method "+m.member());
    }
  }
  private void ownerChanged(Msg m) throws IOException{
    m.body().str();
    m.body().str();
    if (m.body().str().isEmpty()){ throw Violation.trayIconRemoved(); }
    register();
  }
  private void get(Msg m) throws IOException{
    m.body().str();
    var name= m.body().str();
    var w= new Writer();
    if (prop(w,name,icon)){ reply(m,"v",w); return; }
    error(m,"org.freedesktop.DBus.Error.UnknownProperty","Fearless offers no property "+name);
  }
  static Writer all(BufferedImage icon){
    var w= new Writer();
    int at= w.lenAt();
    w.pad(8);
    int start= w.pos;
    for (var p: props){ w.pad(8).str(p); prop(w,p,icon); }
    w.patch(at,w.pos-start);
    return w;
  }
  static boolean prop(Writer w, String name, BufferedImage icon){
    switch(name){
      case "Category" -> w.sig("s").str("ApplicationStatus");
      case "Id" -> w.sig("s").str("fearless-manager");
      case "Title" -> w.sig("s").str("Fearless Manager");
      case "Status" -> w.sig("s").str("Active");
      case "Menu" -> w.sig("o").str("/NO_DBUSMENU");
      case "ItemIsMenu" -> w.sig("b").u32(0);
      case "IconPixmap" -> pixmaps(w.sig("a(iiay)"),icon);
      case "ToolTip" -> { pixmaps(w.sig("(sa(iiay)ss)").pad(8).str("")); w.str("Fearless Manager").str(""); }
      default -> { return false; }
    }
    return true;
  }
  private static void pixmaps(Writer w, BufferedImage... images){
    int at= w.lenAt();
    w.pad(8);
    int start= w.pos;
    for (var img: images){
      w.pad(8).u32(img.getWidth()).u32(img.getHeight());
      int bytesAt= w.lenAt();
      int bytesStart= w.pos;
      for (int p: img.getRGB(0,0,img.getWidth(),img.getHeight(),null,0,img.getWidth())){ w.b(p>>24).b(p>>16).b(p>>8).b(p); }
      w.patch(bytesAt,w.pos-bytesStart);
    }
    w.patch(at,w.pos-start);
  }
  private int call(String dest, String path, String iface, String member, String sig, Writer body) throws IOException{
    write(frame(++serial,1,0,dest,path,iface,member,null,sig,body));
    return serial;
  }
  private void reply(Msg m, String sig, Writer body) throws IOException{ write(frame(++serial,2,m.serial(),m.sender(),null,null,null,null,sig,body)); }
  private void error(Msg m, String name, String text) throws IOException{ write(frame(++serial,3,m.serial(),m.sender(),null,null,null,name,"s",new Writer().str(text))); }
  static byte[] frame(int serial, int type, int reply, String dest, String path, String iface, String member, String error, String sig, Writer body){
    var w= new Writer().b('l').b(type).b(0).b(1).u32(body.pos).u32(serial);
    int at= w.lenAt();
    w.pad(8);
    int start= w.pos;
    field(w,1,"o",path);
    field(w,2,"s",iface);
    field(w,3,"s",member);
    field(w,4,"s",error);
    field(w,6,"s",dest);
    field(w,8,"g",sig);
    if (reply != 0){ w.pad(8).b(5).sig("u").u32(reply); }
    w.patch(at,w.pos-start);
    w.pad(8);
    for (int i= 0; i<body.pos; i++){ w.b(body.buf[i]); }
    return w.bytes();
  }
  private static void field(Writer w, int code, String type, String value){
    if (value == null || value.isEmpty()){ return; }
    w.pad(8).b(code).sig(type);
    if (type.equals("g")){ w.sig(value); } else { w.str(value); }
  }
  private Msg next() throws IOException{
    var head= read(16);
    var r= new Reader(head,4,head[0] == 'B');
    int bodyLen= r.u32(), serial= r.u32(), fieldsLen= r.u32();
    int bodyAt= (16+fieldsLen+7)/8*8;
    var all= Arrays.copyOf(head,bodyAt+bodyLen);
    System.arraycopy(read(bodyAt-16+bodyLen),0,all,16,bodyAt-16+bodyLen);
    return parse(all);
  }
  static Msg parse(byte[] all) throws IOException{
    var h= new Reader(all,4,all[0] == 'B');
    h.u32();
    int serial= h.u32();
    int fieldsLen= h.u32();
    var fields= new String[10];
    int reply= 0;
    while (h.pos < 16+fieldsLen){
      h.pad(8);
      int code= h.b();
      var sig= h.sig();
      switch(sig){
        case "s","o" -> fields[code]= h.str();
        case "g" -> fields[code]= h.sig();
        case "u" -> reply= h.u32();
        default -> throw new IOException("unexpected header field type "+sig);
      }
    }
    h.pad(8);
    return new Msg(all[1],serial,reply,fields,h);
  }
  private byte[] read(int n) throws IOException{
    var b= ByteBuffer.allocate(n);
    while (b.hasRemaining()){ if (channel.read(b) < 0){ throw new IOException("the session bus closed the connection"); } }
    return b.array();
  }
  private String line() throws IOException{
    var sb= new StringBuilder();
    for (int c= read(1)[0]; c != '\n'; c= read(1)[0]){ sb.append((char)c); }
    return sb.toString().strip();
  }
  private void write(byte[] bytes) throws IOException{
    var b= ByteBuffer.wrap(bytes);
    while (b.hasRemaining()){ channel.write(b); }
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
  }
  static final class Writer{
    byte[] buf= new byte[64];
    int pos;
    Writer b(int v){ if (pos == buf.length){ buf= Arrays.copyOf(buf,pos*2); } buf[pos++]= (byte)v; return this; }
    Writer pad(int a){ while (pos%a != 0){ b(0); } return this; }
    Writer u32(int v){ pad(4); return b(v).b(v>>8).b(v>>16).b(v>>24); }
    Writer str(String s){ var bytes= s.getBytes(UTF_8); u32(bytes.length); for (var x: bytes){ b(x); } return b(0); }
    Writer sig(String s){ b(s.length()); for (var x: s.getBytes(UTF_8)){ b(x); } return b(0); }
    int lenAt(){ u32(0); return pos-4; }
    void patch(int at, int len){ for (int i= 0; i<4; i++){ buf[at+i]= (byte)(len>>(8*i)); } }
    byte[] bytes(){ return Arrays.copyOf(buf,pos); }
  }
}

package gui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.function.Consumer;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import controller.Messages;
import gui.Sni.Msg;
import gui.Sni.Reader;
import gui.Sni.Writer;
import tools.Fs;
import utils.Bug;
import userMessages.UserError;

final class SniTest{
  @Test void aCallIsFramedAndParsedBack(){
    var bytes= Sni.frame(7,1,0,"org.kde.StatusNotifierWatcher","/StatusNotifierWatcher","org.kde.StatusNotifierWatcher","RegisterStatusNotifierItem",null,"s",new Writer().str("/StatusNotifierItem"));
    var m= Sni.parse(bytes);
    assertEquals(1,m.type());
    assertEquals(7,m.serial());
    assertEquals(0,m.replySerial());
    assertEquals("RegisterStatusNotifierItem",m.member());
    assertEquals("/StatusNotifierWatcher",m.fields()[1]);
    assertEquals("org.kde.StatusNotifierWatcher",m.fields()[6]);
    assertEquals("s",m.signature());
    assertNull(m.sender());
    assertEquals("/StatusNotifierItem",m.body().str());
    assertEquals(bytes.length,m.body().pos);
  }
  @Test void aReplyCarriesTheSerialItAnswers(){
    var m= Sni.parse(Sni.frame(3,2,7,":1.5",null,null,null,null,"",new Writer()));
    assertEquals(2,m.type());
    assertEquals(7,m.replySerial());
    assertEquals(":1.5",m.fields()[6]);
    assertEquals("",m.signature());
    assertEquals(0,m.body().buf.length%8);
  }
  @Test void anErrorNamesItselfAndExplains(){
    var m= Sni.parse(Sni.frame(4,3,7,":1.5",null,null,null,"org.freedesktop.DBus.Error.UnknownMethod","s",new Writer().str("no such method")));
    assertEquals(3,m.type());
    assertEquals("org.freedesktop.DBus.Error.UnknownMethod",m.error());
    assertEquals("no such method",m.body().str());
  }
  @Test void getAllListsEveryPropertyWithItsSignature(){
    var icon= new BufferedImage(2,2,BufferedImage.TYPE_INT_ARGB);
    icon.setRGB(0,0,0x80FF0010);
    icon.setRGB(1,1,0xFF00FF00);
    var w= Sni.all(Sni.itemPath,icon);
    var r= new Reader(w.bytes(),0,false);
    int len= r.u32();
    r.pad(8);
    assertEquals(w.pos-8,len);
    var seen= new LinkedHashMap<String,String>();
    while (r.pos < w.pos){
      r.pad(8);
      var key= r.str();
      var sig= r.sig();
      switch(sig){
        case "s","o" -> seen.put(key,r.str());
        case "b" -> seen.put(key,""+r.u32());
        case "a(iiay)" -> seen.put(key,pixmaps(r));
        case "(sa(iiay)ss)" -> { r.pad(8); var name= r.str(); var pix= pixmaps(r); seen.put(key,name+pix+r.str()+"|"+r.str()); }
        default -> throw new AssertionError(sig);
      }
    }
    assertEquals(w.pos,r.pos);
    assertEquals(String.join(",",Sni.props(Sni.itemPath)),String.join(",",seen.keySet()));
    assertEquals("ApplicationStatus",seen.get("Category"));
    assertEquals("fearless-manager",seen.get("Id"));
    assertEquals("Fearless Manager",seen.get("Title"));
    assertEquals("Active",seen.get("Status"));
    assertEquals("/MenuBar",seen.get("Menu"));
    assertEquals("0",seen.get("ItemIsMenu"));
    assertEquals("[2x2:80ff0010 00000000 00000000 ff00ff00]",seen.get("IconPixmap"));
    assertEquals("[]Fearless Manager|",seen.get("ToolTip"));
  }
  @Test void theMenuLayoutIsTheRootHoldingShowSeparatorQuit(){
    var w= Sni.layout();
    var r= new Reader(w.bytes(),0,false);
    assertEquals(1,r.u32());
    r.pad(8);
    assertEquals("0{children-display=submenu}",item(r));
    int len= r.u32();
    int end= r.pos+len;
    var kids= new StringBuilder();
    while (r.pos < end){
      assertEquals("(ia{sv}av)",r.sig());
      r.pad(8);
      kids.append(item(r)).append(r.u32()).append(';');
    }
    assertEquals(w.pos,r.pos);
    assertEquals("1{label=Show manager}0;2{type=separator}0;3{label=Quit manager}0;",kids.toString());
  }
  @Test void groupPropertiesListEveryItem(){
    var w= Sni.groupProperties();
    var r= new Reader(w.bytes(),0,false);
    int len= r.u32();
    r.pad(8);
    assertEquals(w.pos-8,len);
    var seen= new StringBuilder();
    while (r.pos < w.pos){ r.pad(8); seen.append(item(r)).append(';'); }
    assertEquals("0{children-display=submenu};1{label=Show manager};2{type=separator};3{label=Quit manager};",seen.toString());
  }
  @Test void theMenuObjectHasVersionThree(){
    var w= Sni.all(Sni.menu,null);
    var r= new Reader(w.bytes(),0,false);
    assertEquals(w.pos-8,r.u32());
    r.pad(8);
    assertEquals("Version",r.str());
    assertEquals("u",r.sig());
    assertEquals(3,r.u32());
    assertEquals(w.pos,r.pos);
  }
  @Test void unixFdsDoNotOverwriteTheReplySerial(){
    var m= Sni.parse(msg(2,0,3,new Writer().str("after"),u(5,7).andThen(s(6,":1.5")).andThen(g("s")).andThen(u(9,2))));
    assertEquals(7,m.replySerial());
    assertEquals(":1.5",m.fields()[6]);
    assertEquals("after",m.body().str());
  }
  @Test void fieldCodesOfTenOrMoreAreSkipped(){
    var m= Sni.parse(msg(2,0,3,new Writer().str("after"),u(5,7).andThen(s(10,"ten")).andThen(h->h.pad(8).b(255).sig("t").pad(8).u32(1).u32(2)).andThen(g("s"))));
    assertEquals(7,m.replySerial());
    assertEquals("s",m.signature());
    assertEquals("after",m.body().str());
  }
  @Test void aFieldOfAnUnknownTypeIsSkippedByItsSignature(){
    Consumer<Writer> dict= h->h.pad(8).b(12).sig("a{sv}").array(8,e->e.str("k").sig("(yx)").pad(8).b(1).pad(8).u32(1).u32(0));
    Consumer<Writer> bytes= h->h.pad(8).b(13).sig("ay").array(1,e->e.b(1).b(2).b(3));
    var m= Sni.parse(msg(2,0,3,new Writer().str("after"),dict.andThen(bytes).andThen(u(5,7)).andThen(s(6,":1.5")).andThen(g("s"))));
    assertEquals(7,m.replySerial());
    assertEquals(":1.5",m.fields()[6]);
    assertEquals("after",m.body().str());
  }
  @Test void aFakeBusDrivesTheItemUntilTheWatcherLeaves() throws Exception{
    Assumptions.assumeTrue(Fs.isLinux());
    var dir= Files.createTempDirectory("sni");
    var socket= dir.resolve("bus");
    var icon= new BufferedImage(2,2,BufferedImage.TYPE_INT_ARGB);
    var log= new ArrayList<String>();
    try(var server= ServerSocketChannel.open(StandardProtocolFamily.UNIX)){
      server.bind(UnixDomainSocketAddress.of(socket));
      var bus= new FutureTask<Void>(()->{ try(var c= server.accept()){ busScript(c,icon); } return null; });
      Thread.startVirtualThread(bus);
      var e= assertThrows(UserError.class,()->new Sni(socket,()->log.add("activate"),()->log.add("quit"),icon).serve());
      bus.get();
      assertEquals(Messages.trayIconRemoved().getMessage(),e.getMessage());
      assertEquals(List.of("activate","activate","quit"),log);
    }
    finally{ Files.deleteIfExists(socket); Files.delete(dir); }
  }
  private static void busScript(SocketChannel c, BufferedImage icon) throws IOException{
    assertEquals("\0AUTH EXTERNAL "+hex(""+Files.getAttribute(Path.of("/proc/self"),"unix:uid")),Sni.line(c));
    Sni.write(c,"OK 0123456789abcdef\r\n".getBytes(StandardCharsets.UTF_8));
    assertEquals("BEGIN",Sni.line(c));
    var hello= Sni.next(c);
    assertEquals("Hello",hello.member());
    Sni.write(c,msg(2,0,1,new Writer().str(":1.7"),fromBus(hello).andThen(g("s"))));
    Sni.write(c,msg(4,0,2,new Writer().str(":1.7"),busSignal("NameAcquired").andThen(s(6,":1.7")).andThen(g("s"))));
    hasWatcher(c,true);
    var match= Sni.next(c);
    assertEquals("AddMatch",match.member());
    assertEquals("type='signal',sender='org.freedesktop.DBus',member='NameOwnerChanged',arg0='org.kde.StatusNotifierWatcher'",match.body().str());
    Sni.write(c,msg(2,0,3,new Writer(),fromBus(match)));
    var register= Sni.next(c);
    assertEquals("RegisterStatusNotifierItem",register.member());
    assertEquals("org.kde.StatusNotifierWatcher",register.fields()[6]);
    assertEquals(Sni.itemPath,register.body().str());
    Sni.write(c,msg(1,0,10,new Writer().str("org.kde.StatusNotifierItem"),fromWatcher(Sni.itemPath,"GetAll","s")));
    var all= answer(c,2,10);
    assertEquals("a{sv}",all.signature());
    assertArrayEquals(Sni.all(Sni.itemPath,icon).bytes(),Arrays.copyOfRange(all.body().buf,all.body().pos,all.body().buf.length));
    Sni.write(c,msg(2,0,4,new Writer(),u(5,register.serial()).andThen(s(7,":1.9"))));
    Sni.write(c,msg(1,1,11,clicked(1),fromWatcher(Sni.menu,"Event","isvu")));
    Sni.write(c,msg(1,0,12,clicked(1),fromWatcher(Sni.menu,"Event","isvu")));
    answer(c,2,12);
    Sni.write(c,msg(1,0,13,new Writer().str("com.canonical.dbusmenu").str("Version"),fromWatcher(Sni.menu,"Get","ss")));
    var version= answer(c,2,13);
    assertEquals("v",version.signature());
    assertEquals("u",version.body().sig());
    assertEquals(3,version.body().u32());
    Sni.write(c,msg(1,0,14,new Writer().str("com.canonical.dbusmenu").str("Id"),fromWatcher(Sni.menu,"Get","ss")));
    assertEquals("org.freedesktop.DBus.Error.UnknownProperty",answer(c,3,14).error());
    Sni.write(c,msg(1,0,15,new Writer().u32(3),fromWatcher(Sni.menu,"Event","i")));
    assertEquals("org.freedesktop.DBus.Error.UnknownMethod",answer(c,3,15).error());
    Sni.write(c,msg(4,0,16,new Writer().str("org.kde.StatusNotifierWatcher").str(":1.9").str(""),fromWatcher("/x","NameOwnerChanged","sss")));
    Sni.write(c,msg(1,0,17,clicked(3),fromWatcher(Sni.menu,"Event","isvu")));
    answer(c,2,17);
    Sni.write(c,msg(4,0,5,new Writer().str("org.kde.StatusNotifierWatcher").str(":1.9").str(""),busSignal("NameOwnerChanged").andThen(g("sss"))));
  }
  @Test void withNoWatcherTheItemIsNotServedAndTheBusIsLeft() throws Exception{
    Assumptions.assumeTrue(Fs.isLinux());
    var dir= Files.createTempDirectory("sni");
    var socket= dir.resolve("bus");
    try(var server= ServerSocketChannel.open(StandardProtocolFamily.UNIX)){
      server.bind(UnixDomainSocketAddress.of(socket));
      var bus= new FutureTask<Void>(()->{ try(var c= server.accept()){ noWatcherScript(c); } return null; });
      Thread.startVirtualThread(bus);
      var sni= new Sni(socket,()->{ throw Bug.unreachable(); },()->{ throw Bug.unreachable(); },new BufferedImage(2,2,BufferedImage.TYPE_INT_ARGB));
      bus.get();
      assertFalse(sni.tray);
    }
    finally{ Files.deleteIfExists(socket); Files.delete(dir); }
  }
  private static void noWatcherScript(SocketChannel c) throws IOException{
    Sni.line(c);
    Sni.write(c,"OK 0123456789abcdef\r\n".getBytes(StandardCharsets.UTF_8));
    assertEquals("BEGIN",Sni.line(c));
    var hello= Sni.next(c);
    Sni.write(c,msg(2,0,1,new Writer().str(":1.7"),fromBus(hello).andThen(g("s"))));
    hasWatcher(c,false);
    assertEquals(-1,c.read(ByteBuffer.allocate(1)));
  }
  private static void hasWatcher(SocketChannel c, boolean has) throws IOException{
    var ask= Sni.next(c);
    assertEquals("NameHasOwner",ask.member());
    assertEquals("org.kde.StatusNotifierWatcher",ask.body().str());
    Sni.write(c,msg(2,0,30,new Writer().u32(has ? 1 : 0),fromBus(ask).andThen(g("b"))));
  }
  private static Msg answer(SocketChannel c, int type, int serial) throws IOException{
    var m= Sni.next(c);
    assertEquals(type,m.type());
    assertEquals(serial,m.replySerial());
    assertEquals(":1.9",m.fields()[6]);
    return m;
  }
  private static Writer clicked(int id){ return new Writer().u32(id).str("clicked").sig("s").str("").u32(0); }
  private static String hex(String s){ return HexFormat.of().formatHex(s.getBytes(StandardCharsets.UTF_8)); }
  private static Consumer<Writer> fromBus(Msg m){ return u(5,m.serial()).andThen(s(6,":1.7")).andThen(s(7,"org.freedesktop.DBus")); }
  private static Consumer<Writer> busSignal(String member){ return s(1,"/org/freedesktop/DBus").andThen(s(2,"org.freedesktop.DBus")).andThen(s(3,member)).andThen(s(7,"org.freedesktop.DBus")); }
  private static Consumer<Writer> fromWatcher(String path, String member, String sig){ return s(1,path).andThen(s(3,member)).andThen(s(6,":1.7")).andThen(s(7,":1.9")).andThen(g(sig)); }
  private static byte[] msg(int type, int flags, int serial, Writer body, Consumer<Writer> fields){
    return new Writer().b('l').b(type).b(flags).b(1).u32(body.pos).u32(serial).array(8,fields).pad(8).append(body).bytes();
  }
  private static Consumer<Writer> s(int code, String v){ return h->h.pad(8).b(code).sig(code == 1 ? "o" : "s").str(v); }
  private static Consumer<Writer> u(int code, int v){ return h->h.pad(8).b(code).sig("u").u32(v); }
  private static Consumer<Writer> g(String sig){ return h->h.pad(8).b(8).sig("g").sig(sig); }
  private static String item(Reader r){
    var id= r.u32();
    int len= r.u32();
    r.pad(8);
    int end= r.pos+len;
    var sb= new StringBuilder().append(id).append('{');
    while (r.pos < end){
      r.pad(8);
      var key= r.str();
      assertEquals("s",r.sig());
      sb.append(key).append('=').append(r.str());
    }
    return sb.append('}').toString();
  }
  private static String pixmaps(Reader r){
    int len= r.u32();
    r.pad(8);
    int end= r.pos+len;
    var sb= new StringBuilder("[");
    while (r.pos < end){
      r.pad(8);
      int width= r.u32(), height= r.u32(), bytes= r.u32();
      sb.append(width).append('x').append(height).append(':');
      for (int i= 0; i<bytes; i++){ sb.append(i>0 && i%4==0 ? " " : "").append("%02x".formatted(r.b())); }
    }
    assertEquals(end,r.pos);
    return sb.append(']').toString();
  }
}

package gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.LinkedHashMap;

import org.junit.jupiter.api.Test;

import gui.Sni.Reader;
import gui.Sni.Writer;

final class SniTest{
  @Test void aCallIsFramedAndParsedBack() throws IOException{
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
  @Test void aReplyCarriesTheSerialItAnswers() throws IOException{
    var m= Sni.parse(Sni.frame(3,2,7,":1.5",null,null,null,null,"",new Writer()));
    assertEquals(2,m.type());
    assertEquals(7,m.replySerial());
    assertEquals(":1.5",m.fields()[6]);
    assertEquals("",m.signature());
    assertEquals(0,m.body().buf.length%8);
  }
  @Test void anErrorNamesItselfAndExplains() throws IOException{
    var m= Sni.parse(Sni.frame(4,3,7,":1.5",null,null,null,"org.freedesktop.DBus.Error.UnknownMethod","s",new Writer().str("no such method")));
    assertEquals(3,m.type());
    assertEquals("org.freedesktop.DBus.Error.UnknownMethod",m.error());
    assertEquals("no such method",m.body().str());
  }
  @Test void getAllListsEveryPropertyWithItsSignature(){
    var icon= new BufferedImage(2,2,BufferedImage.TYPE_INT_ARGB);
    icon.setRGB(0,0,0x80FF0010);
    icon.setRGB(1,1,0xFF00FF00);
    var w= Sni.all(icon);
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
    assertEquals(String.join(",",Sni.props),String.join(",",seen.keySet()));
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
    var w= Sni.menuAll();
    var r= new Reader(w.bytes(),0,false);
    assertEquals(w.pos-8,r.u32());
    r.pad(8);
    assertEquals("Version",r.str());
    assertEquals("u",r.sig());
    assertEquals(3,r.u32());
    assertEquals(w.pos,r.pos);
  }
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

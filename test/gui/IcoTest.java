package gui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tools.Fs;
import utils.Range;

final class IcoTest{
  private static Path redLeftHalf(Path dir){
    var img= new BufferedImage(512,384,BufferedImage.TYPE_INT_ARGB);
    var g= img.createGraphics();
    g.setBackground(new Color(0,255,0,0));
    g.clearRect(0,0,512,384);
    g.setColor(Color.red);
    g.fillRect(0,0,256,384);
    g.dispose();
    var res= dir.resolve("source.png");
    Fs.ofV(()->ImageIO.write(img,"png",res.toFile()));
    return res;
  }
  private static ByteBuffer ico(Path dir){
    var res= dir.resolve("out.ico");
    Ico.fromPng(redLeftHalf(dir),res);
    return ByteBuffer.wrap(Fs.of(()->Files.readAllBytes(res))).order(ByteOrder.LITTLE_ENDIAN);
  }
  @Test void headerAndDirectoryDescribeEightContiguous32BitFrames(@TempDir Path dir){
    var b= ico(dir);
    assertEquals(List.of(0,1,8),List.of((int)b.getShort(0),(int)b.getShort(2),(int)b.getShort(4)));
    var offset= 6+16*8;
    for (int i : Range.of(Ico.sizes)){
      var e= 6+16*i;
      var side= Ico.sizes.get(i) % 256;
      assertEquals(List.of(side,side,0,0,1,32,offset),List.of(
        b.get(e)&0xff,b.get(e+1)&0xff,(int)b.get(e+2),(int)b.get(e+3),(int)b.getShort(e+4),(int)b.getShort(e+6),b.getInt(e+12)));
      offset+= b.getInt(e+8);
    }
    assertEquals(b.capacity(),offset);
  }
  @Test void theLargestFrameIsAnEmbeddedPng(@TempDir Path dir){
    var b= ico(dir);
    var bytes= Arrays.copyOfRange(b.array(),b.getInt(6+16*7+12),b.capacity());
    assertArrayEquals(new byte[]{(byte)0x89,'P','N','G','\r','\n',0x1a,'\n'},Arrays.copyOf(bytes,8));
    var img= Fs.of(()->ImageIO.read(new ByteArrayInputStream(bytes)));
    assertEquals(List.of(256,256),List.of(img.getWidth(),img.getHeight()));
    assertEquals(Color.red.getRGB(),img.getRGB(64,128));
    assertEquals(0,img.getRGB(192,128)>>>24);
    assertEquals(0,img.getRGB(64,10)>>>24);
    assertEquals(0,greenPixels(img));
  }
  @Test void smallerFramesAreBottomUp32BitDibsWithAMatchingAndMask(@TempDir Path dir){
    var b= ico(dir);
    for (int i : Range.of(0,7)){
      var s= Ico.sizes.get(i);
      var at= b.getInt(6+16*i+12);
      var maskRow= (s+31)/32*4;
      assertEquals(List.of(40,s,2*s,1,32,0,40+4*s*s+maskRow*s),List.of(
        b.getInt(at),b.getInt(at+4),b.getInt(at+8),(int)b.getShort(at+12),(int)b.getShort(at+14),b.getInt(at+16),b.getInt(6+16*i+8)));
      var img= new BufferedImage(s,s,BufferedImage.TYPE_INT_ARGB);
      for (int y : Range.of(0,s)){
        for (int x : Range.of(0,s)){
          var argb= b.getInt(at+40+4*((s-1-y)*s+x));
          img.setRGB(x,y,argb);
          var masked= (b.get(at+40+4*s*s+(s-1-y)*maskRow+x/8)>>(7-x%8)&1) == 1;
          assertEquals(argb>>>24 == 0,masked);
        }
      }
      assertEquals(Color.red.getRGB(),img.getRGB(s/4,s/2));
      assertEquals(0,img.getRGB(3*s/4,s/2)>>>24);
      assertEquals(0,img.getRGB(s/4,0)>>>24);
      assertEquals(0,greenPixels(img));
    }
  }
  private static long greenPixels(BufferedImage img){
    return Arrays.stream(img.getRGB(0,0,img.getWidth(),img.getHeight(),null,0,img.getWidth()))
      .filter(c->c>>>24 != 0 && (c>>8&0xff) != 0).count();
  }
}

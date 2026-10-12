package agentTools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

final class PilotTest{
  @Test void seenRingsThePointerAndShotDoesNot(){
    var pilot= new Pilot();
    pilot.glide(600,400,Pilot.none,700,500,Pilot.none);
    var plain= pilot.shot();
    var seen= pilot.seen();
    assertNotEquals(0xa020f0,plain.getRGB(708,500)&0xffffff);
    assertEquals(0xa020f0,seen.getRGB(708,500)&0xffffff);
    assertEquals(0xa020f0,seen.getRGB(692,500)&0xffffff);
    assertEquals(0xa020f0,seen.getRGB(700,492)&0xffffff);
    assertEquals(0xa020f0,seen.getRGB(700,508)&0xffffff);
    assertEquals(plain.getRGB(700,500),seen.getRGB(700,500));
  }
}

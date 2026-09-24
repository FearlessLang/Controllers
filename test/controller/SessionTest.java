package controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class SessionTest{
  private static Session stuck(Path dir, StringBuilder out){ return new Session(dir,dir,_->{},out::append,()->{}); }
  @Test void anIdleProjectRefusesNothing(@TempDir Path dir){
    var out= new StringBuilder();
    assertFalse(stuck(dir,out).refused("clear cache"));
    assertEquals("",out.toString());
  }
  @Test void aJobAskedWhileAnotherRunsIsRefusedWithALine(@TempDir Path dir){
    var out= new StringBuilder();
    var s= stuck(dir,out);
    s.compile();
    s.run(Optional.empty(),List.of());
    assertEquals("--- run refused: the project is busy with compiling ---\n",out.toString());
  }
  @Test void aRequestThatIsNotAJobIsRefusedTooWhileAJobRuns(@TempDir Path dir){
    var out= new StringBuilder();
    var s= stuck(dir,out);
    s.compile();
    assertTrue(s.refused("clear cache"));
    assertEquals("--- clear cache refused: the project is busy with compiling ---\n",out.toString());
  }
  @Test void readingTheMainsWhileAJobRunsIsLeftToTheJob(@TempDir Path dir){
    var out= new StringBuilder();
    var s= stuck(dir,out);
    s.compile();
    s.refresh();
    assertEquals("",out.toString());
    assertEquals("compiling",s.current());
  }
}

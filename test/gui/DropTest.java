package gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

final class DropTest{
  private static final String root= Path.of(".").toAbsolutePath().getRoot().toUri().toString();
  private final List<String> refused= new ArrayList<>();
  @Test void aFileListDropIsReadStraightFromTheFlavor(){
    var t= fixed(DataFlavor.javaFileListFlavor,List.of(new File("/home/me/myproject")));
    assertTrue(Drop.hasFiles(t));
    assertEquals(List.of(Path.of("/home/me/myproject")),Drop.paths(t,refused::add));
  }
  @Test void aUriListStringIsSplitOnLines(){
    var t= fixed(uriList("java.lang.String"),root+"home/me/myproject\r\n"+root+"home/me/other.fearless\r\n");
    assertUris(Drop.paths(t,refused::add),root+"home/me/myproject",root+"home/me/other.fearless");
  }
  @Test void aUriListReaderIsReadInFull(){
    var t= fixed(uriList("java.io.Reader"),new StringReader(root+"home/me/myproject\n"));
    assertUris(Drop.paths(t,refused::add),root+"home/me/myproject");
  }
  @Test void aUriListInputStreamIsReadAsUtf8(){
    var t= fixed(uriList("java.io.InputStream"),new ByteArrayInputStream((root+"home/me/myproject\n").getBytes(StandardCharsets.UTF_8)));
    assertUris(Drop.paths(t,refused::add),root+"home/me/myproject");
  }
  @Test void commentAndBlankLinesAreSkipped(){
    assertUris(Drop.fromUriList("# a comment\r\n\r\n"+root+"home/me/myproject\r\n",refused::add),root+"home/me/myproject");
  }
  @Test void aSpaceInTheNameIsPercentDecoded(){
    assertUris(Drop.fromUriList(root+"home/me/My%20Project\r\n",refused::add),root+"home/me/My%20Project");
  }
  @Test void aBareLineFeedIsAcceptedToo(){
    assertUris(Drop.fromUriList(root+"home/me/myproject\n",refused::add),root+"home/me/myproject");
  }
  @Test void aDropWithNeitherFlavorHasNothingToOffer(){
    var t= fixed(DataFlavor.stringFlavor,"just text");
    assertFalse(Drop.hasFiles(t));
    assertEquals(List.of(),Drop.paths(t,refused::add));
  }
  @Test void everyLineNamingNoFileOrFolderIsRefusedNamingItAndTheOthersAreKept(){
    var t= fixed(uriList("java.lang.String"),"https://x/y\r\n"+root+"data/a\r\nsmb://host/share/b\r\nhome/me/c\r\nfile:///a b\r\n");
    assertUris(Drop.paths(t,refused::add),root+"data/a");
    assertEquals(List.of(
      "The manager was asked to register \"https://x/y\", dropped on its window, but only a \"file:\" URI names a file or folder of this computer.",
      "The manager was asked to register \"smb://host/share/b\", dropped on its window, but only a \"file:\" URI names a file or folder of this computer.",
      "The manager was asked to register \"home/me/c\", dropped on its window, but only a \"file:\" URI names a file or folder of this computer.",
      "The manager was asked to register \"file:///a b\", dropped on its window, but it is not a URI: Illegal character in path."),refused);
  }
  @Test void aDropTheDesktopDoesNotHandOverIsRefused(){
    var t= fixed(uriList("java.io.InputStream"),new InputStream(){ @Override public int read() throws IOException{ throw new IOException("the drag ended"); } });
    assertEquals(List.of(),Drop.paths(t,refused::add));
    assertEquals(List.of("The manager was asked to register what was dropped on its window, but the desktop did not hand it over: the drag ended"),refused);
  }
  private static DataFlavor uriList(String repClass){
    try{ return new DataFlavor("text/uri-list;class="+repClass); }
    catch(ClassNotFoundException e){ throw new RuntimeException(e); }
  }
  private static void assertUris(List<Path> paths, String... expected){
    assertEquals(List.of(expected),paths.stream().map(Path::toUri).map(Object::toString).toList());
  }
  private static Transferable fixed(DataFlavor flavor, Object data){
    return new Transferable(){
      @Override public DataFlavor[] getTransferDataFlavors(){ return new DataFlavor[]{flavor}; }
      @Override public boolean isDataFlavorSupported(DataFlavor f){ return f.equals(flavor); }
      @Override public Object getTransferData(DataFlavor f) throws UnsupportedFlavorException{
        if (!isDataFlavorSupported(f)){ throw new UnsupportedFlavorException(f); }
        return data;
      }
    };
  }
}
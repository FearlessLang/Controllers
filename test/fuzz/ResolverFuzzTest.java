package fuzz;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import resources.ResolveResource;
import suggest.Resolver;
import suggest.ResolverTest;

final class ResolverFuzzTest{
  @Test void noTextNorCursorMakesTheSuggestionsThrow() throws IOException{
    var heads= ResolverTest.head+"\nuse Foo as B;\nuse base. as C;\nuse as D;\nuse lib.Str as;\nuse lib.Nat as N;";
    List<Path> fear;
    try (var all= Files.walk(ResolveResource.stLibPath)){ fear= all.filter(p -> p.toString().endsWith(".fear")).sorted().toList(); }
    for (var p : fear){
      var t= Files.readString(p);
      var r= Resolver.of(ResolverTest.compiled, "base", Map.of("lib", "base"), heads, t);
      IntStream.rangeClosed(0, t.length()).forEach(r::suggest);
    }
    for (var t : List.of(ResolverTest.file, Files.readString(ResolveResource.stLibPath.resolve("pipes.fear")))){
      IntStream.rangeClosed(0, t.length()).forEach(k -> suggest(heads, t.substring(0, k), k));
      IntStream.range(0, t.length()).forEach(k -> suggest(heads, t.substring(0, k)+t.substring(k+1), k));
    }
  }
  static void suggest(String head, String text, int pos){
    var r= Resolver.of(ResolverTest.compiled, "base", Map.of("lib", "base"), head, text);
    r.suggest(pos);
    r.expected(pos);
  }
}

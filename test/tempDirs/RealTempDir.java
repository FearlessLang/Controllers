package tempDirs;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.extension.AnnotatedElementContext;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.io.TempDirFactory;

@Retention(RUNTIME)
@Target({FIELD, PARAMETER})
@TempDir(factory= RealTempDir.Real.class)
public @interface RealTempDir{
  final class Real implements TempDirFactory{
    @Override public Path createTempDirectory(AnnotatedElementContext element, ExtensionContext context) throws IOException{ return Files.createTempDirectory("junit").toRealPath(); }
  }
}

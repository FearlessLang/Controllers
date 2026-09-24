package benchmarks;

import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import tools.Fs;

/// Builds every runtime named in -Dfearless.benchmarks.stlibs (name=StandardLibraryRoot;...), then runs
/// the JMH benchmarks whose name matches the first argument (all of them with no argument) on each,
/// writing the results as Controllers/.out/benchmarks/results.json.
public final class Main{
  private Main(){}
  public static void main(String[] args) throws RunnerException{
    var names= Runtimes.buildAll();
    Fs.ensureDir(Runtimes.out);
    var opts= new OptionsBuilder()
      .include(args.length == 0 ? ".*" : args[0])
      .param("runtime", names.toArray(String[]::new))
      .result(Runtimes.out.resolve("results.json").toString())
      .resultFormat(ResultFormatType.JSON)
      .build();
    new Runner(opts).run();
  }
}

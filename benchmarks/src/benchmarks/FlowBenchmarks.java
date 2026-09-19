package benchmarks;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(1)
@Warmup(iterations= 2, time= 2)
@Measurement(iterations= 3, time= 2)
public class FlowBenchmarks{
  @Param({"wc"}) public String runtime;
  Runtimes.Loaded loaded;
  @Setup(Level.Trial) public void setup(){ loaded= Runtimes.load(runtime); }

  @Benchmark public void cheapDP(){ loaded.run("FlowOfCheapOpsDP"); }
  @Benchmark public void cheapPP(){ loaded.run("FlowOfCheapOpsPP"); }
  @Benchmark public void cheapSeq(){ loaded.run("FlowOfCheapOpsSeq"); }

  @Benchmark public void expensiveDP(){ loaded.run("FlowOfExpensiveOpsDP"); }
  @Benchmark public void expensivePP(){ loaded.run("FlowOfExpensiveOpsPP"); }
  @Benchmark public void expensiveDPThenPP(){ loaded.run("FlowOfExpensiveOpsDPThenPP"); }
  @Benchmark public void expensiveSeq(){ loaded.run("FlowOfExpensiveOpsSeq"); }

  @Benchmark public void fewElemsManyOpsDP(){ loaded.run("FlowOfFewElemsManyOpsDP"); }
  @Benchmark public void fewElemsManyOpsPP(){ loaded.run("FlowOfFewElemsManyOpsPP"); }
  @Benchmark public void fewElemsManyOpsSeq(){ loaded.run("FlowOfFewElemsManyOpsSeq"); }

  @Benchmark public void manyCheapDP(){ loaded.run("FlowOfManyCheapOpsDP"); }
  @Benchmark public void manyCheapPP(){ loaded.run("FlowOfManyCheapOpsPP"); }
  @Benchmark public void manyCheapSeq(){ loaded.run("FlowOfManyCheapOpsSeq"); }

  @Benchmark public void unevenDP(){ loaded.run("FlowOfUnevenOpsDP"); }
  @Benchmark public void unevenPP(){ loaded.run("FlowOfUnevenOpsPP"); }
  @Benchmark public void unevenSeq(){ loaded.run("FlowOfUnevenOpsSeq"); }

  @Benchmark public void firstDP(){ loaded.run("FlowFirstDP"); }
  @Benchmark public void firstSeq(){ loaded.run("FlowFirstSeq"); }
  @Benchmark public void anyLastDP(){ loaded.run("FlowAnyLastDP"); }
  @Benchmark public void anyLastSeq(){ loaded.run("FlowAnyLastSeq"); }

  @Benchmark public void fibSeq(){ loaded.run("FibSeq"); }
  @Benchmark public void fibFlow(){ loaded.run("FibFlow"); }
  @Benchmark public void fibFlowAlways(){ loaded.run("FibFlowAlways"); }

  @Benchmark public void moleculesPar(){ loaded.run("MoleculesPar"); }
  @Benchmark public void moleculesSeq(){ loaded.run("MoleculesSeq"); }

  @Benchmark public void primesPar(){ loaded.run("PrimesPar"); }
  @Benchmark public void primesSeq(){ loaded.run("PrimesSeq"); }

  @Benchmark public void sortCost30(){ loaded.run("SortCost30"); }
  @Benchmark public void sortCost3000(){ loaded.run("SortCost3000"); }
}

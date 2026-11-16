package benchmarks.additional;

import bridges.NativeBridge;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(value = 3, jvmArgsAppend = {"--enable-native-access=ALL-UNNAMED", "-Xms2g", "-Xmx2g"})
public class NoopBenchmark {
    @Param({"jni", "ffm", "ffm-critical"})
    public String bridge;
    private NativeBridge nativeBridge;

    @Setup
    public void open() {
        nativeBridge = NativeBridge.of(bridge);
        nativeBridge.openProbe(0);
    }

    @TearDown
    public void close() {
        nativeBridge.close();
    }

    @Benchmark
    public int noop() {
        return nativeBridge.noop();
    }

    @State(Scope.Thread)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Warmup(iterations = 5, time = 2)
    @Measurement(iterations = 5, time = 2)
    @Fork(value = 3, jvmArgsAppend = {"-Xms2g", "-Xmx2g"})
    public static class JavaCall {
        @Benchmark
        public int javaCall() {
            return javaNoop();
        }

        @CompilerControl(CompilerControl.Mode.DONT_INLINE)
        private static int javaNoop() {
            return 0;
        }
    }
}

package benchmarks.additional;

import bridges.NativeBridge;
import org.openjdk.jmh.annotations.*;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(value = 3, jvmArgsAppend = {"--enable-native-access=ALL-UNNAMED", "-Xms2g", "-Xmx2g"})
public class TransferBenchmark {
    private static final long POOL_BYTES = 64L << 20;

    @Param({"jni-copy", "jni-critical", "jni-direct", "ffm", "ffm-critical", "ffm-heap"})
    public String bridge;
    @Param({"320x240", "640x480", "1280x720", "1920x1080"})
    public String size;
    @Param({"1", "3"})
    public int channels;
    private NativeBridge nativeBridge;
    private byte[][] frames;
    private int next;

    @Setup
    public void open() {
        String[] wh = size.split("x");
        int bytes = Integer.parseInt(wh[0]) * Integer.parseInt(wh[1]) * channels;
        Random rnd = new Random(42);
        frames = new byte[(int) Math.max(8, POOL_BYTES / bytes)][bytes];
        for (byte[] f : frames) rnd.nextBytes(f);
        nativeBridge = NativeBridge.of(bridge);
        nativeBridge.openProbe(bytes);
    }

    @TearDown
    public void close() {
        nativeBridge.close();
    }

    @Benchmark
    public int transfer() {
        nativeBridge.stageFrame(frames[next]);
        next = (next + 1) % frames.length;
        return nativeBridge.touch();
    }
}

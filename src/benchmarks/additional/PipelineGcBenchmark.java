package benchmarks.additional;

import bridges.NativeBridge;
import org.openjdk.jmh.annotations.*;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 4)
public class PipelineGcBenchmark {
    private static final int WIDTH = 640, HEIGHT = 480;
    private static final int FRAMES = 8;
    private static final int UI_ALLOCS = 64, UI_CHUNK = 1024, UI_LIVE = 4096;

    @State(Scope.Thread)
    public static class Frame {
        @Param({"jni-copy", "jni-critical", "jni-direct", "ffm", "ffm-critical", "ffm-heap"})
        public String bridge;
        @Param({"1", "5", "20"})
        public int holdMs;
        private NativeBridge nativeBridge;
        private byte[][] frames;
        private int next;
        private long holdNs;

        @Setup
        public void open() {
            Random rnd = new Random(42);
            frames = new byte[FRAMES][WIDTH * HEIGHT];
            for (byte[] f : frames) rnd.nextBytes(f);
            nativeBridge = NativeBridge.of(bridge);
            nativeBridge.openProbe(WIDTH * HEIGHT);
            holdNs = TimeUnit.MILLISECONDS.toNanos(holdMs);
        }

        @TearDown
        public void close() {
            nativeBridge.close();
        }
    }

    @State(Scope.Thread)
    public static class Ui {
        private final byte[][] live = new byte[UI_LIVE][];
        private int next;
    }

    @Benchmark
    @Group("g1")
    @GroupThreads(1)
    @Fork(value = 2, jvmArgsAppend = {"--enable-native-access=ALL-UNNAMED", "-Xms1g", "-Xmx1g", "-Xmn64m", "-XX:+UseG1GC"})
    public int frameG1(Frame f) {
        return frame(f);
    }

    @Benchmark
    @Group("g1")
    @GroupThreads(1)
    @Fork(value = 2, jvmArgsAppend = {"--enable-native-access=ALL-UNNAMED", "-Xms1g", "-Xmx1g", "-Xmn64m", "-XX:+UseG1GC"})
    public int uiG1(Ui u) {
        return ui(u);
    }

    @Benchmark
    @Group("parallel")
    @GroupThreads(1)
    @Fork(value = 2, jvmArgsAppend = {"--enable-native-access=ALL-UNNAMED", "-Xms1g", "-Xmx1g", "-Xmn64m", "-XX:+UseParallelGC"})
    public int frameParallel(Frame f) {
        return frame(f);
    }

    @Benchmark
    @Group("parallel")
    @GroupThreads(1)
    @Fork(value = 2, jvmArgsAppend = {"--enable-native-access=ALL-UNNAMED", "-Xms1g", "-Xmx1g", "-Xmn64m", "-XX:+UseParallelGC"})
    public int uiParallel(Ui u) {
        return ui(u);
    }

    private static int frame(Frame f) {
        f.nativeBridge.stageFrame(f.frames[f.next]);
        f.next = (f.next + 1) % FRAMES;
        return f.nativeBridge.hold(f.holdNs);
    }

    private static int ui(Ui u) {
        int sum = 0;
        for (int i = 0; i < UI_ALLOCS; i++) {
            byte[] chunk = new byte[UI_CHUNK];
            chunk[i] = (byte) i;
            u.live[u.next] = chunk;
            u.next = (u.next + 1) % UI_LIVE;
            sum += chunk[0];
        }
        return sum;
    }
}

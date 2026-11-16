package benchmarks.additional;

import bridges.NativeBridge;
import org.openjdk.jmh.annotations.*;

import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(value = 3, jvmArgsAppend = {"--enable-native-access=ALL-UNNAMED", "-Xms2g", "-Xmx2g"})
public class CameraBufferBenchmark {
    private static final long POOL_BYTES = 64L << 20;

    @Param({"jni-direct", "ffm", "ffm-critical"})
    public String bridge;
    @Param({"320x240", "640x480", "1280x720", "1920x1080"})
    public String size;
    @Param({"1", "3"})
    public int channels;
    private NativeBridge nativeBridge;
    private ByteBuffer[] buffers;
    private int next;

    @Setup
    public void open() {
        String[] wh = size.split("x");
        int bytes = Integer.parseInt(wh[0]) * Integer.parseInt(wh[1]) * channels;
        Random rnd = new Random(42);
        byte[] tmp = new byte[bytes];
        buffers = new ByteBuffer[(int) Math.max(8, POOL_BYTES / bytes)];
        for (int i = 0; i < buffers.length; i++) {
            rnd.nextBytes(tmp);
            buffers[i] = ByteBuffer.allocateDirect(bytes).put(tmp).clear();
        }
        nativeBridge = NativeBridge.of(bridge);
        nativeBridge.openProbe(bytes);
    }

    @TearDown
    public void close() {
        nativeBridge.close();
    }

    @Benchmark
    public int transfer() {
        nativeBridge.stageBuffer(buffers[next]);
        next = (next + 1) % buffers.length;
        return nativeBridge.touch();
    }
}

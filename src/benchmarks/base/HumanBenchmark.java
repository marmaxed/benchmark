package benchmarks.base;

import benchmarks.helpers.BenchmarkDataHelper;
import benchmarks.helpers.NativeTime;
import bridges.NativeBridge;
import models.TrackerType;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Measurement(iterations = 5, time = 10)
@Warmup(iterations = 5, time = 2)
@Fork(value = 3, jvmArgsAppend = {"--enable-native-access=ALL-UNNAMED", "-Xms2g", "-Xmx2g"})
public class HumanBenchmark {
    @Param({"REAL_FACE_MEDIAPIPE", "REAL_FACE_ERT", "REAL_HAND_MEDIAPIPE", "REAL_HAND_ERT"})
    public TrackerType trackerType;
    @Param({"jni-copy", "jni-critical", "jni-direct", "ffm", "ffm-critical", "ffm-heap"})
    public String bridge;
    private NativeBridge nativeBridge;
    private byte[][] photos;
    private int next;
    private int outFloats;

    @Setup
    public void loadArgumentsByParams() {
        photos = BenchmarkDataHelper.getPhotos(trackerType);
        nativeBridge = NativeBridge.of(bridge);
        outFloats = BenchmarkDataHelper.landmarkFloats(trackerType);
        nativeBridge.open(trackerType, BenchmarkDataHelper.WIDTH, BenchmarkDataHelper.HEIGHT,
                          BenchmarkDataHelper.modelDir(trackerType));
    }

    @TearDown
    public void close() {
        BenchmarkDataHelper.printStats(nativeBridge);
        nativeBridge.close();
    }

    @Benchmark
    public void humanBenchmark(NativeTime time, Blackhole bh) {
        nativeBridge.stageFrame(photos[next]);
        next = (next + 1) % photos.length;
        time.add(nativeBridge.landmarks());
        time.frame();
        bh.consume(nativeBridge.out(outFloats));
    }
}

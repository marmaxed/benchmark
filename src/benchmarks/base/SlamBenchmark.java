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
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(value = 3, jvmArgsAppend = {"--enable-native-access=ALL-UNNAMED", "-Xms2g", "-Xmx2g"})
public class SlamBenchmark {
    private static final int IMU_PER_FRAME = 10;

    @Param({"REAL_SLAM_KLT", "REAL_SLAM_ORB"})
    public TrackerType trackerType;
    @Param({"jni-copy", "jni-critical", "jni-direct", "ffm", "ffm-critical", "ffm-heap"})
    public String bridge;
    private NativeBridge nativeBridge;
    private byte[][] photos;
    private int next;
    private BenchmarkDataHelper.Imu imu;
    private final float[] sample = new float[6];
    private int nextImu;
    private long lap;

    @TearDown
    public void close() {
        BenchmarkDataHelper.printStats(nativeBridge);
        nativeBridge.close();
    }

    @Setup
    public void loadArgumentsByParams() {
        photos = BenchmarkDataHelper.getPhotos(trackerType);
        imu = BenchmarkDataHelper.getImu();
        nativeBridge = NativeBridge.of(bridge);
        nativeBridge.open(trackerType, BenchmarkDataHelper.WIDTH, BenchmarkDataHelper.HEIGHT,
                BenchmarkDataHelper.modelDir(trackerType));
    }

    private long advanceImu() {
        if (nextImu == imu.size()) {
            nextImu = 0;
            lap += imu.span();
        }
        System.arraycopy(imu.samples(), nextImu * 6, sample, 0, 6);
        return imu.ts()[nextImu++] + lap;
    }

    @Benchmark
    public void slamBenchmark(NativeTime time, Blackhole bh) {
        for (int i = 0; i < IMU_PER_FRAME; i++) {
            long ts = advanceImu();
            time.add(nativeBridge.imuStep(sample, ts));
        }
        nativeBridge.stageFrame(photos[next]);
        next = (next + 1) % photos.length;
        time.add(nativeBridge.trackFrame());
        time.frame();
        bh.consume(nativeBridge.pose());
        bh.consume(nativeBridge.out(nativeBridge.pointCount() * 3));
    }
}

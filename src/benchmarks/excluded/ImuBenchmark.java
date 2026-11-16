package benchmarks.excluded;

import benchmarks.helpers.BenchmarkDataHelper;
import bridges.NativeBridge;
import models.TrackerType;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(value = 3, jvmArgsAppend = {"--enable-native-access=ALL-UNNAMED", "-Xms2g", "-Xmx2g"})
public class ImuBenchmark {
    private static final int BATCH = 10;
    private static final long DISPLAY_AHEAD_NS = 16_666_667L;
    @Param({"REAL_FILTER_ESKF", "REAL_FILTER_PREINT"})
    public TrackerType trackerType;
    @Param({"jni-critical", "ffm", "ffm-critical", "ffm-heap"})
    public String bridge;
    private NativeBridge nativeBridge;
    private BenchmarkDataHelper.Imu imu;
    private final float[] sample = new float[6];
    private final float[] batch = new float[BATCH * 6];
    private final long[] batchTs = new long[BATCH];
    private int next;
    private long lap;
    private long lastTs;

    @Setup
    public void loadArgumentsByParams() {
        imu = BenchmarkDataHelper.getImu();
        nativeBridge = NativeBridge.of(bridge);
        nativeBridge.open(trackerType, 0, 0);  // ядро без картинки
        // секунда IMU до замера — иначе predictPose не от чего экстраполировать
        for (int i = 0; i < 200; i++) {
            long ts = advance();
            nativeBridge.imuStep(sample, ts);
        }
    }

    @TearDown
    public void close() {
        nativeBridge.close();
    }

    private long advance() {
        if (next == imu.size()) {
            next = 0;
            lap += imu.span();
        }
        System.arraycopy(imu.samples(), next * 6, sample, 0, 6);
        lastTs = imu.ts()[next++] + lap;
        return lastTs;
    }

    @Benchmark
    public long imuStep() {
        long ts = advance();
        return nativeBridge.imuStep(sample, ts);
    }

    @Benchmark
    public long imuStep6() {
        long ts = advance();
        return nativeBridge.imuStep6(sample[0], sample[1], sample[2], sample[3], sample[4], sample[5], ts);
    }

    @Benchmark
    public long imuBatch() {
        for (int i = 0; i < BATCH; i++) {
            batchTs[i] = advance();
            System.arraycopy(sample, 0, batch, i * 6, 6);
        }
        return nativeBridge.imuBatch(batch, batchTs, BATCH);
    }

    @Benchmark
    public float[] predictPose() {
        nativeBridge.predictPose(lastTs + DISPLAY_AHEAD_NS);
        return nativeBridge.pose();
    }
}

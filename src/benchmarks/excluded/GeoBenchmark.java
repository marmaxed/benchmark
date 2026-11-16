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
public class GeoBenchmark {
    private static final float ACCURACY_M = 3f;
    @Param({"jni-critical", "ffm", "ffm-critical", "ffm-heap"})
    public String bridge;
    private NativeBridge nativeBridge;
    private BenchmarkDataHelper.Gnss gnss;
    private int next;

    @Setup
    public void loadArgumentsByParams() {
        gnss = BenchmarkDataHelper.getGnss(1000);
        nativeBridge = NativeBridge.of(bridge);
        nativeBridge.open(TrackerType.REAL_FILTER_ESKF, 0, 0);
        nativeBridge.gpsUpdate(gnss.lat()[0], gnss.lon()[0], gnss.alt()[0], ACCURACY_M);
        next = 1;
    }

    @TearDown
    public void close() {
        nativeBridge.close();
    }

    @Benchmark
    public long gpsUpdate() {
        if (next == gnss.size()) next = 1;
        long ns = nativeBridge.gpsUpdate(gnss.lat()[next], gnss.lon()[next], gnss.alt()[next], ACCURACY_M);
        next++;
        return ns;
    }
}

package benchmarks.additional;

import bridges.FfmBridge;
import bridges.JniBridge;
import bridges.NativeBridge;
import models.Landmark;
import org.openjdk.jmh.annotations.*;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import static java.lang.foreign.MemoryLayout.PathElement.groupElement;
import static java.lang.foreign.MemoryLayout.PathElement.sequenceElement;
import static java.lang.foreign.ValueLayout.JAVA_FLOAT;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(value = 3, jvmArgsAppend = {"--enable-native-access=ALL-UNNAMED", "-Xms2g", "-Xmx2g"})
public class ReturnBenchmark {
    @Param({"21", "68", "478", "1000"})
    public int points;
    private static final int[] FEW = {33, 263, 168, 127, 356};

    private static final MemoryLayout POINT = MemoryLayout.structLayout(
            JAVA_FLOAT.withName("x"), JAVA_FLOAT.withName("y"), JAVA_FLOAT.withName("z"));
    private static final MemoryLayout POINTS = MemoryLayout.sequenceLayout(NativeBridge.TRACK_MAX_POINTS, POINT);
    private static final VarHandle X = POINTS.varHandle(sequenceElement(), groupElement("x"));
    private static final VarHandle Y = POINTS.varHandle(sequenceElement(), groupElement("y"));
    private static final VarHandle Z = POINTS.varHandle(sequenceElement(), groupElement("z"));

    private JniBridge jni;
    private FfmBridge ffm;

    @Setup
    public void open() {
        jni = (JniBridge) NativeBridge.of("jni");
        jni.openProbe(0);
        ffm = (FfmBridge) NativeBridge.of("ffm");
        ffm.openProbe(0);
    }

    @TearDown
    public void close() {
        jni.close();
        ffm.close();
    }

    @Benchmark
    public float[] jniArray() {
        return jni.fill(points * 3);
    }

    @Benchmark
    public ByteBuffer jniDirect() {
        return jni.fillDirect(points * 3);
    }

    @Benchmark
    public float jniDirectFew() {
        ByteBuffer buf = jni.fillDirect(points * 3);
        float s = 0;
        for (int p : FEW) {
            int base = (p % points) * 3 * Float.BYTES;
            s += buf.getFloat(base) + buf.getFloat(base + 4) + buf.getFloat(base + 8);
        }
        return s;
    }

    @Benchmark
    public Landmark[] jniObjects() {
        return jni.fillObjects(points);
    }

    @Benchmark
    public Landmark[] jniObjectsJava() {
        float[] a = jni.fill(points * 3);
        Landmark[] result = new Landmark[points];
        for (int i = 0; i < points; i++) {
            result[i] = new Landmark(a[3 * i], a[3 * i + 1], a[3 * i + 2]);
        }
        return result;
    }

    @Benchmark
    public float[] ffmArray() {
        return ffm.fill(points * 3);
    }

    @Benchmark
    public Landmark[] ffmObjects() {
        return ffm.fillObjects(points);
    }

    @Benchmark
    public MemorySegment ffmSegment() {
        return ffm.fillSegment(points * 3);
    }

    @Benchmark
    public float ffmSegmentFew() {
        MemorySegment seg = ffm.fillSegment(points * 3);
        float s = 0;
        for (int p : FEW) {
            long i = p % points;
            s += (float) X.get(seg, 0L, i) + (float) Y.get(seg, 0L, i) + (float) Z.get(seg, 0L, i);
        }
        return s;
    }
}

package bridges;

import models.Landmark;
import models.TrackerStat;
import models.TrackerType;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.ByteBuffer;

import static java.lang.foreign.ValueLayout.*;

public final class FfmBridge implements NativeBridge {
    private MemorySegment outSeg;
    private MemorySegment countSeg;
    private final float[] out = new float[OUT_FLOATS];
    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LIB =
            SymbolLookup.libraryLookup(Libs.path("libreal.so"), Arena.global());

    private static final FunctionDescriptor REAL_CREATE = FunctionDescriptor.of(ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS);
    private static final FunctionDescriptor REAL_DESTROY = FunctionDescriptor.ofVoid(ADDRESS);

    private static final FunctionDescriptor REAL_LAST_ERROR = FunctionDescriptor.of(ADDRESS);
    private static final FunctionDescriptor REAL_SET_THREAD = FunctionDescriptor.ofVoid(JAVA_INT);

    private static final FunctionDescriptor REAL_SET_PARAM = FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_DOUBLE);

    private static final FunctionDescriptor REAL_SET_REFERENCE = FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT);

    private static final FunctionDescriptor REAL_PROCESS = FunctionDescriptor.of(JAVA_LONG, ADDRESS, ADDRESS, JAVA_LONG, ADDRESS);

    private static final FunctionDescriptor REAL_LANDMARKS = FunctionDescriptor.of(JAVA_LONG, ADDRESS, ADDRESS, JAVA_LONG, ADDRESS, JAVA_INT);

    private static final FunctionDescriptor REAL_TRACK_FRAME = FunctionDescriptor.of(JAVA_LONG, ADDRESS, ADDRESS, JAVA_LONG, ADDRESS, ADDRESS, JAVA_INT, ADDRESS);

    private static final FunctionDescriptor REAL_IMU_STEP = FunctionDescriptor.of(JAVA_LONG, ADDRESS, ADDRESS, JAVA_LONG);

    private static final FunctionDescriptor REAL_IMU_STEP6 = FunctionDescriptor.of(JAVA_LONG, ADDRESS, JAVA_FLOAT, JAVA_FLOAT, JAVA_FLOAT,
            JAVA_FLOAT, JAVA_FLOAT, JAVA_FLOAT, JAVA_LONG);

    private static final FunctionDescriptor REAL_IMU_BATCH = FunctionDescriptor.of(JAVA_LONG, ADDRESS, ADDRESS, ADDRESS, JAVA_INT);
    private static final FunctionDescriptor REAL_GPS_UPDATE = FunctionDescriptor.of(JAVA_LONG, ADDRESS, JAVA_DOUBLE, JAVA_DOUBLE, JAVA_DOUBLE, JAVA_FLOAT);
    private static final FunctionDescriptor REAL_PREDICT_POSE = FunctionDescriptor.of(JAVA_LONG, ADDRESS, JAVA_LONG, ADDRESS);

    private static final FunctionDescriptor REAL_STATS = FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, JAVA_INT);
    private static final FunctionDescriptor REAL_RESET_STATS = FunctionDescriptor.ofVoid(ADDRESS);

    private static final FunctionDescriptor REAL_NOOP = FunctionDescriptor.of(JAVA_INT);
    private static final FunctionDescriptor REAL_TOUCH = FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_LONG);
    private static final FunctionDescriptor REAL_HOLD = FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_LONG, JAVA_LONG);
    private static final FunctionDescriptor REAL_FILL = FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT);
    private static final MethodHandle CREATE = handle("real_create", REAL_CREATE);
    private static final MethodHandle DESTROY = handle("real_destroy", REAL_DESTROY);
    private static final MethodHandle LAST_ERROR = handle("real_last_error", REAL_LAST_ERROR);
    private static final MethodHandle SET_THREADS = handle("real_set_threads", REAL_SET_THREAD);
    private static final MethodHandle SET_PARAM = handle("real_set_param", REAL_SET_PARAM);
    private static final MethodHandle SET_REFERENCE = handle("real_set_reference", REAL_SET_REFERENCE);

    private static final MethodHandle PROCESS = handle("real_process", REAL_PROCESS);
    private static final MethodHandle PROCESS_CRIT = critical("real_process", REAL_PROCESS);
    private static final MethodHandle PROCESS_HEAP = heap("real_process", REAL_PROCESS);
    private static final MethodHandle LANDMARKS = handle("real_landmarks", REAL_LANDMARKS);
    private static final MethodHandle LANDMARKS_CRIT = critical("real_landmarks", REAL_LANDMARKS);
    private static final MethodHandle TRACK = handle("real_track_frame", REAL_TRACK_FRAME);
    private static final MethodHandle TRACK_CRIT = critical("real_track_frame", REAL_TRACK_FRAME);
    private static final MethodHandle IMU = handle("real_imu_step", REAL_IMU_STEP);
    private static final MethodHandle IMU_CRIT = critical("real_imu_step", REAL_IMU_STEP);
    private static final MethodHandle IMU6 = handle("real_imu_step6", REAL_IMU_STEP6);
    private static final MethodHandle IMU6_CRIT = critical("real_imu_step6", REAL_IMU_STEP6);
    private static final MethodHandle IMU_BATCH = handle("real_imu_batch", REAL_IMU_BATCH);
    private static final MethodHandle IMU_BATCH_CRIT = critical("real_imu_batch", REAL_IMU_BATCH);
    private static final MethodHandle GPS = handle("real_gps_update", REAL_GPS_UPDATE);
    private static final MethodHandle GPS_CRIT = critical("real_gps_update", REAL_GPS_UPDATE);
    private static final MethodHandle PREDICT = handle("real_predict_pose", REAL_PREDICT_POSE);
    private static final MethodHandle PREDICT_CRIT = critical("real_predict_pose", REAL_PREDICT_POSE);
    private static final MethodHandle LANDMARKS_HEAP = heap("real_landmarks", REAL_LANDMARKS);
    private static final MethodHandle TRACK_HEAP = heap("real_track_frame", REAL_TRACK_FRAME);
    private static final MethodHandle IMU_HEAP = heap("real_imu_step", REAL_IMU_STEP);
    private static final MethodHandle IMU_BATCH_HEAP = heap("real_imu_batch", REAL_IMU_BATCH);

    private static final MethodHandle STATS = handle("real_stats", REAL_STATS);
    private static final MethodHandle RESET_STATS = handle("real_reset_stats", REAL_RESET_STATS);

    private static final MethodHandle NOOP = handle("real_noop", REAL_NOOP);
    private static final MethodHandle NOOP_CRIT = critical("real_noop", REAL_NOOP);
    private static final MethodHandle TOUCH = handle("real_touch", REAL_TOUCH);
    private static final MethodHandle TOUCH_CRIT = critical("real_touch", REAL_TOUCH);
    private static final MethodHandle TOUCH_HEAP = heap("real_touch", REAL_TOUCH);
    private static final MethodHandle HOLD = handle("real_hold", REAL_HOLD);
    private static final MethodHandle HOLD_CRIT = critical("real_hold", REAL_HOLD);
    private static final MethodHandle HOLD_HEAP = heap("real_hold", REAL_HOLD);
    private static final MethodHandle FILL = handle("real_fill", REAL_FILL);
    private static final MethodHandle FILL_CRIT = critical("real_fill", REAL_FILL);

    public enum Mode {
        OFFHEAP,
        OFFHEAP_CRITICAL,
        HEAP_CRITICAL
    }

    private final Mode mode;
    private Arena arena;
    private MemorySegment ctx;
    private MemorySegment frameSeg;
    private byte[] heapFrame;
    private long frameLen;
    private MemorySegment poseSeg;
    private final float[] pose = new float[7];
    private MemorySegment imuSeg;
    private MemorySegment batchSeg;
    private MemorySegment batchTsSeg;
    private MemorySegment statsSeg;
    private final long[] stats = new long[TrackerStat.REAL_STAT_COUNT.ordinal()];

    public FfmBridge(Mode mode) {
        this.mode = mode;
    }

    @Override
    public String name() {
        return switch (mode) {
            case OFFHEAP -> "ffm";
            case OFFHEAP_CRITICAL -> "ffm-critical";
            case HEAP_CRITICAL -> "ffm-heap";
        };
    }

    @Override
    public void open(TrackerType type, int width, int height, String modelDir) {
        arena = Arena.ofConfined();
        frameLen = (long) width * height * type.colors();
        frameSeg = arena.allocate(frameLen);
        poseSeg = arena.allocate(JAVA_FLOAT, pose.length);
        outSeg = arena.allocate(JAVA_FLOAT, OUT_FLOATS);
        countSeg = arena.allocate(JAVA_INT);
        imuSeg = arena.allocate(JAVA_FLOAT, IMU_FLOATS);
        batchSeg = arena.allocate(JAVA_FLOAT, (long) IMU_FLOATS * IMU_BATCH_MAX);
        batchTsSeg = arena.allocate(JAVA_LONG, IMU_BATCH_MAX);
        statsSeg = arena.allocate(JAVA_LONG, stats.length);
        MemorySegment dir = modelDir == null ? MemorySegment.NULL : arena.allocateFrom(modelDir);
        try {
            ctx = (MemorySegment) CREATE.invokeExact(type.code(), width, height, dir);
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
        if (ctx.equals(MemorySegment.NULL)) throw new IllegalStateException(lastError());
    }

    @Override
    public void openProbe(int frameBytes) {
        arena = Arena.ofConfined();
        frameLen = frameBytes;
        frameSeg = arena.allocate(frameLen);
        outSeg = arena.allocate(JAVA_FLOAT, OUT_FLOATS);
    }

    @Override
    public void setParam(String key, double value) {
        int r;
        try {
            r = (int) SET_PARAM.invokeExact(ctx, arena.allocateFrom(key), value);
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
        if (r != 0) throw new IllegalArgumentException("real_set_param " + key + ": " + lastError());
    }

    @Override
    public void setReference(byte[] gray, int width, int height) {
        int r;
        try (Arena tmp = Arena.ofConfined()) {
            MemorySegment seg = tmp.allocate(gray.length);
            MemorySegment.copy(gray, 0, seg, JAVA_BYTE, 0, gray.length);
            r = (int) SET_REFERENCE.invokeExact(ctx, seg, width, height);
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
        if (r != 0) throw new IllegalStateException(lastError());
    }

    @Override
    public void close() {
        if (ctx != null && !ctx.equals(MemorySegment.NULL)) {
            try {
                DESTROY.invokeExact(ctx);
            } catch (Throwable t) {
                throw new IllegalStateException(t);
            }
        }
        ctx = null;
        if (arena != null) arena.close();
        arena = null;
    }

    @Override
    public void stageFrame(byte[] frame) {
        if (mode == Mode.HEAP_CRITICAL) {
            heapFrame = frame;
        } else {
            MemorySegment.copy(frame, 0, frameSeg, JAVA_BYTE, 0, frame.length);
        }
    }

    @Override
    public long process() {
        long ns;
        try {
            ns = switch (mode) {
                case OFFHEAP -> (long) PROCESS.invokeExact(ctx, frameSeg, frameLen, poseSeg);
                case OFFHEAP_CRITICAL -> (long) PROCESS_CRIT.invokeExact(ctx, frameSeg, frameLen, poseSeg);
                case HEAP_CRITICAL -> (long) PROCESS_HEAP.invokeExact(ctx, MemorySegment.ofArray(heapFrame), frameLen, poseSeg);
            };
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
        if (ns < 0) throw new IllegalStateException(lastError());
        return ns;
    }

    @Override
    public long landmarks() {
        long ns;
        try {
            ns = switch (mode) {
                case OFFHEAP -> (long) LANDMARKS.invokeExact(ctx, frameSeg, frameLen, outSeg, LANDMARKS_OUT);
                case OFFHEAP_CRITICAL -> (long) LANDMARKS_CRIT.invokeExact(ctx, frameSeg, frameLen, outSeg, LANDMARKS_OUT);
                case HEAP_CRITICAL -> (long) LANDMARKS_HEAP.invokeExact(ctx, MemorySegment.ofArray(heapFrame), frameLen, outSeg, LANDMARKS_OUT);
            };
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
        if (ns < 0) throw new IllegalStateException(lastError());
        return ns;
    }

    @Override
    public float[] out(int n) {
        MemorySegment.copy(outSeg, JAVA_FLOAT, 0, out, 0, n);
        return out;
    }

    @Override
    public long trackFrame() {
        long ns;
        try {
            ns = switch (mode) {
                case OFFHEAP -> (long) TRACK.invokeExact(ctx, frameSeg, frameLen, poseSeg, outSeg, TRACK_MAX_POINTS, countSeg);
                case OFFHEAP_CRITICAL -> (long) TRACK_CRIT.invokeExact(ctx, frameSeg, frameLen, poseSeg, outSeg, TRACK_MAX_POINTS, countSeg);
                case HEAP_CRITICAL -> (long) TRACK_HEAP.invokeExact(ctx, MemorySegment.ofArray(heapFrame), frameLen, poseSeg, outSeg, TRACK_MAX_POINTS, countSeg);
            };
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
        if (ns < 0) throw new IllegalStateException(lastError());
        return ns;
    }

    @Override
    public int pointCount() {
        return countSeg.get(JAVA_INT, 0);
    }

    @Override
    public long imuStep(float[] sample, long tsNanos) {
        long ns;
        try {
            ns = switch (mode) {
                case OFFHEAP -> {
                    MemorySegment.copy(sample, 0, imuSeg, JAVA_FLOAT, 0, IMU_FLOATS);
                    yield (long) IMU.invokeExact(ctx, imuSeg, tsNanos);
                }
                case OFFHEAP_CRITICAL -> {
                    MemorySegment.copy(sample, 0, imuSeg, JAVA_FLOAT, 0, IMU_FLOATS);
                    yield (long) IMU_CRIT.invokeExact(ctx, imuSeg, tsNanos);
                }
                case HEAP_CRITICAL -> (long) IMU_HEAP.invokeExact(ctx, MemorySegment.ofArray(sample), tsNanos);
            };
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
        if (ns < 0) throw new IllegalStateException(lastError());
        return ns;
    }

    @Override
    public long imuStep6(float ax, float ay, float az, float gx, float gy, float gz, long tsNanos) {
        long ns;
        try {
            ns = switch (mode) {
                case OFFHEAP -> (long) IMU6.invokeExact(ctx, ax, ay, az, gx, gy, gz, tsNanos);
                case OFFHEAP_CRITICAL, HEAP_CRITICAL -> (long) IMU6_CRIT.invokeExact(ctx, ax, ay, az, gx, gy, gz, tsNanos);
            };
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
        if (ns < 0) throw new IllegalStateException(lastError());
        return ns;
    }

    @Override
    public long imuBatch(float[] samples, long[] ts, int n) {
        long ns;
        try {
            ns = switch (mode) {
                case OFFHEAP -> {
                    MemorySegment.copy(samples, 0, batchSeg, JAVA_FLOAT, 0, n * IMU_FLOATS);
                    MemorySegment.copy(ts, 0, batchTsSeg, JAVA_LONG, 0, n);
                    yield (long) IMU_BATCH.invokeExact(ctx, batchSeg, batchTsSeg, n);
                }
                case OFFHEAP_CRITICAL -> {
                    MemorySegment.copy(samples, 0, batchSeg, JAVA_FLOAT, 0, n * IMU_FLOATS);
                    MemorySegment.copy(ts, 0, batchTsSeg, JAVA_LONG, 0, n);
                    yield (long) IMU_BATCH_CRIT.invokeExact(ctx, batchSeg, batchTsSeg, n);
                }
                case HEAP_CRITICAL -> (long) IMU_BATCH_HEAP.invokeExact(ctx,
                        MemorySegment.ofArray(samples), MemorySegment.ofArray(ts), n);
            };
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
        if (ns < 0) throw new IllegalStateException(lastError());
        return ns;
    }

    @Override
    public long gpsUpdate(double lat, double lon, double alt, float accuracyM) {
        long ns;
        try {
            ns = switch (mode) {
                case OFFHEAP -> (long) GPS.invokeExact(ctx, lat, lon, alt, accuracyM);
                // скаляры: heap ни при чём, ffm-heap = ffm-critical
                case OFFHEAP_CRITICAL, HEAP_CRITICAL -> (long) GPS_CRIT.invokeExact(ctx, lat, lon, alt, accuracyM);
            };
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
        if (ns < 0) throw new IllegalStateException(lastError());
        return ns;
    }

    @Override
    public long predictPose(long tsNanos) {
        long ns;
        try {
            ns = switch (mode) {
                case OFFHEAP -> (long) PREDICT.invokeExact(ctx, tsNanos, poseSeg);
                case OFFHEAP_CRITICAL, HEAP_CRITICAL -> (long) PREDICT_CRIT.invokeExact(ctx, tsNanos, poseSeg);
            };
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
        if (ns < 0) throw new IllegalStateException(lastError());
        return ns;
    }
    @Override
    public float[] pose() {
        MemorySegment.copy(poseSeg, JAVA_FLOAT, 0, pose, 0, pose.length);
        return pose;
    }

    @Override
    public long[] stats() {
        try {
            STATS.invokeExact(ctx, statsSeg, stats.length);
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
        MemorySegment.copy(statsSeg, JAVA_LONG, 0, stats, 0, stats.length);
        return stats;
    }

    @Override
    public void resetStats() {
        try {
            RESET_STATS.invokeExact(ctx);
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    @Override
    public int noop() {
        try {
            return switch (mode) {
                case OFFHEAP -> (int) NOOP.invokeExact();
                case OFFHEAP_CRITICAL, HEAP_CRITICAL -> (int) NOOP_CRIT.invokeExact();
            };
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    @Override
    public int touch() {
        try {
            return switch (mode) {
                case OFFHEAP -> (int) TOUCH.invokeExact(frameSeg, frameLen);
                case OFFHEAP_CRITICAL -> (int) TOUCH_CRIT.invokeExact(frameSeg, frameLen);
                case HEAP_CRITICAL -> (int) TOUCH_HEAP.invokeExact(MemorySegment.ofArray(heapFrame), frameLen);
            };
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    @Override
    public int hold(long ns) {
        try {
            return switch (mode) {
                case OFFHEAP -> (int) HOLD.invokeExact(frameSeg, frameLen, ns);
                case OFFHEAP_CRITICAL -> (int) HOLD_CRIT.invokeExact(frameSeg, frameLen, ns);
                case HEAP_CRITICAL -> (int) HOLD_HEAP.invokeExact(MemorySegment.ofArray(heapFrame), frameLen, ns);
            };
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    @Override
    public void stageBuffer(ByteBuffer frame) {
        if (mode == Mode.HEAP_CRITICAL) throw new UnsupportedOperationException("ffm-heap: кадр должен быть в byte[]");
        frameSeg = MemorySegment.ofBuffer(frame);
        frameLen = frameSeg.byteSize();
    }

    public MemorySegment fillSegment(int n) {
        try {
            switch (mode) {
                case OFFHEAP -> FILL.invokeExact(outSeg, n);
                case OFFHEAP_CRITICAL, HEAP_CRITICAL -> FILL_CRIT.invokeExact(outSeg, n);
            }
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
        return outSeg;
    }

    @Override
    public float[] fill(int n) {
        fillSegment(n);
        return out(n);
    }

    @Override
    public Landmark[] fillObjects(int nPoints) {
        MemorySegment seg = fillSegment(nPoints * 3);
        Landmark[] result = new Landmark[nPoints];
        for (int i = 0; i < nPoints; i++) {
            long base = 12L * i;
            result[i] = new Landmark(seg.get(JAVA_FLOAT, base), seg.get(JAVA_FLOAT, base + 4),
                    seg.get(JAVA_FLOAT, base + 8));
        }
        return result;
    }

    private static String lastError() {
        try {
            MemorySegment msg = (MemorySegment) LAST_ERROR.invokeExact();
            return msg.equals(MemorySegment.NULL) ? "" : msg.reinterpret(Long.MAX_VALUE).getString(0);
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    private static MethodHandle handle(String symbol, FunctionDescriptor desc) {
        return LINKER.downcallHandle(LIB.findOrThrow(symbol), desc);
    }

    private static MethodHandle critical(String symbol, FunctionDescriptor desc) {
        return LINKER.downcallHandle(LIB.findOrThrow(symbol), desc, Linker.Option.critical(false));
    }

    private static MethodHandle heap(String symbol, FunctionDescriptor desc) {
        return LINKER.downcallHandle(LIB.findOrThrow(symbol), desc, Linker.Option.critical(true));
    }
}

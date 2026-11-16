package bridges;

import models.Landmark;
import models.TrackerStat;
import models.TrackerType;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public final class JniBridge implements NativeBridge {
    private final float[] out = new float[OUT_FLOATS];
    private final ByteBuffer outDirect = ByteBuffer.allocateDirect(OUT_FLOATS * Float.BYTES)
            .order(ByteOrder.nativeOrder());
    private final ByteBuffer poseDirect = ByteBuffer.allocateDirect(7 * Float.BYTES).order(ByteOrder.nativeOrder());
    private final ByteBuffer countDirect = ByteBuffer.allocateDirect(Integer.BYTES).order(ByteOrder.nativeOrder());
    private final FloatBuffer outDirectFloats = outDirect.asFloatBuffer();
    private final FloatBuffer poseDirectFloats = poseDirect.asFloatBuffer();
    private boolean resultDirect;
    private final float[] marks = new float[LANDMARKS_OUT];
    private float[] lastOut = out;
    private final int[] count = new int[1];
    private final long[] stats = new long[TrackerStat.REAL_STAT_COUNT.ordinal()];

    static {
        System.load(Libs.path("libreal.so").toString());
    }

    public enum Mode {
        COPY,
        CRITICAL,
        DIRECT
    }
    private final Mode mode;
    private long ctx;
    private byte[] frame;
    private ByteBuffer direct;
    private final float[] pose = new float[7];

    public JniBridge(Mode mode) {
        this.mode = mode;
    }

    @Override
    public String name() {
        return "jni-" + mode.name().toLowerCase();
    }

    @Override
    public void open(TrackerType type, int width, int height, String modelDir) {
        ctx = create(type.code(), width, height, modelDir);
        if (ctx == 0) throw new IllegalStateException(lastError());
        if (mode == Mode.DIRECT) direct = ByteBuffer.allocateDirect(width * height * type.colors());
    }

    @Override
    public void openProbe(int frameBytes) {
        if (mode == Mode.DIRECT) direct = ByteBuffer.allocateDirect(frameBytes);
    }

    @Override
    public long trackFrame() {
        long ns = switch (mode) {
            case COPY -> trackFrameCopy(ctx, frame, pose, out, count);
            case CRITICAL -> trackFrame(ctx, frame, pose, out, count);
            case DIRECT -> trackFrameDirect(ctx, direct, direct.capacity(), poseDirect, outDirect, TRACK_MAX_POINTS, countDirect);
        };
        if (ns < 0) throw new IllegalStateException(lastError());
        lastOut = out;
        resultDirect = mode == Mode.DIRECT;
        return ns;
    }

    @Override
    public int pointCount() {
        return resultDirect ? countDirect.getInt(0) : count[0];
    }

    @Override
    public void setParam(String key, double value) {
        if (setParam(ctx, key, value) != 0) throw new IllegalArgumentException("real_set_param " + key + ": " + lastError());
    }

    @Override
    public void setReference(byte[] gray, int width, int height) {
        if (setReference(ctx, gray, width, height) != 0) throw new IllegalStateException(lastError());
    }

    @Override
    public void close() {
        if (ctx != 0) destroy(ctx);
        ctx = 0;
    }

    @Override
    public void stageFrame(byte[] frame) {
        if (mode == Mode.DIRECT) {
            direct.clear();
            direct.put(frame);
        } else {
            this.frame = frame;
        }
    }

    @Override
    public long process() {
        long ns = switch (mode) {
            case COPY -> process(ctx, frame, pose);
            case CRITICAL -> processCritical(ctx, frame, pose);
            case DIRECT -> processDirect(ctx, direct, direct.capacity(), poseDirect);
        };
        if (ns < 0) throw new IllegalStateException(lastError());
        resultDirect = mode == Mode.DIRECT;
        return ns;
    }

    @Override
    public long landmarks() {
        long ns = switch (mode) {
            case COPY -> landmarksCopy(ctx, frame, marks);
            case CRITICAL -> landmarks(ctx, frame, marks);
            case DIRECT -> landmarksDirect(ctx, direct, direct.capacity(), outDirect, LANDMARKS_OUT);
        };
        if (ns < 0) throw new IllegalStateException(lastError());
        lastOut = marks;
        resultDirect = mode == Mode.DIRECT;
        return ns;
    }

    @Override
    public float[] out(int n) {
        if (resultDirect) outDirectFloats.get(0, lastOut, 0, n);
        return lastOut;
    }

    @Override
    public long imuStep(float[] sample, long tsNanos) {
        long ns = imuStep(ctx, sample, tsNanos);
        if (ns < 0) throw new IllegalStateException(lastError());
        return ns;
    }

    @Override
    public long imuStep6(float ax, float ay, float az, float gx, float gy, float gz, long tsNanos) {
        long ns = imuStep6(ctx, ax, ay, az, gx, gy, gz, tsNanos);
        if (ns < 0) throw new IllegalStateException(lastError());
        return ns;
    }

    @Override
    public long imuBatch(float[] samples, long[] ts, int n) {
        long ns = imuBatch(ctx, samples, ts, n);
        if (ns < 0) throw new IllegalStateException(lastError());
        return ns;
    }

    @Override
    public long gpsUpdate(double lat, double lon, double alt, float accuracyM) {
        long ns = gpsUpdate(ctx, lat, lon, alt, accuracyM);
        if (ns < 0) throw new IllegalStateException(lastError());
        return ns;
    }

    @Override
    public long predictPose(long tsNanos) {
        long ns = predictPose(ctx, tsNanos, pose);
        if (ns < 0) throw new IllegalStateException(lastError());
        resultDirect = false; // мелкий вызов датчиков — поза через float[] во всех режимах
        return ns;
    }

    @Override
    public float[] pose() {
        if (resultDirect) poseDirectFloats.get(0, pose, 0, pose.length);
        return pose;
    }

    @Override
    public long[] stats() {
        stats(ctx, stats);
        return stats;
    }

    @Override
    public void resetStats() {
        resetStats(ctx);
    }

    @Override
    public int noop() {
        return noopNative();
    }

    @Override
    public int touch() {
        int r = switch (mode) {
            case COPY -> touch(frame);
            case CRITICAL -> touchCritical(frame);
            case DIRECT -> touchDirect(direct, direct.capacity());
        };
        if (r < 0) throw new IllegalStateException("real_touch");
        return r;
    }

    @Override
    public int hold(long ns) {
        int r = switch (mode) {
            case COPY -> hold(frame, ns);
            case CRITICAL -> holdCritical(frame, ns);
            case DIRECT -> holdDirect(direct, direct.capacity(), ns);
        };
        if (r < 0) throw new IllegalStateException("real_hold");
        return r;
    }

    @Override
    public void stageBuffer(ByteBuffer frame) {
        if (mode != Mode.DIRECT) throw new UnsupportedOperationException(name() + ": кадр должен быть в byte[]");
        direct = frame;
    }

    @Override
    public float[] fill(int n) {
        fill(out, n);
        return out;
    }

    public ByteBuffer fillDirect(int n) {
        fillDirect(outDirect, n);
        return outDirect;
    }

    @Override
    public Landmark[] fillObjects(int nPoints) {
        return fillObjectsNative(nPoints);
    }

    private static native long create(int kind, int width, int height, String modelDir);

    private static native void destroy(long ctx);

    private static native String lastError();

    private static native void setThreads(int n);

    private static native int setParam(long ctx, String key, double value);

    private static native int setReference(long ctx, byte[] gray, int w, int h);

    private static native long process(long ctx, byte[] frame, float[] outPose);

    private static native long processCritical(long ctx, byte[] frame, float[] outPose);

    private static native long processDirect(long ctx, ByteBuffer frame, int len, ByteBuffer outPose);

    private static native long landmarks(long ctx, byte[] frame, float[] out);

    private static native long landmarksCopy(long ctx, byte[] frame, float[] out);

    private static native long landmarksDirect(long ctx, ByteBuffer frame, int len, ByteBuffer out, int nOut);

    private static native long trackFrame(long ctx, byte[] frame, float[] outPose, float[] outPoints, int[] outCount);

    private static native long trackFrameCopy(long ctx, byte[] frame, float[] outPose, float[] outPoints, int[] outCount);

    private static native long trackFrameDirect(long ctx, ByteBuffer frame, int len, ByteBuffer outPose,
                                                ByteBuffer outPoints, int maxPoints, ByteBuffer outCount);

    private static native long imuStep(long ctx, float[] sample, long tsNanos);

    private static native long imuStep6(long ctx, float ax, float ay, float az, float gx, float gy,
                                        float gz, long tsNanos);

    private static native long imuBatch(long ctx, float[] samples, long[] ts, int n);

    private static native long gpsUpdate(long ctx, double lat, double lon, double alt, float accuracyM);

    private static native long predictPose(long ctx, long tsNanos, float[] outPose);

    private static native void stats(long ctx, long[] out);

    private static native void resetStats(long ctx);

    private static native int noopNative();

    private static native int touch(byte[] frame);

    private static native int touchCritical(byte[] frame);

    private static native int touchDirect(ByteBuffer frame, int len);

    private static native int hold(byte[] frame, long ns);

    private static native int holdCritical(byte[] frame, long ns);

    private static native int holdDirect(ByteBuffer frame, int len, long ns);

    private static native void fill(float[] out, int n);

    private static native void fillDirect(ByteBuffer out, int n);

    private static native Landmark[] fillObjectsNative(int nPoints);
}

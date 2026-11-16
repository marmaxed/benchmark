package bridges;

import models.Landmark;
import models.TrackerType;

import java.nio.ByteBuffer;

public interface NativeBridge extends AutoCloseable {
    int LANDMARKS_OUT = 478 * 3 + 52;
    int TRACK_MAX_POINTS = 1000;
    int OUT_FLOATS = Math.max(LANDMARKS_OUT, TRACK_MAX_POINTS * 3);
    int IMU_FLOATS = 6;
    int IMU_BATCH_MAX = 64;

    static NativeBridge of(String name) {
        return switch (name) {
            case "jni", "jni-copy" -> new JniBridge(JniBridge.Mode.COPY);
            case "jni-critical" -> new JniBridge(JniBridge.Mode.CRITICAL);
            case "jni-direct" -> new JniBridge(JniBridge.Mode.DIRECT);
            case "ffm" -> new FfmBridge(FfmBridge.Mode.OFFHEAP);
            case "ffm-critical" -> new FfmBridge(FfmBridge.Mode.OFFHEAP_CRITICAL);
            case "ffm-heap" -> new FfmBridge(FfmBridge.Mode.HEAP_CRITICAL);
            default -> throw new IllegalArgumentException("неизвестный мост: " + name);
        };
    }

    String name();

    void open(TrackerType type, int width, int height, String modelDir);

    default void open(TrackerType type, int width, int height) {
        open(type, width, height, null);
    }

    void setParam(String key, double value);

    void setReference(byte[] gray, int width, int height);

    long[] stats();

    void resetStats();

    @Override
    void close();

    void stageFrame(byte[] frame);

    long process();

    long landmarks();

    float[] out(int n);

    long trackFrame();

    int pointCount();

    long imuStep(float[] sample, long tsNanos);

    long imuStep6(float ax, float ay, float az, float gx, float gy, float gz, long tsNanos);

    long imuBatch(float[] samples, long[] ts, int n);

    long gpsUpdate(double lat, double lon, double alt, float accuracyM);

    long predictPose(long tsNanos);

    float[] pose();

    void openProbe(int frameBytes);

    int noop();

    int touch();

    int hold(long ns);

    void stageBuffer(ByteBuffer frame);

    float[] fill(int n);

    Landmark[] fillObjects(int nPoints);
}

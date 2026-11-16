package benchmarks.helpers;

import bridges.NativeBridge;
import models.TrackerStat;
import models.TrackerType;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class BenchmarkDataHelper {
    public static final int WIDTH = 640;
    public static final int HEIGHT = 480;
    public static final int REFERENCE_WIDTH = 400;

    private static final Path MODELS = Path.of(System.getProperty("bench.models.dir", "models"));

    private static final Path DIR = Path.of(System.getProperty("bench.data.dir", "data"));

    public static byte[][] getPhotos(TrackerType trackerType) {
        String file = switch (trackerType) {
            case REAL_ARUCO -> "aruco_640x480.gray";
            case REAL_IMAGE_TARGET -> "target_640x480.gray";
            case REAL_FACE_MEDIAPIPE, REAL_FACE_ERT -> "face_640x480.rgb";
            case REAL_HAND_MEDIAPIPE, REAL_HAND_ERT -> "hand_640x480.rgb";
            case REAL_SLAM_KLT, REAL_SLAM_ORB -> "slam_640x480.gray";
            default -> throw new IllegalArgumentException("нет кадров для " + trackerType);
        };
        byte[] all = read(file);
        int frameBytes = WIDTH * HEIGHT* trackerType.colors();
        if (all.length == 0 || all.length % frameBytes != 0) {
            throw new IllegalStateException(file + ": " + all.length + " байт — не кратно кадру " + frameBytes);
        }
        byte[][] photos = new byte[all.length / frameBytes][];
        for (int i = 0; i < photos.length; i++) {
            photos[i] = Arrays.copyOfRange(all, i * frameBytes, (i + 1) * frameBytes);
        }
        return photos;
    }

    public static String modelDir(TrackerType trackerType) {
        return switch (trackerType) {
            case REAL_FACE_MEDIAPIPE, REAL_HAND_MEDIAPIPE -> MODELS.resolve("mediapipe").toAbsolutePath().toString();
            case REAL_FACE_ERT -> MODELS.resolve("dlib").toAbsolutePath().toString();
            case REAL_SLAM_ORB -> MODELS.resolve("orbslam2").toAbsolutePath().toString();
            default -> null;
        };
    }

    public static int landmarkFloats(TrackerType trackerType) {
        return switch (trackerType) {
            case REAL_FACE_MEDIAPIPE -> 478 * 3 + 52;
            case REAL_FACE_ERT -> 68 * 3;
            case REAL_HAND_MEDIAPIPE, REAL_HAND_ERT -> 21 * 3;
            default -> throw new IllegalArgumentException("нет landmarks у " + trackerType);
        };
    }

    public static void printStats(NativeBridge bridge) {
        long[] s = bridge.stats();
        long frames = Math.max(1, s[TrackerStat.REAL_STAT_FRAMES.ordinal()]);
        System.out.printf("# real_stats %s: frames=%d found=%.1f%% detect=%.1f%% track=%.1f%% features/frame=%.1f imu=%d%n",
                bridge.name(), frames,
                100.0 * s[TrackerStat.REAL_STAT_FOUND.ordinal()] / frames,
                100.0 * s[TrackerStat.REAL_STAT_DETECT.ordinal()] / frames,
                100.0 * s[TrackerStat.REAL_STAT_TRACK.ordinal()] / frames,
                (double) s[TrackerStat.REAL_STAT_FEATURES.ordinal()] / frames,
                s[TrackerStat.REAL_STAT_IMU.ordinal()]);
    }

    public static byte[] getReference() {
        byte[] ref = read("cover.gray");
        if (ref.length % REFERENCE_WIDTH != 0) {
            throw new IllegalStateException("cover.gray: " + ref.length + " байт — не кратно ширине " + REFERENCE_WIDTH);
        }
        return ref;
    }

    private static byte[] read(String file) {
        try {
            return Files.readAllBytes(DIR.resolve(file));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public record Imu(long[] ts, float[] samples) {
        public int size() {
            return ts.length;
        }

        public long span() {
            return ts[ts.length - 1] - ts[0] + (ts[1] - ts[0]);
        }
    }

    public static Imu getImu() {
        List<String> lines = new String(read("imu.csv"), StandardCharsets.US_ASCII).lines()
                .filter(l -> !l.isBlank() && !l.startsWith("#"))
                .toList();
        long[] ts = new long[lines.size()];
        float[] samples = new float[lines.size() * 6];
        for (int i = 0; i < ts.length; i++) {
            String[] p = lines.get(i).split(",");
            ts[i] = Long.parseLong(p[0].trim());
            for (int k = 0; k < 3; k++) {
                samples[6 * i + k] = Float.parseFloat(p[4 + k].trim());
                samples[6 * i + 3 + k] = Float.parseFloat(p[1 + k].trim());
            }
        }
        return new Imu(ts, samples);
    }

    public record Gnss(double[] lat, double[] lon, double[] alt) {
        public int size() {
            return lat.length;
        }
    }

    public static Gnss getGnss(int n) {
        Random rnd = new Random(42);
        double[] lat = new double[n], lon = new double[n], alt = new double[n];
        for (int i = 0; i < n; i++) {
            lat[i] = 47.3763 + rnd.nextGaussian() * 2.7e-5;
            lon[i] = 8.5476 + rnd.nextGaussian() * 4.0e-5;
            alt[i] = 500 + rnd.nextGaussian() * 5;
        }
        return new Gnss(lat, lon, alt);
    }
}

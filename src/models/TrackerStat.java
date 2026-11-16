package models;

public enum TrackerStat {
    REAL_STAT_FRAMES(0),
    REAL_STAT_FOUND(1),
    REAL_STAT_DETECT(2),
    REAL_STAT_TRACK(3),
    REAL_STAT_FEATURES(4),
    REAL_STAT_IMU(5),
    REAL_STAT_GPS(6),
    REAL_STAT_COUNT(7);

    int code;

    TrackerStat(int code) {
        this.code = code;
    }
}

package models;

public enum TrackerType {
    REAL_ARUCO(1, 1),
    REAL_IMAGE_TARGET(3, 1),

    REAL_FACE_MEDIAPIPE(10, 3),
    REAL_HAND_MEDIAPIPE(11, 3),
    REAL_FACE_ERT(12,3),
    REAL_HAND_ERT(13, 3),

    REAL_SLAM_KLT(20, 1),
    REAL_SLAM_ORB(21, 1),

    REAL_FILTER_ESKF(30, 0),
    REAL_FILTER_PREINT(31, 0);

    int code;
    int colors;

    TrackerType(int code, int colors) {
        this.code = code;
        this.colors = colors;
    }

    public int code() {
        return code;
    }

    public int colors() {
        return colors;
    }
}

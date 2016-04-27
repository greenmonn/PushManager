package kr.ac.kaist.nmsl.pushmanager.ble;

/**
 * Created by wns349 on 4/27/2016.
 */
public enum BLEStatus {
    Unknown(-1),
    Idle(0),
    PhoneUse(1),;
    final long code;

    BLEStatus(int code) {
        this.code = code;
    }

    BLEStatus(long code) {
        this.code = code;
    }

    public long getCode() {
        return this.code;
    }

    public static BLEStatus parse(long code) {
        for (BLEStatus s : BLEStatus.values()) {
            if (s.getCode() == code) {
                return s;
            }
        }

        return BLEStatus.Unknown;
    }
}

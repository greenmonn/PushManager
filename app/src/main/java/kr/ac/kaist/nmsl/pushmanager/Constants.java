package kr.ac.kaist.nmsl.pushmanager;

import java.util.Arrays;
import java.util.List;

/**
 * Created by wns349 on 2015-11-30.
 */
public class Constants {
    public static final String DEBUG_TAG = "PM_DEBUG";
    public static final String TAG = "PM";

    public static final String DIR_NAME = "NMSL_PUSH_MANAGER";
    public static final String LABALED_DATA_FILE_NAME = "breakpoint_labeled.arff";
    public static String LOG_NAME = "";

    public static final long ACTIVITY_REQUEST_DURATION = 0;

    public static class BeaconConst {
        //public static final String LAYOUT_STRING = "m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25";
        //public static final String LAYOUT_STRING = "m:2-3=0118,i:4-7,p:8-8,d:9-16,d:18-25";

        public static final String LAYOUT_STRING = "m:2-3=0118,i:4-7,p:8-8,d:9-12,d:13-16,d:17-20,d:21-24";
        //public static final String LAYOUT_STRING = "x,s:0-1=feaa,m:2-2=20,d:3-3,d:4-5,d:6-7,d:8-11,d:12-15";
        //public static final String LAYOUT_STRING = "x,s:0-1=feaa,m:2-2=20,d:3-3,d:4-5,d:6-7,d:8-11,d:12-15,i:16-31";

        public static final int MANUFACTURER = 0x0118;
        public static final int TX_POWER = -59;
        public static final List<Long> EXTRA_DATA_FIELDS = Arrays
                .asList(new Long[] { 0L });
        public static final String UNIQUE_REGION_ID = "myRangingUniqueId";
    }

    public static final int REQUEST_ENABLE_BT = 1842;

    public static final String INTENT_FILTER_NOTIFICATION = "kr.ac.kaist.nmsl.pushmanager.notification";
    public static final String INTENT_FILTER_ACTIVITY = "kr.ac.kaist.nmsl.pushmanager.action.activity";
    public static final String INTENT_FILTER_BLE = "kr.ac.kaist.nmsl.pushmanager.action.ble";
    public static final String INTENT_FILTER_BREAKPOINT = "kr.ac.kaist.nmsl.pushmanager.action.breakpoint";
    public static final String INTENT_FILTER_USING_SMARTPHONE = "kr.ac.kaist.nmsl.pushmanager.action.usingsmartphone";
    public static final String INTENT_FILTER_AUDIO = "kr.ac.kaist.nmsl.pushmanager.action.audio";
    public static final String INTENT_FILTER_MAINSERVICE = "kr.ac.kaist.nmsl.pushmanager";

    public static final String BLUETOOTH_NOT_FOUND = "bt_not_found";
    public static final String BLUETOOTH_DISABLED = "bt_disabled";
    public static final String BLUETOOTH_LE_BEACON = "ble_beacon";

    public static final double AUDIO_SILENCE_SPL = -75.0;
    public static final double AUDIO_SAMPLING_DURATION = 0.7;
    public static final int AUDIO_SAMPLING_PERIOD = 1;

    public static class CONTEXT_ATTRIBUTE_TYPES {
        public static final int WITH_OTHERS = 0;
        public static final int IS_TALKING = 1;
        public static final int ACTIVITY = 2;
        public static final int OTHER_USING_SMARTPHONE = 3;
    }

    public static final int SILENCE_DURATION = 7 * 1000;
    public static final int IS_TALKING_SAMPLE_COUNT = 3;
    public static final int BLE_BEACON_NOT_DETECTED_THRESHOLD = 3;

    public static final String NOTIFICATION_BLACK_LIST_REGEX = "com\\.android\\.vending" +
            "|com\\.android\\.providers\\.downloads" +
            "|com\\.google\\.android\\.gms" +
            "|com\\.android\\.systemui" +
            "|com\\.android\\.chrome" +
            "|\\.launcher" +
            "|com\\.qihoo\\.security" +
            "|com\\.estsoft\\.alyac" +
            "|com\\.ahnlab\\.v3mobile";

    public static final long SMARTPHONE_NOT_USING_INTERVAL = 5000L;

    public static final long SMARTPHONE_USE_EXPIRE = 50000L;
}

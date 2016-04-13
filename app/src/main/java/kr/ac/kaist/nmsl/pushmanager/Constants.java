package kr.ac.kaist.nmsl.pushmanager;

/**
 * Created by wns349 on 2015-11-30.
 */
public class Constants {
    public static final String DEBUG_TAG = "PM_DEBUG";
    public static final String TAG = "PM";

    public static final String DIR_NAME = "CS472_PUSH_MANAGER";
    public static String LOG_NAME = "";
    public static boolean LOG_ENABLED = false;

    public static final long WARNING_DELAY_INTERVAL = 5000L; // In milliseconds.
    public static final long WARNING_POPUP_SHOW_DELAY = 1000L; // In milliseconds.

    public static final String[] WHITELIST_APPS = {
            //"kr.ac.kaist.nmsl.pushmanager", "com.android.systemui"
    };

    public static final String PARSE_APPLICATION_ID = "LF8jevBSVvJpDgrwkZP7RP7Yhpxq6MVIEldFfSLq";
    public static final String PARSE_CLIENT_KEY= "2TIot30hqRKDNNTjbBQ8u0ubetp11B6Lxxfd8TwU";

    public static final long ACTIVITY_REQUEST_DURATION = 2000;
}

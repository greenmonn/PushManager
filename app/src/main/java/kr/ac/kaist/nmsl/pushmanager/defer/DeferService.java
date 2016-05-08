package kr.ac.kaist.nmsl.pushmanager.defer;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.gms.location.DetectedActivity;

import org.altbeacon.beacon.Beacon;

import java.util.*;

import kr.ac.kaist.nmsl.pushmanager.Constants;
import kr.ac.kaist.nmsl.pushmanager.MainActivity;
import kr.ac.kaist.nmsl.pushmanager.activity.PhoneState;
import kr.ac.kaist.nmsl.pushmanager.audio.AudioResult;
import kr.ac.kaist.nmsl.pushmanager.push.LocalPushThread;
import kr.ac.kaist.nmsl.pushmanager.socialcontext.SocialContext;
import kr.ac.kaist.nmsl.pushmanager.util.BLEUtil;
import kr.ac.kaist.nmsl.pushmanager.util.Util;

public class DeferService extends Service {
    private static final int VIBRATION_DURATION = 500;

    private Vibrator mVibrator;
    private Timer mTimer;
    private int mNotificationCount;
    private SocialContext socialContext;

    private LocalPushThread mLocalPushThread = null;

    private DeferServiceReceiver mDeferServiceReceiver = null;

    public DeferService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            socialContext = new SocialContext(getAssets().open(Constants.LABALED_DATA_FILE_NAME));
        } catch (Exception e) {
            socialContext = null;
        }

        mNotificationCount = 0;

        mDeferServiceReceiver = new DeferServiceReceiver();

        // Start push thread
        if (mLocalPushThread != null) {
            mLocalPushThread.terminate();
        }
        mLocalPushThread = new LocalPushThread(this);
        mLocalPushThread.start();

        mVibrator = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);

        long getContextDuration = 5000L;

        mTimer = new Timer();
        mTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {


                HashMap<Integer, SocialContext.Attribute> socialContextAttributes = socialContext.getCurrentContext();
                boolean isBreakpoint = socialContext.getIsBreakpoint(socialContextAttributes);

                if (isBreakpoint && mNotificationCount > 0) {
                    mNotificationCount = 0;
                    mVibrator.vibrate(VIBRATION_DURATION);
                    Log.i(Constants.DEBUG_TAG, "Vibrated");
                }

                Intent localIntent = new Intent(Constants.INTENT_FILTER_BREAKPOINT);
                localIntent.putExtra("breakpoint", isBreakpoint);
                localIntent.putExtra("activity", socialContextAttributes.containsKey(Constants.CONTEXT_ATTRIBUTE_TYPES.ACTIVITY) ? socialContextAttributes.get(Constants.CONTEXT_ATTRIBUTE_TYPES.ACTIVITY).stringValue : "");
                localIntent.putExtra("is_talking", socialContextAttributes.containsKey(Constants.CONTEXT_ATTRIBUTE_TYPES.IS_TALKING) ? socialContextAttributes.get(Constants.CONTEXT_ATTRIBUTE_TYPES.IS_TALKING).stringValue : "");
                localIntent.putExtra("is_using", socialContextAttributes.containsKey(Constants.CONTEXT_ATTRIBUTE_TYPES.OTHER_USING_SMARTPHONE) ? socialContextAttributes.get(Constants.CONTEXT_ATTRIBUTE_TYPES.OTHER_USING_SMARTPHONE).stringValue : "");
                localIntent.putExtra("with_others", socialContextAttributes.containsKey(Constants.CONTEXT_ATTRIBUTE_TYPES.WITH_OTHERS) ? socialContextAttributes.get(Constants.CONTEXT_ATTRIBUTE_TYPES.WITH_OTHERS).doubleValue : -9999);
                sendBroadcast(localIntent);

                for (Integer key : socialContextAttributes.keySet()) {
                    logSocialContextAttribute(socialContextAttributes.get(key));
                }

                String msg = "";
                msg += "isBreakpoint: " + isBreakpoint;
                msg += ", activity: " + (socialContextAttributes.containsKey(Constants.CONTEXT_ATTRIBUTE_TYPES.ACTIVITY) ? socialContextAttributes.get(Constants.CONTEXT_ATTRIBUTE_TYPES.ACTIVITY).stringValue : "");
                msg += ", is_talking: " + (socialContextAttributes.containsKey(Constants.CONTEXT_ATTRIBUTE_TYPES.IS_TALKING) ? socialContextAttributes.get(Constants.CONTEXT_ATTRIBUTE_TYPES.IS_TALKING).stringValue : "");
                msg += ", is_using: " + (socialContextAttributes.containsKey(Constants.CONTEXT_ATTRIBUTE_TYPES.OTHER_USING_SMARTPHONE) ? socialContextAttributes.get(Constants.CONTEXT_ATTRIBUTE_TYPES.OTHER_USING_SMARTPHONE).stringValue : "");
                msg += ", with_others: " + (socialContextAttributes.containsKey(Constants.CONTEXT_ATTRIBUTE_TYPES.WITH_OTHERS) ? socialContextAttributes.get(Constants.CONTEXT_ATTRIBUTE_TYPES.WITH_OTHERS).doubleValue : -9999);

                if (Constants.LOG_ENABLED) {
                    Util.writeLogToFile(getApplicationContext(), Constants.LOG_NAME, "BREAKPONT", msg);
                }

            }


        }, getContextDuration, getContextDuration);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.INTENT_FILTER_ACTIVITY);
        filter.addAction(Constants.INTENT_FILTER_NOTIFICATION);
        filter.addAction(Constants.INTENT_FILTER_BLE);
        filter.addAction(Constants.INTENT_FILTER_USING_SMARTPHONE);
        filter.addAction(Constants.INTENT_FILTER_AUDIO);
        registerReceiver(mDeferServiceReceiver, filter);

        Log.i(Constants.DEBUG_TAG, "DeferService started.");

        return START_STICKY;
    }

    private void logSocialContextAttribute(SocialContext.Attribute attribute) {
        String msg = "[Social Context] ";
        switch (attribute.type) {
            case Constants.CONTEXT_ATTRIBUTE_TYPES.ACTIVITY:
                msg += "ACTIVITY: " + attribute.stringValue;
                break;
            case Constants.CONTEXT_ATTRIBUTE_TYPES.OTHER_USING_SMARTPHONE:
                msg += "OTHER_USING: " + attribute.stringValue;
                break;
            case Constants.CONTEXT_ATTRIBUTE_TYPES.WITH_OTHERS:
                msg += "WITH_OTHERS: " + attribute.doubleValue;
                break;
            case Constants.CONTEXT_ATTRIBUTE_TYPES.IS_TALKING:
                msg += "IS_TALKING: " + attribute.stringValue;
                break;
            default:
                msg += "UNKNOWN CONTEXT";
                break;
        }

        Log.d(Constants.DEBUG_TAG, msg);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mDeferServiceReceiver);

        if (mLocalPushThread != null) {
            mLocalPushThread.terminate();
        }

        mTimer.cancel();
        Log.i(Constants.DEBUG_TAG, "DeferService destroyed.");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private PhoneState.State getActivityName(int activityType) {
        return PhoneState.getStateByDetectedActivity(activityType);
    }

    //TODO: management policy to be added!!
    class DeferServiceReceiver extends BroadcastReceiver {
        public DeferServiceReceiver() {
            Log.d(Constants.TAG, "Starting DeferServiceReceiver broadcast receiver");
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.INTENT_FILTER_NOTIFICATION)) {
                if (intent.getStringExtra("notification_action").equals("posted")) {
                    Log.i(Constants.DEBUG_TAG, "Notification incremented");
                    mNotificationCount++;
                    if (mLocalPushThread != null) {
                        String pack = intent.getStringExtra("notification_package");

                        Log.d(Constants.TAG, "A push notification received from: " + pack);
                        mLocalPushThread.updateLastPushReceivedAtToNow();
                    }

                } else if (intent.getStringExtra("notification_action").equals("removed")) {
                    Log.i(Constants.DEBUG_TAG, "Notification decremented");
                    mNotificationCount--;
                }
            }

            if (intent.getAction().equals(Constants.INTENT_FILTER_ACTIVITY)) {
                int prob = intent.getIntExtra("activity_probability", 0);
                PhoneState.State state = getActivityName(intent.getIntExtra("activity_type", -1));
                PhoneState.getInstance().updateMyState(state);
                Log.d(Constants.DEBUG_TAG, "[Detected Activity] " + state.name() + ": " + prob);
                //Toast.makeText(context, "[Detected Activity] " + state.name() + ": " + prob, Toast.LENGTH_SHORT).show();

                if (intent.hasExtra("detected_activities")) {
                    ArrayList<DetectedActivity> detectedActivities = intent.getParcelableArrayListExtra("detected_activities");
                    socialContext.addDetectedAcitivity(detectedActivities);
                }
            }

            if (intent.getAction().equals(Constants.INTENT_FILTER_BLE)) {
                if (intent.hasExtra(Constants.BLUETOOTH_LE_BEACON)) {
                    ArrayList<Beacon> detectedBeacons =
                            intent.getParcelableArrayListExtra(Constants.BLUETOOTH_LE_BEACON);

                    socialContext.addBeacon(detectedBeacons);

                    Log.d(Constants.DEBUG_TAG, "=================== detected beacons ==================");
                    for (Beacon detectedBeacon : detectedBeacons) {
                        Log.d(Constants.DEBUG_TAG, detectedBeacon.getBluetoothAddress() + ", " + detectedBeacon.getDataFields().size() + ", " + detectedBeacon.getExtraDataFields().size() + ", " + String.valueOf(detectedBeacon.getRssi()));
                        Log.d(Constants.DEBUG_TAG, "talking detected from BLE: " + PhoneState.getInstance().getIsTalkingFromBeacon(detectedBeacon));

                        if (detectedBeacon.getDataFields().size() > 0) {

                            long blePhoneNumber = BLEUtil.getBLEPhoneNumber(detectedBeacon);

                            //Toast.makeText(context, "[BLE] " + blePhoneNumber + " / His state: "+bleState.name(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }

            if (intent.getAction().equals(Constants.INTENT_FILTER_USING_SMARTPHONE)) {
                boolean isUsing = intent.getBooleanExtra("is_using", false);
                //Log.d(Constants.DEBUG_TAG, "isUsing: " + isUsing);
                PhoneState.getInstance().updateIsUsingSmartphone(isUsing);
            }

            if (intent.getAction().equals(Constants.INTENT_FILTER_AUDIO)) {
                boolean isTalking = intent.getBooleanExtra("is_talking", false);

                PhoneState.getInstance().updateIsTalking(isTalking);
                socialContext.addAudioResult(new AudioResult(isTalking));
            }
        }
    }
}
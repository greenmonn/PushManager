package kr.ac.kaist.nmsl.pushmanager.defer;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.DetectedActivity;

import org.altbeacon.beacon.Beacon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import kr.ac.kaist.nmsl.pushmanager.Constants;
import kr.ac.kaist.nmsl.pushmanager.activity.PhoneState;
import kr.ac.kaist.nmsl.pushmanager.audio.AudioResult;
import kr.ac.kaist.nmsl.pushmanager.socialcontext.SocialContext;
import kr.ac.kaist.nmsl.pushmanager.util.BLEUtil;
import kr.ac.kaist.nmsl.pushmanager.util.Util;

public class DeferService extends Service {
    private static final int VIBRATION_DURATION = 500;

    private Vibrator mVibrator;
    private Timer mTimer;
    private int mNotificationCount;
    private HashMap<String, Integer> mNotificationCountHash;
    private SocialContext socialContext;

    private DeferServiceReceiver mDeferServiceReceiver = null;

    public DeferService() {
    }

    private void notifyQueuedNotifications(String cause) {
        if (mNotificationCount > 0 && mNotificationCountHash.keySet().size() > 0) {
            mVibrator.vibrate(VIBRATION_DURATION);
            Log.i(Constants.DEBUG_TAG, "Vibrated");
            Util.writeLogToFile(getApplicationContext(), Constants.LOG_NAME, "BREAKPOINT", "Vibrated. Cause: " + cause + " / Queued Notification Count: " + mNotificationCount + ", application count:" + mNotificationCountHash.keySet().size());

            mNotificationCount = 0;
            mNotificationCountHash.clear();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            socialContext = SocialContext.getInstance();
            socialContext.initialize(getAssets().open(Constants.LABALED_DATA_FILE_NAME));
        } catch (Exception e) {
            socialContext = null;
        }

        mNotificationCount = 0;
        mNotificationCountHash = new HashMap<>();

        mDeferServiceReceiver = new DeferServiceReceiver();

        mVibrator = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);

        long getContextDuration = 2000L;

        mTimer = new Timer();
        mTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                HashMap<Integer, SocialContext.Attribute> socialContextAttributes = socialContext.getCurrentContext();
                boolean isBreakpoint = socialContext.getIsBreakpoint(socialContextAttributes);
                if (isBreakpoint) {
                    notifyQueuedNotifications("BREAKPOINT");
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

                Util.writeLogToFile(getApplicationContext(), Constants.LOG_NAME, "BREAKPOINT", msg);

                if (!socialContextAttributes.containsKey(Constants.CONTEXT_ATTRIBUTE_TYPES.WITH_OTHERS)) {
                    BluetoothAdapter btAdapter = BLEUtil.getBluetoothAdapter();
                    if (!btAdapter.isEnabled()) {
                        btAdapter.enable();
                    }
                }
            }


        }, getContextDuration, getContextDuration);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.INTENT_FILTER_ACTIVITY);
        filter.addAction(Constants.INTENT_FILTER_NOTIFICATION);
        filter.addAction(Constants.INTENT_FILTER_BLE);
        filter.addAction(Constants.INTENT_FILTER_USING_SMARTPHONE);
        filter.addAction(Constants.INTENT_FILTER_AUDIO);
        filter.addAction(Constants.INTENT_FILTER_MAINSERVICE);
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
                    mNotificationCountHash.put(intent.getStringExtra("notification_package"), Integer.valueOf(1));
                    Util.writeLogToFile(getApplicationContext(), Constants.LOG_NAME, "NOTIFICATION", "Notification count incremented to " + mNotificationCount + ", " + mNotificationCountHash.keySet().size() + " application");
                } else if (intent.getStringExtra("notification_action").equals("removed")) {
                    Log.i(Constants.DEBUG_TAG, "Notification decremented");
                    mNotificationCount--;
                    mNotificationCountHash.remove(intent.getStringExtra("notification_package"));
                    Util.writeLogToFile(getApplicationContext(), Constants.LOG_NAME, "NOTIFICATION", "Notification count decremented to " + mNotificationCount + ", " + mNotificationCountHash.keySet().size() + " application");
                }
            }

            if (intent.getAction().equals(Constants.INTENT_FILTER_ACTIVITY)) {
                int prob = intent.getIntExtra("activity_probability", 0);

                if (intent.hasExtra("activity_type")) {
                    PhoneState.State state = getActivityName(intent.getIntExtra("activity_type", -1));
                    PhoneState.getInstance().updateMyState(state);
                    Log.d(Constants.DEBUG_TAG, "[Detected Activity] " + state.name() + ": " + prob);
                    //Toast.makeText(context, "[Detected Activity] " + state.name() + ": " + prob, Toast.LENGTH_SHORT).show();
                } else if (intent.hasExtra("activity_name")) {
                    if (intent.getStringExtra("activity_name").equals("STEP")) {
                        PhoneState.State state = getActivityName(DetectedActivity.WALKING);
                        PhoneState.getInstance().updateMyState(state);
                        Log.d(Constants.DEBUG_TAG, "[Detected Activity from Step Counter] " + state.name() + ": " + prob);
                    } else if (intent.getStringExtra("activity_name").equals("NO_STEP")) {
                        PhoneState.State state = getActivityName(DetectedActivity.STILL);
                        PhoneState.getInstance().updateMyState(state);
                        Log.d(Constants.DEBUG_TAG, "[Detected Activity from Step Counter] " + state.name() + ": " + prob);
                    }
                }

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

                    for (Beacon detectedBeacon : detectedBeacons) {
                        Log.d(Constants.DEBUG_TAG, "detected beacon: " + detectedBeacon.getId1().toString() + ", " + detectedBeacon.getBluetoothAddress() + ", " + detectedBeacon.getDataFields().size() + ", " + detectedBeacon.getExtraDataFields().size() + ", " + String.valueOf(detectedBeacon.getRssi()));
                        Log.d(Constants.DEBUG_TAG, "talking detected from BLE: " + PhoneState.getInstance().getIsTalkingFromBeacon(detectedBeacon));

                        if (detectedBeacon.getDataFields().size() > 0) {
                            if (PhoneState.getInstance().getIsTalkingFromBeacon(detectedBeacon)) {
                                socialContext.addAudioResult(new AudioResult(PhoneState.getInstance().getIsTalkingFromBeacon(detectedBeacon), false));
                            }
                        }
                    }
                }
            }

            if (intent.getAction().equals(Constants.INTENT_FILTER_USING_SMARTPHONE)) {
                boolean isUsing = intent.getBooleanExtra("is_using", false);
                socialContext.setMeUsingSmartphone(isUsing);
                PhoneState.getInstance().updateIsUsingSmartphone(isUsing);
            }

            if (intent.getAction().equals(Constants.INTENT_FILTER_AUDIO)) {
                boolean isTalking = intent.getBooleanExtra("is_talking", false);

                PhoneState.getInstance().updateIsTalking(isTalking);
                socialContext.addAudioResult(new AudioResult(isTalking, true));
            }

            if (intent.getAction().equals(Constants.INTENT_FILTER_MAINSERVICE)) {
                if (intent.getBooleanExtra("sendOutQueuedNotifications", false)) {
                    notifyQueuedNotifications("MODE_CHANGE");
                }
            }
        }
    }
}
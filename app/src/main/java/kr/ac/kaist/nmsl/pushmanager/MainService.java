package kr.ac.kaist.nmsl.pushmanager;

import android.app.PendingIntent;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;

import java.text.SimpleDateFormat;
import java.util.Date;

import kr.ac.kaist.nmsl.pushmanager.activity.ActivityRecognitionIntentService;
import kr.ac.kaist.nmsl.pushmanager.activity.StepCounterService;
import kr.ac.kaist.nmsl.pushmanager.audio.AudioProcessorService;
import kr.ac.kaist.nmsl.pushmanager.ble.BLEService;
import kr.ac.kaist.nmsl.pushmanager.defer.DeferService;
import kr.ac.kaist.nmsl.pushmanager.notification.NotificationService;
import kr.ac.kaist.nmsl.pushmanager.push.LocalPushThread;
import kr.ac.kaist.nmsl.pushmanager.util.Util;

/**
 * Created by wns349 on 5/8/2016.
 */
public class MainService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    public static final String FILE_UTIL_FILE_DATETIME_FORMAT = "yyyyMMdd_HHmmss";
    public static final int TOTAL_NUMBER_OF_TOGGLES = 4;

    // Service toggle related
    private ServiceToggleTimer mServiceToggleTimer = null;
    private long mTotalDuration = 0L;

    // Mute/Unmute related
    private int mOldRingerMode = -1;

    // Google Activity Recognition related
    private GoogleApiClient mGoogleApiClient = null;

    //mLocalPushThread for noIntervention service
    private LocalPushThread mLocalPushThread = null;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Start common services that must be run ALL the time
        Log.d(Constants.TAG, "Starting NotificationService");
        startService(new Intent(this, NotificationService.class));
        //Google API Client
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(ActivityRecognition.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();

        final SimpleDateFormat fileDateFormat = new SimpleDateFormat(FILE_UTIL_FILE_DATETIME_FORMAT);
        Constants.LOG_NAME = "SCAN_PUSH_MANAGER_"+fileDateFormat.format(new Date());

        // Register broadcast receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.INTENT_FILTER_NOTIFICATION);
        registerReceiver(mMainServiceBroadcastReceiver, filter);

        // Start Service Toggle
        mTotalDuration = intent.getLongExtra("duration", 0L) * 1000L;
        int firstPushManagementServiceToExecute = intent.getIntExtra("firstPushManagementServiceToExecute", R.id.radio_btn_no_intervention);

        mServiceToggleTimer = new ServiceToggleTimer(this, 0, firstPushManagementServiceToExecute, mTotalDuration / TOTAL_NUMBER_OF_TOGGLES);
        mServiceToggleTimer.start();

        // Start push thread
        if (mLocalPushThread != null) {
            mLocalPushThread.terminate();
        }
        mLocalPushThread = new LocalPushThread(this);
        mLocalPushThread.start();

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        stopService(new Intent(this, DeferService.class));
        stopService(new Intent(this, BLEService.class));
        stopService(new Intent(this, AudioProcessorService.class));
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            Log.d(Constants.TAG, "google activity request removed");
            ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(mGoogleApiClient, getActivityDetectionPendingIntent());
        }

        // Mark logging
        Util.writeLogToFile(this, Constants.LOG_NAME, "END", "==============All ended===============");

        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }

        // Stop Service Toggle
        if (mServiceToggleTimer != null) {
            mServiceToggleTimer.cancel();
        }

        if (mLocalPushThread != null) {
            mLocalPushThread.terminate();
        }

        // Unregister broadcast receiver
        unregisterReceiver(mMainServiceBroadcastReceiver);

        // Stop common services that must be run ALL the time
        Log.d(Constants.TAG, "Stopping NotificationService");
        stopService(new Intent(this, NotificationService.class));

        // Recover old ringer state
        unmuteDevice();

        // Send broadcast
        Intent serviceDestroyedIntent = new Intent(Constants.INTENT_FILTER_MAINSERVICE);
        serviceDestroyedIntent.putExtra("alive", false);
        LocalBroadcastManager.getInstance(this).sendBroadcast(serviceDestroyedIntent);

        super.onDestroy();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(Constants.TAG, "Google Activity Recognition API connected.");
        if (this.mServiceToggleTimer != null && this.mServiceToggleTimer.getPushManagementMethod() == ServiceState.DeferService) {
            ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mGoogleApiClient, Constants.ACTIVITY_REQUEST_DURATION, getActivityDetectionPendingIntent());
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(Constants.TAG, "onConnectionSuspended called: " + i);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(Constants.TAG, "Google Activity Recognition API connect failed. " + connectionResult.getErrorMessage());
    }

    public void muteDevice() {
        // Get existing state
        AudioManager audioManager = ((AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE));
        if (mOldRingerMode == -1) {
            // update only if not updated before
            mOldRingerMode = audioManager.getRingerMode();
        }
        audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
    }

    public void unmuteDevice() {
        Log.d(Constants.TAG, "Recovering to old ringer mode: " + mOldRingerMode);
        if (mOldRingerMode != -1) {
            ((AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE)).setRingerMode(mOldRingerMode);
            mOldRingerMode = -1;
        }
    }

    public void stopAllServices(boolean stopForGood) {
        // Send out queued push notifications
        Intent intentSendOutQueuedNotifications = new Intent(Constants.INTENT_FILTER_MAINSERVICE);
        intentSendOutQueuedNotifications.putExtra("sendOutQueuedNotifications", true);
        sendBroadcast(intentSendOutQueuedNotifications);

        // Stop services
        stopService(new Intent(this, DeferService.class));
        stopService(new Intent(this, BLEService.class));
        stopService(new Intent(this, AudioProcessorService.class));
        stopService(new Intent(this, StepCounterService.class));
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            Log.d(Constants.TAG, "google activity request removed");
            ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(mGoogleApiClient, getActivityDetectionPendingIntent());
        }

        if (stopForGood) {
            // Destroy this service
            Util.writeLogToFile(this, Constants.LOG_NAME, "END", "==============All ended===============");
            this.stopSelf();
        } else {
            Util.writeLogToFile(this, Constants.LOG_NAME, "SWITCH", "++++++++++++++Service Switch++++++++++++++");
        }
    }

    public void startNoInterventionService() {
        stopAllServices(false);

        Log.d(Constants.TAG, "NoIntervention started");
        Util.writeLogToFile(this, Constants.LOG_NAME, "START", "==============NoIntervention started===============");
    }

    public void startDeferService() {
        stopAllServices(false);

        Log.d(Constants.TAG, "DeferService started");
        Util.writeLogToFile(this, Constants.LOG_NAME, "START", "==============Defer started===============");

        muteDevice();

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mGoogleApiClient, Constants.ACTIVITY_REQUEST_DURATION, getActivityDetectionPendingIntent());
        }
        startService(new Intent(this, AudioProcessorService.class));
        startService(new Intent(this, BLEService.class));
        startService(new Intent(this, DeferService.class));
        startService(new Intent(this, StepCounterService.class));
    }

    public PendingIntent getActivityDetectionPendingIntent() {
        Intent intent = new Intent(this, ActivityRecognitionIntentService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        return pendingIntent;
    }


    public void runNextServiceToggleTimer(int prevToggleCount, int prevPushManagementMethodId, long duration) {
        if (mServiceToggleTimer != null) {
            mServiceToggleTimer.cancel();
        }

        if (prevToggleCount + 1 < TOTAL_NUMBER_OF_TOGGLES) {
            int newToggleCount = prevToggleCount + 1;
            int newPushManagementMethodId = prevPushManagementMethodId == R.id.radio_btn_defer ? R.id.radio_btn_no_intervention : R.id.radio_btn_defer;

            mServiceToggleTimer = new ServiceToggleTimer(this, newToggleCount, newPushManagementMethodId, duration);
            mServiceToggleTimer.start();
        } else {
            // Stop
            stopAllServices(true);
        }
    }


    private void sendError(String message) {
        Intent intent = new Intent(Constants.INTENT_FILTER_MAINSERVICE);
        intent.putExtra("error", message);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        // Stop self
        stopSelf();
    }

    private final BroadcastReceiver mMainServiceBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.INTENT_FILTER_NOTIFICATION)) {
                if (mServiceToggleTimer.getPushManagementMethod() == ServiceState.NoIntervention) {
                    // Need to recover
                    Log.d(Constants.DEBUG_TAG, "Recovering ringer mode to " + mOldRingerMode + " since device is in no intervention.");
                    unmuteDevice();

                    if (intent.getStringExtra("notification_action").equals("posted")) {
                        if (mLocalPushThread != null) {
                            String pack = intent.getStringExtra("notification_package");

                            Log.d(Constants.TAG, "A push notification received from: " + pack);
                            mLocalPushThread.updateLastPushReceivedAtToNow();
                        }
                    }
                }
            }
        }
    };

    private static class ServiceToggleTimer extends CountDownTimer {
        private final MainService mainService;
        private final int toggleCount;
        private final int pushManagementMethodId;
        private final long timeToRun;

        public ServiceToggleTimer(MainService mainService, int toggleCount, int pushManagementMethodId, long timeToRun) {
            super(timeToRun, 1000L);
            this.mainService = mainService;
            this.toggleCount = toggleCount;
            this.pushManagementMethodId = pushManagementMethodId;
            this.timeToRun = timeToRun;

            switch (pushManagementMethodId) {
                case R.id.radio_btn_no_intervention:
                    Log.d(Constants.TAG, "Starting no intervention service " + toggleCount);
                    mainService.startNoInterventionService();
                    break;
                case R.id.radio_btn_defer:
                    Log.d(Constants.TAG, "Starting defer service " + toggleCount);
                    mainService.startDeferService();
                    break;
            }
        }

        public ServiceState getPushManagementMethod() {
            switch (this.pushManagementMethodId) {
                case R.id.radio_btn_no_intervention:
                    return ServiceState.NoIntervention;

                case R.id.radio_btn_defer:
                    return ServiceState.DeferService;
            }

            // Should not reach here!
            return null;
        }

        @Override
        public void onFinish() {
            Log.d(Constants.TAG, "onFinish called from " + toggleCount + " / " + pushManagementMethodId);

            this.mainService.runNextServiceToggleTimer(toggleCount, pushManagementMethodId, timeToRun);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            Intent intent = new Intent(Constants.INTENT_FILTER_MAINSERVICE);
            intent.putExtra("remainingTime", millisUntilFinished);
            intent.putExtra("toggleCount", toggleCount);
            intent.putExtra("totalTime", timeToRun * TOTAL_NUMBER_OF_TOGGLES);
            intent.putExtra("pushManagementMethodId", pushManagementMethodId);

            LocalBroadcastManager.getInstance(mainService).sendBroadcast(intent);
        }
    }
}

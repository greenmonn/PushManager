package kr.ac.kaist.nmsl.pushmanager;

import android.app.Service;
import android.content.Intent;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;

import kr.ac.kaist.nmsl.pushmanager.log.LogService;
import kr.ac.kaist.nmsl.pushmanager.notification.NotificationService;

/**
 * Created by wns349 on 5/8/2016.
 */
public class MainService extends Service {
    public static final String FILE_UTIL_FILE_DATETIME_FORMAT = "yyyyMMdd_HHmmss";
    public static final int TOTAL_NUMBER_OF_TOGGLES = 4;

    private ServiceState mCurrentServiceState = ServiceState.NoService;

    // Service toggle related
    private ServiceToggleTimer mServiceToggleTimer = null;
    private long mTotalDuration = 0L;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Start common services that must be run ALL the time
        Log.d(Constants.TAG, "Starting LogService");
        startService(new Intent(this, LogService.class));
        Log.d(Constants.TAG, "Starting NotificationService");
        startService(new Intent(this, NotificationService.class));


        // Start Service Toggle
        mTotalDuration = intent.getLongExtra("duration", 0L) * 1000L;
        int firstPushManagementServiceToExecute = intent.getIntExtra("firstPushManagementServiceToExecute", R.id.radio_btn_no_intervention);

        mServiceToggleTimer = new ServiceToggleTimer(this, 0, firstPushManagementServiceToExecute, mTotalDuration / TOTAL_NUMBER_OF_TOGGLES);
        mServiceToggleTimer.start();

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        // Stop Service Toggle
        if (mServiceToggleTimer != null) {
            mServiceToggleTimer.cancel();
        }

        // Stop common services that must be run ALL the time
        Log.d(Constants.TAG, "Stopping NotificationService");
        stopService(new Intent(this, NotificationService.class));
        Log.d(Constants.TAG, "Stopping LogService");
        stopService(new Intent(this, LogService.class));

        // Send broadcast
        Intent serviceDestroyedIntent = new Intent(Constants.INTENT_FILTER_MAINSERVICE);
        serviceDestroyedIntent.putExtra("alive", false);
        LocalBroadcastManager.getInstance(this).sendBroadcast(serviceDestroyedIntent);

        super.onDestroy();
    }

    public void stopAllServices(boolean stopForGood) {
        // TODO: Stop services

        if (stopForGood) {
            // Destroy this service
            this.stopSelf();
        }
    }

    public void startNoInterventionService() {

    }

    public void startDeferService() {

    }

    public void runNextServiceToggleTimer(int prevToggleCount, int prevPushManagementMethodId, long duration) {
        if (mServiceToggleTimer != null) {
            mServiceToggleTimer.cancel();
        }

        if (prevToggleCount < TOTAL_NUMBER_OF_TOGGLES) {
            int newToggleCount = prevToggleCount + 1;
            int newPushManagementMethodId = prevPushManagementMethodId == R.id.radio_btn_defer ? R.id.radio_btn_no_intervention : R.id.radio_btn_defer;

            mServiceToggleTimer = new ServiceToggleTimer(this, newToggleCount, newPushManagementMethodId, duration);
            mServiceToggleTimer.start();
        } else {
            // Stop
            stopAllServices(true);
        }
    }

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

            final SimpleDateFormat fileDateFormat = new SimpleDateFormat(FILE_UTIL_FILE_DATETIME_FORMAT);
            switch (pushManagementMethodId) {
                case R.id.radio_btn_no_intervention:
                    Constants.LOG_NAME = fileDateFormat.format(new Date()) + "_" + ServiceState.NoIntervention.toString();
                    Log.d(Constants.TAG, "Starting no intervention service " + toggleCount);
                    mainService.startNoInterventionService();
                    break;
                case R.id.radio_btn_defer:
                    Constants.LOG_NAME = fileDateFormat.format(new Date()) + "_" + ServiceState.DeferService.toString();
                    Log.d(Constants.TAG, "Starting defer service " + toggleCount);
                    mainService.startDeferService();
                    break;
            }
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

package kr.ac.kaist.nmsl.pushmanager.defer;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;

import java.util.*;

import kr.ac.kaist.nmsl.pushmanager.Constants;

public class DeferService extends Service {
    public static final int DEFER_DURATION = 10*1000;
    public static final int VIBRATION_DURATION = 500;

    private AudioManager mAudioManager;
    private Vibrator mVibrator;
    private Timer mTimer;
    private int mNotificationCount;

    public DeferService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mNotificationCount = 0;

        mAudioManager = (AudioManager)getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);

        mVibrator = (Vibrator)getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);

        mTimer = new Timer();
        mTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (mNotificationCount > 0) {
                    mNotificationCount = 0;
                    mVibrator.vibrate(VIBRATION_DURATION);
                    Log.i(Constants.DEBUG_TAG, "Vibrated");
                }
            }
        }, DEFER_DURATION, DEFER_DURATION);

        Log.i(Constants.DEBUG_TAG, "DeferService started.");

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        mAudioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
        mTimer.cancel();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    class DeferReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getStringExtra("command").equals("posted")) {
                Log.i(Constants.DEBUG_TAG, "Notification incremented");
                mNotificationCount++;
            } else if (intent.getStringExtra("command").equals("removed")) {
                Log.i(Constants.DEBUG_TAG, "Notification decremented");
                mNotificationCount--;
            }
        }
    }
}

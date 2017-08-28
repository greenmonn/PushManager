package kr.ac.kaist.nmsl.pushmanager.push;

import android.animation.ObjectAnimator;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import kr.ac.kaist.nmsl.pushmanager.Constants;
import kr.ac.kaist.nmsl.pushmanager.MainActivity;
import kr.ac.kaist.nmsl.pushmanager.R;
import kr.ac.kaist.nmsl.pushmanager.util.Util;

/**
 * Created by wns349 on 5/3/2016.
 */
public class LocalPushThread extends Thread {
    private static final long MILLISECONDS_WITHOUT_PUSH = 3 * 60 * 1000L; // 3 minutes

    private boolean isRunning = false;
    private long lastPushReceivedAt = 0L;
    private long maxMillisecondsWithoutPush = MILLISECONDS_WITHOUT_PUSH;
    private long varianceWithoutPush = 30 * 1000L;  // 30 seconds

    private Object lock = new Object();

    private final Context context;

    public LocalPushThread(Context context) {
        this.context = context;
    }

    private long randomMilliseconds() {
        long min = MILLISECONDS_WITHOUT_PUSH - varianceWithoutPush;
        long max = MILLISECONDS_WITHOUT_PUSH + varianceWithoutPush;

        long range = max - min;

        return (long) (Math.random() * range + min);
    }

    public void updateLastPushReceivedAtToNow() {
        synchronized (lock) {
            lastPushReceivedAt = System.currentTimeMillis();

            // Update new max milliseconds without push limit
            maxMillisecondsWithoutPush = randomMilliseconds();

            //Log.d(Constants.DEBUG_TAG, "Your new waiting time is: " + maxMillisecondsWithoutPush);
            //Util.writeLogToFile(this.context, Constants.LOG_NAME, "LOCAL_PUSH", "New waiting time: " + maxMillisecondsWithoutPush + " ms");
        }
    }

    private boolean isTimeToSendPush() {
        return (System.currentTimeMillis() - lastPushReceivedAt) >= maxMillisecondsWithoutPush;
    }

    private void sendLocalPush() {
        Log.d(Constants.TAG, "Sending local push ");

        Intent intent = new Intent(this.context, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this.context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder b = new NotificationCompat.Builder(this.context);
        b.setAutoCancel(false)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setTicker("KAIST CS")
                .setContentTitle("KAIST Experiment App Notification")
                .setContentText("You can remove it if you want.")
                .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE)
                .setContentIntent(contentIntent)
                .setContentInfo("");
        NotificationManager notificationManager = (NotificationManager) this.context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, b.build());

        Log.d(Constants.TAG, "Local push sent");
        Util.writeLogToFile(this.context, Constants.LOG_NAME, "LOCAL_PUSH", "Local push sent. ");

        updateLastPushReceivedAtToNow();
    }

    @Override
    public void run() {
        this.isRunning = true;
        updateLastPushReceivedAtToNow();

        while (isRunning) {
            try {
                if (isTimeToSendPush()) {
                    sendLocalPush();
                }
            } finally {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Log.e(Constants.TAG, e.getMessage());
                }
            }
        }
    }

    public void terminate() {
        if (this.isRunning) {
            this.isRunning = false;
        }
    }
}

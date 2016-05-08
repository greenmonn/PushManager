package kr.ac.kaist.nmsl.pushmanager.notification;

import android.content.Intent;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import kr.ac.kaist.nmsl.pushmanager.Constants;
import kr.ac.kaist.nmsl.pushmanager.util.Util;

public class NotificationService extends NotificationListenerService {
    public NotificationService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Log.i(Constants.DEBUG_TAG, "Notification received: " + sbn.getPackageName());
        if (Constants.LOG_ENABLED) {
            Util.writeLogToFile(getApplicationContext(), Constants.LOG_NAME, "NOTIFICATION", "received, " + sbn.getPackageName());
        }

        Intent i = new  Intent(Constants.INTENT_FILTER_NOTIFICATION);
        i.putExtra("notification_action", "posted");
        i.putExtra("notification_package", sbn.getPackageName());
        sendBroadcast(i);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.i(Constants.DEBUG_TAG, "Notification removed: " + sbn.getPackageName());
        if (Constants.LOG_ENABLED) {
            Util.writeLogToFile(getApplicationContext(), Constants.LOG_NAME, "NOTIFICATION", "removed, " + sbn.getPackageName() );
        }

        Intent i = new  Intent(Constants.INTENT_FILTER_NOTIFICATION);
        i.putExtra("notification_action","removed");
        i.putExtra("notification_package", sbn.getPackageName());
        sendBroadcast(i);
    }
}

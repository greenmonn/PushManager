package kr.ac.kaist.nmsl.pushmanager.notification;

import android.content.Intent;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import kr.ac.kaist.nmsl.pushmanager.Constants;

public class NotificationService extends NotificationListenerService {
    private static final String INTENT_FILTER = "kr.ac.kaist.nmsl.pushmanager.action.notification";

    public NotificationService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String pack = sbn.getPackageName();

        Log.i(Constants.DEBUG_TAG, "Notification received: " + pack);

        Intent i = new  Intent(INTENT_FILTER);
        i.putExtra("notification_action", "posted");
        i.putExtra("notification_package", pack);
        sendBroadcast(i);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.i(Constants.DEBUG_TAG, "Notification Removed");

        Intent i = new  Intent(INTENT_FILTER);
        i.putExtra("notification_action","removed");
        i.putExtra("notification_package", sbn.getPackageName());
        sendBroadcast(i);
    }
}

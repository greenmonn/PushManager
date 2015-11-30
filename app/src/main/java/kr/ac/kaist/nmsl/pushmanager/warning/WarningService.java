package kr.ac.kaist.nmsl.pushmanager.warning;


import android.content.Context;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import kr.ac.kaist.nmsl.pushmanager.Constants;

public class WarningService extends NotificationListenerService {
    private Context context;

    @Override
    public void onCreate(){
        super.onCreate();
        this.context = getApplicationContext();

        Log.d(Constants.DEBUG_TAG, "WarningService Created");
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String pack = sbn.getPackageName();

        Log.i(Constants.DEBUG_TAG, "Notification received: "+pack);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.i(Constants.DEBUG_TAG, "Notification Removed");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d(Constants.DEBUG_TAG, "WarningService Destroyed");
    }
}
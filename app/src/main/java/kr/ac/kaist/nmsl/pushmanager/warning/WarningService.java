package kr.ac.kaist.nmsl.pushmanager.warning;


import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import kr.ac.kaist.nmsl.pushmanager.Constants;

public class WarningService extends NotificationListenerService {
    private Context context;

    private WarningLayout warningLayout;

    private WindowManager.LayoutParams layoutParams = null;
    private WindowManager windowManager = null;

    @Override
    public void onCreate(){
        super.onCreate();
        this.context = getApplicationContext();

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);

        warningLayout = new WarningLayout(getApplicationContext(), null);

        Log.d(Constants.DEBUG_TAG, "WarningService Created");
    }

    public void showWarningLayout(){
        windowManager.addView(warningLayout, layoutParams);

        Log.d(Constants.DEBUG_TAG, "Show Warning Layout called");
    }

    public void hideWarningLayout(){
        windowManager.removeView(warningLayout);

        Log.d(Constants.DEBUG_TAG, "Hide Warning Layout called");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String pack = sbn.getPackageName();

        Log.i(Constants.DEBUG_TAG, "Notification received: "+pack);

        showWarningLayout();
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
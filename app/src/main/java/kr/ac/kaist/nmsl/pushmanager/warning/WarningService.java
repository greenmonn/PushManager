package kr.ac.kaist.nmsl.pushmanager.warning;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
    private static final String INTENT_FILTER = "kr.ac.kaist.nmsl.pushmanager";

    private Context context;

    private WarningLayout warningLayout;

    private WindowManager.LayoutParams layoutParams = null;
    private WindowManager windowManager = null;

    private WarningServiceReceiver warningServiceReceiver = null;

    private boolean isWarningShowing = false;

    @Override
    public void onCreate(){
        super.onCreate();
        this.context = getApplicationContext();

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);

        warningLayout = new WarningLayout(getApplicationContext(), null);

        windowManager.addView(warningLayout, layoutParams);
        hideWarningLayout();

        warningServiceReceiver = new WarningServiceReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(INTENT_FILTER);
        registerReceiver(warningServiceReceiver, filter);

        Log.d(Constants.DEBUG_TAG, "WarningService Created");
    }

    public void showWarningLayout(){
        warningLayout.setVisibility(View.VISIBLE);
        Log.d(Constants.DEBUG_TAG, "Show Warning Layout called");
    }

    public void hideWarningLayout(){
        warningLayout.setVisibility(View.GONE);

        Log.d(Constants.DEBUG_TAG, "Hide Warning Layout called");
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
        i.putExtra("command", "show");
        sendBroadcast(i);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.i(Constants.DEBUG_TAG, "Notification Removed");

        Intent i = new  Intent(INTENT_FILTER);
        i.putExtra("command","hide");
        sendBroadcast(i);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(warningServiceReceiver != null) {
            unregisterReceiver(warningServiceReceiver);
        }
        if(warningLayout != null){
            windowManager.removeView(warningLayout);
        }

        Log.d(Constants.DEBUG_TAG, "WarningService Destroyed");

        stopSelf();
    }

    class WarningServiceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getStringExtra("command").equals("show")) {
                showWarningLayout();
            } else if (intent.getStringExtra("command").equals("hide")) {
                hideWarningLayout();
            }
        }
    }
}